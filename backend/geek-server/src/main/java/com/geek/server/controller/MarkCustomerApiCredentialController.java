package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.MarkPlatformMapping;
import com.geek.server.domain.dto.MarkCustomerApiCredentialCreateRequest;
import com.geek.server.domain.dto.MarkCustomerApiCredentialUpdateRequest;
import com.geek.server.domain.dto.MarkOpenApiCredentialStatusRequest;
import com.geek.server.service.IMarkPlatformMappingService;
import com.geek.server.service.MarkOpenApiCredentialService;
import jakarta.validation.Valid;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Administration endpoints for API keys through which customers submit mark orders. */
@RestController
@RequestMapping("/server/markCustomerApi")
@RequiredArgsConstructor
public class MarkCustomerApiCredentialController extends BaseController {
    private final MarkOpenApiCredentialService credentialService;
    private final IMarkPlatformMappingService platformMappingService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('server:markCustomerApi:list')")
    public AjaxResult list(String name, String status) {
        return success(credentialService.listCustomer(name, status));
    }

    @GetMapping("/agent/options")
    @PreAuthorize("@ss.hasPermi('server:markCustomerApi:list')")
    public AjaxResult agentOptions() {
        return success(credentialService.listActiveAgents());
    }

    @GetMapping("/platform/options")
    @PreAuthorize("@ss.hasPermi('server:markCustomerApi:list')")
    public AjaxResult platformOptions() {
        Map<String, Map<String, String>> options = new LinkedHashMap<>();
        for (MarkPlatformMapping mapping : platformMappingService.selectList(new MarkPlatformMapping())) {
            if (!"0".equals(mapping.getSourceEnabled()) || !"0".equals(mapping.getTargetEnabled())
                    || !"0".equals(mapping.getMappingEnabled()) || mapping.getSourcePlatformCode() == null) {
                continue;
            }
            String code = mapping.getSourcePlatformCode().trim().toLowerCase();
            if (code.isEmpty()) {
                continue;
            }
            Map<String, String> option = new LinkedHashMap<>();
            option.put("platformCode", code);
            option.put("platformName", mapping.getSourcePlatformName() == null ? code : mapping.getSourcePlatformName());
            options.putIfAbsent(code, option);
        }
        return success(new ArrayList<>(options.values()));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('server:markCustomerApi:add')")
    @Log(title = "客户API", businessType = BusinessType.INSERT)
    public AjaxResult create(@Valid @RequestBody MarkCustomerApiCredentialCreateRequest request) {
        return AjaxResult.success("创建成功，请立即保存 Secret", credentialService.createCustomer(request));
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('server:markCustomerApi:edit')")
    @Log(title = "客户API", businessType = BusinessType.UPDATE)
    public AjaxResult update(@Valid @RequestBody MarkCustomerApiCredentialUpdateRequest request) {
        return toAjax(credentialService.updateCustomer(request));
    }

    @PutMapping("/status")
    @PreAuthorize("@ss.hasPermi('server:markCustomerApi:edit')")
    @Log(title = "客户API", businessType = BusinessType.UPDATE)
    public AjaxResult status(@Valid @RequestBody MarkOpenApiCredentialStatusRequest request) {
        return toAjax(credentialService.updateCustomerStatus(request.getId(), request.getStatus()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('server:markCustomerApi:remove')")
    @Log(title = "客户API", businessType = BusinessType.DELETE)
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(credentialService.deleteCustomer(id));
    }

    @PostMapping("/{id}/rotate")
    @PreAuthorize("@ss.hasPermi('server:markCustomerApi:rotate')")
    @Log(title = "客户API Secret轮换", businessType = BusinessType.UPDATE)
    public AjaxResult rotate(@PathVariable Long id) {
        return AjaxResult.success("Secret 已轮换，请立即保存", credentialService.rotateCustomer(id));
    }
}
