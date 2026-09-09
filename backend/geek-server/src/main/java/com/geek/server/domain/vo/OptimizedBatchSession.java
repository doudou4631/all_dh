package com.geek.server.domain.vo;

import com.geek.server.domain.UserAggregateConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Read-only context for optimized batch workers; built on submit thread.
 * Not used by ApiServiceImpl.single.
 */
@Getter
@AllArgsConstructor
public class OptimizedBatchSession {

    private final Long userId;
    private final String username;
    private final UserAggregateConfig aggregateConfig;
}
