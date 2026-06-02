package com.geek.server.mapper;

import com.geek.server.domain.MarkPlatformTemplate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 标记平台模板 Mapper
 */
public interface MarkPlatformTemplateMapper {

    MarkPlatformTemplate selectMarkPlatformTemplateById(Long id);
    MarkPlatformTemplate selectOwnerDefaultTemplate(@Param("ownerUserId") Long ownerUserId);

    List<MarkPlatformTemplate> selectMarkPlatformTemplateList(MarkPlatformTemplate markPlatformTemplate);

    int insertMarkPlatformTemplate(MarkPlatformTemplate markPlatformTemplate);

    int updateMarkPlatformTemplate(MarkPlatformTemplate markPlatformTemplate);
    int clearOwnerDefaultTemplate(@Param("ownerUserId") Long ownerUserId, @Param("excludeId") Long excludeId);

    int deleteMarkPlatformTemplateByIds(Long[] ids);
}
