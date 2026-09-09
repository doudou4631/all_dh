package com.geek.server.domain.vo;

import com.geek.server.domain.MarkUserPlatformPrice;
import lombok.Data;

import java.util.List;

/**
 * 钱包汇总
 */
@Data
public class MarkWalletSummaryVO {

    private Long userId;

    private Integer pointsBalance;

    private Long totalDeductAmount;

    private Long totalRefundAmount;

    private List<MarkUserPlatformPrice> platformPrices;
}
