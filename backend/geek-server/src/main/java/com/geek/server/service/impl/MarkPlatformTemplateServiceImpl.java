package com.geek.server.service.impl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.common.core.domain.entity.SysMenu;
import com.geek.common.exception.ServiceException;

import com.geek.common.utils.DateUtils;
import com.geek.common.utils.SecurityUtils;
import com.geek.server.domain.MarkPlatformTemplate;
import com.geek.server.mapper.MarkPlatformTemplateMapper;
import com.geek.server.service.IMarkPlatformTemplateService;
import com.mybatisflex.core.query.QueryChain;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 标记平台模板服务实现
 */
@Service
public class MarkPlatformTemplateServiceImpl implements IMarkPlatformTemplateService {
    private static final Long MARK_ROOT_MENU_ID = 900100000001L;
    private static final String MARK_USER_MENU_COMPONENT = "server/mark/user/index";
    private static final Map<String, String> LEGACY_PLATFORM_NAME_MAP = new LinkedHashMap<>();

    static {
        LEGACY_PLATFORM_NAME_MAP.put("mobile_gaopin", "高频拦截");
        LEGACY_PLATFORM_NAME_MAP.put("td_gaopin", "泰迪高频");
        LEGACY_PLATFORM_NAME_MAP.put("td_second", "泰迪二次");
        LEGACY_PLATFORM_NAME_MAP.put("qihu_first", "360首次");
        LEGACY_PLATFORM_NAME_MAP.put("qihu_second", "360二次");
        LEGACY_PLATFORM_NAME_MAP.put("dianhuabang", "电话邦");
        LEGACY_PLATFORM_NAME_MAP.put("tencent_mark", "腾讯");
    }

    @Autowired
    private MarkPlatformTemplateMapper markPlatformTemplateMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public MarkPlatformTemplate selectMarkPlatformTemplateById(Long id) {
        MarkPlatformTemplate template = markPlatformTemplateMapper.selectMarkPlatformTemplateById(id);
        if (template == null) {
            return null;
        }
        assertTemplateAccessible(template);
        return template;
    }
    @Override
    public MarkPlatformTemplate selectOwnerDefaultTemplate(Long ownerUserId) {
        if (ownerUserId == null) {
            return null;
        }
        if (!isAdminRole()) {
            Long currentUserId = SecurityUtils.getUserId();
            if (currentUserId == null || !ownerUserId.equals(currentUserId)) {
                throw new ServiceException("无权访问该模板");
            }
        }
        return markPlatformTemplateMapper.selectOwnerDefaultTemplate(ownerUserId);
    }

    @Override
    public List<MarkPlatformTemplate> selectMarkPlatformTemplateList(MarkPlatformTemplate query) {
        MarkPlatformTemplate scopedQuery = query == null ? new MarkPlatformTemplate() : query;
        applyOwnerScope(scopedQuery);
        return markPlatformTemplateMapper.selectMarkPlatformTemplateList(scopedQuery);
    }

