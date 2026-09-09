package com.geek.server.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class MarkInspectionPolicy implements Serializable {
    private Long id;
    private String policyName;
    private String platformCode;
    private String platformName;
    private Integer intervalSeconds;
    private Integer intervalValue;
    private String intervalUnit;
    private Integer batchLimit;
    private String autoAction;
    private String status;
    private String running;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date lastRunTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date nextRunTime;
}
