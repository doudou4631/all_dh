package com.geek.server.domain.vo;

import lombok.Data;

import java.util.Map;

/**
 * Taidixiong二次申诉提交结果
 */
@Data
public class MarkTdxSecondSubmitResultVO {

    private String phone;

    private Long tdxId;

    private String orderpicinumber;

    private Boolean accepted;

    private String message;

    private Long itemId;

    private Long orderId;

    private String orderNo;

    /** 0待处理 1成功 2失败 */
    private String processStatus;

    private Map<String, Object> submitResponse;
}
