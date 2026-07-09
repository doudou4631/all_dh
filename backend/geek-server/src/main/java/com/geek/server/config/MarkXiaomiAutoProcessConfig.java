package com.geek.server.config;

import com.geek.server.service.IMarkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled auto-detection for pending xiaomi order items.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarkXiaomiAutoProcessConfig {

    private final IMarkOrderService markOrderService;

    @Scheduled(fixedRate = 30000)
    public void autoDetectXiaomiPendingItems() {
        try {
            markOrderService.processXiaomiPendingItemsAuto();
        } catch (Exception ex) {
            log.warn("xiaomi auto-detect task failed", ex);
        }
    }
}
