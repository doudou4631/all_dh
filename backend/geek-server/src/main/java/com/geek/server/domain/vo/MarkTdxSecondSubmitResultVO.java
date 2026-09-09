package com.geek.server.domain.vo;

import lombok.Data;

@Data
public class MarkTdxSecondSubmitResultVO {
    private String phone;
    private Long tdxId;
    private String orderpicinumber;
    private String taskId;
    private String upstreamStatus;
    private Boolean refundSuggested;
    private String message;
    private Long itemId;
    private Long orderId;
    private String orderNo;
    private String processStatus;
}
