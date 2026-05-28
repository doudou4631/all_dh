package com.geek.server.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 迁移订单对象 mark_order
 */
@Schema(description = "迁移订单对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class MarkOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(title = "主键ID")
    @Excel(name = "主键ID")
    private Long id;

    @Schema(title = "订单号")
    @Excel(name = "订单号")
    private String orderNo;

    @Schema(title = "请求幂等号")
    private String requestNo;

    @Schema(title = "下单用户ID")
    @Excel(name = "下单用户ID")
    private Long userId;

    @Schema(title = "分配代理ID")
    @Excel(name = "分配代理ID")
    private Long assignedAgentId;

    @Schema(title = "平台编码")
    @Excel(name = "平台编码")
    private String platformCode;

    @Schema(title = "平台名称")
    @Excel(name = "平台名称")
    private String platformName;

    @Schema(title = "总明细数")
    @Excel(name = "总明细数")
    private Integer totalCount;

    @Schema(title = "成功数")
    @Excel(name = "成功数")
    private Integer successCount;

    @Schema(title = "失败数")
    @Excel(name = "失败数")
    private Integer failedCount;

    @Schema(title = "扣费总额")
    @Excel(name = "扣费总额")
    private Long totalAmount;

    @Schema(title = "退款总额")
    @Excel(name = "退款总额")
    private Long refundAmount;

    @Schema(title = "订单状态（0待处理 1处理中 2已完成 3已取消）")
    @Excel(name = "订单状态", readConverterExp = "0=待处理,1=处理中,2=已完成,3=已取消")
    private String orderStatus;

    @Schema(title = "完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date completedTime;

    @Schema(title = "查询号码（仅查询条件）")
    private String phone;

    @Schema(title = "号码预览")
    private String phonePreview;
}
