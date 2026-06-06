package com.geek.server.service;

import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.MarkUserPlatformPrice;
import com.geek.server.domain.MarkWalletLog;
import com.geek.server.domain.dto.MarkAgentPlatformQuotaAdjustRequest;
import com.geek.server.domain.dto.MarkOrderCreateRequest;
import com.geek.server.domain.dto.MarkOrderItemProcessRequest;
import com.geek.server.domain.vo.MarkAgentPlatformQuotaAdjustResultVO;
import com.geek.server.domain.vo.MarkOrderDetailVO;
import com.geek.server.domain.vo.MarkOrderPrecheckResultVO;
import com.geek.server.domain.vo.MarkWalletSummaryVO;

import java.util.List;

/**
 * 迁移订单/钱包服务
 */
public interface IMarkOrderService {

    MarkOrderDetailVO createOrder(MarkOrderCreateRequest request);

    MarkOrderPrecheckResultVO precheckOrder(MarkOrderCreateRequest request);

    List<MarkOrder> selectMyOrderList(MarkOrder query);

    MarkOrderDetailVO selectMyOrderDetail(Long orderId);

    MarkWalletSummaryVO selectMyWalletSummary();

    List<MarkWalletLog> selectMyWalletLogList(MarkWalletLog query);

    List<MarkUserPlatformPrice> selectMyPlatformPriceList();

    List<MarkOrder> selectAgentOrderList(MarkOrder query);

    MarkOrderDetailVO selectAgentOrderDetail(Long orderId);

    MarkOrderDetailVO feedbackOrderItem(Long itemId, MarkOrderItemProcessRequest request);

    MarkOrderDetailVO completeOrder(Long orderId);

    List<MarkOrder> selectAdminAuditOrderList(MarkOrder query);

    List<MarkWalletLog> selectAdminWalletLogList(MarkWalletLog query);

    List<MarkUserPlatformPrice> selectAgentUserPlatformPriceList(Long userId);

    MarkAgentPlatformQuotaAdjustResultVO adjustAgentUserPlatformQuota(MarkAgentPlatformQuotaAdjustRequest request);
}
