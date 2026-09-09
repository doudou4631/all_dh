package com.geek.server.service;

import java.util.Date;
import java.util.Map;

public interface IFreeQueryStatisticsService {
    void record(String visitorKey, String ipAddr, String userAgent, String queryPhone,
                Map<String, Object> queryResult, long responseTime, String errorMessage);

    int cleanupExpiredRecords(Date cutoffTime);

    Map<String, Object> getStatistics();
}
