package com.geek.server.domain;

import com.geek.common.core.domain.model.LoginUser;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class MarkOpenApiPrincipal {
    private final MarkOpenApiCredential credential;
    private final LoginUser loginUser;
    private final Set<String> scopes;
    private final Set<String> platformCodes;
}
