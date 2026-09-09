package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MarkTdxSecondSendCodeRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的11位手机号")
    private String phone;

    @Pattern(regexp = "^line[12]$", message = "通道线路仅支持line1或line2")
    private String line;
}
