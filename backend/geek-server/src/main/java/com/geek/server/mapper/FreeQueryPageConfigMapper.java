package com.geek.server.mapper;

import com.geek.server.domain.FreeQueryPageConfig;

import java.util.List;

public interface FreeQueryPageConfigMapper {
    FreeQueryPageConfig selectByPageCode(String pageCode);

    List<FreeQueryPageConfig> selectAll();

    int saveConfig(FreeQueryPageConfig config);
}
