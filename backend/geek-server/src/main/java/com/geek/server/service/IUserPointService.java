package com.geek.server.service;

import java.util.List;
import com.geek.server.domain.UserPoint;

/**
 * 用户积分关联Service接口
 * 
 * @author geek
 * @date 2026-03-09
 */
public interface IUserPointService 
{
    /**
     * 查询用户积分关联
     * 
     * @param id 用户积分关联主键
     * @return 用户积分关联
     */
    public UserPoint selectUserPointById(Long id);

    /**
     * 查询用户积分关联列表
     * 
     * @param userPoint 用户积分关联
     * @return 用户积分关联集合
     */
    public List<UserPoint> selectUserPointList(UserPoint userPoint);

    /**
     * 新增用户积分关联
     * 
     * @param userPoint 用户积分关联
     * @return 结果
     */
    public int insertUserPoint(UserPoint userPoint);

    /**
     * 修改用户积分关联
     * 
     * @param userPoint 用户积分关联
     * @return 结果
     */
    public int updateUserPoint(UserPoint userPoint);

    /**
     * 批量删除用户积分关联
     * 
     * @param ids 需要删除的用户积分关联主键集合
     * @return 结果
     */
    public int deleteUserPointByIds(Long[] ids);

    /**
     * 删除用户积分关联信息
     * 
     * @param id 用户积分关联主键
     * @return 结果
     */
    public int deleteUserPointById(Long id);
}
