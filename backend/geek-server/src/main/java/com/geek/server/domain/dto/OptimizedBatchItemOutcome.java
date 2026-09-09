package com.geek.server.domain.dto;

import com.geek.server.domain.UserApiQueryRecord;
import com.geek.server.domain.entity.BatchTask;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One optimized-batch HTTP item: query row to flush in batch, and in-memory API result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizedBatchItemOutcome {

    /** Row for user_api_query_record; null when same early-exit paths as legacy single() (no insert). */
    private UserApiQueryRecord queryRecord;

    private BatchTask.ApiResult apiResult;

    /** True when business success: one point to deduct after batch (same rule as legacy). */
    private boolean needPointDeduction;
}
