package com.geek.server.service;

import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.MarkUserPlatformPrice;
import com.geek.server.domain.MarkWalletLog;
import com.geek.server.domain.dto.MarkAgentPlatformQuotaAdjustRequest;
import com.geek.server.domain.dto.MarkOrderCreateRequest;
import com.geek.server.domain.dto.MarkOrderItemProcessRequest;
import com.geek.server.domain.dto.MarkTencentStatusQueryRequest;
import com.geek.server.domain.dto.MarkTencentSubmitRequest;
import com.geek.server.domain.vo.MarkAgentPlatformQuotaAdjustResultVO;
import com.geek.server.domain.vo.MarkOrderDetailVO;
import com.geek.server.domain.vo.MarkOrderPrecheckResultVO;
import com.geek.server.domain.vo.MarkTencentStatusQueryResultVO;
import com.geek.server.domain.vo.MarkTencentSubmitResultVO;
import com.geek.server.domain.vo.MarkWalletSummaryVO;

import java.util.List;

/**
 * 迁移订单/钱包服务
 */
public interface IMarkOrderService {

    MarkOrderDetailVO createOrder(MarkOrderCreateRequest request);

    MarkOrderPrecheckResultVO precheckOrder(MarkOrderCreateRequest request);
    MarkTencentStatusQueryResultVO queryTencentStatus(MarkTencentStatusQueryRequest request);
    MarkTencentSubmitResultVO submitTencent(MarkTencentSubmitRequest request);

    List<MarkOrder> selectMyOrderList(MarkOrder query);

    MarkOrderDetailVO selectMyOrderDetail(Long orderId);

    MarkWalletSummaryVO selectMyWalletSummary();

    List<MarkWalletLog> selectMyWalletLogList(MarkWalletLog query);

    List<MarkUserPlatformPrice> selectMyPlatformPriceList();

    List<MarkOrder> selectAgentOrderList(MarkOrder query);

    MarkOrderDetailVO selectAgentOrderDetail(Long orderId);
    List<MarkWalletLog> selectAgentWalletLogList(MarkWalletLog query);

    MarkOrderDetailVO feedbackOrderItem(Long itemId, MarkOrderItemProcessRequest request);

    MarkOrderDetailVO completeOrder(Long orderId);
    MarkOrderDetailVO completeOrder(Long orderId, MarkOrderItemProcessRequest request);

    List<MarkOrder> selectAdminAuditOrderList(MarkOrder query);

    List<MarkWalletLog> selectAdminWalletLogList(MarkWalletLog query);

    List<MarkUserPlatformPrice> selectAgentUserPlatformPriceList(Long userId);

    MarkAgentPlatformQuotaAdjustResultVO adjustAgentUserPlatformQuota(MarkAgentPlatformQuotaAdjustRequest request);
}
