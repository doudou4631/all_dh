package com.geek.server.service.impl;

import com.geek.common.constant.CacheConstants;
import com.geek.common.core.domain.entity.SysDictData;
import com.geek.common.utils.CacheUtils;
import com.geek.common.utils.StringUtils;
import com.geek.server.domain.UserApiQueryRecord;
import com.geek.server.domain.dto.OptimizedBatchItemOutcome;
import com.geek.server.domain.entity.BatchTask;
import com.geek.server.domain.vo.ApiRequestVO;
import com.geek.server.domain.vo.FreeSingleQueryRequest;
import com.geek.server.domain.vo.OptimizedBatchSession;
import com.geek.server.service.IFreeQueryService;
import com.geek.server.service.IOptimizedBatchApiExecutor;
import com.geek.server.service.IUserAggregateConfigService;
import com.geek.server.service.IUserPlatformUrlConfigService;
import com.geek.server.service.IUserApiQueryRecordService;
import com.geek.system.service.ISysDictTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FreeQueryServiceImpl implements IFreeQueryService {

    private static final String DICT_TYPE_FREE_QUERY_CONFIG = "free_query_config";
    private static final String DICT_KEY_DAILY_LIMIT = "daily_limit";
    private static final String DICT_KEY_OVER_LIMIT_MSG = "over_limit_msg";
    private static final String DICT_KEY_DAILY_ALL_LIMIT = "daily_all_limit";
    private static final String DICT_KEY_DAILY_DEVICE_LIMIT = "daily_device_limit";
    private static final String DICT_KEY_DEVICE_OVER_LIMIT_MSG = "device_over_limit_msg";
    private static final String DICT_KEY_REQUIRE_DEVICE_ID = "require_device_id";
    private static final String DICT_KEY_REQUIRE_DEVICE_ID_MSG = "require_device_id_msg";
    /** \u514d\u8d39\u67e5\u8be2\u7981\u7528\u7684\u5e73\u53f0\u540d\u79f0\uff08\u82f1\u6587\u9017\u53f7\u5206\u9694\uff0c\u4e0e UserPlatformUrlConfig.platformName \u4e00\u81f4\uff09\uff1b\u672a\u914d\u7f6e\u5b57\u5178\u9879\u65f6\u9ed8\u8ba4\u7981\u7528\u8054\u901a\u7ba1\u5bb6 */
    private static final String DICT_KEY_DISABLED_PLATFORMS = "disabled_platforms";
    /** \u5b57\u5178\u672a\u914d\u7f6e disabled_platforms \u65f6\u7684\u9ed8\u8ba4\u7981\u7528\u5217\u8868 */
    private static final String DEFAULT_FREE_QUERY_DISABLED_PLATFORMS = "\u8054\u901a\u7ba1\u5bb6";
    /** \u5e73\u53f0\u5f53\u65e5\u514d\u8d39\u989d\u5ea6\u5df2\u7528\u5b8c\uff0c\u8bf7\u8054\u7cfb\u5ba2\u670d */
    private static final String ALL_LIMIT_MSG =
            "\u5e73\u53f0\u5f53\u65e5\u514d\u8d39\u989d\u5ea6\u5df2\u7528\u5b8c\uff0c\u8bf7\u8054\u7cfb\u5ba2\u670d";
    private static final String DEFAULT_OVER_LIMIT_MSG =
            "\u5f53\u524dIP\u4eca\u65e5\u514d\u8d39\u67e5\u8be2\u6b21\u6570\u5df2\u8fbe\u4e0a\u9650\uff0c\u8bf7\u6dfb\u52a0\u5ba2\u670d\u5fae\u4fe1\u67e5\u8be2\u3002";
    private static final String DEFAULT_DEVICE_OVER_LIMIT_MSG =
            "\u5f53\u524d\u8bbe\u5907\u4eca\u65e5\u514d\u8d39\u67e5\u8be2\u6b21\u6570\u5df2\u8fbe\u4e0a\u9650\uff0c\u8bf7\u660e\u65e5\u518d\u8bd5\u3002";
    private static final String DEFAULT_REQUIRE_DEVICE_ID_MSG =
            "\u5f53\u524d\u8bbe\u5907\u6807\u8bc6\u7f3a\u5931\uff0c\u8bf7\u5237\u65b0\u9875\u9762\u540e\u91cd\u8bd5\u3002";
    private static final boolean DEFAULT_REQUIRE_DEVICE_ID = false;
    private static final int DEFAULT_DAILY_LIMIT = 20;
    private static final int DEFAULT_DAILY_ALL_LIMIT = 2000;
    private static final int MAX_DEVICE_ID_LENGTH = 128;
    /** \u672a\u914d\u7f6e daily_all_limit \u6216\u975e\u6b63\u6570\u65f6\u4e0d\u9650\u5236\u5168\u5c40\u6b21\u6570 */
    private static final String FREE_LOG_PLATFORM_NAME = "FREE_QUERY";
    private static final DateTimeFormatter DATE_KEY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ConcurrentHashMap<String, Object> COUNTER_LOCKS = new ConcurrentHashMap<>();
    private static final Object GLOBAL_FREE_QUERY_LOCK = new Object();
    private static final Long FREE_QUERY_USER_ID = 0L;

    private final ISysDictTypeService dictTypeService;
    private final IUserPlatformUrlConfigService userPlatformUrlConfigService;
    private final IUserAggregateConfigService userAggregateConfigService;
    private final IOptimizedBatchApiExecutor optimizedBatchApiExecutor;
    private final IUserApiQueryRecordService userApiQueryRecordService;

    @Override
    public Map<String, Object> getQuota(String ip) {
        DictConfig config = loadDictConfig();
        QuotaCounter counter = currentCounter(ip, config.dailyLimit());
        Map<String, Object> quota = buildQuota(ip, config.dailyLimit(), counter.used());
        quota.put("deviceLimit", config.dailyDeviceLimit());
        quota.put("deviceRequired", config.requireDeviceId());
        quota.put("disabledPlatforms", loadFreeQueryDisabledPlatformNames());
        if (config.dailyAllLimit() < DEFAULT_DAILY_ALL_LIMIT) {
            int allUsed = getCounter(buildGlobalDailyCounterKey());
            int cap = config.dailyAllLimit();
            quota.put("allLimit", cap);
            quota.put("allUsed", allUsed);
            quota.put("allRemaining", Math.max(0, cap - allUsed));
        }
        return quota;
    }

    @Override
    public Map<String, Object> singleQuery(FreeSingleQueryRequest request, String ip) {
        DictConfig config = loadDictConfig();
        String phone = normalizePhone(request != null ? request.getPhone() : null);
        NormalizedDeviceId normalizedDeviceId = normalizeDeviceId(request != null ? request.getDeviceId() : null, ip);
        String deviceId = normalizedDeviceId.value();
        validatePhone(phone);
        List<String> disabledPlatformNames = loadFreeQueryDisabledPlatformNames();
        if (normalizedDeviceId.usedIpFallback()) {
            log.warn("free_query missing client deviceId, fallback to ip-based deviceId. ip={}, phone={}, deviceId={}",
                    ip, phone, deviceId);
        }
        if (config.requireDeviceId() && normalizedDeviceId.usedIpFallback()) {
            QuotaCounter deviceCounter = currentDeviceCounter(deviceId, config.dailyDeviceLimit());
            saveIpLog(ip, phone, "1", 0L, config.dailyDeviceLimit(), deviceCounter.used(), deviceCounter.used(),
                    config.requireDeviceIdMsg(), deviceId, null, normalizedDeviceId.deviceSource());
            Map<String, Object> fail = new HashMap<>();
            fail.put("code", 42903);
            fail.put("message", config.requireDeviceIdMsg());
            fail.put("quota", withDisabledPlatformsQuota(ip, config.dailyLimit(),
                    currentCounter(ip, config.dailyLimit()).used(), disabledPlatformNames, config));
            return fail;
        }

        CounterDecision globalDecision = reserveGlobalSlotIfNeeded(config.dailyAllLimit());
        if (!globalDecision.allowed()) {
            saveIpLog(ip, phone, "1", 0L, config.dailyAllLimit(), globalDecision.usedBefore(),
                    globalDecision.usedAfter(), ALL_LIMIT_MSG, deviceId, null, normalizedDeviceId.deviceSource());
            Map<String, Object> fail = new HashMap<>();
            fail.put("code", 42902);
            fail.put("message", ALL_LIMIT_MSG);
            fail.put("quota", withDisabledPlatformsQuota(ip, config.dailyLimit(),
                    currentCounter(ip, config.dailyLimit()).used(), disabledPlatformNames, config));
            return fail;
        }

        CounterDecision decision = increaseIfAllowed(ip, config.dailyLimit());
        if (!decision.allowed()) {
            rollbackGlobalSlotIfNeeded(config.dailyAllLimit());
            saveIpLog(ip, phone, "1", 0L, config.dailyLimit(), decision.usedBefore(), decision.usedAfter(),
                    config.overLimitMsg(), deviceId, null, normalizedDeviceId.deviceSource());
            Map<String, Object> fail = new HashMap<>();
            fail.put("code", 42901);
            fail.put("message", config.overLimitMsg());
            fail.put("quota", withDisabledPlatformsQuota(ip, config.dailyLimit(), decision.usedAfter(), disabledPlatformNames, config));
            return fail;
        }

        CounterDecision deviceDecision = increaseDeviceIfAllowed(deviceId, config.dailyDeviceLimit());
        if (!deviceDecision.allowed()) {
            rollbackIpOne(ip);
            rollbackGlobalSlotIfNeeded(config.dailyAllLimit());
            saveIpLog(ip, phone, "1", 0L, config.dailyDeviceLimit(), deviceDecision.usedBefore(), deviceDecision.usedAfter(),
                    config.deviceOverLimitMsg(), deviceId, null, normalizedDeviceId.deviceSource());
            Map<String, Object> fail = new HashMap<>();
            fail.put("code", 42901);
            fail.put("message", config.deviceOverLimitMsg());
            fail.put("quota", withDisabledPlatformsQuota(ip, config.dailyLimit(),
                    currentCounter(ip, config.dailyLimit()).used(), disabledPlatformNames, config));
            return fail;
        }

        long start = System.currentTimeMillis();
        String taskId = UUID.randomUUID().toString().replace("-", "");
        List<BatchTask.ApiResult> results = new ArrayList<>();

        List<com.geek.server.domain.UserPlatformUrlConfig> platforms = userPlatformUrlConfigService
                .selectUserPlatformUrlConfigList(new com.geek.server.domain.UserPlatformUrlConfig());
        Set<String> disabledNameSet = new HashSet<>(disabledPlatformNames);
        List<com.geek.server.domain.UserPlatformUrlConfig> enabledPlatforms = platforms.stream()
                .filter(p -> "0".equals(p.getStatus()))
                .filter(p -> {
                    String name = p.getPlatformName();
                    if (StringUtils.isEmpty(name)) {
                        return true;
                    }
                    return !disabledNameSet.contains(name.trim());
                })
                .toList();
        if (enabledPlatforms.isEmpty()) {
            saveIpLog(ip, phone, "1", 0L, config.dailyLimit(), decision.usedBefore(), decision.usedAfter(),
                    "\u6682\u65e0\u53ef\u7528\u5e73\u53f0", deviceId, taskId, normalizedDeviceId.deviceSource());
            Map<String, Object> fail = new HashMap<>();
            fail.put("code", 500);
            fail.put("message", "\u6682\u65e0\u53ef\u7528\u5e73\u53f0");
            fail.put("quota", withDisabledPlatformsQuota(ip, config.dailyLimit(), decision.usedAfter(), disabledPlatformNames, config));
            return fail;
        }

        OptimizedBatchSession session = new OptimizedBatchSession(
                FREE_QUERY_USER_ID,
                "free-ip-" + ip,
                userAggregateConfigService.selectUserAggregateConfigById(1L)
        );

        List<UserApiQueryRecord> records = new ArrayList<>();
        for (com.geek.server.domain.UserPlatformUrlConfig platform : enabledPlatforms) {
            ApiRequestVO req = buildApiRequest(platform, phone);
            OptimizedBatchItemOutcome outcome = optimizedBatchApiExecutor.execute(req, session, taskId);
            if (outcome.getApiResult() != null) {
                results.add(outcome.getApiResult());
            }
            if (outcome.getQueryRecord() != null) {
                UserApiQueryRecord row = outcome.getQueryRecord();
                row.setTaskId(taskId);
                row.setCreateBy("free-ip-" + ip);
                row.setUserId(FREE_QUERY_USER_ID);
                row.setRequestParams(appendIpToRequestParams(row.getRequestParams(), ip));
                records.add(row);
            }
        }

        for (UserApiQueryRecord record : records) {
            userApiQueryRecordService.insertUserApiQueryRecord(record);
        }

        long costMs = System.currentTimeMillis() - start;
        saveIpLog(ip, phone, "0", costMs, config.dailyLimit(), decision.usedBefore(), decision.usedAfter(),
                "\u67e5\u8be2\u6210\u529f", deviceId, taskId, normalizedDeviceId.deviceSource());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", taskId);
        data.put("results", results);
        data.put("count", results.size());
        data.put("disabledPlatforms", disabledPlatformNames);

        Map<String, Object> ok = new HashMap<>();
        ok.put("code", 0);
        ok.put("message", "\u67e5\u8be2\u6210\u529f");
        ok.put("data", data);
        ok.put("quota", withDisabledPlatformsQuota(ip, config.dailyLimit(), decision.usedAfter(), disabledPlatformNames, config));
        return ok;
    }

    @Override
    public List<UserApiQueryRecord> listIpLogs(String ip, String beginTime, String endTime) {
        UserApiQueryRecord q = new UserApiQueryRecord();
        q.setPlatformName(FREE_LOG_PLATFORM_NAME);
        if (StringUtils.isNotEmpty(ip)) {
            q.setCreateBy("free-ip-" + ip.trim());
        }
        if (StringUtils.isNotEmpty(beginTime) || StringUtils.isNotEmpty(endTime)) {
            Map<String, Object> params = q.getParams();
            if (StringUtils.isNotEmpty(beginTime)) {
                params.put("beginTime", beginTime);
            }
            if (StringUtils.isNotEmpty(endTime)) {
                params.put("endTime", endTime);
            }
        }
        return userApiQueryRecordService.selectUserApiQueryRecordList(q);
    }

    private ApiRequestVO buildApiRequest(com.geek.server.domain.UserPlatformUrlConfig platform, String phone) {
        ApiRequestVO req = new ApiRequestVO();
        req.setQueryType("2");
        req.setPlatformId(platform.getPlatformId() != null ? platform.getPlatformId() : String.valueOf(platform.getId()));
        req.setPlatformName(platform.getPlatformName());
        req.setUrl(platform.getUrl());
        req.setRequestIntervalMs(platform.getRequestIntervalMs());
        req.setTimeoutMs(platform.getTimeoutMs());
        req.setRetryCount(platform.getRetryCount());
        req.setPreActionType(platform.getPreActionType());
        req.setPreActionConfig(platform.getPreActionConfig());
        req.setHeadersTemplate(platform.getHeadersTemplate());
        req.setConcurrencyLimit(platform.getConcurrencyLimit());
        req.setPhoneNumber(phone);
        return req;
    }

    private void validatePhone(String phone) {
        if (StringUtils.isEmpty(phone)) {
            throw new IllegalArgumentException("\u8bf7\u8f93\u5165\u53f7\u7801");
        }
        String phoneRegex = "^(1[3-9]\\d{9}|(0\\d{2,3}-?)?\\d{7,8}|400-?\\d{7}|800-?\\d{7}|1[0-9]{1,4})$";
        if (!phone.matches(phoneRegex)) {
            throw new IllegalArgumentException("\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u67e5\u8be2\u53f7\u7801\u683c\u5f0f");
        }
    }

    private String normalizePhone(String val) {
        if (val == null) {
            return "";
        }
        return val.replaceAll("[^\\d]", "");
    }

    private NormalizedDeviceId normalizeDeviceId(String value, String ip) {
        String normalized = StringUtils.isEmpty(value) ? "" : value.trim().replaceAll("\\s+", "");
        boolean usedIpFallback = false;
        if (StringUtils.isEmpty(normalized)) {
            normalized = "ip#" + ip;
            usedIpFallback = true;
        }
        if (normalized.length() > MAX_DEVICE_ID_LENGTH) {
            normalized = normalized.substring(0, MAX_DEVICE_ID_LENGTH);
        }
        return new NormalizedDeviceId(normalized, usedIpFallback, usedIpFallback ? "ip-fallback" : "client");
    }

    private String appendIpToRequestParams(String requestParams, String ip) {
        String base = StringUtils.isEmpty(requestParams) ? "{}" : requestParams;
        return base + " | sourceIp=" + ip;
    }

    private void saveIpLog(String ip, String phone, String requestStatus, Long requestTime, int limit, int usedBefore,
                           int usedAfter, String message, String deviceId, String taskId, String deviceSource) {
        UserApiQueryRecord row = new UserApiQueryRecord();
        row.setQueryType("9");
        row.setPlatformId(0L);
        row.setPlatformName(FREE_LOG_PLATFORM_NAME);
        row.setRequestStatus(requestStatus);
        row.setRequestTime(requestTime);
        row.setPhone(phone);
        row.setCreateBy("free-ip-" + ip);
        row.setUserId(FREE_QUERY_USER_ID);
        row.setCreateTime(new Date());
        row.setTaskId(taskId);
        row.setResults(message);
        row.setRequestParams("ip=" + ip + ", deviceId=" + deviceId + ", deviceSource=" + deviceSource
                + ", limit=" + limit + ", usedBefore=" + usedBefore + ", usedAfter=" + usedAfter);
        row.setResponseResult(message);
        userApiQueryRecordService.insertUserApiQueryRecord(row);
    }

    /**
     * \u8bfb\u53d6\u514d\u8d39\u67e5\u8be2\u4e0d\u53d1\u8d77\u5916\u90e8\u8bf7\u6c42\u7684\u5e73\u53f0\u540d\u79f0\u5217\u8868\u3002
     * \u5b57\u5178\u65e0 disabled_platforms \u9879\u65f6\u4f7f\u7528\u9ed8\u8ba4\uff08\u8054\u901a\u7ba1\u5bb6\uff09\uff1b
     * \u5b57\u5178\u9879\u5b58\u5728\u4e14\u503c\u4e3a\u7a7a\u5b57\u7b26\u4e32\u65f6\u8868\u793a\u4e0d\u989d\u5916\u7981\u7528\u4efb\u4f55\u5e73\u53f0\u3002
     */
    private List<String> loadFreeQueryDisabledPlatformNames() {
        List<SysDictData> dicts = dictTypeService.selectDictDataByType(DICT_TYPE_FREE_QUERY_CONFIG);
        String raw = findDictValue(dicts, DICT_KEY_DISABLED_PLATFORMS);
        if (raw == null) {
            raw = DEFAULT_FREE_QUERY_DISABLED_PLATFORMS;
        }
        raw = raw.trim();
        if (raw.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .toList();
    }

    private DictConfig loadDictConfig() {
        List<SysDictData> dicts = dictTypeService.selectDictDataByType(DICT_TYPE_FREE_QUERY_CONFIG);
        if (dicts == null) {
            return new DictConfig(DEFAULT_DAILY_LIMIT, DEFAULT_OVER_LIMIT_MSG, DEFAULT_DAILY_ALL_LIMIT,
                    DEFAULT_DAILY_LIMIT, DEFAULT_DEVICE_OVER_LIMIT_MSG, DEFAULT_REQUIRE_DEVICE_ID,
                    DEFAULT_REQUIRE_DEVICE_ID_MSG);
        }
        String dailyLimitRaw = findDictValue(dicts, DICT_KEY_DAILY_LIMIT);
        String dailyAllLimitRaw = findDictValue(dicts, DICT_KEY_DAILY_ALL_LIMIT);
        String dailyDeviceLimitRaw = findDictValue(dicts, DICT_KEY_DAILY_DEVICE_LIMIT);
        String overLimitMsg = findDictValue(dicts, DICT_KEY_OVER_LIMIT_MSG);
        String deviceOverLimitMsg = findDictValue(dicts, DICT_KEY_DEVICE_OVER_LIMIT_MSG);
        String requireDeviceIdRaw = findDictValue(dicts, DICT_KEY_REQUIRE_DEVICE_ID);
        String requireDeviceIdMsg = findDictValue(dicts, DICT_KEY_REQUIRE_DEVICE_ID_MSG);
        int dailyLimit = DEFAULT_DAILY_LIMIT;
        try {
            if (StringUtils.isNotEmpty(dailyLimitRaw)) {
                dailyLimit = Integer.parseInt(dailyLimitRaw.trim());
            }
        } catch (Exception ignored) {
        }
        if (dailyLimit <= 0) {
            dailyLimit = DEFAULT_DAILY_LIMIT;
        }
        int dailyDeviceLimit = dailyLimit;
        try {
            if (StringUtils.isNotEmpty(dailyDeviceLimitRaw)) {
                dailyDeviceLimit = Integer.parseInt(dailyDeviceLimitRaw.trim());
            }
        } catch (Exception ignored) {
        }
        if (dailyDeviceLimit <= 0) {
            dailyDeviceLimit = dailyLimit;
        }
        int dailyAllLimit = DEFAULT_DAILY_ALL_LIMIT;
        try {
            if (StringUtils.isNotEmpty(dailyAllLimitRaw)) {
                dailyAllLimit = Integer.parseInt(dailyAllLimitRaw.trim());
            }
        } catch (Exception ignored) {
        }
        if (dailyAllLimit <= 0) {
            dailyAllLimit = DEFAULT_DAILY_ALL_LIMIT;
        }
        if (StringUtils.isEmpty(overLimitMsg)) {
            overLimitMsg = DEFAULT_OVER_LIMIT_MSG;
        }
        if (StringUtils.isEmpty(deviceOverLimitMsg)) {
            deviceOverLimitMsg = DEFAULT_DEVICE_OVER_LIMIT_MSG;
        }
        if (StringUtils.isEmpty(requireDeviceIdMsg)) {
            requireDeviceIdMsg = DEFAULT_REQUIRE_DEVICE_ID_MSG;
        }
        boolean requireDeviceId = parseDictBoolean(requireDeviceIdRaw, DEFAULT_REQUIRE_DEVICE_ID);
        return new DictConfig(dailyLimit, overLimitMsg, dailyAllLimit, dailyDeviceLimit, deviceOverLimitMsg,
                requireDeviceId, requireDeviceIdMsg);
    }

    private String findDictValue(List<SysDictData> dicts, String key) {
        if (dicts == null || dicts.isEmpty()) {
            return null;
        }
        return dicts.stream()
                .filter(Objects::nonNull)
                .filter(d -> key.equals(d.getDictLabel()))
                .map(SysDictData::getDictValue)
                .findFirst()
                .orElse(null);
    }

    private boolean parseDictBoolean(String raw, boolean defaultValue) {
        if (StringUtils.isEmpty(raw)) {
            return defaultValue;
        }
        String normalized = raw.trim().toLowerCase();
        if ("1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized)
                || "y".equals(normalized) || "on".equals(normalized)) {
            return true;
        }
        if ("0".equals(normalized) || "false".equals(normalized) || "no".equals(normalized)
                || "n".equals(normalized) || "off".equals(normalized)) {
            return false;
        }
        return defaultValue;
    }

    /**
     * \u672a\u914d\u7f6e\u6216\u4e3a MAX_VALUE \u65f6\u4e0d\u5360\u7528\u5168\u5c40\u989d\u5ea6\u8ba1\u6570\u3002
     */
    private CounterDecision reserveGlobalSlotIfNeeded(int dailyAllLimit) {
        if (dailyAllLimit >= DEFAULT_DAILY_ALL_LIMIT) {
            return new CounterDecision(true, 0, 0);
        }
        return increaseGlobalIfAllowed(dailyAllLimit);
    }

    private void rollbackGlobalSlotIfNeeded(int dailyAllLimit) {
        if (dailyAllLimit >= DEFAULT_DAILY_ALL_LIMIT) {
            return;
        }
        rollbackGlobalOne();
    }

    private CounterDecision increaseGlobalIfAllowed(int limit) {
        synchronized (GLOBAL_FREE_QUERY_LOCK) {
            String key = buildGlobalDailyCounterKey();
            int before = getCounter(key);
            if (before >= limit) {
                return new CounterDecision(false, before, before);
            }
            int after = before + 1;
            putCounter(key, after);
            return new CounterDecision(true, before, after);
        }
    }

    private CounterDecision increaseDeviceIfAllowed(String deviceId, int limit) {
        String key = buildDailyDeviceCounterKey(deviceId);
        Object lock = COUNTER_LOCKS.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            int before = getCounter(key);
            if (before >= limit) {
                return new CounterDecision(false, before, before);
            }
            int after = before + 1;
            putCounter(key, after);
            return new CounterDecision(true, before, after);
        }
    }

    private void rollbackIpOne(String ip) {
        rollbackCounterOne(buildDailyCounterKey(ip));
    }

    private void rollbackCounterOne(String key) {
        Object lock = COUNTER_LOCKS.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            int value = getCounter(key);
            if (value > 0) {
                putCounter(key, value - 1);
            }
        }
    }

    private void rollbackGlobalOne() {
        synchronized (GLOBAL_FREE_QUERY_LOCK) {
            String key = buildGlobalDailyCounterKey();
            int v = getCounter(key);
            if (v > 0) {
                putCounter(key, v - 1);
            }
        }
    }

    private CounterDecision increaseIfAllowed(String ip, int limit) {
        String key = buildDailyCounterKey(ip);
        Object lock = COUNTER_LOCKS.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            int before = getCounter(key);
            if (before >= limit) {
                return new CounterDecision(false, before, before);
            }
            int after = before + 1;
            putCounter(key, after);
            return new CounterDecision(true, before, after);
        }
    }

    private QuotaCounter currentCounter(String ip, int limit) {
        String key = buildDailyCounterKey(ip);
        int used = getCounter(key);
        int remaining = Math.max(0, limit - used);
        return new QuotaCounter(used, remaining);
    }

    private QuotaCounter currentDeviceCounter(String deviceId, int limit) {
        String key = buildDailyDeviceCounterKey(deviceId);
        int used = getCounter(key);
        int remaining = Math.max(0, limit - used);
        return new QuotaCounter(used, remaining);
    }

    private int getCounter(String key) {
        Integer count = CacheUtils.get(CacheConstants.RATE_LIMIT_KEY, key, Integer.class);
        return count == null ? 0 : count;
    }

    private void putCounter(String key, int value) {
        long seconds = secondsToTomorrow();
        CacheUtils.put(CacheConstants.RATE_LIMIT_KEY, key, value, seconds, TimeUnit.SECONDS);
    }

    private long secondsToTomorrow() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIN);
        long sec = Duration.between(now, tomorrow).getSeconds();
        return Math.max(1L, sec);
    }

    private String buildDailyCounterKey(String ip) {
        return "free_query:ip:" + LocalDate.now().format(DATE_KEY_FORMAT) + ":" + ip;
    }

    private String buildDailyDeviceCounterKey(String deviceId) {
        return "free_query:device:" + LocalDate.now().format(DATE_KEY_FORMAT) + ":" + deviceId;
    }

    /** \u5168\u7ad9\u5f53\u65e5\u514d\u8d39\u67e5\u8be2\u603b\u6b21\u6570\uff08\u6240\u6709 IP \u5171\u7528\uff09 */
    private String buildGlobalDailyCounterKey() {
        return "free_query:all:" + LocalDate.now().format(DATE_KEY_FORMAT);
    }

    private Map<String, Object> buildQuota(String ip, int limit, int used) {
        Map<String, Object> quota = new LinkedHashMap<>();
        quota.put("ip", ip);
        quota.put("limit", limit);
        quota.put("used", used);
        quota.put("remaining", Math.max(0, limit - used));
        quota.put("resetAt", LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIN).format(DATETIME_FORMAT));
        return quota;
    }

    private Map<String, Object> withDisabledPlatformsQuota(String ip, int limit, int used, List<String> disabledPlatformNames) {
        Map<String, Object> quota = buildQuota(ip, limit, used);
        quota.put("disabledPlatforms", disabledPlatformNames);
        return quota;
    }

    private Map<String, Object> withDisabledPlatformsQuota(String ip, int limit, int used,
                                                           List<String> disabledPlatformNames, DictConfig config) {
        Map<String, Object> quota = withDisabledPlatformsQuota(ip, limit, used, disabledPlatformNames);
        quota.put("deviceLimit", config.dailyDeviceLimit());
        quota.put("deviceRequired", config.requireDeviceId());
        return quota;
    }

    private record DictConfig(int dailyLimit, String overLimitMsg, int dailyAllLimit,
                              int dailyDeviceLimit, String deviceOverLimitMsg,
                              boolean requireDeviceId, String requireDeviceIdMsg) {}

    private record NormalizedDeviceId(String value, boolean usedIpFallback, String deviceSource) {}

    private record CounterDecision(boolean allowed, int usedBefore, int usedAfter) {}

    private record QuotaCounter(int used, int remaining) {}
}

