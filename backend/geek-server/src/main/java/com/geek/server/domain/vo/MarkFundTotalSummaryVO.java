package com.geek.server.domain.vo;

import lombok.Data;

@Data
public class MarkFundTotalSummaryVO {
    private Long totalRechargeAmount;
    private Long totalDeductAmount;
    private Long totalRefundAmount;
    private Long totalTransferInAmount;
    private Long totalTransferOutAmount;
    private Long netChangeAmount;
    private Long affectedUserCount;
    private Long recordCount;
}
