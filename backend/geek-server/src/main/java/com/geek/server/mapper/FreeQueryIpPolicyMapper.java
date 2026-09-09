package com.geek.server.mapper;

import com.geek.server.domain.FreeQueryIpPolicy;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface FreeQueryIpPolicyMapper {
    FreeQueryIpPolicy selectConfig();

    int updateConfig(FreeQueryIpPolicy config);

    long countQueries(@Param("ipAddr") String ipAddr, @Param("windowStart") LocalDateTime windowStart);
}
