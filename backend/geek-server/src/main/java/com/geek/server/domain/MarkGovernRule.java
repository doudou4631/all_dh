package com.geek.server.domain;

import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 治理规则对象 mark_govern_rule
 */
@Schema(description = "治理规则对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class MarkGovernRule extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(title = "主键ID")
    @Excel(name = "主键ID")
    private Long id;

    @Schema(title = "规则名称")
    @Excel(name = "规则名称")
    private String ruleName;

    @Schema(title = "规则键")
    @Excel(name = "规则键")
    private String ruleKey;

    @Schema(title = "规则值")
    @Excel(name = "规则值")
    private String ruleValue;

    @Schema(title = "状态（0启用 1停用）")
    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private String status;
}
