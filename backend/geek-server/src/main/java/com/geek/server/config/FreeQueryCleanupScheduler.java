package com.geek.server.config;

import com.geek.server.domain.FreeQueryScheduleConfig;
import com.geek.server.service.IFreeQueryScheduleConfigService;
import com.geek.server.service.IFreeQueryStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes enabled free-query cleanup configurations stored in the database.
 * The database configuration is polled so changes take effect without a restart.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FreeQueryCleanupScheduler {
    private static final String DATA_CLEANUP = "DATA_CLEANUP";
    private static final long POLL_INTERVAL_SECONDS = 30L;

    private final IFreeQueryScheduleConfigService scheduleConfigService;
    private final IFreeQueryStatisticsService statisticsService;
    private final Map<Long, LocalDateTime> lastCheckedAt = new ConcurrentHashMap<>();

    @Scheduled(
            fixedDelayString = "${free-query.cleanup.poll-interval-ms:30000}",
            initialDelayString = "${free-query.cleanup.initial-delay-ms:10000}")
    public void runDueCleanupTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<FreeQueryScheduleConfig> configs;
        try {
            configs = scheduleConfigService.selectEnabledList();
        } catch (Exception ex) {
            log.error("Free-query cleanup configuration lookup failed", ex);
            return;
        }

        Set<Long> activeTaskIds = new HashSet<>();
        for (FreeQueryScheduleConfig config : configs) {
            if (config == null || !DATA_CLEANUP.equals(config.getTaskType()) || config.getId() == null) {
                continue;
            }
            activeTaskIds.add(config.getId());
            processConfig(config, now);
        }
        lastCheckedAt.keySet().removeIf(id -> !activeTaskIds.contains(id));
    }

    private void processConfig(FreeQueryScheduleConfig config, LocalDateTime now) {
        Long taskId = config.getId();
        CronExpression cron;
        try {
            if (StringUtils.isBlank(config.getCronExpression())) {
                log.warn("Free-query cleanup task {} has no cron expression", taskId);
                lastCheckedAt.put(taskId, now);
                return;
            }
            cron = CronExpression.parse(config.getCronExpression());
        } catch (Exception ex) {
            log.warn("Free-query cleanup task {} has invalid cron expression: {}",
                    taskId, config.getCronExpression());
            lastCheckedAt.put(taskId, now);
            return;
        }

        LocalDateTime previousCheck = lastCheckedAt.get(taskId);
        if (previousCheck == null) {
            previousCheck = now.minusSeconds(POLL_INTERVAL_SECONDS);
        }
        LocalDateTime nextTrigger = cron.next(previousCheck);
        if (nextTrigger == null || nextTrigger.isAfter(now)) {
            lastCheckedAt.put(taskId, now);
            return;
        }

        Integer retentionDays = config.getRetentionDays();
        if (retentionDays == null || retentionDays < 1) {
            log.warn("Free-query cleanup task {} has invalid retention days: {}", taskId, retentionDays);
            lastCheckedAt.put(taskId, now);
            return;
        }

        Date cutoffTime = Date.from(now.minusDays(retentionDays)
                .atZone(ZoneId.systemDefault())
                .toInstant());
        try {
            int deleted = statisticsService.cleanupExpiredRecords(cutoffTime);
            log.info("Free-query cleanup finished: taskId={}, taskName={}, cron={}, retentionDays={}, cutoff={}, deleted={}",
                    taskId, config.getTaskName(), config.getCronExpression(), retentionDays, cutoffTime, deleted);
            lastCheckedAt.put(taskId, now);
        } catch (Exception ex) {
            log.error("Free-query cleanup failed: taskId={}, taskName={}, cutoff={}",
                    taskId, config.getTaskName(), cutoffTime, ex);
        }
    }
}
