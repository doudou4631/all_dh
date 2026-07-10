package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.MarkUserNotice;
import com.geek.server.domain.MarkWalletLog;
import com.geek.server.domain.dto.MarkOrderCreateRequest;
import com.geek.server.domain.dto.MarkTdxSecondSendCodeRequest;
import com.geek.server.domain.dto.MarkTdxSecondSubmitRequest;
import com.geek.server.domain.dto.MarkTencentStatusQueryRequest;
import com.geek.server.domain.dto.MarkTencentSubmitRequest;
import com.geek.server.service.IMarkOrderService;
import com.geek.server.service.IMarkUserNoticeService;
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
    private final IMarkUserNoticeService markUserNoticeService;

    @Operation(summary = "用户提交记录（我的订单）")
    @PreAuthorize("@ss.hasPermi('server:markUser:order:list')")
    @GetMapping("/submission/list")
    public TableDataInfo listSubmission(MarkOrder query) {
        startPage();
        List<MarkOrder> list = markOrderService.selectMyOrderList(query);
        return getDataTable(list);
    }

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
    @Operation(summary = "用户提交消除订单")
    @PreAuthorize("@ss.hasPermi('server:markUser:order:clear')")
    @Log(title = "迁移用户订单消除提交", businessType = BusinessType.INSERT)
    @PostMapping("/order/clear")
    public AjaxResult createClearOrder(@Valid @RequestBody MarkOrderCreateRequest request) {
        return AjaxResult.success("下单成功", markOrderService.createOrder(request));
    }

    @Operation(summary = "用户提交前预查询号码是否被标记")
    @PreAuthorize("@ss.hasPermi('server:markUser:order:precheck')")
    @PostMapping("/order/precheck")
    public AjaxResult precheckOrder(@Valid @RequestBody MarkOrderCreateRequest request) {
        return AjaxResult.success("预查询完成", markOrderService.precheckOrder(request));
    }

    @Operation(summary = "腾讯手机号验证码提交")
    @PreAuthorize("@ss.hasPermi('server:markUser:order:add')")
    @Log(title = "腾讯手机号验证码提交", businessType = BusinessType.OTHER)
    @PostMapping("/tencent/submit")
    public AjaxResult submitTencent(@Valid @RequestBody MarkTencentSubmitRequest request) {
        return AjaxResult.success("提交完成", markOrderService.submitTencent(request));
    }

    @Operation(summary = "Taidixiong二次发送短信验证码")
    @PreAuthorize("@ss.hasPermi('server:markUser:order:add')")
    @Log(title = "Taidixiong二次发送短信验证码", businessType = BusinessType.OTHER)
    @PostMapping("/tdxSecond/sendCode")
    public AjaxResult sendTdxSecondCode(@Valid @RequestBody MarkTdxSecondSendCodeRequest request) {
        return AjaxResult.success("短信验证码已发送", markOrderService.sendTdxSecondCode(request));
    }

    @Operation(summary = "Taidixiong二次申诉提交")
    @PreAuthorize("@ss.hasPermi('server:markUser:order:add')")
    @Log(title = "Taidixiong二次申诉提交", businessType = BusinessType.OTHER)
    @PostMapping("/tdxSecond/submit")
    public AjaxResult submitTdxSecond(@Valid @RequestBody MarkTdxSecondSubmitRequest request) {
        return AjaxResult.success("提交完成", markOrderService.submitTdxSecond(request));
    }

    @Operation(summary = "腾讯提交结果查询")
    @PreAuthorize("@ss.hasPermi('server:markUser:order:query')")
    @GetMapping("/tencent/submit/result/{itemId}")
    public AjaxResult tencentSubmitResult(@PathVariable Long itemId) {
        return AjaxResult.success("查询完成", markOrderService.selectMyTencentSubmitResult(itemId));
    }

    @Operation(summary = "腾讯号码实时状态查询")
    @PreAuthorize("@ss.hasPermi('server:markUser:order:precheck')")
    @PostMapping("/tencent/status/query")
    public AjaxResult queryTencentStatus(@Valid @RequestBody MarkTencentStatusQueryRequest request) {
        return AjaxResult.success("查询完成", markOrderService.queryTencentStatus(request));
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

    @Operation(summary = "我的消息列表")
    @PreAuthorize("@ss.hasPermi('server:markUser:notice:list')")
    @GetMapping("/notice/list")
    public TableDataInfo noticeList(MarkUserNotice query) {
        startPage();
        List<MarkUserNotice> list = markUserNoticeService.selectMyNoticeList(query);
        return getDataTable(list);
    }

    @Operation(summary = "未读消息数量")
    @PreAuthorize("@ss.hasPermi('server:markUser:notice:list')")
    @GetMapping("/notice/unread/count")
    public AjaxResult noticeUnreadCount() {
        return success(markUserNoticeService.countMyUnread());
    }

    @Operation(summary = "消息详情")
    @PreAuthorize("@ss.hasPermi('server:markUser:notice:list')")
    @GetMapping("/notice/{noticeId}")
    public AjaxResult noticeDetail(@PathVariable Long noticeId) {
        return success(markUserNoticeService.selectMyNoticeDetail(noticeId));
    }

    @Operation(summary = "标记消息已读")
    @PreAuthorize("@ss.hasPermi('server:markUser:notice:read')")
    @PostMapping("/notice/{noticeId}/read")
    public AjaxResult noticeRead(@PathVariable Long noticeId) {
        return success(markUserNoticeService.markMyNoticeRead(noticeId));
    }

    @Operation(summary = "全部标记已读")
    @PreAuthorize("@ss.hasPermi('server:markUser:notice:read')")
    @PostMapping("/notice/readAll")
    public AjaxResult noticeReadAll() {
        return success(markUserNoticeService.markAllMyNoticeRead());
    }
}
