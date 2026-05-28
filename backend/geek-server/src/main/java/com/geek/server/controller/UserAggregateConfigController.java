package com.geek.server.controller;

import java.util.Date;
import java.util.List;
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
import com.geek.server.domain.UserAggregateConfig;
import com.geek.server.service.IUserAggregateConfigService;
import com.geek.common.utils.poi.ExcelUtil;
import com.geek.common.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

/**
 * 聚合配置Controller
 * 
 * @author geek
 * @date 2026-03-09
 */
@RestController
@RequestMapping("/server/aggConfig")
@Tag(name = "【聚合配置】管理")
public class UserAggregateConfigController extends BaseController
{
    @Autowired
    private IUserAggregateConfigService userAggregateConfigService;

    /**
     * 查询聚合配置列表
     */
    @Operation(summary = "查询聚合配置列表")
    @PreAuthorize("@ss.hasPermi('server:aggConfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(UserAggregateConfig userAggregateConfig)
    {
        startPage();
        List<UserAggregateConfig> list = userAggregateConfigService.selectUserAggregateConfigList(userAggregateConfig);
        return getDataTable(list);
    }

    /**
     * 导出聚合配置列表
     */
    @Operation(summary = "导出聚合配置列表")
    @PreAuthorize("@ss.hasPermi('server:aggConfig:export')")
    @Log(title = "聚合配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, UserAggregateConfig userAggregateConfig)
    {
        List<UserAggregateConfig> list = userAggregateConfigService.selectUserAggregateConfigList(userAggregateConfig);
        ExcelUtil<UserAggregateConfig> util = new ExcelUtil<>(UserAggregateConfig.class);
        util.exportExcel(response, list, "聚合配置数据");
    }

    /**
     * 获取聚合配置详细信息
     */
    @Operation(summary = "获取聚合配置详细信息")
    @PreAuthorize("@ss.hasPermi('server:aggConfig:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(userAggregateConfigService.selectUserAggregateConfigById(id));
    }

    /**
     * 新增聚合配置
     */
    @Operation(summary = "新增聚合配置")
    @PreAuthorize("@ss.hasPermi('server:aggConfig:add')")
    @Log(title = "聚合配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody UserAggregateConfig userAggregateConfig)
    {
        userAggregateConfig.setCreateBy(getUsername());
        userAggregateConfig.setCreateTime(new Date());
        return toAjax(userAggregateConfigService.insertUserAggregateConfig(userAggregateConfig));
    }

    /**
     * 修改聚合配置
     */
    @Operation(summary = "修改聚合配置")
    @PreAuthorize("@ss.hasPermi('server:aggConfig:edit')")
    @Log(title = "聚合配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UserAggregateConfig userAggregateConfig)
    {
        userAggregateConfig.setUpdateBy(getUsername());
        userAggregateConfig.setUpdateTime(new Date());
        return toAjax(userAggregateConfigService.updateUserAggregateConfig(userAggregateConfig));
    }

    /**
     * 删除聚合配置
     */
    @Operation(summary = "删除聚合配置")
    @PreAuthorize("@ss.hasPermi('server:aggConfig:remove')")
    @Log(title = "聚合配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable( name = "ids" ) Long[] ids) 
    {
        return toAjax(userAggregateConfigService.deleteUserAggregateConfigByIds(ids));
    }
}
