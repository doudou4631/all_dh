package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    @Size(max = 500, message = "用户备注不能超过500个字符")
    private String userRemark;

}
