package com.geek.server.domain;

import java.util.Date;

public class FreeQueryAuditRecord {
    private Long id;
    private String ipAddr;
    private String queryPhone;
    private String queryStatus;
    private String hasResult;
    private Integer markedCount;
    private Long responseTime;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIpAddr() { return ipAddr; }
    public void setIpAddr(String ipAddr) { this.ipAddr = ipAddr; }
    public String getQueryPhone() { return queryPhone; }
    public void setQueryPhone(String queryPhone) { this.queryPhone = queryPhone; }
    public String getQueryStatus() { return queryStatus; }
    public void setQueryStatus(String queryStatus) { this.queryStatus = queryStatus; }
    public String getHasResult() { return hasResult; }
    public void setHasResult(String hasResult) { this.hasResult = hasResult; }
    public Integer getMarkedCount() { return markedCount; }
    public void setMarkedCount(Integer markedCount) { this.markedCount = markedCount; }
    public Long getResponseTime() { return responseTime; }
    public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
