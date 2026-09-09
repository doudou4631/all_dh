package com.geek.framework.web.service;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TokenServiceTest {

    @Test
    void invalidTokenIsTreatedAsUnauthenticated() {
        TokenService tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

        assertNull(tokenService.getLoginUser("Bearer invalid-token"));
    }
}
