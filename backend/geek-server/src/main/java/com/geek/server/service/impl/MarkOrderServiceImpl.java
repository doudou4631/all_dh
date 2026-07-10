package com.geek.server.service.impl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.common.core.domain.entity.SysMenu;
import com.geek.common.exception.ServiceException;
import com.geek.common.utils.DateUtils;
import com.geek.common.utils.SecurityUtils;
import com.geek.common.utils.http.HttpUtils;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.MarkOrderItem;
import com.geek.server.domain.MarkPlatformTemplate;
import com.geek.server.domain.MarkUserPlatformPrice;
import com.geek.server.domain.MarkUserPlatformQuota;
import com.geek.server.domain.MarkWalletLog;
import com.geek.server.domain.UserAggregateConfig;
import com.geek.server.domain.UserPlatformUrlConfig;
import com.geek.server.domain.dto.OptimizedBatchItemOutcome;
import com.geek.server.domain.entity.BatchTask;
import com.geek.server.domain.dto.MarkAgentPlatformQuotaAdjustRequest;
import com.geek.server.domain.dto.MarkAgentPlatformStatusRequest;
import com.geek.server.domain.dto.MarkOrderAuditRequest;
import com.geek.server.domain.dto.MarkOrderCreateRequest;
import com.geek.server.domain.dto.MarkOrderItemProcessRequest;
import com.geek.server.domain.dto.MarkTencentStatusQueryRequest;
import com.geek.server.domain.dto.MarkTencentSubmitRequest;
import com.geek.server.domain.dto.MarkTdxSecondSendCodeRequest;
import com.geek.server.domain.dto.MarkTdxSecondSubmitRequest;
import com.geek.server.domain.vo.ApiRequestVO;
import com.geek.server.domain.vo.MarkAgentAuditStatsVO;
import com.geek.server.domain.vo.MarkAgentDownstreamSummaryVO;
import com.geek.server.domain.vo.MarkAgentMeSummaryVO;
import com.geek.server.domain.vo.MarkAgentOrderItemVO;
import com.geek.server.domain.vo.MarkAgentPlatformQuotaAdjustResultVO;
import com.geek.server.domain.vo.MarkOrderDetailVO;
import com.geek.server.domain.vo.MarkOrderPrecheckResultVO;
import com.geek.server.domain.vo.MarkPhoneCheckItemVO;
import com.geek.server.domain.vo.MarkTdxSecondSendCodeResultVO;
import com.geek.server.domain.vo.MarkTdxSecondSubmitResultVO;
import com.geek.server.domain.vo.MarkTencentStatusItemVO;
import com.geek.server.domain.vo.MarkTencentStatusQueryResultVO;
import com.geek.server.domain.vo.MarkTencentSubmitResultVO;
import com.geek.server.domain.vo.MarkWalletSummaryVO;
import com.geek.server.domain.vo.OptimizedBatchSession;
import com.geek.server.mapper.MarkOrderItemMapper;
import com.geek.server.mapper.MarkOrderMapper;
import com.geek.server.mapper.MarkPlatformTemplateMapper;
import com.geek.server.mapper.MarkUserPlatformPriceMapper;
import com.geek.server.mapper.MarkUserPlatformQuotaMapper;
import com.geek.server.mapper.MarkWalletLogMapper;
import com.geek.server.service.IMarkOrderService;
import com.geek.server.service.IOptimizedBatchApiExecutor;
import com.geek.server.service.TdxSecondAppealService;
import com.geek.server.service.IUserAggregateConfigService;
import com.geek.server.service.IUserPlatformUrlConfigService;
import com.geek.server.service.IMarkUserNoticeService;
import com.geek.system.mapper.SysUserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 迁移订单/钱包服务实现
 */
@Service
@Slf4j
public class MarkOrderServiceImpl implements IMarkOrderService {

    private static final Long MARK_ROOT_MENU_ID = 900100000001L;
    private static final String MARK_USER_MENU_COMPONENT = "server/mark/user/index";
    private static final String TENCENT_BASE_URL = "https://yun.m.qq.com";
    private static final String TENCENT_REFERER = TENCENT_BASE_URL + "/shouguan/audit/index.html";
    private static final String TENCENT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String TENCENT_AUTO_OPERATOR = "tencent-auto";
    private static final long TENCENT_AUTO_PROCESS_DELAY_MS = 30_000L;
    private static final String TDX_SECOND_PLATFORM_CODE = "td_second";
    private static final String TDX_SECOND_AUTO_OPERATOR = "tdx-second-api";
    private static final String TD_GAOPIN_AUTO_OPERATOR = "td-gaopin-auto";
    private static final int TD_GAOPIN_AUTO_BATCH_LIMIT = 200;
    private static final String XIAOMI_AUTO_OPERATOR = "xiaomi-auto";
    private static final int XIAOMI_AUTO_BATCH_LIMIT = 50;
    private static final String TD_GAOPIN_HF_KEY = "高频标记至少需要10个工作日或找平台方帮忙处理";
    private static final String TD_GAOPIN_FRAUD_KEY = "疑似诈骗";
    private static final String TD_GAOPIN_DUPLICATE_APPEAL_KEY = "号码当天已提交过申诉，不可重复提交";
    private static final Pattern JSONP_WRAPPER_PATTERN = Pattern.compile("^[\\w$]+\\((.*)\\)\\s*;?$", Pattern.DOTALL);
    private static final Map<String, String> LEGACY_PLATFORM_NAME_MAP = new LinkedHashMap<>();

    static {
        LEGACY_PLATFORM_NAME_MAP.put("mobile_gaopin", "高频拦截");
        LEGACY_PLATFORM_NAME_MAP.put("td_gaopin", "泰迪高频");
        LEGACY_PLATFORM_NAME_MAP.put("td_second", "泰迪二次");
        LEGACY_PLATFORM_NAME_MAP.put("qihu_first", "360首次");
        LEGACY_PLATFORM_NAME_MAP.put("qihu_second", "360二次");
        LEGACY_PLATFORM_NAME_MAP.put("dianhuabang", "电话邦");
        LEGACY_PLATFORM_NAME_MAP.put("tencent_mark", "腾讯速解");
    }

    @Autowired
    private MarkOrderMapper markOrderMapper;

    @Autowired
    private MarkOrderItemMapper markOrderItemMapper;

    @Autowired
    private MarkWalletLogMapper markWalletLogMapper;

    @Autowired
    private MarkUserPlatformPriceMapper markUserPlatformPriceMapper;
    @Autowired
    private MarkUserPlatformQuotaMapper markUserPlatformQuotaMapper;
    @Autowired
    private MarkPlatformTemplateMapper markPlatformTemplateMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private IUserPlatformUrlConfigService userPlatformUrlConfigService;

    @Autowired
    private IUserAggregateConfigService userAggregateConfigService;

    @Autowired
    private IOptimizedBatchApiExecutor optimizedBatchApiExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IMarkUserNoticeService markUserNoticeService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TdxSecondAppealService tdxSecondAppealService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkOrderDetailVO createOrder(MarkOrderCreateRequest request) {
        if (request == null) {
            throw new ServiceException("请求参数不能为空");
        }
        Long currentUserId = SecurityUtils.getUserId();
        String currentUserName = SecurityUtils.getUsername();
        String platformCode = normalizePlatformCode(request.getPlatformCode());
        if (StringUtils.isBlank(platformCode)) {
            throw new ServiceException("平台编码不能为空");
        }
        assertNotLegacyTencentBatchPlatform(platformCode);
        assertPlatformAvailableForSubmit(currentUserId, platformCode, request.getPlatformName());

        List<String> normalizedPhones = normalizePhones(request.getPhones());
        if (normalizedPhones.isEmpty()) {
            throw new ServiceException("没有可用号码");
        }
        long unitPrice = getEffectiveUnitPrice(currentUserId, platformCode);
        long totalAmount = unitPrice * normalizedPhones.size();
        assertUserPlatformQuotaSufficient(currentUserId, platformCode, totalAmount);

        String requestNo = StringUtils.trimToEmpty(request.getRequestNo());
        if (StringUtils.isNotBlank(requestNo)) {
            Long existedOrderId = markOrderMapper.selectOrderIdByUserAndRequestNo(currentUserId, requestNo);
            if (existedOrderId != null) {
                return selectMyOrderDetail(existedOrderId);
            }
        }

        String platformName = resolvePlatformNameByUser(currentUserId, platformCode, request.getPlatformName());
        assertTdGaopinOrderPhonesValid(
                currentUserId,
                currentUserName,
                platformCode,
                platformName,
                normalizedPhones
        );
        assertMobileGaopinOrderPhonesValid(
                currentUserId,
                currentUserName,
                platformCode,
                platformName,
                normalizedPhones
        );

        Date now = DateUtils.getNowDate();
        MarkUserPlatformQuota quota = lockUserPlatformQuota(
                currentUserId,
                platformCode,
                platformName,
                currentUserName,
                now
        );
        long balanceBefore = sanitizeRemainCount(quota.getRemainCount());
        assertUserPlatformQuotaSufficient(balanceBefore, totalAmount);
        long balanceAfter = balanceBefore - totalAmount;
        quota.setPlatformName(platformName);
        quota.setRemainCount(balanceAfter);
        quota.setUpdateBy(currentUserName);
        quota.setUpdateTime(now);
        markUserPlatformQuotaMapper.updateMarkUserPlatformQuota(quota);
        MarkOrder order = new MarkOrder();
        order.setOrderNo(generateOrderNo());
        order.setRequestNo(StringUtils.isBlank(requestNo) ? null : requestNo);
        order.setUserId(currentUserId);
        order.setPlatformCode(platformCode);
        order.setPlatformName(platformName);
        order.setTotalCount(normalizedPhones.size());
        order.setSuccessCount(0);
        order.setFailedCount(0);
        order.setTotalAmount(totalAmount);
        order.setRefundAmount(0L);
        order.setOrderStatus("0");
        order.setAuditStatus("0");
        order.setRemark(request.getRemark());
        Long assignedAgentId = resolveAssignedAgentId(currentUserId);
        if (assignedAgentId != null) {
            order.setAssignedAgentId(assignedAgentId);
        }
        order.setCreateBy(currentUserName);
        order.setCreateTime(now);
        order.setUpdateBy(currentUserName);
        order.setUpdateTime(now);
        markOrderMapper.insertMarkOrder(order);

        for (String phone : normalizedPhones) {
            MarkOrderItem item = new MarkOrderItem();
            item.setOrderId(order.getId());
            item.setPhone(phone);
            item.setUnitPrice(unitPrice);
            item.setItemAmount(unitPrice);
            item.setProcessStatus("0");
            item.setRefunded("0");
            if (StringUtils.isNotBlank(request.getRemark())) {
                item.setRemark(request.getRemark());
            }
            item.setCreateBy(currentUserName);
            item.setCreateTime(now);
            item.setUpdateBy(currentUserName);
            item.setUpdateTime(now);
            markOrderItemMapper.insertMarkOrderItem(item);
        }

        autoPassOrderForAgentProcessing(order.getId(), assignedAgentId, currentUserName, now);

        insertWalletLog(
                currentUserId,
                order.getId(),
                null,
                order.getPlatformCode(),
                order.getPlatformName(),
                "DEDUCT",
                -totalAmount,
                balanceBefore,
                balanceAfter,
                "用户下单扣费",
                currentUserName,
                now
        );
        markUserNoticeService.sendOrderSubmitNotice(
                currentUserId,
                order.getId(),
                order.getOrderNo(),
                platformName,
                currentUserName
        );
        return buildOrderDetail(order.getId());
    }

    @Override
    public MarkOrderPrecheckResultVO precheckOrder(MarkOrderCreateRequest request) {
        Long currentUserId = SecurityUtils.getUserId();
        String currentUserName = SecurityUtils.getUsername();
        if (request == null || StringUtils.isBlank(request.getPlatformCode())) {
            throw new ServiceException("平台编码不能为空");
        }
        String platformCode = normalizePlatformCode(request.getPlatformCode());
        if (StringUtils.isBlank(platformCode)) {
            throw new ServiceException("平台编码不能为空");
        }
        assertNotLegacyTencentBatchPlatform(platformCode);
        assertPlatformAvailableForSubmit(currentUserId, platformCode, request.getPlatformName());
        List<String> normalizedPhones = normalizePhones(request.getPhones());
        if (normalizedPhones.isEmpty()) {
            throw new ServiceException("没有可用号码");
        }
        assertUserPlatformQuotaSufficient(
                currentUserId,
                platformCode,
                getEffectiveUnitPrice(currentUserId, platformCode)
        );

        MarkOrderPrecheckResultVO resultVO = new MarkOrderPrecheckResultVO();
        resultVO.setPlatformCode(platformCode);
        String resolvedPlatformName = resolvePlatformName(platformCode, request.getPlatformName());
        resultVO.setPlatformName(resolvedPlatformName);

        List<String> markedPhones = new ArrayList<>();
        List<String> unmarkedPhones = new ArrayList<>();
        List<String> failedPhones = new ArrayList<>();
        List<MarkPhoneCheckItemVO> items = new ArrayList<>();

        for (String phone : normalizedPhones) {
            MarkPhoneCheckItemVO item = new MarkPhoneCheckItemVO();
            item.setPhone(phone);
            try {
                Map<String, Object> queryResult = executeMarkPrecheckSingleQuery(
                        phone,
                        currentUserId,
                        currentUserName,
                        platformCode,
                        resolvedPlatformName
                );
                int code = parseInt(queryResult == null ? null : queryResult.get("code"), -1);
                if (code != 0) {
                    String errorMessage = asString(queryResult == null ? null : queryResult.get("message"));
                    if (StringUtils.isBlank(errorMessage)) {
                        errorMessage = "预查询失败";
                    }
                    item.setQuerySuccess(false);
                    item.setMarked(false);
                    item.setStatus("FAIL");
                    item.setDetail(errorMessage);
                    item.setErrorMessage(errorMessage);
                    failedPhones.add(phone);
                } else {
                    item.setQuerySuccess(true);
                    item.setMarked(false);
                    fillPrecheckItemFromData(item, queryResult.get("data"));
                    if (isTdGaopinPlatform(platformCode)) {
                        applyTdGaopinPrecheckRules(item);
                    } else if (isMobileGaopinPlatform(platformCode)) {
                        applyMobileGaopinPrecheckRules(item);
                    }
                    if (Boolean.TRUE.equals(item.getMarked())) {
                        markedPhones.add(phone);
                    } else {
                        unmarkedPhones.add(phone);
                    }
                }
            } catch (Exception e) {
                String errorMessage = StringUtils.defaultIfBlank(e.getMessage(), "预查询失败");
                item.setQuerySuccess(false);
                item.setMarked(false);
                item.setStatus("FAIL");
                item.setDetail(errorMessage);
                item.setErrorMessage(errorMessage);
                failedPhones.add(phone);
            }
            items.add(item);
        }

        resultVO.setTotalCount(normalizedPhones.size());
        resultVO.setMarkedCount(markedPhones.size());
        resultVO.setUnmarkedCount(unmarkedPhones.size());
        resultVO.setFailedCount(failedPhones.size());
        resultVO.setMarkedPhones(markedPhones);
        resultVO.setUnmarkedPhones(unmarkedPhones);
        resultVO.setFailedPhones(failedPhones);
        resultVO.setItems(items);
        return resultVO;
    }

