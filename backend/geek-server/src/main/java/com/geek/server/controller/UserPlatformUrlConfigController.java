package com.geek.server.controller;

import java.util.List;

import com.geek.common.annotation.Anonymous;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.UserPlatformUrlConfig;
import com.geek.server.service.IUserPlatformUrlConfigService;
import com.geek.common.utils.poi.ExcelUtil;
import com.geek.common.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

/**
 * 查询平台url配置Controller
 * 
 * @author geek
 * @date 2026-03-09
 */
@RestController
@RequestMapping("/server/platformConfig")
@Tag(name = "【查询平台url配置】管理")
public class UserPlatformUrlConfigController extends BaseController
{
    @Autowired
    private IUserPlatformUrlConfigService userPlatformUrlConfigService;

    /**
     * 查询查询平台url配置列表
     */
    @Operation(summary = "查询平台url配置列表")
    @Anonymous
    @GetMapping("/list")
    public TableDataInfo list(UserPlatformUrlConfig userPlatformUrlConfig)
    {
        startPage();
        List<UserPlatformUrlConfig> list = userPlatformUrlConfigService.selectUserPlatformUrlConfigList(userPlatformUrlConfig);
        return getDataTable(list);
    }

    /**
     * 查询用户可用平台url列表
     */
    @Operation(summary = "查询平台url配置列表")
//    @PreAuthorize("@ss.hasPermi('server:platformConfig:list')")
    @GetMapping("/userList")
    public TableDataInfo userList(UserPlatformUrlConfig userPlatformUrlConfig)
    {
        startPage();
        List<UserPlatformUrlConfig> list = userPlatformUrlConfigService.selectUserPlatformUrlUserList(userPlatformUrlConfig);
        return getDataTable(list);
    }

    /**
     * 导出查询平台url配置列表
     */
    @Operation(summary = "导出查询平台url配置列表")
    @PreAuthorize("@ss.hasPermi('server:platformConfig:export')")
    @Log(title = "查询平台url配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, UserPlatformUrlConfig userPlatformUrlConfig)
    {
        List<UserPlatformUrlConfig> list = userPlatformUrlConfigService.selectUserPlatformUrlConfigList(userPlatformUrlConfig);
        ExcelUtil<UserPlatformUrlConfig> util = new ExcelUtil<>(UserPlatformUrlConfig.class);
        util.exportExcel(response, list, "查询平台url配置数据");
    }

    /**
     * 获取查询平台url配置详细信息
     */
    @Operation(summary = "获取查询平台url配置详细信息")
    @PreAuthorize("@ss.hasPermi('server:platformConfig:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(userPlatformUrlConfigService.selectUserPlatformUrlConfigById(id));
    }

    /**
     * 新增查询平台url配置
     */
    @Operation(summary = "新增查询平台url配置")
    @PreAuthorize("@ss.hasPermi('server:platformConfig:add')")
    @Log(title = "查询平台url配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody UserPlatformUrlConfig userPlatformUrlConfig)
    {
        return toAjax(userPlatformUrlConfigService.insertUserPlatformUrlConfig(userPlatformUrlConfig));
    }

    /**
     * 修改查询平台url配置
     */
    @Operation(summary = "修改查询平台url配置")
    @PreAuthorize("@ss.hasPermi('server:platformConfig:edit')")
    @Log(title = "查询平台url配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UserPlatformUrlConfig userPlatformUrlConfig)
    {
        return toAjax(userPlatformUrlConfigService.updateUserPlatformUrlConfig(userPlatformUrlConfig));
    }

    /**
     * 删除查询平台url配置
     */
    @Operation(summary = "删除查询平台url配置")
    @PreAuthorize("@ss.hasPermi('server:platformConfig:remove')")
    @Log(title = "查询平台url配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable( name = "ids" ) Long[] ids) 
    {
        return toAjax(userPlatformUrlConfigService.deleteUserPlatformUrlConfigByIds(ids));
    }
}
