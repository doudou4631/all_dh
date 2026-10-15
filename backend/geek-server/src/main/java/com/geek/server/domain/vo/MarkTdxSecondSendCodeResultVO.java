package com.geek.server.domain.vo;

import lombok.Data;

@Data
public class MarkTdxSecondSendCodeResultVO {
    private String phone;
    private String line;
    private String message;
    private Integer phoneCheckCode;
    private String taskId;
    private String externalRequestId;
    private String upstreamStatus;
    // Compatibility fields retained for the legacy appeal service response.
    private Boolean allowed;
    private String status;
    private Object precheckResponse;
    private Object sendCodeResponse;
}
