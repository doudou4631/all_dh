package com.geek.server.service;

import com.geek.server.domain.MobilePageConfig;

import java.util.List;
import java.util.Map;

public interface IMobilePageConfigService {

    MobilePageConfig selectMobilePageConfigById(Long id);

    List<MobilePageConfig> selectMobilePageConfigList(MobilePageConfig mobilePageConfig);

    int insertMobilePageConfig(MobilePageConfig mobilePageConfig);

    int updateMobilePageConfig(MobilePageConfig mobilePageConfig);

    int deleteMobilePageConfigByIds(Long[] ids);

    int deleteMobilePageConfigById(Long id);

    Map<String, Object> selectPublicConfig(String pageCode);
}
