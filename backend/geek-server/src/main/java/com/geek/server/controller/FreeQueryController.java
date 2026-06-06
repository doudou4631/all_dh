package com.geek.server.controller;

import com.geek.common.annotation.Anonymous;
import com.geek.common.constant.CacheConstants;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.exception.ServiceException;
import com.geek.common.utils.CacheUtils;
import com.geek.common.utils.StringUtils;
import com.geek.common.utils.ip.IpUtils;
import com.geek.common.utils.uuid.IdUtils;
import com.geek.server.domain.FreeQueryUser;
import com.geek.server.domain.UserApiQueryRecord;
import com.geek.server.domain.dto.FreeQueryLoginSession;
import com.geek.server.domain.entity.BatchTask;
import com.geek.server.domain.vo.FreeBatchQueryRequest;
import com.geek.server.domain.vo.FreeLoginRequest;
import com.geek.server.domain.vo.FreeSingleQueryRequest;
import com.geek.server.service.IFreeQueryService;
import com.geek.server.service.IUserApiQueryRecordService;
import com.geek.server.service.IFreeQueryUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Tag(name = "free-query")
@RestController
@RequestMapping(value = "/server/freeQuery", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@RequiredArgsConstructor
public class FreeQueryController extends BaseController {

    private static final int FREE_QUERY_TOKEN_EXPIRE_HOURS = 24 * 7;
    private static final String FREE_TOKEN_HEADER = "X-Free-Token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SOURCE_TYPE_FREE_SINGLE = "FREE_SINGLE";
    private static final String SOURCE_TYPE_FREE_BATCH = "FREE_BATCH";
    private static final int DEFAULT_RECORD_LIMIT = 120;
    private static final int MAX_RECORD_LIMIT = 200;

    private final IFreeQueryService freeQueryService;
    private final IFreeQueryUserService freeQueryUserService;
    private final IUserApiQueryRecordService userApiQueryRecordService;

    @Operation(summary = "获取操作IP的查询次数")
    @Anonymous
    @GetMapping("/quota")
    public AjaxResult quota(HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        return AjaxResult.success(freeQueryService.getQuota(ip));
    }

    @Operation(summary = "提交查询")
    @Anonymous
    @PostMapping("/single")
    public AjaxResult single(@RequestBody FreeSingleQueryRequest body, HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        FreeSingleQueryRequest payload = body == null ? new FreeSingleQueryRequest() : body;
        payload.setSourceType(SOURCE_TYPE_FREE_SINGLE);
        String token = resolveToken((String) null, request);
        Long loginUserId = null;
        String loginAccount = null;
        if (StringUtils.isNotEmpty(token)) {
            FreeQueryLoginSession session = resolveLoginSession(token);
            if (session != null && session.getUserId() != null) {
                try {
                    FreeQueryUser loginUser = freeQueryUserService.requireEnabledUser(session.getUserId());
                    loginUserId = loginUser.getId();
                    loginAccount = loginUser.getAccount();
                    refreshLoginSession(token, session);
                } catch (ServiceException e) {
                    CacheUtils.remove(CacheConstants.FREE_QUERY_LOGIN_TOKEN_KEY, token);
                }
            }
        }
        try {
            Map<String, Object> result = freeQueryService.singleQuery(payload, ip, loginUserId, loginAccount);
            Integer code = (Integer) result.get("code");
            if (code != null && code == 42901) {
                AjaxResult rsp = AjaxResult.error(42901, String.valueOf(result.get("message")));
                rsp.put("quota", result.get("quota"));
                return rsp;
            }
            if (code != null && code == 42902) {
                AjaxResult rsp = AjaxResult.error(42902, String.valueOf(result.get("message")));
                rsp.put("quota", result.get("quota"));
                return rsp;
            }
            if (code != null && code == 42903) {
                AjaxResult rsp = AjaxResult.error(42903, String.valueOf(result.get("message")));
                rsp.put("quota", result.get("quota"));
                return rsp;
            }
            if (code != null && code != 0) {
                AjaxResult rsp = AjaxResult.error(String.valueOf(result.get("message")));
                rsp.put("quota", result.get("quota"));
                return rsp;
            }
            AjaxResult ok = AjaxResult.success(String.valueOf(result.get("message")), result.get("data"));
            ok.put("quota", result.get("quota"));
            return ok;
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error("查询失败" + e.getMessage());
        }
    }

    @Operation(summary = "账号登录校验")
    @Anonymous
    @PostMapping("/login")
    public AjaxResult login(@RequestBody FreeLoginRequest body, HttpServletRequest request) {
        try {
            String loginAccount = normalizeLoginAccount(body != null
                    ? (StringUtils.isNotEmpty(body.getAccount()) ? body.getAccount() : body.getPhone())
                    : null);
            String rawPassword = body == null ? "" : StringUtils.trimToEmpty(body.getPassword());
            String loginIp = IpUtils.getIpAddr(request);
            FreeQueryUser user = freeQueryUserService.authenticate(loginAccount, rawPassword, loginIp);

            String token = IdUtils.fastSimpleUUID();
            long expireSeconds = TimeUnit.HOURS.toSeconds(FREE_QUERY_TOKEN_EXPIRE_HOURS);
            FreeQueryLoginSession session = new FreeQueryLoginSession(user.getId(), user.getAccount());
            CacheUtils.put(CacheConstants.FREE_QUERY_LOGIN_TOKEN_KEY, token, session,
                    FREE_QUERY_TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userId", user.getId());
            data.put("account", user.getAccount());
            data.put("userName", user.getAccount());
            data.put("phone", StringUtils.defaultString(user.getPhone()));
            data.put("nickName", StringUtils.defaultIfEmpty(user.getNickName(), user.getAccount()));
            data.put("points", user.getPoints() == null ? 0 : Math.max(0, user.getPoints()));
            data.put("status", user.getStatus());
            data.put("statusText", "已开通");
            data.put("token", token);
            data.put("tokenExpireSeconds", expireSeconds);
            return AjaxResult.success("登录成功", data);
        } catch (ServiceException e) {
            return AjaxResult.error(40101, e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error("登录失败" + e.getMessage());
        }
    }

    @Operation(summary = "批量提交查询")
    @Anonymous
    @PostMapping("/batch")
    public AjaxResult batch(@RequestBody FreeBatchQueryRequest body, HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        String taskId = IdUtils.fastSimpleUUID();
        int chargedPoints = 0;
        int refundedPoints = 0;
        Long loginUserId = null;
        try {
            if (body == null || CollectionUtils.isEmpty(body.getPhones())) {
                return AjaxResult.error("请输入至少一个号码");
            }

            String token = resolveToken(body, request);
            if (StringUtils.isEmpty(token)) {
                return AjaxResult.error(40101, "请先登录后再使用批量查询");
            }
            FreeQueryLoginSession session = resolveLoginSession(token);
            if (session == null || session.getUserId() == null) {
                return AjaxResult.error(40101, "登录已失效，请重新登录");
            }
            refreshLoginSession(token, session);

            FreeQueryUser loginUser;
            try {
                loginUser = freeQueryUserService.requireEnabledUser(session.getUserId());
            } catch (ServiceException e) {
                CacheUtils.remove(CacheConstants.FREE_QUERY_LOGIN_TOKEN_KEY, token);
                return AjaxResult.error(40101, e.getMessage());
            }
            loginUserId = loginUser.getId();

            List<String> phones = body.getPhones().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .map(s -> s.replaceAll("[^\\d]", ""))
                    .filter(StringUtils::isNotEmpty)
                    .distinct()
                    .limit(20)
                    .toList();

            if (phones.isEmpty()) {
                return AjaxResult.error("请输入正确的号码");
            }

            int platformCount = freeQueryService.countEnabledPlatforms();
            if (platformCount <= 0) {
                return AjaxResult.error("当前暂无可用查询平台");
            }

            int totalChargePoints = phones.size() * platformCount;
            freeQueryUserService.consumePointsForBatch(loginUser.getId(), totalChargePoints, taskId, loginUser.getAccount());
            chargedPoints = totalChargePoints;

            List<Map<String, Object>> itemResults = new ArrayList<>();
            int successCount = 0;
            int failedCount = 0;
            int failedEntries = 0;
            Object latestQuota = null;
            boolean quotaReached = false;
            int quotaCode = 42901;
            String quotaMsg = "查询次数已达上限";

            for (String phone : phones) {
                if (quotaReached) {
                    int phoneFailedEntries = platformCount;
                    failedEntries += phoneFailedEntries;
                    failedCount++;
                    itemResults.add(buildBatchItem(phone, quotaCode, quotaMsg, null, latestQuota, phoneFailedEntries));
                    continue;
                }

                FreeSingleQueryRequest req = new FreeSingleQueryRequest();
                req.setPhone(phone);
                req.setDeviceId(buildBatchDeviceId(loginUser));
                req.setSourceType(SOURCE_TYPE_FREE_BATCH);

                try {
                    Map<String, Object> result = freeQueryService.singleQuery(req, ip, loginUser.getId(), loginUser.getAccount());
                    Integer code = asInteger(result.get("code"));
                    String message = String.valueOf(result.getOrDefault("message", "查询失败"));
                    Object data = result.get("data");
                    Object quota = result.get("quota");
                    latestQuota = quota != null ? quota : latestQuota;
                    int phoneFailedEntries = calculateFailedEntries(code, data, platformCount);
                    failedEntries += phoneFailedEntries;

                    itemResults.add(buildBatchItem(phone, code != null ? code : 500, message, data, quota, phoneFailedEntries));
                    if (code != null && code == 0) {
                        successCount++;
                    } else {
                        failedCount++;
                    }

                    if (code != null && (code == 42901 || code == 42902 || code == 42903)) {
                        quotaReached = true;
                        quotaCode = code;
                        quotaMsg = message;
                    }
                } catch (IllegalArgumentException e) {
                    int phoneFailedEntries = platformCount;
                    failedEntries += phoneFailedEntries;
                    failedCount++;
                    itemResults.add(buildBatchItem(phone, 400, e.getMessage(), null, latestQuota, phoneFailedEntries));
                } catch (Exception e) {
                    int phoneFailedEntries = platformCount;
                    failedEntries += phoneFailedEntries;
                    failedCount++;
                    itemResults.add(buildBatchItem(phone, 500, "查询失败", null, latestQuota, phoneFailedEntries));
                }
            }

            if (failedEntries > 0) {
                freeQueryUserService.refundPointsForBatch(loginUser.getId(), failedEntries, taskId,
                        loginUser.getAccount(), "批量查询失败自动退回积分");
                refundedPoints = failedEntries;
            }

            FreeQueryUser latestUser = freeQueryUserService.selectFreeQueryUserById(loginUser.getId());
            int remainingPoints = latestUser == null || latestUser.getPoints() == null ? 0 : Math.max(0, latestUser.getPoints());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("taskId", taskId);
            data.put("total", phones.size());
            data.put("platformCount", platformCount);
            data.put("successCount", successCount);
            data.put("failedCount", failedCount);
            data.put("totalChargePoints", chargedPoints);
            data.put("refundedPoints", refundedPoints);
            data.put("actualCostPoints", Math.max(0, chargedPoints - refundedPoints));
            data.put("remainingPoints", remainingPoints);
            data.put("results", itemResults);
            if (latestQuota != null) {
                data.put("quota", latestQuota);
            }
            return AjaxResult.success("批量查询完成", data);
        } catch (ServiceException e) {
            safeRefund(loginUserId, taskId, chargedPoints, refundedPoints, "批量查询异常退回积分");
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            safeRefund(loginUserId, taskId, chargedPoints, refundedPoints, "批量查询异常退回积分");
            return AjaxResult.error("批量查询失败" + e.getMessage());
        }
    }

    @Operation(summary = "查询当前账号查询记录")
    @Anonymous
    @GetMapping("/records")
    public AjaxResult records(@RequestParam(required = false) Integer limit,
                              HttpServletRequest request) {
        String token = resolveToken((String) null, request);
        if (StringUtils.isEmpty(token)) {
            return AjaxResult.error(40101, "请先登录后查看查询记录");
        }
        FreeQueryLoginSession session = resolveLoginSession(token);
        if (session == null || session.getUserId() == null) {
            return AjaxResult.error(40101, "登录已失效，请重新登录");
        }
        refreshLoginSession(token, session);
        FreeQueryUser loginUser;
        try {
            loginUser = freeQueryUserService.requireEnabledUser(session.getUserId());
        } catch (ServiceException e) {
            CacheUtils.remove(CacheConstants.FREE_QUERY_LOGIN_TOKEN_KEY, token);
            return AjaxResult.error(40101, e.getMessage());
        }
        int normalizedLimit = normalizeRecordLimit(limit);
        return AjaxResult.success(loadAccountRecords(loginUser.getId(), normalizedLimit));
    }

    @Operation(summary = "查询操作IP的查询记录")
    @GetMapping("/logs")
    public TableDataInfo<UserApiQueryRecord> logs(@RequestParam(required = false) String ip,
                                                  @RequestParam(required = false) String phone,
                                                  @RequestParam(required = false) String requestStatus,
                                                  @RequestParam(required = false) String taskId,
                                                  @RequestParam(required = false) String deviceId,
                                                  @RequestParam(required = false) String deviceSource,
                                                  @RequestParam(required = false) String queryType,
                                                  @RequestParam(required = false) String sourceType,
                                                  @RequestParam(required = false) String beginTime,
                                                  @RequestParam(required = false) String endTime) {
        startPage();
        List<UserApiQueryRecord> list = freeQueryService.listIpLogs(ip, phone, requestStatus, taskId,
                deviceId, deviceSource, queryType, sourceType, beginTime, endTime);
        return getDataTable(list);
    }

    @Operation(summary = "查询日志看板聚合")
    @GetMapping("/logs/dashboard")
    public AjaxResult logsDashboard(@RequestParam(required = false) String ip,
                                    @RequestParam(required = false) String phone,
                                    @RequestParam(required = false) String requestStatus,
                                    @RequestParam(required = false) String taskId,
                                    @RequestParam(required = false) String deviceId,
                                    @RequestParam(required = false) String deviceSource,
                                    @RequestParam(required = false) String queryType,
                                    @RequestParam(required = false) String sourceType,
                                    @RequestParam(required = false) String beginTime,
                                    @RequestParam(required = false) String endTime) {
        return AjaxResult.success(freeQueryService.logDashboard(ip, phone, requestStatus, taskId,
                deviceId, deviceSource, queryType, sourceType, beginTime, endTime));
    }

    private void safeRefund(Long userId, String bizNo, int chargedPoints, int refundedPoints, String reason) {
        int needRefund = chargedPoints - refundedPoints;
        if (userId == null || needRefund <= 0) {
            return;
        }
        try {
            freeQueryUserService.refundPointsForBatch(userId, needRefund, bizNo, "system", reason);
        } catch (Exception ex) {
            log.error("free query batch safe refund failed, userId={}, bizNo={}, needRefund={}", userId, bizNo, needRefund, ex);
        }
    }

    private int calculateFailedEntries(Integer code, Object data, int platformCount) {
        if (code == null || code != 0) {
            return platformCount;
        }
        if (!(data instanceof Map<?, ?> dataMap)) {
            return platformCount;
        }
        Object resultsObj = dataMap.get("results");
        if (!(resultsObj instanceof List<?> results) || results.isEmpty()) {
            return platformCount;
        }
        int failed = 0;
        int seen = 0;
        for (Object row : results) {
            seen++;
            if (isFailedPlatformResult(row)) {
                failed++;
            }
        }
        if (seen < platformCount) {
            failed += (platformCount - seen);
        }
        return Math.max(0, failed);
    }

    private boolean isFailedPlatformResult(Object row) {
        if (row == null) {
            return true;
        }
        if (row instanceof BatchTask.ApiResult apiResult) {
            return StringUtils.isNotEmpty(apiResult.getError());
        }
        if (row instanceof Map<?, ?> map) {
            Object error = map.get("error");
            return error != null && StringUtils.isNotEmpty(String.valueOf(error));
        }
        return true;
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildBatchItem(String phone, Integer code, String message, Object data, Object quota,
                                               int failedEntries) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("phone", phone);
        row.put("code", code);
        row.put("message", message);
        row.put("data", data);
        row.put("quota", quota);
        row.put("failedEntries", failedEntries);
        return row;
    }

    private String normalizeLoginAccount(String val) {
        return val == null ? "" : val.trim();
    }

    private String resolveToken(FreeBatchQueryRequest body, HttpServletRequest request) {
        String bodyToken = body == null ? "" : StringUtils.trimToEmpty(body.getToken());
        String token = resolveToken(bodyToken, request);
        if (StringUtils.isNotEmpty(token)) {
            return token;
        }
        String legacyToken = body == null ? "" : StringUtils.trimToEmpty(body.getDeviceId());
        return legacyToken;
    }

    private String resolveToken(String bodyToken, HttpServletRequest request) {
        if (StringUtils.isNotEmpty(bodyToken)) {
            return bodyToken;
        }
        String headerToken = StringUtils.trimToEmpty(request.getHeader(FREE_TOKEN_HEADER));
        if (StringUtils.isNotEmpty(headerToken)) {
            return headerToken;
        }
        String authHeader = StringUtils.trimToEmpty(request.getHeader(AUTHORIZATION_HEADER));
        if (StringUtils.startsWithIgnoreCase(authHeader, BEARER_PREFIX)) {
            return StringUtils.trimToEmpty(authHeader.substring(BEARER_PREFIX.length()));
        }
        return "";
    }

    private FreeQueryLoginSession resolveLoginSession(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        return CacheUtils.get(CacheConstants.FREE_QUERY_LOGIN_TOKEN_KEY, token, FreeQueryLoginSession.class);
    }

    private void refreshLoginSession(String token, FreeQueryLoginSession session) {
        if (StringUtils.isEmpty(token) || session == null) {
            return;
        }
        CacheUtils.put(CacheConstants.FREE_QUERY_LOGIN_TOKEN_KEY, token, session,
                FREE_QUERY_TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    private String buildBatchDeviceId(FreeQueryUser user) {
        if (user == null || user.getId() == null) {
            return "free-query-user";
        }
        return "fqu#" + user.getId();
    }

    private int normalizeRecordLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_RECORD_LIMIT;
        }
        return Math.min(limit, MAX_RECORD_LIMIT);
    }

    private List<Map<String, Object>> loadAccountRecords(Long userId, int limit) {
        UserApiQueryRecord query = new UserApiQueryRecord();
        query.setUserId(userId);
        query.setQueryType("2");
        List<UserApiQueryRecord> rows = userApiQueryRecordService.selectUserApiQueryRecordList(query);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        Map<String, MobileRecordGroup> groups = new LinkedHashMap<>();
        for (UserApiQueryRecord row : rows) {
            if (row == null) {
                continue;
            }
            String sourceType = StringUtils.trimToEmpty(row.getSourceType()).toUpperCase();
            if (!SOURCE_TYPE_FREE_SINGLE.equals(sourceType) && !SOURCE_TYPE_FREE_BATCH.equals(sourceType)) {
                continue;
            }
            String phone = StringUtils.trimToEmpty(row.getPhone());
            if (StringUtils.isEmpty(phone)) {
                continue;
            }
            String taskId = StringUtils.trimToEmpty(row.getTaskId());
            String key = phone + "|" + (StringUtils.isNotEmpty(taskId) ? taskId : ("__id_" + row.getId()));
            MobileRecordGroup group = groups.get(key);
            if (group == null) {
                group = new MobileRecordGroup();
                group.phone = phone;
                group.type = SOURCE_TYPE_FREE_BATCH.equals(sourceType) ? "批量查询" : "单号查询";
                group.taskId = taskId;
                group.latestTime = row.getCreateTime();
                groups.put(key, group);
            } else if (row.getCreateTime() != null && (group.latestTime == null || row.getCreateTime().after(group.latestTime))) {
                group.latestTime = row.getCreateTime();
            }
            if (isMarkedPlatformRow(row)) {
                String platform = StringUtils.trimToEmpty(row.getPlatformName());
                if (StringUtils.isNotEmpty(platform)) {
                    group.markedPlatforms.add(platform);
                }
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (MobileRecordGroup group : groups.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("phone", group.phone);
            row.put("type", group.type);
            row.put("marked", group.markedPlatforms.size());
            row.put("markedPlatforms", new ArrayList<>(group.markedPlatforms));
            row.put("time", formatRecordTime(group.latestTime));
            row.put("taskId", group.taskId);
            list.add(row);
            if (list.size() >= limit) {
                break;
            }
        }
        return list;
    }

    private boolean isMarkedPlatformRow(UserApiQueryRecord row) {
        if (row == null) {
            return false;
        }
        if (!"0".equals(StringUtils.trimToEmpty(row.getRequestStatus()))) {
            return false;
        }
        String result = StringUtils.trimToEmpty(row.getResults());
        if (StringUtils.isEmpty(result)) {
            return false;
        }
        if ("无标记".equals(result) || "查询失败".equals(result) || "-".equals(result)) {
            return false;
        }
        return !result.startsWith("未开放");
    }

    private String formatRecordTime(Date date) {
        if (date == null) {
            return "";
        }
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    private static class MobileRecordGroup {
        private String phone;
        private String type;
        private String taskId;
        private Date latestTime;
        private final LinkedHashSet<String> markedPlatforms = new LinkedHashSet<>();
    }
}