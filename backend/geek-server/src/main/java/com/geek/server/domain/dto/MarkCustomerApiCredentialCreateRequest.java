package com.geek.server.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;
import java.util.List;

/** Request used by administrators to create a customer-facing mark API key. */
@Data
public class MarkCustomerApiCredentialCreateRequest {
    @NotBlank(message = "API名称不能为空")
    private String name;

    @NotBlank(message = "客户账号不能为空")
    private String userName;

    @NotNull(message = "责任代理不能为空")
    private Long assignedAgentId;

    @NotEmpty(message = "至少选择一个平台")
    private List<String> platformCodes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date expireTime;
}
