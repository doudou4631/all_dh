package com.geek.server.service.impl;

import java.util.List;
import com.geek.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.geek.server.mapper.UserPointMapper;
import com.geek.server.domain.UserPoint;
import com.geek.server.service.IUserPointService;

/**
 * 用户积分关联Service业务层处理
 * 
 * @author geek
 * @date 2026-03-09
 */
@Service
public class UserPointServiceImpl implements IUserPointService 
{
    @Autowired
    private UserPointMapper userPointMapper;

    /**
     * 查询用户积分关联
     * 
     * @param id 用户积分关联主键
     * @return 用户积分关联
     */
    @Override
    public UserPoint selectUserPointById(Long id)
    {
        return userPointMapper.selectUserPointById(id);
    }

    /**
     * 查询用户积分关联列表
     * 
     * @param userPoint 用户积分关联
     * @return 用户积分关联
     */
    @Override
    public List<UserPoint> selectUserPointList(UserPoint userPoint)
    {
        return userPointMapper.selectUserPointList(userPoint);
    }

    /**
     * 新增用户积分关联
     * 
     * @param userPoint 用户积分关联
     * @return 结果
     */
    @Override
    public int insertUserPoint(UserPoint userPoint)
    {
        userPoint.setCreateTime(DateUtils.getNowDate());
        return userPointMapper.insertUserPoint(userPoint);
    }

    /**
     * 修改用户积分关联
     * 
     * @param userPoint 用户积分关联
     * @return 结果
     */
    @Override
    public int updateUserPoint(UserPoint userPoint)
    {
        userPoint.setUpdateTime(DateUtils.getNowDate());
        return userPointMapper.updateUserPoint(userPoint);
    }

    /**
     * 批量删除用户积分关联
     * 
     * @param ids 需要删除的用户积分关联主键
     * @return 结果
     */
    @Override
    public int deleteUserPointByIds(Long[] ids)
    {
        return userPointMapper.deleteUserPointByIds(ids);
    }

    /**
     * 删除用户积分关联信息
     * 
     * @param id 用户积分关联主键
     * @return 结果
     */
    @Override
    public int deleteUserPointById(Long id)
    {
        return userPointMapper.deleteUserPointById(id);
    }
}
