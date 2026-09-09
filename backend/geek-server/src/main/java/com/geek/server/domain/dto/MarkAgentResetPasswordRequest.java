package com.geek.server.domain.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data public class MarkAgentResetPasswordRequest { @NotNull private Long userId; @NotBlank @Size(min=5,max=20) private String password; }
