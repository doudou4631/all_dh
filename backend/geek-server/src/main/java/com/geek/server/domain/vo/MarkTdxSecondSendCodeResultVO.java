package com.geek.server.domain.vo;

import lombok.Data;

import java.util.Map;

/**
 * Taidixiong二次短信验证码发送结果
 */
@Data
public class MarkTdxSecondSendCodeResultVO {

    private String phone;

    private Boolean allowed;

    private String status;

    private String line;

    private String message;

    private Map<String, Object> precheckResponse;

    private Map<String, Object> sendCodeResponse;
}
