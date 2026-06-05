package com.geek.server.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "手机端用户积分调整请求")
public class FreeQueryPointAdjustRequest {

    @Schema(title = "用户ID")
    private Long userId;

    @Schema(title = "积分值（正整数）")
    private Integer pointAmount;

    @Schema(title = "积分类型（1增加 2扣减）")
    private String pointType;

    @Schema(title = "变动原因")
    private String reason;
}
