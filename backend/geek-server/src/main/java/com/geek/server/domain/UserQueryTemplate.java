package com.geek.server.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;

/**
 * 查询模板定义对象 user_query_template
 * 
 * @author geek
 * @date 2026-03-09
 */
@Schema(description = "查询模板定义对象")
public class UserQueryTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;


    /** 主键ID */
    @Schema(title = "主键ID")
    private Long id;

    /** 模板名称 */
    @Schema(title = "模板名称")
    @Excel(name = "模板名称")
    private String templateName;

    /** 等待延时（毫秒） */
    @Schema(title = "等待延时（毫秒）")
    @Excel(name = "等待延时", readConverterExp = "毫=秒")
    private Long waitDelay;

    /** 模板关联信息，包含平台ID，平台名称，是否启用等 */
    @Schema(title = "模板关联信息，包含平台ID，平台名称，是否启用等")
    @Excel(name = "模板关联信息，包含平台ID，平台名称，是否启用等")
    private String templateInfo;

    /** 状态（0正常 1停用） */
    @Schema(title = "状态（0正常 1停用）")
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;
    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }


    public void setTemplateName(String templateName) 
    {
        this.templateName = templateName;
    }

    public String getTemplateName() 
    {
        return templateName;
    }


    public void setWaitDelay(Long waitDelay) 
    {
        this.waitDelay = waitDelay;
    }

    public Long getWaitDelay() 
    {
        return waitDelay;
    }


    public void setTemplateInfo(String templateInfo) 
    {
        this.templateInfo = templateInfo;
    }

    public String getTemplateInfo() 
    {
        return templateInfo;
    }


    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }



    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("templateName", getTemplateName())
            .append("waitDelay", getWaitDelay())
            .append("templateInfo", getTemplateInfo())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
