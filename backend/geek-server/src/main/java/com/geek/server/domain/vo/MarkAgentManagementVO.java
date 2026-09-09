package com.geek.server.domain.vo;
import lombok.Data;
import java.util.Date;
@Data public class MarkAgentManagementVO {
    private Long userId; private String userName; private String nickName; private String phonenumber;
    private String status; private Integer agentLevel; private Long parentUserId; private String parentUserName;
    private Long totalRemainCount; private Date createTime;
}
