package com.geek.server.domain;

import lombok.Data;

import java.util.Date;

@Data
public class MarkOpenApiCredential {
    private Long id;
    private String name;
    private String apiKey;
    private String secretHash;
    private Long userId;
    private String userName;
    private String status;
    private String scopes;
    private Date expireTime;
    private Date lastUsedTime;
    private Date createTime;
}
