package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.FreeQueryPlatform;
import com.geek.server.service.IFreeQueryPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/server/freeQuerySettings/platform")
public class FreeQueryPlatformController extends BaseController {
    @Autowired
    private IFreeQueryPlatformService platformService;

    @PreAuthorize("@ss.hasPermi('freeQuery:platform:list')")
    @GetMapping("/list")
    public AjaxResult list() {
        return success(platformService.selectList());
    }

    @PreAuthorize("@ss.hasPermi('freeQuery:platform:edit')")
    @Log(title = "免费查询平台展示配置", businessType = BusinessType.UPDATE)
    @PutMapping("/display")
    public AjaxResult updateDisplay(@RequestBody FreeQueryPlatform platform) {
        try {
            return toAjax(platformService.updateDisplay(platform));
        } catch (IllegalArgumentException ex) {
            return error(ex.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('freeQuery:platform:status')")
    @Log(title = "免费查询平台状态", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult changeStatus(@RequestBody FreeQueryPlatform platform) {
        try {
            return toAjax(platformService.changeStatus(platform.getId(), platform.getStatus()));
        } catch (IllegalArgumentException ex) {
            return error(ex.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('freeQuery:platform:sort')")
    @Log(title = "免费查询平台优先级", businessType = BusinessType.UPDATE)
    @PutMapping("/sort")
    public AjaxResult updateSort(@RequestBody Map<String, List<Long>> request) {
        try {
            return toAjax(platformService.updateSort(request == null ? null : request.get("ids")));
        } catch (IllegalArgumentException ex) {
            return error(ex.getMessage());
        }
    }
}
