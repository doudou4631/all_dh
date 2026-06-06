package com.geek.server.mapper;

import com.geek.server.domain.MarkUserPlatformQuota;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 标记用户平台余额 Mapper
 */
public interface MarkUserPlatformQuotaMapper {

    List<MarkUserPlatformQuota> selectByUserId(@Param("userId") Long userId);

    MarkUserPlatformQuota selectByUserAndPlatform(@Param("userId") Long userId, @Param("platformCode") String platformCode);

    MarkUserPlatformQuota selectByUserAndPlatformForUpdate(@Param("userId") Long userId, @Param("platformCode") String platformCode);

    int insertMarkUserPlatformQuota(MarkUserPlatformQuota markUserPlatformQuota);

    int updateMarkUserPlatformQuota(MarkUserPlatformQuota markUserPlatformQuota);
}
