package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.FreeQueryUser;
import com.geek.server.domain.vo.FreeQueryPointAdjustRequest;
import com.geek.server.domain.vo.FreeQueryUserResetPwdRequest;
import com.geek.server.service.IFreeQueryUserService;
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

@Tag(name = "【手机端用户】管理")
@RestController
@RequestMapping("/server/freeQueryUser")
@RequiredArgsConstructor
public class FreeQueryUserController extends BaseController {

    private final IFreeQueryUserService freeQueryUserService;

    @Operation(summary = "手机端用户列表")
    @PreAuthorize("@ss.hasPermi('server:freeQueryUser:list')")
    @GetMapping("/list")
    public TableDataInfo list(FreeQueryUser query) {
        startPage();
        List<FreeQueryUser> list = freeQueryUserService.selectFreeQueryUserList(query);
        list.forEach(row -> row.setPassword(null));
        return getDataTable(list);
    }

    @Operation(summary = "手机端用户详情")
    @PreAuthorize("@ss.hasPermi('server:freeQueryUser:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        FreeQueryUser user = freeQueryUserService.selectFreeQueryUserById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return success(user);
    }

    @Operation(summary = "新增手机端用户")
    @PreAuthorize("@ss.hasPermi('server:freeQueryUser:add')")
    @Log(title = "手机端用户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody FreeQueryUser user) {
        user.setCreateBy(getUsername());
        return toAjax(freeQueryUserService.insertFreeQueryUser(user));
    }

    @Operation(summary = "修改手机端用户")
    @PreAuthorize("@ss.hasPermi('server:freeQueryUser:edit')")
    @Log(title = "手机端用户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody FreeQueryUser user) {
        user.setUpdateBy(getUsername());
        return toAjax(freeQueryUserService.updateFreeQueryUser(user));
    }

    @Operation(summary = "删除手机端用户")
    @PreAuthorize("@ss.hasPermi('server:freeQueryUser:remove')")
    @Log(title = "手机端用户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(freeQueryUserService.deleteFreeQueryUserByIds(ids, getUsername()));
    }

    @Operation(summary = "调整手机端用户积分")
    @PreAuthorize("@ss.hasPermi('server:freeQueryUser:adjust')")
    @Log(title = "手机端用户", businessType = BusinessType.UPDATE)
    @PostMapping("/adjustPoints")
    public AjaxResult adjustPoints(@RequestBody FreeQueryPointAdjustRequest request) {
        return toAjax(freeQueryUserService.adjustPoints(request, getUserId(), getUsername()));
    }

    @Operation(summary = "重置手机端用户密码")
    @PreAuthorize("@ss.hasPermi('server:freeQueryUser:resetPwd')")
    @Log(title = "手机端用户", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody FreeQueryUserResetPwdRequest request) {
        if (request == null) {
            return AjaxResult.error("参数不能为空");
        }
        return toAjax(freeQueryUserService.resetPassword(request.getUserId(), request.getPassword(), getUsername()));
    }
}
