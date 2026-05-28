package com.geek.server.service.impl;

import java.util.List;
import com.geek.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.geek.server.mapper.UserPlatformUrlConfigMapper;
import com.geek.server.domain.UserPlatformUrlConfig;
import com.geek.server.service.IUserPlatformUrlConfigService;

/**
 * 查询平台url配置Service业务层处理
 * 
 * @author geek
 * @date 2026-03-09
 */
@Service
public class UserPlatformUrlConfigServiceImpl implements IUserPlatformUrlConfigService 
{
    @Autowired
    private UserPlatformUrlConfigMapper userPlatformUrlConfigMapper;

    /**
     * 查询查询平台url配置
     * 
     * @param id 查询平台url配置主键
     * @return 查询平台url配置
     */
    @Override
    public UserPlatformUrlConfig selectUserPlatformUrlConfigById(Long id)
    {
        return userPlatformUrlConfigMapper.selectUserPlatformUrlConfigById(id);
    }

    /**
     * 查询查询平台url配置列表
     * 
     * @param userPlatformUrlConfig 查询平台url配置
     * @return 查询平台url配置
     */
    @Override
    public List<UserPlatformUrlConfig> selectUserPlatformUrlConfigList(UserPlatformUrlConfig userPlatformUrlConfig)
    {
        return userPlatformUrlConfigMapper.selectUserPlatformUrlConfigList(userPlatformUrlConfig);
    }

    /**
     * 新增查询平台url配置
     * 
     * @param userPlatformUrlConfig 查询平台url配置
     * @return 结果
     */
    @Override
    public int insertUserPlatformUrlConfig(UserPlatformUrlConfig userPlatformUrlConfig)
    {
        userPlatformUrlConfig.setCreateTime(DateUtils.getNowDate());
        return userPlatformUrlConfigMapper.insertUserPlatformUrlConfig(userPlatformUrlConfig);
    }

    /**
     * 修改查询平台url配置
     * 
     * @param userPlatformUrlConfig 查询平台url配置
     * @return 结果
     */
    @Override
    public int updateUserPlatformUrlConfig(UserPlatformUrlConfig userPlatformUrlConfig)
    {
        userPlatformUrlConfig.setUpdateTime(DateUtils.getNowDate());
        return userPlatformUrlConfigMapper.updateUserPlatformUrlConfig(userPlatformUrlConfig);
    }

    /**
     * 批量删除查询平台url配置
     * 
     * @param ids 需要删除的查询平台url配置主键
     * @return 结果
     */
    @Override
    public int deleteUserPlatformUrlConfigByIds(Long[] ids)
    {
        return userPlatformUrlConfigMapper.deleteUserPlatformUrlConfigByIds(ids);
    }

    /**
     * 删除查询平台url配置信息
     * 
     * @param id 查询平台url配置主键
     * @return 结果
     */
    @Override
    public int deleteUserPlatformUrlConfigById(Long id)
    {
        return userPlatformUrlConfigMapper.deleteUserPlatformUrlConfigById(id);
    }

    @Override
    public List<UserPlatformUrlConfig> selectUserPlatformUrlUserList(UserPlatformUrlConfig userPlatformUrlConfig) {
        return userPlatformUrlConfigMapper.selectUserPlatformUrlUserList(userPlatformUrlConfig);
    }
}
