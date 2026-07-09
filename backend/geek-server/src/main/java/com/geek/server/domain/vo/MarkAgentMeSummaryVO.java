package com.geek.server.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Current agent account summary.
 */
@Data
public class MarkAgentMeSummaryVO {

    @Schema(title = "User ID")
    private Long userId;

    @Schema(title = "Login name")
    private String userName;

    @Schema(title = "Nick name")
    private String nickName;

    @Schema(title = "Remark")
    private String remark;

    @Schema(title = "Agent level label")
    private String agentLevelLabel;

    @Schema(title = "Own total remain count")
    private Long totalRemainCount;

    @Schema(title = "Downstream user count")
    private Long downstreamCount;

    @Schema(title = "Pending audit count")
    private Long pendingAuditCount;

    @Schema(title = "Sample unit price")
    private Long sampleUnitPrice;

    @Schema(title = "Sample platform name")
    private String samplePlatformName;
}
