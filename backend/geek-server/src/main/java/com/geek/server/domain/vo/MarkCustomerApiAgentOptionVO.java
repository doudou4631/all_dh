package com.geek.server.domain.vo;

import lombok.Data;

/** Active agent selectable as the owner of customer API orders. */
@Data
public class MarkCustomerApiAgentOptionVO {
    private Long userId;
    private String userName;
    private String nickName;
}
