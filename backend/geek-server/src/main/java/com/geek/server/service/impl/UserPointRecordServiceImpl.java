package com.geek.server.service.impl;

import java.util.List;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.common.exception.ServiceException;
import com.geek.common.utils.DateUtils;
import com.geek.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.geek.server.mapper.UserPointRecordMapper;
import com.geek.server.domain.UserPointRecord;
import com.geek.server.service.IUserPointRecordService;
import com.geek.system.service.ISysUserService;

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
    private ISysUserService sysUserService;

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

    /**
     * 调整用户积分并记录流水（同一事务）
     *
     * @param userPointRecord 积分流水
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int adjustUserPoints(UserPointRecord userPointRecord)
    {
        if (userPointRecord == null || userPointRecord.getUserId() == null)
        {
            throw new ServiceException("用户ID不能为空");
        }
        if (userPointRecord.getPointAmount() == null || userPointRecord.getPointAmount() <= 0)
        {
            throw new ServiceException("积分值必须大于0");
        }
        if (!"1".equals(userPointRecord.getPointType()) && !"2".equals(userPointRecord.getPointType()))
        {
            throw new ServiceException("积分变动类型不合法");
        }

        // 校验当前操作人是否有目标用户数据权限（含代理仅可操作自己创建用户）
        sysUserService.checkUserDataScope(userPointRecord.getUserId());

        SysUser targetUser = sysUserService.selectUserById(userPointRecord.getUserId());
        if (targetUser == null)
        {
            throw new ServiceException("用户不存在");
        }

        int currentPoints = targetUser.getPoints() == null ? 0 : targetUser.getPoints();
        int delta = Math.toIntExact(userPointRecord.getPointAmount());
        int nextPoints = "1".equals(userPointRecord.getPointType()) ? currentPoints + delta : currentPoints - delta;
        if (nextPoints < 0)
        {
            throw new ServiceException("用户积分不足，无法扣减");
        }

        // 先更新用户积分
        SysUser patch = new SysUser();
        patch.setUserId(targetUser.getUserId());
        patch.setPoints(nextPoints);
        patch.setUpdateBy(userPointRecord.getCreateBy());
        patch.setUpdateTime(DateUtils.getNowDate());
        boolean updated = sysUserService.updateUserProfile(patch);
        if (!updated)
        {
            throw new ServiceException("更新用户积分失败");
        }

        // 再记录积分流水
        if (StringUtils.isEmpty(userPointRecord.getReason()))
        {
            userPointRecord.setReason("1".equals(userPointRecord.getPointType()) ? "充值" : "扣减");
        }
        userPointRecord.setCreateTime(DateUtils.getNowDate());
        return userPointRecordMapper.insertUserPointRecord(userPointRecord);
    }
}
