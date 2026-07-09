package com.geek.server.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 迁移订单明细对象 mark_order_item
 */
@Schema(description = "迁移订单明细对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class MarkOrderItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(title = "主键ID")
    @Excel(name = "主键ID")
    private Long id;

    @Schema(title = "订单ID")
    @Excel(name = "订单ID")
    private Long orderId;

    @Schema(title = "号码")
    @Excel(name = "号码")
    private String phone;

    @Schema(title = "单价")
    @Excel(name = "单价")
    private Long unitPrice;

    @Schema(title = "明细金额")
    @Excel(name = "明细金额")
    private Long itemAmount;

    @Schema(title = "处理状态（0待处理 1处理完成 2处理失败 3处理中/已手动提交）")
    @Excel(name = "处理状态", readConverterExp = "0=待处理,1=处理完成,2=处理失败,3=处理中")
    private String processStatus;

    @Schema(title = "处理结果")
    @Excel(name = "处理结果")
    private String processResult;

    @Schema(title = "处理备注")
    @Excel(name = "处理备注")
    private String processNote;

    @Schema(title = "处理人")
    @Excel(name = "处理人")
    private String processedBy;

    @Schema(title = "处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date processedTime;

    @Schema(title = "是否已退款（0否 1是）")
    @Excel(name = "是否已退款", readConverterExp = "0=否,1=是")
    private String refunded;
}
