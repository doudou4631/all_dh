package com.geek.server.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Agent downstream account summary.
 */
@Data
public class MarkAgentDownstreamSummaryVO {

    @Schema(title = "User ID")
    private Long userId;

    @Schema(title = "Login name")
    private String userName;

    @Schema(title = "Nick name")
    private String nickName;

    @Schema(title = "Remark")
    private String remark;

    @Schema(title = "Account status")
    private String status;

    @Schema(title = "Total remain count")
    private Long totalRemainCount;
}
