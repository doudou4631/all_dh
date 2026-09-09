package com.geek.server.service;

import com.geek.server.domain.MarkInspectionPolicy;
import com.geek.server.domain.MarkInspectionRunDetail;
import com.geek.server.domain.MarkInspectionRunLog;
import java.util.List;
import java.util.Map;
import java.util.Date;

public interface IMarkInspectionService {
    List<MarkInspectionPolicy> selectPolicies(MarkInspectionPolicy query);
    MarkInspectionPolicy selectPolicy(Long id);
    int savePolicy(MarkInspectionPolicy policy);
    int updatePolicy(MarkInspectionPolicy policy);
    int deletePolicy(Long id);
    List<MarkInspectionRunLog> selectLogs(MarkInspectionRunLog query);
    List<MarkInspectionRunDetail> selectLogDetails(Long runLogId);
    List<Map<String, Object>> platformOptions();
    MarkInspectionRunLog runPolicy(Long id, String triggerType);
    void runDuePolicies();
    Map<String, Integer> recoverInterruptedRuns();
    Map<String, Integer> cleanupExpiredLogs(Date cutoffTime);
}
