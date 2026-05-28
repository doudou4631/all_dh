package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.MarkArbitrationCase;
import com.geek.server.domain.MarkGovernRule;
import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.MarkWalletLog;
import com.geek.server.service.IMarkGovernService;
import com.geek.server.service.IMarkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端治理/仲裁/审计控制器
 */
@Tag(name = "【迁移管理端】管理")
@RestController
@RequestMapping("/server/markAdmin")
@RequiredArgsConstructor
public class MarkAdminController extends BaseController {

    private final IMarkOrderService markOrderService;
    private final IMarkGovernService markGovernService;

    @Operation(summary = "治理规则列表")
    @PreAuthorize("@ss.hasPermi('server:markAdmin:rule:list')")
    @GetMapping("/rule/list")
    public TableDataInfo ruleList(MarkGovernRule query) {
        startPage();
        List<MarkGovernRule> list = markGovernService.selectMarkGovernRuleList(query);
        return getDataTable(list);
    }

    @Operation(summary = "治理规则详情")
    @PreAuthorize("@ss.hasPermi('server:markAdmin:rule:query')")
    @GetMapping("/rule/{id}")
    public AjaxResult ruleDetail(@PathVariable Long id) {
        return success(markGovernService.selectMarkGovernRuleById(id));
    }

    @Operation(summary = "新增治理规则")
    @PreAuthorize("@ss.hasPermi('server:markAdmin:rule:add')")
    @Log(title = "治理规则", businessType = BusinessType.INSERT)
    @PostMapping("/rule")
    public AjaxResult addRule(@RequestBody MarkGovernRule markGovernRule) {
        return toAjax(markGovernService.insertMarkGovernRule(markGovernRule));
    }

    @Operation(summary = "修改治理规则")
    @PreAuthorize("@ss.hasPermi('server:markAdmin:rule:edit')")
    @Log(title = "治理规则", businessType = BusinessType.UPDATE)
    @PutMapping("/rule")
    public AjaxResult editRule(@RequestBody MarkGovernRule markGovernRule) {
        return toAjax(markGovernService.updateMarkGovernRule(markGovernRule));
    }

    @Operation(summary = "删除治理规则")
    @PreAuthorize("@ss.hasPermi('server:markAdmin:rule:remove')")
    @Log(title = "治理规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/rule/{ids}")
    public AjaxResult removeRule(@PathVariable Long[] ids) {
        return toAjax(markGovernService.deleteMarkGovernRuleByIds(ids));
    }

    @Operation(summary = "仲裁工单列表")
    @PreAuthorize("@ss.hasPermi('server:markAdmin:case:list')")
    @GetMapping("/case/list")
    public TableDataInfo caseList(MarkArbitrationCase query) {
        startPage();
        List<MarkArbitrationCase> list = markGovernService.selectMarkArbitrationCaseList(query);
        return getDataTable(list);
    }

    @Operation(summary = "仲裁工单详情")
    @PreAuthorize("@ss.hasPermi('server:markAdmin:case:query')")
    @GetMapping("/case/{id}")
    public AjaxResult caseDetail(@PathVariable Long id) {
        return success(markGovernService.selectMarkArbitrationCaseById(id));
    }

    @Operation(summary = "新增仲裁工单")
    @PreAuthorize("@ss.hasPermi('server:markAdmin:case:add')")
    @Log(title = "仲裁工单", businessType = BusinessType.INSERT)
    @PostMapping("/case")
    public AjaxResult addCase(@RequestBody MarkArbitrationCase markArbitrationCase) {
        return toAjax(markGovernService.insertMarkArbitrationCase(markArbitrationCase));
    }

    @Operation(summary = "更新仲裁工单")
    @PreAuthorize("@ss.hasPermi('server:markAdmin:case:edit')")
    @Log(title = "仲裁工单", businessType = BusinessType.UPDATE)
    @PutMapping("/case")
    public AjaxResult editCase(@RequestBody MarkArbitrationCase markArbitrationCase) {
        return toAjax(markGovernService.updateMarkArbitrationCase(markArbitrationCase));
    }

    @Operation(summary = "订单审计列表")
    @PreAuthorize("@ss.hasPermi('server:markAdmin:audit:order:list')")
    @GetMapping("/audit/order/list")
    public TableDataInfo auditOrderList(MarkOrder query) {
        startPage();
        List<MarkOrder> list = markOrderService.selectAdminAuditOrderList(query);
        return getDataTable(list);
    }

    @Operation(summary = "流水审计列表")
    @PreAuthorize("@ss.hasPermi('server:markAdmin:audit:wallet:list')")
    @GetMapping("/audit/wallet/list")
    public TableDataInfo auditWalletList(MarkWalletLog query) {
        startPage();
        List<MarkWalletLog> list = markOrderService.selectAdminWalletLogList(query);
        return getDataTable(list);
    }
}
