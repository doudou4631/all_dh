package com.geek.server.domain.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 批量查询任务实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTask {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务状态：RUNNING, COMPLETED, FAILED, CANCELLED
     */
    private TaskStatus status;

    /**
     * 总查询数量
     */
    private Integer totalCount;

    /**
     * 已完成数量
     */
    private Integer completedCount;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failedCount;

    /**
     * 进度百分比
     */
    private Integer percentage;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 完成时间
     */
    private LocalDateTime endTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 查询结果列表
     */
    @Builder.Default
    private List<ApiResult> results = new java.util.ArrayList<>();

    /**
     * 为 true 时：RUNNING 状态下 taskStatus/taskResults 不返回明细，仅在 COMPLETED 后一次性返回（优化版批量）。
     */
    @Builder.Default
    private boolean deferResultsUntilComplete = false;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        RUNNING("执行中"),
        COMPLETED("已完成"),
        FAILED("失败"),
        CANCELLED("已取消");

        private final String description;

        TaskStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * API查询结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResult {
        /**
         * 手机号码
         */
        private String phoneNumber;

        /**
         * 平台ID
         */
        private String platformId;

        /**
         * 平台名称
         */
        private String platformName;

        /**
         * 查询是否成功
         */
        private Boolean success;

        /**
         * 查询数据
         */
        private Object data;

        /**
         * 错误信息
         */
        private String error;

        /**
         * 响应时间（毫秒）
         */
        private Long responseTime;

        /**
         * 查询时间戳
         */
        private LocalDateTime timestamp;
    }
}
