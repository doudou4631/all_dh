package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.dto.MarkOrderItemProcessRequest;
import com.geek.server.service.IMarkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @Operation(summary = "代理回填处理结果")
    @PreAuthorize("@ss.hasPermi('server:markAgent:item:feedback')")
    @Log(title = "代理回填处理结果", businessType = BusinessType.UPDATE)
    @PostMapping("/item/{itemId}/feedback")
    public AjaxResult feedback(@PathVariable Long itemId, @Valid @RequestBody MarkOrderItemProcessRequest request) {
        return AjaxResult.success("回填成功", markOrderService.feedbackOrderItem(itemId, request));
    }

    @Operation(summary = "代理完成整单")
    @PreAuthorize("@ss.hasPermi('server:markAgent:order:complete')")
    @Log(title = "代理完成整单", businessType = BusinessType.UPDATE)
    @PostMapping("/order/{orderId}/complete")
    public AjaxResult complete(@PathVariable Long orderId) {
        return AjaxResult.success("整单已完成", markOrderService.completeOrder(orderId));
    }
}
