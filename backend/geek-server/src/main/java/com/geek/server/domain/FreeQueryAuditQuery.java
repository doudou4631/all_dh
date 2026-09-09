package com.geek.server.domain;

public class FreeQueryAuditQuery {
    private String ipAddr;
    private String queryPhone;
    private String markedOnly;

    public String getIpAddr() { return ipAddr; }
    public void setIpAddr(String ipAddr) { this.ipAddr = ipAddr; }
    public String getQueryPhone() { return queryPhone; }
    public void setQueryPhone(String queryPhone) { this.queryPhone = queryPhone; }
    public String getMarkedOnly() { return markedOnly; }
    public void setMarkedOnly(String markedOnly) { this.markedOnly = markedOnly; }
}
