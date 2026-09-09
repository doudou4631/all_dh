package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.FreeQueryScheduleConfig;
import com.geek.server.service.IFreeQueryScheduleConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
@RequestMapping("/server/freeQuerySettings/schedule")
@Tag(name = "【免费查询定时任务】管理")
public class FreeQueryScheduleConfigController extends BaseController {
    @Autowired
    private IFreeQueryScheduleConfigService service;

    @GetMapping("/list")
    @Operation(summary = "查询定时任务配置列表")
    @PreAuthorize("@ss.hasPermi('freeQuery:schedule:list')")
    public TableDataInfo list(FreeQueryScheduleConfig config) {
        startPage();
        return getDataTable(service.selectList(config));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取定时任务配置")
    @PreAuthorize("@ss.hasPermi('freeQuery:schedule:query')")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(service.selectById(id));
    }

    @GetMapping("/effective")
    @Operation(summary = "获取已启用的调度配置")
    @PreAuthorize("@ss.hasPermi('freeQuery:schedule:list')")
    public AjaxResult effective() {
        return success(service.selectEnabledList());
    }

    @PostMapping
    @Operation(summary = "新增定时任务配置")
    @PreAuthorize("@ss.hasPermi('freeQuery:schedule:add')")
    @Log(title = "免费查询定时任务", businessType = BusinessType.INSERT)
    public AjaxResult add(@Valid @RequestBody FreeQueryScheduleConfig config) {
        String error = service.validate(config);
        if (error != null) {
            return error(error);
        }
        config.setCreateBy(getUsername());
        return toAjax(service.insert(config));
    }

    @PutMapping
    @Operation(summary = "修改定时任务配置")
    @PreAuthorize("@ss.hasPermi('freeQuery:schedule:edit')")
    @Log(title = "免费查询定时任务", businessType = BusinessType.UPDATE)
    public AjaxResult edit(@Valid @RequestBody FreeQueryScheduleConfig config) {
        String error = service.validate(config);
        if (error != null) {
            return error(error);
        }
        config.setUpdateBy(getUsername());
        return toAjax(service.update(config));
    }

    @PutMapping("/changeStatus")
    @Operation(summary = "启用或停用定时任务配置")
    @PreAuthorize("@ss.hasPermi('freeQuery:schedule:changeStatus')")
    @Log(title = "免费查询定时任务状态", businessType = BusinessType.UPDATE)
    public AjaxResult changeStatus(@RequestBody FreeQueryScheduleConfig config) {
        if (config.getId() == null || !List.of("0", "1").contains(config.getStatus())) {
            return error("无效的任务状态");
        }
        config.setUpdateBy(getUsername());
        return toAjax(service.changeStatus(config));
    }

    @DeleteMapping("/{ids}")
    @Operation(summary = "删除定时任务配置")
    @PreAuthorize("@ss.hasPermi('freeQuery:schedule:remove')")
    @Log(title = "免费查询定时任务", businessType = BusinessType.DELETE)
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(service.deleteByIds(ids));
    }
}
