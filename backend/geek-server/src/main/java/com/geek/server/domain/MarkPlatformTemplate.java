package com.geek.server.domain;

import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标记平台模板对象 mark_platform_template
 */
@Schema(description = "标记平台模板对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class MarkPlatformTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(title = "主键ID")
    @Excel(name = "主键ID")
    private Long id;

    @Schema(title = "模板名称")
    @Excel(name = "模板名称")
    private String templateName;

    @Schema(title = "模板信息（平台编码数组JSON）")
    @Excel(name = "模板信息")
    private String templateInfo;

    @Schema(title = "状态（0正常 1停用）")
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;
    @Schema(title = "是否默认模板（0否 1是）")
    @Excel(name = "默认模板", readConverterExp = "0=否,1=是")
    private String isDefault;

    @Schema(title = "模板归属用户ID")
    private Long ownerUserId;
}
