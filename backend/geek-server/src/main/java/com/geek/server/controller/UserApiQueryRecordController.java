package com.geek.server.controller;

import java.util.List;

import org.springframework.util.StringUtils;
import com.geek.common.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.UserApiQueryBatchSummary;
import com.geek.server.domain.UserApiQueryRecord;
import com.geek.server.service.IUserApiQueryRecordService;
import com.geek.common.utils.poi.ExcelUtil;
import com.geek.common.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

/**
 * 接口查询记录通用Controller
 * 
 * @author geek
 * @date 2026-03-09
 */
@RestController
@RequestMapping("/server/apiRecord")
@Tag(name = "【接口查询记录通用】管理")
public class UserApiQueryRecordController extends BaseController
{
    @Autowired
    private IUserApiQueryRecordService userApiQueryRecordService;

    /**
     * 查询接口查询记录通用列表
     */
    @Operation(summary = "查询接口查询记录通用列表")
//    @PreAuthorize("@ss.hasPermi('server:apiRecord:list')")
    @GetMapping("/list")
    public TableDataInfo list(UserApiQueryRecord userApiQueryRecord)
    {
        startPage();
        // 列表查询统一按当前登录用户过滤
        Long currentUserId = SecurityUtils.getUserId();
        userApiQueryRecord.setUserId(currentUserId);
        if (!SecurityUtils.isAdmin()) {
            userApiQueryRecord.setCreateBy(SecurityUtils.getUsername());
        }
        List<UserApiQueryRecord> list = userApiQueryRecordService.selectUserApiQueryRecordList(userApiQueryRecord);
        return getDataTable(list);
    }

    /**
     * 查询记录分组列表：按手机号 + 批次聚合，支持分页；含单条、批量等全部类型，可按 queryType 筛选
     */
    @Operation(summary = "查询记录分组列表（按批次聚合）")
    @GetMapping("/singleBatch/list")
    public TableDataInfo singleBatchList(UserApiQueryRecord userApiQueryRecord)
    {
        startPage();
        // 先按当前登录用户过滤，再做分组计算
        Long currentUserId = SecurityUtils.getUserId();
        userApiQueryRecord.setUserId(currentUserId);
        if (!SecurityUtils.isAdmin())
        {
            userApiQueryRecord.setCreateBy(SecurityUtils.getUsername());
        }
        List<UserApiQueryBatchSummary> list = userApiQueryRecordService.selectSingleQueryBatchGroupList(userApiQueryRecord);
        return getDataTable(list);
    }

    /**
     * 某批次（phone + batchKey）下各平台完整记录，不区分单条/批量
     */
    @Operation(summary = "查询批次详情（全平台）")
    @GetMapping("/singleBatch/detail")
    public AjaxResult singleBatchDetail(
        @RequestParam("phone") String phone,
        @RequestParam("batchKey") String batchKey,
        UserApiQueryRecord userApiQueryRecord)
    {
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(batchKey))
        {
            return error("phone 与 batchKey 不能为空");
        }
        Long currentUserId = SecurityUtils.getUserId();
        String createBy = null;
        // 权限判断必须基于当前登录用户，不能信任前端传入的 userId
        if (!SecurityUtils.isAdmin())
        {
            createBy = SecurityUtils.getUsername();
        }
        List<UserApiQueryRecord> list = userApiQueryRecordService.selectSingleQueryBatchDetailList(
            phone.trim(), batchKey.trim(), createBy, currentUserId);
        return success(list);
    }

    /**
     * 导出接口查询记录通用列表
     */
    @Operation(summary = "导出接口查询记录通用列表")
//    @PreAuthorize("@ss.hasPermi('server:apiRecord:export')")
    @Log(title = "接口查询记录通用", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, UserApiQueryRecord userApiQueryRecord)
    {
        // 导出查询统一按当前登录用户过滤
        Long currentUserId = SecurityUtils.getUserId();
        userApiQueryRecord.setUserId(currentUserId);
        // 与列表保持一致：非管理员仅可导出本人数据
        if (!SecurityUtils.isAdmin()) {
            userApiQueryRecord.setCreateBy(SecurityUtils.getUsername());
        }
        List<UserApiQueryRecord> list = userApiQueryRecordService.selectUserApiQueryRecordList(userApiQueryRecord);
        ExcelUtil<UserApiQueryRecord> util = new ExcelUtil<>(UserApiQueryRecord.class);
        util.exportExcel(response, list, "接口查询记录通用数据");
    }

    /**
     * 获取接口查询记录通用详细信息
     */
    @Operation(summary = "获取接口查询记录通用详细信息")
//    @PreAuthorize("@ss.hasPermi('server:apiRecord:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(userApiQueryRecordService.selectUserApiQueryRecordById(id));
    }

    /**
     * 新增接口查询记录通用
     */
    @Operation(summary = "新增接口查询记录通用")
//    @PreAuthorize("@ss.hasPermi('server:apiRecord:add')")
    @Log(title = "接口查询记录通用", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody UserApiQueryRecord userApiQueryRecord)
    {
        return toAjax(userApiQueryRecordService.insertUserApiQueryRecord(userApiQueryRecord));
    }

    /**
     * 修改接口查询记录通用
     */
    @Operation(summary = "修改接口查询记录通用")
    @PreAuthorize("@ss.hasPermi('server:apiRecord:edit')")
    @Log(title = "接口查询记录通用", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UserApiQueryRecord userApiQueryRecord)
    {
        return toAjax(userApiQueryRecordService.updateUserApiQueryRecord(userApiQueryRecord));
    }

    /**
     * 删除接口查询记录通用
     */
    @Operation(summary = "删除接口查询记录通用")
    @PreAuthorize("@ss.hasPermi('server:apiRecord:remove')")
    @Log(title = "接口查询记录通用", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable( name = "ids" ) Long[] ids) 
    {
        return toAjax(userApiQueryRecordService.deleteUserApiQueryRecordByIds(ids));
    }
}
