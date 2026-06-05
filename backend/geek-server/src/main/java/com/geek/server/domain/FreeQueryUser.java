package com.geek.server.domain;

import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 免费查询独立用户对象 free_query_user
 */
@Schema(description = "免费查询独立用户对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class FreeQueryUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "账号")
    @Excel(name = "账号")
    private String account;

    @Schema(title = "密码（BCrypt）")
    private String password;

    @Schema(title = "昵称")
    @Excel(name = "昵称")
    private String nickName;

    @Schema(title = "手机号")
    @Excel(name = "手机号")
    private String phone;

    @Schema(title = "积分余额")
    @Excel(name = "积分余额")
    private Integer points;

    @Schema(title = "状态（0正常 1停用）")
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;
}
