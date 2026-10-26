package com.geek.server.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;
import java.util.List;

/** Editable fields of a customer-facing mark API key. The customer account is immutable. */
@Data
public class MarkCustomerApiCredentialUpdateRequest {
    @NotNull(message = "API编号不能为空")
    private Long id;

    @NotBlank(message = "API名称不能为空")
    private String name;

    @NotNull(message = "责任代理不能为空")
    private Long assignedAgentId;

    @NotEmpty(message = "至少选择一个平台")
    private List<String> platformCodes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date expireTime;
}
