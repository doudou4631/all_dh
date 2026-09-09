package com.geek.server.config;

import com.geek.server.task.BatchTaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务配置
 * 用于清理已完成的批量任务
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class TaskCleanupConfig {

    private final BatchTaskManager batchTaskManager;

    /**
     * 每小时清理一次已完成的任务
     * 清理24小时前的已完成、失败、取消的任务
     */
    @Scheduled(cron = "0 0 * * * ?") // 每小时执行一次
    public void cleanupCompletedTasks() {
        try {
            log.info("开始清理已完成的批量任务...");

            BatchTaskManager.TaskStatistics beforeStats = batchTaskManager.getTaskStatistics();
            batchTaskManager.cleanupCompletedTasks();
            BatchTaskManager.TaskStatistics afterStats = batchTaskManager.getTaskStatistics();

            log.info("任务清理完成。清理前：总任务数={}, 运行中={}",
                    beforeStats.totalTasks(), beforeStats.runningTasks());
            log.info("任务清理完成。清理后：总任务数={}, 运行中={}",
                    afterStats.totalTasks(), afterStats.runningTasks());

        } catch (Exception e) {
            log.error("清理已完成任务时发生异常", e);
        }
    }

    /**
     * 每30分钟输出一次任务统计信息
     */
    @Scheduled(cron = "0 */30 * * * ?") // 每30分钟执行一次
    public void logTaskStatistics() {
        try {
            BatchTaskManager.TaskStatistics stats = batchTaskManager.getTaskStatistics();

            log.info("批量任务统计信息：总任务数={}, 运行中={}, 已完成={}, 失败={}, 已取消={}",
                    stats.totalTasks(), stats.runningTasks(), stats.completedTasks(),
                    stats.failedTasks(), stats.cancelledTasks());

        } catch (Exception e) {
            log.error("获取任务统计信息时发生异常", e);
        }
    }
}
