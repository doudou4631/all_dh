package com.geek.server.mapper;

import com.geek.server.domain.MarkPlatformTemplate;

import java.util.List;

/**
 * 标记平台模板 Mapper
 */
public interface MarkPlatformTemplateMapper {

    MarkPlatformTemplate selectMarkPlatformTemplateById(Long id);

    List<MarkPlatformTemplate> selectMarkPlatformTemplateList(MarkPlatformTemplate markPlatformTemplate);

    int insertMarkPlatformTemplate(MarkPlatformTemplate markPlatformTemplate);

    int updateMarkPlatformTemplate(MarkPlatformTemplate markPlatformTemplate);

    int deleteMarkPlatformTemplateByIds(Long[] ids);
}
