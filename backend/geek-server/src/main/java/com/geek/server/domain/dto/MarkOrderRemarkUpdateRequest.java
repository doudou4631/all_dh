package com.geek.server.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MarkOrderRemarkUpdateRequest {

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
