package com.geek.server.service;

import java.util.List;
import com.geek.server.domain.UserPointRecord;

/**
 * 积分流水记录Service接口
 * 
 * @author geek
 * @date 2026-03-09
 */
public interface IUserPointRecordService 
{
    /**
     * 查询积分流水记录
     * 
     * @param id 积分流水记录主键
     * @return 积分流水记录
     */
    public UserPointRecord selectUserPointRecordById(Long id);

    /**
     * 查询积分流水记录列表
     * 
     * @param userPointRecord 积分流水记录
     * @return 积分流水记录集合
     */
    public List<UserPointRecord> selectUserPointRecordList(UserPointRecord userPointRecord);

    /**
     * 新增积分流水记录
     * 
     * @param userPointRecord 积分流水记录
     * @return 结果
     */
    public int insertUserPointRecord(UserPointRecord userPointRecord);

    /**
     * 修改积分流水记录
     * 
     * @param userPointRecord 积分流水记录
     * @return 结果
     */
    public int updateUserPointRecord(UserPointRecord userPointRecord);

    /**
     * 批量删除积分流水记录
     * 
     * @param ids 需要删除的积分流水记录主键集合
     * @return 结果
     */
    public int deleteUserPointRecordByIds(Long[] ids);

    /**
     * 删除积分流水记录信息
     * 
     * @param id 积分流水记录主键
     * @return 结果
     */
    public int deleteUserPointRecordById(Long id);

    /**
     * 调整用户积分并记录流水（同一事务）
     *
     * @param userPointRecord 积分流水记录
     * @return 结果
     */
    public int adjustUserPoints(UserPointRecord userPointRecord);
}
