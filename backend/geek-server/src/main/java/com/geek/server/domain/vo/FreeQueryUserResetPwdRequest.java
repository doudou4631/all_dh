package com.geek.server.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "免费查询用户重置密码请求")
public class FreeQueryUserResetPwdRequest {

    @Schema(title = "用户ID")
    private Long userId;

    @Schema(title = "新密码")
    private String password;
}
