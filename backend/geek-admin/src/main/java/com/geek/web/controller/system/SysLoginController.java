package com.geek.web.controller.system;

import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.geek.common.constant.Constants;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.domain.entity.SysMenu;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.common.core.domain.model.LoginBody;
import com.geek.common.core.domain.model.LoginUser;
import com.geek.common.core.text.Convert;
import com.geek.common.utils.DateUtils;
import com.geek.common.utils.Sb;
import com.geek.common.utils.SecurityUtils;
import com.geek.common.utils.StringUtils;
import com.geek.framework.web.service.SysLoginService;
import com.geek.framework.web.service.SysPermissionService;
import com.geek.framework.web.service.TokenService;
import com.geek.server.domain.MarkUserPlatformPrice;
import com.geek.server.service.IMarkOrderService;
import com.geek.system.service.ISysConfigService;
import com.geek.system.service.ISysMenuService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 登录验证
 * 
 * @author geek
 */
@Tag(name = "登录验证")
@RestController
public class SysLoginController {
    private static final Logger log = LoggerFactory.getLogger(SysLoginController.class);
    private static final String MARK_USER_MENU_COMPONENT = "server/mark/user/index";
    private static final String MARK_AGENT_PROCESS_MENU_COMPONENT = "server/mark/agent/process/platform";

