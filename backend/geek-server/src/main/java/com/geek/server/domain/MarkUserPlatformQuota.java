package com.geek.server.domain;

import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标记用户平台余额对象 mark_user_platform_quota
 */
@Schema(description = "标记用户平台余额对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class MarkUserPlatformQuota extends BaseEntity {

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

    @Schema(title = "剩余次数")
    @Excel(name = "剩余次数")
    private Long remainCount;
}
