package com.geek.server.service.impl;

import com.geek.common.utils.SecurityUtils;
import com.geek.server.domain.UserApiQueryRecord;
import com.geek.server.domain.dto.OptimizedBatchItemOutcome;
import com.geek.server.domain.entity.BatchTask;
import com.geek.server.domain.entity.BatchTaskRecord;
import com.geek.server.domain.vo.ApiRequestVO;
import com.geek.server.domain.vo.OptimizedBatchSession;
import com.geek.server.service.IAsyncBatchOptimizedService;
import com.geek.server.service.IOptimizedBatchApiExecutor;
import com.geek.server.service.IBatchTaskRecordService;
import com.geek.server.service.IUserAggregateConfigService;
import com.geek.server.task.BatchTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Optimized async batch: {@link IOptimizedBatchApiExecutor}, no per-request delay, batch persist, deferred API results.
 */
@Slf4j
@Service
public class AsyncBatchOptimizedServiceImpl implements IAsyncBatchOptimizedService {

    private static final int PERSIST_PROGRESS_EVERY_N = 20;
    private static final long PERSIST_PROGRESS_MIN_INTERVAL_MS = 1500L;

    private final BatchTaskManager taskManager;
    private final IBatchTaskRecordService batchTaskRecordService;
    private final IAsyncBatchOptimizedService self;
    private final Executor batchOptimizedItemExecutor;
    private final IOptimizedBatchApiExecutor optimizedBatchApiExecutor;
    private final IUserAggregateConfigService userAggregateConfigService;
    private final OptimizedBatchPersistenceService optimizedBatchPersistenceService;

    public AsyncBatchOptimizedServiceImpl(
            BatchTaskManager taskManager,
            IBatchTaskRecordService batchTaskRecordService,
            @Lazy IAsyncBatchOptimizedService self,
            @Qualifier("batchOptimizedItemExecutor") Executor batchOptimizedItemExecutor,
            IOptimizedBatchApiExecutor optimizedBatchApiExecutor,
            IUserAggregateConfigService userAggregateConfigService,
            OptimizedBatchPersistenceService optimizedBatchPersistenceService) {
        this.taskManager = taskManager;
        this.batchTaskRecordService = batchTaskRecordService;
        this.self = self;
        this.batchOptimizedItemExecutor = batchOptimizedItemExecutor;
        this.optimizedBatchApiExecutor = optimizedBatchApiExecutor;
        this.userAggregateConfigService = userAggregateConfigService;
        this.optimizedBatchPersistenceService = optimizedBatchPersistenceService;
    }

    @Override
    public String submitBatchQueryOptimized(List<ApiRequestVO> requests) {
        if (requests == null) {
            throw new IllegalArgumentException("request list must not be null");
        }

        OptimizedBatchSession session = new OptimizedBatchSession(
                SecurityUtils.getUserId(),
                SecurityUtils.getUsername(),
                userAggregateConfigService.selectUserAggregateConfigById(1L));

        String taskId = UUID.randomUUID().toString().replace("-", "");

        BatchTask task = BatchTask.builder()
                .taskId(taskId)
                .status(BatchTask.TaskStatus.RUNNING)
                .totalCount(requests.size())
                .completedCount(0)
                .successCount(0)
                .failedCount(0)
                .percentage(0)
                .createTime(LocalDateTime.now())
                .startTime(LocalDateTime.now())
                .deferResultsUntilComplete(true)
                .build();
        taskManager.saveTask(task);

        Long userId = SecurityUtils.getUserId();
        BatchTaskRecord taskRecord = batchTaskRecordService.createTaskRecord(
                taskId, userId, "batch query (optimized)", requests);

        log.info("optimized batch submitted taskId={} count={} recordId={}",
                taskId, requests.size(), taskRecord.getId());

        SecurityContext parent = SecurityContextHolder.getContext();
        SecurityContext delegated = SecurityContextHolder.createEmptyContext();
        if (parent.getAuthentication() != null) {
            delegated.setAuthentication(parent.getAuthentication());
        }

        self.executeBatchQueryOptimizedAsync(taskId, requests, delegated, session);

        return taskId;
    }

