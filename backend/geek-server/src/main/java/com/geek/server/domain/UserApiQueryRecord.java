package com.geek.server.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;

/**
 * 接口查询记录通用对象 user_api_query_record
 * 
 * @author geek
 * @date 2026-03-09
 */
@Schema(description = "接口查询记录通用对象")
public class UserApiQueryRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;


    /** 主键ID */
    @Schema(title = "主键ID")
    private Long id;

    /** 查询类型（1批量 2单条） */
    @Schema(title = "查询类型（1批量 2单条）")
    @Excel(name = "查询类型", readConverterExp = "1=批量,2=单条")
    private String queryType;

    /** 平台ID */
    @Schema(title = "平台ID")
//    @Excel(name = "平台ID")
    private Long platformId;

    /** 平台名称 */
    @Schema(title = "平台名称")
    @Excel(name = "平台名称")
    private String platformName;

    /** 请求状态（0成功 1失败） */
    @Schema(title = "请求状态（0成功 1失败）")
    @Excel(name = "请求状态", readConverterExp = "0=成功,1=失败")
    private String requestStatus;

    /** 请求耗时（毫秒） */
    @Schema(title = "请求耗时（毫秒）")
    @Excel(name = "请求耗时")
    private Long requestTime;

    /** 操作用户ID */
    @Schema(title = "操作用户ID")
//    @Excel(name = "操作用户ID")
    private Long userId;

    /** 查询号码 */
    @Schema(title = "查询号码")
    @Excel(name = "查询号码")
    private String phone;
    /** 查询来源（FREE_SINGLE/FREE_BATCH） */
    @Schema(title = "查询来源（FREE_SINGLE/FREE_BATCH）")
    private String sourceType;

    /** 设备ID */
    @Schema(title = "设备ID")
    private String deviceId;

    /** 设备来源（client/ip-fallback） */
    @Schema(title = "设备来源（client/ip-fallback）")
    private String deviceSource;

    /** 使用前次数 */
    @Schema(title = "使用前次数")
    private Integer usedBefore;

    /** 使用后次数 */
    @Schema(title = "使用后次数")
    private Integer usedAfter;

    /** 限额值 */
    @Schema(title = "限额值")
    private Integer limitValue;

    /** 错误码（结构化） */
    @Schema(title = "错误码（结构化）")
    private String errorCode;

    /** 来源IP */
    @Schema(title = "来源IP")
    private String ipAddr;

    /** 请求参数 */
    @Schema(title = "请求参数")
//    @Excel(name = "请求参数")
    private String requestParams;

    /** 响应结果 */
    @Schema(title = "响应结果")
//    @Excel(name = "响应结果")
    private String responseResult;

    /** 查询结果 */
    @Schema(title = "查询结果")
    @Excel(name = "查询结果")
    private String results;

    /** 任务ID */
    @Schema(title = "任务ID")
    private String taskId;

    /** 创建者 */
    @Schema(title = "操作人")
    @Excel(name = "操作人")
    private String createBy;


    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }


    public void setQueryType(String queryType) 
    {
        this.queryType = queryType;
    }

    public String getQueryType() 
    {
        return queryType;
    }

    public Long getPlatformId() {
        return platformId;
    }

    public void setPlatformId(Long platformId) {
        this.platformId = platformId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public void setRequestStatus(String requestStatus)
    {
        this.requestStatus = requestStatus;
    }

    public String getRequestStatus() 
    {
        return requestStatus;
    }


    public void setRequestTime(Long requestTime) 
    {
        this.requestTime = requestTime;
    }

    public Long getRequestTime() 
    {
        return requestTime;
    }


    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceSource() {
        return deviceSource;
    }

    public void setDeviceSource(String deviceSource) {
        this.deviceSource = deviceSource;
    }

    public Integer getUsedBefore() {
        return usedBefore;
    }

    public void setUsedBefore(Integer usedBefore) {
        this.usedBefore = usedBefore;
    }

    public Integer getUsedAfter() {
        return usedAfter;
    }

    public void setUsedAfter(Integer usedAfter) {
        this.usedAfter = usedAfter;
    }

    public Integer getLimitValue() {
        return limitValue;
    }

    public void setLimitValue(Integer limitValue) {
        this.limitValue = limitValue;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public void setRequestParams(String requestParams)
    {
        this.requestParams = requestParams;
    }

    public String getRequestParams() 
    {
        return requestParams;
    }


    public void setResponseResult(String responseResult) 
    {
        this.responseResult = responseResult;
    }

    public String getResponseResult() 
    {
        return responseResult;
    }

    public String getResults() {
        return results;
    }

    public void setResults(String results) {
        this.results = results;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public String getCreateBy() {
        return createBy;
    }

    @Override
    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("queryType", getQueryType())
            .append("platformId", getPlatformId())
            .append("platformName", getPlatformName())
            .append("requestStatus", getRequestStatus())
            .append("requestTime", getRequestTime())
            .append("userId", getUserId())
            .append("phone", getPhone())
            .append("sourceType", getSourceType())
            .append("deviceId", getDeviceId())
            .append("deviceSource", getDeviceSource())
            .append("usedBefore", getUsedBefore())
            .append("usedAfter", getUsedAfter())
            .append("limitValue", getLimitValue())
            .append("errorCode", getErrorCode())
            .append("ipAddr", getIpAddr())
            .append("requestParams", getRequestParams())
            .append("responseResult", getResponseResult())
            .append("results", getResults())
            .append("taskId", getTaskId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
