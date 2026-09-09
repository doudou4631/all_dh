package com.geek.server.domain.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data public class MarkAgentAccountStatusRequest { @NotNull private Long userId; private String status; }
