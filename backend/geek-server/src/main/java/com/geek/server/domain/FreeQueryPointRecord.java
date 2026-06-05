package com.geek.server.domain;

import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 手机端用户积分流水对象 free_query_point_record
 */
@Schema(description = "手机端用户积分流水对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class FreeQueryPointRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "手机端用户ID")
    private Long freeUserId;

    @Schema(title = "积分变动值")
    private Integer pointAmount;

    @Schema(title = "积分变动类型（1增加 2扣减）")
    private String pointType;

    @Schema(title = "业务类型")
    private String businessType;

    @Schema(title = "业务单号")
    private String bizNo;

    @Schema(title = "变动原因")
    private String reason;

    @Schema(title = "操作人ID")
    private Long operatorId;

    @Schema(title = "变动后余额")
    private Integer balanceAfter;
}
