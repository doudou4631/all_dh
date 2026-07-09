package com.geek.server.domain.vo;

import lombok.Data;

import java.util.Map;

/**
 * 腾讯手机号验证码提交结果
 */
@Data
public class MarkTencentSubmitResultVO {

    private String phone;

    private Integer originalPhoneType;

    private Integer submittedPhoneType;

    private String complainStatus;

    private Integer verifyReCode;

    private String verifyData;

    private Integer submitReCode;

    private String submitData;

    private Boolean accepted;

    private Long itemId;

    private Long orderId;

    private String orderNo;

    /** 0待处理 1成功 2失败 */
    private String processStatus;

    private Map<String, Object> phoneTypeResponse;

    private Map<String, Object> complainStatusResponse;

    private Map<String, Object> verifyResponse;

    private Map<String, Object> submitResponse;
}
