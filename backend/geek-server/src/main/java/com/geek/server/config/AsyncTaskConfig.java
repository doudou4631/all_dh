package com.geek.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncTaskConfig {

    /**
     * 批量任务执行器
     * 用于执行异步批量查询任务
     */
    @Bean("batchTaskExecutor")
    public Executor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(2);

        // 最大线程数
        executor.setMaxPoolSize(10);

        // 队列容量
        executor.setQueueCapacity(100);

        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);

        // 线程名前缀
        executor.setThreadNamePrefix("batch-task-");

        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("批量任务执行器初始化完成：核心线程数={}, 最大线程数={}, 队列容量={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * 优化版批量任务入口（@Async）
     */
    @Bean("batchOptimizedTaskExecutor")
    public Executor batchOptimizedTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("batch-opt-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("优化批量任务执行器初始化完成：核心线程数={}, 最大线程数={}, 队列容量={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }

    /**
     * 优化版单条并行执行器（CompletableFuture）
     */
    @Bean("batchOptimizedItemExecutor")
    public Executor batchOptimizedItemExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(32);
        executor.setMaxPoolSize(64);
        // Large queue so CallerRunsPolicy does not run overflow tasks on the submit thread (would serialize HTTP).
        executor.setQueueCapacity(50000);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("batch-opt-item-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(90);
        executor.initialize();
        log.info("优化批量子任务执行器初始化完成：核心线程数={}, 最大线程数={}, 队列容量={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }

    /**
     * 定时任务执行器
     * 用于执行定时清理任务
     */
    @Bean("scheduledTaskExecutor")
    public Executor scheduledTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("scheduled-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("定时任务执行器初始化完成：核心线程数={}, 最大线程数={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }
}
