package com.geek.server.config;

import com.geek.server.service.IMarkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled auto-detection for pending td_gaopin order items.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarkTdGaopinAutoProcessConfig {

    private final IMarkOrderService markOrderService;

    @Scheduled(fixedRate = 30000)
    public void autoDetectTdGaopinPendingItems() {
        try {
            markOrderService.processTdGaopinPendingItemsAuto();
        } catch (Exception ex) {
            log.warn("td_gaopin auto-detect task failed", ex);
        }
    }
}
