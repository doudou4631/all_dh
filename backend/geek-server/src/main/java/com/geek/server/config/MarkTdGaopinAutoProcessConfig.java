package com.geek.server.config;

import com.geek.server.service.IMarkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Xiaomi keeps its own polling workflow. td_gaopin is dispatched exclusively
 * through MarkInspectionScheduler so its inspection policy is the sole switch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarkTdGaopinAutoProcessConfig {

    private final IMarkOrderService markOrderService;

    @Scheduled(fixedRate = 30000, initialDelay = 15000)
    public void autoDetectXiaomiPendingItems() {
        try {
            markOrderService.processPendingInspection("xiaomi", 100, "SUCCESS");
        } catch (Exception ex) {
            log.warn("xiaomi auto-detect task failed", ex);
        }
    }
}
