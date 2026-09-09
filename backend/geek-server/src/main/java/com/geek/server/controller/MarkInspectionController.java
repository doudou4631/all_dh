package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.MarkInspectionPolicy;
import com.geek.server.domain.MarkInspectionRunLog;
import com.geek.server.service.IMarkInspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/server/mark/inspection")
@RequiredArgsConstructor
public class MarkInspectionController extends BaseController {
    private final IMarkInspectionService service;

    @GetMapping("/policy/list") @PreAuthorize("@ss.hasPermi('server:markInspection:policy:list')")
    public TableDataInfo policyList(MarkInspectionPolicy query) { startPage(); return getDataTable(service.selectPolicies(query)); }
    @GetMapping("/policy/{id}") @PreAuthorize("@ss.hasPermi('server:markInspection:policy:list')")
    public AjaxResult policy(@PathVariable Long id) { return success(service.selectPolicy(id)); }
    @PostMapping("/policy") @PreAuthorize("@ss.hasPermi('server:markInspection:policy:add')")
    @Log(title="巡检策略", businessType=BusinessType.INSERT) public AjaxResult add(@RequestBody MarkInspectionPolicy policy) { return toAjax(service.savePolicy(policy)); }
    @PutMapping("/policy") @PreAuthorize("@ss.hasPermi('server:markInspection:policy:edit')")
    @Log(title="巡检策略", businessType=BusinessType.UPDATE) public AjaxResult edit(@RequestBody MarkInspectionPolicy policy) { return toAjax(service.updatePolicy(policy)); }
    @DeleteMapping("/policy/{id}") @PreAuthorize("@ss.hasPermi('server:markInspection:policy:remove')")
    @Log(title="巡检策略", businessType=BusinessType.DELETE) public AjaxResult remove(@PathVariable Long id) { return toAjax(service.deletePolicy(id)); }
    @PostMapping("/policy/{id}/run") @PreAuthorize("@ss.hasPermi('server:markInspection:policy:run')")
    public AjaxResult run(@PathVariable Long id) { return AjaxResult.success("巡检完成", service.runPolicy(id, "MANUAL")); }
    @GetMapping("/platform/options") @PreAuthorize("@ss.hasPermi('server:markInspection:policy:list')")
    public AjaxResult platforms() { return success(service.platformOptions()); }
    @GetMapping("/log/list") @PreAuthorize("@ss.hasPermi('server:markInspection:log:list')")
    public TableDataInfo logList(MarkInspectionRunLog query) { startPage(); return getDataTable(service.selectLogs(query)); }
    @GetMapping("/log/{id}/detail/list") @PreAuthorize("@ss.hasPermi('server:markInspection:log:list')")
    public TableDataInfo logDetailList(@PathVariable Long id) { startPage(); return getDataTable(service.selectLogDetails(id)); }
}
