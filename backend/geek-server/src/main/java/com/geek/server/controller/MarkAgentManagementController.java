package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.dto.*;
import com.geek.server.service.IMarkAgentManagementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/server/markAgentManagement")
public class MarkAgentManagementController extends BaseController {
    @Autowired private IMarkAgentManagementService service;
    @GetMapping("/list") @PreAuthorize("@ss.hasPermi('server:markAgentManagement:list')")
    public AjaxResult list(@RequestParam Integer agentLevel,@RequestParam(required=false) String keyword,@RequestParam(required=false) String status){return success(service.selectList(agentLevel,keyword,status));}
    @GetMapping("/capabilities") @PreAuthorize("@ss.hasPermi('server:markAgentManagement:list')") public AjaxResult capabilities(){return success(service.capabilities());}
    @PostMapping @PreAuthorize("@ss.hasPermi('server:markAgentManagement:add')") @Log(title="代理账号管理",businessType=BusinessType.INSERT)
    public AjaxResult add(@Valid @RequestBody MarkAgentCreateRequest request){return AjaxResult.success("创建成功",service.createAgent(request));}
    @PutMapping("/status") @PreAuthorize("@ss.hasPermi('server:markAgentManagement:edit')") @Log(title="下级账号状态",businessType=BusinessType.UPDATE)
    public AjaxResult status(@Valid @RequestBody MarkAgentAccountStatusRequest request){return toAjax(service.updateStatus(request));}
    @PutMapping("/resetPassword") @PreAuthorize("@ss.hasPermi('server:markAgentManagement:resetPwd')") @Log(title="代理初始化密码",businessType=BusinessType.UPDATE)
    public AjaxResult reset(@Valid @RequestBody MarkAgentResetPasswordRequest request){return toAjax(service.resetPassword(request));}
    @GetMapping("/platform/options/{targetUserId}") @PreAuthorize("@ss.hasPermi('server:markAgentManagement:allocate')") public AjaxResult options(@PathVariable Long targetUserId){return success(service.platformOptions(targetUserId));}
    @PostMapping("/admin/recharge") @PreAuthorize("@ss.hasPermi('server:markAgentManagement:allocate')") @Log(title="管理员平台次数充值",businessType=BusinessType.UPDATE)
    public AjaxResult recharge(@Valid @RequestBody MarkAgentPointOperationRequest request){return AjaxResult.success("次数充值成功",service.adminRecharge(request));}
    @PostMapping("/agent/transfer") @PreAuthorize("@ss.hasPermi('server:markAgentManagement:allocate')") @Log(title="代理平台次数充值",businessType=BusinessType.UPDATE)
    public AjaxResult transfer(@Valid @RequestBody MarkAgentPointOperationRequest request){return AjaxResult.success("次数充值成功",service.agentTransfer(request));}
}
