package com.geek.server.controller;
import com.geek.common.annotation.Anonymous;

import com.geek.common.core.domain.AjaxResult;
import com.geek.server.domain.entity.BatchTask;
import com.geek.server.domain.vo.ApiRequestVO;
import com.geek.server.domain.vo.FreeBatchQueryRequest;
import com.geek.server.service.IAsyncBatchOptimizedService;
import com.geek.server.service.IAsyncBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 异步批量查询控制器
 */
@Tag(name = "异步批量查询")
@RestController
@RequestMapping("/server/apiServer")
@RequiredArgsConstructor
public class BatchApiController {

    private final IAsyncBatchService asyncBatchService;
    private final IAsyncBatchOptimizedService asyncBatchOptimizedService;
    private final FreeQueryController freeQueryController;

    /**
     * 提交异步批量查询任务
     */
    @Operation(summary = "提交异步批量查询任务")
    @PostMapping("/asyncBatch")
    public AjaxResult submitBatchQuery(@RequestBody List<ApiRequestVO> requests) {
        try {
            String taskId = asyncBatchService.submitBatchQuery(requests);
            return AjaxResult.success("任务提交成功", Map.of("taskId", taskId));
        } catch (Exception e) {
            return AjaxResult.error("任务提交失败：" + e.getMessage());
        }
    }

    /**
     * 提交异步批量查询任务（免费用户兼容入口）
     */
    @Operation(summary = "提交异步批量查询任务（免费用户兼容入口）")
    @Anonymous
    @PostMapping(value = "/asyncBatchOpt", headers = "X-Free-Token")
    public AjaxResult submitBatchQueryOptimizedForFree(@RequestBody FreeBatchQueryRequest request,
                                                       HttpServletRequest httpServletRequest) {
        return freeQueryController.batch(request, httpServletRequest);
    }

    /**
     * 提交异步批量查询任务（优化版：线程池并行外呼 + 进度落库节流；不按平台限流）
     */
    @Operation(summary = "提交异步批量查询任务（优化版）")
    @PostMapping("/asyncBatchOpt")
    public AjaxResult submitBatchQueryOptimized(@RequestBody List<ApiRequestVO> requests) {
        try {
            String taskId = asyncBatchOptimizedService.submitBatchQueryOptimized(requests);
            return AjaxResult.success("任务提交成功", Map.of("taskId", taskId));
        } catch (Exception e) {
            return AjaxResult.error("任务提交失败：" + e.getMessage());
        }
    }

    /**
     * 查询批量任务状态
     */
    @Operation(summary = "查询批量任务状态")
    @GetMapping("/taskStatus/{taskId}")
    public AjaxResult getBatchTaskStatus(@PathVariable String taskId) {
        try {
            BatchTask task = asyncBatchService.getBatchTaskStatus(taskId);
            if (task == null) {
                return AjaxResult.error("任务不存在");
            }

            // 构建返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("status", task.getStatus().name());

            Map<String, Object> progress = new HashMap<>();
            progress.put("total", task.getTotalCount());
            progress.put("completed", task.getCompletedCount());
            progress.put("successCount", task.getSuccessCount());
            progress.put("failedCount", task.getFailedCount());
            progress.put("percentage", task.getPercentage());
            result.put("progress", progress);

            if (BatchTask.TaskStatus.RUNNING.equals(task.getStatus()) && task.isDeferResultsUntilComplete()) {
                result.put("results", new ArrayList<>());
            } else {
                result.put("results", task.getResults() != null ? task.getResults() : new ArrayList<>());
            }
            result.put("createTime", task.getCreateTime());
            result.put("startTime", task.getStartTime());
            result.put("errorMessage", task.getErrorMessage());

            return AjaxResult.success(result);
        } catch (Exception e) {
            return AjaxResult.error("查询任务状态失败：" + e.getMessage());
        }
    }

    /**
     * 获取批量任务结果
     */
    @Operation(summary = "获取批量任务结果")
    @GetMapping("/taskResults/{taskId}")
    public AjaxResult getBatchTaskResults(@PathVariable String taskId) {
        try {
            BatchTask task = asyncBatchService.getBatchTaskResults(taskId);
            if (task == null) {
                return AjaxResult.error("任务不存在");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("taskId", task.getTaskId());
            result.put("status", task.getStatus().name());
            result.put("totalCount", task.getTotalCount());
            result.put("completedCount", task.getCompletedCount());
            result.put("successCount", task.getSuccessCount());
            result.put("failedCount", task.getFailedCount());
            result.put("percentage", task.getPercentage());
            if (BatchTask.TaskStatus.RUNNING.equals(task.getStatus()) && task.isDeferResultsUntilComplete()) {
                result.put("results", new ArrayList<>());
            } else {
                result.put("results", task.getResults() != null ? task.getResults() : new ArrayList<>());
            }
            result.put("createTime", task.getCreateTime());
            result.put("startTime", task.getStartTime());
            result.put("endTime", task.getEndTime());
            result.put("errorMessage", task.getErrorMessage());

            return AjaxResult.success(result);
        } catch (Exception e) {
            return AjaxResult.error("获取任务结果失败：" + e.getMessage());
        }
    }

    /**
     * 取消批量任务
     */
    @Operation(summary = "取消批量任务")
    @DeleteMapping("/cancelTask/{taskId}")
    public AjaxResult cancelBatchTask(@PathVariable String taskId) {
        try {
            boolean success = asyncBatchService.cancelBatchTask(taskId);
            if (success) {
                return AjaxResult.success("任务取消成功");
            } else {
                return AjaxResult.error("任务取消失败，任务可能已完成或不存在");
            }
        } catch (Exception e) {
            return AjaxResult.error("取消任务失败：" + e.getMessage());
        }
    }
}
