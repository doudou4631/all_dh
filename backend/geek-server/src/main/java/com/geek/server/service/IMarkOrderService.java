package com.geek.server.service;

import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.MarkUserPlatformPrice;
import com.geek.server.domain.MarkWalletLog;
import com.geek.server.domain.dto.MarkAgentPlatformQuotaAdjustRequest;
import com.geek.server.domain.dto.MarkAgentPlatformStatusRequest;
import com.geek.server.domain.dto.MarkOrderAuditRequest;
import com.geek.server.domain.dto.MarkOrderCreateRequest;
import com.geek.server.domain.dto.MarkOrderItemProcessRequest;
import com.geek.server.domain.dto.MarkTdxSecondSendCodeRequest;
import com.geek.server.domain.dto.MarkTdxSecondSubmitRequest;
import com.geek.server.domain.dto.MarkTencentStatusQueryRequest;
import com.geek.server.domain.dto.MarkTencentSubmitRequest;
import com.geek.server.domain.vo.MarkAgentDownstreamSummaryVO;
import com.geek.server.domain.vo.MarkAgentMeSummaryVO;
import com.geek.server.domain.vo.MarkAgentOrderItemVO;
import com.geek.server.domain.vo.MarkAgentPlatformQuotaAdjustResultVO;
import com.geek.server.domain.vo.MarkOrderDetailVO;
import com.geek.server.domain.vo.MarkOrderPrecheckResultVO;
import com.geek.server.domain.vo.MarkTdxSecondSendCodeResultVO;
import com.geek.server.domain.vo.MarkTdxSecondSubmitResultVO;
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
    MarkTdxSecondSendCodeResultVO sendTdxSecondCode(MarkTdxSecondSendCodeRequest request);
    MarkTdxSecondSubmitResultVO submitTdxSecond(MarkTdxSecondSubmitRequest request);

    MarkTencentSubmitResultVO selectMyTencentSubmitResult(Long itemId);

    List<MarkOrder> selectMyOrderList(MarkOrder query);

    MarkOrderDetailVO selectMyOrderDetail(Long orderId);

    MarkWalletSummaryVO selectMyWalletSummary();

    List<MarkWalletLog> selectMyWalletLogList(MarkWalletLog query);

    List<MarkUserPlatformPrice> selectMyPlatformPriceList();

    List<MarkOrder> selectAgentOrderList(MarkOrder query);

    List<MarkAgentOrderItemVO> selectAgentOrderItemList(MarkAgentOrderItemVO query);

    MarkOrderDetailVO selectAgentOrderDetail(Long orderId);
    List<MarkWalletLog> selectAgentWalletLogList(MarkWalletLog query);

    List<MarkAgentDownstreamSummaryVO> selectAgentDownstreamSummaryList();

    MarkAgentMeSummaryVO selectAgentMeSummary();

    MarkOrderDetailVO feedbackOrderItem(Long itemId, MarkOrderItemProcessRequest request);

    MarkOrderDetailVO completeOrder(Long orderId);
    MarkOrderDetailVO completeOrder(Long orderId, MarkOrderItemProcessRequest request);

    List<MarkOrder> selectAgentAuditPendingList(MarkOrder query);

    List<MarkOrder> selectAgentAuditHistoryList(MarkOrder query);

    MarkOrderDetailVO auditOrderPass(Long orderId, MarkOrderAuditRequest request);

    MarkOrderDetailVO auditOrderReject(Long orderId, MarkOrderAuditRequest request);

    MarkOrderDetailVO auditOrderReturn(Long orderId, MarkOrderAuditRequest request);

    List<MarkOrder> selectAdminAuditOrderList(MarkOrder query);

    List<MarkWalletLog> selectAdminWalletLogList(MarkWalletLog query);

    List<MarkUserPlatformPrice> selectAgentUserPlatformPriceList(Long userId);

    MarkAgentPlatformQuotaAdjustResultVO adjustAgentUserPlatformQuota(MarkAgentPlatformQuotaAdjustRequest request);

    MarkUserPlatformPrice updateAgentUserPlatformStatus(MarkAgentPlatformStatusRequest request);

    void processTdGaopinPendingItemsAuto();

    void processXiaomiPendingItemsAuto();

    java.util.Map<String, Object> batchProcessXiaomiItems(java.util.List<Long> itemIds);

    java.util.Map<String, Object> batchMarkSuccessOrderItems(java.util.List<Long> itemIds);

    java.util.Map<String, Object> batchMarkFailedOrderItems(java.util.List<Long> itemIds);
}
