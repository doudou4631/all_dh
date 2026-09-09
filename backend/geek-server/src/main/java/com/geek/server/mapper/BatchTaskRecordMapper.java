package com.geek.server.mapper;

import com.geek.server.domain.entity.BatchTaskRecord;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 批量任务记录Mapper接口
 */
@Mapper
public interface BatchTaskRecordMapper extends BaseMapper<BatchTaskRecord> {

    /**
     * 根据用户ID查询任务记录
     *
     * @param userId 用户ID
     * @return 任务记录列表
     */
    List<BatchTaskRecord> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和状态查询任务记录
     *
     * @param userId 用户ID
     * @param status 任务状态
     * @return 任务记录列表
     */
    List<BatchTaskRecord> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 根据任务ID查询任务记录
     *
     * @param taskId 任务ID
     * @return 任务记录
     */
    BatchTaskRecord selectByTaskId(@Param("taskId") String taskId);

    /**
     * 查询指定时间之后的任务记录
     *
     * @param userId 用户ID
     * @param startTime 开始时间
     * @return 任务记录列表
     */
    List<BatchTaskRecord> selectByUserIdAfterTime(@Param("userId") Long userId, @Param("startTime") Date startTime);

    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 新状态
     * @param completedCount 完成数量
     * @param successCount 成功数量
     * @param failedCount 失败数量
     * @param percentage 进度百分比
     * @param errorMessage 错误信息
     * @return 影响行数
     */
    int updateTaskStatus(@Param("taskId") String taskId,
                         @Param("status") String status,
                         @Param("completedCount") Integer completedCount,
                         @Param("successCount") Integer successCount,
                         @Param("failedCount") Integer failedCount,
                         @Param("percentage") Integer percentage,
                         @Param("errorMessage") String errorMessage);

    /**
     * 更新任务结束时间
     *
     * @param taskId 任务ID
     * @param endTime 结束时间
     * @return 影响行数
     */
    int updateTaskEndTime(@Param("taskId") String taskId, @Param("endTime") Date endTime);

    /**
     * 批量删除过期任务记录
     *
     * @param beforeTime 时间点之前的记录
     * @return 删除数量
     */
    int deleteExpiredRecords(@Param("beforeTime") Date beforeTime);

    /**
     * 根据用户ID和任务ID列表查询任务记录
     *
     * @param userId 用户ID
     * @param taskIds 任务ID列表
     * @return 任务记录列表
     */
    List<BatchTaskRecord> selectByUserIdAndTaskIds(@Param("userId") Long userId, @Param("taskIds") List<String> taskIds);
}
