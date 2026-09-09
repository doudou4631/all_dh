package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MarkAgentHiddenUserRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "屏蔽状态不能为空")
    private Boolean hidden;
}
