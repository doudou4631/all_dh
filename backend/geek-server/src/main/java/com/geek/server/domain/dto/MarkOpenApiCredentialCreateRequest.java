package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class MarkOpenApiCredentialCreateRequest {
    @NotBlank private String name;
    @NotBlank private String userName;
    @NotEmpty private List<String> platformCodes;
    @NotEmpty private List<String> scopes;
    private Date expireTime;
}
