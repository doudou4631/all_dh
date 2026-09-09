package com.geek.server.domain;

import lombok.Data;

import java.io.Serializable;

/** 标记业务用户端与后台处理端平台映射。 */
@Data
public class MarkPlatformMapping implements Serializable {
    private Long id;
    private String sourcePlatformName;
    private String sourcePlatformCode;
    private String sourceEnabled;
    private String targetPlatformName;
    private String targetPlatformCode;
    private String targetEnabled;
    private String mappingEnabled;
    private Integer sortNum;
}
