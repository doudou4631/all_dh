package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.FreeQueryPageConfig;
import com.geek.server.service.IFreeQueryPageConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/server/freeQuerySettings/page")
public class FreeQueryPageConfigController extends BaseController {
    @Autowired
    private IFreeQueryPageConfigService pageConfigService;

    @PreAuthorize("@ss.hasPermi('freeQuery:page:list')")
    @GetMapping("/configs")
    public AjaxResult configs() {
        return success(pageConfigService.listConfigs());
    }

    @PreAuthorize("@ss.hasPermi('freeQuery:page:list')")
    @GetMapping("/config")
    public AjaxResult config(@RequestParam(defaultValue = "1") String pageCode) {
        try {
            return success(pageConfigService.getConfig(pageCode));
        } catch (IllegalArgumentException ex) {
            return error(ex.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('freeQuery:page:edit')")
    @Log(title = "免费查询页面配置", businessType = BusinessType.UPDATE)
    @PutMapping("/config")
    public AjaxResult update(@RequestBody FreeQueryPageConfig config) {
        try {
            return toAjax(pageConfigService.saveConfig(config));
        } catch (IllegalArgumentException ex) {
            return error(ex.getMessage());
        }
    }
}
