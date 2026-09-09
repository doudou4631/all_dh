package com.geek.server.domain;

import com.geek.common.core.domain.BaseEntity;

public class FreeQueryRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String visitorKey;
    private String ipAddr;
    private String userAgent;
    private String queryPhone;
    private String resultDetails;
    private String queryStatus;
    private String hasResult;
    private Integer platformTotal;
    private Integer platformSuccess;
    private Integer markedCount;
    private Long responseTime;
    private String errorMessage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVisitorKey() { return visitorKey; }
    public void setVisitorKey(String visitorKey) { this.visitorKey = visitorKey; }
    public String getIpAddr() { return ipAddr; }
    public void setIpAddr(String ipAddr) { this.ipAddr = ipAddr; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getQueryPhone() { return queryPhone; }
    public void setQueryPhone(String queryPhone) { this.queryPhone = queryPhone; }
    public String getResultDetails() { return resultDetails; }
    public void setResultDetails(String resultDetails) { this.resultDetails = resultDetails; }
    public String getQueryStatus() { return queryStatus; }
    public void setQueryStatus(String queryStatus) { this.queryStatus = queryStatus; }
    public String getHasResult() { return hasResult; }
    public void setHasResult(String hasResult) { this.hasResult = hasResult; }
    public Integer getPlatformTotal() { return platformTotal; }
    public void setPlatformTotal(Integer platformTotal) { this.platformTotal = platformTotal; }
    public Integer getPlatformSuccess() { return platformSuccess; }
    public void setPlatformSuccess(Integer platformSuccess) { this.platformSuccess = platformSuccess; }
    public Integer getMarkedCount() { return markedCount; }
    public void setMarkedCount(Integer markedCount) { this.markedCount = markedCount; }
    public Long getResponseTime() { return responseTime; }
    public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
