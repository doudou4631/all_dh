package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.MarkUserPlatformPrice;
import com.geek.server.domain.MarkWalletLog;
import com.geek.server.domain.dto.MarkAgentPlatformQuotaAdjustRequest;
import com.geek.server.domain.dto.MarkOrderAuditRequest;
import com.geek.server.domain.dto.MarkOrderItemBatchIdsRequest;
import com.geek.server.domain.dto.MarkOrderItemProcessRequest;
import com.geek.server.domain.vo.MarkAgentDownstreamSummaryVO;
import com.geek.server.domain.vo.MarkAgentMeSummaryVO;
import com.geek.server.domain.vo.MarkAgentOrderItemVO;
import com.geek.server.service.IMarkOrderService;
import com.geek.server.service.IMarkUserNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 代理端迁移流程控制器
 */
@Tag(name = "【迁移代理端】管理")
@RestController
@RequestMapping("/server/markAgent")
@RequiredArgsConstructor
public class MarkAgentController extends BaseController {

    private final IMarkOrderService markOrderService;
    private final IMarkUserNoticeService markUserNoticeService;

    @Operation(summary = "代理审核统计")
    @PreAuthorize("@ss.hasPermi('server:markAgent:audit:stats')")
    @GetMapping("/audit/stats")
    public AjaxResult auditStats() {
        return success(markUserNoticeService.selectAgentAuditStats());
    }

    @Operation(summary = "代理待审核订单")
    @PreAuthorize("@ss.hasPermi('server:markAgent:audit:list')")
    @GetMapping("/audit/pending")
    public TableDataInfo listAuditPending(MarkOrder query) {
        startPage();
        List<MarkOrder> list = markOrderService.selectAgentAuditPendingList(query);
        return getDataTable(list);
    }

    @Operation(summary = "代理审核历史")
    @PreAuthorize("@ss.hasPermi('server:markAgent:audit:list')")
    @GetMapping("/audit/history")
    public TableDataInfo listAuditHistory(MarkOrder query) {
        startPage();
        List<MarkOrder> list = markOrderService.selectAgentAuditHistoryList(query);
        return getDataTable(list);
    }

    @Operation(summary = "代理审核通过")
    @PreAuthorize("@ss.hasPermi('server:markAgent:audit:pass')")
    @Log(title = "代理审核通过", businessType = BusinessType.UPDATE)
    @PostMapping("/audit/{orderId}/pass")
    public AjaxResult auditPass(@PathVariable Long orderId, @RequestBody(required = false) MarkOrderAuditRequest request) {
        return AjaxResult.success("审核通过", markOrderService.auditOrderPass(orderId, request));
    }

    @Operation(summary = "代理审核拒绝")
    @PreAuthorize("@ss.hasPermi('server:markAgent:audit:reject')")
    @Log(title = "代理审核拒绝", businessType = BusinessType.UPDATE)
    @PostMapping("/audit/{orderId}/reject")
    public AjaxResult auditReject(@PathVariable Long orderId, @RequestBody MarkOrderAuditRequest request) {
        return AjaxResult.success("已拒绝", markOrderService.auditOrderReject(orderId, request));
    }

    @Operation(summary = "代理审核打回")
    @PreAuthorize("@ss.hasPermi('server:markAgent:audit:return')")
    @Log(title = "代理审核打回", businessType = BusinessType.UPDATE)
    @PostMapping("/audit/{orderId}/return")
    public AjaxResult auditReturn(@PathVariable Long orderId, @RequestBody MarkOrderAuditRequest request) {
        return AjaxResult.success("已打回", markOrderService.auditOrderReturn(orderId, request));
    }

    @Operation(summary = "代理订单列表")
    @PreAuthorize("@ss.hasPermi('server:markAgent:order:list')")
    @GetMapping("/order/list")
    public TableDataInfo listOrder(MarkOrder query) {
        startPage();
        List<MarkOrder> list = markOrderService.selectAgentOrderList(query);
        return getDataTable(list);
    }

    @Operation(summary = "代理处理明细列表")
    @PreAuthorize("@ss.hasPermi('server:markAgent:order:list')")
    @GetMapping("/item/list")
    public TableDataInfo listOrderItem(MarkAgentOrderItemVO query) {
        startPage();
        List<MarkAgentOrderItemVO> list = markOrderService.selectAgentOrderItemList(query);
        return getDataTable(list);
    }

    @Operation(summary = "代理订单详情")
    @PreAuthorize("@ss.hasPermi('server:markAgent:order:query')")
    @GetMapping("/order/{orderId}")
    public AjaxResult orderDetail(@PathVariable Long orderId) {
        return success(markOrderService.selectAgentOrderDetail(orderId));
    }
    @Operation(summary = "代理查看下线流水")
    @PreAuthorize("@ss.hasPermi('server:markAgent:wallet:list')")
    @GetMapping("/wallet/log/list")
    public TableDataInfo walletLogList(MarkWalletLog query) {
        startPage();
        List<MarkWalletLog> list = markOrderService.selectAgentWalletLogList(query);
        return getDataTable(list);
    }

