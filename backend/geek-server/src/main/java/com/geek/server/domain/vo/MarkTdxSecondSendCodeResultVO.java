package com.geek.server.domain.vo;

import lombok.Data;

@Data
public class MarkTdxSecondSendCodeResultVO {
    private String phone;
    private String line;
    private String message;
    private Integer phoneCheckCode;
}
