package com.geek.server.service.impl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.common.core.domain.entity.SysMenu;

import com.geek.common.exception.ServiceException;
import com.geek.common.utils.DateUtils;
import com.geek.common.utils.SecurityUtils;
import com.geek.common.utils.http.HttpUtils;
import com.geek.common.utils.ip.IpUtils;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.MarkOrderItem;
import com.geek.server.domain.MarkPlatformTemplate;
import com.geek.server.domain.MarkUserPlatformPrice;
import com.geek.server.domain.MarkUserPlatformQuota;
import com.geek.server.domain.MarkWalletLog;
import com.geek.server.domain.entity.BatchTask;
import com.geek.server.domain.dto.MarkAgentPlatformQuotaAdjustRequest;
import com.geek.server.domain.dto.MarkOrderCreateRequest;
import com.geek.server.domain.dto.MarkOrderItemProcessRequest;
import com.geek.server.domain.dto.MarkTencentStatusQueryRequest;
import com.geek.server.domain.dto.MarkTencentSubmitRequest;
import com.geek.server.domain.vo.FreeSingleQueryRequest;
import com.geek.server.domain.vo.MarkAgentPlatformQuotaAdjustResultVO;
import com.geek.server.domain.vo.MarkOrderDetailVO;
import com.geek.server.domain.vo.MarkOrderPrecheckResultVO;
import com.geek.server.domain.vo.MarkPhoneCheckItemVO;
import com.geek.server.domain.vo.MarkTencentStatusItemVO;
import com.geek.server.domain.vo.MarkTencentStatusQueryResultVO;
import com.geek.server.domain.vo.MarkTencentSubmitResultVO;
import com.geek.server.domain.vo.MarkWalletSummaryVO;
import com.geek.server.mapper.MarkOrderItemMapper;
import com.geek.server.mapper.MarkOrderMapper;
import com.geek.server.mapper.MarkPlatformTemplateMapper;
import com.geek.server.mapper.MarkUserPlatformPriceMapper;
import com.geek.server.mapper.MarkUserPlatformQuotaMapper;
import com.geek.server.mapper.MarkWalletLogMapper;
import com.geek.server.service.IFreeQueryService;
import com.geek.server.service.IMarkOrderService;
import com.geek.system.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final String TENCENT_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148";
    private static final Pattern JSONP_WRAPPER_PATTERN = Pattern.compile("^[\\w$]+\\((.*)\\)\\s*;?$", Pattern.DOTALL);
    private static final Map<String, String> LEGACY_PLATFORM_NAME_MAP = new LinkedHashMap<>();

    static {
        LEGACY_PLATFORM_NAME_MAP.put("mobile_gaopin", "高频拦截");
        LEGACY_PLATFORM_NAME_MAP.put("td_gaopin", "泰迪高频");
        LEGACY_PLATFORM_NAME_MAP.put("td_second", "泰迪二次");
        LEGACY_PLATFORM_NAME_MAP.put("qihu_first", "360首次");
        LEGACY_PLATFORM_NAME_MAP.put("qihu_second", "360二次");
        LEGACY_PLATFORM_NAME_MAP.put("dianhuabang", "电话邦");
        LEGACY_PLATFORM_NAME_MAP.put("tencent_mark", "腾讯");
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
    private IFreeQueryService freeQueryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkOrderDetailVO createOrder(MarkOrderCreateRequest request) {
        Long currentUserId = SecurityUtils.getUserId();
        String currentUserName = SecurityUtils.getUsername();
        if (request == null || StringUtils.isBlank(request.getPlatformCode())) {
            throw new ServiceException("平台编码不能为空");
        }
        if (!isPlatformAvailableForUser(currentUserId, request.getPlatformCode())) {
            throw new ServiceException("当前账号未开通该平台");
        }

        List<String> normalizedPhones = normalizePhones(request.getPhones());
        if (normalizedPhones.isEmpty()) {
            throw new ServiceException("没有可用号码");
        }

        String requestNo = StringUtils.trimToEmpty(request.getRequestNo());
        if (StringUtils.isNotBlank(requestNo)) {
            Long existedOrderId = markOrderMapper.selectOrderIdByUserAndRequestNo(currentUserId, requestNo);
            if (existedOrderId != null) {
                return selectMyOrderDetail(existedOrderId);
            }
        }


        Date now = DateUtils.getNowDate();
        String platformName = resolvePlatformNameByUser(currentUserId, request.getPlatformCode(), request.getPlatformName());
        long unitPrice = getEffectiveUnitPrice(currentUserId, request.getPlatformCode());
        long totalAmount = unitPrice * normalizedPhones.size();
        MarkUserPlatformQuota quota = lockUserPlatformQuota(
                currentUserId,
                request.getPlatformCode(),
                platformName,
                currentUserName,
                now
        );
        long balanceBefore = sanitizeRemainCount(quota.getRemainCount());
        if (balanceBefore < totalAmount) {
            throw new ServiceException("当前平台剩余次数不足，无法下单");
        }
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
        order.setPlatformCode(request.getPlatformCode());
        order.setPlatformName(platformName);
        order.setTotalCount(normalizedPhones.size());
        order.setSuccessCount(0);
        order.setFailedCount(0);
        order.setTotalAmount(totalAmount);
        order.setRefundAmount(0L);
        order.setOrderStatus("0");
        order.setRemark(request.getRemark());
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
            item.setCreateBy(currentUserName);
            item.setCreateTime(now);
            item.setUpdateBy(currentUserName);
            item.setUpdateTime(now);
            markOrderItemMapper.insertMarkOrderItem(item);
        }

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
        return buildOrderDetail(order.getId());
    }

    @Override
    public MarkOrderPrecheckResultVO precheckOrder(MarkOrderCreateRequest request) {
        Long currentUserId = SecurityUtils.getUserId();
        if (request == null || StringUtils.isBlank(request.getPlatformCode())) {
            throw new ServiceException("平台编码不能为空");
        }
        if (!isPlatformAvailableForUser(currentUserId, request.getPlatformCode())) {
            throw new ServiceException("当前账号未开通该平台");
        }
        List<String> normalizedPhones = normalizePhones(request.getPhones());
        if (normalizedPhones.isEmpty()) {
            throw new ServiceException("没有可用号码");
        }

        String sourceIp;
        try {
            sourceIp = IpUtils.getIpAddr();
        } catch (Exception e) {
            sourceIp = "mark-precheck-" + currentUserId;
        }
        if (StringUtils.isBlank(sourceIp) || "unknown".equalsIgnoreCase(sourceIp)) {
            sourceIp = "mark-precheck-" + currentUserId;
        }

        MarkOrderPrecheckResultVO resultVO = new MarkOrderPrecheckResultVO();
        resultVO.setPlatformCode(request.getPlatformCode());
        String resolvedPlatformName = resolvePlatformName(request.getPlatformCode(), request.getPlatformName());
        resultVO.setPlatformName(resolvedPlatformName);

        List<String> markedPhones = new ArrayList<>();
        List<String> unmarkedPhones = new ArrayList<>();
        List<String> failedPhones = new ArrayList<>();
        List<MarkPhoneCheckItemVO> items = new ArrayList<>();

        for (String phone : normalizedPhones) {
            MarkPhoneCheckItemVO item = new MarkPhoneCheckItemVO();
            item.setPhone(phone);
            try {
                FreeSingleQueryRequest singleQueryRequest = new FreeSingleQueryRequest();
                singleQueryRequest.setPhone(phone);
                singleQueryRequest.setDeviceId("mark-user-" + currentUserId);
                singleQueryRequest.setPlatformCode(request.getPlatformCode());
                singleQueryRequest.setPlatformName(resolvedPlatformName);
                Map<String, Object> queryResult = freeQueryService.singleQuery(singleQueryRequest, sourceIp);
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
    public MarkTencentSubmitResultVO submitTencent(MarkTencentSubmitRequest request) {
        if (request == null) {
            throw new ServiceException("请求参数不能为空");
        }
        Long currentUserId = SecurityUtils.getUserId();
        String currentUserName = SecurityUtils.getUsername();
        String phone = normalizeSinglePhone(request.getPhone());
        if (StringUtils.isBlank(phone)) {
            throw new ServiceException("手机号格式不正确");
        }
        String smsCode = StringUtils.trimToEmpty(request.getSmsCode());
        if (!smsCode.matches("\\d{6}")) {
            throw new ServiceException("验证码应为6位数字");
        }
        String tencentPlatformCode = resolveTencentPlatformCodeForUser(currentUserId);
        String tencentPlatformName = resolvePlatformNameByUser(currentUserId, tencentPlatformCode, "腾讯");
        if (getPlatformRemainCountByUser(currentUserId, tencentPlatformCode) < 1) {
            throw new ServiceException("当前腾讯平台剩余次数不足");
        }

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
        boolean forceTamper = Boolean.TRUE.equals(request.getForceTamper());
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
        boolean accepted = submitReCode != null && submitReCode == 0;
        resultVO.setAccepted(accepted);
        if (accepted) {
            deductTencentQuotaOnSuccess(currentUserId, currentUserName, tencentPlatformCode, tencentPlatformName);
        }
        recordTencentSubmitOrder(currentUserId, currentUserName, tencentPlatformCode, tencentPlatformName, phone, resultVO);
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
    public MarkOrderDetailVO selectAgentOrderDetail(Long orderId) {
        MarkOrder order = requireOrder(orderId);
        assertAgentReadable(order, false);
        return buildOrderDetail(orderId);
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
        if (!"0".equals(item.getProcessStatus())) {
            throw new ServiceException("订单明细已处理，不能重复回填");
        }

        MarkOrder order = requireOrder(item.getOrderId());
        assertAgentReadable(order, true);

        Date now = DateUtils.getNowDate();
        String currentUserName = SecurityUtils.getUsername();

        item.setProcessStatus(request.getProcessStatus());
        item.setProcessResult(request.getProcessResult());
        item.setProcessNote(request.getProcessNote());
        item.setProcessedBy(currentUserName);
        item.setProcessedTime(now);
        item.setUpdateBy(currentUserName);
        item.setUpdateTime(now);

        if ("2".equals(request.getProcessStatus()) && !"1".equals(item.getRefunded())) {
            refundOrderItem(order, item, currentUserName, now);
            item.setRefunded("1");
        }

        markOrderItemMapper.updateMarkOrderItem(item);
        refreshOrderStats(order.getId(), currentUserName);
        return buildOrderDetail(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkOrderDetailVO completeOrder(Long orderId) {
        MarkOrder order = requireOrder(orderId);
        assertAgentReadable(order, true);
        List<MarkOrderItem> itemList = markOrderItemMapper.selectMarkOrderItemsByOrderId(orderId);
        boolean hasPending = itemList.stream().anyMatch(item -> "0".equals(item.getProcessStatus()));
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

        String platformCode = StringUtils.trimToEmpty(request.getPlatformCode());
        if (StringUtils.isBlank(platformCode)) {
            throw new ServiceException("平台编码不能为空");
        }
        if (!isPlatformAvailableForUser(targetUser.getUserId(), platformCode)) {
            throw new ServiceException("目标用户未开通该平台");
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

    private void refundOrderItem(MarkOrder order, MarkOrderItem item, String currentUserName, Date now) {
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
                "明细失败自动退款",
                currentUserName,
                now
        );
    }

    private void recordTencentSubmitOrder(Long userId,
                                          String operator,
                                          String platformCode,
                                          String platformName,
                                          String phone,
                                          MarkTencentSubmitResultVO resultVO) {
        if (resultVO == null || StringUtils.isBlank(phone)) {
            return;
        }
        boolean accepted = Boolean.TRUE.equals(resultVO.getAccepted());
        long deductAmount = accepted ? 1L : 0L;
        Date now = DateUtils.getNowDate();
        String resolvedPlatformName = StringUtils.defaultIfBlank(
                platformName,
                resolvePlatformNameByUser(userId, platformCode, "腾讯")
        );
        Integer submitReCode = resultVO.getSubmitReCode();
        String submitData = StringUtils.trimToNull(resultVO.getSubmitData());
        Integer verifyReCode = resultVO.getVerifyReCode();
        String verifyData = StringUtils.trimToNull(resultVO.getVerifyData());

        MarkOrder order = new MarkOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPlatformCode(platformCode);
        order.setPlatformName(resolvedPlatformName);
        order.setTotalCount(1);
        order.setSuccessCount(accepted ? 1 : 0);
        order.setFailedCount(accepted ? 0 : 1);
        order.setTotalAmount(deductAmount);
        order.setRefundAmount(0L);
        order.setOrderStatus("2");
        order.setCompletedTime(now);
        order.setRemark(accepted
                ? "腾讯验证码提交成功"
                : buildTencentRecordText("腾讯验证码提交失败", submitReCode, submitData));
        order.setCreateBy(operator);
        order.setCreateTime(now);
        order.setUpdateBy(operator);
        order.setUpdateTime(now);
        markOrderMapper.insertMarkOrder(order);

        MarkOrderItem item = new MarkOrderItem();
        item.setOrderId(order.getId());
        item.setPhone(phone);
        item.setUnitPrice(1L);
        item.setItemAmount(deductAmount);
        item.setProcessStatus(accepted ? "1" : "2");
        item.setProcessResult(buildTencentRecordText(accepted ? "腾讯受理成功" : "腾讯受理失败", submitReCode, submitData));
        item.setProcessNote(buildTencentSubmitChainDetailText(resultVO));
        item.setProcessedBy(operator);
        item.setProcessedTime(now);
        item.setRefunded("0");
        item.setCreateBy(operator);
        item.setCreateTime(now);
        item.setUpdateBy(operator);
        item.setUpdateTime(now);
        markOrderItemMapper.insertMarkOrderItem(item);
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
        String summaryLine = "链路：phone_type 查询 -> 申诉状态查询 -> 验证码校验 -> 提交受理";
        String phoneTypeLine = "phone_type：原始=" + safeTencentText(resultVO.getOriginalPhoneType())
                + "，提交=" + safeTencentText(resultVO.getSubmittedPhoneType())
                + "（" + resolveTencentSubmitModeText(resultVO.getOriginalPhoneType(), resultVO.getSubmittedPhoneType()) + "）";
        String complainLine = "申诉状态：data=" + safeTencentText(resultVO.getComplainStatus());
        String verifyLine = "验证码校验：reCode=" + safeTencentText(resultVO.getVerifyReCode())
                + "，data=" + safeTencentText(resultVO.getVerifyData());
        String submitLine = "提交受理：reCode=" + safeTencentText(resultVO.getSubmitReCode())
                + "，data=" + safeTencentText(resultVO.getSubmitData());
        String finalLine = "最终结果：" + (Boolean.TRUE.equals(resultVO.getAccepted())
                ? "腾讯受理成功（已扣次数 1）"
                : "腾讯受理失败（未扣次数）");
        return StringUtils.abbreviate(String.join("\n",
                summaryLine,
                phoneTypeLine,
                complainLine,
                verifyLine,
                submitLine,
                finalLine
        ), 500);
    }

    private String safeTencentText(Object value) {
        String text = value == null ? null : StringUtils.trimToNull(String.valueOf(value));
        return StringUtils.defaultIfBlank(text, "-");
    }

    private String resolveTencentSubmitModeText(Integer originalPhoneType, Integer submittedPhoneType) {
        if (submittedPhoneType == null) {
            return "提交类型未知";
        }
        if (originalPhoneType == null) {
            return "兜底提交";
        }
        return originalPhoneType.equals(submittedPhoneType) ? "原样提交" : "篡改提交";
    }

    private String buildTencentRealtimeStatusText(Integer phoneType, String complainStatus) {
        String typeText = phoneType == null ? "-" : String.valueOf(phoneType);
        String complainText = StringUtils.defaultIfBlank(StringUtils.trimToNull(complainStatus), "-");
        return "phone_type=" + typeText + "｜申诉状态=" + complainText;
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
        if (order.getAssignedAgentId() != null && !currentUserId.equals(order.getAssignedAgentId())) {
            throw new ServiceException("该订单已由其他代理处理");
        }
        if (order.getAssignedAgentId() == null) {
            SysUser orderUser = requireUser(order.getUserId());
            if (!StringUtils.equals(StringUtils.trimToEmpty(orderUser.getCreateBy()), currentUsername)) {
                throw new ServiceException("仅可处理自己创建用户的订单");
            }
        }
        if (lockWhenUnassigned && order.getAssignedAgentId() == null) {
            MarkOrder lockOrder = new MarkOrder();
            lockOrder.setId(order.getId());
            lockOrder.setAssignedAgentId(currentUserId);
            lockOrder.setUpdateBy(currentUsername);
            lockOrder.setUpdateTime(DateUtils.getNowDate());
            markOrderMapper.updateMarkOrder(lockOrder);
            order.setAssignedAgentId(currentUserId);
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
        MarkUserPlatformPrice platformPrice = markUserPlatformPriceMapper.selectByUserAndPlatform(userId, platformCode);
        if (platformPrice == null || platformPrice.getUnitPrice() == null || platformPrice.getUnitPrice() <= 0) {
            Map<String, TemplatePlatformConfig> templateConfigMap = resolveTemplatePlatformConfigMapByUser(userId);
            TemplatePlatformConfig templateConfig = templateConfigMap.get(platformCode);
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
                .collect(Collectors.toMap(MarkUserPlatformPrice::getPlatformCode, item -> item, (a, b) -> a, LinkedHashMap::new));
        List<MarkUserPlatformQuota> quotas = markUserPlatformQuotaMapper.selectByUserId(userId);
        Map<String, Long> quotaMap = new LinkedHashMap<>();
        for (MarkUserPlatformQuota quota : quotas) {
            if (quota == null || StringUtils.isBlank(quota.getPlatformCode())) {
                continue;
            }
            quotaMap.put(quota.getPlatformCode(), sanitizeRemainCount(quota.getRemainCount()));
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
            price.setRemainCount(sanitizeRemainCount(quotaMap.get(platformCode)));
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
        String normalizedCode = StringUtils.trimToEmpty(code);
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
        if (StringUtils.isBlank(platformCode)) {
            return false;
        }
        List<MarkUserPlatformPrice> prices = buildPlatformPriceListByUser(userId);
        return prices.stream().anyMatch(item -> StringUtils.equals(platformCode, item.getPlatformCode()));
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
            item.setDetail(StringUtils.defaultIfBlank(fallbackDetail, marked ? "已标记" : "未标记"));
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
        } else if ("yes".equalsIgnoreCase(pureStatus)) {
            detail = "有标记";
        } else if ("no".equalsIgnoreCase(pureStatus) || "无".equals(pureStatus)
                || "无标记".equals(pureStatus) || "未标记".equals(pureStatus)) {
            detail = "未标记";
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
            detail = marked ? "有标记" : "未标记";
        }
        if (StringUtils.isBlank(platform)) {
            return detail;
        }
        return platform + "：" + detail;
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
            String platformCode = StringUtils.trimToEmpty(platformPrice.getPlatformCode());
            String platformName = StringUtils.trimToEmpty(platformPrice.getPlatformName());
            if (isTencentPlatformCode(platformCode) || StringUtils.contains(platformName, "腾讯")) {
                return platformCode;
            }
        }
        throw new ServiceException("当前账号未开通腾讯平台");
    }

    private boolean isTencentPlatformCode(String platformCode) {
        String normalizedCode = StringUtils.lowerCase(StringUtils.trimToEmpty(platformCode));
        return "tencent_mark".equals(normalizedCode)
                || "tencent".equals(normalizedCode)
                || "tx".equals(normalizedCode)
                || "txwz".equals(normalizedCode);
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
                resolvePlatformNameByUser(userId, platformCode, "腾讯")
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
        MarkUserPlatformQuota quota = markUserPlatformQuotaMapper.selectByUserAndPlatformForUpdate(userId, platformCode);
        if (quota != null) {
            return quota;
        }
        MarkUserPlatformQuota insert = new MarkUserPlatformQuota();
        insert.setUserId(userId);
        insert.setPlatformCode(platformCode);
        insert.setPlatformName(platformName);
        insert.setRemainCount(0L);
        insert.setCreateBy(operator);
        insert.setCreateTime(now);
        insert.setUpdateBy(operator);
        insert.setUpdateTime(now);
        markUserPlatformQuotaMapper.insertMarkUserPlatformQuota(insert);
        return markUserPlatformQuotaMapper.selectByUserAndPlatformForUpdate(userId, platformCode);
    }

    private long sanitizeRemainCount(Long remainCount) {
        return remainCount == null ? 0L : Math.max(remainCount, 0L);
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
        return SecurityUtils.hasRole("agent");
    }

    private boolean hasAuditReadPermission() {
        return SecurityUtils.hasPermi("server:markAdmin:audit:order:list")
                || SecurityUtils.hasPermi("server:markAdmin:audit:wallet:list");
    }
}
