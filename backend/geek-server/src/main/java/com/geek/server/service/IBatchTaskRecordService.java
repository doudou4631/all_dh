package com.geek.server.service;

import com.geek.server.domain.entity.BatchTaskRecord;
import com.geek.server.domain.vo.ApiRequestVO;
import com.mybatisflex.core.service.IService;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 批量任务记录服务接口
 */
public interface IBatchTaskRecordService extends IService<BatchTaskRecord> {

    /**
     * 创建任务记录
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param taskName 任务名称
     * @param requests 查询请求列表
     * @return 任务记录
     */
    BatchTaskRecord createTaskRecord(String taskId, Long userId, String taskName, List<ApiRequestVO> requests);

    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 任务状态
     * @param completedCount 完成数量
     * @param successCount 成功数量
     * @param failedCount 失败数量
     * @param percentage 进度百分比
     * @param errorMessage 错误信息
     * @return 是否成功
     */
    boolean updateTaskStatus(String taskId, String status, Integer completedCount,
                             Integer successCount, Integer failedCount, Integer percentage,
                             String errorMessage);

    /**
     * 更新任务结束时间
     *
     * @param taskId 任务ID
     * @param endTime 结束时间
     * @return 是否成功
     */
    boolean updateTaskEndTime(String taskId, Date endTime);

    /**
     * 根据用户ID查询任务记录
     *
     * @param userId 用户ID
     * @return 任务记录列表
     */
    List<BatchTaskRecord> getTaskRecordsByUserId(Long userId);

    /**
     * 根据用户ID和状态查询任务记录
     *
     * @param userId 用户ID
     * @param status 任务状态
     * @return 任务记录列表
     */
    List<BatchTaskRecord> getTaskRecordsByUserIdAndStatus(Long userId, String status);

    /**
     * 根据任务ID查询任务记录
     *
     * @param taskId 任务ID
     * @return 任务记录
     */
    BatchTaskRecord getTaskRecordByTaskId(String taskId);

    /**
     * 获取当前用户的运行中任务
     *
     * @param userId 用户ID
     * @return 运行中的任务记录列表
     */
    List<BatchTaskRecord> getRunningTasks(Long userId);

    /**
     * 获取用户最近的任务记录
     *
     * @param userId 用户ID
     * @param days 天数
     * @return 任务记录列表
     */
    List<BatchTaskRecord> getRecentTaskRecords(Long userId, Integer days);

    /**
     * 清理过期任务记录
     *
     * @param beforeTime 时间点
     * @return 清理数量
     */
    int cleanupExpiredRecords(Date beforeTime);

    /**
     * 导出任务记录
     *
     * @param userId 用户ID
     * @param taskIds 任务ID列表（可选）
     * @return 任务记录列表
     */
    List<BatchTaskRecord> exportTaskRecords(Long userId, List<String> taskIds);
}
