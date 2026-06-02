package com.geek.web.controller.system;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.mybatisflex.core.query.QueryChain;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.domain.entity.SysDept;
import com.geek.common.core.domain.entity.SysRole;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.common.core.page.PageDomain;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.core.page.TableSupport;
import com.geek.common.enums.BusinessType;
import com.geek.common.utils.SecurityUtils;
import com.geek.common.utils.StringUtils;
import com.geek.common.utils.poi.ExcelUtil;
import com.geek.server.domain.MarkPlatformTemplate;
import com.geek.server.service.IMarkPlatformTemplateService;
import com.geek.system.service.ISysDeptService;
import com.geek.system.service.ISysPostService;
import com.geek.system.service.ISysRoleService;
import com.geek.system.service.ISysUserService;
import com.mybatisflex.core.paginate.Page;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 用户信息
 * 
 * @author geek
 */
@Tag(name = "用户信息")
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController {
    private static final String ROLE_KEY_AGENT = "agent";
    private static final String ROLE_KEY_MARK_AGENT = "mark_agent";
    private static final String ROLE_KEY_USER = "user";
    private static final String ROLE_KEY_MARK_USER = "mark_user";
    private static final List<String> AGENT_SELF_ROLE_KEYS = Arrays.asList(ROLE_KEY_AGENT, ROLE_KEY_MARK_AGENT);
    private static final List<String> AGENT_DOWNSTREAM_ROLE_KEYS = Arrays.asList(ROLE_KEY_USER, ROLE_KEY_MARK_USER);

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private ISysPostService postService;

    @Autowired
    private IMarkPlatformTemplateService markPlatformTemplateService;

    /**
     * 获取用户列表
     */
    @Operation(summary = "获取用户列表")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public TableDataInfo<SysUser> list(SysUser user) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<SysUser> list = userService.page(user, pageDomain.getPageNum(), pageDomain.getPageSize());
        return getDataTable(list);
    }

    @Operation(summary = "导出用户列表")
    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:user:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysUser user) {
        userService.export(user, response);
    }

    @Operation(summary = "导入用户列表")
    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('system:user:import')")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<SysUser> util = new ExcelUtil<>(SysUser.class);
        List<SysUser> userList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取导入用户模板")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil<SysUser> util = new ExcelUtil<>(SysUser.class);
        util.importTemplateExcel(response, "用户数据");
    }

    /**
     * 根据用户编号获取详细信息
     */
    @Operation(summary = "根据用户编号获取详细信息")
    @PreAuthorize("@ss.hasPermi('system:user:query') or @ss.hasAnyRoles('agent,mark_agent')")
    @GetMapping(value = { "/", "/{userId}" })
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId) {
        AjaxResult ajax = AjaxResult.success();
        if (StringUtils.isNotNull(userId)) {
            userService.checkUserDataScope(userId);
            SysUser sysUser = userService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, sysUser);
            ajax.put("postIds", postService.selectPostListByUserId(userId));
            ajax.put("roleIds", sysUser.getRoles().stream().map(SysRole::getRoleId).collect(Collectors.toList()));
        }
        List<SysRole> roles;
        if (SecurityUtils.isAdmin()) {
            roles = roleService.selectRoleAll();
        } else if (isAgentOperator()) {
            roles = selectEnabledRolesByKeys(resolveAllowedRoleKeysForTarget(userId));
        } else {
            roles = roleService.selectRoleAll().stream().filter(r -> !r.isAdmin()).collect(Collectors.toList());
        }
        ajax.put("roles", roles);
        ajax.put("posts", postService.list());
        return ajax;
    }

    /**
     * 新增用户
     */
    @Operation(summary = "新增用户")
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUser user) {
        try {
            checkAssignableRoleKeys(null, user.getRoleIds());
            applyAgentMarkTemplate(user, true);
            userService.checkUserAllowedBeforeUpdate(user);
        } catch (Exception e) {
            return error("新增用户'" + user.getUserName() + "'失败，" + e.getMessage());
        }
        user.setCreateBy(getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return toAjax(userService.insertUser(user));
    }

    /**
     * 修改用户
     */
    @Operation(summary = "修改用户")
    @PreAuthorize("@ss.hasPermi('system:user:edit') or @ss.hasAnyRoles('agent,mark_agent')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysUser user) {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        try {
            checkAssignableRoleKeys(user.getUserId(), user.getRoleIds());
            applyAgentMarkTemplate(user, false);
            userService.checkUserAllowedBeforeUpdate(user);
        } catch (Exception e) {
            return error("修改用户'" + user.getUserName() + "'失败，" + e.getMessage());
        }
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUser(user));
    }

    private void applyAgentMarkTemplate(SysUser user, boolean isCreate) {
        if (user == null || !isAgentOperator()) {
            return;
        }
        Long currentUserId = getUserId();
        if (currentUserId == null) {
            return;
        }
        boolean isSelf = !isCreate && currentUserId.equals(user.getUserId());
        if (user.getRelMarkTemplate() == null && !isSelf) {
            Long defaultTemplateId = resolveAgentDefaultMarkTemplateId(currentUserId);
            if (defaultTemplateId != null) {
                user.setRelMarkTemplate(defaultTemplateId);
            }
        }
        if (user.getRelMarkTemplate() == null) {
            if (!isSelf) {
                throw new IllegalArgumentException("请选择标记模板");
            }
            return;
        }
        MarkPlatformTemplate template = markPlatformTemplateService.selectMarkPlatformTemplateById(user.getRelMarkTemplate());
        if (template == null || !"0".equals(template.getStatus())) {
            throw new IllegalArgumentException("所选标记模板不可用");
        }
    }

    private Long resolveAgentDefaultMarkTemplateId(Long currentUserId) {
        MarkPlatformTemplate ownerDefaultTemplate = markPlatformTemplateService.selectOwnerDefaultTemplate(currentUserId);
        if (ownerDefaultTemplate != null) {
            return ownerDefaultTemplate.getId();
        }
        SysUser currentUser = userService.selectUserById(currentUserId);
        if (currentUser != null && currentUser.getRelMarkTemplate() != null) {
            return currentUser.getRelMarkTemplate();
        }
        MarkPlatformTemplate query = new MarkPlatformTemplate();
        query.setStatus("0");
        query.setIsDefault("1");
        List<MarkPlatformTemplate> ownTemplates = markPlatformTemplateService.selectMarkPlatformTemplateList(query);
        if (!ownTemplates.isEmpty()) {
            return ownTemplates.get(0).getId();
        }
        query.setIsDefault(null);
        ownTemplates = markPlatformTemplateService.selectMarkPlatformTemplateList(query);
        if (ownTemplates.size() == 1) {
            return ownTemplates.get(0).getId();
        }
        return null;
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户")
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable(name = "userIds") List<Long> userIds) {
        if (ArrayUtils.contains(userIds.toArray(), getUserId())) {
            return error("当前用户不能删除");
        }
        return toAjax(userService.deleteUserByIds(userIds));
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码")
    @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody SysUser user) {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUser(user));
    }

    /**
     * 状态修改
     */
    @Operation(summary = "状态修改")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysUser user) {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUserStatus(user));
    }

    /**
     * 根据用户编号获取授权角色
     */
    @Operation(summary = "根据用户编号获取授权角色")
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/authRole/{userId}")
    public AjaxResult authRole(@PathVariable("userId") Long userId) {
        AjaxResult ajax = AjaxResult.success();
        SysUser user = userService.selectUserById(userId);
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        if (!SecurityUtils.isAdmin()) {
            userService.checkUserDataScope(userId);
        }
        List<SysRole> visibleRoles;
        if (SecurityUtils.isAdmin()) {
            visibleRoles = roles;
        } else if (isAgentOperator()) {
            List<String> allowedRoleKeys = resolveAllowedRoleKeysForTarget(userId);
            visibleRoles = roles.stream()
                    .filter(r -> allowedRoleKeys.contains(r.getRoleKey()))
                    .collect(Collectors.toList());
        } else {
            visibleRoles = roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList());
        }
        ajax.put("user", user);
        ajax.put("roles", visibleRoles);
        return ajax;
    }

    /**
     * 用户授权角色
     */
    @Operation(summary = "用户授权角色")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.GRANT)
    @PutMapping("/authRole")
    public AjaxResult insertAuthRole(Long userId, List<Long> roleIds) {
        userService.checkUserDataScope(userId);
        checkAssignableRoleKeys(userId, roleIds);
        userService.insertUserAuth(userId, roleIds);
        return success();
    }

    private boolean isAgentOperator() {
        return !SecurityUtils.isAdmin() && (SecurityUtils.hasRole(ROLE_KEY_AGENT) || SecurityUtils.hasRole(ROLE_KEY_MARK_AGENT));
    }

    private List<String> resolveAllowedRoleKeysForTarget(Long targetUserId) {
        if (!isAgentOperator()) {
            return List.of();
        }
        Long currentUserId = getUserId();
        boolean targetIsSelf = targetUserId != null && currentUserId != null && currentUserId.equals(targetUserId);
        return targetIsSelf ? AGENT_SELF_ROLE_KEYS : AGENT_DOWNSTREAM_ROLE_KEYS;
    }

    private List<SysRole> selectEnabledRolesByKeys(List<String> roleKeys) {
        if (CollectionUtils.isEmpty(roleKeys)) {
            return List.of();
        }
        return QueryChain.of(SysRole.class)
                .in(SysRole::getRoleKey, roleKeys)
                .eq(SysRole::getStatus, "0")
                .list();
    }

    private void checkAssignableRoleKeys(Long targetUserId, List<Long> roleIds) {
        if (!isAgentOperator()) {
            return;
        }
        if (CollectionUtils.isEmpty(roleIds)) {
            throw new IllegalArgumentException("请选择角色");
        }
        Set<Long> distinctRoleIds = roleIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinctRoleIds.isEmpty()) {
            throw new IllegalArgumentException("请选择角色");
        }
        List<SysRole> selectedRoles = QueryChain.of(SysRole.class)
                .in(SysRole::getRoleId, distinctRoleIds)
                .eq(SysRole::getStatus, "0")
                .list();
        if (selectedRoles.size() != distinctRoleIds.size()) {
            throw new IllegalArgumentException("包含不可用角色");
        }
        List<String> allowedRoleKeys = resolveAllowedRoleKeysForTarget(targetUserId);
        boolean hasIllegalRole = selectedRoles.stream()
                .map(SysRole::getRoleKey)
                .anyMatch(roleKey -> !allowedRoleKeys.contains(roleKey));
        if (hasIllegalRole) {
            boolean targetIsSelf = targetUserId != null && targetUserId.equals(getUserId());
            throw new IllegalArgumentException(targetIsSelf ? "仅允许分配标记代理角色" : "仅允许分配标记用户角色");
        }
    }

    /**
     * 获取部门树列表
     */
    @Operation(summary = "获取部门树列表")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/deptTree")
    public AjaxResult deptTree(SysDept dept) {
        return success(deptService.selectDeptTreeList(dept));
    }
}
