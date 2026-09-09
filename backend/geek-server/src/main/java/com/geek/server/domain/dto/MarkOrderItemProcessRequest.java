package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 代理处理明细请求
 */
@Data
public class MarkOrderItemProcessRequest {

    @NotBlank(message = "处理状态不能为空")
    private String processStatus;

    private String processResult;

    private String processNote;
}
