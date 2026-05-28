package com.geek.server.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.common.utils.http.HttpUtils;
import com.geek.common.utils.sign.Md5Utils;
import com.geek.server.domain.UserApiQueryRecord;
import com.geek.server.domain.dto.OptimizedBatchItemOutcome;
import com.geek.server.domain.entity.BatchTask;
import com.geek.server.domain.vo.ApiRequestVO;
import com.geek.server.domain.vo.OptimizedBatchSession;
import com.geek.server.domain.vo.PreActionConfig;
import com.geek.server.domain.vo.UserInfo360VO;
import com.geek.server.service.IOptimizedBatchApiExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optimized-batch HTTP: same branching as ApiServiceImpl.single; does not call single; no DB; no points; no sleep.
 */
@Service
public class OptimizedBatchApiExecutor implements IOptimizedBatchApiExecutor {

    private static final Logger log = LoggerFactory.getLogger(OptimizedBatchApiExecutor.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OptimizedBatchItemOutcome execute(ApiRequestVO apiRequestVO, OptimizedBatchSession session, String taskId) {
        if (apiRequestVO == null) {
            return failOutcome(null, null, null, "request is null", 0L);
        }
        if (apiRequestVO.getUrl() == null || apiRequestVO.getPhoneNumber() == null || apiRequestVO.getPreActionType() == null) {
            return failOutcome(apiRequestVO, session, taskId, "missing required parameters", 0L);
        }

        Long currentUserId = session.getUserId();
        String currentUserName = session.getUsername();

        UserApiQueryRecord apiQueryRecord = new UserApiQueryRecord();
        apiQueryRecord.setQueryType(apiRequestVO.getQueryType() != null ? apiRequestVO.getQueryType() : "1");
        apiQueryRecord.setPlatformId(apiRequestVO.getPlatformId() != null ? Long.valueOf(apiRequestVO.getPlatformId()) : null);
        apiQueryRecord.setPlatformName(apiRequestVO.getPlatformName());
        apiQueryRecord.setUserId(currentUserId);
        apiQueryRecord.setPhone(apiRequestVO.getPhoneNumber());
        apiQueryRecord.setCreateBy(currentUserName);
        apiQueryRecord.setCreateTime(new Date());
        apiQueryRecord.setTaskId(taskId);

        long startTime = System.currentTimeMillis();
        Map<String, Object> requestParams = new HashMap<>();
        String phoneNumber = apiRequestVO.getPhoneNumber();
        requestParams.put("phone", phoneNumber);

        Map<String, String> headers = new HashMap<>();

        if (apiRequestVO.getPreActionType() == 1) {
            Map<String, Object> tokenResult = executePreAction(apiRequestVO.getPreActionConfig());
            if (!Boolean.TRUE.equals(tokenResult.get("success"))) {
                return failOutcomeEarly(apiRequestVO, tokenResult, startTime);
            }
            headers.put("Authorization", "" + tokenResult.get("token"));
            requestParams.put("platforms", "4");
        } else {
            if (session.getAggregateConfig() == null) {
                return failOutcome(apiRequestVO, session, taskId, "aggregate config not found", 0L);
            }
            String skey = session.getAggregateConfig().getsKey();
            String sid = session.getAggregateConfig().getSid();
            String sign;
            try {
                sign = Md5Utils.encryptMd5(sid + phoneNumber + skey);
            } catch (UnsupportedEncodingException e) {
                return failOutcome(apiRequestVO, session, taskId, "sign error: " + e.getMessage(), 0L);
            }
            requestParams.put("sj", phoneNumber);
            requestParams.put("uid", sid);
            requestParams.put("sign", sign);
        }

        if (apiRequestVO.getHeadersTemplate() != null && !apiRequestVO.getHeadersTemplate().isEmpty()) {
            try {
                Map<String, String> templateHeaders = objectMapper.readValue(apiRequestVO.getHeadersTemplate(), Map.class);
                headers.putAll(templateHeaders);
            } catch (Exception e) {
                log.warn("parse headers template failed: {}", e.getMessage());
            }
        }
        apiQueryRecord.setRequestParams(requestParams.toString());

        String response;
        try {
            if (!headers.isEmpty()) {
                response = HttpUtils.postJson(apiRequestVO.getUrl(), requestParams, headers);
            } else {
                response = HttpUtils.postCall(apiRequestVO.getUrl(), requestParams);
            }
        } catch (Exception e) {
            log.warn("HTTP error: {}", e.getMessage());
            apiQueryRecord.setRequestStatus("1");
            apiQueryRecord.setRequestTime(0L);
            apiQueryRecord.setResponseResult("HTTP request error: " + e.getMessage());
            long rt = System.currentTimeMillis() - startTime;
            return OptimizedBatchItemOutcome.builder()
                    .queryRecord(apiQueryRecord)
                    .apiResult(buildApiResult(apiRequestVO, false, null, "request error: " + e.getMessage(), rt))
                    .needPointDeduction(false)
                    .build();
        }

        long requestTime = System.currentTimeMillis() - startTime;
        apiQueryRecord.setRequestTime(requestTime);

        if (response == null) {
            apiQueryRecord.setRequestStatus("1");
            apiQueryRecord.setRequestTime(0L);
            apiQueryRecord.setResponseResult("empty response");
            return OptimizedBatchItemOutcome.builder()
                    .queryRecord(apiQueryRecord)
                    .apiResult(buildApiResult(apiRequestVO, false, null, "empty response", requestTime))
                    .needPointDeduction(false)
                    .build();
        }

        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);

            if (responseMap.containsKey("data")) {
                Object dataObj = responseMap.get("data");
                if (dataObj instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                    Map<String, Object> result = parseInnerResponse(dataMap, "default");
                    fillQueryRecordFromResult(apiQueryRecord, result, response);
                    boolean ok = Boolean.TRUE.equals(result.get("success"));
                    if (ok) {
                        log.info("opt-batch ok userId={} platform={}", currentUserId, apiRequestVO.getPlatformName());
                    } else {
                        log.warn("opt-batch fail userId={} platform={} msg={}", currentUserId, apiRequestVO.getPlatformName(),
                                result.getOrDefault("message", "error"));
                    }
                    return OptimizedBatchItemOutcome.builder()
                            .queryRecord(apiQueryRecord)
                            .apiResult(buildApiResultFromMap(apiRequestVO, result, requestTime))
                            .needPointDeduction(ok)
                            .build();
                }
                apiQueryRecord.setRequestStatus("1");
                apiQueryRecord.setRequestTime(0L);
                apiQueryRecord.setResponseResult("data is not a map");
                return outcomeForRecord(apiRequestVO, apiQueryRecord, false, null, "data is not a map", requestTime);
            }

            if (responseMap.containsKey("results")) {
                String outerStatus = (String) responseMap.get("status");
                if ("success".equals(outerStatus)) {
                    Object resultsObj = responseMap.get("results");
                    if (resultsObj instanceof List) {
                        List<Object> resultsList = (List<Object>) resultsObj;
                        if (!resultsList.isEmpty()) {
                            Object firstResult = resultsList.get(0);
                            if (firstResult instanceof Map) {
                                Map<String, Object> firstResultMap = (Map<String, Object>) firstResult;
                                Map<String, Object> result = parseInnerResponse(firstResultMap, "default");
                                fillQueryRecordFromResult(apiQueryRecord, result, response);
                                boolean ok = Boolean.TRUE.equals(result.get("success"));
                                if (ok) {
                                    log.info("opt-batch ok userId={} platform={}", currentUserId, apiRequestVO.getPlatformName());
                                } else {
                                    log.warn("opt-batch fail userId={} platform={}", currentUserId, apiRequestVO.getPlatformName());
                                }
                                return OptimizedBatchItemOutcome.builder()
                                        .queryRecord(apiQueryRecord)
                                        .apiResult(buildApiResultFromMap(apiRequestVO, result, requestTime))
                                        .needPointDeduction(ok)
                                        .build();
                            }
                            apiQueryRecord.setRequestStatus("1");
                            apiQueryRecord.setRequestTime(0L);
                            apiQueryRecord.setResponseResult("first results element is not a map");
                            return outcomeForRecord(apiRequestVO, apiQueryRecord, false, null,
                                    "first results element is not a map", requestTime);
                        }
                        apiQueryRecord.setRequestStatus("1");
                        apiQueryRecord.setRequestTime(0L);
                        apiQueryRecord.setResponseResult("results list empty");
                        return outcomeForRecord(apiRequestVO, apiQueryRecord, false, null, "results list empty", requestTime);
                    }
                    apiQueryRecord.setRequestStatus("1");
                    apiQueryRecord.setRequestTime(0L);
                    apiQueryRecord.setResponseResult("results is not a list");
                    return outcomeForRecord(apiRequestVO, apiQueryRecord, false, null, "results is not a list", requestTime);
                }
                apiQueryRecord.setRequestStatus("1");
                apiQueryRecord.setRequestTime(0L);
                apiQueryRecord.setResponseResult("outer status not success: " + outerStatus);
                return outcomeForRecord(apiRequestVO, apiQueryRecord, false, null,
                        "outer status not success: " + outerStatus, requestTime);
            }

            Map<String, Object> result = parseInnerResponse(responseMap, "default");
            fillQueryRecordFromResult(apiQueryRecord, result, response);
            boolean ok = Boolean.TRUE.equals(result.get("success"));
            if (ok) {
                log.info("opt-batch ok userId={} platform={}", currentUserId, apiRequestVO.getPlatformName());
            } else {
                log.warn("opt-batch fail userId={} platform={}", currentUserId, apiRequestVO.getPlatformName());
            }
            return OptimizedBatchItemOutcome.builder()
                    .queryRecord(apiQueryRecord)
                    .apiResult(buildApiResultFromMap(apiRequestVO, result, requestTime))
                    .needPointDeduction(ok)
                    .build();

        } catch (JsonProcessingException e) {
            apiQueryRecord.setRequestStatus("1");
            apiQueryRecord.setRequestTime(0L);
            apiQueryRecord.setResponseResult("json parse error: " + e.getMessage());
            return outcomeForRecord(apiRequestVO, apiQueryRecord, false, null, "json parse error: " + e.getMessage(), requestTime);
        } catch (Exception e) {
            apiQueryRecord.setRequestStatus("1");
            apiQueryRecord.setRequestTime(0L);
            apiQueryRecord.setResponseResult("parse error: " + e.getMessage());
            return outcomeForRecord(apiRequestVO, apiQueryRecord, false, null, "parse error: " + e.getMessage(), requestTime);
        }
    }

    private static OptimizedBatchItemOutcome outcomeForRecord(ApiRequestVO req, UserApiQueryRecord rec,
                                                              boolean success, Map<String, Object> data, String err, long rt) {
        return OptimizedBatchItemOutcome.builder()
                .queryRecord(rec)
                .apiResult(buildApiResult(req, success, data, err, rt))
                .needPointDeduction(success)
                .build();
    }

    private OptimizedBatchItemOutcome failOutcome(ApiRequestVO req, OptimizedBatchSession session, String taskId, String msg, long rt) {
        if (req == null) {
            return OptimizedBatchItemOutcome.builder()
                    .queryRecord(null)
                    .apiResult(BatchTask.ApiResult.builder()
                            .success(false)
                            .error(msg)
                            .responseTime(rt)
                            .timestamp(LocalDateTime.now())
                            .build())
                    .needPointDeduction(false)
                    .build();
        }
        return OptimizedBatchItemOutcome.builder()
                .queryRecord(null)
                .apiResult(buildApiResult(req, false, null, msg, rt))
                .needPointDeduction(false)
                .build();
    }

    private OptimizedBatchItemOutcome failOutcomeEarly(ApiRequestVO req, Map<String, Object> tokenResult, long startTime) {
        long rt = System.currentTimeMillis() - startTime;
        String msg = tokenResult.get("message") != null ? String.valueOf(tokenResult.get("message")) : "pre-action failed";
        return OptimizedBatchItemOutcome.builder()
                .queryRecord(null)
                .apiResult(buildApiResult(req, false, null, msg, rt))
                .needPointDeduction(false)
                .build();
    }

    private static BatchTask.ApiResult buildApiResult(ApiRequestVO request, boolean successFlag, Object data, String error, long responseTime) {
        return BatchTask.ApiResult.builder()
                .phoneNumber(request.getPhoneNumber())
                .platformId(request.getPlatformId())
                .platformName(request.getPlatformName())
                .success(successFlag)
                .data(data)
                .error(error)
                .responseTime(responseTime)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private static BatchTask.ApiResult buildApiResultFromMap(ApiRequestVO request, Map<String, Object> result, long responseTime) {
        boolean successFlag = Boolean.TRUE.equals(result.get("success"));
        Object data = successFlag ? result : null;
        String error = successFlag ? null : (String) result.get("message");
        return buildApiResult(request, successFlag, data, error, responseTime);
    }

    private void fillQueryRecordFromResult(UserApiQueryRecord apiQueryRecord, Map<String, Object> result, String response) {
        try {
            if (Boolean.TRUE.equals(result.get("success"))) {
                apiQueryRecord.setRequestStatus("0");
            } else {
                apiQueryRecord.setRequestStatus("1");
            }
            if (result != null) {
                apiQueryRecord.setResponseResult(result.toString());
                if (result.containsKey("platformResults")) {
                    Object platformResultsObj = result.get("platformResults");
                    if (platformResultsObj instanceof List) {
                        List<Map<String, Object>> platformResults = (List<Map<String, Object>>) platformResultsObj;
                        if (!platformResults.isEmpty()) {
                            Map<String, Object> firstPlatform = platformResults.get(0);
                            if (firstPlatform.containsKey("status")) {
                                apiQueryRecord.setResults((String) firstPlatform.get("status"));
                            }
                        }
                    }
                }
            } else {
                apiQueryRecord.setResponseResult(response);
            }
        } catch (Exception e) {
            log.warn("fill query record failed: {}", e.getMessage());
        }
    }

    private Map<String, Object> parseInnerResponse(Map<String, Object> dataMap, String platformName) {
        String phone = null;
        List<Map<String, Object>> platformResults = new ArrayList<>();

        if (dataMap.containsKey("phone")) {
            phone = String.valueOf(dataMap.get("phone"));
        }

        String[] platformFields = {"xiaomi", "taidixiong", "baidu", "dianhuabang", "sghmt",
                "sanliuling", "yidonggaopin", "ltgj", "tengxun"};

        if (dataMap.containsKey("platform")) {
            Map<String, Object> result = new HashMap<>(2);
            result.put("platform", String.valueOf(dataMap.get("platform")));
            result.put("status", String.valueOf(dataMap.get("status")));
            platformResults.add(result);
        }

        for (String field : platformFields) {
            if ("platform".equals(field)) {
                continue;
            }
            if (dataMap.containsKey(field)) {
                Map<String, Object> result = new HashMap<>(2);
                result.put("platform", field);
                Object value = dataMap.get(field);
                result.put("status", value != null ? String.valueOf(value) : null);
                platformResults.add(result);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("phone", phone);
        result.put("platformResults", platformResults);
        return result;
    }

    private Map<String, Object> executePreAction(String preActionConfig) {
        if (preActionConfig == null || preActionConfig.isEmpty()) {
            return Map.of("success", false, "message", "pre-action config empty");
        }
        try {
            PreActionConfig config = objectMapper.readValue(preActionConfig, PreActionConfig.class);
            String url = config.getUrl();
            Map<String, Object> params = config.getParams();
            if (url == null || url.isEmpty()) {
                return Map.of("success", false, "message", "pre-action url empty");
            }
            String response;
            try {
                UserInfo360VO userInfo360VO = objectMapper.convertValue(params, UserInfo360VO.class);
                response = HttpUtils.postJson(url, userInfo360VO);
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null) {
                    return Map.of("success", false, "message", "request error");
                }
                return Map.of("success", false, "message", "pre-action request error: " + errorMessage);
            }

            if (response != null) {
                Map<?, ?> responseMap = objectMapper.readValue(response, Map.class);
                Object token = responseMap.get("token");
                if (token != null) {
                    return Map.of("success", true, "token", token);
                }
                Object errorMsg = responseMap.get("message") != null ? responseMap.get("message") :
                        responseMap.get("error") != null ? responseMap.get("error") :
                                responseMap.get("msg");
                if (errorMsg != null) {
                    return Map.of("success", false, "message", "get token failed: " + errorMsg);
                }
                return Map.of("success", false, "message", "token not found in response");
            }
            return Map.of("success", false, "message", "empty token response");
        } catch (Exception e) {
            log.warn("pre-action failed: {}", e.getMessage());
            return Map.of("success", false, "message", "pre-action failed: " + e.getMessage());
        }
    }
}
