package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MarkOpenApiCredentialStatusRequest {
    @NotNull private Long id;
    @NotNull private String status;
}
