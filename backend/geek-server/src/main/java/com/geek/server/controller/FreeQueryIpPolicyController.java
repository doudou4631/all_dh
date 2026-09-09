package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.FreeQueryIpPolicy;
import com.geek.server.service.IFreeQueryIpPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/server/freeQuerySettings/ipPolicy")
public class FreeQueryIpPolicyController extends BaseController {
    @Autowired
    private IFreeQueryIpPolicyService policyService;

    @PreAuthorize("@ss.hasPermi('freeQuery:ipPolicy:list')")
    @GetMapping("/config")
    public AjaxResult config() {
        return success(policyService.getConfig());
    }

    @PreAuthorize("@ss.hasPermi('freeQuery:ipPolicy:edit')")
    @Log(title = "免费查询IP访问策略", businessType = BusinessType.UPDATE)
    @PutMapping("/config")
    public AjaxResult update(@RequestBody FreeQueryIpPolicy config) {
        try {
            return toAjax(policyService.updateConfig(config));
        } catch (IllegalArgumentException ex) {
            return error(ex.getMessage());
        }
    }
}
