package com.geek.server.controller;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.enums.BusinessType;
import com.geek.common.exception.ServiceException;
import com.geek.common.utils.SecurityUtils;
import com.geek.server.domain.MarkPlatformTemplate;
import com.geek.server.service.IMarkPlatformTemplateService;
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

/**
 * 标记平台模板控制器
 */
@Tag(name = "【标记平台模板】管理")
@RestController
@RequestMapping("/server/markTemplate")
@RequiredArgsConstructor
public class MarkPlatformTemplateController extends BaseController {

    private final IMarkPlatformTemplateService markPlatformTemplateService;

    @Operation(summary = "标记模板列表")
    @PreAuthorize("@ss.hasPermi('server:markTemplate:list')")
    @GetMapping("/list")
    public TableDataInfo list(MarkPlatformTemplate query) {
        startPage();
        List<MarkPlatformTemplate> list = markPlatformTemplateService.selectMarkPlatformTemplateList(query);
        return getDataTable(list);
    }

    @Operation(summary = "标记模板详情")
    @PreAuthorize("@ss.hasPermi('server:markTemplate:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        return success(markPlatformTemplateService.selectMarkPlatformTemplateById(id));
    }

    @Operation(summary = "启用标记模板选项")
    @GetMapping("/options")
    public AjaxResult options() {
        return success(markPlatformTemplateService.selectEnabledTemplateOptionsForCurrentUser());
    }

    @Operation(summary = "标记平台候选")
    @PreAuthorize("@ss.hasPermi('server:markTemplate:list')")
    @GetMapping("/platformOptions")
    public AjaxResult platformOptions() {
        return success(markPlatformTemplateService.selectPlatformOptions());
    }

    @Operation(summary = "新增标记模板")
    @PreAuthorize("@ss.hasPermi('server:markTemplate:add')")
    @Log(title = "标记平台模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MarkPlatformTemplate markPlatformTemplate) {
        ensureTemplateManagePermission();
        return toAjax(markPlatformTemplateService.insertMarkPlatformTemplate(markPlatformTemplate));
    }

    @Operation(summary = "修改标记模板")
    @PreAuthorize("@ss.hasPermi('server:markTemplate:edit')")
    @Log(title = "标记平台模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MarkPlatformTemplate markPlatformTemplate) {
        ensureTemplateManagePermission();
        return toAjax(markPlatformTemplateService.updateMarkPlatformTemplate(markPlatformTemplate));
    }

    @Operation(summary = "删除标记模板")
    @PreAuthorize("@ss.hasPermi('server:markTemplate:remove')")
    @Log(title = "标记平台模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        ensureTemplateManagePermission();
        return toAjax(markPlatformTemplateService.deleteMarkPlatformTemplateByIds(ids));
    }

    private void ensureTemplateManagePermission() {
        if (SecurityUtils.isAdmin()
                || SecurityUtils.hasRole("admin")
                || SecurityUtils.hasRole("mark_admin")
                || SecurityUtils.hasRole("agent")
                || SecurityUtils.hasRole("mark_agent")) {
            return;
        }
        throw new ServiceException("无权管理标记模板");
    }
}