    @Operation(summary = "代理下线账户概览")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/downstream/summary")
    public TableDataInfo downstreamSummary() {
        startPage();
        List<MarkAgentDownstreamSummaryVO> list = markOrderService.selectAgentDownstreamSummaryList();
        return getDataTable(list);
    }

    @Operation(summary = "当前代理账户概览")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/me/summary")
    public AjaxResult meSummary() {
        return success(markOrderService.selectAgentMeSummary());
    }

    @Operation(summary = "代理模板平台列表")
    @PreAuthorize("@ss.hasPermi('server:markAgent:order:query')")
    @GetMapping("/platform/list")
    public AjaxResult platformList() {
        return success(markOrderService.selectMyPlatformPriceList());
    }

    @Operation(summary = "代理回填处理结果")
    @PreAuthorize("@ss.hasPermi('server:markAgent:item:feedback')")
    @Log(title = "代理回填处理结果", businessType = BusinessType.UPDATE)
    @PostMapping("/item/{itemId}/feedback")
    public AjaxResult feedback(@PathVariable Long itemId, @Valid @RequestBody MarkOrderItemProcessRequest request) {
        return AjaxResult.success("回填成功", markOrderService.feedbackOrderItem(itemId, request));
    }

    @Operation(summary = "泰迪高频待处理订单自动检测")
    @PreAuthorize("@ss.hasPermi('server:markAgent:order:list')")
    @PostMapping("/item/autoDetectTdGaopin")
    public AjaxResult autoDetectTdGaopin() {
        markOrderService.processTdGaopinPendingItemsAuto();
        return success("自动检测已执行");
    }

    @Operation(summary = "小米待处理订单自动检测")
    @PreAuthorize("@ss.hasPermi('server:markAgent:order:list')")
    @PostMapping("/item/autoDetectXiaomi")
    public AjaxResult autoDetectXiaomi() {
        markOrderService.processXiaomiPendingItemsAuto();
        return success("自动检测已执行");
    }

    @Operation(summary = "小米批量处理（开启自动检测）")
    @PreAuthorize("@ss.hasPermi('server:markAgent:item:feedback')")
    @Log(title = "小米批量处理", businessType = BusinessType.UPDATE)
    @PostMapping({"/item/batchProcessXiaomi", "/item/batchMarkXiaomiSubmitted"})
    public AjaxResult batchProcessXiaomi(@Valid @RequestBody MarkOrderItemBatchIdsRequest request) {
        return AjaxResult.success("批量处理已开启", markOrderService.batchProcessXiaomiItems(request.getItemIds()));
    }

    @Operation(summary = "小米批量手动检测")
    @PreAuthorize("@ss.hasPermi('server:markAgent:item:feedback')")
    @Log(title = "小米批量检测", businessType = BusinessType.UPDATE)
    @PostMapping("/item/batchDetectXiaomi")
    public AjaxResult batchDetectXiaomi(@Valid @RequestBody MarkOrderItemBatchIdsRequest request) {
        return AjaxResult.success("批量检测完成", markOrderService.batchDetectXiaomiItems(request.getItemIds()));
    }

    @Operation(summary = "代理批量标记成功")
    @PreAuthorize("@ss.hasPermi('server:markAgent:item:feedback')")
    @Log(title = "代理批量标记成功", businessType = BusinessType.UPDATE)
    @PostMapping("/item/batchMarkSuccess")
    public AjaxResult batchMarkSuccess(@Valid @RequestBody MarkOrderItemBatchIdsRequest request) {
        return AjaxResult.success("批量标记成功", markOrderService.batchMarkSuccessOrderItems(request.getItemIds()));
    }

    @Operation(summary = "代理整单处理（完成/成功/失败）")
    @PreAuthorize("@ss.hasPermi('server:markAgent:order:complete')")
    @Log(title = "代理整单处理", businessType = BusinessType.UPDATE)
    @PostMapping("/order/{orderId}/complete")
    public AjaxResult complete(@PathVariable Long orderId, @RequestBody(required = false) MarkOrderItemProcessRequest request) {
        if (request != null && StringUtils.isNotBlank(request.getProcessStatus())) {
            return AjaxResult.success("整单处理完成", markOrderService.completeOrder(orderId, request));
        }
        return AjaxResult.success("整单已完成", markOrderService.completeOrder(orderId));
    }

    @Operation(summary = "代理查询下线用户平台次数")
    @PreAuthorize("@ss.hasPermi('server:pointRecord:add')")
    @GetMapping("/quota/platformOptions/{userId}")
    public AjaxResult platformOptions(@PathVariable Long userId) {
        List<MarkUserPlatformPrice> list = markOrderService.selectAgentUserPlatformPriceList(userId);
        return success(list);
    }

    @Operation(summary = "代理给下线按平台充值/扣减")
    @PreAuthorize("@ss.hasPermi('server:pointRecord:add')")
    @Log(title = "代理平台次数调整", businessType = BusinessType.UPDATE)
    @PostMapping("/quota/adjust")
    public AjaxResult adjustQuota(@Valid @RequestBody MarkAgentPlatformQuotaAdjustRequest request) {
        return AjaxResult.success("操作成功", markOrderService.adjustAgentUserPlatformQuota(request));
    }
}
