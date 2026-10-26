package com.geek.server.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class MarkInspectionRunLog implements Serializable {
    private Long id;
    private String runNo;
    private Long policyId;
    private String policyName;
    private String platformCode;
    private String platformName;
    private String triggerType;
    private Integer scannedCount;
    private Integer checkedCount;
    private Integer markedCount;
    private Integer matchedCount;
    private Integer successCount;
    /** Success count filter: SUCCESS (greater than zero) or NO_SUCCESS (zero). */
    private String successFilter;
    /** Display only the latest run for each inspection policy. */
    private String latestOnly;
    private Integer failedCount;
    private Integer skippedCount;
    private String runStatus;
    private String errorMessage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date endTime;
}