    @Override
    @Async("batchOptimizedTaskExecutor")
    public void executeBatchQueryOptimizedAsync(String taskId, List<ApiRequestVO> requests,
                                                SecurityContext securityContext,
                                                OptimizedBatchSession session) {
        SecurityContext contextTemplate = securityContext != null ? securityContext : SecurityContextHolder.createEmptyContext();
        final Object persistLock = new Object();
        final AtomicLong lastPersistMs = new AtomicLong(System.currentTimeMillis());
        final ConcurrentLinkedQueue<OptimizedBatchItemOutcome> outcomeQueue = new ConcurrentLinkedQueue<>();

        try {
            BatchTask task = taskManager.getTask(taskId);
            if (task == null || task.getStatus() != BatchTask.TaskStatus.RUNNING) {
                log.warn("optimized batch missing or bad status taskId={}", taskId);
                return;
            }

            int totalCount = requests.size();
            if (totalCount == 0) {
                completeRunningTask(taskId, 0, 0, 0, 0);
                return;
            }

            // No per-platform semaphore: only thread pool limits concurrent HTTP; all sub-requests are submitted as fast as the pool allows.

            AtomicInteger completed = new AtomicInteger(0);
            AtomicInteger success = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);

            List<CompletableFuture<Void>> futures = new ArrayList<>(totalCount);
            for (ApiRequestVO request : requests) {
                final ApiRequestVO req = request;
                futures.add(CompletableFuture.runAsync(
                        () -> runOneItem(taskId, req, totalCount, contextTemplate,
                                completed, success, failed, persistLock, lastPersistMs, session, outcomeQueue),
                        batchOptimizedItemExecutor));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            List<OptimizedBatchItemOutcome> outcomes = new ArrayList<>();
            OptimizedBatchItemOutcome polled;
            while ((polled = outcomeQueue.poll()) != null) {
                outcomes.add(polled);
            }

            int c = outcomes.size();
            int sc = (int) outcomes.stream()
                    .filter(o -> o.getApiResult() != null && Boolean.TRUE.equals(o.getApiResult().getSuccess()))
                    .count();
            int fc = c - sc;
            int pct = totalCount > 0 ? (c * 100) / totalCount : 0;
            syncPersist(taskId, c, sc, fc, pct, persistLock, lastPersistMs);

            List<BatchTask.ApiResult> apiResults = new ArrayList<>(c);
            List<UserApiQueryRecord> records = new ArrayList<>();
            for (OptimizedBatchItemOutcome o : outcomes) {
                if (o.getApiResult() != null) {
                    apiResults.add(o.getApiResult());
                }
                if (o.getQueryRecord() != null) {
                    records.add(o.getQueryRecord());
                }
            }

            try {
                optimizedBatchPersistenceService.persistAfterOptimizedBatch(records, session.getUserId(), new ArrayList<>());
            } catch (Exception e) {
                log.error("optimized batch persist failed taskId={}", taskId, e);
                BatchTask t = taskManager.getTask(taskId);
                if (t != null) {
                    t.setStatus(BatchTask.TaskStatus.FAILED);
                    t.setEndTime(LocalDateTime.now());
                    t.setErrorMessage(e.getMessage());
                    taskManager.saveTask(t);
                    batchTaskRecordService.updateTaskStatus(taskId, "FAILED", c, sc, fc, pct, e.getMessage());
                    batchTaskRecordService.updateTaskEndTime(taskId, new Date());
                }
                taskManager.setFinalResultsAndProgress(taskId, apiResults, c, sc, fc, pct);
                return;
            }

            int finalPct = totalCount > 0 ? Math.min(100, (c * 100) / totalCount) : 0;
            taskManager.setFinalResultsAndProgress(taskId, apiResults, c, sc, fc, finalPct);

            BatchTask finalTask = taskManager.getTask(taskId);
            if (finalTask != null && finalTask.getStatus() == BatchTask.TaskStatus.RUNNING) {
                finalTask.setStatus(BatchTask.TaskStatus.COMPLETED);
                finalTask.setEndTime(LocalDateTime.now());
                taskManager.saveTask(finalTask);
                batchTaskRecordService.updateTaskStatus(taskId, "COMPLETED", c, sc, fc, finalPct, null);
                batchTaskRecordService.updateTaskEndTime(taskId, new Date());
                log.info("optimized batch done taskId={} total={} ok={} fail={}", taskId, totalCount, sc, fc);
            }

        } catch (Exception e) {
            log.error("optimized batch failed taskId={}", taskId, e);
            BatchTask t = taskManager.getTask(taskId);
            if (t != null) {
                t.setStatus(BatchTask.TaskStatus.FAILED);
                t.setEndTime(LocalDateTime.now());
                t.setErrorMessage(e.getMessage());
                taskManager.saveTask(t);
                batchTaskRecordService.updateTaskStatus(taskId, "FAILED",
                        t.getCompletedCount(), t.getSuccessCount(), t.getFailedCount(),
                        t.getPercentage(), e.getMessage());
                batchTaskRecordService.updateTaskEndTime(taskId, new Date());
            }
        }
    }

