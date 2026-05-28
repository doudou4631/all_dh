package com.geek.server.controller;

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
import com.geek.server.domain.UserQueryTemplate;
import com.geek.server.service.IUserQueryTemplateService;
import com.geek.common.utils.poi.ExcelUtil;
import com.geek.common.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

/**
 * 查询模板定义Controller
 * 
 * @author geek
 * @date 2026-03-09
 */
@RestController
@RequestMapping("/server/template")
@Tag(name = "【查询模板定义】管理")
public class UserQueryTemplateController extends BaseController
{
    @Autowired
    private IUserQueryTemplateService userQueryTemplateService;

    /**
     * 查询查询模板定义列表
     */
    @Operation(summary = "查询查询模板定义列表")
//    @PreAuthorize("@ss.hasPermi('server:template:list')")
    @GetMapping("/list")
    public TableDataInfo list(UserQueryTemplate userQueryTemplate)
    {
        startPage();
        List<UserQueryTemplate> list = userQueryTemplateService.selectUserQueryTemplateList(userQueryTemplate);
        return getDataTable(list);
    }

    /**
     * 导出查询模板定义列表
     */
    @Operation(summary = "导出查询模板定义列表")
    @PreAuthorize("@ss.hasPermi('server:template:export')")
    @Log(title = "查询模板定义", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, UserQueryTemplate userQueryTemplate)
    {
        List<UserQueryTemplate> list = userQueryTemplateService.selectUserQueryTemplateList(userQueryTemplate);
        ExcelUtil<UserQueryTemplate> util = new ExcelUtil<>(UserQueryTemplate.class);
        util.exportExcel(response, list, "查询模板定义数据");
    }

    /**
     * 获取查询模板定义详细信息
     */
    @Operation(summary = "获取查询模板定义详细信息")
//    @PreAuthorize("@ss.hasPermi('server:template:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(userQueryTemplateService.selectUserQueryTemplateById(id));
    }

    /**
     * 新增查询模板定义
     */
    @Operation(summary = "新增查询模板定义")
    @PreAuthorize("@ss.hasPermi('server:template:add')")
    @Log(title = "查询模板定义", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody UserQueryTemplate userQueryTemplate)
    {
        return toAjax(userQueryTemplateService.insertUserQueryTemplate(userQueryTemplate));
    }

    /**
     * 修改查询模板定义
     */
    @Operation(summary = "修改查询模板定义")
    @PreAuthorize("@ss.hasPermi('server:template:edit')")
    @Log(title = "查询模板定义", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UserQueryTemplate userQueryTemplate)
    {
        return toAjax(userQueryTemplateService.updateUserQueryTemplate(userQueryTemplate));
    }

    /**
     * 删除查询模板定义
     */
    @Operation(summary = "删除查询模板定义")
    @PreAuthorize("@ss.hasPermi('server:template:remove')")
    @Log(title = "查询模板定义", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable( name = "ids" ) Long[] ids) 
    {
        return toAjax(userQueryTemplateService.deleteUserQueryTemplateByIds(ids));
    }
}
