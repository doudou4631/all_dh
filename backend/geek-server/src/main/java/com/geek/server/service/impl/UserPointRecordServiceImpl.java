package com.geek.server.service.impl;

import java.util.List;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.common.exception.ServiceException;
import com.geek.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.geek.server.mapper.UserPointRecordMapper;
import com.geek.server.domain.UserPointRecord;
import com.geek.server.domain.dto.QueryPointRechargeRequest;
import com.geek.server.service.IUserPointRecordService;
import com.geek.system.mapper.SysUserMapper;

/**
 * 积分流水记录Service业务层处理
 * 
 * @author geek
 * @date 2026-03-09
 */
@Service
public class UserPointRecordServiceImpl implements IUserPointRecordService 
{
    @Autowired
    private UserPointRecordMapper userPointRecordMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer rechargeQueryPoints(QueryPointRechargeRequest request, Long operatorId, String operatorName)
    {
        SysUser user = sysUserMapper.selectOneById(request.getUserId());
        if (user == null || Integer.valueOf(2).equals(user.getDelFlag()))
        {
            throw new ServiceException("用户不存在");
        }

        int updated = sysUserMapper.rechargeQueryPoints(
            request.getUserId(), request.getAmount(), operatorName);
        if (updated != 1)
        {
            throw new ServiceException("充值失败，用户查询积分已达到上限");
        }

        UserPointRecord record = new UserPointRecord();
        record.setUserId(request.getUserId());
        record.setPointAmount(request.getAmount().longValue());
        record.setPointType("1");
        record.setReason("查询积分充值");
        record.setOperatorId(operatorId);
        record.setCreateBy(operatorName);
        record.setCreateTime(DateUtils.getNowDate());
        record.setRemark(request.getRemark());
        if (userPointRecordMapper.insertUserPointRecord(record) != 1)
        {
            throw new ServiceException("查询积分流水记录失败");
        }

        SysUser updatedUser = sysUserMapper.selectOneById(request.getUserId());
        return updatedUser == null || updatedUser.getPoints() == null ? 0 : updatedUser.getPoints();
    }

    /**
     * 查询积分流水记录
     * 
     * @param id 积分流水记录主键
     * @return 积分流水记录
     */
    @Override
    public UserPointRecord selectUserPointRecordById(Long id)
    {
        return userPointRecordMapper.selectUserPointRecordById(id);
    }

    /**
     * 查询积分流水记录列表
     * 
     * @param userPointRecord 积分流水记录
     * @return 积分流水记录
     */
    @Override
    public List<UserPointRecord> selectUserPointRecordList(UserPointRecord userPointRecord)
    {
        return userPointRecordMapper.selectUserPointRecordList(userPointRecord);
    }

    /**
     * 新增积分流水记录
     * 
     * @param userPointRecord 积分流水记录
     * @return 结果
     */
    @Override
    public int insertUserPointRecord(UserPointRecord userPointRecord)
    {
        userPointRecord.setCreateTime(DateUtils.getNowDate());
        return userPointRecordMapper.insertUserPointRecord(userPointRecord);
    }

    /**
     * 修改积分流水记录
     * 
     * @param userPointRecord 积分流水记录
     * @return 结果
     */
    @Override
    public int updateUserPointRecord(UserPointRecord userPointRecord)
    {
        userPointRecord.setUpdateTime(DateUtils.getNowDate());
        return userPointRecordMapper.updateUserPointRecord(userPointRecord);
    }

    /**
     * 批量删除积分流水记录
     * 
     * @param ids 需要删除的积分流水记录主键
     * @return 结果
     */
    @Override
    public int deleteUserPointRecordByIds(Long[] ids)
    {
        return userPointRecordMapper.deleteUserPointRecordByIds(ids);
    }

    /**
     * 删除积分流水记录信息
     * 
     * @param id 积分流水记录主键
     * @return 结果
     */
    @Override
    public int deleteUserPointRecordById(Long id)
    {
        return userPointRecordMapper.deleteUserPointRecordById(id);
    }
}
