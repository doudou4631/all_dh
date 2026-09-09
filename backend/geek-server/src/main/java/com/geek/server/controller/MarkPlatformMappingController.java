package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.MarkPlatformMapping;
import com.geek.server.service.IMarkPlatformMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/server/mark/platformMapping")
public class MarkPlatformMappingController extends BaseController {
    @Autowired private IMarkPlatformMappingService service;
    @GetMapping("/list") @PreAuthorize("@ss.hasPermi('server:markPlatform:list')") public AjaxResult list(MarkPlatformMapping query) { return success(service.selectList(query)); }
    @PostMapping @PreAuthorize("@ss.hasPermi('server:markPlatform:add')") @Log(title="标记平台映射", businessType=BusinessType.INSERT) public AjaxResult add(@RequestBody MarkPlatformMapping item) { return toAjax(service.save(item)); }
    @PutMapping @PreAuthorize("@ss.hasPermi('server:markPlatform:edit')") @Log(title="标记平台映射", businessType=BusinessType.UPDATE) public AjaxResult edit(@RequestBody MarkPlatformMapping item) { return toAjax(service.update(item)); }
    @DeleteMapping("/{id}") @PreAuthorize("@ss.hasPermi('server:markPlatform:remove')") @Log(title="标记平台映射", businessType=BusinessType.DELETE) public AjaxResult remove(@PathVariable Long id) { return toAjax(service.delete(id)); }
    @PutMapping("/source-status") @PreAuthorize("@ss.hasPermi('server:markPlatform:status')") public AjaxResult sourceStatus(@RequestBody MarkPlatformMapping item) { return toAjax(service.updateSourceEnabled(item.getId(), item.getSourceEnabled())); }
    @PutMapping("/target-status") @PreAuthorize("@ss.hasPermi('server:markPlatform:status')") public AjaxResult targetStatus(@RequestBody MarkPlatformMapping item) { return toAjax(service.updateTargetEnabled(item.getId(), item.getTargetEnabled())); }
    @PutMapping("/mapping-status") @PreAuthorize("@ss.hasPermi('server:markPlatform:status')") public AjaxResult mappingStatus(@RequestBody MarkPlatformMapping item) { return toAjax(service.updateMappingEnabled(item.getId(), item.getMappingEnabled())); }
}
