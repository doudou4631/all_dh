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
import com.geek.server.domain.UserPoint;
import com.geek.server.service.IUserPointService;
import com.geek.common.utils.poi.ExcelUtil;
import com.geek.common.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

/**
 * 用户积分关联Controller
 * 
 * @author geek
 * @date 2026-03-09
 */
@RestController
@RequestMapping("/server/point")
@Tag(name = "【用户积分关联】管理")
public class UserPointController extends BaseController
{
    @Autowired
    private IUserPointService userPointService;

    /**
     * 查询用户积分关联列表
     */
    @Operation(summary = "查询用户积分关联列表")
    @PreAuthorize("@ss.hasPermi('server:point:list')")
    @GetMapping("/list")
    public TableDataInfo list(UserPoint userPoint)
    {
        startPage();
        List<UserPoint> list = userPointService.selectUserPointList(userPoint);
        return getDataTable(list);
    }

    /**
     * 导出用户积分关联列表
     */
    @Operation(summary = "导出用户积分关联列表")
    @PreAuthorize("@ss.hasPermi('server:point:export')")
    @Log(title = "用户积分关联", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, UserPoint userPoint)
    {
        List<UserPoint> list = userPointService.selectUserPointList(userPoint);
        ExcelUtil<UserPoint> util = new ExcelUtil<>(UserPoint.class);
        util.exportExcel(response, list, "用户积分关联数据");
    }

    /**
     * 获取用户积分关联详细信息
     */
    @Operation(summary = "获取用户积分关联详细信息")
    @PreAuthorize("@ss.hasPermi('server:point:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(userPointService.selectUserPointById(id));
    }

    /**
     * 新增用户积分关联
     */
    @Operation(summary = "新增用户积分关联")
    @PreAuthorize("@ss.hasPermi('server:point:add')")
    @Log(title = "用户积分关联", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody UserPoint userPoint)
    {
        return toAjax(userPointService.insertUserPoint(userPoint));
    }

    /**
     * 修改用户积分关联
     */
    @Operation(summary = "修改用户积分关联")
    @PreAuthorize("@ss.hasPermi('server:point:edit')")
    @Log(title = "用户积分关联", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UserPoint userPoint)
    {
        return toAjax(userPointService.updateUserPoint(userPoint));
    }

    /**
     * 删除用户积分关联
     */
    @Operation(summary = "删除用户积分关联")
    @PreAuthorize("@ss.hasPermi('server:point:remove')")
    @Log(title = "用户积分关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable( name = "ids" ) Long[] ids) 
    {
        return toAjax(userPointService.deleteUserPointByIds(ids));
    }
}
