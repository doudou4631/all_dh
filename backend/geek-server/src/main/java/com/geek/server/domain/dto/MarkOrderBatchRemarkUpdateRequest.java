package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MarkOrderBatchRemarkUpdateRequest {

    @NotEmpty(message = "请选择需要备注的号码")
    @Size(max = 500, message = "单次最多备注500个号码")
    private List<Long> itemIds;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
