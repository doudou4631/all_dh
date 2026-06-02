package com.geek.server.service.impl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.common.core.domain.entity.SysMenu;

import com.geek.common.exception.ServiceException;
import com.geek.common.utils.DateUtils;
import com.geek.common.utils.SecurityUtils;
import com.geek.common.utils.ip.IpUtils;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.MarkOrderItem;
import com.geek.server.domain.MarkPlatformTemplate;
import com.geek.server.domain.MarkUserPlatformPrice;
import com.geek.server.domain.MarkWalletLog;
import com.geek.server.domain.entity.BatchTask;
import com.geek.server.domain.dto.MarkOrderCreateRequest;
import com.geek.server.domain.dto.MarkOrderItemProcessRequest;
import com.geek.server.domain.vo.FreeSingleQueryRequest;
import com.geek.server.domain.vo.MarkOrderDetailVO;
import com.geek.server.domain.vo.MarkOrderPrecheckResultVO;
import com.geek.server.domain.vo.MarkPhoneCheckItemVO;
import com.geek.server.domain.vo.MarkWalletSummaryVO;
import com.geek.server.mapper.MarkOrderItemMapper;
import com.geek.server.mapper.MarkOrderMapper;
import com.geek.server.mapper.MarkPlatformTemplateMapper;
import com.geek.server.mapper.MarkUserPlatformPriceMapper;
import com.geek.server.mapper.MarkWalletLogMapper;
import com.geek.server.service.IFreeQueryService;
import com.geek.server.service.IMarkOrderService;
import com.geek.system.mapper.SysUserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * 迁移订单/钱包服务实现
 */
@Service
public class MarkOrderServiceImpl implements IMarkOrderService {

    private static final Long MARK_ROOT_MENU_ID = 900100000001L;
    private static final String MARK_USER_MENU_COMPONENT = "server/mark/user/index";
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

        long unitPrice = getEffectiveUnitPrice(currentUserId, request.getPlatformCode());
        long totalAmount = unitPrice * normalizedPhones.size();
        SysUser currentUser = requireUser(currentUserId);
        long balanceBefore = currentUser.getPoints() == null ? 0L : currentUser.getPoints();
        if (balanceBefore < totalAmount) {
            throw new ServiceException("积分不足，无法下单");
        }
        long balanceAfter = balanceBefore - totalAmount;
        currentUser.setPoints(safePointValue(balanceAfter));
        sysUserMapper.update(currentUser);

        Date now = DateUtils.getNowDate();
        MarkOrder order = new MarkOrder();
        order.setOrderNo(generateOrderNo());
        order.setRequestNo(StringUtils.isBlank(requestNo) ? null : requestNo);
        order.setUserId(currentUserId);
        order.setPlatformCode(request.getPlatformCode());
        order.setPlatformName(resolvePlatformName(request.getPlatformCode(), request.getPlatformName()));
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
        resultVO.setPlatformName(resolvePlatformName(request.getPlatformCode(), request.getPlatformName()));

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
    public List<MarkOrder> selectMyOrderList(MarkOrder query) {
        MarkOrder orderQuery = query == null ? new MarkOrder() : query;
        orderQuery.setUserId(SecurityUtils.getUserId());
        return markOrderMapper.selectMarkOrderList(orderQuery);
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
        SysUser user = requireUser(currentUserId);
        MarkWalletLog query = new MarkWalletLog();
        query.setUserId(currentUserId);
        List<MarkWalletLog> logs = markWalletLogMapper.selectMarkWalletLogList(query);

        long totalDeduct = 0L;
        long totalRefund = 0L;
        for (MarkWalletLog log : logs) {
            long amount = log.getChangeAmount() == null ? 0L : log.getChangeAmount();
            if (amount < 0) {
                totalDeduct += Math.abs(amount);
            } else {
                totalRefund += amount;
            }
        }

        MarkWalletSummaryVO summaryVO = new MarkWalletSummaryVO();
        summaryVO.setUserId(currentUserId);
        summaryVO.setPointsBalance(user.getPoints() == null ? 0 : user.getPoints());
        summaryVO.setTotalDeductAmount(totalDeduct);
        summaryVO.setTotalRefundAmount(totalRefund);
        summaryVO.setPlatformPrices(selectMyPlatformPriceList());
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
    public List<MarkOrder> selectAdminAuditOrderList(MarkOrder query) {
        if (!isAdminRole()) {
            throw new ServiceException("仅管理员可查看订单审计");
        }
        MarkOrder orderQuery = query == null ? new MarkOrder() : query;
        return markOrderMapper.selectMarkOrderList(orderQuery);
    }

    @Override
    public List<MarkWalletLog> selectAdminWalletLogList(MarkWalletLog query) {
        if (!isAdminRole()) {
            throw new ServiceException("仅管理员可查看流水审计");
        }
        MarkWalletLog walletLogQuery = query == null ? new MarkWalletLog() : query;
        return markWalletLogMapper.selectMarkWalletLogList(walletLogQuery);
    }

    private void refundOrderItem(MarkOrder order, MarkOrderItem item, String currentUserName, Date now) {
        SysUser user = requireUser(order.getUserId());
        long balanceBefore = user.getPoints() == null ? 0L : user.getPoints();
        long refundAmount = item.getItemAmount() == null ? 0L : item.getItemAmount();
        long balanceAfter = balanceBefore + refundAmount;
        user.setPoints(safePointValue(balanceAfter));
        sysUserMapper.update(user);

        insertWalletLog(
                order.getUserId(),
                order.getId(),
                item.getId(),
                "REFUND",
                refundAmount,
                balanceBefore,
                balanceAfter,
                "明细失败自动退款",
                currentUserName,
                now
        );
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
        Set<String> explicitlyConfiguredCodes = new LinkedHashSet<>(savedMap.keySet());
        Map<String, String> dynamicPlatformNameMap = resolveMenuPlatformNameMap();
        Map<String, String> platformNameMap = resolvePlatformNameMap(dynamicPlatformNameMap);
        Map<String, TemplatePlatformConfig> templateConfigMap = resolveTemplatePlatformConfigMapByUser(userId);
        Set<String> availableCodes = resolveAvailablePlatformCodes(explicitlyConfiguredCodes, dynamicPlatformNameMap, templateConfigMap);
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
            result.add(price);
        }
        return result;
    }

