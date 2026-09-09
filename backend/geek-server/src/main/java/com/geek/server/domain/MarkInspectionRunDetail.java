package com.geek.server.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class MarkInspectionRunDetail implements Serializable {
    private Long id;
    private Long runLogId;
    private Long orderId;
    private Long orderItemId;
    private String orderNo;
    private String phone;
    private String detectionResult;
    private String qualified;
    private String submissionStatus;
    private String submissionResult;
    private String processNote;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date detectedTime;
}
