package com.geek.server.mapper;

import com.geek.server.domain.FreeQueryScheduleConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FreeQueryScheduleConfigMapper {
    FreeQueryScheduleConfig selectById(Long id);

    List<FreeQueryScheduleConfig> selectList(FreeQueryScheduleConfig config);

    List<FreeQueryScheduleConfig> selectEnabledList();

    int countByTaskName(@Param("taskName") String taskName, @Param("excludeId") Long excludeId);

    int insert(FreeQueryScheduleConfig config);

    int update(FreeQueryScheduleConfig config);

    int changeStatus(FreeQueryScheduleConfig config);

    int deleteByIds(Long[] ids);
}
