package com.geek.server.service;

import com.geek.server.domain.vo.FreeSingleQueryRequest;

import com.geek.server.domain.UserApiQueryRecord;
import java.util.Map;
import java.util.List;

public interface IFreeQueryService {

    Map<String, Object> getQuota(String ip);
    int countEnabledPlatforms();

    Map<String, Object> singleQuery(FreeSingleQueryRequest request, String ip);

    List<UserApiQueryRecord> listIpLogs(String ip, String phone, String requestStatus, String taskId,
                                        String deviceId, String deviceSource, String queryType,
                                        String sourceType, String beginTime, String endTime);

    Map<String, Object> logDashboard(String ip, String phone, String requestStatus, String taskId,
                                     String deviceId, String deviceSource, String queryType,
                                     String sourceType, String beginTime, String endTime);
}

