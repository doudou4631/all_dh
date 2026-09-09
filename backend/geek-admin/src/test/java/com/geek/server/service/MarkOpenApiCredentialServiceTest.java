package com.geek.server.service;

import com.geek.common.core.domain.entity.SysRole;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.server.domain.MarkOpenApiCredential;
import com.geek.server.domain.MarkOpenApiPrincipal;
import com.geek.server.mapper.MarkOpenApiCredentialMapper;
import com.geek.system.service.ISysMenuService;
import com.geek.system.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarkOpenApiCredentialServiceTest {

    @Test
    void authenticatesBoundUserWithoutPasswordLoginContext() {
        MarkOpenApiCredentialMapper mapper = mock(MarkOpenApiCredentialMapper.class);
        ISysUserService userService = mock(ISysUserService.class);
        ISysMenuService menuService = mock(ISysMenuService.class);
        MarkOpenApiCredentialService service = new MarkOpenApiCredentialService(mapper, userService, menuService);

        String secret = "test-api-secret";
        MarkOpenApiCredential credential = new MarkOpenApiCredential();
        credential.setId(7L);
        credential.setApiKey("ak_test");
        credential.setSecretHash(new BCryptPasswordEncoder().encode(secret));
        credential.setUserId(125L);
        credential.setStatus("0");
        credential.setScopes("MARK_READ");

        SysRole role = new SysRole();
        role.setRoleKey("mark_agent");
        SysUser user = new SysUser();
        user.setUserId(125L);
        user.setDeptId(10L);
        user.setUserName("110125");
        user.setStatus("0");
        user.setDelFlag(0);
        user.setRoles(List.of(role));

        when(mapper.selectByApiKey("ak_test")).thenReturn(credential);
        when(userService.selectUserById(125L)).thenReturn(user);
        when(menuService.selectMenuPermsByUserId(125L)).thenReturn(Set.of("server:markAgent:order:query"));
        when(mapper.selectPlatforms(7L)).thenReturn(List.of("td_gaopin"));

        MarkOpenApiPrincipal principal = service.authenticate("ak_test", secret);

        assertEquals(125L, principal.getLoginUser().getUserId());
        assertEquals(Set.of("MARK_READ"), principal.getScopes());
        assertEquals(Set.of("td_gaopin"), principal.getPlatformCodes());
        assertTrue(principal.getLoginUser().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_mark_agent".equals(authority.getAuthority())));
        assertTrue(principal.getLoginUser().getAuthorities().stream()
                .anyMatch(authority -> "server:markAgent:order:query".equals(authority.getAuthority())));
        verify(mapper).updateLastUsed(7L);
    }
}
