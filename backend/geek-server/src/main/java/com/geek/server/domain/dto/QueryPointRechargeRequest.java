package com.geek.server.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员为用户充值号码查询积分的请求。
 */
@Data
public class QueryPointRechargeRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "充值积分不能为空")
    @Min(value = 1L, message = "充值积分必须大于0")
    @Max(value = 10000000L, message = "单次充值积分不能超过10000000")
    private Integer amount;

    @Size(max = 255, message = "充值备注不能超过255个字符")
    private String remark;
}
