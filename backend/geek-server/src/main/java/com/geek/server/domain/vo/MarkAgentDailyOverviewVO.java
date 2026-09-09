package com.geek.server.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Daily processing totals grouped by downstream user.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MarkAgentDailyOverviewVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(title = "Processing date")
    private String statDate;

    @Schema(title = "User ID")
    private Long userId;

    @Schema(title = "Login name")
    private String userName;

    @Schema(title = "Nick name")
    private String nickName;

    @Schema(title = "Platform code filter")
    private String platformCode;

    @Schema(title = "Platform count")
    private Long platformCount;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastProcessedTime;

    @JsonIgnore
    private String platformSummaryJson;

    @Schema(title = "Platform summaries")
    private List<MarkAgentDailyPlatformSummaryVO> platforms = new ArrayList<>();
}
