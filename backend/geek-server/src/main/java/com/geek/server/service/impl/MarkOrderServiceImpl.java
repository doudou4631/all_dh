package com.geek.server.service.impl;

import com.geek.common.exception.ServiceException;
import com.geek.common.utils.DateUtils;
import com.geek.common.utils.SecurityUtils;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.MarkOrderItem;
import com.geek.server.domain.MarkUserPlatformPrice;
import com.geek.server.domain.MarkWalletLog;
import com.geek.server.domain.dto.MarkOrderCreateRequest;
import com.geek.server.domain.dto.MarkOrderItemProcessRequest;
import com.geek.server.domain.vo.MarkOrderDetailVO;
import com.geek.server.domain.vo.MarkWalletSummaryVO;
import com.geek.server.mapper.MarkOrderItemMapper;
import com.geek.server.mapper.MarkOrderMapper;
import com.geek.server.mapper.MarkUserPlatformPriceMapper;
import com.geek.server.mapper.MarkWalletLogMapper;
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

    private static final Map<String, String> PLATFORM_NAME_MAP = new LinkedHashMap<>();

    static {
        PLATFORM_NAME_MAP.put("mobile_gaopin", "高频拦截");
        PLATFORM_NAME_MAP.put("td_gaopin", "泰迪高频");
        PLATFORM_NAME_MAP.put("td_second", "泰迪二次");
        PLATFORM_NAME_MAP.put("qihu_first", "360首次");
        PLATFORM_NAME_MAP.put("qihu_second", "360二次");
        PLATFORM_NAME_MAP.put("dianhuabang", "电话邦");
        PLATFORM_NAME_MAP.put("tencent_mark", "腾讯");
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
    private SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkOrderDetailVO createOrder(MarkOrderCreateRequest request) {
        Long currentUserId = SecurityUtils.getUserId();
        String currentUserName = SecurityUtils.getUsername();
        if (request == null || StringUtils.isBlank(request.getPlatformCode())) {
            throw new ServiceException("平台编码不能为空");
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
            return 1L;
        }
        return platformPrice.getUnitPrice();
    }

    private List<MarkUserPlatformPrice> buildPlatformPriceListByUser(Long userId) {
        MarkUserPlatformPrice query = new MarkUserPlatformPrice();
        query.setUserId(userId);
        List<MarkUserPlatformPrice> savedPrices = markUserPlatformPriceMapper.selectMarkUserPlatformPriceList(query);
        Map<String, MarkUserPlatformPrice> savedMap = savedPrices.stream()
                .collect(Collectors.toMap(MarkUserPlatformPrice::getPlatformCode, item -> item, (a, b) -> a, LinkedHashMap::new));
        List<MarkUserPlatformPrice> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : PLATFORM_NAME_MAP.entrySet()) {
            MarkUserPlatformPrice price = savedMap.get(entry.getKey());
            if (price == null) {
                price = new MarkUserPlatformPrice();
                price.setUserId(userId);
                price.setPlatformCode(entry.getKey());
                price.setPlatformName(entry.getValue());
                price.setUnitPrice(1L);
            }
            result.add(price);
        }
        for (MarkUserPlatformPrice extra : savedPrices) {
            if (!PLATFORM_NAME_MAP.containsKey(extra.getPlatformCode())) {
                result.add(extra);
            }
        }
        return result;
    }

    private String resolvePlatformName(String platformCode, String requestPlatformName) {
        if (StringUtils.isNotBlank(requestPlatformName)) {
            return requestPlatformName.trim();
        }
        String name = PLATFORM_NAME_MAP.get(platformCode);
        return StringUtils.isBlank(name) ? platformCode : name;
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
