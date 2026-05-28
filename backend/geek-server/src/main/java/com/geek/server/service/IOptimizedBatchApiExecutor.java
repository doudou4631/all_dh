package com.geek.server.service;

import com.geek.server.domain.dto.OptimizedBatchItemOutcome;
import com.geek.server.domain.vo.ApiRequestVO;
import com.geek.server.domain.vo.OptimizedBatchSession;

/**
 * Optimized-batch HTTP only: no IApiService.single, no DB, no points.
 */
public interface IOptimizedBatchApiExecutor {

    OptimizedBatchItemOutcome execute(ApiRequestVO apiRequestVO, OptimizedBatchSession session, String taskId);
}
