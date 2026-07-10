package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Taidixiong二次获取短信验证码请求
 */
@Data
public class MarkTdxSecondSendCodeRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "手机号应为11位数字")
    private String phone;

    @Pattern(regexp = "^line[12]$", message = "通道线路仅支持line1或line2")
    private String line;
}