    @Override
    public List<Map<String, Object>> selectPlatformOptions() {
        Map<String, String> dynamicPlatformMap = resolveMenuPlatformNameMap();
        Map<String, String> sourceMap = new LinkedHashMap<>();
        Set<String> systemCodes = new HashSet<>();
        if (dynamicPlatformMap.isEmpty()) {
            for (Map.Entry<String, String> entry : LEGACY_PLATFORM_NAME_MAP.entrySet()) {
                sourceMap.put(entry.getKey(), entry.getValue());
                systemCodes.add(entry.getKey());
            }
        } else {
            for (Map.Entry<String, String> entry : dynamicPlatformMap.entrySet()) {
                sourceMap.put(entry.getKey(), entry.getValue());
                systemCodes.add(entry.getKey());
            }
        }
        Map<String, String> templatePlatformMap = resolveTemplatePlatformNameMap();
        for (Map.Entry<String, String> entry : templatePlatformMap.entrySet()) {
            sourceMap.putIfAbsent(entry.getKey(), entry.getValue());
        }
        List<Map<String, Object>> options = new ArrayList<>();
        for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("code", entry.getKey());
            option.put("name", StringUtils.defaultIfBlank(entry.getValue(), entry.getKey()));
            option.put("isSystem", systemCodes.contains(entry.getKey()));
            options.add(option);
        }
        return options;
    }

    @Override
    public int insertMarkPlatformTemplate(MarkPlatformTemplate markPlatformTemplate) {
        if (markPlatformTemplate == null) {
            throw new ServiceException("模板参数不能为空");
        }
        Long ownerUserId = SecurityUtils.getUserId();
        if (ownerUserId == null) {
            throw new ServiceException("无法识别模板归属人");
        }
        markPlatformTemplate.setStatus(StringUtils.defaultIfBlank(markPlatformTemplate.getStatus(), "0"));
        markPlatformTemplate.setOwnerUserId(ownerUserId);
        String requestedDefault = StringUtils.defaultIfBlank(markPlatformTemplate.getIsDefault(), "0");
        MarkPlatformTemplate ownerDefault = markPlatformTemplateMapper.selectOwnerDefaultTemplate(ownerUserId);
        markPlatformTemplate.setIsDefault("1".equals(requestedDefault) || ownerDefault == null ? "1" : "0");
        markPlatformTemplate.setCreateBy(SecurityUtils.getUsername());
        markPlatformTemplate.setCreateTime(DateUtils.getNowDate());
        int rows = markPlatformTemplateMapper.insertMarkPlatformTemplate(markPlatformTemplate);
        if (rows > 0 && "1".equals(markPlatformTemplate.getIsDefault())) {
            markPlatformTemplateMapper.clearOwnerDefaultTemplate(ownerUserId, markPlatformTemplate.getId());
        }
        ensureOwnerHasDefault(ownerUserId);
        return rows;
    }

    @Override
    public int updateMarkPlatformTemplate(MarkPlatformTemplate markPlatformTemplate) {
        if (markPlatformTemplate == null || markPlatformTemplate.getId() == null) {
            throw new ServiceException("模板参数不能为空");
        }
        MarkPlatformTemplate stored = markPlatformTemplateMapper.selectMarkPlatformTemplateById(markPlatformTemplate.getId());
        if (stored == null) {
            throw new ServiceException("模板不存在");
        }
        assertTemplateAccessible(stored);
        Long ownerUserId = stored.getOwnerUserId();
        markPlatformTemplate.setOwnerUserId(ownerUserId);
        markPlatformTemplate.setIsDefault(normalizeDefaultFlag(
                StringUtils.defaultIfBlank(markPlatformTemplate.getIsDefault(), stored.getIsDefault())
        ));
        markPlatformTemplate.setUpdateBy(SecurityUtils.getUsername());
        markPlatformTemplate.setUpdateTime(DateUtils.getNowDate());
        int rows = markPlatformTemplateMapper.updateMarkPlatformTemplate(markPlatformTemplate);
        if (rows > 0 && "1".equals(markPlatformTemplate.getIsDefault())) {
            markPlatformTemplateMapper.clearOwnerDefaultTemplate(ownerUserId, markPlatformTemplate.getId());
        }
        ensureOwnerHasDefault(ownerUserId);
        return rows;
    }

    @Override
    public int deleteMarkPlatformTemplateByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        Set<Long> affectedOwnerIds = new HashSet<>();
        for (Long id : ids) {
            MarkPlatformTemplate stored = markPlatformTemplateMapper.selectMarkPlatformTemplateById(id);
            if (stored == null) {
                continue;
            }
            assertTemplateAccessible(stored);
            if (stored.getOwnerUserId() != null) {
                affectedOwnerIds.add(stored.getOwnerUserId());
            }
        }
        int rows = markPlatformTemplateMapper.deleteMarkPlatformTemplateByIds(ids);
        for (Long ownerUserId : affectedOwnerIds) {
            ensureOwnerHasDefault(ownerUserId);
        }
        return rows;
    }

    private Map<String, String> resolveMenuPlatformNameMap() {
        Map<String, String> platformNameMap = new LinkedHashMap<>();
        List<SysMenu> menus = QueryChain.of(SysMenu.class)
                .eq(SysMenu::getParentId, MARK_ROOT_MENU_ID)
                .eq(SysMenu::getMenuType, "C")
                .eq(SysMenu::getStatus, "0")
                .eq(SysMenu::getComponent, MARK_USER_MENU_COMPONENT)
                .orderBy(SysMenu::getOrderNum, true)
                .list();
        for (SysMenu menu : menus) {
            Map<String, Object> queryMap = parseJsonMap(menu.getQuery());
            String platformCode = firstNonBlank(
                    getStringOrNull(queryMap == null ? null : queryMap.get("platformCode")),
                    getStringOrNull(queryMap == null ? null : queryMap.get("code")),
                    getStringOrNull(queryMap == null ? null : queryMap.get("value"))
            );
            if (StringUtils.isBlank(platformCode)) {
                continue;
            }
            String platformName = firstNonBlank(
                    getStringOrNull(queryMap == null ? null : queryMap.get("platformName")),
                    getStringOrNull(queryMap == null ? null : queryMap.get("name")),
                    StringUtils.trimToNull(menu.getMenuName()),
                    platformCode
            );
            platformNameMap.putIfAbsent(platformCode, platformName);
        }
        return platformNameMap;
    }

    private Map<String, String> resolveTemplatePlatformNameMap() {
        Map<String, String> platformNameMap = new LinkedHashMap<>();
        MarkPlatformTemplate query = new MarkPlatformTemplate();
        applyOwnerScope(query);
        List<MarkPlatformTemplate> templateList = markPlatformTemplateMapper.selectMarkPlatformTemplateList(query);
        for (MarkPlatformTemplate template : templateList) {
            String templateInfo = template.getTemplateInfo();
            if (StringUtils.isBlank(templateInfo)) {
                continue;
            }
            try {
                List<Object> rawList = objectMapper.readValue(templateInfo, new TypeReference<List<Object>>() {});
                for (Object item : rawList) {
                    addTemplatePlatformEntry(platformNameMap, item);
                }
            } catch (Exception ignored) {
                // ignore malformed template info and continue collecting from others
            }
        }
        return platformNameMap;
    }

    private void addTemplatePlatformEntry(Map<String, String> target, Object item) {
        if (item == null) {
            return;
        }
        String code = null;
        String name = null;
        if (item instanceof String) {
            code = StringUtils.trimToNull((String) item);
        } else if (item instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) item;
            code = firstNonBlank(
                    getStringOrNull(map.get("platformCode")),
                    getStringOrNull(map.get("code")),
                    getStringOrNull(map.get("value"))
            );
            name = firstNonBlank(
                    getStringOrNull(map.get("platformName")),
                    getStringOrNull(map.get("name")),
                    getStringOrNull(map.get("label"))
            );
        }
        if (StringUtils.isBlank(code)) {
            return;
        }
        String normalizedName = StringUtils.defaultIfBlank(name, code);
        if (!target.containsKey(code)) {
            target.put(code, normalizedName);
            return;
        }
        String currentName = target.get(code);
        if (StringUtils.equals(currentName, code) && StringUtils.isNotBlank(normalizedName)) {
            target.put(code, normalizedName);
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getStringOrNull(Object value) {
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
    private String normalizeDefaultFlag(String value) {
        return "1".equals(StringUtils.trimToEmpty(value)) ? "1" : "0";
    }

    private void ensureOwnerHasDefault(Long ownerUserId) {
        if (ownerUserId == null) {
            return;
        }
        MarkPlatformTemplate ownerDefault = markPlatformTemplateMapper.selectOwnerDefaultTemplate(ownerUserId);
        if (ownerDefault != null) {
            return;
        }
        MarkPlatformTemplate query = new MarkPlatformTemplate();
        query.setOwnerUserId(ownerUserId);
        query.setStatus("0");
        List<MarkPlatformTemplate> templates = markPlatformTemplateMapper.selectMarkPlatformTemplateList(query);
        if (templates == null || templates.isEmpty()) {
            return;
        }
        MarkPlatformTemplate candidate = templates.get(0);
        MarkPlatformTemplate update = new MarkPlatformTemplate();
        update.setId(candidate.getId());
        update.setIsDefault("1");
        update.setUpdateBy(SecurityUtils.getUsername());
        update.setUpdateTime(DateUtils.getNowDate());
        markPlatformTemplateMapper.updateMarkPlatformTemplate(update);
        markPlatformTemplateMapper.clearOwnerDefaultTemplate(ownerUserId, candidate.getId());
    }

    private void applyOwnerScope(MarkPlatformTemplate query) {
        if (query == null || isAdminRole()) {
            return;
        }
        query.setOwnerUserId(SecurityUtils.getUserId());
    }

    private void assertTemplateAccessible(MarkPlatformTemplate template) {
        if (template == null || isAdminRole()) {
            return;
        }
        Long ownerUserId = template.getOwnerUserId();
        Long currentUserId = SecurityUtils.getUserId();
        if (ownerUserId == null || currentUserId == null || !ownerUserId.equals(currentUserId)) {
            throw new ServiceException("无权访问该模板");
        }
    }

    private boolean isAdminRole() {
        return SecurityUtils.isAdmin() || SecurityUtils.hasRole("admin");
    }
}
