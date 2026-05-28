package com.geek.server.service.impl;

import com.geek.common.utils.SecurityUtils;
import com.geek.server.domain.UserApiQueryRecord;
import com.geek.server.domain.entity.BatchTask;
import com.geek.server.domain.entity.BatchTaskRecord;
import com.geek.server.domain.vo.ApiRequestVO;
import com.geek.server.service.IAsyncBatchService;
import com.geek.server.service.IApiService;
import com.geek.server.service.IBatchTaskRecordService;
import com.geek.server.service.IUserApiQueryRecordService;
import com.geek.server.task.BatchTaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 异步批量查询服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncBatchServiceImpl implements IAsyncBatchService {

    /**
     * 与前端 batchRecord.vue getPlatformCode 保持一致，用于历史结果表格列对齐
     */
    private static final Map<String, String> PLATFORM_NAME_TO_CODE = createPlatformNameToCodeMap();

    private static Map<String, String> createPlatformNameToCodeMap() {
        Map<String, String> m = new HashMap<>();
        m.put("腾讯", "tengxun");
        m.put("腾讯平台", "tengxun");
        m.put("百度", "baidu");
        m.put("百度平台", "baidu");
        m.put("360平台", "360");
        m.put("360平台查询", "360");
        m.put("360手机卫士", "sanliuling");
        m.put("电话邦", "dianhuabang");
        m.put("小米电话", "xiaomi");
        m.put("小米手机", "xiaomi");
        m.put("联通安全管家", "ltgj");
        m.put("联通", "ltgj");
        m.put("中国联通", "ltgj");
        m.put("搜狗号码通", "sghmt");
        m.put("移动高频", "yidonggaopin");
        m.put("泰迪熊", "taidixiong");
        m.put("泰迪熊平台", "taidixiong");
        return Collections.unmodifiableMap(m);
    }

    private final BatchTaskManager taskManager;
    private final IApiService apiService;
    private final IBatchTaskRecordService batchTaskRecordService;
    private final IUserApiQueryRecordService userApiQueryRecordService;

    @Override
    public String submitBatchQuery(List<ApiRequestVO> requests) {
        // 生成任务ID
        String taskId = UUID.randomUUID().toString().replace("-", "");

        // 创建内存任务
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
                .build();

        // 保存任务到内存
        taskManager.saveTask(task);

        // 创建数据库任务记录（这里需要获取当前用户ID，暂时使用1L）
        Long userId = SecurityUtils.getUserId();
        BatchTaskRecord taskRecord = batchTaskRecordService.createTaskRecord(
                taskId, userId, "批量查询任务", requests);

        log.info("异步批量查询任务已提交，任务ID: {}, 总查询数量: {}, 记录ID: {}",
                taskId, requests.size(), taskRecord.getId());

        // 异步执行任务
        executeBatchQueryAsync(taskId, requests);

        return taskId;
    }

    @Override
    public BatchTask getBatchTaskStatus(String taskId) {
        return taskManager.getTask(taskId);
    }

    @Override
    public BatchTask getBatchTaskResults(String taskId) {
        BatchTask memory = taskManager.getTask(taskId);
        if (memory != null) {
            return memory;
        }
        return rebuildBatchTaskFromDb(taskId);
    }

    /**
     * 内存任务已清理或重启后，从 batch_task_record + user_api_query_record 还原结果（仅当前用户自己的任务）
     */
    private BatchTask rebuildBatchTaskFromDb(String taskId) {
        BatchTaskRecord record = batchTaskRecordService.getTaskRecordByTaskId(taskId);
        if (record == null) {
            return null;
        }
        Long uid = SecurityUtils.getUserId();
        if (uid == null || !Objects.equals(record.getUserId(), uid)) {
            return null;
        }

        UserApiQueryRecord query = new UserApiQueryRecord();
        query.setTaskId(taskId);
        query.setQueryType("1");
        query.setUserId(uid);
        List<UserApiQueryRecord> rows = userApiQueryRecordService.selectUserApiQueryRecordList(query);
        if (rows == null) {
            rows = Collections.emptyList();
        } else {
            rows = new ArrayList<>(rows);
            rows.sort(Comparator.comparing(UserApiQueryRecord::getId, Comparator.nullsLast(Long::compareTo)));
        }

        List<BatchTask.ApiResult> results = new ArrayList<>(rows.size());
        for (UserApiQueryRecord r : rows) {
            results.add(apiResultFromQueryRecord(r));
        }

        BatchTask.TaskStatus st = parsePersistedTaskStatus(record.getStatus());
        LocalDateTime createLt = toLocalDateTime(record.getCreateTime());
        LocalDateTime startLt = toLocalDateTime(record.getStartTime());
        if (startLt == null) {
            startLt = createLt;
        }
        LocalDateTime endLt = toLocalDateTime(record.getEndTime());

        int total = record.getTotalCount() != null ? record.getTotalCount() : Math.max(results.size(), 0);
        int completed = record.getCompletedCount() != null ? record.getCompletedCount() : results.size();
        int succ = record.getSuccessCount() != null ? record.getSuccessCount() : (int) results.stream().filter(a -> Boolean.TRUE.equals(a.getSuccess())).count();
        int fail = record.getFailedCount() != null ? record.getFailedCount() : (int) results.stream().filter(a -> !Boolean.TRUE.equals(a.getSuccess())).count();
        int pct = record.getPercentage() != null ? record.getPercentage() : (total > 0 ? Math.min(100, completed * 100 / total) : 0);

        return BatchTask.builder()
                .taskId(taskId)
                .status(st)
                .totalCount(total)
                .completedCount(completed)
                .successCount(succ)
                .failedCount(fail)
                .percentage(pct)
                .createTime(createLt != null ? createLt : LocalDateTime.now())
                .startTime(startLt != null ? startLt : LocalDateTime.now())
                .endTime(endLt)
                .errorMessage(record.getErrorMessage())
                .results(results)
                .build();
    }

    private static BatchTask.TaskStatus parsePersistedTaskStatus(String status) {
        if (status == null || status.isEmpty()) {
            return BatchTask.TaskStatus.COMPLETED;
        }
        try {
            return BatchTask.TaskStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return BatchTask.TaskStatus.COMPLETED;
        }
    }

    private static LocalDateTime toLocalDateTime(Date d) {
        if (d == null) {
            return null;
        }
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static String resolvePlatformCode(String platformName) {
        if (platformName == null || platformName.isBlank()) {
            return "unknown";
        }
        String hit = PLATFORM_NAME_TO_CODE.get(platformName.trim());
        if (hit != null) {
            return hit;
        }
        return platformName.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static BatchTask.ApiResult apiResultFromQueryRecord(UserApiQueryRecord r) {
        boolean ok = "0".equals(r.getRequestStatus());
        String statusText = r.getResults();
        if (statusText == null || statusText.isBlank()) {
            statusText = ok ? "normal" : "fail";
        }
        String code = resolvePlatformCode(r.getPlatformName());
        Map<String, Object> prItem = new HashMap<>(4);
        prItem.put("platform", code);
        prItem.put("status", statusText);
        Map<String, Object> data = new HashMap<>(4);
        data.put("success", ok);
        data.put("platformResults", Collections.singletonList(prItem));

        LocalDateTime ts = toLocalDateTime(r.getCreateTime());
        if (ts == null) {
            ts = LocalDateTime.now();
        }

        return BatchTask.ApiResult.builder()
                .phoneNumber(r.getPhone())
                .platformId(r.getPlatformId() != null ? String.valueOf(r.getPlatformId()) : null)
                .platformName(r.getPlatformName())
                .success(ok)
                .data(data)
                .error(ok ? null : (r.getResponseResult() != null ? r.getResponseResult() : "失败"))
                .responseTime(r.getRequestTime() != null ? r.getRequestTime() : 0L)
                .timestamp(ts)
                .build();
    }

    @Override
    public boolean cancelBatchTask(String taskId) {
        BatchTask task = taskManager.getTask(taskId);
        if (task == null || task.getStatus() != BatchTask.TaskStatus.RUNNING) {
            return false;
        }

        task.setStatus(BatchTask.TaskStatus.CANCELLED);
        task.setEndTime(LocalDateTime.now());
        taskManager.saveTask(task);

        // 更新数据库记录
        batchTaskRecordService.updateTaskStatus(taskId, "CANCELLED",
                task.getCompletedCount(), task.getSuccessCount(), task.getFailedCount(),
                task.getPercentage(), null);
        batchTaskRecordService.updateTaskEndTime(taskId, new Date());

        log.info("批量查询任务已取消，任务ID: {}", taskId);
        return true;
    }

    @Override
    public void cleanupCompletedTasks() {
        taskManager.cleanupCompletedTasks();
    }

    /**
     * 异步执行批量查询
     */
    @Async("batchTaskExecutor")
    public void executeBatchQueryAsync(String taskId, List<ApiRequestVO> requests) {
        try {
            BatchTask task = taskManager.getTask(taskId);
            if (task == null || task.getStatus() != BatchTask.TaskStatus.RUNNING) {
                log.warn("任务不存在或状态异常，任务ID: {}", taskId);
                return;
            }

            int totalCount = requests.size();
            int completedCount = 0;
            int successCount = 0;
            int failedCount = 0;

            for (ApiRequestVO request : requests) {
                // 检查任务是否被取消
                BatchTask currentTask = taskManager.getTask(taskId);
                if (currentTask == null || currentTask.getStatus() != BatchTask.TaskStatus.RUNNING) {
                    log.info("任务已取消，停止执行，任务ID: {}", taskId);
                    return;
                }

                try {
                    // 执行单个API查询
                    request.setTaskId(taskId);
                    request.setQueryType("1");
                    BatchTask.ApiResult result = executeSingleQuery(request);

                    // 添加结果到内存任务
                    taskManager.addResult(taskId, result);

                    // 更新计数
                    completedCount++;
                    if (result.getSuccess()) {
                        successCount++;
                    } else {
                        failedCount++;
                    }

                    // 更新任务进度
                    int percentage = totalCount > 0 ? (completedCount * 100) / totalCount : 0;
                    taskManager.updateProgress(taskId, completedCount, successCount, failedCount, percentage);

                    // 更新数据库记录
                    batchTaskRecordService.updateTaskStatus(taskId, "RUNNING",
                            completedCount, successCount, failedCount, percentage, null);

                    log.debug("批量查询进度更新，任务ID: {}, 进度: {}/{}",
                            taskId, completedCount, totalCount);

                } catch (Exception e) {
                    log.error("单个查询执行失败，任务ID: {}, 手机号: {}, 错误: {}",
                            taskId, request.getPhoneNumber(), e.getMessage());

                    // 添加失败结果
                    BatchTask.ApiResult errorResult = BatchTask.ApiResult.builder()
                            .phoneNumber(request.getPhoneNumber())
                            .platformId(request.getPlatformId())
                            .platformName(request.getPlatformName())
                            .success(false)
                            .error(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build();

                    taskManager.addResult(taskId, errorResult);

                    completedCount++;
                    failedCount++;

                    // 更新进度
                    int percentage = totalCount > 0 ? (completedCount * 100) / totalCount : 0;
                    taskManager.updateProgress(taskId, completedCount, successCount, failedCount, percentage);

                    // 更新数据库记录
                    batchTaskRecordService.updateTaskStatus(taskId, "RUNNING",
                            completedCount, successCount, failedCount, percentage, null);
                }
            }

            // 任务完成
            BatchTask finalTask = taskManager.getTask(taskId);
            if (finalTask != null && finalTask.getStatus() == BatchTask.TaskStatus.RUNNING) {
                finalTask.setStatus(BatchTask.TaskStatus.COMPLETED);
                finalTask.setEndTime(LocalDateTime.now());
                taskManager.saveTask(finalTask);

                // 更新数据库记录
                int finalPercentage = totalCount > 0 ? 100 : 0;
                batchTaskRecordService.updateTaskStatus(taskId, "COMPLETED",
                        totalCount, successCount, failedCount, finalPercentage, null);
                batchTaskRecordService.updateTaskEndTime(taskId, new Date());

                log.info("批量查询任务完成，任务ID: {}, 总数: {}, 成功: {}, 失败: {}",
                        taskId, totalCount, successCount, failedCount);
            }

        } catch (Exception e) {
            log.error("批量查询任务执行异常，任务ID: {}", taskId, e);

            // 标记任务失败
            BatchTask task = taskManager.getTask(taskId);
            if (task != null) {
                task.setStatus(BatchTask.TaskStatus.FAILED);
                task.setEndTime(LocalDateTime.now());
                task.setErrorMessage(e.getMessage());
                taskManager.saveTask(task);

                // 更新数据库记录
                batchTaskRecordService.updateTaskStatus(taskId, "FAILED",
                        task.getCompletedCount(), task.getSuccessCount(), task.getFailedCount(),
                        task.getPercentage(), e.getMessage());
                batchTaskRecordService.updateTaskEndTime(taskId, new Date());
            }
        }
    }

    /**
     * 执行单个API查询
     */
    private BatchTask.ApiResult executeSingleQuery(ApiRequestVO request) {
        long startTime = System.currentTimeMillis();

        try {
            // 调用现有的API服务
            Map<String, Object> result = (Map<String, Object>) apiService.single(request);
            long responseTime = System.currentTimeMillis() - startTime;

            // 检查返回结果结构
            boolean success = false;
            Object data = null;
            String error = null;
            
            if (result != null) {
                // 检查是否有success字段
                if (result.containsKey("success")) {
                    success = Boolean.TRUE.equals(result.get("success"));
                } else {
                    // 如果没有success字段，默认认为成功
                    success = true;
                }
                
                if (success) {
                    data = result;
                } else {
                    error = (String) result.get("message");
                }
            }

            return BatchTask.ApiResult.builder()
                    .phoneNumber(request.getPhoneNumber())
                    .platformId(request.getPlatformId())
                    .platformName(request.getPlatformName())
                    .success(success)
                    .data(data)
                    .error(error)
                    .responseTime(responseTime)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;

            return BatchTask.ApiResult.builder()
                    .phoneNumber(request.getPhoneNumber())
                    .platformId(request.getPlatformId())
                    .platformName(request.getPlatformName())
                    .success(false)
                    .error(e.getMessage())
                    .responseTime(responseTime)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }
}
