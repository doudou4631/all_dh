package com.geek.server.domain.vo;

import lombok.Data;

@Data
public class MarkAgentProcessUserOptionVO {

    private Long userId;

    private String userName;

    private String nickName;

    private Boolean hidden;
}