    private void runOneItem(String taskId, ApiRequestVO req, int totalCount,
                            SecurityContext contextTemplate,
                            AtomicInteger completed, AtomicInteger success, AtomicInteger failed,
                            Object persistLock, AtomicLong lastPersistMs,
                            OptimizedBatchSession session,
                            ConcurrentLinkedQueue<OptimizedBatchItemOutcome> outcomeQueue) {
        SecurityContextHolder.setContext(contextTemplate);
        try {
            BatchTask current = taskManager.getTask(taskId);
            if (current == null || current.getStatus() != BatchTask.TaskStatus.RUNNING) {
                return;
            }
            try {
                OptimizedBatchItemOutcome outcome = optimizedBatchApiExecutor.execute(req, session, taskId);
                outcomeQueue.offer(outcome);
                if (outcome.getApiResult() != null) {
                    onItemDonePartial(taskId, outcome.getApiResult(), totalCount, completed, success, failed, persistLock, lastPersistMs);
                }
            } catch (Exception e) {
                log.error("optimized batch item error taskId={} phone={}", taskId, req.getPhoneNumber(), e);
                BatchTask.ApiResult err = BatchTask.ApiResult.builder()
                        .phoneNumber(req.getPhoneNumber())
                        .platformId(req.getPlatformId())
                        .platformName(req.getPlatformName())
                        .success(false)
                        .error(e.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();
                outcomeQueue.offer(OptimizedBatchItemOutcome.builder()
                        .queryRecord(null)
                        .apiResult(err)
                        .needPointDeduction(false)
                        .build());
                onItemDonePartial(taskId, err, totalCount, completed, success, failed, persistLock, lastPersistMs);
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void onItemDonePartial(String taskId, BatchTask.ApiResult result, int totalCount,
                                   AtomicInteger completed, AtomicInteger success, AtomicInteger failed,
                                   Object persistLock, AtomicLong lastPersistMs) {
        int c = completed.incrementAndGet();
        if (Boolean.TRUE.equals(result.getSuccess())) {
            success.incrementAndGet();
        } else {
            failed.incrementAndGet();
        }
        int sc = success.get();
        int fc = failed.get();
        int pct = totalCount > 0 ? (c * 100) / totalCount : 0;
        taskManager.updateProgress(taskId, c, sc, fc, pct);
        tryPersistThrottled(taskId, c, sc, fc, pct, persistLock, lastPersistMs);
    }

    private void tryPersistThrottled(String taskId, int c, int sc, int fc, int pct,
                                     Object persistLock, AtomicLong lastPersistMs) {
        long now = System.currentTimeMillis();
        boolean byCount = c % PERSIST_PROGRESS_EVERY_N == 0;
        boolean byTime = now - lastPersistMs.get() >= PERSIST_PROGRESS_MIN_INTERVAL_MS;
        if (!byCount && !byTime) {
            return;
        }
        synchronized (persistLock) {
            long now2 = System.currentTimeMillis();
            if (now2 - lastPersistMs.get() < PERSIST_PROGRESS_MIN_INTERVAL_MS && c % PERSIST_PROGRESS_EVERY_N != 0) {
                return;
            }
            lastPersistMs.set(now2);
            batchTaskRecordService.updateTaskStatus(taskId, "RUNNING", c, sc, fc, pct, null);
        }
    }

    private void syncPersist(String taskId, int c, int sc, int fc, int pct,
                             Object persistLock, AtomicLong lastPersistMs) {
        synchronized (persistLock) {
            lastPersistMs.set(System.currentTimeMillis());
            batchTaskRecordService.updateTaskStatus(taskId, "RUNNING", c, sc, fc, pct, null);
        }
    }

    private void completeRunningTask(String taskId, int totalCount, int successCount, int failedCount, int percentage) {
        BatchTask finalTask = taskManager.getTask(taskId);
        if (finalTask != null && finalTask.getStatus() == BatchTask.TaskStatus.RUNNING) {
            finalTask.setStatus(BatchTask.TaskStatus.COMPLETED);
            finalTask.setEndTime(LocalDateTime.now());
            taskManager.saveTask(finalTask);
            batchTaskRecordService.updateTaskStatus(taskId, "COMPLETED",
                    totalCount, successCount, failedCount, percentage, null);
            batchTaskRecordService.updateTaskEndTime(taskId, new Date());
        }
    }
}
