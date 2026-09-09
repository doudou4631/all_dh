package com.geek.server.service.impl;

import com.geek.common.utils.DateUtils;
import com.geek.common.utils.SecurityUtils;
import com.geek.server.domain.FreeQueryIpPolicy;
import com.geek.server.mapper.FreeQueryIpPolicyMapper;
import com.geek.server.service.IFreeQueryIpPolicyService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class FreeQueryIpPolicyServiceImpl implements IFreeQueryIpPolicyService {
    private static final int LOCK_STRIPES = 256;
    private final ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];

    @Autowired
    private FreeQueryIpPolicyMapper mapper;

    public FreeQueryIpPolicyServiceImpl() {
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new ReentrantLock();
        }
    }

    @Override
    public FreeQueryIpPolicy getConfig() {
        FreeQueryIpPolicy config = mapper.selectConfig();
        if (config != null) {
            return config;
        }
        FreeQueryIpPolicy defaults = new FreeQueryIpPolicy();
        defaults.setId(1L);
        defaults.setDailyLimit(20);
        defaults.setIntervalMs(500);
        defaults.setResetTime(LocalTime.MIDNIGHT);
        defaults.setExceedMessage("今日查询次数已用完，请明日再试。");
        defaults.setWhitelistIps("");
        return defaults;
    }

    @Override
    public int updateConfig(FreeQueryIpPolicy config) {
        validateConfig(config);
        config.setId(1L);
        config.setWhitelistIps(normalizeWhitelist(config.getWhitelistIps()));
        config.setUpdateBy(SecurityUtils.getUsername());
        config.setUpdateTime(DateUtils.getNowDate());
        return mapper.updateConfig(config);
    }

    @Override
    public ReentrantLock lockFor(String ipAddr) {
        int index = (ipAddr == null ? 0 : ipAddr.hashCode()) & (LOCK_STRIPES - 1);
        return locks[index];
    }

    @Override
    public String validateAccess(String ipAddr) {
        FreeQueryIpPolicy config = getConfig();
        if (!whitelist(config).contains(ipAddr)) {
            LocalDateTime current = LocalDateTime.now();
            LocalTime resetTime = config.getResetTime() == null ? LocalTime.MIDNIGHT : config.getResetTime();
            LocalDateTime windowStart = current.toLocalDate().atTime(resetTime);
            if (current.isBefore(windowStart)) {
                windowStart = windowStart.minusDays(1);
            }
            long used = mapper.countQueries(ipAddr, windowStart);
            if (used >= config.getDailyLimit()) {
                return config.getExceedMessage();
            }
        }

        return null;
    }

    @Override
    public void waitForConfiguredDelay(long requestStartedAt) {
        Integer configured = getConfig().getIntervalMs();
        long delayMs = configured == null ? 0L : configured.longValue();
        long remaining = delayMs - (System.currentTimeMillis() - requestStartedAt);
        if (remaining <= 0) {
            return;
        }
        try {
            Thread.sleep(remaining);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void validateConfig(FreeQueryIpPolicy config) {
        if (config == null || config.getDailyLimit() == null
                || config.getDailyLimit() < 1 || config.getDailyLimit() > 1000000) {
            throw new IllegalArgumentException("每日查询限额必须在1到1000000之间");
        }
        if (config.getIntervalMs() == null || config.getIntervalMs() < 0 || config.getIntervalMs() > 600000) {
            throw new IllegalArgumentException("查询间隔必须在0到600000毫秒之间");
        }
        if (config.getResetTime() == null) {
            throw new IllegalArgumentException("请选择配额重置时间");
        }
        if (StringUtils.isBlank(config.getExceedMessage()) || config.getExceedMessage().trim().length() > 500) {
            throw new IllegalArgumentException("超限提示文案不能为空且不能超过500个字符");
        }
        for (String ip : whitelist(config)) {
            if (ip.length() > 64 || !ip.matches("^[0-9A-Fa-f:.]+$")) {
                throw new IllegalArgumentException("白名单包含无效IP：" + ip);
            }
        }
    }

    private Set<String> whitelist(FreeQueryIpPolicy config) {
        if (config == null || StringUtils.isBlank(config.getWhitelistIps())) {
            return Set.of();
        }
        return Arrays.stream(config.getWhitelistIps().split("[,;\\s]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeWhitelist(String value) {
        FreeQueryIpPolicy holder = new FreeQueryIpPolicy();
        holder.setWhitelistIps(value);
        return String.join("\n", whitelist(holder));
    }
}
