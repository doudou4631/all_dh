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
    /** PENDING, PROCESSING or BOTH. */
    private String orderScope;
    private Boolean autoTakeover;
    private String status;
    private String running;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date lastRunTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date nextRunTime;

    public String resolvedOrderScope() {
        if (orderScope != null) return orderScope;
        if (MarkWumaPlatform.contains(platformCode)) return "PROCESSING";
        return "xiaomi".equalsIgnoreCase(platformCode) ? "BOTH" : "PENDING";
    }

    public boolean takeoverEnabled() { return Boolean.TRUE.equals(autoTakeover); }

    public boolean acceptsStatus(String processStatus) {
        String scope = resolvedOrderScope();
        if ("0".equals(processStatus)) {
            return ("PENDING".equals(scope) || "BOTH".equals(scope)) && (!MarkWumaPlatform.contains(platformCode) || takeoverEnabled());
        }
        return "3".equals(processStatus) && ("PROCESSING".equals(scope) || "BOTH".equals(scope));
    }
}
