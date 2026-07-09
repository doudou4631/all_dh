package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 腾讯手机号验证码提交请求
 */
@Data
public class MarkTencentSubmitRequest {

    @Pattern(regexp = "^[A-Za-z0-9_:-]{1,64}$", message = "平台编码格式不正确")
    private String platformCode;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "手机号应为11位数字")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码应为6位数字")
    private String smsCode;

    /**
     * 是否强制按篡改模式提交（phone_type=2）
     */
    private Boolean forceTamper;
}
