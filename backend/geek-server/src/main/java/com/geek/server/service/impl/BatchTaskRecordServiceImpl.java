package com.geek.server.service.impl;

import com.geek.server.domain.entity.BatchTaskRecord;
import com.geek.server.domain.vo.ApiRequestVO;
import com.geek.server.mapper.BatchTaskRecordMapper;
import com.geek.server.service.IBatchTaskRecordService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 批量任务记录服务实现
 */
@Slf4j
@Service
public class BatchTaskRecordServiceImpl extends ServiceImpl<BatchTaskRecordMapper, BatchTaskRecord> implements IBatchTaskRecordService {

    @Autowired
    private BatchTaskRecordMapper batchTaskRecordMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public BatchTaskRecord createTaskRecord(String taskId, Long userId, String taskName, List<ApiRequestVO> requests) {
        try {
            // 提取手机号码列表
            List<String> phoneNumbers = requests.stream()
                    .map(ApiRequestVO::getPhoneNumber)
                    .distinct()
                    .collect(Collectors.toList());

            // 提取平台配置信息
            List<Object> platformConfigs = requests.stream()
                    .map(request -> {
                        return new Object() {
                            public final Long platformId = Long.valueOf(request.getPlatformId());
                            public final String platformName = request.getPlatformName();
                            public final String url = request.getUrl();
                        };
                    })
                    .distinct()
                    .collect(Collectors.toList());

            BatchTaskRecord record = BatchTaskRecord.builder()
                    .taskId(taskId)
                    .userId(userId)
                    .taskName(taskName != null ? taskName : "批量查询任务")
                    .status("RUNNING")
                    .totalCount(requests.size())
                    .completedCount(0)
                    .successCount(0)
                    .failedCount(0)
                    .percentage(0)
                    .phoneNumbers(objectMapper.writeValueAsString(phoneNumbers))
                    .platformConfigs(objectMapper.writeValueAsString(platformConfigs))
                    .description("批量查询 " + phoneNumbers.size() + " 个号码")
                    .startTime(new Date())
                    .delFlag("0")
                    .build();

            this.save(record);
            log.info("创建任务记录成功，任务ID: {}, 用户ID: {}, 总数量: {}", taskId, userId, requests.size());
            return record;

        } catch (JsonProcessingException e) {
            log.error("创建任务记录失败，JSON序列化异常，任务ID: {}", taskId, e);
            throw new RuntimeException("创建任务记录失败", e);
        }
    }

    @Override
    public boolean updateTaskStatus(String taskId, String status, Integer completedCount,
                                    Integer successCount, Integer failedCount, Integer percentage,
                                    String errorMessage) {
        try {
            int result = batchTaskRecordMapper.updateTaskStatus(taskId, status, completedCount,
                    successCount, failedCount, percentage, errorMessage);

            if (result > 0) {
                log.debug("更新任务状态成功，任务ID: {}, 状态: {}, 进度: {}/{}",
                        taskId, status, completedCount, completedCount != null ? completedCount : 0);
                return true;
            } else {
                log.warn("更新任务状态失败，任务ID: {}, 状态: {}", taskId, status);
                return false;
            }
        } catch (Exception e) {
            log.error("更新任务状态异常，任务ID: {}, 状态: {}", taskId, status, e);
            return false;
        }
    }

    @Override
    public boolean updateTaskEndTime(String taskId, Date endTime) {
        try {
            int result = batchTaskRecordMapper.updateTaskEndTime(taskId, endTime);

            if (result > 0) {
                log.debug("更新任务结束时间成功，任务ID: {}, 结束时间: {}", taskId, endTime);
                return true;
            } else {
                log.warn("更新任务结束时间失败，任务ID: {}", taskId);
                return false;
            }
        } catch (Exception e) {
            log.error("更新任务结束时间异常，任务ID: {}", taskId, e);
            return false;
        }
    }

    @Override
    public List<BatchTaskRecord> getTaskRecordsByUserId(Long userId) {
        return batchTaskRecordMapper.selectByUserId(userId);
    }

    @Override
    public List<BatchTaskRecord> getTaskRecordsByUserIdAndStatus(Long userId, String status) {
        return batchTaskRecordMapper.selectByUserIdAndStatus(userId, status);
    }

    @Override
    public BatchTaskRecord getTaskRecordByTaskId(String taskId) {
        return batchTaskRecordMapper.selectByTaskId(taskId);
    }

    @Override
    public List<BatchTaskRecord> getRunningTasks(Long userId) {
        return batchTaskRecordMapper.selectByUserIdAndStatus(userId, "RUNNING");
    }

    @Override
    public List<BatchTaskRecord> getRecentTaskRecords(Long userId, Integer days) {
        Date startTime = new Date();
        startTime.setTime(startTime.getTime() - days * 24 * 60 * 60 * 1000);
        return batchTaskRecordMapper.selectByUserIdAfterTime(userId, startTime);
    }

    @Override
    public int cleanupExpiredRecords(Date beforeTime) {
        try {
            int count = batchTaskRecordMapper.deleteExpiredRecords(beforeTime);
            log.info("清理过期任务记录完成，清理数量: {}, 时间点: {}", count, beforeTime);
            return count;
        } catch (Exception e) {
            log.error("清理过期任务记录异常", e);
            return 0;
        }
    }

    @Override
    public List<BatchTaskRecord> exportTaskRecords(Long userId, List<String> taskIds) {
        if (taskIds != null && !taskIds.isEmpty()) {
            // 使用自定义Mapper方法查询
            List<BatchTaskRecord> records = batchTaskRecordMapper.selectByUserIdAndTaskIds(userId, taskIds);
            // 过滤已删除的记录并按创建时间倒序排序
            return records.stream()
                    .filter(record -> "0".equals(record.getDelFlag()))
                    .sorted((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()))
                    .collect(Collectors.toList());
        } else {
            return getTaskRecordsByUserId(userId);
        }
    }
}
