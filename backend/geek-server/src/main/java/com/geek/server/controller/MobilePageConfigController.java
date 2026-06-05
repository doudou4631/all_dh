package com.geek.server.controller;

import com.geek.common.annotation.Anonymous;
import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.MobilePageConfig;
import com.geek.server.service.IMobilePageConfigService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "【手机页配置】管理")
@RestController
@RequestMapping("/server/mobilePageConfig")
@RequiredArgsConstructor
public class MobilePageConfigController extends BaseController {

    private final IMobilePageConfigService mobilePageConfigService;

    @Operation(summary = "查询手机页配置列表")
    @PreAuthorize("@ss.hasPermi('server:mobilePageConfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(MobilePageConfig query) {
        startPage();
        List<MobilePageConfig> list = mobilePageConfigService.selectMobilePageConfigList(query);
        return getDataTable(list);
    }

    @Operation(summary = "获取手机页配置详情")
    @PreAuthorize("@ss.hasPermi('server:mobilePageConfig:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(mobilePageConfigService.selectMobilePageConfigById(id));
    }

    @Operation(summary = "新增手机页配置")
    @PreAuthorize("@ss.hasPermi('server:mobilePageConfig:add')")
    @Log(title = "手机页配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MobilePageConfig mobilePageConfig) {
        mobilePageConfig.setCreateBy(getUsername());
        return toAjax(mobilePageConfigService.insertMobilePageConfig(mobilePageConfig));
    }

    @Operation(summary = "修改手机页配置")
    @PreAuthorize("@ss.hasPermi('server:mobilePageConfig:edit')")
    @Log(title = "手机页配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MobilePageConfig mobilePageConfig) {
        mobilePageConfig.setUpdateBy(getUsername());
        return toAjax(mobilePageConfigService.updateMobilePageConfig(mobilePageConfig));
    }

    @Operation(summary = "删除手机页配置")
    @PreAuthorize("@ss.hasPermi('server:mobilePageConfig:remove')")
    @Log(title = "手机页配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(mobilePageConfigService.deleteMobilePageConfigByIds(ids));
    }

    @Operation(summary = "获取手机页公开配置")
    @Anonymous
    @GetMapping("/public/current")
    public AjaxResult publicCurrent(@RequestParam(value = "page", required = false) String page) {
        return AjaxResult.success(mobilePageConfigService.selectPublicConfig(page));
    }
}
