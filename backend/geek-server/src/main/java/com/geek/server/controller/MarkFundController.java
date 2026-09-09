package com.geek.server.controller;

import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.server.domain.MarkWalletLog;
import com.geek.server.service.IMarkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "【标记业务】总资金明细")
@RestController
@RequestMapping("/server/mark/fund/total")
@RequiredArgsConstructor
public class MarkFundController extends BaseController {
    private final IMarkOrderService markOrderService;

    @Operation(summary = "全部用户资金流水")
    @PreAuthorize("@ss.hasPermi('server:markFund:total:list')")
    @GetMapping("/list")
    public TableDataInfo list(MarkWalletLog query) {
        startPage();
        return getDataTable(markOrderService.selectAdminWalletLogList(query));
    }

    @Operation(summary = "全部用户资金流水汇总")
    @PreAuthorize("@ss.hasPermi('server:markFund:total:list')")
    @GetMapping("/summary")
    public AjaxResult summary(MarkWalletLog query) {
        return success(markOrderService.selectAdminWalletLogSummary(query));
    }

    @Operation(summary = "资金流水平台选项")
    @PreAuthorize("@ss.hasPermi('server:markFund:total:list')")
    @GetMapping("/platform/options")
    public AjaxResult platformOptions() {
        return success(markOrderService.selectAdminWalletPlatformOptions());
    }
}
