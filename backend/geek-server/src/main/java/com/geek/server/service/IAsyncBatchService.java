package com.geek.server.service;

import com.geek.server.domain.entity.BatchTask;
import com.geek.server.domain.vo.ApiRequestVO;

import java.util.List;

/**
 * 异步批量查询服务接口
 */
public interface IAsyncBatchService {

    /**
     * 提交异步批量查询任务
     *
     * @param requests 查询请求列表
     * @return 任务ID
     */
    String submitBatchQuery(List<ApiRequestVO> requests);

    /**
     * 获取批量任务状态
     *
     * @param taskId 任务ID
     * @return 任务信息
     */
    BatchTask getBatchTaskStatus(String taskId);

    /**
     * 获取批量任务结果
     *
     * @param taskId 任务ID
     * @return 任务结果
     */
    BatchTask getBatchTaskResults(String taskId);

    /**
     * 取消批量任务
     *
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    boolean cancelBatchTask(String taskId);

    /**
     * 清理已完成的任务（可选，用于定时清理）
     */
    void cleanupCompletedTasks();
}
