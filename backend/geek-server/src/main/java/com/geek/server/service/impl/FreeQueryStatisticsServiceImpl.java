package com.geek.server.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.common.utils.DateUtils;
import com.geek.server.domain.FreeQueryRecord;
import com.geek.server.mapper.FreeQueryStatisticsMapper;
import com.geek.server.service.IFreeQueryStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FreeQueryStatisticsServiceImpl implements IFreeQueryStatisticsService {
    private static final int CLEANUP_BATCH_SIZE = 2000;

    @Autowired
    private FreeQueryStatisticsMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void record(String visitorKey, String ipAddr, String userAgent, String queryPhone,
                       Map<String, Object> queryResult, long responseTime, String errorMessage) {
        boolean success = queryResult != null && Boolean.TRUE.equals(queryResult.get("success"));
        int markedCount = intValue(queryResult == null ? null : queryResult.get("markedCount"));

        FreeQueryRecord record = new FreeQueryRecord();
        record.setVisitorKey(limit(visitorKey, 128));
        record.setIpAddr(limit(ipAddr, 64));
        record.setUserAgent(limit(userAgent, 500));
        record.setQueryPhone(limit(queryPhone, 32));
        record.setResultDetails(buildResultDetails(queryResult));
        record.setQueryStatus(success ? "0" : "1");
        record.setHasResult(success && markedCount > 0 ? "1" : "0");
        record.setPlatformTotal(intValue(queryResult == null ? null : queryResult.get("total")));
        record.setPlatformSuccess(intValue(queryResult == null ? null : queryResult.get("successfulPlatforms")));
        record.setMarkedCount(markedCount);
        record.setResponseTime(responseTime);
        record.setErrorMessage(limit(errorMessage, 500));
        record.setCreateTime(DateUtils.getNowDate());
        mapper.insertRecord(record);
    }

    @Override
    public int cleanupExpiredRecords(Date cutoffTime) {
        if (cutoffTime == null) {
            throw new IllegalArgumentException("cutoffTime must not be null");
        }

        int total = 0;
        int deleted;
        do {
            deleted = mapper.deleteExpiredRecords(cutoffTime);
            total += deleted;
        } while (deleted == CLEANUP_BATCH_SIZE);
        return total;
    }

    private String buildResultDetails(Map<String, Object> queryResult) {
        List<Map<String, Object>> details = new ArrayList<>();
        Object results = queryResult == null ? null : queryResult.get("results");
        if (results instanceof List<?> list) {
            for (Object value : list) {
                if (!(value instanceof Map<?, ?> item) || !Boolean.TRUE.equals(item.get("marked"))) {
                    continue;
                }
                Object resultText = item.get("resultText");
                String markType = String.valueOf(resultText == null ? "存在标记" : resultText);
                if (markType.regionMatches(true, 0, "yes-", 0, 4)) {
                    markType = markType.substring(4);
                }
                Map<String, Object> detail = new LinkedHashMap<>();
                Object platformName = item.get("platformName");
                detail.put("platformName", String.valueOf(platformName == null ? "未知平台" : platformName));
                detail.put("markType", markType);
                details.add(detail);
            }
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception ignored) {
            return "[]";
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> summary = new LinkedHashMap<>(mapper.selectSummary());
        long todayVisitors = longValue(summary.get("todayVisitors"));
        long newVisitorsToday = longValue(summary.get("newVisitorsToday"));
        summary.put("returningVisitorsToday", Math.max(0, todayVisitors - newVisitorsToday));
        summary.put("avgQueriesPerUser", decimal(summary.get("avgQueriesPerUser")));
        summary.put("successRate", decimal(summary.get("successRate")));
        summary.put("noResultRate", decimal(summary.get("noResultRate")));

        List<Map<String, Object>> trend = mapper.selectSevenDayTrend();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("trend", trend);
        result.put("generatedAt", DateUtils.getTime());
        return result;
    }

    private int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
