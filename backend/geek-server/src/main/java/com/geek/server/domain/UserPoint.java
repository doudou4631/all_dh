package com.geek.server.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;

/**
 * 用户积分关联对象 user_point
 * 
 * @author geek
 * @date 2026-03-09
 */
@Schema(description = "用户积分关联对象")
public class UserPoint extends BaseEntity
{
    private static final long serialVersionUID = 1L;


    /** 主键ID */
    @Schema(title = "主键ID")
    private Long id;

    /** 用户ID */
    @Schema(title = "用户ID")
    @Excel(name = "用户ID")
    private Long userId;

    /** 积分余额 */
    @Schema(title = "积分余额")
    @Excel(name = "积分余额")
    private Long pointBalance;
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


    public void setPointBalance(Long pointBalance) 
    {
        this.pointBalance = pointBalance;
    }

    public Long getPointBalance() 
    {
        return pointBalance;
    }



    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("pointBalance", getPointBalance())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
