package com.geek.server.service;

import java.util.List;
import java.util.Map;

import com.geek.server.domain.UserApiQueryBatchSummary;
import com.geek.server.domain.UserApiQueryRecord;

/**
 * 接口查询记录通用Service接口
 * 
 * @author geek
 * @date 2026-03-09
 */
public interface IUserApiQueryRecordService 
{
    /**
     * 查询接口查询记录通用
     * 
     * @param id 接口查询记录通用主键
     * @return 接口查询记录通用
     */
    public UserApiQueryRecord selectUserApiQueryRecordById(Long id);

    /**
     * 查询接口查询记录通用列表
     * 
     * @param userApiQueryRecord 接口查询记录通用
     * @return 接口查询记录通用集合
     */
    public List<UserApiQueryRecord> selectUserApiQueryRecordList(UserApiQueryRecord userApiQueryRecord);

    /**
     * 查询日志聚合基础数据（轻量字段）。
     *
     * @param userApiQueryRecord 查询条件
     * @return 轻量字段列表
     */
    List<Map<String, Object>> selectFreeQueryTrendBaseList(UserApiQueryRecord userApiQueryRecord);

    /**
     * 查询记录分组列表（phone + 批次，含单条/批量），分页由 Controller startPage 控制
     */
    List<UserApiQueryBatchSummary> selectSingleQueryBatchGroupList(UserApiQueryRecord userApiQueryRecord);

    /**
     * 某批次全部平台记录
     */
    List<UserApiQueryRecord> selectSingleQueryBatchDetailList(String phone, String batchKey, String createBy, Long userId);

    /**
     * 新增接口查询记录通用
     * 
     * @param userApiQueryRecord 接口查询记录通用
     * @return 结果
     */
    public int insertUserApiQueryRecord(UserApiQueryRecord userApiQueryRecord);

    /**
     * 修改接口查询记录通用
     * 
     * @param userApiQueryRecord 接口查询记录通用
     * @return 结果
     */
    public int updateUserApiQueryRecord(UserApiQueryRecord userApiQueryRecord);

    /**
     * 批量删除接口查询记录通用
     * 
     * @param ids 需要删除的接口查询记录通用主键集合
     * @return 结果
     */
    public int deleteUserApiQueryRecordByIds(Long[] ids);

    /**
     * 删除接口查询记录通用信息
     * 
     * @param id 接口查询记录通用主键
     * @return 结果
     */
    public int deleteUserApiQueryRecordById(Long id);
}
