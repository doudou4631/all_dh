package com.geek.server.config;

import com.geek.server.service.IMarkInspectionService;
import com.geek.server.service.IMarkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarkInspectionScheduler {
    private final IMarkInspectionService service;
    private final IMarkOrderService markOrderService;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedRuns() {
        try {
            Map<String, Integer> result = service.recoverInterruptedRuns();
            if (result.getOrDefault("failedRuns", 0) > 0 || result.getOrDefault("releasedPolicies", 0) > 0) {
                log.warn("已恢复中断的巡检任务: {}", result);
            }
        } catch (Exception ex) {
            log.error("恢复中断的巡检任务失败", ex);
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void dispatch() {
        try { service.runDuePolicies(); } catch (Exception ex) { log.warn("巡检策略调度失败", ex); }
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void syncTdxSecondTasks() {
        try { markOrderService.processTdxSecondPendingItemsAuto(); }
        catch (Exception ex) { log.warn("Taidixiong二次任务调度失败", ex); }
    }

    @Scheduled(cron = "${mark.inspection.log-cleanup-cron:0 30 3 * * ?}")
    public void cleanupInspectionLogs() {
        try {
            Date cutoff = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
            Map<String, Integer> result = service.cleanupExpiredLogs(cutoff);
            if (result.getOrDefault("logs", 0) > 0) {
                log.info("巡检日志定时清理完成, cutoff={}, result={}", cutoff, result);
            }
        } catch (Exception ex) {
            log.error("巡检日志定时清理失败", ex);
        }
    }
}
