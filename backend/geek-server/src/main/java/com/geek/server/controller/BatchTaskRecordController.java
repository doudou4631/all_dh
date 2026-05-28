package com.geek.server.controller;

import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.annotation.Log;
import com.geek.common.enums.BusinessType;
import com.geek.common.utils.poi.ExcelUtil;
import com.geek.server.domain.entity.BatchTaskRecord;
import com.geek.server.service.IBatchTaskRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.geek.common.utils.PageUtils.startPage;
import static com.geek.common.utils.SecurityUtils.isAdmin;

/**
 * 批量任务记录控制器
 */
@RestController
@RequestMapping("/server/batchTask")
@RequiredArgsConstructor
public class BatchTaskRecordController  extends BaseController {

    private final IBatchTaskRecordService batchTaskRecordService;

    /**
     * 查询批量任务记录列表
     */
    @GetMapping("/list")
    public TableDataInfo list(BatchTaskRecord batchTaskRecord) {
        startPage();
        List<BatchTaskRecord> list = batchTaskRecordService.getTaskRecordsByUserId(getUserId());
        return getDataTable(list);
    }

    /**
     * 获取当前用户的运行中任务
     */
    @GetMapping("/running")
    public AjaxResult getRunningTasks() {
        List<BatchTaskRecord> runningTasks = batchTaskRecordService.getRunningTasks(getUserId());
        return AjaxResult.success(runningTasks);
    }

    /**
     * 获取当前用户最近的任务记录
     */
    @GetMapping("/recent/{days}")
    public AjaxResult getRecentTasks(@PathVariable("days") Integer days) {
        List<BatchTaskRecord> recentTasks = batchTaskRecordService.getRecentTaskRecords(getUserId(), days);
        return AjaxResult.success(recentTasks);
    }

    /**
     * 导出批量任务记录
     */
    @PreAuthorize("@ss.hasPermi('server:batchTask:export')")
    @Log(title = "批量任务记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BatchTaskRecord batchTaskRecord) {
        List<BatchTaskRecord> list = batchTaskRecordService.getTaskRecordsByUserId(getUserId());
        ExcelUtil<BatchTaskRecord> util = new ExcelUtil<BatchTaskRecord>(BatchTaskRecord.class);
        util.exportExcel(response, list, "批量任务记录数据");
    }

    /**
     * 获取批量任务记录详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        BatchTaskRecord record = batchTaskRecordService.getById(id);
        if (record != null && !record.getUserId().equals(getUserId())) {
            return AjaxResult.error("无权访问该任务记录");
        }
        return AjaxResult.success(record);
    }

    /**
     * 根据任务ID获取任务记录
     */
    @GetMapping(value = "/task/{taskId}")
    public AjaxResult getByTaskId(@PathVariable("taskId") String taskId) {
        BatchTaskRecord record = batchTaskRecordService.getTaskRecordByTaskId(taskId);
        if (record != null && !record.getUserId().equals(getUserId())) {
            return AjaxResult.error("无权访问该任务记录");
        }
        return AjaxResult.success(record);
    }

    /**
     * 删除批量任务记录
     */
    @PreAuthorize("@ss.hasPermi('server:batchTask:remove')")
    @Log(title = "批量任务记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        // 检查权限：只能删除自己的任务记录
        for (Long id : ids) {
            BatchTaskRecord record = batchTaskRecordService.getById(id);
            if (record != null && !record.getUserId().equals(getUserId())) {
                return AjaxResult.error("无权删除该任务记录");
            }
        }

        return toAjax(batchTaskRecordService.removeByIds(Arrays.asList(ids)));
    }

    /**
     * 清理过期任务记录
     */
    @PreAuthorize("@ss.hasPermi('server:batchTask:cleanup')")
    @Log(title = "批量任务记录", businessType = BusinessType.CLEAN)
    @DeleteMapping("/cleanup/{days}")
    public AjaxResult cleanup(@PathVariable("days") Integer days) {
        // 只有管理员才能执行清理操作
        if (!isAdmin()) {
            return AjaxResult.error("无权限执行此操作");
        }

        int count = batchTaskRecordService.cleanupExpiredRecords(
                new Date());
        return AjaxResult.success("清理完成，共清理 " + count + " 条记录");
    }

    /**
     * 获取任务统计信息
     */
    @GetMapping("/statistics")
    public AjaxResult getStatistics() {
        Long userId = getUserId();

        List<BatchTaskRecord> allTasks = batchTaskRecordService.getTaskRecordsByUserId(userId);
        List<BatchTaskRecord> runningTasks = batchTaskRecordService.getRunningTasks(userId);
        List<BatchTaskRecord> recentTasks = batchTaskRecordService.getRecentTaskRecords(userId, 7);

        // 统计各状态任务数量
        long totalCount = allTasks.size();
        long runningCount = runningTasks.size();
        long completedCount = allTasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        long failedCount = allTasks.stream().filter(t -> "FAILED".equals(t.getStatus())).count();
        long cancelledCount = allTasks.stream().filter(t -> "CANCELLED".equals(t.getStatus())).count();

        java.util.Map<String, Object> statistics = new java.util.HashMap<>();
        statistics.put("totalCount", totalCount);
        statistics.put("runningCount", runningCount);
        statistics.put("completedCount", completedCount);
        statistics.put("failedCount", failedCount);
        statistics.put("cancelledCount", cancelledCount);
        statistics.put("recentCount", recentTasks.size());

        return AjaxResult.success(statistics);
    }
}
