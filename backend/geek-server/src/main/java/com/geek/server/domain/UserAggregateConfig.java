package com.geek.server.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;

/**
 * 聚合配置对象 user_aggregate_config
 * 
 * @author geek
 * @date 2026-03-09
 */
@Schema(description = "聚合配置对象")
public class UserAggregateConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;


    /** 主键ID */
    @Schema(title = "主键ID")
    private Long id;

    /** 聚合方式名称 */
    @Schema(title = "聚合方式名称")
    @Excel(name = "聚合方式名称")
    private String aggregateName;

    /** 通用sid */
    @Schema(title = "通用sid")
    @Excel(name = "通用sid")
    private String sid;

    /** 通用key */
    @Schema(title = "通用key")
    @Excel(name = "通用key")
    private String sKey;

    /** 其他通用配置参数 */
    @Schema(title = "其他通用配置参数")
    @Excel(name = "其他通用配置参数")
    private String configParams;

    /** 状态 */
    @Schema(title = "状态")
    @Excel(name = "状态")
    private String status;
    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }


    public void setAggregateName(String aggregateName) 
    {
        this.aggregateName = aggregateName;
    }

    public String getAggregateName() 
    {
        return aggregateName;
    }


    public void setSid(String sid) 
    {
        this.sid = sid;
    }

    public String getSid() 
    {
        return sid;
    }


    public String getsKey() {
        return sKey;
    }

    public void setsKey(String sKey) {
        this.sKey = sKey;
    }

    public void setConfigParams(String configParams)
    {
        this.configParams = configParams;
    }

    public String getConfigParams() 
    {
        return configParams;
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
            .append("aggregateName", getAggregateName())
            .append("sid", getSid())
            .append("sKey", getsKey())
            .append("configParams", getConfigParams())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
