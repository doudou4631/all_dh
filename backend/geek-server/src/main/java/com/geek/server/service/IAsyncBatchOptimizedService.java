package com.geek.server.service;

import com.geek.server.domain.vo.ApiRequestVO;
import com.geek.server.domain.vo.OptimizedBatchSession;
import org.springframework.security.core.context.SecurityContext;

import java.util.List;

/**
 * Optimized async batch: parallel HTTP via thread pool only; throttled DB progress for task record.
 * Shares {@link com.geek.server.task.BatchTaskManager} with {@link IAsyncBatchService}.
 */
public interface IAsyncBatchOptimizedService {

    /**
     * Submit optimized batch (same request body as POST /asyncBatch).
     */
    String submitBatchQueryOptimized(List<ApiRequestVO> requests);

    /**
     * Runs the batch asynchronously; invoked via Spring proxy from submit.
     *
     * @param securityContext snapshot from the HTTP thread for worker threads
     */
    void executeBatchQueryOptimizedAsync(String taskId, List<ApiRequestVO> requests,
                                         SecurityContext securityContext,
                                         OptimizedBatchSession session);
}
