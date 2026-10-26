package com.geek.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.common.core.domain.entity.SysRole;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.common.exception.ServiceException;
import com.geek.server.domain.MarkOpenApiCredential;
import com.geek.server.domain.MarkOpenApiPrincipal;
import com.geek.server.domain.dto.MarkCustomerApiCredentialCreateRequest;
import com.geek.server.domain.vo.MarkOpenApiSecretVO;
import com.geek.server.mapper.MarkOpenApiCredentialMapper;
import com.geek.system.service.ISysMenuService;
import com.geek.system.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Set;
import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarkOpenApiCredentialServiceTest {

    @Test
    void deletesCustomerCredentialAndPlatformGrantsOnly() {
        MarkOpenApiCredentialMapper mapper = mock(MarkOpenApiCredentialMapper.class);
        MarkOpenApiCredentialService service = new MarkOpenApiCredentialService(
                mapper, mock(ISysUserService.class), mock(ISysMenuService.class));
        MarkOpenApiCredential credential = new MarkOpenApiCredential();
        credential.setId(19L);
        credential.setCredentialType(MarkOpenApiCredentialService.TYPE_CUSTOMER);
        when(mapper.selectById(19L)).thenReturn(credential);
        when(mapper.deleteCustomer(19L)).thenReturn(1);

        assertEquals(1, service.deleteCustomer(19L));

        verify(mapper).selectById(19L);
        verify(mapper).deletePlatforms(19L);
        verify(mapper).deleteCustomer(19L);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void refusesToDeleteProcessingCredentialThroughCustomerManagement() {
        MarkOpenApiCredentialMapper mapper = mock(MarkOpenApiCredentialMapper.class);
        MarkOpenApiCredentialService service = new MarkOpenApiCredentialService(
                mapper, mock(ISysUserService.class), mock(ISysMenuService.class));
        MarkOpenApiCredential credential = new MarkOpenApiCredential();
        credential.setCredentialType(MarkOpenApiCredentialService.TYPE_PROCESSING);
        when(mapper.selectById(19L)).thenReturn(credential);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.deleteCustomer(19L));

        assertEquals("API Key 类型不匹配", exception.getMessage());
        verify(mapper).selectById(19L);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void refusesToDeleteMissingCredential() {
        MarkOpenApiCredentialMapper mapper = mock(MarkOpenApiCredentialMapper.class);
        MarkOpenApiCredentialService service = new MarkOpenApiCredentialService(
                mapper, mock(ISysUserService.class), mock(ISysMenuService.class));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.deleteCustomer(19L));

        assertEquals("API Key 不存在", exception.getMessage());
        verify(mapper).selectById(19L);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void acceptsCustomerApiDatePickerFormat() throws Exception {
        MarkCustomerApiCredentialCreateRequest request = new ObjectMapper().readValue(
                "{\"name\":\"客户一号\",\"userName\":\"customer001\",\"assignedAgentId\":88,\"platformCodes\":[\"ltgj\"],\"expireTime\":\"2026-11-27 00:00:00\"}",
                MarkCustomerApiCredentialCreateRequest.class);

        assertEquals("2026-11-27 00:00:00", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(request.getExpireTime()));
    }

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

    @Test
    void createsCustomerCredentialWithBoundAgentAndPlatforms() {
        MarkOpenApiCredentialMapper mapper = mock(MarkOpenApiCredentialMapper.class);
        ISysUserService userService = mock(ISysUserService.class);
        ISysMenuService menuService = mock(ISysMenuService.class);
        MarkOpenApiCredentialService service = new MarkOpenApiCredentialService(mapper, userService, menuService);

        SysUser customer = new SysUser();
        customer.setUserId(125L);
        customer.setUserName("customer001");
        customer.setStatus("0");
        customer.setDelFlag(0);
        when(userService.selectUserByUserName("customer001")).thenReturn(customer);
        when(mapper.countActiveAgent(88L)).thenReturn(1);
        doAnswer(invocation -> {
            ((MarkOpenApiCredential) invocation.getArgument(0)).setId(19L);
            return 1;
        }).when(mapper).insert(any(MarkOpenApiCredential.class));

        MarkCustomerApiCredentialCreateRequest request = new MarkCustomerApiCredentialCreateRequest();
        request.setName("客户一号");
        request.setUserName("customer001");
        request.setAssignedAgentId(88L);
        request.setPlatformCodes(List.of("ltgj", "xiaomi", "ltgj"));

        MarkOpenApiSecretVO secret = service.createCustomer(request);

        org.mockito.ArgumentCaptor<MarkOpenApiCredential> credentialCaptor = org.mockito.ArgumentCaptor.forClass(MarkOpenApiCredential.class);
        verify(mapper).insert(credentialCaptor.capture());
        MarkOpenApiCredential created = credentialCaptor.getValue();
        assertEquals(MarkOpenApiCredentialService.TYPE_CUSTOMER, created.getCredentialType());
        assertEquals(125L, created.getUserId());
        assertEquals(88L, created.getAssignedAgentId());
        assertEquals("MARK_CUSTOMER_SUBMIT,MARK_CUSTOMER_READ", created.getScopes());
        assertEquals(19L, secret.getId());
        assertTrue(secret.getApiKey().startsWith("ak_"));
        assertTrue(secret.getApiSecret().length() > 20);
        verify(mapper).insertPlatform(19L, "ltgj");
        verify(mapper).insertPlatform(19L, "xiaomi");
    }

    @Test
    void customerCredentialCannotUseProcessingApiType() {
        MarkOpenApiCredentialMapper mapper = mock(MarkOpenApiCredentialMapper.class);
        ISysUserService userService = mock(ISysUserService.class);
        ISysMenuService menuService = mock(ISysMenuService.class);
        MarkOpenApiCredentialService service = new MarkOpenApiCredentialService(mapper, userService, menuService);

        MarkOpenApiCredential credential = new MarkOpenApiCredential();
        credential.setCredentialType(MarkOpenApiCredentialService.TYPE_CUSTOMER);
        MarkOpenApiPrincipal principal = new MarkOpenApiPrincipal(credential, null, Set.of(), Set.of());

        assertThrows(ServiceException.class, () -> service.requireCredentialType(
                principal, MarkOpenApiCredentialService.TYPE_PROCESSING));
    }
}
