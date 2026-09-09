package com.geek.server.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MarkOpenApiSecretVO {
    private Long id;
    private String apiKey;
    private String apiSecret;
}
