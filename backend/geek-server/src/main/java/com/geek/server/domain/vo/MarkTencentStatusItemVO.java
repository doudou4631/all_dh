package com.geek.server.domain.vo;

import lombok.Data;

import java.util.Map;

/**
 * 腾讯号码实时状态查询项
 */
@Data
public class MarkTencentStatusItemVO {

    private String phone;

    private Boolean success;

    private Integer phoneType;

    private String complainStatus;

    private Boolean marked;

    private String detail;

    private String errorMessage;

    private Map<String, Object> phoneTypeResponse;

    private Map<String, Object> complainStatusResponse;
}
