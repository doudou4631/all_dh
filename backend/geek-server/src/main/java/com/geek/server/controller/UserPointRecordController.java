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
import com.geek.server.domain.UserPointRecord;
import com.geek.server.service.IUserPointRecordService;
import com.geek.common.utils.poi.ExcelUtil;
import com.geek.common.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

/**
 * 积分流水记录Controller
 * 
 * @author geek
 * @date 2026-03-09
 */
@RestController
@RequestMapping("/server/pointRecord")
@Tag(name = "【积分流水记录】管理")
public class UserPointRecordController extends BaseController
{
    @Autowired
    private IUserPointRecordService userPointRecordService;

    /**
     * 查询积分流水记录列表
     */
    @Operation(summary = "查询积分流水记录列表")
//    @PreAuthorize("@ss.hasPermi('server:pointRecord:list')")
    @GetMapping("/list")
    public TableDataInfo list(UserPointRecord userPointRecord)
    {
        startPage();
        List<UserPointRecord> list = userPointRecordService.selectUserPointRecordList(userPointRecord);
        return getDataTable(list);
    }

    /**
     * 导出积分流水记录列表
     */
    @Operation(summary = "导出积分流水记录列表")
    @PreAuthorize("@ss.hasPermi('server:pointRecord:export')")
    @Log(title = "积分流水记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, UserPointRecord userPointRecord)
    {
        List<UserPointRecord> list = userPointRecordService.selectUserPointRecordList(userPointRecord);
        ExcelUtil<UserPointRecord> util = new ExcelUtil<>(UserPointRecord.class);
        util.exportExcel(response, list, "积分流水记录数据");
    }

    /**
     * 获取积分流水记录详细信息
     */
    @Operation(summary = "获取积分流水记录详细信息")
    @PreAuthorize("@ss.hasPermi('server:pointRecord:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(userPointRecordService.selectUserPointRecordById(id));
    }

    /**
     * 新增积分流水记录
     */
    @Operation(summary = "新增积分流水记录")
//    @PreAuthorize("@ss.hasPermi('server:pointRecord:add')")
    @Log(title = "积分流水记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody UserPointRecord userPointRecord)
    {
        userPointRecord.setOperatorId(getUserId());
        userPointRecord.setCreateBy(this.getUsername());
        return toAjax(userPointRecordService.insertUserPointRecord(userPointRecord));
    }

    /**
     * 调整用户积分并记录流水
     */
    @Operation(summary = "调整用户积分并记录流水")
    @PreAuthorize("@ss.hasPermi('server:pointRecord:add')")
    @Log(title = "积分流水记录", businessType = BusinessType.UPDATE)
    @PostMapping("/adjust")
    public AjaxResult adjust(@RequestBody UserPointRecord userPointRecord)
    {
        userPointRecord.setOperatorId(getUserId());
        userPointRecord.setCreateBy(this.getUsername());
        return toAjax(userPointRecordService.adjustUserPoints(userPointRecord));
    }

    /**
     * 修改积分流水记录
     */
    @Operation(summary = "修改积分流水记录")
//    @PreAuthorize("@ss.hasPermi('server:pointRecord:edit')")
    @Log(title = "积分流水记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UserPointRecord userPointRecord)
    {
        return toAjax(userPointRecordService.updateUserPointRecord(userPointRecord));
    }

    /**
     * 删除积分流水记录
     */
    @Operation(summary = "删除积分流水记录")
    @PreAuthorize("@ss.hasPermi('server:pointRecord:remove')")
    @Log(title = "积分流水记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable( name = "ids" ) Long[] ids) 
    {
        return toAjax(userPointRecordService.deleteUserPointRecordByIds(ids));
    }
}
