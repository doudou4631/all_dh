package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 用户下单请求
 */
@Data
public class MarkOrderCreateRequest {

    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    private String platformName;

    @NotEmpty(message = "号码列表不能为空")
    private List<String> phones;

    private String requestNo;

    private String remark;
}
