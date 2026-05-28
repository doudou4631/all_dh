package com.geek.server.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 仲裁工单对象 mark_arbitration_case
 */
@Schema(description = "仲裁工单对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class MarkArbitrationCase extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(title = "主键ID")
    @Excel(name = "主键ID")
    private Long id;

    @Schema(title = "订单ID")
    @Excel(name = "订单ID")
    private Long orderId;

    @Schema(title = "订单明细ID")
    @Excel(name = "订单明细ID")
    private Long orderItemId;

    @Schema(title = "用户ID")
    @Excel(name = "用户ID")
    private Long userId;

    @Schema(title = "代理ID")
    @Excel(name = "代理ID")
    private Long agentId;

    @Schema(title = "仲裁状态（0待处理 1已裁决 2已驳回）")
    @Excel(name = "仲裁状态", readConverterExp = "0=待处理,1=已裁决,2=已驳回")
    private String caseStatus;

    @Schema(title = "问题描述")
    @Excel(name = "问题描述")
    private String issueDesc;

    @Schema(title = "证据内容")
    @Excel(name = "证据内容")
    private String evidenceText;

    @Schema(title = "裁决内容")
    @Excel(name = "裁决内容")
    private String decisionText;

    @Schema(title = "裁决人")
    @Excel(name = "裁决人")
    private String decidedBy;

    @Schema(title = "裁决时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date decidedTime;
}