    private Map<String, Object> executeMarkPrecheckSingleQuery(String phone,
                                                               Long currentUserId,
                                                               String currentUserName,
                                                               String platformCode,
                                                               String platformName) {
        UserPlatformUrlConfig selectedPlatform = selectMarkPrecheckPlatform(platformCode, platformName);
        if (selectedPlatform == null) {
            return buildMarkPrecheckFail("当前平台未配置可用API: " + displayMarkPrecheckPlatform(platformCode, platformName));
        }

        UserAggregateConfig aggregateConfig = userAggregateConfigService.selectUserAggregateConfigById(1L);
        if (aggregateConfig == null) {
            return buildMarkPrecheckFail("聚合配置缺失，无法执行预查询");
        }

        String taskId = generateMarkPrecheckTaskId();
        ApiRequestVO apiRequest = buildMarkPrecheckApiRequest(selectedPlatform, phone);
        OptimizedBatchSession session = new OptimizedBatchSession(currentUserId, currentUserName, aggregateConfig);
        OptimizedBatchItemOutcome outcome = optimizedBatchApiExecutor.execute(apiRequest, session, taskId);

        List<Object> results = new ArrayList<>();
        if (outcome != null && outcome.getApiResult() != null) {
            results.add(outcome.getApiResult());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", taskId);
        data.put("results", results);
        data.put("count", results.size());

        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("code", 0);
        ok.put("message", "查询成功");
        ok.put("data", data);
        return ok;
    }

    private Map<String, Object> buildMarkPrecheckFail(String message) {
        Map<String, Object> fail = new LinkedHashMap<>();
        fail.put("code", 500);
        fail.put("message", StringUtils.defaultIfBlank(message, "预查询失败"));
        return fail;
    }

    private UserPlatformUrlConfig selectMarkPrecheckPlatform(String platformCode, String platformName) {
        List<UserPlatformUrlConfig> platforms = userPlatformUrlConfigService
                .selectUserPlatformUrlConfigList(new UserPlatformUrlConfig());
        List<UserPlatformUrlConfig> enabledPlatforms = platforms.stream()
                .filter(platform -> platform != null && "0".equals(platform.getStatus()))
                .collect(Collectors.toList());
        if (enabledPlatforms.isEmpty()) {
            return null;
        }
        List<String> candidates = buildMarkPrecheckPlatformCandidates(platformCode, platformName);
        if (candidates.isEmpty()) {
            return null;
        }
        UserPlatformUrlConfig selected = null;
        for (UserPlatformUrlConfig platform : enabledPlatforms) {
            if (!isMarkPrecheckPlatformMatched(platform.getPlatformName(), candidates)) {
                continue;
            }
            if (selected == null || compareMarkPrecheckPlatformPriority(platform, selected) < 0) {
                selected = platform;
            }
        }
        return selected;
    }

    private ApiRequestVO buildMarkPrecheckApiRequest(UserPlatformUrlConfig platform, String phone) {
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

    private String generateMarkPrecheckTaskId() {
        return "markpre" + System.currentTimeMillis() + Math.abs(new Random().nextInt(100000));
    }

    private List<String> buildMarkPrecheckPlatformCandidates(String platformCode, String platformName) {
        Set<String> candidates = new LinkedHashSet<>();
        addMarkPrecheckPlatformCandidate(candidates, platformName);
        addMarkPrecheckPlatformCandidate(candidates, resolveMarkPrecheckPlatformNameByCode(platformCode));
        if (candidates.contains("联通管家")) {
            addMarkPrecheckPlatformCandidate(candidates, "联通安全管家");
        }
        if (candidates.contains("联通安全管家")) {
            addMarkPrecheckPlatformCandidate(candidates, "联通管家");
        }
        if (candidates.contains("腾讯")) {
            addMarkPrecheckPlatformCandidate(candidates, "腾讯平台");
        }
        if (candidates.contains("360")) {
            addMarkPrecheckPlatformCandidate(candidates, "360手机卫士");
            addMarkPrecheckPlatformCandidate(candidates, "360首次");
            addMarkPrecheckPlatformCandidate(candidates, "360二次");
        }
        if (candidates.contains("搜狗")) {
            addMarkPrecheckPlatformCandidate(candidates, "搜狗号码通");
        }
        if (candidates.contains("移动高频")) {
            addMarkPrecheckPlatformCandidate(candidates, "高频拦截");
        }
        if (candidates.contains("泰迪熊")) {
            addMarkPrecheckPlatformCandidate(candidates, "泰迪高频");
            addMarkPrecheckPlatformCandidate(candidates, "泰迪二次");
            addMarkPrecheckPlatformCandidate(candidates, "泰迪熊平台");
        }
        return new ArrayList<>(candidates);
    }

    private void addMarkPrecheckPlatformCandidate(Set<String> target, String platformName) {
        if (target == null || StringUtils.isBlank(platformName)) {
            return;
        }
        String raw = platformName.trim();
        if (raw.isEmpty()) {
            return;
        }
        target.add(raw);
        String normalized = normalizeMarkPrecheckPlatformAlias(raw);
        if (!normalized.isEmpty()) {
            target.add(normalized);
        }
    }

    private boolean isMarkPrecheckPlatformMatched(String platformName, List<String> candidates) {
        if (StringUtils.isBlank(platformName) || candidates == null || candidates.isEmpty()) {
            return false;
        }
        String raw = platformName.trim();
        String normalizedRaw = normalizeMarkPrecheckPlatformAlias(raw);
        for (String candidate : candidates) {
            if (StringUtils.isBlank(candidate)) {
                continue;
            }
            String rawCandidate = candidate.trim();
            if (raw.equals(rawCandidate)) {
                return true;
            }
            if (normalizedRaw.equals(normalizeMarkPrecheckPlatformAlias(rawCandidate))) {
                return true;
            }
        }
        return false;
    }

    private int compareMarkPrecheckPlatformPriority(UserPlatformUrlConfig left, UserPlatformUrlConfig right) {
        int leftSort = left != null && left.getSort() != null ? left.getSort() : Integer.MAX_VALUE;
        int rightSort = right != null && right.getSort() != null ? right.getSort() : Integer.MAX_VALUE;
        if (leftSort != rightSort) {
            return Integer.compare(leftSort, rightSort);
        }
        long leftId = left != null && left.getId() != null ? left.getId() : Long.MAX_VALUE;
        long rightId = right != null && right.getId() != null ? right.getId() : Long.MAX_VALUE;
        return Long.compare(leftId, rightId);
    }

    private String displayMarkPrecheckPlatform(String platformCode, String platformName) {
        if (StringUtils.isNotBlank(platformName)) {
            return platformName.trim();
        }
        String byCode = resolveMarkPrecheckPlatformNameByCode(platformCode);
        if (StringUtils.isNotBlank(byCode)) {
            return byCode;
        }
        return StringUtils.defaultIfBlank(StringUtils.trimToNull(platformCode), "当前平台");
    }

    private String resolveMarkPrecheckPlatformNameByCode(String platformCode) {
        if (StringUtils.isBlank(platformCode)) {
            return null;
        }
        String code = platformCode.trim();
        return switch (code) {
            case "taidixiong", "td_gaopin", "td_second" -> "泰迪熊";
            case "tengxun", "tencent_mark" -> "腾讯";
            case "sanliuling", "qihu_first", "qihu_second" -> "360";
            case "baidu" -> "百度";
            case "sghmt" -> "搜狗";
            case "yidonggaopin", "mobile_gaopin" -> "移动高频";
            case "dianhuabang" -> "电话邦";
            case "ltgj" -> "联通管家";
            case "xiaomi" -> "小米手机";
            default -> null;
        };
    }

    private String normalizeMarkPrecheckPlatformAlias(String platformName) {
        if (StringUtils.isBlank(platformName)) {
            return "";
        }
        String normalized = platformName.trim().replaceAll("\\s+", "");
        if (normalized.isEmpty()) {
            return "";
        }
        return switch (normalized) {
            case "腾讯平台" -> "腾讯";
            case "360手机卫士", "360首次", "360二次", "360平台查询" -> "360";
            case "搜狗号码通" -> "搜狗";
            case "高频拦截" -> "移动高频";
            case "联通安全管家" -> "联通管家";
            case "泰迪高频", "泰迪二次", "泰迪熊平台" -> "泰迪熊";
            default -> normalized.endsWith("平台") && normalized.length() > 2
                    ? normalized.substring(0, normalized.length() - 2)
                    : normalized;
        };
    }
    @Override
    public MarkTencentStatusQueryResultVO queryTencentStatus(MarkTencentStatusQueryRequest request) {
        if (request == null || request.getPhones() == null || request.getPhones().isEmpty()) {
            throw new ServiceException("号码列表不能为空");
        }
        Long currentUserId = SecurityUtils.getUserId();
        resolveTencentPlatformCodeForUser(currentUserId);
        List<String> phones = normalizePhones(request.getPhones());
        if (phones.isEmpty()) {
            throw new ServiceException("没有可用号码");
        }

        List<MarkTencentStatusItemVO> items = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        for (String phone : phones) {
            MarkTencentStatusItemVO item = new MarkTencentStatusItemVO();
            item.setPhone(phone);
            try {
                Map<String, Object> phoneTypeResponse = callTencentJsonp(
                        "/core/sjg/moblie_phone_type",
                        Map.of("phone", phone)
                );
                item.setPhoneTypeResponse(phoneTypeResponse);
                if (phoneTypeResponse == null) {
                    item.setSuccess(false);
                    item.setErrorMessage("phone_type 查询失败");
                    item.setDetail("复查失败：phone_type 查询失败");
                    failedCount++;
                    items.add(item);
                    continue;
                }

                Integer phoneType = parseNullableInt(phoneTypeResponse.get("data"));
                item.setPhoneType(phoneType);

                Map<String, Object> complainStatusResponse = callTencentJsonp(
                        "/core/sjg/phone_complain_status",
                        Map.of("phone", phone)
                );
                item.setComplainStatusResponse(complainStatusResponse);
                String complainStatus = complainStatusResponse == null ? null : asString(complainStatusResponse.get("data"));
                item.setComplainStatus(complainStatus);
                item.setMarked(isTencentPhoneMarked(phoneType, complainStatus));

                item.setSuccess(true);
                item.setDetail(buildTencentRealtimeStatusText(phoneType, complainStatus));
                successCount++;
            } catch (Exception e) {
                item.setSuccess(false);
                item.setErrorMessage(StringUtils.defaultIfBlank(e.getMessage(), "复查异常"));
                item.setDetail("复查失败：" + item.getErrorMessage());
                failedCount++;
            }
            items.add(item);
        }

        MarkTencentStatusQueryResultVO resultVO = new MarkTencentStatusQueryResultVO();
        resultVO.setTotalCount(phones.size());
        resultVO.setSuccessCount(successCount);
        resultVO.setFailedCount(failedCount);
        resultVO.setItems(items);
        return resultVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkTencentSubmitResultVO submitTencent(MarkTencentSubmitRequest request) {
        if (request == null) {
            throw new ServiceException("请求参数不能为空");
        }
        Long currentUserId = SecurityUtils.getUserId();
        String currentUserName = SecurityUtils.getUsername();
        String phone = normalizeSinglePhone(request.getPhone());
        if (!phone.matches("\\d{11}")) {
            throw new ServiceException("手机号应为11位数字");
        }
        String smsCode = StringUtils.trimToEmpty(request.getSmsCode());
        if (!smsCode.matches("\\d{6}")) {
            throw new ServiceException("验证码应为6位数字");
        }
        String tencentPlatformCode = resolveTencentStyleSubmitPlatformCodeForUser(
                currentUserId,
                request.getPlatformCode()
        );
        String tencentPlatformName = resolvePlatformNameByUser(currentUserId, tencentPlatformCode, "腾讯速解");
        return createTencentUserSubmitOrder(
                currentUserId,
                currentUserName,
                phone,
                smsCode,
                tencentPlatformCode,
                tencentPlatformName
        );
    }

    @Override
    public MarkTdxSecondSendCodeResultVO sendTdxSecondCode(MarkTdxSecondSendCodeRequest request) {
        if (request == null) {
            throw new ServiceException("请求参数不能为空");
        }
        Long currentUserId = SecurityUtils.getUserId();
        assertPlatformAvailableForSubmit(currentUserId, TDX_SECOND_PLATFORM_CODE, "Taidixiong二次");
        long unitPrice = getEffectiveUnitPrice(currentUserId, TDX_SECOND_PLATFORM_CODE);
        assertUserPlatformQuotaSufficient(currentUserId, TDX_SECOND_PLATFORM_CODE, unitPrice);
        return tdxSecondAppealService.sendCode(request.getPhone(), request.getLine());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkTdxSecondSubmitResultVO submitTdxSecond(MarkTdxSecondSubmitRequest request) {
        if (request == null) {
            throw new ServiceException("请求参数不能为空");
        }
        String requestedPlatformCode = normalizePlatformCode(request.getPlatformCode());
        if (StringUtils.isNotBlank(requestedPlatformCode)
                && !TDX_SECOND_PLATFORM_CODE.equals(requestedPlatformCode)) {
            throw new ServiceException("当前接口仅支持Taidixiong二次平台");
        }

        Long currentUserId = SecurityUtils.getUserId();
        String currentUserName = SecurityUtils.getUsername();
        assertPlatformAvailableForSubmit(currentUserId, TDX_SECOND_PLATFORM_CODE, "Taidixiong二次");

        String phone = normalizeSinglePhone(request.getPhone());
        if (!phone.matches("\\d{11}")) {
            throw new ServiceException("手机号应为11位数字");
        }
        String smsCode = StringUtils.trimToEmpty(request.getSmsCode());
        if (!smsCode.matches("\\d{6}")) {
            throw new ServiceException("验证码应为6位数字");
        }

        Date now = DateUtils.getNowDate();
        String platformName = resolvePlatformNameByUser(
                currentUserId,
                TDX_SECOND_PLATFORM_CODE,
                "Taidixiong二次"
        );
        long unitPrice = getEffectiveUnitPrice(currentUserId, TDX_SECOND_PLATFORM_CODE);
        assertUserPlatformQuotaSufficient(currentUserId, TDX_SECOND_PLATFORM_CODE, unitPrice);

        MarkTdxSecondSubmitResultVO apiResult = tdxSecondAppealService.submit(
                phone,
                smsCode,
                request.getLine(),
                request.getRotate()
        );

        MarkUserPlatformQuota quota = lockUserPlatformQuota(
                currentUserId,
                TDX_SECOND_PLATFORM_CODE,
                platformName,
                currentUserName,
                now
        );
        long balanceBefore = sanitizeRemainCount(quota.getRemainCount());
        assertUserPlatformQuotaSufficient(balanceBefore, unitPrice);
        long balanceAfter = balanceBefore - unitPrice;
        quota.setPlatformName(platformName);
        quota.setRemainCount(balanceAfter);
        quota.setUpdateBy(currentUserName);
        quota.setUpdateTime(now);
        markUserPlatformQuotaMapper.updateMarkUserPlatformQuota(quota);

        Long assignedAgentId = resolveAssignedAgentId(currentUserId);
        MarkOrder order = new MarkOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(currentUserId);
        order.setPlatformCode(TDX_SECOND_PLATFORM_CODE);
        order.setPlatformName(platformName);
        order.setTotalCount(1);
        order.setSuccessCount(1);
        order.setFailedCount(0);
        order.setTotalAmount(unitPrice);
        order.setRefundAmount(0L);
        order.setOrderStatus("2");
        order.setAuditStatus("1");
        order.setAuditOpinion("TDX二次接口自动完成");
        order.setAuditBy(TDX_SECOND_AUTO_OPERATOR);
        order.setAuditTime(now);
        order.setCompletedTime(now);
        order.setRemark("TDX二次申诉成功");
        if (assignedAgentId != null) {
            order.setAssignedAgentId(assignedAgentId);
        }
        order.setCreateBy(currentUserName);
        order.setCreateTime(now);
        order.setUpdateBy(TDX_SECOND_AUTO_OPERATOR);
        order.setUpdateTime(now);
        markOrderMapper.insertMarkOrder(order);

        String processNote = buildTdxSecondProcessNote(apiResult, request.getLine());
        MarkOrderItem item = new MarkOrderItem();
        item.setOrderId(order.getId());
        item.setPhone(phone);
        item.setUnitPrice(unitPrice);
        item.setItemAmount(unitPrice);
        item.setProcessStatus("1");
        item.setProcessResult("TDX二次申诉成功");
        item.setProcessNote(processNote);
        item.setProcessedBy(TDX_SECOND_AUTO_OPERATOR);
        item.setProcessedTime(now);
        item.setRefunded("0");
        item.setRemark(smsCode);
        item.setCreateBy(currentUserName);
        item.setCreateTime(now);
        item.setUpdateBy(TDX_SECOND_AUTO_OPERATOR);
        item.setUpdateTime(now);
        markOrderItemMapper.insertMarkOrderItem(item);

        insertWalletLog(
                currentUserId,
                order.getId(),
                item.getId(),
                TDX_SECOND_PLATFORM_CODE,
                platformName,
                "DEDUCT",
                -unitPrice,
                balanceBefore,
                balanceAfter,
                "TDX二次申诉成功扣次",
                currentUserName,
                now
        );
        markUserNoticeService.sendOrderSubmitNotice(
                currentUserId,
                order.getId(),
                order.getOrderNo(),
                platformName,
                currentUserName
        );

        apiResult.setItemId(item.getId());
        apiResult.setOrderId(order.getId());
        apiResult.setOrderNo(order.getOrderNo());
        apiResult.setProcessStatus("1");
        return apiResult;
    }

    @Override
    public MarkTencentSubmitResultVO selectMyTencentSubmitResult(Long itemId) {
        if (itemId == null) {
            throw new ServiceException("记录不存在");
        }
        MarkOrderItem item = markOrderItemMapper.selectMarkOrderItemById(itemId);
        if (item == null) {
            throw new ServiceException("记录不存在");
        }
        MarkOrder order = requireOrder(item.getOrderId());
        if (!SecurityUtils.getUserId().equals(order.getUserId())) {
            throw new ServiceException("无权查看该记录");
        }
        MarkTencentSubmitResultVO resultVO = new MarkTencentSubmitResultVO();
        resultVO.setItemId(item.getId());
        resultVO.setOrderId(order.getId());
        resultVO.setOrderNo(order.getOrderNo());
        resultVO.setPhone(item.getPhone());
        resultVO.setProcessStatus(StringUtils.defaultIfBlank(item.getProcessStatus(), "0"));
        if ("1".equals(item.getProcessStatus())) {
            resultVO.setAccepted(true);
        } else if ("2".equals(item.getProcessStatus())) {
            resultVO.setAccepted(false);
            resultVO.setSubmitData(buildTencentUserSubmitFailMessage(item.getProcessResult()));
        }
        return resultVO;
    }

    @Override
    public List<MarkOrder> selectMyOrderList(MarkOrder query) {
        MarkOrder orderQuery = query == null ? new MarkOrder() : query;
        orderQuery.setUserId(SecurityUtils.getUserId());
        return markOrderMapper.selectMyOrderItemList(orderQuery);
    }

    @Override
    public MarkOrderDetailVO selectMyOrderDetail(Long orderId) {
        MarkOrder order = requireOrder(orderId);
        if (!SecurityUtils.getUserId().equals(order.getUserId())) {
            throw new ServiceException("无权查看该订单");
        }
        return buildOrderDetail(orderId);
    }

    @Override
    public MarkWalletSummaryVO selectMyWalletSummary() {
        Long currentUserId = SecurityUtils.getUserId();
        MarkWalletLog query = new MarkWalletLog();
        query.setUserId(currentUserId);
        List<MarkWalletLog> logs = markWalletLogMapper.selectMarkWalletLogList(query);
        List<MarkUserPlatformPrice> platformPrices = selectMyPlatformPriceList();

        long totalDeduct = 0L;
        long totalRefund = 0L;
        long totalRemain = 0L;
        for (MarkWalletLog log : logs) {
            long amount = log.getChangeAmount() == null ? 0L : log.getChangeAmount();
            if (amount < 0) {
                totalDeduct += Math.abs(amount);
            } else {
                totalRefund += amount;
            }
        }
        for (MarkUserPlatformPrice price : platformPrices) {
            totalRemain += sanitizeRemainCount(price.getRemainCount());
        }

        MarkWalletSummaryVO summaryVO = new MarkWalletSummaryVO();
        summaryVO.setUserId(currentUserId);
        summaryVO.setPointsBalance(safeInteger(totalRemain));
        summaryVO.setTotalDeductAmount(totalDeduct);
        summaryVO.setTotalRefundAmount(totalRefund);
        summaryVO.setPlatformPrices(platformPrices);
        return summaryVO;
    }

    @Override
    public List<MarkWalletLog> selectMyWalletLogList(MarkWalletLog query) {
        MarkWalletLog walletLogQuery = query == null ? new MarkWalletLog() : query;
        walletLogQuery.setUserId(SecurityUtils.getUserId());
        return markWalletLogMapper.selectMarkWalletLogList(walletLogQuery);
    }

    @Override
    public List<MarkUserPlatformPrice> selectMyPlatformPriceList() {
        Long currentUserId = SecurityUtils.getUserId();
        return buildPlatformPriceListByUser(currentUserId);
    }

    @Override
    public List<MarkOrder> selectAgentOrderList(MarkOrder query) {
        Long currentUserId = SecurityUtils.getUserId();
        String currentUsername = SecurityUtils.getUsername();
        boolean isAdmin = isAdminRole();
        if (!isAdmin && !isAgentRole()) {
            throw new ServiceException("仅代理或管理员可操作");
        }
        MarkOrder orderQuery = query == null ? new MarkOrder() : query;
        return markOrderMapper.selectAgentOrderList(orderQuery, currentUserId, currentUsername, isAdmin);
    }

    @Override
    public List<MarkAgentOrderItemVO> selectAgentOrderItemList(MarkAgentOrderItemVO query) {
        Long currentUserId = SecurityUtils.getUserId();
        String currentUsername = SecurityUtils.getUsername();
        boolean isAdmin = isAdminRole();
        if (!isAdmin && !isAgentRole()) {
            throw new ServiceException("仅代理或管理员可操作");
        }
        MarkAgentOrderItemVO itemQuery = query == null ? new MarkAgentOrderItemVO() : query;
        return markOrderItemMapper.selectAgentOrderItemList(itemQuery, currentUserId, currentUsername, isAdmin);
    }

    @Override
    public MarkOrderDetailVO selectAgentOrderDetail(Long orderId) {
        MarkOrder order = requireOrder(orderId);
        assertAgentReadable(order, false);
        return buildOrderDetail(orderId);
    }
    @Override
    public List<MarkWalletLog> selectAgentWalletLogList(MarkWalletLog query) {
        String currentUsername = SecurityUtils.getUsername();
        boolean isAdmin = isAdminRole();
        if (!isAdmin && !isAgentRole()) {
            throw new ServiceException("仅代理或管理员可操作");
        }
        MarkWalletLog walletLogQuery = query == null ? new MarkWalletLog() : query;
        if (!isAdmin && walletLogQuery.getUserId() != null) {
            SysUser targetUser = requireUser(walletLogQuery.getUserId());
            String owner = StringUtils.trimToEmpty(targetUser.getCreateBy());
            if (!StringUtils.equals(owner, currentUsername)) {
                throw new ServiceException("仅可查看自己下线用户流水");
            }
        }
        return markWalletLogMapper.selectAgentWalletLogList(walletLogQuery, currentUsername, isAdmin);
    }

    @Override
    public List<MarkAgentDownstreamSummaryVO> selectAgentDownstreamSummaryList() {
        boolean isAdmin = isAdminRole();
        if (!isAdmin && !isAgentRole()) {
            throw new ServiceException("仅代理或管理员可操作");
        }
        return markUserPlatformQuotaMapper.selectDownstreamSummaryByAgent(SecurityUtils.getUsername());
    }

    @Override
    public MarkAgentMeSummaryVO selectAgentMeSummary() {
        if (!isAdminRole() && !isAgentRole()) {
            throw new ServiceException("仅代理或管理员可操作");
        }
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        SysUser user = requireUser(userId);

        MarkAgentMeSummaryVO summary = new MarkAgentMeSummaryVO();
        summary.setUserId(userId);
        summary.setUserName(user.getUserName());
        summary.setNickName(user.getNickName());
        summary.setRemark(user.getRemark());
        summary.setAgentLevelLabel("一级代理");

        Long remain = markUserPlatformQuotaMapper.sumRemainCountByUserId(userId);
        summary.setTotalRemainCount(remain == null ? 0L : remain);

        Long downstreamCount = markUserPlatformQuotaMapper.countDownstreamUsersByAgent(username);
        summary.setDownstreamCount(downstreamCount == null ? 0L : downstreamCount);

        try {
            MarkAgentAuditStatsVO stats = markUserNoticeService.selectAgentAuditStats();
            summary.setPendingAuditCount(stats == null || stats.getPendingCount() == null ? 0L : stats.getPendingCount());
        } catch (Exception ex) {
            summary.setPendingAuditCount(0L);
        }

        List<MarkUserPlatformPrice> prices = buildPlatformPriceListByUser(userId);
        if (prices != null && !prices.isEmpty()) {
            MarkUserPlatformPrice first = prices.get(0);
            summary.setSamplePlatformName(first.getPlatformName());
            summary.setSampleUnitPrice(first.getUnitPrice());
        }
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkOrderDetailVO feedbackOrderItem(Long itemId, MarkOrderItemProcessRequest request) {
        if (request == null || StringUtils.isBlank(request.getProcessStatus())) {
            throw new ServiceException("处理状态不能为空");
        }
        if (!"1".equals(request.getProcessStatus()) && !"2".equals(request.getProcessStatus())) {
            throw new ServiceException("处理状态仅支持 1(成功) 或 2(失败)");
        }

        MarkOrderItem item = markOrderItemMapper.selectMarkOrderItemById(itemId);
        if (item == null) {
            throw new ServiceException("订单明细不存在");
        }

        MarkOrder order = requireOrder(item.getOrderId());
        assertAgentReadable(order, true);
        assertAuditPassed(order);

        if (isTencentPlatformCode(order.getPlatformCode())) {
            throw new ServiceException("腾讯订单由后台自动处理，不支持手动修改");
        }

        Date now = DateUtils.getNowDate();
        String currentUserName = SecurityUtils.getUsername();
        String previousStatus = StringUtils.defaultIfBlank(item.getProcessStatus(), "0");
        String newStatus = request.getProcessStatus();

        if ("2".equals(newStatus) && !"1".equals(item.getRefunded())) {
            String refundReason = ("0".equals(previousStatus) || "3".equals(previousStatus))
                    ? "明细失败自动退款" : "代理修改状态失败退回";
            refundOrderItem(order, item, currentUserName, now, refundReason);
            item.setRefunded("1");
        } else if ("1".equals(newStatus) && "2".equals(previousStatus) && "1".equals(item.getRefunded())) {
            deductOrderItem(order, item, currentUserName, now, "代理修改状态成功重新扣费");
            item.setRefunded("0");
        }

        item.setProcessStatus(newStatus);
        if (StringUtils.isBlank(request.getProcessResult())) {
            item.setProcessResult("1".equals(newStatus) ? "代理手动标记成功" : "代理手动标记失败");
        } else {
            item.setProcessResult(request.getProcessResult());
        }
        if (StringUtils.isBlank(request.getProcessNote())) {
            item.setProcessNote("1".equals(newStatus) ? "代理手动处理成功" : "代理手动处理失败");
        } else {
            item.setProcessNote(request.getProcessNote());
        }
        item.setProcessedBy(currentUserName);
        item.setProcessedTime(now);
        item.setUpdateBy(currentUserName);
        item.setUpdateTime(now);

        markOrderItemMapper.updateMarkOrderItem(item);
        refreshOrderStats(order.getId(), currentUserName);
        return buildOrderDetail(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkOrderDetailVO completeOrder(Long orderId) {
        MarkOrder order = requireOrder(orderId);
        assertAgentReadable(order, true);
        assertAuditPassed(order);
        List<MarkOrderItem> itemList = markOrderItemMapper.selectMarkOrderItemsByOrderId(orderId);
        boolean hasPending = itemList.stream().anyMatch(item -> {
            String status = StringUtils.defaultIfBlank(item.getProcessStatus(), "0");
            return "0".equals(status) || "3".equals(status);
        });
        if (hasPending) {
            throw new ServiceException("仍有待处理明细，无法完成整单");
        }
        refreshOrderStats(orderId, SecurityUtils.getUsername());
        return buildOrderDetail(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkOrderDetailVO completeOrder(Long orderId, MarkOrderItemProcessRequest request) {
        if (request == null || StringUtils.isBlank(request.getProcessStatus())) {
            return completeOrder(orderId);
        }
        String processStatus = StringUtils.trimToEmpty(request.getProcessStatus());
        if (!"1".equals(processStatus) && !"2".equals(processStatus)) {
            throw new ServiceException("处理状态仅支持 1(成功) 或 2(失败)");
        }
        MarkOrder order = requireOrder(orderId);
        assertAgentReadable(order, true);
        assertAuditPassed(order);
        List<MarkOrderItem> itemList = markOrderItemMapper.selectMarkOrderItemsByOrderId(orderId);
        Date now = DateUtils.getNowDate();
        String currentUserName = SecurityUtils.getUsername();
        String processResult = StringUtils.trimToNull(request.getProcessResult());
        if (StringUtils.isBlank(processResult)) {
            processResult = "1".equals(processStatus) ? "代理整单标记成功" : "代理整单标记失败";
        }
        String processNote = StringUtils.trimToNull(request.getProcessNote());
        for (MarkOrderItem item : itemList) {
            if (!"0".equals(item.getProcessStatus())) {
                continue;
            }
            item.setProcessStatus(processStatus);
            item.setProcessResult(processResult);
            item.setProcessNote(processNote);
            item.setProcessedBy(currentUserName);
            item.setProcessedTime(now);
            item.setUpdateBy(currentUserName);
            item.setUpdateTime(now);

            if ("2".equals(processStatus) && !"1".equals(item.getRefunded())) {
                refundOrderItem(order, item, currentUserName, now);
                item.setRefunded("1");
            }
            markOrderItemMapper.updateMarkOrderItem(item);
        }
        refreshOrderStats(orderId, currentUserName);
        return buildOrderDetail(orderId);
    }

    @Override
    public List<MarkOrder> selectAgentAuditPendingList(MarkOrder query) {
        MarkOrder orderQuery = query == null ? new MarkOrder() : query;
        orderQuery.setAuditStatus("0");
        return selectAgentOrderList(orderQuery);
    }

    @Override
    public List<MarkOrder> selectAgentAuditHistoryList(MarkOrder query) {
        MarkOrder orderQuery = query == null ? new MarkOrder() : query;
        orderQuery.setAuditStatus(null);
        orderQuery.getParams().put("auditHistoryOnly", true);
        return selectAgentOrderList(orderQuery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkOrderDetailVO auditOrderPass(Long orderId, MarkOrderAuditRequest request) {
        MarkOrder order = requireOrder(orderId);
        assertAgentReadable(order, true);
        assertAuditPending(order);
        Date now = DateUtils.getNowDate();
        String currentUserName = SecurityUtils.getUsername();
        MarkOrder update = new MarkOrder();
        update.setId(orderId);
        update.setAuditStatus("1");
        update.setAuditOpinion(StringUtils.trimToNull(request == null ? null : request.getAuditOpinion()));
        update.setAuditBy(currentUserName);
        update.setAuditTime(now);
        update.setOrderStatus("1");
        update.setUpdateBy(currentUserName);
        update.setUpdateTime(now);
        markOrderMapper.updateMarkOrder(update);
        markUserNoticeService.sendOrderAuditNotice(
                order.getUserId(),
                orderId,
                order.getOrderNo(),
                order.getPlatformName(),
                "1",
                update.getAuditOpinion(),
                currentUserName
        );
        return buildOrderDetail(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkOrderDetailVO auditOrderReject(Long orderId, MarkOrderAuditRequest request) {
        MarkOrder order = requireOrder(orderId);
        assertAgentReadable(order, false);
        assertAuditPending(order);
        String opinion = StringUtils.trimToNull(request == null ? null : request.getAuditOpinion());
        if (StringUtils.isBlank(opinion)) {
            throw new ServiceException("拒绝时请填写审核意见");
        }
        Date now = DateUtils.getNowDate();
        String currentUserName = SecurityUtils.getUsername();
        refundWholeOrder(order, currentUserName, now, "审核拒绝退款");
        MarkOrder update = new MarkOrder();
        update.setId(orderId);
        update.setAuditStatus("2");
        update.setAuditOpinion(opinion);
        update.setAuditBy(currentUserName);
        update.setAuditTime(now);
        update.setOrderStatus("3");
        update.setCompletedTime(now);
        update.setUpdateBy(currentUserName);
        update.setUpdateTime(now);
        markOrderMapper.updateMarkOrder(update);
        markUserNoticeService.sendOrderAuditNotice(
                order.getUserId(),
                orderId,
                order.getOrderNo(),
                order.getPlatformName(),
                "2",
                opinion,
                currentUserName
        );
        return buildOrderDetail(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkOrderDetailVO auditOrderReturn(Long orderId, MarkOrderAuditRequest request) {
        MarkOrder order = requireOrder(orderId);
        assertAgentReadable(order, false);
        assertAuditPending(order);
        String opinion = StringUtils.trimToNull(request == null ? null : request.getAuditOpinion());
        if (StringUtils.isBlank(opinion)) {
            throw new ServiceException("打回时请填写审核意见");
        }
        Date now = DateUtils.getNowDate();
        String currentUserName = SecurityUtils.getUsername();
        MarkOrder update = new MarkOrder();
        update.setId(orderId);
        update.setAuditStatus("3");
        update.setAuditOpinion(opinion);
        update.setAuditBy(currentUserName);
        update.setAuditTime(now);
        update.setOrderStatus("3");
        update.setUpdateBy(currentUserName);
        update.setUpdateTime(now);
        markOrderMapper.updateMarkOrder(update);
        markUserNoticeService.sendOrderAuditNotice(
                order.getUserId(),
                orderId,
                order.getOrderNo(),
                order.getPlatformName(),
                "3",
                opinion,
                currentUserName
        );
        return buildOrderDetail(orderId);
    }

    @Override
    public List<MarkOrder> selectAdminAuditOrderList(MarkOrder query) {
        if (!hasAuditReadPermission()) {
            throw new ServiceException("无权查看订单审计");
        }
        MarkOrder orderQuery = query == null ? new MarkOrder() : query;
        return markOrderMapper.selectMarkOrderList(orderQuery);
    }

    @Override
    public List<MarkWalletLog> selectAdminWalletLogList(MarkWalletLog query) {
        if (!hasAuditReadPermission()) {
            throw new ServiceException("无权查看流水审计");
        }
        MarkWalletLog walletLogQuery = query == null ? new MarkWalletLog() : query;
        return markWalletLogMapper.selectMarkWalletLogList(walletLogQuery);
    }

    @Override
    public List<MarkUserPlatformPrice> selectAgentUserPlatformPriceList(Long userId) {
        SysUser targetUser = requireUser(userId);
        assertAgentAdjustAllowed(targetUser);
        return buildPlatformPriceListByUser(targetUser.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkAgentPlatformQuotaAdjustResultVO adjustAgentUserPlatformQuota(MarkAgentPlatformQuotaAdjustRequest request) {
        if (request == null) {
            throw new ServiceException("请求参数不能为空");
        }
        SysUser targetUser = requireUser(request.getUserId());
        assertAgentAdjustAllowed(targetUser);

        String platformCode = normalizePlatformCode(request.getPlatformCode());
        if (StringUtils.isBlank(platformCode)) {
            throw new ServiceException("平台编码不能为空");
        }
        if (!isPlatformConfiguredForUser(targetUser.getUserId(), platformCode)) {
            throw new ServiceException("目标用户未配置该平台");
        }
        String adjustType = normalizeAdjustType(request.getAdjustType());
        long changeCount = request.getChangeCount() == null ? 0L : request.getChangeCount();
        if (changeCount <= 0) {
            throw new ServiceException("变动次数必须大于0");
        }

        Date now = DateUtils.getNowDate();
        String operator = SecurityUtils.getUsername();
        String platformName = resolvePlatformNameByUser(targetUser.getUserId(), platformCode, request.getPlatformName());

        MarkUserPlatformQuota quota = lockUserPlatformQuota(
                targetUser.getUserId(),
                platformCode,
                platformName,
                operator,
                now
        );
        long balanceBefore = sanitizeRemainCount(quota.getRemainCount());
        long balanceAfter;
        long changeAmount;
        if ("ADD".equals(adjustType)) {
            balanceAfter = balanceBefore + changeCount;
            changeAmount = changeCount;
        } else {
            if (balanceBefore < changeCount) {
                throw new ServiceException("当前平台剩余次数不足，无法扣减");
            }
            balanceAfter = balanceBefore - changeCount;
            changeAmount = -changeCount;
        }
        quota.setPlatformName(platformName);
        quota.setRemainCount(balanceAfter);
        quota.setUpdateBy(operator);
        quota.setUpdateTime(now);
        markUserPlatformQuotaMapper.updateMarkUserPlatformQuota(quota);

        String remark = StringUtils.defaultIfBlank(StringUtils.trimToNull(request.getRemark()), "代理平台次数调整");
        insertWalletLog(
                targetUser.getUserId(),
                null,
                null,
                platformCode,
                platformName,
                "ADJUST",
                changeAmount,
                balanceBefore,
                balanceAfter,
                remark,
                operator,
                now
        );

        MarkAgentPlatformQuotaAdjustResultVO resultVO = new MarkAgentPlatformQuotaAdjustResultVO();
        resultVO.setUserId(targetUser.getUserId());
        resultVO.setPlatformCode(platformCode);
        resultVO.setPlatformName(platformName);
        resultVO.setAdjustType(adjustType);
        resultVO.setChangeCount(changeCount);
        resultVO.setBalanceBefore(balanceBefore);
        resultVO.setBalanceAfter(balanceAfter);
        resultVO.setRemainCount(balanceAfter);
        return resultVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkUserPlatformPrice updateAgentUserPlatformStatus(MarkAgentPlatformStatusRequest request) {
        if (request == null) {
            throw new ServiceException("请求参数不能为空");
        }
        SysUser targetUser = requireUser(request.getUserId());
        assertAgentAdjustAllowed(targetUser);

        String platformCode = normalizePlatformCode(request.getPlatformCode());
        if (StringUtils.isBlank(platformCode)) {
            throw new ServiceException("平台编码不能为空");
        }
        if (!isPlatformConfiguredForUser(targetUser.getUserId(), platformCode)) {
            throw new ServiceException("目标用户未配置该平台");
        }

        String status = normalizePlatformStatus(request.getStatus());
        Date now = DateUtils.getNowDate();
        String operator = SecurityUtils.getUsername();
        String platformName = resolvePlatformNameByUser(targetUser.getUserId(), platformCode, request.getPlatformName());
        MarkUserPlatformPrice price = markUserPlatformPriceMapper.selectByUserAndPlatform(targetUser.getUserId(), platformCode);
        if (price == null) {
            price = new MarkUserPlatformPrice();
            price.setUserId(targetUser.getUserId());
            price.setPlatformCode(platformCode);
            price.setPlatformName(platformName);
            price.setUnitPrice(resolveDefaultUnitPrice(targetUser.getUserId(), platformCode));
            price.setStatus(status);
            price.setCreateBy(operator);
            price.setCreateTime(now);
            price.setUpdateBy(operator);
            price.setUpdateTime(now);
            markUserPlatformPriceMapper.insertMarkUserPlatformPrice(price);
        } else {
            price.setPlatformCode(platformCode);
            price.setPlatformName(StringUtils.defaultIfBlank(price.getPlatformName(), platformName));
            if (price.getUnitPrice() == null || price.getUnitPrice() <= 0) {
                price.setUnitPrice(resolveDefaultUnitPrice(targetUser.getUserId(), platformCode));
            }
            price.setStatus(status);
            price.setUpdateBy(operator);
            price.setUpdateTime(now);
            markUserPlatformPriceMapper.updateMarkUserPlatformPrice(price);
        }
        MarkUserPlatformPrice result = markUserPlatformPriceMapper.selectByUserAndPlatform(targetUser.getUserId(), platformCode);
        return result == null ? price : result;
    }

    private void refundOrderItem(MarkOrder order, MarkOrderItem item, String currentUserName, Date now) {
        refundOrderItem(order, item, currentUserName, now, "明细失败自动退款");
    }

    private void refundOrderItem(MarkOrder order, MarkOrderItem item, String currentUserName, Date now, String reason) {
        String platformCode = order.getPlatformCode();
        String platformName = resolvePlatformNameByUser(order.getUserId(), platformCode, order.getPlatformName());
        MarkUserPlatformQuota quota = lockUserPlatformQuota(
                order.getUserId(),
                platformCode,
                platformName,
                currentUserName,
                now
        );
        long balanceBefore = sanitizeRemainCount(quota.getRemainCount());
        long refundAmount = item.getItemAmount() == null ? 0L : item.getItemAmount();
        long balanceAfter = balanceBefore + refundAmount;
        quota.setPlatformName(platformName);
        quota.setRemainCount(balanceAfter);
        quota.setUpdateBy(currentUserName);
        quota.setUpdateTime(now);
        markUserPlatformQuotaMapper.updateMarkUserPlatformQuota(quota);

        insertWalletLog(
                order.getUserId(),
                order.getId(),
                item.getId(),
                platformCode,
                platformName,
                "REFUND",
                refundAmount,
                balanceBefore,
                balanceAfter,
                reason,
                currentUserName,
                now
        );
    }

    private void deductOrderItem(MarkOrder order, MarkOrderItem item, String currentUserName, Date now, String reason) {
        String platformCode = order.getPlatformCode();
        String platformName = resolvePlatformNameByUser(order.getUserId(), platformCode, order.getPlatformName());
        MarkUserPlatformQuota quota = lockUserPlatformQuota(
                order.getUserId(),
                platformCode,
                platformName,
                currentUserName,
                now
        );
        long balanceBefore = sanitizeRemainCount(quota.getRemainCount());
        long deductAmount = item.getItemAmount() == null ? 0L : item.getItemAmount();
        if (balanceBefore < deductAmount) {
            throw new ServiceException("用户剩余次数不足，无法重新扣费");
        }
        long balanceAfter = balanceBefore - deductAmount;
        quota.setPlatformName(platformName);
        quota.setRemainCount(balanceAfter);
        quota.setUpdateBy(currentUserName);
        quota.setUpdateTime(now);
        markUserPlatformQuotaMapper.updateMarkUserPlatformQuota(quota);

        insertWalletLog(
                order.getUserId(),
                order.getId(),
                item.getId(),
                platformCode,
                platformName,
                "DEDUCT",
                -deductAmount,
                balanceBefore,
                balanceAfter,
                reason,
                currentUserName,
                now
        );
    }

    private void refundWholeOrder(MarkOrder order, String operator, Date now, String reason) {
        List<MarkOrderItem> itemList = markOrderItemMapper.selectMarkOrderItemsByOrderId(order.getId());
        long refundAmount = 0L;
        for (MarkOrderItem item : itemList) {
            if ("1".equals(item.getRefunded())) {
                refundAmount += item.getItemAmount() == null ? 0L : item.getItemAmount();
                continue;
            }
            refundOrderItem(order, item, operator, now, reason);
            item.setRefunded("1");
            item.setUpdateBy(operator);
            item.setUpdateTime(now);
            markOrderItemMapper.updateMarkOrderItem(item);
            refundAmount += item.getItemAmount() == null ? 0L : item.getItemAmount();
        }
        MarkOrder update = new MarkOrder();
        update.setId(order.getId());
        update.setRefundAmount(refundAmount);
        update.setUpdateBy(operator);
        update.setUpdateTime(now);
        markOrderMapper.updateMarkOrder(update);
    }

    private MarkTencentSubmitResultVO createTencentUserSubmitOrder(Long userId,
                                                                   String operator,
                                                                   String phone,
                                                                   String smsCode,
                                                                   String platformCode,
                                                                   String platformName) {
        Date now = DateUtils.getNowDate();
        String resolvedPlatformName = StringUtils.defaultIfBlank(
                platformName,
                resolvePlatformNameByUser(userId, platformCode, "腾讯速解")
        );
        long unitPrice = getEffectiveUnitPrice(userId, platformCode);
        MarkUserPlatformQuota quota = lockUserPlatformQuota(
                userId,
                platformCode,
                resolvedPlatformName,
                operator,
                now
        );
        long balanceBefore = sanitizeRemainCount(quota.getRemainCount());
        assertUserPlatformQuotaSufficient(balanceBefore, unitPrice);
        long balanceAfter = balanceBefore - unitPrice;
        quota.setPlatformName(resolvedPlatformName);
        quota.setRemainCount(balanceAfter);
        quota.setUpdateBy(operator);
        quota.setUpdateTime(now);
        markUserPlatformQuotaMapper.updateMarkUserPlatformQuota(quota);

        Long assignedAgentId = resolveAssignedAgentId(userId);
        MarkOrder order = new MarkOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPlatformCode(platformCode);
        order.setPlatformName(resolvedPlatformName);
        order.setTotalCount(1);
        order.setSuccessCount(0);
        order.setFailedCount(0);
        order.setTotalAmount(unitPrice);
        order.setRefundAmount(0L);
        order.setOrderStatus("0");
        order.setAuditStatus("0");
        order.setRemark("腾讯号码待代理处理");
        if (assignedAgentId != null) {
            order.setAssignedAgentId(assignedAgentId);
        }
        order.setCreateBy(operator);
        order.setCreateTime(now);
        order.setUpdateBy(operator);
        order.setUpdateTime(now);
        markOrderMapper.insertMarkOrder(order);

        MarkOrderItem item = new MarkOrderItem();
        item.setOrderId(order.getId());
        item.setPhone(phone);
        item.setUnitPrice(unitPrice);
        item.setItemAmount(unitPrice);
        item.setProcessStatus("0");
        item.setRefunded("0");
        item.setRemark(smsCode);
        item.setCreateBy(operator);
        item.setCreateTime(now);
        item.setUpdateBy(operator);
        item.setUpdateTime(now);
        markOrderItemMapper.insertMarkOrderItem(item);

        autoPassOrderForAgentProcessing(order.getId(), assignedAgentId, operator, now);
        insertWalletLog(
                userId,
                order.getId(),
                item.getId(),
                platformCode,
                resolvedPlatformName,
                "DEDUCT",
                -unitPrice,
                balanceBefore,
                balanceAfter,
                "腾讯号码提交扣费",
                operator,
                now
        );
        markUserNoticeService.sendOrderSubmitNotice(
                userId,
                order.getId(),
                order.getOrderNo(),
                resolvedPlatformName,
                operator
        );

        MarkTencentSubmitResultVO resultVO = new MarkTencentSubmitResultVO();
        resultVO.setPhone(phone);
        resultVO.setItemId(item.getId());
        resultVO.setOrderId(order.getId());
        resultVO.setOrderNo(order.getOrderNo());
        resultVO.setProcessStatus("0");
        resultVO.setAccepted(true);
        if (isTencentPlatformCode(platformCode)) {
            scheduleTencentAutoProcess(item.getId());
        }
        return resultVO;
    }

    private void scheduleTencentAutoProcess(Long itemId) {
        if (itemId == null) {
            return;
        }
        Runnable task = () -> {
            try {
                Thread.sleep(TENCENT_AUTO_PROCESS_DELAY_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
            invokeTencentAutoProcess(itemId);
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            CompletableFuture.runAsync(task);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CompletableFuture.runAsync(task);
            }
        });
    }

    private void invokeTencentAutoProcess(Long itemId) {
        try {
            applicationContext.getBean(MarkOrderServiceImpl.class).processTencentOrderItemAuto(itemId);
        } catch (Exception ex) {
            log.warn("腾讯订单后台自动处理失败 itemId={}", itemId, ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void processTencentOrderItemAuto(Long itemId) {
        MarkOrderItem item = markOrderItemMapper.selectMarkOrderItemById(itemId);
        if (item == null || !"0".equals(StringUtils.defaultIfBlank(item.getProcessStatus(), "0"))) {
            return;
        }
        MarkOrder order = requireOrder(item.getOrderId());
        if (!isTencentPlatformCode(order.getPlatformCode())) {
            return;
        }
        String smsCode = StringUtils.trimToEmpty(item.getRemark());
        if (!smsCode.matches("\\d{6}")) {
            log.warn("腾讯订单缺少有效验证码 itemId={}", itemId);
            return;
        }

        MarkTencentSubmitResultVO apiResult = executeTencentApiSubmit(item.getPhone(), smsCode, false);
        Date now = DateUtils.getNowDate();
        boolean accepted = Boolean.TRUE.equals(apiResult.getAccepted());

        item.setProcessStatus(accepted ? "1" : "2");
        item.setProcessResult(accepted ? smsCode : buildTencentAgentSubmitFailMessage(apiResult));
        item.setProcessNote(buildTencentSubmitChainDetailText(apiResult));
        item.setProcessedBy(TENCENT_AUTO_OPERATOR);
        item.setProcessedTime(now);
        item.setUpdateBy(TENCENT_AUTO_OPERATOR);
        item.setUpdateTime(now);

        if (!accepted && !"1".equals(item.getRefunded())) {
            refundOrderItem(order, item, TENCENT_AUTO_OPERATOR, now, "腾讯后台提交失败退回");
            item.setRefunded("1");
        }

        markOrderItemMapper.updateMarkOrderItem(item);
        refreshOrderStats(order.getId(), TENCENT_AUTO_OPERATOR);
        notifyTencentProcessResult(order, item, accepted);
    }

    @Override
    public void processTdGaopinPendingItemsAuto() {
        List<MarkOrderItem> pendingItems = markOrderItemMapper.selectPendingTdGaopinProcessItems(TD_GAOPIN_AUTO_BATCH_LIMIT);
        if (pendingItems == null || pendingItems.isEmpty()) {
            return;
        }
        for (MarkOrderItem pendingItem : pendingItems) {
            if (pendingItem == null || pendingItem.getId() == null) {
                continue;
            }
            try {
                applicationContext.getBean(MarkOrderServiceImpl.class).processTdGaopinOrderItemAuto(pendingItem.getId());
            } catch (Exception ex) {
                log.warn("泰迪高频自动检测失败 itemId={}", pendingItem.getId(), ex);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void processTdGaopinOrderItemAuto(Long itemId) {
        MarkOrderItem item = markOrderItemMapper.selectMarkOrderItemById(itemId);
        if (item == null || !"0".equals(StringUtils.defaultIfBlank(item.getProcessStatus(), "0"))) {
            return;
        }
        MarkOrder order = requireOrder(item.getOrderId());
        if (!isTdGaopinPlatform(order.getPlatformCode())) {
            return;
        }
        if (!"1".equals(StringUtils.defaultIfBlank(order.getAuditStatus(), "0"))) {
            return;
        }
        if (order.getUserId() == null) {
            return;
        }
        Date now = DateUtils.getNowDate();
        SysUser orderUser;
        try {
            orderUser = requireUser(order.getUserId());
        } catch (ServiceException ex) {
            failTdGaopinItemForMissingUser(order, item, now);
            return;
        }
        MarkPhoneCheckItemVO checkItem = queryTdGaopinPhoneStatus(
                item.getPhone(),
                orderUser.getUserId(),
                orderUser.getUserName(),
                order
        );
        if (!Boolean.TRUE.equals(checkItem.getQuerySuccess())) {
            touchTdGaopinPendingItem(itemId, now);
            return;
        }
        if (!isTdGaopinAutoCompleteDetail(checkItem.getDetail())) {
            touchTdGaopinPendingItem(itemId, now);
            return;
        }

        String detail = StringUtils.defaultIfBlank(StringUtils.trimToNull(checkItem.getDetail()), "有标记");
        item.setProcessStatus("1");
        item.setProcessResult(detail);
        item.setProcessNote("自动检测：号码状态为「" + detail + "」，处理完成");
        item.setProcessedBy(TD_GAOPIN_AUTO_OPERATOR);
        item.setProcessedTime(now);
        item.setUpdateBy(TD_GAOPIN_AUTO_OPERATOR);
        item.setUpdateTime(now);
        markOrderItemMapper.updateMarkOrderItem(item);
        refreshOrderStats(order.getId(), TD_GAOPIN_AUTO_OPERATOR);
        notifyTdGaopinProcessResult(order, item, detail);
    }

    private void failTdGaopinItemForMissingUser(MarkOrder order, MarkOrderItem item, Date now) {
        item.setProcessStatus("2");
        item.setProcessResult("订单用户不存在");
        item.setProcessNote("自动检测：订单用户不存在或已删除，处理失败");
        item.setProcessedBy(TD_GAOPIN_AUTO_OPERATOR);
        item.setProcessedTime(now);
        item.setUpdateBy(TD_GAOPIN_AUTO_OPERATOR);
        item.setUpdateTime(now);
        if (!"1".equals(item.getRefunded())) {
            try {
                refundOrderItem(order, item, TD_GAOPIN_AUTO_OPERATOR, now, "订单用户不存在自动退款");
                item.setRefunded("1");
            } catch (Exception ex) {
                item.setProcessNote("自动检测：订单用户不存在或已删除，处理失败；自动退款失败");
                log.warn("泰迪高频订单用户不存在，自动退款失败 itemId={} orderId={}", item.getId(), order.getId(), ex);
            }
        }
        markOrderItemMapper.updateMarkOrderItem(item);
        refreshOrderStats(order.getId(), TD_GAOPIN_AUTO_OPERATOR);
    }

    @Override
    public void processXiaomiPendingItemsAuto() {
        List<MarkOrderItem> recheckItems = markOrderItemMapper.selectPendingXiaomiRecheckItems(XIAOMI_AUTO_BATCH_LIMIT);
        if (recheckItems == null || recheckItems.isEmpty()) {
            return;
        }
        for (MarkOrderItem pendingItem : recheckItems) {
            if (pendingItem == null || pendingItem.getId() == null) {
                continue;
            }
            try {
                applicationContext.getBean(MarkOrderServiceImpl.class).processXiaomiOrderItemAutoRecheck(pendingItem.getId());
            } catch (Exception ex) {
                log.warn("小米自动检测失败 itemId={}", pendingItem.getId(), ex);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchProcessXiaomiItems(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new ServiceException("请选择至少一条明细");
        }
        String currentUserName = SecurityUtils.getUsername();
        Date now = DateUtils.getNowDate();
        int updatedCount = 0;
        int skippedCount = 0;
        Set<Long> affectedOrderIds = new LinkedHashSet<>();
        for (Long itemId : itemIds) {
            if (itemId == null) {
                skippedCount++;
                continue;
            }
            MarkOrderItem item = markOrderItemMapper.selectMarkOrderItemById(itemId);
            if (item == null || !"0".equals(StringUtils.defaultIfBlank(item.getProcessStatus(), "0"))) {
                skippedCount++;
                continue;
            }
            MarkOrder order = requireOrder(item.getOrderId());
            assertAgentReadable(order, true);
            assertAuditPassed(order);
            if (!isXiaomiPlatform(order.getPlatformCode())) {
                skippedCount++;
                continue;
            }
            item.setProcessStatus("3");
            item.setProcessNote("代理已开启批量处理");
            item.setProcessedBy(currentUserName);
            item.setProcessedTime(now);
            item.setUpdateBy(currentUserName);
            item.setUpdateTime(now);
            markOrderItemMapper.updateMarkOrderItem(item);
            affectedOrderIds.add(order.getId());
            updatedCount++;
        }
        for (Long orderId : affectedOrderIds) {
            refreshOrderStats(orderId, currentUserName);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updatedCount", updatedCount);
        result.put("skippedCount", skippedCount);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void processXiaomiOrderItemAutoRecheck(Long itemId) {
        MarkOrderItem item = markOrderItemMapper.selectMarkOrderItemById(itemId);
        if (item == null || !"3".equals(StringUtils.defaultIfBlank(item.getProcessStatus(), "0"))) {
            return;
        }
        processXiaomiOrderItemWithQuery(item, XIAOMI_AUTO_OPERATOR);
    }

    @Override
    public Map<String, Object> batchMarkSuccessOrderItems(List<Long> itemIds) {
        return batchMarkOrderItems(itemIds, "1", "success", "代理批量标记成功");
    }

    @Override
    public Map<String, Object> batchMarkFailedOrderItems(List<Long> itemIds) {
        return batchMarkOrderItems(itemIds, "2", "failed", "代理批量标记失败");
    }

    private Map<String, Object> batchMarkOrderItems(List<Long> itemIds,
                                                   String targetStatus,
                                                   String processResult,
                                                   String processNote) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new ServiceException("请选择至少一条明细");
        }
        MarkOrderItemProcessRequest request = new MarkOrderItemProcessRequest();
        request.setProcessStatus(targetStatus);
        request.setProcessResult(processResult);
        request.setProcessNote(processNote);
        int updatedCount = 0;
        int skippedCount = 0;
        MarkOrderServiceImpl self = applicationContext.getBean(MarkOrderServiceImpl.class);
        for (Long itemId : itemIds) {
            if (itemId == null) {
                skippedCount++;
                continue;
            }
            MarkOrderItem item = markOrderItemMapper.selectMarkOrderItemById(itemId);
            if (item == null) {
                skippedCount++;
                continue;
            }
            String currentStatus = StringUtils.defaultIfBlank(item.getProcessStatus(), "0");
            if (targetStatus.equals(currentStatus)) {
                skippedCount++;
                continue;
            }
            if (!canBatchChangeStatus(currentStatus, targetStatus)) {
                skippedCount++;
                continue;
            }
            MarkOrder order = requireOrder(item.getOrderId());
            if (isTencentPlatformCode(order.getPlatformCode())) {
                skippedCount++;
                continue;
            }
            try {
                assertAgentReadable(order, true);
                assertAuditPassed(order);
                self.feedbackOrderItem(itemId, request);
                updatedCount++;
            } catch (ServiceException ex) {
                skippedCount++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updatedCount", updatedCount);
        result.put("skippedCount", skippedCount);
        return result;
    }

    private boolean canBatchChangeStatus(String currentStatus, String targetStatus) {
        if ("1".equals(targetStatus)) {
            return "0".equals(currentStatus) || "2".equals(currentStatus) || "3".equals(currentStatus);
        }
        if ("2".equals(targetStatus)) {
            return "0".equals(currentStatus) || "1".equals(currentStatus) || "3".equals(currentStatus);
        }
        return false;
    }

    private void processXiaomiOrderItemWithQuery(MarkOrderItem item) {
        processXiaomiOrderItemWithQuery(item, XIAOMI_AUTO_OPERATOR);
    }

    private void processXiaomiOrderItemWithQuery(MarkOrderItem item, String operator) {
        MarkOrderItem latest = markOrderItemMapper.selectMarkOrderItemById(item.getId());
        if (latest == null || !"3".equals(StringUtils.defaultIfBlank(latest.getProcessStatus(), "0"))) {
            return;
        }
        item = latest;
        MarkOrder order = requireOrder(item.getOrderId());
        if (!isXiaomiPlatform(order.getPlatformCode())) {
            return;
        }
        if (!"1".equals(StringUtils.defaultIfBlank(order.getAuditStatus(), "0"))) {
            return;
        }
        if (order.getUserId() == null) {
            return;
        }
        SysUser orderUser = requireUser(order.getUserId());
        MarkPhoneCheckItemVO checkItem = queryXiaomiPhoneStatus(
                item.getPhone(),
                orderUser.getUserId(),
                orderUser.getUserName(),
                order
        );
        Date now = DateUtils.getNowDate();
        if (!Boolean.TRUE.equals(checkItem.getQuerySuccess())) {
            touchXiaomiPendingItem(item.getId(), operator, now);
            return;
        }
        if (isXiaomiNoMark(checkItem)) {
            item.setProcessStatus("1");
            item.setProcessResult("无标记");
            item.setProcessNote("自动检测：号码已无标记，处理完成");
            item.setProcessedBy(operator);
            item.setProcessedTime(now);
            item.setUpdateBy(operator);
            item.setUpdateTime(now);
            markOrderItemMapper.updateMarkOrderItem(item);
            refreshOrderStats(order.getId(), operator);
            return;
        }
        if (isXiaomiHasMark(checkItem)) {
            String detail = StringUtils.defaultIfBlank(StringUtils.trimToNull(checkItem.getDetail()), "有标记");
            item.setProcessStatus("3");
            item.setProcessResult("有标记:" + detail);
            item.setProcessNote("自动检测：仍有标记，继续等待");
            item.setUpdateBy(operator);
            item.setUpdateTime(now);
            markOrderItemMapper.updateMarkOrderItem(item);
            return;
        }
        touchXiaomiPendingItem(item.getId(), operator, now);
    }

    private void touchXiaomiPendingItem(Long itemId, Date now) {
        touchXiaomiPendingItem(itemId, XIAOMI_AUTO_OPERATOR, now);
    }

    private void touchXiaomiPendingItem(Long itemId, String operator, Date now) {
        if (itemId == null) {
            return;
        }
        MarkOrderItem touch = new MarkOrderItem();
        touch.setId(itemId);
        touch.setUpdateBy(operator);
        touch.setUpdateTime(now);
        markOrderItemMapper.updateMarkOrderItem(touch);
    }

    private MarkPhoneCheckItemVO queryXiaomiPhoneStatus(String phone,
                                                        Long userId,
                                                        String userName,
                                                        MarkOrder order) {
        MarkPhoneCheckItemVO item = new MarkPhoneCheckItemVO();
        item.setPhone(phone);
        item.setQuerySuccess(false);
        try {
            Map<String, Object> queryResult = executeMarkPrecheckSingleQuery(
                    phone,
                    userId,
                    userName,
                    order.getPlatformCode(),
                    order.getPlatformName()
            );
            int code = parseInt(queryResult == null ? null : queryResult.get("code"), -1);
            if (code != 0) {
                String errorMessage = asString(queryResult == null ? null : queryResult.get("message"));
                item.setDetail(StringUtils.defaultIfBlank(errorMessage, "查询失败"));
                return item;
            }
            item.setQuerySuccess(true);
            fillPrecheckItemFromData(item, queryResult.get("data"));
            applyXiaomiPrecheckRules(item);
            if (StringUtils.isBlank(item.getDetail())) {
                item.setDetail(normalizeTeddyMarkDetailLabel(StringUtils.defaultIfBlank(item.getStatus(), "未知")));
            }
        } catch (Exception ex) {
            item.setDetail(StringUtils.defaultIfBlank(ex.getMessage(), "查询失败"));
        }
        return item;
    }

    private void applyXiaomiPrecheckRules(MarkPhoneCheckItemVO item) {
        if (item == null) {
            return;
        }
        String status = StringUtils.trimToNull(item.getStatus());
        if (StringUtils.isNotBlank(status)) {
            String normalizedStatus = status.trim().toLowerCase();
            if ("no".equals(normalizedStatus) || normalizedStatus.startsWith("no-")) {
                item.setMarked(false);
                item.setDetail("无");
                return;
            }
            if ("yes".equals(normalizedStatus) || normalizedStatus.startsWith("yes")) {
                item.setMarked(true);
                item.setDetail("有标记");
                return;
            }
        }
        String detail = normalizeTeddyMarkDetailLabel(StringUtils.trimToNull(item.getDetail()));
        if ("无".equals(detail) || "无标记".equals(detail) || "未标记".equals(detail)) {
            item.setMarked(false);
            item.setDetail("无");
            return;
        }
        if (Boolean.TRUE.equals(item.getMarked()) || "有标记".equals(detail)) {
            item.setMarked(true);
            item.setDetail(StringUtils.defaultIfBlank(detail, "有标记"));
        }
    }

    private boolean isXiaomiNoMark(MarkPhoneCheckItemVO item) {
        if (item == null) {
            return false;
        }
        if (Boolean.FALSE.equals(item.getMarked())) {
            return true;
        }
        String detail = normalizeTeddyMarkDetailLabel(StringUtils.trimToNull(item.getDetail()));
        return "无".equals(detail) || "无标记".equals(detail) || "未标记".equals(detail);
    }

    private boolean isXiaomiHasMark(MarkPhoneCheckItemVO item) {
        if (item == null) {
            return false;
        }
        if (Boolean.TRUE.equals(item.getMarked())) {
            return true;
        }
        String detail = normalizeTeddyMarkDetailLabel(StringUtils.trimToNull(item.getDetail()));
        return "有标记".equals(detail) || (detail != null && detail.contains("有标记"));
    }

    private boolean isXiaomiPlatform(String platformCode) {
        return "xiaomi".equalsIgnoreCase(StringUtils.trimToNull(platformCode));
    }

    private void touchTdGaopinPendingItem(Long itemId, Date now) {
        if (itemId == null) {
            return;
        }
        MarkOrderItem touch = new MarkOrderItem();
        touch.setId(itemId);
        touch.setUpdateBy(TD_GAOPIN_AUTO_OPERATOR);
        touch.setUpdateTime(now);
        markOrderItemMapper.updateMarkOrderItem(touch);
    }

    private void notifyTdGaopinProcessResult(MarkOrder order, MarkOrderItem item, String detail) {
        if (order == null || item == null || order.getUserId() == null) {
            return;
        }
        markUserNoticeService.sendTdGaopinProcessNotice(
                order.getUserId(),
                order.getId(),
                order.getOrderNo(),
                item.getPhone(),
                detail,
                TD_GAOPIN_AUTO_OPERATOR
        );
    }

    private void assertTdGaopinOrderPhonesValid(Long userId,
                                                String userName,
                                                String platformCode,
                                                String platformName,
                                                List<String> phones) {
        if (!isTdGaopinPlatform(platformCode) || phones == null || phones.isEmpty()) {
            return;
        }
        Set<String> uniquePhones = new LinkedHashSet<>();
        List<String> duplicateInBatch = new ArrayList<>();
        for (String phone : phones) {
            if (!uniquePhones.add(phone)) {
                duplicateInBatch.add(phone);
            }
        }
        if (!duplicateInBatch.isEmpty()) {
            throw new ServiceException("以下号码重复提交：" + String.join("、", duplicateInBatch));
        }

        List<String> pendingPhones = markOrderItemMapper.selectUserPendingTdGaopinPhones(userId, phones);
        if (pendingPhones != null && !pendingPhones.isEmpty()) {
            throw new ServiceException("以下号码已有待处理泰迪高频订单：" + String.join("、", pendingPhones));
        }

        List<String> rejectedPhones = new ArrayList<>();
        for (String phone : phones) {
            MarkPhoneCheckItemVO item = new MarkPhoneCheckItemVO();
            item.setPhone(phone);
            try {
                Map<String, Object> queryResult = executeMarkPrecheckSingleQuery(
                        phone,
                        userId,
                        userName,
                        platformCode,
                        platformName
                );
                int code = parseInt(queryResult == null ? null : queryResult.get("code"), -1);
                if (code != 0) {
                    rejectedPhones.add(phone + "(查询失败)");
                    continue;
                }
                item.setQuerySuccess(true);
                fillPrecheckItemFromData(item, queryResult.get("data"));
                applyTdGaopinPrecheckRules(item);
                if (!isTdGaopinSubmittableRawDetail(extractPrecheckRawDetail(item))) {
                    rejectedPhones.add(phone);
                }
            } catch (Exception ex) {
                rejectedPhones.add(phone + "(查询失败)");
            }
        }
        if (!rejectedPhones.isEmpty()) {
            throw new ServiceException("以下号码不是「泰迪熊高频」结果，无法提交：" + String.join("、", rejectedPhones));
        }
    }

    private void assertMobileGaopinOrderPhonesValid(Long userId,
                                                    String userName,
                                                    String platformCode,
                                                    String platformName,
                                                    List<String> phones) {
        if (!isMobileGaopinPlatform(platformCode) || phones == null || phones.isEmpty()) {
            return;
        }
        List<String> rejectedPhones = new ArrayList<>();
        for (String phone : phones) {
            MarkPhoneCheckItemVO item = new MarkPhoneCheckItemVO();
            item.setPhone(phone);
            try {
                Map<String, Object> queryResult = executeMarkPrecheckSingleQuery(
                        phone,
                        userId,
                        userName,
                        platformCode,
                        platformName
                );
                int code = parseInt(queryResult == null ? null : queryResult.get("code"), -1);
                if (code != 0) {
                    rejectedPhones.add(phone + "(查询失败)");
                    continue;
                }
                item.setQuerySuccess(true);
                fillPrecheckItemFromData(item, queryResult.get("data"));
                applyMobileGaopinPrecheckRules(item);
                if (!isMobileGaopinSubmittableItem(item)) {
                    rejectedPhones.add(phone);
                }
            } catch (Exception ex) {
                rejectedPhones.add(phone + "(查询失败)");
            }
        }
        if (!rejectedPhones.isEmpty()) {
            throw new ServiceException("以下号码未显示「有标记」，无法提交：" + String.join("、", rejectedPhones));
        }
    }

    private MarkPhoneCheckItemVO queryTdGaopinPhoneStatus(String phone,
                                                          Long userId,
                                                          String userName,
                                                          MarkOrder order) {
        MarkPhoneCheckItemVO item = new MarkPhoneCheckItemVO();
        item.setPhone(phone);
        item.setQuerySuccess(false);
        try {
            Map<String, Object> queryResult = executeMarkPrecheckSingleQuery(
                    phone,
                    userId,
                    userName,
                    order.getPlatformCode(),
                    order.getPlatformName()
            );
            int code = parseInt(queryResult == null ? null : queryResult.get("code"), -1);
            if (code != 0) {
                String errorMessage = asString(queryResult == null ? null : queryResult.get("message"));
                item.setDetail(StringUtils.defaultIfBlank(errorMessage, "查询失败"));
                return item;
            }
            item.setQuerySuccess(true);
            fillPrecheckItemFromData(item, queryResult.get("data"));
            applyTdGaopinPrecheckRules(item);
            if (StringUtils.isBlank(item.getDetail())) {
                item.setDetail(normalizeTeddyMarkDetailLabel(StringUtils.defaultIfBlank(item.getStatus(), "未知")));
            }
        } catch (Exception ex) {
            item.setDetail(StringUtils.defaultIfBlank(ex.getMessage(), "查询失败"));
        }
        return item;
    }

    private boolean isTdGaopinAutoCompleteDetail(String detail) {
        String normalized = StringUtils.trimToNull(detail);
        return "有标记".equals(normalized)
                || StringUtils.contains(normalized, TD_GAOPIN_DUPLICATE_APPEAL_KEY);
    }

    private void notifyTencentProcessResult(MarkOrder order, MarkOrderItem item, boolean accepted) {
        if (order == null || item == null || order.getUserId() == null) {
            return;
        }
        String failMessage = buildTencentUserSubmitFailMessage(item.getProcessResult());
        markUserNoticeService.sendTencentProcessNotice(
                order.getUserId(),
                order.getId(),
                order.getOrderNo(),
                item.getPhone(),
                accepted,
                failMessage,
                TENCENT_AUTO_OPERATOR
        );
    }

    private String buildTencentUserSubmitFailMessage(String processResult) {
        String message = StringUtils.trimToNull(processResult);
        if (StringUtils.contains(message, "验证码")) {
            return "提交失败，验证码错误或者失效";
        }
        return "提交失败，验证码错误或者失效";
    }

    private MarkTencentSubmitResultVO executeTencentApiSubmit(String phone, String smsCode, boolean forceTamper) {
        MarkTencentSubmitResultVO resultVO = new MarkTencentSubmitResultVO();
        resultVO.setPhone(phone);

        Map<String, Object> phoneTypeResponse = callTencentJsonp(
                "/core/sjg/moblie_phone_type",
                Map.of("phone", phone)
        );
        if (phoneTypeResponse == null) {
            throw new ServiceException("无法获取 phone_type");
        }
        resultVO.setPhoneTypeResponse(phoneTypeResponse);
        Integer originalPhoneType = parseNullableInt(phoneTypeResponse.get("data"));
        resultVO.setOriginalPhoneType(originalPhoneType);
        int submittedPhoneType = forceTamper
                ? 2
                : ((originalPhoneType != null && (originalPhoneType == 1 || originalPhoneType == 2))
                ? originalPhoneType
                : 2);
        resultVO.setSubmittedPhoneType(submittedPhoneType);

        Map<String, Object> complainStatusResponse = callTencentJsonp(
                "/core/sjg/phone_complain_status",
                Map.of("phone", phone)
        );
        resultVO.setComplainStatusResponse(complainStatusResponse);
        if (complainStatusResponse != null) {
            resultVO.setComplainStatus(asString(complainStatusResponse.get("data")));
        }

        Map<String, Object> verifyResponse = callTencentJsonp(
                "/core/txwz/get_phone_type",
                Map.of("phone", phone, "code", smsCode)
        );
        resultVO.setVerifyResponse(verifyResponse);
        if (verifyResponse != null) {
            resultVO.setVerifyReCode(parseNullableInt(verifyResponse.get("reCode")));
            resultVO.setVerifyData(asString(verifyResponse.get("data")));
        }

        Map<String, Object> submitResponse = callTencentJsonp(
                "/core/txwz/complian_phone",
                Map.of("phone", phone, "phone_type", submittedPhoneType, "code", smsCode, "src", 2)
        );
        if (submitResponse == null) {
            throw new ServiceException("提交请求失败");
        }
        resultVO.setSubmitResponse(submitResponse);
        Integer submitReCode = parseNullableInt(submitResponse.get("reCode"));
        resultVO.setSubmitReCode(submitReCode);
        resultVO.setSubmitData(asString(submitResponse.get("data")));
        resultVO.setAccepted(submitReCode != null && submitReCode == 0);
        return resultVO;
    }

    private String buildTencentAgentSubmitFailMessage(MarkTencentSubmitResultVO resultVO) {
        if (resultVO == null) {
            return "腾讯提交失败，验证码错误或者失效";
        }
        String submitData = StringUtils.trimToNull(resultVO.getSubmitData());
        if (StringUtils.contains(submitData, "验证码")) {
            return "腾讯提交失败，验证码错误或者失效";
        }
        return buildTencentRecordText("腾讯提交失败", resultVO.getSubmitReCode(), submitData);
    }

    private String buildTencentRecordText(String prefix, Integer reCode, String data) {
        String safePrefix = StringUtils.defaultIfBlank(prefix, "腾讯提交");
        String codeText = reCode == null ? "-" : String.valueOf(reCode);
        String dataText = StringUtils.defaultIfBlank(StringUtils.trimToNull(data), "-");
        return StringUtils.abbreviate(safePrefix + "（reCode=" + codeText + "，data=" + dataText + "）", 200);
    }

    private String buildTencentSubmitChainDetailText(MarkTencentSubmitResultVO resultVO) {
        if (resultVO == null) {
            return "-";
        }
        return Boolean.TRUE.equals(resultVO.getAccepted()) ? "腾讯提交成功" : "腾讯提交失败";
    }

    private String buildTdxSecondProcessNote(MarkTdxSecondSubmitResultVO resultVO, String line) {
        if (resultVO == null) {
            return "TDX二次申诉成功";
        }
        StringBuilder note = new StringBuilder("TDX二次申诉成功");
        if (resultVO.getTdxId() != null) {
            note.append("｜tdxId=").append(resultVO.getTdxId());
        }
        if (StringUtils.isNotBlank(resultVO.getOrderpicinumber())) {
            note.append("｜orderpicinumber=").append(resultVO.getOrderpicinumber());
        }
        note.append("｜line=").append(StringUtils.defaultIfBlank(StringUtils.trimToNull(line), "line1"));
        return StringUtils.abbreviate(note.toString(), 500);
    }

    private String buildTencentRealtimeStatusText(Integer phoneType, String complainStatus) {
        String typeText = phoneType == null ? "-" : String.valueOf(phoneType);
        String complainText = StringUtils.defaultIfBlank(StringUtils.trimToNull(complainStatus), "-");
        return "phone_type=" + typeText + "｜申诉状态=" + complainText;
    }

    private boolean isTencentPhoneMarked(Integer phoneType, String complainStatus) {
        String status = StringUtils.trimToNull(complainStatus);
        if ("0".equals(status)) {
            return false;
        }
        if (StringUtils.isNotBlank(status) && !"-".equals(status)) {
            return true;
        }
        return phoneType != null && (phoneType == 1 || phoneType == 2);
    }

    private void refreshOrderStats(Long orderId, String updateBy) {
        List<MarkOrderItem> itemList = markOrderItemMapper.selectMarkOrderItemsByOrderId(orderId);
        int successCount = 0;
        int failedCount = 0;
        long refundAmount = 0L;
        for (MarkOrderItem item : itemList) {
            if ("1".equals(item.getProcessStatus())) {
                successCount++;
            } else if ("2".equals(item.getProcessStatus())) {
                failedCount++;
            }
            if ("1".equals(item.getRefunded())) {
                refundAmount += item.getItemAmount() == null ? 0L : item.getItemAmount();
            }
        }
        int totalCount = itemList.size();
        int processedCount = successCount + failedCount;
        String orderStatus = "0";
        Date completedTime = null;
        if (processedCount > 0 && processedCount < totalCount) {
            orderStatus = "1";
        } else if (processedCount == totalCount && totalCount > 0) {
            orderStatus = "2";
            completedTime = DateUtils.getNowDate();
        }

        MarkOrder update = new MarkOrder();
        update.setId(orderId);
        update.setSuccessCount(successCount);
        update.setFailedCount(failedCount);
        update.setRefundAmount(refundAmount);
        update.setOrderStatus(orderStatus);
        update.setCompletedTime(completedTime);
        update.setUpdateBy(updateBy);
        update.setUpdateTime(DateUtils.getNowDate());
        markOrderMapper.updateMarkOrder(update);
    }

    private void assertAgentReadable(MarkOrder order, boolean lockWhenUnassigned) {
        if (isAdminRole()) {
            return;
        }
        if (!isAgentRole()) {
            throw new ServiceException("仅代理或管理员可操作");
        }
        Long currentUserId = SecurityUtils.getUserId();
        String currentUsername = SecurityUtils.getUsername();
        SysUser currentAgent = requireUser(currentUserId);
        SysUser orderUser = requireUser(order.getUserId());
        if (!canAgentAccessOrder(currentAgent, orderUser, order)) {
            throw new ServiceException("无权处理该订单");
        }
        if (lockWhenUnassigned && (order.getAssignedAgentId() == null || !currentUserId.equals(order.getAssignedAgentId()))) {
            MarkOrder lockOrder = new MarkOrder();
            lockOrder.setId(order.getId());
            lockOrder.setAssignedAgentId(currentUserId);
            lockOrder.setUpdateBy(currentUsername);
            lockOrder.setUpdateTime(DateUtils.getNowDate());
            markOrderMapper.updateMarkOrder(lockOrder);
            order.setAssignedAgentId(currentUserId);
        }
    }

    private boolean canAgentAccessOrder(SysUser agent, SysUser orderUser, MarkOrder order) {
        if (agent == null || orderUser == null || order == null) {
            return false;
        }
        if (order.getAssignedAgentId() != null && order.getAssignedAgentId().equals(agent.getUserId())) {
            return true;
        }
        if (StringUtils.equals(StringUtils.trimToEmpty(orderUser.getCreateBy()), agent.getUserName())) {
            return true;
        }
        if (sameMarkTemplate(agent, orderUser)) {
            return true;
        }
        if (isTemplateOwnerAgent(agent, orderUser)) {
            return true;
        }
        if (order.getAssignedAgentId() == null) {
            return false;
        }
        SysUser assignedAgent = sysUserMapper.selectOneById(order.getAssignedAgentId());
        return assignedAgent != null && sameMarkTemplate(agent, orderUser) && !isAgentAccount(assignedAgent);
    }

    private boolean sameMarkTemplate(SysUser left, SysUser right) {
        Long leftTemplate = left == null ? null : left.getRelMarkTemplate();
        Long rightTemplate = right == null ? null : right.getRelMarkTemplate();
        return leftTemplate != null && leftTemplate > 0 && leftTemplate.equals(rightTemplate);
    }

    private boolean isAgentAccount(SysUser user) {
        if (user == null || user.getUserId() == null) {
            return false;
        }
        Long agentUserId = markOrderMapper.selectAgentUserIdByMarkTemplate(
                user.getRelMarkTemplate() == null ? -1L : user.getRelMarkTemplate());
        return agentUserId != null && agentUserId.equals(user.getUserId());
    }

    private Long resolveAssignedAgentId(Long userId) {
        SysUser user = requireUser(userId);
        Long fromCreator = resolveAgentUserIdByUsername(user.getCreateBy());
        if (fromCreator != null) {
            return fromCreator;
        }
        if (user.getRelMarkTemplate() == null || user.getRelMarkTemplate() <= 0) {
            return null;
        }
        Long fromTemplate = markOrderMapper.selectAgentUserIdByMarkTemplate(user.getRelMarkTemplate());
        if (fromTemplate != null) {
            return fromTemplate;
        }
        return resolveAgentUserIdByTemplateOwner(user.getRelMarkTemplate());
    }

    private Long resolveAgentUserIdByTemplateOwner(Long templateId) {
        if (templateId == null || templateId <= 0) {
            return null;
        }
        MarkPlatformTemplate template = markPlatformTemplateMapper.selectMarkPlatformTemplateById(templateId);
        if (template == null || template.getOwnerUserId() == null) {
            return null;
        }
        return isAgentAccount(requireUser(template.getOwnerUserId())) ? template.getOwnerUserId() : null;
    }

    private boolean isTemplateOwnerAgent(SysUser agent, SysUser orderUser) {
        if (agent == null || orderUser == null || agent.getUserId() == null) {
            return false;
        }
        Long templateId = orderUser.getRelMarkTemplate();
        if (templateId == null || templateId <= 0) {
            return false;
        }
        MarkPlatformTemplate template = markPlatformTemplateMapper.selectMarkPlatformTemplateById(templateId);
        return template != null && agent.getUserId().equals(template.getOwnerUserId());
    }

    private Long resolveAgentUserIdByUsername(String username) {
        String agentUsername = StringUtils.trimToEmpty(username);
        if (StringUtils.isBlank(agentUsername)) {
            return null;
        }
        SysUser candidate = sysUserMapper.selectOneByQuery(
                QueryWrapper.create().from(SysUser.class).eq(SysUser::getUserName, agentUsername));
        if (candidate == null || candidate.getUserId() == null) {
            return null;
        }
        Long matchedAgentId = markOrderMapper.selectAgentUserIdByMarkTemplate(
                candidate.getRelMarkTemplate() == null ? -1L : candidate.getRelMarkTemplate());
        if (matchedAgentId != null && matchedAgentId.equals(candidate.getUserId())) {
            return candidate.getUserId();
        }
        return null;
    }

    private void autoPassOrderForAgentProcessing(Long orderId, Long assignedAgentId, String operator, Date now) {
        if (orderId == null) {
            return;
        }
        MarkOrder update = new MarkOrder();
        update.setId(orderId);
        update.setAuditStatus("1");
        update.setAuditOpinion("用户提交自动审核");
        update.setAuditBy(operator);
        update.setAuditTime(now);
        update.setOrderStatus("1");
        if (assignedAgentId != null) {
            update.setAssignedAgentId(assignedAgentId);
        }
        update.setUpdateBy(operator);
        update.setUpdateTime(now);
        markOrderMapper.updateMarkOrder(update);
    }

    private void assertAuditPending(MarkOrder order) {
        if (!"0".equals(StringUtils.defaultIfBlank(order.getAuditStatus(), "0"))) {
            throw new ServiceException("订单不在待审核状态");
        }
    }

    private void assertAuditPassed(MarkOrder order) {
        if (!"1".equals(StringUtils.defaultIfBlank(order.getAuditStatus(), "1"))) {
            throw new ServiceException("订单尚未审核通过，无法处理");
        }
    }

    private MarkOrder requireOrder(Long orderId) {
        MarkOrder order = markOrderMapper.selectMarkOrderById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        return order;
    }

    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectOneById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return user;
    }

    private MarkOrderDetailVO buildOrderDetail(Long orderId) {
        MarkOrderDetailVO vo = new MarkOrderDetailVO();
        MarkOrder order = requireOrder(orderId);
        List<MarkOrderItem> itemList = markOrderItemMapper.selectMarkOrderItemsByOrderId(orderId);
        String preview = itemList.stream()
                .map(MarkOrderItem::getPhone)
                .filter(StringUtils::isNotBlank)
                .limit(5)
                .collect(Collectors.joining("、"));
        order.setPhonePreview(StringUtils.isBlank(preview) ? "-" : preview);
        vo.setOrder(order);
        vo.setItems(itemList);
        return vo;
    }

    private long getEffectiveUnitPrice(Long userId, String platformCode) {
        String normalizedCode = normalizePlatformCode(platformCode);
        MarkUserPlatformPrice platformPrice = markUserPlatformPriceMapper.selectByUserAndPlatform(userId, normalizedCode);
        if (platformPrice == null || platformPrice.getUnitPrice() == null || platformPrice.getUnitPrice() <= 0) {
            Map<String, TemplatePlatformConfig> templateConfigMap = resolveTemplatePlatformConfigMapByUser(userId);
            TemplatePlatformConfig templateConfig = templateConfigMap.get(normalizedCode);
            if (templateConfig == null || templateConfig.getUnitPrice() == null || templateConfig.getUnitPrice() <= 0) {
                return 1L;
            }
            return templateConfig.getUnitPrice();
        }
        return platformPrice.getUnitPrice();
    }

    private List<MarkUserPlatformPrice> buildPlatformPriceListByUser(Long userId) {
        MarkUserPlatformPrice query = new MarkUserPlatformPrice();
        query.setUserId(userId);
        List<MarkUserPlatformPrice> savedPrices = markUserPlatformPriceMapper.selectMarkUserPlatformPriceList(query);
        Map<String, MarkUserPlatformPrice> savedMap = savedPrices.stream()
                .filter(item -> StringUtils.isNotBlank(item.getPlatformCode()))
                .collect(Collectors.toMap(item -> normalizePlatformCode(item.getPlatformCode()), item -> item, (a, b) -> a, LinkedHashMap::new));
        List<MarkUserPlatformQuota> quotas = markUserPlatformQuotaMapper.selectByUserId(userId);
        Map<String, Long> quotaMap = new LinkedHashMap<>();
        for (MarkUserPlatformQuota quota : quotas) {
            if (quota == null || StringUtils.isBlank(quota.getPlatformCode())) {
                continue;
            }
            quotaMap.put(normalizePlatformCode(quota.getPlatformCode()), sanitizeRemainCount(quota.getRemainCount()));
        }
        Set<String> explicitlyConfiguredCodes = new LinkedHashSet<>(savedMap.keySet());
        SysUser currentUser = requireUser(userId);
        Long boundTemplateId = currentUser.getRelMarkTemplate();
        Map<String, String> dynamicPlatformNameMap = resolveMenuPlatformNameMap();
        Map<String, String> platformNameMap = resolvePlatformNameMap(dynamicPlatformNameMap);
        Map<String, TemplatePlatformConfig> templateConfigMap = resolveTemplatePlatformConfigMapByUser(currentUser);
        Set<String> availableCodes = resolveAvailablePlatformCodes(
                boundTemplateId != null,
                explicitlyConfiguredCodes,
                dynamicPlatformNameMap,
                templateConfigMap
        );
        log.info("mark platform candidates resolved userId={}, boundTemplateId={}, menuCodes={}, templateCodes={}, explicitCodes={}, availableCodes={}",
                userId,
                boundTemplateId,
                dynamicPlatformNameMap.keySet(),
                templateConfigMap.keySet(),
                explicitlyConfiguredCodes,
                availableCodes);
        List<MarkUserPlatformPrice> result = new ArrayList<>();
        for (String platformCode : availableCodes) {
            platformCode = normalizePlatformCode(platformCode);
            if (StringUtils.isBlank(platformCode)) {
                continue;
            }
            MarkUserPlatformPrice price = savedMap.get(platformCode);
            TemplatePlatformConfig templateConfig = templateConfigMap.get(platformCode);
            String platformName = resolvePlatformNameByMap(platformNameMap, platformCode);
            if (templateConfig != null && StringUtils.isNotBlank(templateConfig.getPlatformName())) {
                platformName = templateConfig.getPlatformName();
            }
            long defaultUnitPrice = (templateConfig != null && templateConfig.getUnitPrice() != null && templateConfig.getUnitPrice() > 0)
                    ? templateConfig.getUnitPrice()
                    : 1L;
            if (price == null) {
                price = new MarkUserPlatformPrice();
                price.setUserId(userId);
                price.setPlatformCode(platformCode);
                price.setPlatformName(platformName);
                price.setUnitPrice(defaultUnitPrice);
            } else if (StringUtils.isBlank(price.getPlatformName())) {
                price.setPlatformName(platformName);
            } else if (price.getUnitPrice() == null || price.getUnitPrice() <= 0) {
                price.setUnitPrice(defaultUnitPrice);
            }
            if (StringUtils.isBlank(price.getStatus())) {
                price.setStatus("0");
            }
            price.setRemainCount(sanitizeRemainCount(quotaMap.get(normalizePlatformCode(platformCode))));
            result.add(price);
        }
        return result;
    }

    private Set<String> resolveAvailablePlatformCodes(boolean hasBoundTemplate,
                                                      Set<String> explicitlyConfiguredCodes,
                                                      Map<String, String> dynamicPlatformNameMap,
                                                      Map<String, TemplatePlatformConfig> templateConfigMap) {
        Set<String> availableCodes = new LinkedHashSet<>();
        if (hasBoundTemplate) {
            if (templateConfigMap != null && !templateConfigMap.isEmpty()) {
                availableCodes.addAll(templateConfigMap.keySet());
            }
            availableCodes.removeIf(StringUtils::isBlank);
            return availableCodes;
        }
        if (dynamicPlatformNameMap != null && !dynamicPlatformNameMap.isEmpty()) {
            availableCodes.addAll(dynamicPlatformNameMap.keySet());
        }
        if (templateConfigMap != null && !templateConfigMap.isEmpty()) {
            availableCodes.addAll(templateConfigMap.keySet());
        }
        if (explicitlyConfiguredCodes != null && !explicitlyConfiguredCodes.isEmpty()) {
            availableCodes.addAll(explicitlyConfiguredCodes);
        }
        availableCodes.removeIf(StringUtils::isBlank);
        if (!availableCodes.isEmpty()) {
            return availableCodes;
        }
        return new LinkedHashSet<>(LEGACY_PLATFORM_NAME_MAP.keySet());
    }
    private Map<String, TemplatePlatformConfig> resolveTemplatePlatformConfigMapByUser(Long userId) {
        if (userId == null) {
            return new LinkedHashMap<>();
        }
        return resolveTemplatePlatformConfigMapByUser(requireUser(userId));
    }

    private Map<String, TemplatePlatformConfig> resolveTemplatePlatformConfigMapByUser(SysUser user) {
        Map<String, TemplatePlatformConfig> configMap = new LinkedHashMap<>();
        if (user == null || user.getUserId() == null) {
            return configMap;
        }
        Long userId = user.getUserId();
        Long templateId = user.getRelMarkTemplate();
        if (templateId == null) {
            log.info("mark template not bound, fallback to menu platforms. userId={}", userId);
            return configMap;
        }
        MarkPlatformTemplate template = markPlatformTemplateMapper.selectMarkPlatformTemplateById(templateId);
        if (template == null || !"0".equals(template.getStatus())) {
            log.warn("mark template unavailable, fallback to menu platforms. userId={}, templateId={}", userId, templateId);
            return configMap;
        }
        Map<String, TemplatePlatformConfig> parsedConfigMap = parseTemplatePlatformConfigs(template.getTemplateInfo());
        log.info("mark template parsed for platform availability. userId={}, templateId={}, platformCodes={}",
                userId, templateId, parsedConfigMap.keySet());
        return parsedConfigMap;
    }

    private Map<String, TemplatePlatformConfig> parseTemplatePlatformConfigs(String templateInfo) {
        Map<String, TemplatePlatformConfig> configMap = new LinkedHashMap<>();
        if (StringUtils.isBlank(templateInfo)) {
            return configMap;
        }
        try {
            List<Object> rawList = objectMapper.readValue(templateInfo, new TypeReference<List<Object>>() {});
            for (Object item : rawList) {
                addTemplatePlatformConfig(configMap, item);
            }
        } catch (Exception e) {
            log.warn("mark template parse failed, fallback to menu platforms. templateInfoLength={}",
                    templateInfo.length(), e);
            return new LinkedHashMap<>();
        }
        return configMap;
    }

    private void addTemplatePlatformConfig(Map<String, TemplatePlatformConfig> target, Object item) {
        if (item == null) {
            return;
        }
        String code = null;
        String platformName = null;
        Long unitPrice = null;
        if (item instanceof String) {
            code = StringUtils.trimToEmpty((String) item);
        } else if (item instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) item;
            code = firstNonBlank(
                    asString(map.get("platformCode")),
                    asString(map.get("code")),
                    asString(map.get("value"))
            );
            platformName = firstNonBlank(
                    asString(map.get("platformName")),
                    asString(map.get("name")),
                    asString(map.get("label"))
            );
            unitPrice = parseLong(map.get("unitPrice"));
        }
        String normalizedCode = normalizePlatformCode(code);
        if (StringUtils.isBlank(normalizedCode)) {
            return;
        }
        if (unitPrice == null || unitPrice <= 0) {
            unitPrice = 1L;
        }
        TemplatePlatformConfig existing = target.get(normalizedCode);
        if (existing == null) {
            target.put(normalizedCode, new TemplatePlatformConfig(normalizedCode, StringUtils.trimToNull(platformName), unitPrice));
            return;
        }
        if (StringUtils.isBlank(existing.getPlatformName()) && StringUtils.isNotBlank(platformName)) {
            existing.setPlatformName(StringUtils.trimToNull(platformName));
        }
        if (existing.getUnitPrice() == null || existing.getUnitPrice() <= 0) {
            existing.setUnitPrice(unitPrice);
        }
    }

    private boolean isPlatformAvailableForUser(Long userId, String platformCode) {
        String normalizedCode = normalizePlatformCode(platformCode);
        if (StringUtils.isBlank(normalizedCode)) {
            return false;
        }
        List<MarkUserPlatformPrice> prices = buildPlatformPriceListByUser(userId);
        return prices.stream().anyMatch(item -> StringUtils.equals(normalizedCode, normalizePlatformCode(item.getPlatformCode()))
                && isPlatformPriceEnabled(item));
    }

    private boolean isPlatformConfiguredForUser(Long userId, String platformCode) {
        String normalizedCode = normalizePlatformCode(platformCode);
        if (StringUtils.isBlank(normalizedCode)) {
            return false;
        }
        List<MarkUserPlatformPrice> prices = buildPlatformPriceListByUser(userId);
        return prices.stream().anyMatch(item -> StringUtils.equals(normalizedCode, normalizePlatformCode(item.getPlatformCode())));
    }

    private void assertPlatformAvailableForSubmit(Long userId, String platformCode, String requestPlatformName) {
        if (isPlatformAvailableForUser(userId, platformCode)) {
            return;
        }
        String platformName = resolvePlatformNameByUser(userId, platformCode, requestPlatformName);
        throw new ServiceException(platformName + "平台未开启，请联系管理员");
    }

    private boolean isPlatformPriceEnabled(MarkUserPlatformPrice price) {
        return price != null && !"1".equals(StringUtils.trimToEmpty(price.getStatus()));
    }

    private String normalizePlatformStatus(String status) {
        String normalized = StringUtils.lowerCase(StringUtils.trimToEmpty(status));
        if ("0".equals(normalized) || "enable".equals(normalized) || "enabled".equals(normalized)
                || "open".equals(normalized) || "true".equals(normalized)) {
            return "0";
        }
        if ("1".equals(normalized) || "disable".equals(normalized) || "disabled".equals(normalized)
                || "close".equals(normalized) || "closed".equals(normalized) || "false".equals(normalized)) {
            return "1";
        }
        throw new ServiceException("平台状态仅支持开启或关闭");
    }

    private Long resolveDefaultUnitPrice(Long userId, String platformCode) {
        String normalizedCode = normalizePlatformCode(platformCode);
        Map<String, TemplatePlatformConfig> templateConfigMap = resolveTemplatePlatformConfigMapByUser(userId);
        TemplatePlatformConfig templateConfig = templateConfigMap.get(normalizedCode);
        if (templateConfig != null && templateConfig.getUnitPrice() != null && templateConfig.getUnitPrice() > 0) {
            return templateConfig.getUnitPrice();
        }
        return 1L;
    }

    private String resolvePlatformName(String platformCode, String requestPlatformName) {
        if (StringUtils.isNotBlank(requestPlatformName)) {
            return requestPlatformName.trim();
        }
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId != null) {
            Map<String, TemplatePlatformConfig> templateConfigMap = resolveTemplatePlatformConfigMapByUser(currentUserId);
            TemplatePlatformConfig templateConfig = templateConfigMap.get(platformCode);
            if (templateConfig != null && StringUtils.isNotBlank(templateConfig.getPlatformName())) {
                return templateConfig.getPlatformName();
            }
        }
        Map<String, String> platformNameMap = resolvePlatformNameMap(resolveMenuPlatformNameMap());
        return resolvePlatformNameByMap(platformNameMap, platformCode);
    }

    private Map<String, String> resolveMenuPlatformNameMap() {
        Map<String, String> platformNameMap = new LinkedHashMap<>();
        List<SysMenu> menus = com.mybatisflex.core.query.QueryChain.of(SysMenu.class)
                .eq(SysMenu::getParentId, MARK_ROOT_MENU_ID)
                .eq(SysMenu::getMenuType, "C")
                .eq(SysMenu::getStatus, "0")
                .eq(SysMenu::getComponent, MARK_USER_MENU_COMPONENT)
                .orderBy(SysMenu::getOrderNum, true)
                .list();
        for (SysMenu menu : menus) {
            Map<String, Object> queryMap = parseJsonMap(menu.getQuery());
            String platformCode = firstNonBlank(
                    getStringOrNull(queryMap == null ? null : queryMap.get("platformCode")),
                    getStringOrNull(queryMap == null ? null : queryMap.get("code")),
                    getStringOrNull(queryMap == null ? null : queryMap.get("value"))
            );
            if (StringUtils.isBlank(platformCode)) {
                continue;
            }
            platformCode = normalizePlatformCode(platformCode);
            String platformName = firstNonBlank(
                    getStringOrNull(queryMap == null ? null : queryMap.get("platformName")),
                    getStringOrNull(queryMap == null ? null : queryMap.get("name")),
                    StringUtils.trimToNull(menu.getMenuName()),
                    platformCode
            );
            platformNameMap.putIfAbsent(platformCode, platformName);
        }
        return platformNameMap;
    }

    private Map<String, String> resolvePlatformNameMap(Map<String, String> dynamicPlatformNameMap) {
        Map<String, String> platformNameMap = new LinkedHashMap<>();
        if (dynamicPlatformNameMap != null && !dynamicPlatformNameMap.isEmpty()) {
            platformNameMap.putAll(dynamicPlatformNameMap);
        }
        for (Map.Entry<String, String> entry : LEGACY_PLATFORM_NAME_MAP.entrySet()) {
            platformNameMap.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return platformNameMap;
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolvePlatformNameByMap(Map<String, String> platformNameMap, String platformCode) {
        if (StringUtils.isBlank(platformCode)) {
            return platformCode;
        }
        String name = platformNameMap == null ? null : platformNameMap.get(platformCode);
        return StringUtils.isBlank(name) ? platformCode : name;
    }

    private String getStringOrNull(Object value) {
        if (value == null) {
            return null;
        }
        return StringUtils.trimToNull(String.valueOf(value));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private void fillPrecheckItemFromData(MarkPhoneCheckItemVO item, Object dataObj) {
        if (!(dataObj instanceof Map<?, ?>)) {
            item.setStatus("UNKNOWN");
            item.setDetail("未返回平台结果");
            return;
        }
        Map<?, ?> dataMap = (Map<?, ?>) dataObj;
        Object resultsObj = dataMap.get("results");
        if (!(resultsObj instanceof List<?>)) {
            item.setStatus("EMPTY");
            item.setDetail("未返回平台结果");
            return;
        }
        List<?> results = (List<?>) resultsObj;
        if (results.isEmpty()) {
            item.setStatus("EMPTY");
            item.setDetail("未返回平台结果");
            return;
        }

        String fallbackStatus = null;
        String fallbackDetail = null;
        Long maxResponseTime = null;
        boolean marked = false;

        for (Object resultObj : results) {
            if (resultObj instanceof BatchTask.ApiResult) {
                BatchTask.ApiResult apiResult = (BatchTask.ApiResult) resultObj;
                maxResponseTime = maxLong(maxResponseTime, apiResult.getResponseTime());
                if (Boolean.FALSE.equals(apiResult.getSuccess()) && StringUtils.isBlank(item.getErrorMessage())) {
                    item.setErrorMessage(asString(apiResult.getError()));
                }
                PrecheckStatusInfo statusInfo = extractStatusInfo(apiResult.getData());
                if (statusInfo == null) {
                    continue;
                }
                if (fallbackStatus == null) {
                    fallbackStatus = statusInfo.status;
                    fallbackDetail = statusInfo.detail;
                }
                if (statusInfo.marked) {
                    marked = true;
                    item.setStatus(statusInfo.status);
                    item.setDetail(statusInfo.detail);
                }
            } else if (resultObj instanceof Map<?, ?>) {
                Map<?, ?> resultMap = (Map<?, ?>) resultObj;
                maxResponseTime = maxLong(maxResponseTime, parseLong(resultMap.get("responseTime")));
                Object success = resultMap.get("success");
                if (Boolean.FALSE.equals(success) && StringUtils.isBlank(item.getErrorMessage())) {
                    item.setErrorMessage(asString(resultMap.get("error")));
                }
                PrecheckStatusInfo statusInfo = extractStatusInfo(resultMap.get("data"));
                if (statusInfo == null) {
                    continue;
                }
                if (fallbackStatus == null) {
                    fallbackStatus = statusInfo.status;
                    fallbackDetail = statusInfo.detail;
                }
                if (statusInfo.marked) {
                    marked = true;
                    item.setStatus(statusInfo.status);
                    item.setDetail(statusInfo.detail);
                }
            }
        }

        item.setMarked(marked);
        if (StringUtils.isBlank(item.getStatus())) {
            item.setStatus(StringUtils.defaultIfBlank(fallbackStatus, marked ? "yes" : "no"));
        }
        if (StringUtils.isBlank(item.getDetail())) {
            item.setDetail(normalizeTeddyMarkDetailLabel(StringUtils.defaultIfBlank(fallbackDetail, marked ? "已标记" : "未标记")));
        } else {
            item.setDetail(normalizeTeddyMarkDetailLabel(item.getDetail()));
        }
        if (maxResponseTime != null) {
            item.setResponseTime(maxResponseTime);
        }
    }

    private PrecheckStatusInfo extractStatusInfo(Object apiDataObj) {
        if (!(apiDataObj instanceof Map<?, ?>)) {
            return null;
        }
        Map<?, ?> dataMap = (Map<?, ?>) apiDataObj;
        Object platformResultsObj = dataMap.get("platformResults");
        if (!(platformResultsObj instanceof List<?>)) {
            return null;
        }
        List<?> platformResults = (List<?>) platformResultsObj;
        PrecheckStatusInfo fallback = null;
        for (Object platformObj : platformResults) {
            if (!(platformObj instanceof Map<?, ?>)) {
                continue;
            }
            Map<?, ?> platformMap = (Map<?, ?>) platformObj;
            String status = asString(platformMap.get("status"));
            if (StringUtils.isBlank(status)) {
                continue;
            }
            String platform = asString(platformMap.get("platform"));
            boolean marked = isMarkedStatus(status);
            String detail = buildStatusDetail(platform, status, marked);
            PrecheckStatusInfo info = new PrecheckStatusInfo(marked, status, detail);
            if (fallback == null) {
                fallback = info;
            }
            if (marked) {
                return info;
            }
        }
        return fallback;
    }

    private String buildStatusDetail(String platform, String status, boolean marked) {
        String pureStatus = StringUtils.trimToEmpty(status);
        String detail;
        if (pureStatus.toLowerCase().startsWith("yes-")) {
            detail = pureStatus.substring(4).trim();
            detail = normalizeTeddyMarkDetailLabel(detail);
        } else if ("yes".equalsIgnoreCase(pureStatus)) {
            detail = "有标记";
        } else if (pureStatus.toLowerCase().startsWith("no-") || "no".equalsIgnoreCase(pureStatus)) {
            detail = "无";
        } else if ("无".equals(pureStatus) || "无标记".equals(pureStatus) || "未标记".equals(pureStatus)) {
            detail = "无";
        } else if ("normal".equalsIgnoreCase(pureStatus)) {
            detail = "正常";
        } else if ("risk".equalsIgnoreCase(pureStatus)) {
            detail = "风险";
        } else if ("unknown".equalsIgnoreCase(pureStatus)) {
            detail = "未知";
        } else {
            detail = pureStatus;
        }
        if (StringUtils.isBlank(detail)) {
            detail = marked ? "有标记" : "无";
        }
        return normalizeTeddyMarkDetailLabel(detail);
    }

    private String normalizeTeddyMarkDetailLabel(String detail) {
        if (StringUtils.isBlank(detail)) {
            return detail;
        }
        String trimmed = detail.trim();
        if (isTdGaopinSubmittableRawDetail(trimmed)) {
            return "泰迪熊高频";
        }
        if (trimmed.equals("普通标记") || trimmed.startsWith("普通标记-") || trimmed.startsWith("普通标记—")) {
            return "有标记";
        }
        if (trimmed.equals("无") || trimmed.equals("无标记") || trimmed.equals("未标记")
                || trimmed.toLowerCase().startsWith("no-") || "no".equalsIgnoreCase(trimmed)) {
            return "无";
        }
        return trimmed;
    }

    private boolean isTdGaopinPlatform(String platformCode) {
        return "td_gaopin".equals(StringUtils.trimToNull(platformCode));
    }

    private boolean isMobileGaopinPlatform(String platformCode) {
        String code = StringUtils.trimToNull(platformCode);
        return "mobile_gaopin".equals(code) || "yidonggaopin".equals(code);
    }

    private boolean isMobileGaopinSubmittableItem(MarkPhoneCheckItemVO item) {
        if (item == null || !Boolean.TRUE.equals(item.getQuerySuccess())) {
            return false;
        }
        if (Boolean.TRUE.equals(item.getMarked())) {
            return true;
        }
        String detail = normalizeTeddyMarkDetailLabel(StringUtils.trimToNull(item.getDetail()));
        return "有标记".equals(detail) || "高频拦截".equals(detail);
    }

    private void applyMobileGaopinPrecheckRules(MarkPhoneCheckItemVO item) {
        String status = StringUtils.trimToNull(item.getStatus());
        if (StringUtils.isNotBlank(status)) {
            String normalizedStatus = status.trim().toLowerCase();
            if ("no".equals(normalizedStatus) || normalizedStatus.startsWith("no-")) {
                item.setMarked(false);
                item.setDetail("无");
                return;
            }
        }
        String rawDetail = extractPrecheckRawDetail(item);
        if (StringUtils.isBlank(rawDetail)
                || "无".equals(rawDetail)
                || "无标记".equals(rawDetail)
                || "未标记".equals(rawDetail)) {
            item.setMarked(false);
            item.setDetail("无");
            return;
        }
        if (Boolean.TRUE.equals(item.getMarked()) || "yes".equalsIgnoreCase(StringUtils.trimToEmpty(item.getStatus()))) {
            item.setMarked(true);
            item.setDetail("有标记");
        }
    }

    private boolean isTdGaopinSubmittableRawDetail(String rawDetail) {
        if (StringUtils.isBlank(rawDetail)) {
            return false;
        }
        String trimmed = rawDetail.trim();
        if (trimmed.contains(TD_GAOPIN_HF_KEY)) {
            return true;
        }
        return trimmed.contains(TD_GAOPIN_FRAUD_KEY) && trimmed.contains("高频");
    }

    private String extractPrecheckRawDetail(MarkPhoneCheckItemVO item) {
        if (item == null) {
            return null;
        }
        String status = StringUtils.trimToNull(item.getStatus());
        if (StringUtils.isNotBlank(status)) {
            String normalizedStatus = status.trim();
            if (normalizedStatus.toLowerCase().startsWith("yes-")) {
                return normalizedStatus.substring(4).trim();
            }
            if (normalizedStatus.toLowerCase().startsWith("no-")) {
                return normalizedStatus.substring(3).trim();
            }
            return normalizedStatus;
        }
        return StringUtils.trimToNull(item.getDetail());
    }

    private void applyTdGaopinPrecheckRules(MarkPhoneCheckItemVO item) {
        String status = StringUtils.trimToNull(item.getStatus());
        if (StringUtils.isNotBlank(status)) {
            String normalizedStatus = status.trim().toLowerCase();
            if ("no".equals(normalizedStatus) || normalizedStatus.startsWith("no-")) {
                item.setMarked(false);
                item.setDetail("无");
                return;
            }
        }
        String rawDetail = extractPrecheckRawDetail(item);
        if (isTdGaopinSubmittableRawDetail(rawDetail)) {
            item.setMarked(true);
            item.setDetail("泰迪熊高频");
            return;
        }
        item.setMarked(false);
        if (StringUtils.isNotBlank(rawDetail)
                && (rawDetail.startsWith("普通标记") || rawDetail.contains("普通标记"))) {
            item.setDetail("有标记");
            return;
        }
        if (StringUtils.isNotBlank(rawDetail)
                && ("无".equals(rawDetail) || "无标记".equals(rawDetail) || "未标记".equals(rawDetail))) {
            item.setDetail("无");
        }
    }

    private boolean isMarkedStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return false;
        }
        String normalized = status.trim().toLowerCase();
        if ("no".equals(normalized) || normalized.startsWith("no-")) {
            return false;
        }
        if ("无".equals(status) || "无标记".equals(status) || "未标记".equals(status)
                || "查询失败".equals(status) || "未开放".equals(status) || "-".equals(status)) {
            return false;
        }
        if ("normal".equals(normalized) || "unknown".equals(normalized)) {
            return false;
        }
        return true;
    }
    private Map<String, Object> callTencentJsonp(String path, Map<String, ?> params) {
        Map<String, Object> requestParams = new LinkedHashMap<>();
        if (params != null && !params.isEmpty()) {
            requestParams.putAll(params);
        }
        requestParams.putIfAbsent("callback", "cb");
        String url = buildTencentUrl(path, requestParams);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Referer", TENCENT_REFERER);
        headers.put("User-Agent", TENCENT_USER_AGENT);
        headers.put("Accept", "*/*");
        headers.put("X-Requested-With", "XMLHttpRequest");
        String body;
        try {
            body = HttpUtils.get(url, headers);
        } catch (Exception e) {
            log.warn("Tencent request failed. url={}", url, e);
            return null;
        }
        if (StringUtils.isBlank(body)) {
            return null;
        }
        return parseTencentJsonp(body);
    }

    private String buildTencentUrl(String path, Map<String, ?> params) {
        String normalizedPath = StringUtils.startsWith(path, "/") ? path : "/" + StringUtils.trimToEmpty(path);
        String query = buildQueryString(params);
        if (StringUtils.isBlank(query)) {
            return TENCENT_BASE_URL + normalizedPath;
        }
        return TENCENT_BASE_URL + normalizedPath + "?" + query;
    }

    private String buildQueryString(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            if (StringUtils.isBlank(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(urlEncode(entry.getKey()))
                    .append("=")
                    .append(urlEncode(String.valueOf(entry.getValue())));
        }
        return builder.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private Map<String, Object> parseTencentJsonp(String body) {
        String jsonPayload = extractJsonPayload(body);
        if (StringUtils.isBlank(jsonPayload)) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonPayload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Tencent JSONP parse failed. body={}", body, e);
            return null;
        }
    }

    private String extractJsonPayload(String body) {
        String trimmed = StringUtils.trimToEmpty(body);
        if (StringUtils.isBlank(trimmed)) {
            return null;
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            return StringUtils.trimToNull(trimmed.substring(1, trimmed.length() - 1));
        }
        Matcher matcher = JSONP_WRAPPER_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return StringUtils.trimToNull(matcher.group(1));
        }
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return null;
    }

    private Integer parseNullableInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = StringUtils.trimToNull(String.valueOf(value));
        if (text == null) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeSinglePhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return null;
        }
        String clean = phone.replaceAll("[^0-9]", "");
        if (clean.length() < 7 || clean.length() > 15) {
            return null;
        }
        return clean;
    }

    private String resolveTencentPlatformCodeForUser(Long userId) {
        List<MarkUserPlatformPrice> platformPrices = buildPlatformPriceListByUser(userId);
        for (MarkUserPlatformPrice platformPrice : platformPrices) {
            if (platformPrice == null || StringUtils.isBlank(platformPrice.getPlatformCode())) {
                continue;
            }
            if (!isPlatformPriceEnabled(platformPrice)) {
                continue;
            }
            String platformCode = StringUtils.trimToEmpty(platformPrice.getPlatformCode());
            String platformName = StringUtils.trimToEmpty(platformPrice.getPlatformName());
            if (isTencentPlatformCode(platformCode) || StringUtils.contains(platformName, "腾讯")) {
                return platformCode;
            }
        }
        throw new ServiceException("当前账号未开通腾讯平台");
    }

    private String resolveTencentStyleSubmitPlatformCodeForUser(Long userId, String requestedPlatformCode) {
        String normalizedRequestedCode = StringUtils.lowerCase(StringUtils.trimToEmpty(requestedPlatformCode));
        if (StringUtils.isBlank(normalizedRequestedCode)) {
            return resolveTencentPlatformCodeForUser(userId);
        }
        if (!isTencentStyleSubmitPlatformCode(normalizedRequestedCode)) {
            throw new ServiceException("当前平台不支持验证码提交页面");
        }
        List<MarkUserPlatformPrice> platformPrices = buildPlatformPriceListByUser(userId);
        for (MarkUserPlatformPrice platformPrice : platformPrices) {
            if (platformPrice == null || StringUtils.isBlank(platformPrice.getPlatformCode())) {
                continue;
            }
            if (!isPlatformPriceEnabled(platformPrice)) {
                continue;
            }
            String platformCode = StringUtils.trimToEmpty(platformPrice.getPlatformCode());
            if (StringUtils.equalsIgnoreCase(platformCode, normalizedRequestedCode)) {
                return platformCode;
            }
        }
        throw new ServiceException("当前账号未开通该平台");
    }

    private void assertNotLegacyTencentBatchPlatform(String platformCode) {
        if (isTencentPlatformCode(platformCode)) {
            throw new ServiceException("腾讯平台已切换为专用页面，请从菜单「腾讯速解」进入提交");
        }
    }

    private boolean isTencentPlatformCode(String platformCode) {
        String normalizedCode = StringUtils.lowerCase(StringUtils.trimToEmpty(platformCode));
        return "tencent_mark".equals(normalizedCode)
                || "tengxun".equals(normalizedCode)
                || "tencent".equals(normalizedCode)
                || "tx".equals(normalizedCode)
                || "txwz".equals(normalizedCode);
    }

    private boolean isTencentStyleSubmitPlatformCode(String platformCode) {
        String normalizedCode = StringUtils.lowerCase(StringUtils.trimToEmpty(platformCode));
        return isTencentPlatformCode(normalizedCode)
                || "td_second".equals(normalizedCode);
    }

    private long getPlatformRemainCountByUser(Long userId, String platformCode) {
        if (userId == null || StringUtils.isBlank(platformCode)) {
            return 0L;
        }
        List<MarkUserPlatformPrice> platformPrices = buildPlatformPriceListByUser(userId);
        for (MarkUserPlatformPrice platformPrice : platformPrices) {
            if (platformPrice == null) {
                continue;
            }
            if (StringUtils.equals(platformCode, platformPrice.getPlatformCode())) {
                return sanitizeRemainCount(platformPrice.getRemainCount());
            }
        }
        return 0L;
    }

    private void deductTencentQuotaOnSuccess(Long userId,
                                             String operator,
                                             String platformCode,
                                             String platformName) {
        Date now = DateUtils.getNowDate();
        String resolvedPlatformName = StringUtils.defaultIfBlank(
                platformName,
                resolvePlatformNameByUser(userId, platformCode, "腾讯速解")
        );
        MarkUserPlatformQuota quota = lockUserPlatformQuota(
                userId,
                platformCode,
                resolvedPlatformName,
                operator,
                now
        );
        long balanceBefore = sanitizeRemainCount(quota.getRemainCount());
        if (balanceBefore < 1) {
            throw new ServiceException("当前腾讯平台剩余次数不足");
        }
        long balanceAfter = balanceBefore - 1;
        quota.setPlatformName(resolvedPlatformName);
        quota.setRemainCount(balanceAfter);
        quota.setUpdateBy(operator);
        quota.setUpdateTime(now);
        markUserPlatformQuotaMapper.updateMarkUserPlatformQuota(quota);
        insertWalletLog(
                userId,
                null,
                null,
                platformCode,
                resolvedPlatformName,
                "DEDUCT",
                -1L,
                balanceBefore,
                balanceAfter,
                "腾讯验证码提交成功扣次",
                operator,
                now
        );
    }

    private int parseInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Long maxLong(Long current, Long candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate > current) {
            return candidate;
        }
        return current;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static class PrecheckStatusInfo {
        private final boolean marked;
        private final String status;
        private final String detail;

        private PrecheckStatusInfo(boolean marked, String status, String detail) {
            this.marked = marked;
            this.status = status;
            this.detail = detail;
        }
    }

    private static class TemplatePlatformConfig {
        private final String platformCode;
        private String platformName;
        private Long unitPrice;

        private TemplatePlatformConfig(String platformCode, String platformName, Long unitPrice) {
            this.platformCode = platformCode;
            this.platformName = platformName;
            this.unitPrice = unitPrice;
        }

        private String getPlatformCode() {
            return platformCode;
        }

        private String getPlatformName() {
            return platformName;
        }

        private void setPlatformName(String platformName) {
            this.platformName = platformName;
        }

        private Long getUnitPrice() {
            return unitPrice;
        }

        private void setUnitPrice(Long unitPrice) {
            this.unitPrice = unitPrice;
        }
    }

    private List<String> normalizePhones(List<String> phones) {
        if (phones == null || phones.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String phone : phones) {
            if (StringUtils.isBlank(phone)) {
                continue;
            }
            String clean = phone.replaceAll("[^0-9]", "");
            if (clean.length() < 7 || clean.length() > 15) {
                continue;
            }
            normalized.add(clean);
        }
        return new ArrayList<>(normalized);
    }

    private String generateOrderNo() {
        return "MO" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + (1000 + new Random().nextInt(9000));
    }

    private void insertWalletLog(Long userId,
                                 Long orderId,
                                 Long orderItemId,
                                 String platformCode,
                                 String platformName,
                                 String bizType,
                                 Long changeAmount,
                                 Long balanceBefore,
                                 Long balanceAfter,
                                 String remark,
                                 String createBy,
                                 Date createTime) {
        MarkWalletLog walletLog = new MarkWalletLog();
        walletLog.setUserId(userId);
        walletLog.setOrderId(orderId);
        walletLog.setOrderItemId(orderItemId);
        walletLog.setPlatformCode(platformCode);
        walletLog.setPlatformName(platformName);
        walletLog.setBizType(bizType);
        walletLog.setChangeAmount(changeAmount);
        walletLog.setBalanceBefore(balanceBefore);
        walletLog.setBalanceAfter(balanceAfter);
        walletLog.setRemark(remark);
        walletLog.setCreateBy(createBy);
        walletLog.setCreateTime(createTime);
        markWalletLogMapper.insertMarkWalletLog(walletLog);
    }
    private String resolvePlatformNameByUser(Long userId, String platformCode, String requestPlatformName) {
        if (StringUtils.isNotBlank(requestPlatformName)) {
            return requestPlatformName.trim();
        }
        if (StringUtils.isBlank(platformCode)) {
            return platformCode;
        }
        List<MarkUserPlatformPrice> prices = buildPlatformPriceListByUser(userId);
        for (MarkUserPlatformPrice price : prices) {
            if (StringUtils.equals(platformCode, price.getPlatformCode())
                    && StringUtils.isNotBlank(price.getPlatformName())) {
                return price.getPlatformName();
            }
        }
        return resolvePlatformName(platformCode, null);
    }

    private MarkUserPlatformQuota lockUserPlatformQuota(Long userId,
                                                        String platformCode,
                                                        String platformName,
                                                        String operator,
                                                        Date now) {
        String normalizedCode = normalizePlatformCode(platformCode);
        if (StringUtils.isBlank(normalizedCode)) {
            throw new ServiceException("平台编码不能为空");
        }
        MarkUserPlatformQuota quota = markUserPlatformQuotaMapper.selectByUserAndPlatformForUpdate(userId, normalizedCode);
        if (quota != null) {
            if (!StringUtils.equals(normalizedCode, quota.getPlatformCode())) {
                quota.setPlatformCode(normalizedCode);
                markUserPlatformQuotaMapper.updateMarkUserPlatformQuota(quota);
            }
            return quota;
        }
        MarkUserPlatformQuota insert = new MarkUserPlatformQuota();
        insert.setUserId(userId);
        insert.setPlatformCode(normalizedCode);
        insert.setPlatformName(platformName);
        insert.setRemainCount(0L);
        insert.setCreateBy(operator);
        insert.setCreateTime(now);
        insert.setUpdateBy(operator);
        insert.setUpdateTime(now);
        markUserPlatformQuotaMapper.insertMarkUserPlatformQuota(insert);
        return markUserPlatformQuotaMapper.selectByUserAndPlatformForUpdate(userId, normalizedCode);
    }

    private String normalizePlatformCode(String platformCode) {
        return StringUtils.lowerCase(StringUtils.trimToEmpty(platformCode));
    }

    private long sanitizeRemainCount(Long remainCount) {
        return remainCount == null ? 0L : Math.max(remainCount, 0L);
    }

    private void assertUserPlatformQuotaSufficient(Long userId, String platformCode, long requiredAmount) {
        if (requiredAmount <= 0) {
            return;
        }
        String normalizedCode = normalizePlatformCode(platformCode);
        MarkUserPlatformQuota quota = markUserPlatformQuotaMapper.selectByUserAndPlatform(userId, normalizedCode);
        long balance = sanitizeRemainCount(quota == null ? null : quota.getRemainCount());
        assertUserPlatformQuotaSufficient(balance, requiredAmount);
    }

    private void assertUserPlatformQuotaSufficient(long balance, long requiredAmount) {
        if (requiredAmount <= 0) {
            return;
        }
        if (balance < requiredAmount) {
            throw new ServiceException("当前平台剩余次数不足，无法提交");
        }
    }

    private int safeInteger(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    private String normalizeAdjustType(String adjustType) {
        String normalized = StringUtils.upperCase(StringUtils.trimToEmpty(adjustType));
        if ("ADD".equals(normalized) || "PLUS".equals(normalized)) {
            return "ADD";
        }
        if ("SUBTRACT".equals(normalized) || "REDUCE".equals(normalized) || "DEDUCT".equals(normalized)
                || "MINUS".equals(normalized) || "SUB".equals(normalized)) {
            return "SUBTRACT";
        }
        throw new ServiceException("调整类型仅支持 ADD 或 SUBTRACT");
    }

    private void assertAgentAdjustAllowed(SysUser targetUser) {
        if (targetUser == null) {
            throw new ServiceException("目标用户不存在");
        }
        if (isAdminRole()) {
            return;
        }
        if (!isAgentRole()) {
            throw new ServiceException("仅代理或管理员可操作");
        }
        String operator = SecurityUtils.getUsername();
        String owner = StringUtils.trimToEmpty(targetUser.getCreateBy());
        if (!StringUtils.equals(operator, owner)) {
            throw new ServiceException("仅可调整自己下线用户的平台次数");
        }
    }

    private boolean isAdminRole() {
        return SecurityUtils.isAdmin() || SecurityUtils.hasRole("admin");
    }

    private boolean isAgentRole() {
        return SecurityUtils.hasRole("agent") || SecurityUtils.hasRole("mark_agent");
    }

    private boolean hasAuditReadPermission() {
        return SecurityUtils.hasPermi("server:markAdmin:audit:order:list")
                || SecurityUtils.hasPermi("server:markAdmin:audit:wallet:list");
    }
}
