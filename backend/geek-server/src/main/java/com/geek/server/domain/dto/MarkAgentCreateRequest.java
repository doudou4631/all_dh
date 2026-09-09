package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MarkAgentCreateRequest {
    @NotBlank @Size(min=2,max=20) private String userName;
    @NotBlank @Size(max=30) private String nickName;
    @Size(max=11) private String phonenumber;
    @NotBlank @Size(min=5,max=20) private String password;
    private Integer agentLevel;
}
