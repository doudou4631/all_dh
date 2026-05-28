package com.geek.server.task;

import com.geek.server.domain.entity.BatchTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 批量任务管理器
 * 用于管理异步批量查询任务的状态和结果
 */
@Slf4j
@Component
public class BatchTaskManager {

    /**
     * 任务存储，使用ConcurrentHashMap保证线程安全
     */
    private final ConcurrentHashMap<String, BatchTask> taskMap = new ConcurrentHashMap<>();

    /**
     * 保存任务
     */
    public void saveTask(BatchTask task) {
        taskMap.put(task.getTaskId(), task);
        log.debug("任务已保存，任务ID: {}, 状态: {}", task.getTaskId(), task.getStatus());
    }

    /**
     * 获取任务
     */
    public BatchTask getTask(String taskId) {
        return taskMap.get(taskId);
    }

    /**
     * 添加查询结果到任务
     */
    public synchronized void addResult(String taskId, BatchTask.ApiResult result) {
        BatchTask task = taskMap.get(taskId);
        if (task != null) {
            task.getResults().add(result);
            log.debug("已添加查询结果到任务，任务ID: {}, 手机号: {}, 平台: {}",
                    taskId, result.getPhoneNumber(), result.getPlatformName());
        }
    }

    /**
     * 任务结束时一次性写入全部结果与最终进度（优化版批量）
     */
    public synchronized void setFinalResultsAndProgress(String taskId, List<BatchTask.ApiResult> results,
                                                        int completedCount, int successCount, int failedCount, int percentage) {
        BatchTask task = taskMap.get(taskId);
        if (task != null) {
            task.getResults().clear();
            if (results != null) {
                task.getResults().addAll(results);
            }
            task.setCompletedCount(completedCount);
            task.setSuccessCount(successCount);
            task.setFailedCount(failedCount);
            task.setPercentage(percentage);
            log.debug("任务最终结果已写入，任务ID: {}, 条数: {}", taskId, results != null ? results.size() : 0);
        }
    }

    /**
     * 更新任务进度
     */
    public synchronized void updateProgress(String taskId, int completedCount, int successCount, int failedCount, int percentage) {
        BatchTask task = taskMap.get(taskId);
        if (task != null) {
            task.setCompletedCount(completedCount);
            task.setSuccessCount(successCount);
            task.setFailedCount(failedCount);
            task.setPercentage(percentage);

            log.debug("任务进度已更新，任务ID: {}, 完成数: {}, 成功数: {}, 失败数: {}, 进度: {}%",
                    taskId, completedCount, successCount, failedCount, percentage);
        }
    }

    /**
     * 更新任务状态
     */
    public synchronized void updateStatus(String taskId, BatchTask.TaskStatus status) {
        BatchTask task = taskMap.get(taskId);
        if (task != null) {
            task.setStatus(status);
            if (status == BatchTask.TaskStatus.COMPLETED ||
                    status == BatchTask.TaskStatus.FAILED ||
                    status == BatchTask.TaskStatus.CANCELLED) {
                task.setEndTime(LocalDateTime.now());
            }

            log.info("任务状态已更新，任务ID: {}, 新状态: {}", taskId, status);
        }
    }

    /**
     * 设置任务错误信息
     */
    public synchronized void setErrorMessage(String taskId, String errorMessage) {
        BatchTask task = taskMap.get(taskId);
        if (task != null) {
            task.setErrorMessage(errorMessage);
            log.debug("任务错误信息已设置，任务ID: {}, 错误: {}", taskId, errorMessage);
        }
    }

    /**
     * 删除任务
     */
    public void removeTask(String taskId) {
        BatchTask removed = taskMap.remove(taskId);
        if (removed != null) {
            log.debug("任务已删除，任务ID: {}", taskId);
        }
    }

    /**
     * 获取所有任务
     */
    public List<BatchTask> getAllTasks() {
        return List.copyOf(taskMap.values());
    }

    /**
     * 获取运行中的任务数量
     */
    public int getRunningTaskCount() {
        return (int) taskMap.values().stream()
                .filter(task -> task.getStatus() == BatchTask.TaskStatus.RUNNING)
                .count();
    }

    /**
     * 清理已完成的任务
     * 清理超过指定时间的已完成任务，避免内存占用过多
     */
    public void cleanupCompletedTasks() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24); // 清理24小时前的任务

        taskMap.entrySet().removeIf(entry -> {
            BatchTask task = entry.getValue();
            boolean shouldRemove = (task.getStatus() == BatchTask.TaskStatus.COMPLETED ||
                    task.getStatus() == BatchTask.TaskStatus.FAILED ||
                    task.getStatus() == BatchTask.TaskStatus.CANCELLED) &&
                    task.getEndTime() != null &&
                    task.getEndTime().isBefore(cutoffTime);

            if (shouldRemove) {
                log.debug("清理已完成任务，任务ID: {}, 完成时间: {}",
                        task.getTaskId(), task.getEndTime());
            }

            return shouldRemove;
        });

        log.info("任务清理完成，当前任务总数: {}", taskMap.size());
    }

    /**
     * 获取任务统计信息
     */
    public TaskStatistics getTaskStatistics() {
        long runningCount = taskMap.values().stream()
                .filter(task -> task.getStatus() == BatchTask.TaskStatus.RUNNING)
                .count();

        long completedCount = taskMap.values().stream()
                .filter(task -> task.getStatus() == BatchTask.TaskStatus.COMPLETED)
                .count();

        long failedCount = taskMap.values().stream()
                .filter(task -> task.getStatus() == BatchTask.TaskStatus.FAILED)
                .count();

        long cancelledCount = taskMap.values().stream()
                .filter(task -> task.getStatus() == BatchTask.TaskStatus.CANCELLED)
                .count();

        return new TaskStatistics(
                taskMap.size(),
                (int) runningCount,
                (int) completedCount,
                (int) failedCount,
                (int) cancelledCount
        );
    }

    /**
     * 任务统计信息
     */
    public record TaskStatistics(
            int totalTasks,
            int runningTasks,
            int completedTasks,
            int failedTasks,
            int cancelledTasks
    ) {}
}
