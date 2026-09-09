package com.geek.server.mapper;

import com.geek.server.domain.MarkInspectionPolicy;
import com.geek.server.domain.MarkInspectionRunDetail;
import com.geek.server.domain.MarkInspectionRunLog;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

public interface MarkInspectionMapper {
    List<MarkInspectionPolicy> selectPolicyList(MarkInspectionPolicy query);
    MarkInspectionPolicy selectPolicyById(Long id);
    int insertPolicy(MarkInspectionPolicy policy);
    int updatePolicy(MarkInspectionPolicy policy);
    int deletePolicy(Long id);
    List<MarkInspectionPolicy> selectDuePolicies(@Param("now") Date now);
    int claimPolicy(Long id);
    int deferPolicy(@Param("id") Long id, @Param("nextRunTime") Date nextRunTime);
    int releasePolicy(@Param("id") Long id, @Param("lastRunTime") Date lastRunTime, @Param("nextRunTime") Date nextRunTime);
    List<MarkInspectionRunLog> selectLogList(MarkInspectionRunLog query);
    List<MarkInspectionRunDetail> selectDetailList(@Param("runLogId") Long runLogId);
    int insertDetail(MarkInspectionRunDetail detail);
    int insertLog(MarkInspectionRunLog log);
    int updateLog(MarkInspectionRunLog log);
    int failInterruptedRuns(@Param("endTime") Date endTime, @Param("errorMessage") String errorMessage);
    int releaseInterruptedPolicies(@Param("nextRunTime") Date nextRunTime);
    int deleteExpiredDetails(@Param("cutoffTime") Date cutoffTime);
    int deleteExpiredLogs(@Param("cutoffTime") Date cutoffTime);
}
