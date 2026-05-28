package com.geek.server.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;

/**
 * 积分流水记录对象 user_point_record
 * 
 * @author geek
 * @date 2026-03-09
 */
@Schema(description = "积分流水记录对象")
public class UserPointRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;


    /** 主键ID */
    @Schema(title = "主键ID")
    private Long id;

    /** 用户ID */
    @Schema(title = "用户ID")
    @Excel(name = "用户ID")
    private Long userId;

    /** 积分变动金额 */
    @Schema(title = "积分变动金额")
    @Excel(name = "积分变动金额")
    private Long pointAmount;

    /** 变动类型（1充值 2扣减） */
    @Schema(title = "变动类型（1充值 2扣减）")
    @Excel(name = "变动类型", readConverterExp = "1=充值,2=扣减")
    private String pointType;

    /** 变动原因 */
    @Schema(title = "变动原因")
    @Excel(name = "变动原因")
    private String reason;

    /** 操作人ID */
    @Schema(title = "操作人ID")
    @Excel(name = "操作人ID")
    private Long operatorId;
    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }


    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }


    public void setPointAmount(Long pointAmount) 
    {
        this.pointAmount = pointAmount;
    }

    public Long getPointAmount() 
    {
        return pointAmount;
    }


    public void setPointType(String pointType) 
    {
        this.pointType = pointType;
    }

    public String getPointType() 
    {
        return pointType;
    }


    public void setReason(String reason) 
    {
        this.reason = reason;
    }

    public String getReason() 
    {
        return reason;
    }


    public void setOperatorId(Long operatorId) 
    {
        this.operatorId = operatorId;
    }

    public Long getOperatorId() 
    {
        return operatorId;
    }



    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("pointAmount", getPointAmount())
            .append("pointType", getPointType())
            .append("reason", getReason())
            .append("operatorId", getOperatorId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
