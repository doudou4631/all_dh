package com.geek.server.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.geek.server.domain.UserApiQueryBatchSummary;
import com.geek.server.domain.UserApiQueryRecord;

/**
 * 接口查询记录通用Mapper接口
 * 
 * @author geek
 * @date 2026-03-09
 */
public interface UserApiQueryRecordMapper 
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
     * 查询日志聚合基础数据（按筛选条件返回轻量字段，供服务层做趋势聚合）。
     *
     * @param userApiQueryRecord 查询条件
     * @return 轻量字段列表
     */
    List<Map<String, Object>> selectFreeQueryTrendBaseList(UserApiQueryRecord userApiQueryRecord);

    /**
     * 查询记录：按 phone + 批次分组（含单条/批量等），可选 queryType 筛选；配合 PageHelper 分页
     */
    List<UserApiQueryBatchSummary> selectSingleQueryBatchGroupList(UserApiQueryRecord userApiQueryRecord);

    /**
     * 某批次下各平台完整记录（不限制 query_type）
     */
    List<UserApiQueryRecord> selectSingleQueryBatchDetailList(
        @Param("phone") String phone,
        @Param("batchKey") String batchKey,
        @Param("createBy") String createBy,
        @Param("userId") Long userId);

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
     * 删除接口查询记录通用
     * 
     * @param id 接口查询记录通用主键
     * @return 结果
     */
    public int deleteUserApiQueryRecordById(Long id);

    /**
     * 批量删除接口查询记录通用
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteUserApiQueryRecordByIds(Long[] ids);

    /**
     * 批量新增（优化版批量落库）
     */
    int insertUserApiQueryRecordBatch(@Param("list") List<UserApiQueryRecord> list);
}