    private Set<String> resolveAvailablePlatformCodes(Set<String> explicitlyConfiguredCodes,
                                                      Map<String, String> dynamicPlatformNameMap,
                                                      Map<String, TemplatePlatformConfig> templateConfigMap) {
        if (explicitlyConfiguredCodes != null && !explicitlyConfiguredCodes.isEmpty()) {
            return new LinkedHashSet<>(explicitlyConfiguredCodes);
        }
        if (templateConfigMap != null && !templateConfigMap.isEmpty()) {
            return new LinkedHashSet<>(templateConfigMap.keySet());
        }
        if (dynamicPlatformNameMap != null && !dynamicPlatformNameMap.isEmpty()) {
            return new LinkedHashSet<>(dynamicPlatformNameMap.keySet());
        }
        return new LinkedHashSet<>(LEGACY_PLATFORM_NAME_MAP.keySet());
    }
    private Map<String, TemplatePlatformConfig> resolveTemplatePlatformConfigMapByUser(Long userId) {
        Map<String, TemplatePlatformConfig> configMap = new LinkedHashMap<>();
        SysUser user = requireUser(userId);
        Long templateId = user.getRelMarkTemplate();
        if (templateId == null) {
            return configMap;
        }
        MarkPlatformTemplate template = markPlatformTemplateMapper.selectMarkPlatformTemplateById(templateId);
        if (template == null || !"0".equals(template.getStatus())) {
            return configMap;
        }
        if (!canUseTemplate(user, template)) {
            return configMap;
        }
        return parseTemplatePlatformConfigs(template.getTemplateInfo());
    }

    private boolean canUseTemplate(SysUser user, MarkPlatformTemplate template) {
        if (user == null || template == null) {
            return false;
        }
        Long ownerUserId = template.getOwnerUserId();
        if (ownerUserId != null) {
            if (user.getUserId() != null && ownerUserId.equals(user.getUserId())) {
                return true;
            }
            Long creatorUserId = resolveUserIdByUserName(user.getCreateBy());
            return creatorUserId != null && ownerUserId.equals(creatorUserId);
        }
        String userName = StringUtils.trimToNull(user.getUserName());
        String creatorName = StringUtils.trimToNull(user.getCreateBy());
        String templateCreatorName = StringUtils.trimToNull(template.getCreateBy());
        if (templateCreatorName == null) {
            return false;
        }
        return StringUtils.equals(templateCreatorName, userName) || StringUtils.equals(templateCreatorName, creatorName);
    }

    private Long resolveUserIdByUserName(String userName) {
        String normalizedUserName = StringUtils.trimToNull(userName);
        if (normalizedUserName == null) {
            return null;
        }
        SysUser creator = com.mybatisflex.core.query.QueryChain.of(SysUser.class)
                .select(SysUser::getUserId)
                .eq(SysUser::getUserName, normalizedUserName)
                .one();
        return creator == null ? null : creator.getUserId();
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
        } catch (Exception ignored) {
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
        walletLog.setBizType(bizType);
        walletLog.setChangeAmount(changeAmount);
        walletLog.setBalanceBefore(balanceBefore);
        walletLog.setBalanceAfter(balanceAfter);
        walletLog.setRemark(remark);
        walletLog.setCreateBy(createBy);
        walletLog.setCreateTime(createTime);
        markWalletLogMapper.insertMarkWalletLog(walletLog);
    }

    private int safePointValue(long points) {
        if (points < Integer.MIN_VALUE || points > Integer.MAX_VALUE) {
            throw new ServiceException("积分值超出可用范围");
        }
        return (int) points;
    }

    private boolean isAdminRole() {
        return SecurityUtils.isAdmin() || SecurityUtils.hasRole("admin");
    }

    private boolean isAgentRole() {
        return SecurityUtils.hasRole("agent");
    }
}
