package com.geek.server.service;

import com.geek.server.domain.FreeQueryPageConfig;

import java.util.List;

public interface IFreeQueryPageConfigService {
    FreeQueryPageConfig getConfig(String pageCode);

    List<FreeQueryPageConfig> listConfigs();

    int saveConfig(FreeQueryPageConfig config);
}
