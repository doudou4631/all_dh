package com.geek.server.mapper;

import com.geek.server.domain.MarkUserPlatformPrice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户平台单价 Mapper
 */
public interface MarkUserPlatformPriceMapper {

    List<MarkUserPlatformPrice> selectMarkUserPlatformPriceList(MarkUserPlatformPrice markUserPlatformPrice);

    MarkUserPlatformPrice selectByUserAndPlatform(@Param("userId") Long userId, @Param("platformCode") String platformCode);

    int insertMarkUserPlatformPrice(MarkUserPlatformPrice markUserPlatformPrice);

    int updateMarkUserPlatformPrice(MarkUserPlatformPrice markUserPlatformPrice);
}
