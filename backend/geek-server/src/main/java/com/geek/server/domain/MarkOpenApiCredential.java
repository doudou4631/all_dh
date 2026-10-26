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
    /** PROCESSING: agent task API; CUSTOMER: customer submit/query API. */
    private String credentialType;
    /** Required for CUSTOMER credentials. New customer orders are assigned to this agent. */
    private Long assignedAgentId;
    private String assignedAgentName;
    private String status;
    private String scopes;
    private Date expireTime;
    private Date lastUsedTime;
    private Date createTime;
    private java.util.List<String> platformCodes;
}
