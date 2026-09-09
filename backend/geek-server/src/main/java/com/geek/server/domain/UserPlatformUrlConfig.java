package com.geek.server.domain;

import com.mybatisflex.annotation.Column;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;

import java.beans.Transient;
import java.util.List;

/**
 * 查询平台url配置对象 user_platform_url_config
 *
 * @author geek
 * @date 2026-03-09
 */
@Schema(description = "查询平台url配置对象")
public class UserPlatformUrlConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;


    /** 主键ID */
    @Schema(title = "主键ID")
    private Long id;

    /** 平台ID */
    @Schema(title = "平台ID")
    @Excel(name = "平台ID")
    private String platformId;

    /** Stable key used by mark business to bind a platform to its query API. */
    @Schema(title = "平台编码")
    @Excel(name = "平台编码")
    private String platformKey;

    /** 平台名称 */
    @Schema(title = "平台名称")
    @Excel(name = "平台名称")
    private String platformName;

    /** Platform-management values used by interface management. */
    private String bindingPlatformName;
    private Boolean bindingEnabled;
    private Integer bindingSort;

    /** 平台URL地址 */
    @Schema(title = "平台URL地址")
    @Excel(name = "平台URL地址")
    private String url;

    /** 状态（0正常 1停用） */
    @Schema(title = "状态（0正常 1停用）")
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 请求间隔毫秒数 (默认1秒) */
    @Schema(title = "请求间隔毫秒数", description = "默认1秒")
    @Excel(name = "请求间隔毫秒数")
    private Long requestIntervalMs;

    /** 单次请求超时时间毫秒数 */
    @Schema(title = "单次请求超时时间毫秒数")
    @Excel(name = "单次请求超时时间")
    private Long timeoutMs;

    /** 失败重试次数 */
    @Schema(title = "失败重试次数")
    @Excel(name = "失败重试次数")
    private Integer retryCount;

    /** 前置操作类型 (0:无 1:登录 2:获取Token 3:执行JS) */
    @Schema(title = "前置操作类型", description = "0:无 1:登录 2:获取Token 3:执行JS")
    @Excel(name = "前置操作类型", readConverterExp = "0=无,1=登录,2=获取Token,3=执行JS")
    private Integer preActionType;

    /** 前置操作详细配置 (JSON格式) */
    @Schema(title = "前置操作详细配置", description = "JSON格式")
    @Excel(name = "前置操作详细配置")
    private String preActionConfig;

    /** 默认请求头模板 (JSON格式) */
    @Schema(title = "默认请求头模板", description = "JSON格式")
    @Excel(name = "默认请求头模板")
    private String headersTemplate;

    /** 最大并发限制数 */
    @Schema(title = "最大并发限制数")
    @Excel(name = "最大并发限制数")
    private Integer concurrencyLimit;

    @Schema(title = "排序")
    private Integer sort;

    @Schema(title = "免费查询状态", description = "0启用 1停用")
    private String freeQueryStatus;

    @Schema(title = "免费查询优先级")
    private Integer freeQuerySort;

    @Schema(title = "免费查询平台图标")
    private String freeQueryIcon;

    @Schema(title = "免费查询数据来源说明")
    private String freeQueryDescription;

    /** 平台id集合 */
    @Column
    private Integer[] ids;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }


    public void setPlatformId(String platformId)
    {
        this.platformId = platformId;
    }

    public String getPlatformId()
    {
        return platformId;
    }

    public String getPlatformKey()
    {
        return platformKey;
    }

    public void setPlatformKey(String platformKey)
    {
        this.platformKey = platformKey;
    }


    public void setPlatformName(String platformName)
    {
        this.platformName = platformName;
    }

    public String getPlatformName()
    {
        return platformName;
    }


    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }


    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setRequestIntervalMs(Long requestIntervalMs)
    {
        this.requestIntervalMs = requestIntervalMs;
    }

    public Long getRequestIntervalMs()
    {
        return requestIntervalMs;
    }

    public void setTimeoutMs(Long timeoutMs)
    {
        this.timeoutMs = timeoutMs;
    }

    public Long getTimeoutMs()
    {
        return timeoutMs;
    }

    public void setRetryCount(Integer retryCount)
    {
        this.retryCount = retryCount;
    }

    public Integer getRetryCount()
    {
        return retryCount;
    }

    public void setPreActionType(Integer preActionType)
    {
        this.preActionType = preActionType;
    }

    public Integer getPreActionType()
    {
        return preActionType;
    }

    public void setPreActionConfig(String preActionConfig)
    {
        this.preActionConfig = preActionConfig;
    }

    public String getPreActionConfig()
    {
        return preActionConfig;
    }

    public void setHeadersTemplate(String headersTemplate)
    {
        this.headersTemplate = headersTemplate;
    }

    public String getHeadersTemplate()
    {
        return headersTemplate;
    }

    public void setConcurrencyLimit(Integer concurrencyLimit)
    {
        this.concurrencyLimit = concurrencyLimit;
    }

    public Integer getConcurrencyLimit()
    {
        return concurrencyLimit;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getBindingPlatformName() { return bindingPlatformName; }
    public void setBindingPlatformName(String bindingPlatformName) { this.bindingPlatformName = bindingPlatformName; }
    public Boolean getBindingEnabled() { return bindingEnabled; }
    public void setBindingEnabled(Boolean bindingEnabled) { this.bindingEnabled = bindingEnabled; }
    public Integer getBindingSort() { return bindingSort; }
    public void setBindingSort(Integer bindingSort) { this.bindingSort = bindingSort; }

    public String getFreeQueryStatus() { return freeQueryStatus; }
    public void setFreeQueryStatus(String freeQueryStatus) { this.freeQueryStatus = freeQueryStatus; }
    public Integer getFreeQuerySort() { return freeQuerySort; }
    public void setFreeQuerySort(Integer freeQuerySort) { this.freeQuerySort = freeQuerySort; }
    public String getFreeQueryIcon() { return freeQueryIcon; }
    public void setFreeQueryIcon(String freeQueryIcon) { this.freeQueryIcon = freeQueryIcon; }
    public String getFreeQueryDescription() { return freeQueryDescription; }
    public void setFreeQueryDescription(String freeQueryDescription) { this.freeQueryDescription = freeQueryDescription; }

    public Integer[] getIds() {
        return ids;
    }

    public void setIds(Integer[] ids) {
        this.ids = ids;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("platformId", getPlatformId())
                .append("platformKey", getPlatformKey())
                .append("platformName", getPlatformName())
                .append("url", getUrl())
                .append("status", getStatus())
                .append("requestIntervalMs", getRequestIntervalMs())
                .append("timeoutMs", getTimeoutMs())
                .append("retryCount", getRetryCount())
                .append("preActionType", getPreActionType())
                .append("preActionConfig", getPreActionConfig())
                .append("headersTemplate", getHeadersTemplate())
                .append("concurrencyLimit", getConcurrencyLimit())
                .append("sort", getSort())
                .append("freeQueryStatus", getFreeQueryStatus())
                .append("freeQuerySort", getFreeQuerySort())
                .append("freeQueryIcon", getFreeQueryIcon())
                .append("freeQueryDescription", getFreeQueryDescription())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .toString();
    }
}
