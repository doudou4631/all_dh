package com.geek.server.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.geek.common.core.domain.entity.SysMenu;
import com.geek.common.exception.ServiceException;
import com.geek.common.utils.StringUtils;
import com.geek.server.domain.MarkPlatformMapping;
import com.geek.server.mapper.MarkPlatformMappingMapper;
import com.geek.server.service.IMarkPlatformMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MarkPlatformMappingServiceImpl implements IMarkPlatformMappingService {
    private static final long USER_PARENT = 900100000102L;
    private static final long AGENT_PARENT = 900100000103L;

    @Autowired private MarkPlatformMappingMapper mapper;
    @Autowired private ObjectMapper objectMapper;

    @Override public List<MarkPlatformMapping> selectList(MarkPlatformMapping query) { return mapper.selectList(query == null ? new MarkPlatformMapping() : query); }
    @Override public MarkPlatformMapping selectById(Long id) { return mapper.selectById(id); }
    @Transactional
    @Override public int save(MarkPlatformMapping item) {
        normalize(item, true);
        if (mapper.selectList(item).stream().anyMatch(x -> StringUtils.equalsIgnoreCase(x.getSourcePlatformCode(), item.getSourcePlatformCode()) && StringUtils.equalsIgnoreCase(x.getTargetPlatformCode(), item.getTargetPlatformCode()))) throw new ServiceException("该平台映射已存在");
        int rows = mapper.insert(item);
        mapper.updateSourceSort(item.getSourcePlatformCode(), item.getSortNum());
        return rows;
    }
    @Transactional
    @Override public int update(MarkPlatformMapping item) {
        if (item == null || item.getId() == null) throw new ServiceException("映射ID不能为空");
        normalize(item, false);
        int rows = mapper.update(item);
        mapper.updateSourceName(item.getSourcePlatformCode(), item.getSourcePlatformName());
        mapper.updateTargetName(item.getTargetPlatformCode(), item.getTargetPlatformName());
        mapper.updateSourceSort(item.getSourcePlatformCode(), item.getSortNum());
        return rows;
    }
    @Override public int delete(Long id) { if (id == null) throw new ServiceException("映射ID不能为空"); return mapper.deleteById(id); }
    @Override public int updateSourceEnabled(Long id, String enabled) { validateId(id); validateEnabled(enabled); return mapper.updateSourceEnabled(id, enabled); }
    @Override public int updateTargetEnabled(Long id, String enabled) { validateId(id); validateEnabled(enabled); return mapper.updateTargetEnabled(id, enabled); }
    @Override public int updateMappingEnabled(Long id, String enabled) { if (id == null) throw new ServiceException("映射ID不能为空"); validateEnabled(enabled); return mapper.updateMappingEnabled(id, enabled); }

    @Override public boolean isSourceEnabled(String code) {
        if (StringUtils.isBlank(code)) return false;
        return selectEnabledSourceCodes().contains(code.trim().toLowerCase());
    }
    @Override public boolean isTargetEnabled(String code) {
        if (StringUtils.isBlank(code)) return false;
        return selectEnabledTargetCodes().contains(code.trim().toLowerCase());
    }
    @Override public Set<String> selectEnabledSourceCodes() { return effectiveMappings().stream().map(x -> x.getSourcePlatformCode().toLowerCase()).collect(Collectors.toSet()); }
    @Override public Set<String> selectEnabledTargetCodes() { return effectiveMappings().stream().map(x -> x.getTargetPlatformCode().toLowerCase()).collect(Collectors.toSet()); }
    private List<MarkPlatformMapping> effectiveMappings() { return mapper.selectList(new MarkPlatformMapping()).stream().filter(x -> "0".equals(x.getSourceEnabled()) && "0".equals(x.getTargetEnabled()) && "0".equals(x.getMappingEnabled())).toList(); }

    @Override public List<SysMenu> filterMenuTree(List<SysMenu> menus) {
        if (menus == null) return Collections.emptyList();
        List<MarkPlatformMapping> mappings = effectiveMappings();
        Map<String, String> sourceNames = mappings.stream().collect(Collectors.toMap(
                x -> x.getSourcePlatformCode().toLowerCase(),
                x -> x.getSourcePlatformName().trim(),
                (first, ignored) -> first));
        Map<String, String> targetSourceNames = mappings.stream().collect(Collectors.toMap(
                x -> x.getTargetPlatformCode().toLowerCase(),
                x -> x.getSourcePlatformName().trim(),
                (first, ignored) -> first));
        Map<String, Integer> sourceSorts = mappings.stream().collect(Collectors.toMap(
                x -> x.getSourcePlatformCode().toLowerCase(), MarkPlatformMapping::getSortNum, Math::min));
        Map<String, Integer> targetSourceSorts = mappings.stream().collect(Collectors.toMap(
                x -> x.getTargetPlatformCode().toLowerCase(), MarkPlatformMapping::getSortNum, Math::min));
        filterChildren(menus, null, sourceNames, targetSourceNames, sourceSorts, targetSourceSorts);
        return menus;
    }
    private void filterChildren(List<SysMenu> menus, Long parentId, Map<String, String> sourceNames,
                                Map<String, String> targetSourceNames, Map<String, Integer> sourceSorts,
                                Map<String, Integer> targetSourceSorts) {
        if (menus == null) return;
        Iterator<SysMenu> it = menus.iterator();
        while (it.hasNext()) {
            SysMenu menu = it.next();
            if (parentId != null && (parentId == USER_PARENT || parentId == AGENT_PARENT) && "C".equals(menu.getMenuType())) {
                boolean keep;
                if (parentId == USER_PARENT) {
                    String code = codeFromQuery(menu.getQuery());
                    String displayName = code == null ? null : sourceNames.get(code.toLowerCase());
                    keep = displayName != null;
                    if (keep) updateMenuDisplay(menu, displayName, sourceSorts.get(code.toLowerCase()));
                } else {
                    keep = retainEffectiveTargetCodes(menu, targetSourceNames, targetSourceSorts);
                }
                if (!keep) { it.remove(); continue; }
            }
            if (menu.getChildren() != null) filterChildren(menu.getChildren(), menu.getMenuId(), sourceNames, targetSourceNames, sourceSorts, targetSourceSorts);
        }
        if (parentId != null && (parentId == USER_PARENT || parentId == AGENT_PARENT)) {
            menus.sort(Comparator.comparing(SysMenu::getOrderNum, Comparator.nullsLast(Integer::compareTo)));
        }
    }
    private boolean retainEffectiveTargetCodes(SysMenu menu, Map<String, String> targetSourceNames, Map<String, Integer> targetSourceSorts) {
        String query = menu.getQuery();
        if (StringUtils.isBlank(query)) return false;
        try {
            JsonNode node = objectMapper.readTree(query);
            if (node.hasNonNull("platformCodes")) {
                List<String> enabledCodes = new ArrayList<>();
                String displayName = null;
                Integer sortNum = null;
                for (String code : node.get("platformCodes").asText().split(",")) {
                    String normalized = code.trim();
                    String sourceName = targetSourceNames.get(normalized.toLowerCase());
                    if (sourceName != null) {
                        enabledCodes.add(normalized);
                        if (displayName == null) {
                            displayName = sourceName;
                            sortNum = targetSourceSorts.get(normalized.toLowerCase());
                        }
                    }
                }
                if (enabledCodes.isEmpty()) return false;
                if (node instanceof ObjectNode objectNode) {
                    objectNode.put("platformCodes", String.join(",", enabledCodes));
                }
                updateMenuDisplay(menu, displayName, sortNum, node);
                return true;
            }
            if (!node.hasNonNull("platformCode")) return false;
            String displayName = targetSourceNames.get(node.get("platformCode").asText().trim().toLowerCase());
            if (displayName == null) return false;
            String code = node.get("platformCode").asText().trim().toLowerCase();
            updateMenuDisplay(menu, displayName, targetSourceSorts.get(code), node);
            return true;
        } catch (Exception ignored) { }
        return false;
    }
    private void updateMenuDisplay(SysMenu menu, String displayName, Integer sortNum) {
        try { updateMenuDisplay(menu, displayName, sortNum, objectMapper.readTree(menu.getQuery())); } catch (Exception ignored) { }
    }
    private void updateMenuDisplay(SysMenu menu, String displayName, Integer sortNum, JsonNode node) throws Exception {
        if (StringUtils.isBlank(displayName)) return;
        menu.setMenuName(displayName.trim());
        if (sortNum != null) menu.setOrderNum(sortNum);
        if (node instanceof ObjectNode objectNode) {
            objectNode.put("platformName", displayName.trim());
            menu.setQuery(objectMapper.writeValueAsString(objectNode));
        }
    }
    private String codeFromQuery(String query) {
        if (StringUtils.isBlank(query)) return null;
        try { JsonNode node = objectMapper.readTree(query); if (node.hasNonNull("platformCode")) return node.get("platformCode").asText(); if (node.hasNonNull("platformCodes")) return node.get("platformCodes").asText().split(",")[0]; } catch (Exception ignored) { }
        return null;
    }
    private void normalize(MarkPlatformMapping item, boolean create) {
        if (item == null) throw new ServiceException("映射不能为空");
        validateCode(item.getSourcePlatformCode()); validateCode(item.getTargetPlatformCode());
        item.setSourcePlatformCode(item.getSourcePlatformCode().trim()); item.setTargetPlatformCode(item.getTargetPlatformCode().trim());
        if (item.getSourcePlatformName() != null) item.setSourcePlatformName(item.getSourcePlatformName().trim());
        if (item.getTargetPlatformName() != null) item.setTargetPlatformName(item.getTargetPlatformName().trim());
        if (item.getSortNum() == null || item.getSortNum() < 0) item.setSortNum(0);
        if (StringUtils.isBlank(item.getSourceEnabled())) item.setSourceEnabled("0");
        if (StringUtils.isBlank(item.getTargetEnabled())) item.setTargetEnabled("0");
        if (StringUtils.isBlank(item.getMappingEnabled())) item.setMappingEnabled("0");
        validateEnabled(item.getSourceEnabled()); validateEnabled(item.getTargetEnabled()); validateEnabled(item.getMappingEnabled());
    }
    private void validateCode(String code) { if (StringUtils.isBlank(code)) throw new ServiceException("平台编码不能为空"); }
    private void validateId(Long id) { if (id == null) throw new ServiceException("映射ID不能为空"); }
    private void validateEnabled(String value) { if (!"0".equals(value) && !"1".equals(value)) throw new ServiceException("平台状态无效"); }
}
