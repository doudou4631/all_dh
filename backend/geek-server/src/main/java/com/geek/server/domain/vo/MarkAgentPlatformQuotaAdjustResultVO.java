package com.geek.server.domain.vo;

import lombok.Data;

/**
 * 标记代理平台余额调整结果
 */
@Data
public class MarkAgentPlatformQuotaAdjustResultVO {

    private Long userId;

    private String platformCode;

    private String platformName;

    private String adjustType;

    private Long changeCount;

    private Long balanceBefore;

    private Long balanceAfter;

    private Long remainCount;
}