    @Autowired
    private SysLoginService loginService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private IMarkOrderService markOrderService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 登录方法
     * 
     * @param loginBody 登录信息
     * @return 结果
     */
    @Operation(summary = "登录方法")
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody) {
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        String token = loginService.login(
                loginBody.getUsername(),
                loginBody.getPassword(),
                loginBody.getCaptcha());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 获取用户信息
     * 
     * @return 用户信息
     */
    @Operation(summary = "获取用户信息")
    @GetMapping("getInfo")
    public AjaxResult getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        if (!loginUser.getPermissions().equals(permissions)) {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        if (user.getAvatar() != null) {
            try {

                //http://localhost:8080/files/master/avatar/admin/1/20260305112747-avatar.jpeg
//                user.setAvatar(Sb.getURL(user.getAvatar()));
                String avatar = user.getAvatar();
                if (!(avatar.startsWith("http://") || avatar.startsWith("https://"))) {
                    user.setAvatar(Sb.getURL(avatar));
                }
            } catch (Exception e) {
            }
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        ajax.put("isDefaultModifyPwd", initPasswordIsModify(user.getPwdUpdateDate()));
        ajax.put("isPasswordExpired", passwordIsExpiration(user.getPwdUpdateDate()));
        return ajax;
    }

    // 检查初始密码是否提醒修改
    public boolean initPasswordIsModify(Date pwdUpdateDate) {
        Integer initPasswordModify = Convert.toInt(configService.selectConfigByKey("sys.account.initPasswordModify"));
        return initPasswordModify != null && initPasswordModify == 1 && pwdUpdateDate == null;
    }

    // 检查密码是否过期
    public boolean passwordIsExpiration(Date pwdUpdateDate) {
        Integer passwordValidateDays = Convert
                .toInt(configService.selectConfigByKey("sys.account.passwordValidateDays"));
        if (passwordValidateDays != null && passwordValidateDays > 0) {
            if (StringUtils.isNull(pwdUpdateDate)) {
                // 如果从未修改过初始密码，直接提醒过期
                return true;
            }
            Date nowDate = DateUtils.getNowDate();
            return DateUtils.differentDaysByMillisecond(nowDate, pwdUpdateDate) > passwordValidateDays;
        }
        return false;
    }

    /**
     * 获取路由信息
     * 
     * @return 路由信息
     */
    @Operation(summary = "获取路由信息")
    @GetMapping("getRouters")
    public AjaxResult getRouters() {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        menus = filterMarkUserMenusByTemplate(menus);
        return AjaxResult.success(menuService.buildMenus(menus));
    }

    private List<SysMenu> filterMarkUserMenusByTemplate(List<SysMenu> menus) {
        if (StringUtils.isEmpty(menus)) {
            return menus;
        }
        Set<String> allowedPlatformCodes = resolveCurrentUserPlatformCodes();
        if (allowedPlatformCodes == null) {
            return menus;
        }
        List<SysMenu> filteredMenus = removeDisallowedPlatformMenus(menus, allowedPlatformCodes);
        reorderPlatformMenusByTemplate(filteredMenus, allowedPlatformCodes);
        return filteredMenus;
    }

    private Set<String> resolveCurrentUserPlatformCodes() {
        try {
            List<MarkUserPlatformPrice> platformPriceList = markOrderService.selectMyPlatformPriceList();
            Set<String> allowedPlatformCodes = new LinkedHashSet<>();
            for (MarkUserPlatformPrice price : platformPriceList) {
                if (price == null) {
                    continue;
                }
                String platformCode = StringUtils.trimToNull(price.getPlatformCode());
                if (platformCode != null) {
                    allowedPlatformCodes.add(platformCode);
                }
            }
            return allowedPlatformCodes;
        } catch (Exception e) {
            Long userId = SecurityUtils.getUserId();
            log.warn("skip template-driven route filter because platform resolution failed, userId={}", userId, e);
            return null;
        }
    }

    private List<SysMenu> removeDisallowedPlatformMenus(List<SysMenu> menus, Set<String> allowedPlatformCodes) {
        List<SysMenu> filtered = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (menu == null) {
                continue;
            }
            if (StringUtils.isNotEmpty(menu.getChildren())) {
                menu.setChildren(removeDisallowedPlatformMenus(menu.getChildren(), allowedPlatformCodes));
            }
            if (isPlatformDrivenMenu(menu)) {
                String platformCode = resolvePlatformCode(menu);
                if (StringUtils.isNotEmpty(platformCode) && !allowedPlatformCodes.contains(platformCode)) {
                    continue;
                }
            }
            filtered.add(menu);
        }
        return filtered;
    }

    private void reorderPlatformMenusByTemplate(List<SysMenu> menus, Set<String> platformCodesInOrder) {
        if (StringUtils.isEmpty(menus) || StringUtils.isEmpty(platformCodesInOrder)) {
            return;
        }
        Map<String, Integer> platformOrderMap = buildPlatformOrderMap(platformCodesInOrder);
        reorderPlatformMenusRecursively(menus, platformOrderMap);
    }

    private void reorderPlatformMenusRecursively(List<SysMenu> menus, Map<String, Integer> platformOrderMap) {
        if (StringUtils.isEmpty(menus)) {
            return;
        }
        reorderPlatformMenusInCurrentLevel(menus, platformOrderMap);
        for (SysMenu menu : menus) {
            if (menu == null || StringUtils.isEmpty(menu.getChildren())) {
                continue;
            }
            reorderPlatformMenusRecursively(menu.getChildren(), platformOrderMap);
        }
    }

    private void reorderPlatformMenusInCurrentLevel(List<SysMenu> menus, Map<String, Integer> platformOrderMap) {
        List<Integer> platformPositions = new ArrayList<>();
        List<SysMenu> platformMenus = new ArrayList<>();
        for (int i = 0; i < menus.size(); i++) {
            SysMenu menu = menus.get(i);
            if (menu == null || !isPlatformDrivenMenu(menu)) {
                continue;
            }
            String platformCode = resolvePlatformCode(menu);
            if (StringUtils.isBlank(platformCode)) {
                continue;
            }
            platformPositions.add(i);
            platformMenus.add(menu);
        }
        if (platformMenus.size() <= 1) {
            return;
        }
        platformMenus.sort((left, right) -> Integer.compare(
                resolvePlatformOrder(platformOrderMap, left),
                resolvePlatformOrder(platformOrderMap, right)
        ));
        for (int i = 0; i < platformPositions.size(); i++) {
            menus.set(platformPositions.get(i), platformMenus.get(i));
        }
    }
    private Map<String, Integer> buildPlatformOrderMap(Set<String> platformCodesInOrder) {
        Map<String, Integer> platformOrderMap = new LinkedHashMap<>();
        int order = 0;
        for (String platformCode : platformCodesInOrder) {
            if (StringUtils.isBlank(platformCode) || platformOrderMap.containsKey(platformCode)) {
                continue;
            }
            platformOrderMap.put(platformCode, order++);
        }
        return platformOrderMap;
    }
    private int resolvePlatformOrder(Map<String, Integer> platformOrderMap, SysMenu menu) {
        String platformCode = resolvePlatformCode(menu);
        if (StringUtils.isBlank(platformCode)) {
            return Integer.MAX_VALUE;
        }
        return platformOrderMap.getOrDefault(platformCode, Integer.MAX_VALUE);
    }

    private boolean isPlatformDrivenMenu(SysMenu menu) {
        return isMarkUserPlatformMenu(menu) || isMarkAgentProcessPlatformMenu(menu);
    }

    private boolean isMarkUserPlatformMenu(SysMenu menu) {
        return menu != null && StringUtils.equals(MARK_USER_MENU_COMPONENT, menu.getComponent());
    }

    private boolean isMarkAgentProcessPlatformMenu(SysMenu menu) {
        return menu != null && StringUtils.equals(MARK_AGENT_PROCESS_MENU_COMPONENT, menu.getComponent());
    }

    private String resolvePlatformCode(SysMenu menu) {
        if (menu == null) {
            return null;
        }
        Map<String, Object> queryMap = parseQueryMap(menu.getQuery());
        if (StringUtils.isEmpty(queryMap)) {
            return null;
        }
        return firstNonBlank(
                asTrimmedString(queryMap.get("platformCode")),
                asTrimmedString(queryMap.get("code")),
                asTrimmedString(queryMap.get("value"))
        );
    }

    private Map<String, Object> parseQueryMap(String query) {
        if (StringUtils.isBlank(query)) {
            return null;
        }
        try {
            return objectMapper.readValue(query, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ignored) {
            return null;
        }
    }

    private String asTrimmedString(Object value) {
        if (value == null) {
            return null;
        }
        return StringUtils.trimToNull(String.valueOf(value));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
