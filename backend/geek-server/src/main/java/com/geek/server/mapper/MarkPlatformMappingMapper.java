package com.geek.server.mapper;

import com.geek.server.domain.MarkPlatformMapping;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface MarkPlatformMappingMapper {
    List<MarkPlatformMapping> selectList(MarkPlatformMapping query);
    MarkPlatformMapping selectById(Long id);
    int insert(MarkPlatformMapping item);
    int update(MarkPlatformMapping item);
    int deleteById(Long id);
    int updateSourceEnabled(@Param("id") Long id, @Param("enabled") String enabled);
    int updateTargetEnabled(@Param("id") Long id, @Param("enabled") String enabled);
    int updateSourceName(@Param("code") String code, @Param("name") String name);
    int updateTargetName(@Param("code") String code, @Param("name") String name);
    int updateSourceSort(@Param("code") String code, @Param("sortNum") Integer sortNum);
    int updateMappingEnabled(@Param("id") Long id, @Param("enabled") String enabled);
}
