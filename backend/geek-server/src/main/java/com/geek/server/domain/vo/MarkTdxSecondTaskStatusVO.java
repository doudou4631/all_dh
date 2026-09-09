package com.geek.server.domain.vo;

import lombok.Data;

@Data
public class MarkTdxSecondTaskStatusVO {
    private String taskId;
    private String status;
    private String rawStatus;
    private String message;
    private Boolean refundSuggested;
    private Integer attempts;
}
