package com.geek.server.domain;

import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户平台单价对象 mark_user_platform_price
 */
@Schema(description = "用户平台单价对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class MarkUserPlatformPrice extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(title = "主键ID")
    @Excel(name = "主键ID")
    private Long id;

    @Schema(title = "用户ID")
    @Excel(name = "用户ID")
    private Long userId;

    @Schema(title = "平台编码")
    @Excel(name = "平台编码")
    private String platformCode;

    @Schema(title = "平台名称")
    @Excel(name = "平台名称")
    private String platformName;

    @Schema(title = "单价（每号码消耗积分）")
    @Excel(name = "单价")
    private Long unitPrice;

    @Schema(title = "剩余次数（按平台）")
    @Excel(name = "剩余次数")
    private Long remainCount;

    @Schema(title = "状态（0开启 1关闭）")
    @Excel(name = "状态")
    private String status;
}
