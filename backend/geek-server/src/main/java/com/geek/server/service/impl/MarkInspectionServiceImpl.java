package com.geek.server.service.impl;

import com.geek.common.exception.ServiceException;
import com.geek.common.utils.StringUtils;
import com.geek.server.domain.MarkInspectionPolicy;
import com.geek.server.domain.MarkInspectionRunDetail;
import com.geek.server.domain.MarkInspectionRunLog;
import com.geek.server.domain.MarkPlatformMapping;
import com.geek.server.domain.MarkWumaPlatform;
import com.geek.server.mapper.MarkInspectionMapper;
import com.geek.server.service.IMarkInspectionService;
import com.geek.server.service.IMarkOrderService;
import com.geek.server.service.IMarkPlatformMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MarkInspectionServiceImpl implements IMarkInspectionService {
    private final MarkInspectionMapper mapper;
    private final IMarkOrderService orderService;
    private final IMarkPlatformMappingService platformMappingService;

    @Override public List<MarkInspectionPolicy> selectPolicies(MarkInspectionPolicy query) { return mapper.selectPolicyList(query == null ? new MarkInspectionPolicy() : query); }
    @Override public MarkInspectionPolicy selectPolicy(Long id) { return mapper.selectPolicyById(id); }
    @Override public int savePolicy(MarkInspectionPolicy policy) { normalize(policy); policy.setNextRunTime(nextTime(policy)); return mapper.insertPolicy(policy); }
    @Override public int updatePolicy(MarkInspectionPolicy policy) {
        if (policy == null || policy.getId() == null) throw new ServiceException("策略ID不能为空");
        MarkInspectionPolicy existing = mapper.selectPolicyById(policy.getId());
        if (existing == null) throw new ServiceException("巡检策略不存在");
        if ("1".equals(existing.getRunning())) throw new ServiceException("策略正在执行，请结束后再修改");
        if (policy.getOrderScope() == null) policy.setOrderScope(existing.resolvedOrderScope());
        if (policy.getAutoTakeover() == null) policy.setAutoTakeover(existing.takeoverEnabled());
        normalize(policy);
        policy.setNextRunTime(nextTime(policy));
        int updated = mapper.updatePolicy(policy);
        if (updated == 0) throw new ServiceException("策略正在执行或已删除，请刷新后重试");
        return updated;
    }
    @Override public int deletePolicy(Long id) { if (id == null) throw new ServiceException("策略ID不能为空"); return mapper.deletePolicy(id); }
    @Override public List<MarkInspectionRunLog> selectLogs(MarkInspectionRunLog query) { return mapper.selectLogList(query == null ? new MarkInspectionRunLog() : query); }
    @Override public List<MarkInspectionRunDetail> selectLogDetails(Long runLogId) { return runLogId == null ? Collections.emptyList() : mapper.selectDetailList(runLogId); }

    @Override public List<Map<String, Object>> platformOptions() {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> addedCodes = new HashSet<>();
        for (MarkPlatformMapping mapping : platformMappingService.selectList(new MarkPlatformMapping())) {
            if (StringUtils.isBlank(mapping.getTargetPlatformCode()) || !addedCodes.add(mapping.getTargetPlatformCode().toLowerCase())) continue;
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("platformCode", mapping.getTargetPlatformCode());
            option.put("platformName", mapping.getTargetPlatformName());
            option.put("sortNum", mapping.getSortNum());
            option.put("enabled", "0".equals(mapping.getSourceEnabled()) && "0".equals(mapping.getTargetEnabled()) && "0".equals(mapping.getMappingEnabled()));
            option.put("inspectionSupported", true);
            result.add(option);
        }
        return result;
    }

    @Override public void runDuePolicies() {
        for (MarkInspectionPolicy policy : mapper.selectDuePolicies(new Date())) {
            try {
                if (!orderService.hasPendingInspection(policy)) {
                    mapper.deferPolicy(policy.getId(), nextTime(policy));
                    continue;
                }
                runPolicy(policy.getId(), "SCHEDULED");
            } catch (Exception ignored) { }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> recoverInterruptedRuns() {
        Date now = new Date();
        int failedRuns = mapper.failInterruptedRuns(now, "服务重启时恢复未完成的巡检任务");
        int releasedPolicies = mapper.releaseInterruptedPolicies(now);
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("failedRuns", failedRuns);
        result.put("releasedPolicies", releasedPolicies);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> cleanupExpiredLogs(Date cutoffTime) {
        if (cutoffTime == null) throw new ServiceException("巡检日志截止时间不能为空");
        int details = mapper.deleteExpiredDetails(cutoffTime);
        int logs = mapper.deleteExpiredLogs(cutoffTime);
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("details", details);
        result.put("logs", logs);
        return result;
    }

    @Override public MarkInspectionRunLog runPolicy(Long id, String triggerType) {
        MarkInspectionPolicy policy = mapper.selectPolicyById(id);
        if (policy == null) throw new ServiceException("巡检策略不存在");
        if (!platformMappingService.isTargetEnabled(policy.getPlatformCode())) throw new ServiceException("平台已关闭，不能执行巡检");
        if (mapper.claimPolicy(id) == 0) throw new ServiceException("策略已停用或正在执行");
        policy = mapper.selectPolicyById(id);
        Date start = new Date();
        MarkInspectionRunLog log = new MarkInspectionRunLog();
        log.setRunNo("XJ" + System.currentTimeMillis()); log.setPolicyId(id); log.setPolicyName(policy.getPolicyName());
        log.setPlatformCode(policy.getPlatformCode()); log.setPlatformName(policy.getPlatformName());
        log.setTriggerType(StringUtils.defaultIfBlank(triggerType, "MANUAL")); log.setRunStatus("RUNNING"); log.setStartTime(start);
        mapper.insertLog(log);
        try {
            Map<String, Object> stats = orderService.processPendingInspection(policy, log.getId());
            log.setScannedCount(number(stats, "scannedCount")); log.setMatchedCount(number(stats, "matchedCount"));
            log.setCheckedCount(number(stats, "checkedCount")); log.setMarkedCount(number(stats, "markedCount"));
            log.setSuccessCount(number(stats, "successCount")); log.setFailedCount(number(stats, "failedCount"));
            log.setSkippedCount(number(stats, "skippedCount"));
            log.setRunStatus(log.getFailedCount() > 0 ? "PARTIAL" : "SUCCESS");
        } catch (Exception ex) {
            log.setScannedCount(0); log.setMatchedCount(0); log.setSuccessCount(0); log.setFailedCount(1); log.setSkippedCount(0);
            log.setCheckedCount(0); log.setMarkedCount(0);
            log.setRunStatus("FAILED"); log.setErrorMessage(StringUtils.substring(ex.getMessage(), 0, 500));
        } finally {
            Date end = new Date(); log.setEndTime(end); mapper.updateLog(log);
            mapper.releasePolicy(id, end, new Date(end.getTime() + policy.getIntervalSeconds() * 1000L));
        }
        return log;
    }

    private void normalize(MarkInspectionPolicy policy) {
        if (policy == null || StringUtils.isBlank(policy.getPolicyName())) throw new ServiceException("策略名称不能为空");
        if (StringUtils.isBlank(policy.getPlatformCode())) throw new ServiceException("处理平台不能为空");
        String platformCode = policy.getPlatformCode().trim().toLowerCase();
        MarkPlatformMapping mapping = platformMappingService.selectList(new MarkPlatformMapping()).stream()
                .filter(x -> platformCode.equalsIgnoreCase(x.getTargetPlatformCode())).findFirst()
                .orElseThrow(() -> new ServiceException("处理平台不存在或已删除"));
        policy.setPolicyName(policy.getPolicyName().trim()); policy.setPlatformCode(platformCode);
        policy.setPlatformName(mapping.getTargetPlatformName());
        String intervalUnit = "HOUR".equalsIgnoreCase(policy.getIntervalUnit()) ? "HOUR" : "MINUTE";
        int intervalValue = Math.max(1, Math.min(9999, policy.getIntervalValue() == null ? 30 : policy.getIntervalValue()));
        long intervalSeconds = (long) intervalValue * ("HOUR".equals(intervalUnit) ? 3600L : 60L);
        policy.setIntervalUnit(intervalUnit);
        policy.setIntervalValue(intervalValue);
        policy.setIntervalSeconds((int) Math.min(Integer.MAX_VALUE, intervalSeconds));
        policy.setBatchLimit(Math.max(1, Math.min(500, policy.getBatchLimit() == null ? 100 : policy.getBatchLimit())));
        policy.setAutoAction("FAILED".equalsIgnoreCase(policy.getAutoAction()) ? "FAILED" : "SUCCESS");
        String scope = StringUtils.upperCase(StringUtils.trimToEmpty(policy.resolvedOrderScope()));
        if (!Set.of("PENDING", "PROCESSING", "BOTH").contains(scope)) throw new ServiceException("检测订单范围无效");
        policy.setOrderScope(scope);
        policy.setAutoTakeover(policy.takeoverEnabled());
        if (MarkWumaPlatform.contains(platformCode) && !"PROCESSING".equals(scope) && !policy.takeoverEnabled()) {
            throw new ServiceException("无码巡检包含待处理订单时需开启自动接管，或选择仅处理中");
        }
        if ("td_gaopin".equals(platformCode) && (!"PENDING".equals(scope) || policy.takeoverEnabled())) {
            throw new ServiceException("泰迪高频使用专用待处理队列，仅支持待处理且无需自动接管");
        }
        if (!"0".equals(policy.getStatus()) && !"1".equals(policy.getStatus())) policy.setStatus("1");
    }
    private Date nextTime(MarkInspectionPolicy policy) { return new Date(System.currentTimeMillis() + policy.getIntervalSeconds() * 1000L); }
    private int number(Map<String, Object> values, String key) { Object value = values.get(key); return value instanceof Number n ? n.intValue() : 0; }
}
