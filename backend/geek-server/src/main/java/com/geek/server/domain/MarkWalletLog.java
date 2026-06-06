package com.geek.server.domain;

import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 迁移钱包流水对象 mark_wallet_log
 */
@Schema(description = "迁移钱包流水对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class MarkWalletLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(title = "主键ID")
    @Excel(name = "主键ID")
    private Long id;

    @Schema(title = "用户ID")
    @Excel(name = "用户ID")
    private Long userId;

    @Schema(title = "订单ID")
    @Excel(name = "订单ID")
    private Long orderId;

    @Schema(title = "订单明细ID")
    @Excel(name = "订单明细ID")
    private Long orderItemId;
    @Schema(title = "平台编码")
    @Excel(name = "平台编码")
    private String platformCode;

    @Schema(title = "平台名称")
    @Excel(name = "平台名称")
    private String platformName;

    @Schema(title = "流水业务类型（DEDUCT/REFUND/ADJUST）")
    @Excel(name = "流水业务类型")
    private String bizType;

    @Schema(title = "变动金额（正负）")
    @Excel(name = "变动金额")
    private Long changeAmount;

    @Schema(title = "变动前余额")
    @Excel(name = "变动前余额")
    private Long balanceBefore;

    @Schema(title = "变动后余额")
    @Excel(name = "变动后余额")
    private Long balanceAfter;
}
