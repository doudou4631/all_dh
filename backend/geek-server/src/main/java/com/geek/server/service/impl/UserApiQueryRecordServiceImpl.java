package com.geek.server.service.impl;

import java.util.List;

import com.geek.common.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.geek.server.domain.UserApiQueryBatchSummary;
import com.geek.server.domain.UserApiQueryRecord;
import com.geek.server.mapper.UserApiQueryRecordMapper;
import com.geek.server.service.IUserApiQueryRecordService;

/**
 * 接口查询记录通用Service业务层处理
 * 
 * @author geek
 * @date 2026-03-09
 */
@Service
public class UserApiQueryRecordServiceImpl implements IUserApiQueryRecordService 
{
    @Autowired
    private UserApiQueryRecordMapper userApiQueryRecordMapper;

    /**
     * 查询接口查询记录通用
     * 
     * @param id 接口查询记录通用主键
     * @return 接口查询记录通用
     */
    @Override
    public UserApiQueryRecord selectUserApiQueryRecordById(Long id)
    {
        return userApiQueryRecordMapper.selectUserApiQueryRecordById(id);
    }

    /**
     * 查询接口查询记录通用列表
     * 
     * @param userApiQueryRecord 接口查询记录通用
     * @return 接口查询记录通用
     */
    @Override
    public List<UserApiQueryRecord> selectUserApiQueryRecordList(UserApiQueryRecord userApiQueryRecord)
    {
        List<UserApiQueryRecord> list = userApiQueryRecordMapper.selectUserApiQueryRecordList(userApiQueryRecord);
        if (list != null)
        {
            for (UserApiQueryRecord row : list)
            {
                row.setResults(convertResults(row.getPlatformName(), row.getResults()));
            }
        }
        return list;
    }

    /**
     * 统一转换结果文案，不对外暴露 yes/no 原始值。
     */
    private String convertResults(String platformName, String rawResults)
    {
        if (StringUtils.isBlank(rawResults))
        {
            return rawResults;
        }

        String normalized = rawResults.trim().toLowerCase();
        if (normalized.contains("no"))
        {
            return "无标记";
        }

        if ("yes".equals(normalized))
        {
            if ("移动高频".equals(platformName))
            {
                return "移动高频拦截";
            }
            return "有标记";
        }

        if (!"移动高频".equals(platformName) && normalized.contains("yes"))
        {
            String cleaned = rawResults
                .replace("yes-", "")
                .replace("YES-", "")
                .replace("Yes-", "")
                .replace("yes", "")
                .replace("YES", "")
                .replace("Yes", "");
            return cleaned.trim();
        }

        return rawResults;
    }

    @Override
    public List<UserApiQueryBatchSummary> selectSingleQueryBatchGroupList(UserApiQueryRecord userApiQueryRecord)
    {
        List<UserApiQueryBatchSummary> list = userApiQueryRecordMapper.selectSingleQueryBatchGroupList(userApiQueryRecord);
        if (list != null)
        {
            for (UserApiQueryBatchSummary row : list)
            {
                int m = row.getMarkedPlatformCount() == null ? 0 : row.getMarkedPlatformCount();
                row.setResultSummary(m <= 0 ? "全部无标记" : (m + "个平台有标记"));
            }
        }
        return list;
    }

    @Override
    public List<UserApiQueryRecord> selectSingleQueryBatchDetailList(String phone, String batchKey, String createBy, Long userId)
    {
        return userApiQueryRecordMapper.selectSingleQueryBatchDetailList(phone, batchKey, createBy, userId);
    }

    /**
     * 新增接口查询记录通用
     * 
     * @param userApiQueryRecord 接口查询记录通用
     * @return 结果
     */
    @Override
    public int insertUserApiQueryRecord(UserApiQueryRecord userApiQueryRecord)
    {
        userApiQueryRecord.setCreateTime(DateUtils.getNowDate());
        return userApiQueryRecordMapper.insertUserApiQueryRecord(userApiQueryRecord);
    }

    /**
     * 修改接口查询记录通用
     * 
     * @param userApiQueryRecord 接口查询记录通用
     * @return 结果
     */
    @Override
    public int updateUserApiQueryRecord(UserApiQueryRecord userApiQueryRecord)
    {
        userApiQueryRecord.setUpdateTime(DateUtils.getNowDate());
        return userApiQueryRecordMapper.updateUserApiQueryRecord(userApiQueryRecord);
    }

    /**
     * 批量删除接口查询记录通用
     * 
     * @param ids 需要删除的接口查询记录通用主键
     * @return 结果
     */
    @Override
    public int deleteUserApiQueryRecordByIds(Long[] ids)
    {
        return userApiQueryRecordMapper.deleteUserApiQueryRecordByIds(ids);
    }

    /**
     * 删除接口查询记录通用信息
     * 
     * @param id 接口查询记录通用主键
     * @return 结果
     */
    @Override
    public int deleteUserApiQueryRecordById(Long id)
    {
        return userApiQueryRecordMapper.deleteUserApiQueryRecordById(id);
    }
}
