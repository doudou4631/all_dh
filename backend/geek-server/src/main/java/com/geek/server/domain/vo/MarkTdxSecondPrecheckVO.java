package com.geek.server.domain.vo;

import lombok.Data;

@Data
public class MarkTdxSecondPrecheckVO {
    private String phone;
    private Boolean ok;
    private Boolean allowed;
    private String status;
    private String message;
    private Integer phoneCheckCode;
}
