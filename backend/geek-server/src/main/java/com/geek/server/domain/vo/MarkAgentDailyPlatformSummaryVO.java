package com.geek.server.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Per-platform counts within an agent user's daily overview row.
 */
@Data
public class MarkAgentDailyPlatformSummaryVO {

    @Schema(title = "Platform code")
    private String platformCode;

    @Schema(title = "Platform name")
    private String platformName;

    @Schema(title = "Order count")
    private Long orderCount;

    @Schema(title = "Phone item count")
    private Long itemCount;

    @Schema(title = "Success count")
    private Long successCount;

    @Schema(title = "Failed count")
    private Long failedCount;

    @Schema(title = "Processing count")
    private Long processingCount;

    @Schema(title = "Pending count")
    private Long pendingCount;

    @Schema(title = "Last processed time")
    private String lastProcessedTime;
}
