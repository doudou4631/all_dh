package com.geek.server.mapper;

import java.util.List;
import com.geek.server.domain.UserAggregateConfig;

/**
 * 聚合配置Mapper接口
 * 
 * @author geek
 * @date 2026-03-09
 */
public interface UserAggregateConfigMapper 
{
    /**
     * 查询聚合配置
     * 
     * @param id 聚合配置主键
     * @return 聚合配置
     */
    public UserAggregateConfig selectUserAggregateConfigById(Long id);

    /**
     * 查询聚合配置列表
     * 
     * @param userAggregateConfig 聚合配置
     * @return 聚合配置集合
     */
    public List<UserAggregateConfig> selectUserAggregateConfigList(UserAggregateConfig userAggregateConfig);

    /**
     * 新增聚合配置
     * 
     * @param userAggregateConfig 聚合配置
     * @return 结果
     */
    public int insertUserAggregateConfig(UserAggregateConfig userAggregateConfig);

    /**
     * 修改聚合配置
     * 
     * @param userAggregateConfig 聚合配置
     * @return 结果
     */
    public int updateUserAggregateConfig(UserAggregateConfig userAggregateConfig);

    /**
     * 删除聚合配置
     * 
     * @param id 聚合配置主键
     * @return 结果
     */
    public int deleteUserAggregateConfigById(Long id);

    /**
     * 批量删除聚合配置
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteUserAggregateConfigByIds(Long[] ids);
}
