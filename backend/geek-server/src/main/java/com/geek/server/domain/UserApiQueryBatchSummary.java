package com.geek.server.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单次查询记录：按手机号 + 批次聚合后的列表行
 */
@Schema(description = "单次查询批次聚合摘要")
public class UserApiQueryBatchSummary {

    @Schema(title = "展示用 ID（组内最小主键）")
    private Long displayId;

    @Schema(title = "查询号码")
    private String phone;

    @Schema(title = "任务/批次 ID")
    private String taskId;

    @Schema(title = "批次分组键，详情查询必传")
    private String batchKey;

    @Schema(title = "查询类型")
    private String queryType;

    @Schema(title = "该批次最后一条记录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date queryTime;

    @Schema(title = "平台条数")
    private Integer platformCount;

    @Schema(title = "有标记平台数")
    private Integer markedPlatformCount;

    @Schema(title = "列表摘要文案")
    private String resultSummary;

    @Schema(title = "组内记录 id，逗号分隔")
    private String memberIds;

    public Long getDisplayId() {
        return displayId;
    }

    public void setDisplayId(Long displayId) {
        this.displayId = displayId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getBatchKey() {
        return batchKey;
    }

    public void setBatchKey(String batchKey) {
        this.batchKey = batchKey;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public Date getQueryTime() {
        return queryTime;
    }

    public void setQueryTime(Date queryTime) {
        this.queryTime = queryTime;
    }

    public Integer getPlatformCount() {
        return platformCount;
    }

    public void setPlatformCount(Integer platformCount) {
        this.platformCount = platformCount;
    }

    public Integer getMarkedPlatformCount() {
        return markedPlatformCount;
    }

    public void setMarkedPlatformCount(Integer markedPlatformCount) {
        this.markedPlatformCount = markedPlatformCount;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public String getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(String memberIds) {
        this.memberIds = memberIds;
    }
}
