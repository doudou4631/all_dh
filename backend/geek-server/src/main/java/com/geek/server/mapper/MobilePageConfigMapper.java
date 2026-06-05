package com.geek.server.mapper;

import com.geek.server.domain.MobilePageConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MobilePageConfigMapper {

    MobilePageConfig selectMobilePageConfigById(Long id);

    MobilePageConfig selectMobilePageConfigByPageCode(@Param("pageCode") String pageCode);

    List<MobilePageConfig> selectMobilePageConfigList(MobilePageConfig mobilePageConfig);

    int insertMobilePageConfig(MobilePageConfig mobilePageConfig);

    int updateMobilePageConfig(MobilePageConfig mobilePageConfig);

    int deleteMobilePageConfigById(Long id);

    int deleteMobilePageConfigByIds(@Param("ids") Long[] ids);

    MobilePageConfig selectPublicConfigByPageCode(@Param("pageCode") String pageCode);

    MobilePageConfig selectFirstPublicConfig();
}
