package com.geek.server.service;

import com.geek.server.domain.MarkPlatformTemplate;

import java.util.List;
import java.util.Map;

/**
 * 标记平台模板服务
 */
public interface IMarkPlatformTemplateService {

    MarkPlatformTemplate selectMarkPlatformTemplateById(Long id);
    MarkPlatformTemplate selectOwnerDefaultTemplate(Long ownerUserId);

    List<MarkPlatformTemplate> selectMarkPlatformTemplateList(MarkPlatformTemplate query);
    List<Map<String, Object>> selectPlatformOptions();

    int insertMarkPlatformTemplate(MarkPlatformTemplate markPlatformTemplate);

    int updateMarkPlatformTemplate(MarkPlatformTemplate markPlatformTemplate);

    int deleteMarkPlatformTemplateByIds(Long[] ids);
}
