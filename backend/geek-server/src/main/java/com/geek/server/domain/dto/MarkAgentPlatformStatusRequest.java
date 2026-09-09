package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 代理端下线用户平台开启/关闭请求
 */
@Data
public class MarkAgentPlatformStatusRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    private String platformName;

    @NotBlank(message = "平台状态不能为空")
    private String status;
}
