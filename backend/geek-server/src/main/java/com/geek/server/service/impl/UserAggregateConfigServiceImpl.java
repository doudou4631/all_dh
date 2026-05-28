package com.geek.server.service.impl;

import java.util.List;
import com.geek.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.geek.server.mapper.UserAggregateConfigMapper;
import com.geek.server.domain.UserAggregateConfig;
import com.geek.server.service.IUserAggregateConfigService;

/**
 * 聚合配置Service业务层处理
 * 
 * @author geek
 * @date 2026-03-09
 */
@Service
public class UserAggregateConfigServiceImpl implements IUserAggregateConfigService 
{
    @Autowired
    private UserAggregateConfigMapper userAggregateConfigMapper;

    /**
     * 查询聚合配置
     * 
     * @param id 聚合配置主键
     * @return 聚合配置
     */
    @Override
    public UserAggregateConfig selectUserAggregateConfigById(Long id)
    {
        return userAggregateConfigMapper.selectUserAggregateConfigById(id);
    }

    /**
     * 查询聚合配置列表
     * 
     * @param userAggregateConfig 聚合配置
     * @return 聚合配置
     */
    @Override
    public List<UserAggregateConfig> selectUserAggregateConfigList(UserAggregateConfig userAggregateConfig)
    {
        return userAggregateConfigMapper.selectUserAggregateConfigList(userAggregateConfig);
    }

    /**
     * 新增聚合配置
     * 
     * @param userAggregateConfig 聚合配置
     * @return 结果
     */
    @Override
    public int insertUserAggregateConfig(UserAggregateConfig userAggregateConfig)
    {
        userAggregateConfig.setCreateTime(DateUtils.getNowDate());
        return userAggregateConfigMapper.insertUserAggregateConfig(userAggregateConfig);
    }

    /**
     * 修改聚合配置
     * 
     * @param userAggregateConfig 聚合配置
     * @return 结果
     */
    @Override
    public int updateUserAggregateConfig(UserAggregateConfig userAggregateConfig)
    {
        userAggregateConfig.setUpdateTime(DateUtils.getNowDate());
        return userAggregateConfigMapper.updateUserAggregateConfig(userAggregateConfig);
    }

    /**
     * 批量删除聚合配置
     * 
     * @param ids 需要删除的聚合配置主键
     * @return 结果
     */
    @Override
    public int deleteUserAggregateConfigByIds(Long[] ids)
    {
        return userAggregateConfigMapper.deleteUserAggregateConfigByIds(ids);
    }

    /**
     * 删除聚合配置信息
     * 
     * @param id 聚合配置主键
     * @return 结果
     */
    @Override
    public int deleteUserAggregateConfigById(Long id)
    {
        return userAggregateConfigMapper.deleteUserAggregateConfigById(id);
    }
}
