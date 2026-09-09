package com.geek.server.domain.vo;

import lombok.Data;

/**
 * Agent audit statistics.
 */
@Data
public class MarkAgentAuditStatsVO {

    private Long pendingCount;

    private Long todayAuditCount;

    private Long todayPassCount;

    private Long todayRejectCount;

    private Long todayReturnCount;

    private Long totalAuditCount;

    private Long totalPassCount;

    private Double passRate;
}
