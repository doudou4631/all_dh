package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 腾讯号码实时状态查询请求
 */
@Data
public class MarkTencentStatusQueryRequest {

    @NotEmpty(message = "号码列表不能为空")
    private List<String> phones;
}
