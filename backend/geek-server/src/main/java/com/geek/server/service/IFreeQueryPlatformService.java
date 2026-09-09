package com.geek.server.service;

import com.geek.server.domain.FreeQueryPlatform;

import java.util.List;

public interface IFreeQueryPlatformService {
    List<FreeQueryPlatform> selectList();

    int updateDisplay(FreeQueryPlatform platform);

    int changeStatus(Long id, String status);

    int updateSort(List<Long> ids);
}
