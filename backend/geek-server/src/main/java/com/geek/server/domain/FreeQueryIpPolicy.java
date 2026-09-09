package com.geek.server.domain;

import com.geek.common.core.domain.BaseEntity;

import java.time.LocalTime;

public class FreeQueryIpPolicy extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer dailyLimit;
    private Integer intervalMs;
    private LocalTime resetTime;
    private String exceedMessage;
    private String whitelistIps;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Integer dailyLimit) { this.dailyLimit = dailyLimit; }
    public Integer getIntervalMs() { return intervalMs; }
    public void setIntervalMs(Integer intervalMs) { this.intervalMs = intervalMs; }
    public LocalTime getResetTime() { return resetTime; }
    public void setResetTime(LocalTime resetTime) { this.resetTime = resetTime; }
    public String getExceedMessage() { return exceedMessage; }
    public void setExceedMessage(String exceedMessage) { this.exceedMessage = exceedMessage; }
    public String getWhitelistIps() { return whitelistIps; }
    public void setWhitelistIps(String whitelistIps) { this.whitelistIps = whitelistIps; }
}
