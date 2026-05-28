package com.geek.server.service;

import java.util.List;
import com.geek.server.domain.UserQueryTemplate;

/**
 * 查询模板定义Service接口
 * 
 * @author geek
 * @date 2026-03-09
 */
public interface IUserQueryTemplateService 
{
    /**
     * 查询查询模板定义
     * 
     * @param id 查询模板定义主键
     * @return 查询模板定义
     */
    public UserQueryTemplate selectUserQueryTemplateById(Long id);

    /**
     * 查询查询模板定义列表
     * 
     * @param userQueryTemplate 查询模板定义
     * @return 查询模板定义集合
     */
    public List<UserQueryTemplate> selectUserQueryTemplateList(UserQueryTemplate userQueryTemplate);

    /**
     * 新增查询模板定义
     * 
     * @param userQueryTemplate 查询模板定义
     * @return 结果
     */
    public int insertUserQueryTemplate(UserQueryTemplate userQueryTemplate);

    /**
     * 修改查询模板定义
     * 
     * @param userQueryTemplate 查询模板定义
     * @return 结果
     */
    public int updateUserQueryTemplate(UserQueryTemplate userQueryTemplate);

    /**
     * 批量删除查询模板定义
     * 
     * @param ids 需要删除的查询模板定义主键集合
     * @return 结果
     */
    public int deleteUserQueryTemplateByIds(Long[] ids);

    /**
     * 删除查询模板定义信息
     * 
     * @param id 查询模板定义主键
     * @return 结果
     */
    public int deleteUserQueryTemplateById(Long id);
}
