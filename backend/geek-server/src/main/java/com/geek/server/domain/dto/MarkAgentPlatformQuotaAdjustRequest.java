package com.geek.server.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 标记代理给下线按平台充值/扣减请求
 */
@Data
public class MarkAgentPlatformQuotaAdjustRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    private String platformName;

    @NotBlank(message = "变动类型不能为空")
    private String adjustType;

    @NotNull(message = "变动次数不能为空")
    @Min(value = 1L, message = "变动次数必须大于0")
    private Long changeCount;

    private String remark;
}
