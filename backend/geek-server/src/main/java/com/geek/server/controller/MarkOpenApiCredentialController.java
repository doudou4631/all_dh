package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.dto.MarkOpenApiCredentialCreateRequest;
import com.geek.server.domain.dto.MarkOpenApiCredentialStatusRequest;
import com.geek.server.service.MarkOpenApiCredentialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/server/markOpenApi")
@RequiredArgsConstructor
public class MarkOpenApiCredentialController extends BaseController {
    private final MarkOpenApiCredentialService service;
    @GetMapping("/list") @PreAuthorize("@ss.hasPermi('server:markOpenApi:list')")
    public AjaxResult list(String name, String status) { return success(service.list(name, status)); }
    @PostMapping @PreAuthorize("@ss.hasPermi('server:markOpenApi:add')") @Log(title="自动化API", businessType=BusinessType.INSERT)
    public AjaxResult create(@Valid @RequestBody MarkOpenApiCredentialCreateRequest request) { return AjaxResult.success("创建成功，请立即保存 Secret", service.create(request)); }
    @PutMapping("/status") @PreAuthorize("@ss.hasPermi('server:markOpenApi:edit')") @Log(title="自动化API", businessType=BusinessType.UPDATE)
    public AjaxResult status(@Valid @RequestBody MarkOpenApiCredentialStatusRequest request) { return toAjax(service.updateStatus(request.getId(), request.getStatus())); }
    @PostMapping("/{id}/rotate") @PreAuthorize("@ss.hasPermi('server:markOpenApi:rotate')") @Log(title="自动化API Secret轮换", businessType=BusinessType.UPDATE)
    public AjaxResult rotate(@PathVariable Long id) { return AjaxResult.success("Secret 已轮换，请立即保存", service.rotate(id)); }
}
