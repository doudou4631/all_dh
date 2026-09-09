package com.geek.server.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.common.utils.http.HttpUtils;
import com.geek.common.utils.sign.Md5Utils;
import com.geek.server.domain.UserAggregateConfig;
import com.geek.server.domain.UserPlatformUrlConfig;
import com.geek.server.service.IFreeQueryService;
import com.geek.server.service.IUserAggregateConfigService;
import com.geek.server.service.IUserPlatformUrlConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class FreeQueryServiceImpl implements IFreeQueryService {
    private static final List<String> RESULT_FIELDS = List.of(
            "xiaomi", "taidixiong", "baidu", "dianhuabang", "sghmt",
            "sanliuling", "yidonggaopin", "ltgj", "tengxun", "platform", "status");

    @Autowired
    private IUserPlatformUrlConfigService platformService;

    @Autowired
    private IUserAggregateConfigService aggregateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("batchOptimizedItemExecutor")
    private Executor executor;

    @Override
    public Map<String, Object> query(String phoneNumber) {
        UserPlatformUrlConfig query = new UserPlatformUrlConfig();
        query.setStatus("0");
        query.setFreeQueryStatus("0");
        List<UserPlatformUrlConfig> platforms = platformService.selectUserPlatformUrlConfigList(query);
        if (platforms == null || platforms.isEmpty()) {
            return Map.of("success", false, "message", "暂无可用查询平台");
        }

        UserAggregateConfig aggregate = aggregateService.selectUserAggregateConfigById(1L);
        List<CompletableFuture<Map<String, Object>>> futures = platforms.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> queryPlatform(item, aggregate, phoneNumber), executor))
                .toList();

        List<Map<String, Object>> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        long markedCount = results.stream().filter(item -> Boolean.TRUE.equals(item.get("marked"))).count();
        long successfulPlatforms = results.stream()
                .filter(item -> Boolean.TRUE.equals(item.get("requestSuccess")))
                .count();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", successfulPlatforms > 0);
        if (successfulPlatforms == 0) {
            response.put("message", "所有查询平台均请求失败");
        }
        response.put("phoneNumber", phoneNumber);
        response.put("total", results.size());
        response.put("successfulPlatforms", successfulPlatforms);
        response.put("failedPlatforms", results.size() - successfulPlatforms);
        response.put("markedCount", markedCount);
        response.put("results", results);
        return response;
    }

    private Map<String, Object> queryPlatform(UserPlatformUrlConfig platform,
                                               UserAggregateConfig aggregate,
                                               String phoneNumber) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("platformId", platform.getPlatformId());
        result.put("platformName", platform.getPlatformName());
        result.put("platformIcon", platform.getFreeQueryIcon());
        result.put("marked", false);
        result.put("requestSuccess", false);
        result.put("resultText", "暂无标记");

        try {
            if (BiaojichaxunApiClient.supports(platform.getPlatformKey(), platform.getPlatformName())) {
                String response = BiaojichaxunApiClient.query(platform.getPlatformKey(), platform.getPlatformName(), phoneNumber);
                Map<String, Object> payload = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
                Map<?, ?> data = payload.get("data") instanceof Map<?, ?> m ? m : Map.of();
                boolean marked = Boolean.TRUE.equals(data.get("marked"));
                result.put("requestSuccess", Boolean.TRUE.equals(data.get("success")));
                result.put("marked", marked);
                Object message = data.get("message");
                result.put("resultText", String.valueOf(message != null ? message : (marked ? "存在标记" : "未查询到标记")));
                if (marked) result.put("markType", result.get("resultText"));
                return result;
            }
            result.put("error", "平台未接入统一查询 API");
            result.put("resultText", "查询失败");
            return result;
            /*
            Map<String, Object> params = new HashMap<>();
            params.put("phone", phoneNumber);
            params.put("sj", phoneNumber);
            if (aggregate != null) {
                params.put("uid", aggregate.getSid());
                params.put("sign", Md5Utils.encryptMd5(aggregate.getSid() + phoneNumber + aggregate.getsKey()));
            }

            Map<String, String> headers = parseHeaders(platform.getHeadersTemplate());
            String response = headers.isEmpty()
                    ? HttpUtils.postCall(platform.getUrl(), params)
                    : HttpUtils.postJson(platform.getUrl(), params, headers);
            String text = extractResultText(response);
            boolean marked = isMarked(text);
            result.put("requestSuccess", true);
            result.put("marked", marked);
            result.put("resultText", StringUtils.defaultIfBlank(text, marked ? "存在标记" : "暂无标记"));
            if (marked) {
                result.put("markType", text);
            }
            */
        } catch (Exception ex) {
            result.put("error", "平台请求失败");
            result.put("resultText", "查询失败");
        }
        return result;
    }

    private Map<String, String> parseHeaders(String json) {
        if (StringUtils.isBlank(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() { });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String extractResultText(String response) {
        if (StringUtils.isBlank(response)) {
            return "";
        }
        try {
            Map<String, Object> root = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() { });
            Object data = root.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                return extractFromMap(dataMap);
            }
            Object results = root.get("results");
            if (results instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> item) {
                return extractFromMap(item);
            }
            return extractFromMap(root);
        } catch (Exception ignored) {
            return response.length() > 200 ? response.substring(0, 200) : response;
        }
    }

    private String extractFromMap(Map<?, ?> values) {
        for (String field : RESULT_FIELDS) {
            Object value = values.get(field);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (!"phone".equalsIgnoreCase(key) && value != null
                    && !(value instanceof Map<?, ?>) && !(value instanceof List<?>)) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    static boolean isMarked(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        String normalized = text.trim().toLowerCase();
        return !(normalized.equals("no") || normalized.startsWith("no-")
                || normalized.equals("none") || normalized.equals("null")
                || normalized.contains("标记已取消")
                || normalized.contains("未标记") || normalized.contains("无标记")
                || normalized.contains("正常") || normalized.contains("健康"));
    }
}
