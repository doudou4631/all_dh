package com.geek.server.domain.vo;

public class ApiRequestVO {

    private static final long serialVersionUID = 1L;

    /** 平台ID */
    private String platformId;

    /** 平台名称 */
    private String platformName;

    /** 平台URL地址 */
    private String url;

    /** 状态（0正常 1停用） */
    private String status;

    /** 请求间隔毫秒数 (默认1秒) */
    private Long requestIntervalMs;

    /** 单次请求超时时间毫秒数 */
    private Long timeoutMs;

    /** 失败重试次数 */
    private Integer retryCount;

    /** 前置操作类型 (0:无 1:获取Token  */
    private Integer preActionType;

    /** 前置操作详细配置 (JSON格式) */
    private String preActionConfig;

    /** 默认请求头模板 (JSON格式) */
    private String headersTemplate;

    /** 最大并发限制数 */
    private Integer concurrencyLimit;

    /** 查询号码 */
    private String phoneNumber;

    /** 查询类型 (1:批量查询 2:单条查询) */
    private String queryType;

    public String getPlatformId() {
        return platformId;
    }

    /**批次号 */
    private String taskId;

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getRequestIntervalMs() {
        return requestIntervalMs;
    }

    public void setRequestIntervalMs(Long requestIntervalMs) {
        this.requestIntervalMs = requestIntervalMs;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getPreActionType() {
        return preActionType;
    }

    public void setPreActionType(Integer preActionType) {
        this.preActionType = preActionType;
    }

    public String getPreActionConfig() {
        return preActionConfig;
    }

    public void setPreActionConfig(String preActionConfig) {
        this.preActionConfig = preActionConfig;
    }

    public String getHeadersTemplate() {
        return headersTemplate;
    }

    public void setHeadersTemplate(String headersTemplate) {
        this.headersTemplate = headersTemplate;
    }

    public Integer getConcurrencyLimit() {
        return concurrencyLimit;
    }

    public void setConcurrencyLimit(Integer concurrencyLimit) {
        this.concurrencyLimit = concurrencyLimit;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
