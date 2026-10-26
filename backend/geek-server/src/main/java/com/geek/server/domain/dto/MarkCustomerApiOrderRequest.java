package com.geek.server.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** Customer API request body. The platform is supplied by the URL path, not trusted from the body. */
@Data
public class MarkCustomerApiOrderRequest {
    @NotEmpty(message = "号码列表不能为空")
    @Size(max = 100, message = "单次最多提交100个号码")
    private List<String> phones;

    /** Mandatory for submit; optional for check. */
    @Size(max = 100, message = "请求编号不能超过100个字符")
    @JsonAlias({"request_no"})
    private String requestNo;

    @Size(max = 500, message = "备注不能超过500个字符")
    @JsonAlias({"agent_remark"})
    private String remark;
}
