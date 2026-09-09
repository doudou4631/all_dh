package com.geek.server.domain;

public class FreeQueryPlatform {
    private Long id;
    private String platformName;
    private String icon;
    private String sourceDescription;
    private String status;
    private String globalStatus;
    private Integer priority;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getSourceDescription() { return sourceDescription; }
    public void setSourceDescription(String sourceDescription) { this.sourceDescription = sourceDescription; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getGlobalStatus() { return globalStatus; }
    public void setGlobalStatus(String globalStatus) { this.globalStatus = globalStatus; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}
