package com.geek.server.mapper;

import java.util.List;
import com.geek.server.domain.UserPlatformUrlConfig;

/**
 * 查询平台url配置Mapper接口
 * 
 * @author geek
 * @date 2026-03-09
 */
public interface UserPlatformUrlConfigMapper 
{
    /**
     * 查询查询平台url配置
     * 
     * @param id 查询平台url配置主键
     * @return 查询平台url配置
     */
    public UserPlatformUrlConfig selectUserPlatformUrlConfigById(Long id);

    /**
     * 查询查询平台url配置列表
     * 
     * @param userPlatformUrlConfig 查询平台url配置
     * @return 查询平台url配置集合
     */
    public List<UserPlatformUrlConfig> selectUserPlatformUrlConfigList(UserPlatformUrlConfig userPlatformUrlConfig);

    /**
     * 新增查询平台url配置
     * 
     * @param userPlatformUrlConfig 查询平台url配置
     * @return 结果
     */
    public int insertUserPlatformUrlConfig(UserPlatformUrlConfig userPlatformUrlConfig);

    /**
     * 修改查询平台url配置
     * 
     * @param userPlatformUrlConfig 查询平台url配置
     * @return 结果
     */
    public int updateUserPlatformUrlConfig(UserPlatformUrlConfig userPlatformUrlConfig);

    /**
     * 删除查询平台url配置
     * 
     * @param id 查询平台url配置主键
     * @return 结果
     */
    public int deleteUserPlatformUrlConfigById(Long id);

    /**
     * 批量删除查询平台url配置
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteUserPlatformUrlConfigByIds(Long[] ids);
    /**
     * 查询用户可用平台url列表
     *
     * @param userPlatformUrlConfig 查询平台url配置
     * @return 查询平台url配置集合
     */
    public List<UserPlatformUrlConfig> selectUserPlatformUrlUserList(UserPlatformUrlConfig userPlatformUrlConfig);
}
