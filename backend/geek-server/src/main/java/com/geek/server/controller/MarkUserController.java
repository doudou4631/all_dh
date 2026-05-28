package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.MarkWalletLog;
import com.geek.server.domain.dto.MarkOrderCreateRequest;
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
 * 用户端迁移流程控制器
 */
@Tag(name = "【迁移用户端】管理")
@RestController
@RequestMapping("/server/markUser")
@RequiredArgsConstructor
public class MarkUserController extends BaseController {

    private final IMarkOrderService markOrderService;

    @Operation(summary = "用户订单列表")
    @PreAuthorize("@ss.hasPermi('server:markUser:order:list')")
    @GetMapping("/order/list")
    public TableDataInfo listOrder(MarkOrder query) {
        startPage();
        List<MarkOrder> list = markOrderService.selectMyOrderList(query);
        return getDataTable(list);
    }

    @Operation(summary = "用户提交订单")
    @PreAuthorize("@ss.hasPermi('server:markUser:order:add')")
    @Log(title = "迁移用户订单", businessType = BusinessType.INSERT)
    @PostMapping("/order")
    public AjaxResult createOrder(@Valid @RequestBody MarkOrderCreateRequest request) {
        return AjaxResult.success("下单成功", markOrderService.createOrder(request));
    }

    @Operation(summary = "用户订单详情")
    @PreAuthorize("@ss.hasPermi('server:markUser:order:query')")
    @GetMapping("/order/{orderId}")
    public AjaxResult orderDetail(@PathVariable Long orderId) {
        return success(markOrderService.selectMyOrderDetail(orderId));
    }

    @Operation(summary = "钱包汇总")
    @PreAuthorize("@ss.hasPermi('server:markUser:wallet:list')")
    @GetMapping("/wallet/summary")
    public AjaxResult walletSummary() {
        return success(markOrderService.selectMyWalletSummary());
    }

    @Operation(summary = "钱包流水")
    @PreAuthorize("@ss.hasPermi('server:markUser:wallet:log:list')")
    @GetMapping("/wallet/log/list")
    public TableDataInfo walletLogList(MarkWalletLog query) {
        startPage();
        List<MarkWalletLog> list = markOrderService.selectMyWalletLogList(query);
        return getDataTable(list);
    }

    @Operation(summary = "我的平台单价")
    @PreAuthorize("@ss.hasPermi('server:markUser:price:list')")
    @GetMapping("/price/list")
    public AjaxResult myPlatformPriceList() {
        return success(markOrderService.selectMyPlatformPriceList());
    }
}
