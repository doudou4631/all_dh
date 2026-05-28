package com.geek.server.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.geek.server.domain.UserPointRecord;

/**
 * 积分流水记录Mapper接口
 * 
 * @author geek
 * @date 2026-03-09
 */
public interface UserPointRecordMapper 
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
     * 删除积分流水记录
     * 
     * @param id 积分流水记录主键
     * @return 结果
     */
    public int deleteUserPointRecordById(Long id);

    /**
     * 批量删除积分流水记录
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteUserPointRecordByIds(Long[] ids);

    /**
     * 批量新增积分流水（优化版批量扣减）
     */
    int insertUserPointRecordBatch(@Param("list") List<UserPointRecord> list);
}
