package com.geek.server.service;

import com.geek.server.domain.FreeQueryScheduleConfig;

import java.util.List;

public interface IFreeQueryScheduleConfigService {
    FreeQueryScheduleConfig selectById(Long id);

    List<FreeQueryScheduleConfig> selectList(FreeQueryScheduleConfig config);

    List<FreeQueryScheduleConfig> selectEnabledList();

    String validate(FreeQueryScheduleConfig config);

    int insert(FreeQueryScheduleConfig config);

    int update(FreeQueryScheduleConfig config);

    int changeStatus(FreeQueryScheduleConfig config);

    int deleteByIds(Long[] ids);
}
