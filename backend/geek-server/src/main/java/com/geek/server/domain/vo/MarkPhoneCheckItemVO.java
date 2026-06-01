package com.geek.server.domain.vo;

import lombok.Data;

/**
 * 单个号码预查询结果
 */
@Data
public class MarkPhoneCheckItemVO {

    private String phone;

    private Boolean querySuccess;

    private Boolean marked;

    private String status;

    private String detail;

    private String errorMessage;

    private Long responseTime;
}
