package com.geek.server.domain.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data public class MarkAgentPointOperationRequest {
    @NotBlank private String requestNo;
    @NotNull private Long targetUserId;
    @NotBlank private String platformCode;
    private String platformName;
    @NotNull @Min(1) private Long amount;
    private String reason;
}
