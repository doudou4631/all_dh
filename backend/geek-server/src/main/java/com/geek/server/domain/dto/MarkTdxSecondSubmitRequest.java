package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MarkTdxSecondSubmitRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的11位手机号")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码应为6位数字")
    private String smsCode;

    @Pattern(regexp = "^line[123]$", message = "通道线路仅支持line1、line2或line3")
    private String line;

    /** Original failed task to reuse when submitting through line3. */
    private String sourceTaskId;

    /** Task id returned by the external line2 API when the code was sent. */
    private String externalTaskId;

    private Boolean rotate;
}
