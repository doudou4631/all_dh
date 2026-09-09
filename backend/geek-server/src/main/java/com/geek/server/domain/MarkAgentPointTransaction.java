package com.geek.server.domain;

import lombok.Data;
import java.util.Date;

@Data
public class MarkAgentPointTransaction {
    private Long id;
    private String transactionNo;
    private String requestNo;
    private String transactionType;
    private String platformCode;
    private String platformName;
    private Long amount;
    private Long fromUserId;
    private Long toUserId;
    private Long operatorUserId;
    private String reason;
    private String status;
    private Date createTime;
}
