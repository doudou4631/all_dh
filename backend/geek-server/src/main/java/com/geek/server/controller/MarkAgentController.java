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
import com.geek.server.domain.dto.MarkOrderItemProcessRequest;
import com.geek.server.service.IMarkOrderService;
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

    @Operation(summary = "代理订单列表")
    @PreAuthorize("@ss.hasPermi('server:markAgent:order:list')")
    @GetMapping("/order/list")
    public TableDataInfo listOrder(MarkOrder query) {
        startPage();
        List<MarkOrder> list = markOrderService.selectAgentOrderList(query);
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

    @Operation(summary = "代理回填处理结果")
    @PreAuthorize("@ss.hasPermi('server:markAgent:item:feedback')")
    @Log(title = "代理回填处理结果", businessType = BusinessType.UPDATE)
    @PostMapping("/item/{itemId}/feedback")
    public AjaxResult feedback(@PathVariable Long itemId, @Valid @RequestBody MarkOrderItemProcessRequest request) {
        return AjaxResult.success("回填成功", markOrderService.feedbackOrderItem(itemId, request));
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
