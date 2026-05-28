package com.geek.server.service.impl;

import java.util.List;
import com.geek.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.geek.server.mapper.UserQueryTemplateMapper;
import com.geek.server.domain.UserQueryTemplate;
import com.geek.server.service.IUserQueryTemplateService;

/**
 * 查询模板定义Service业务层处理
 * 
 * @author geek
 * @date 2026-03-09
 */
@Service
public class UserQueryTemplateServiceImpl implements IUserQueryTemplateService 
{
    @Autowired
    private UserQueryTemplateMapper userQueryTemplateMapper;

    /**
     * 查询查询模板定义
     * 
     * @param id 查询模板定义主键
     * @return 查询模板定义
     */
    @Override
    public UserQueryTemplate selectUserQueryTemplateById(Long id)
    {
        return userQueryTemplateMapper.selectUserQueryTemplateById(id);
    }

    /**
     * 查询查询模板定义列表
     * 
     * @param userQueryTemplate 查询模板定义
     * @return 查询模板定义
     */
    @Override
    public List<UserQueryTemplate> selectUserQueryTemplateList(UserQueryTemplate userQueryTemplate)
    {
        return userQueryTemplateMapper.selectUserQueryTemplateList(userQueryTemplate);
    }

    /**
     * 新增查询模板定义
     * 
     * @param userQueryTemplate 查询模板定义
     * @return 结果
     */
    @Override
    public int insertUserQueryTemplate(UserQueryTemplate userQueryTemplate)
    {
        userQueryTemplate.setCreateTime(DateUtils.getNowDate());
        return userQueryTemplateMapper.insertUserQueryTemplate(userQueryTemplate);
    }

    /**
     * 修改查询模板定义
     * 
     * @param userQueryTemplate 查询模板定义
     * @return 结果
     */
    @Override
    public int updateUserQueryTemplate(UserQueryTemplate userQueryTemplate)
    {
        userQueryTemplate.setUpdateTime(DateUtils.getNowDate());
        return userQueryTemplateMapper.updateUserQueryTemplate(userQueryTemplate);
    }

    /**
     * 批量删除查询模板定义
     * 
     * @param ids 需要删除的查询模板定义主键
     * @return 结果
     */
    @Override
    public int deleteUserQueryTemplateByIds(Long[] ids)
    {
        return userQueryTemplateMapper.deleteUserQueryTemplateByIds(ids);
    }

    /**
     * 删除查询模板定义信息
     * 
     * @param id 查询模板定义主键
     * @return 结果
     */
    @Override
    public int deleteUserQueryTemplateById(Long id)
    {
        return userQueryTemplateMapper.deleteUserQueryTemplateById(id);
    }
}
