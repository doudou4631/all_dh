package com.geek.server.service;

import com.geek.common.core.domain.entity.SysMenu;
import com.geek.server.domain.MarkPlatformMapping;
import java.util.List;
import java.util.Set;

public interface IMarkPlatformMappingService {
    List<MarkPlatformMapping> selectList(MarkPlatformMapping query);
    MarkPlatformMapping selectById(Long id);
    int save(MarkPlatformMapping item);
    int update(MarkPlatformMapping item);
    int delete(Long id);
    int updateSourceEnabled(Long id, String enabled);
    int updateTargetEnabled(Long id, String enabled);
    int updateMappingEnabled(Long id, String enabled);
    boolean isSourceEnabled(String code);
    boolean isTargetEnabled(String code);
    Set<String> selectEnabledSourceCodes();
    Set<String> selectEnabledTargetCodes();
    List<SysMenu> filterMenuTree(List<SysMenu> menus);
}
