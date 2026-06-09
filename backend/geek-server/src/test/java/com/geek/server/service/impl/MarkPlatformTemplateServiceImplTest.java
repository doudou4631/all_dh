package com.geek.server.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.common.core.domain.entity.SysRole;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.common.core.domain.model.LoginUser;
import com.geek.common.exception.ServiceException;
import com.geek.server.domain.MarkPlatformTemplate;
import com.geek.server.mapper.MarkPlatformTemplateMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkPlatformTemplateServiceImplTest {

    private static final Long OWNER_USER_ID = 99875802591000131L;
    private static final String OWNER_USERNAME = "110126";

    @Mock
    private MarkPlatformTemplateMapper markPlatformTemplateMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MarkPlatformTemplateServiceImpl markPlatformTemplateService;

    @BeforeEach
    void setUpSecurityContext() {
        setLoginUser(OWNER_USER_ID, OWNER_USERNAME, "agent");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void insertShouldRejectDuplicateTemplateNameForSameOwner() {
        MarkPlatformTemplate request = new MarkPlatformTemplate();
        request.setTemplateName(" 测试 ");

        MarkPlatformTemplate existing = new MarkPlatformTemplate();
        existing.setId(3L);
        existing.setOwnerUserId(OWNER_USER_ID);
        when(markPlatformTemplateMapper.selectMarkPlatformTemplateByOwnerAndName(OWNER_USER_ID, "测试"))
                .thenReturn(existing);

        assertThatThrownBy(() -> markPlatformTemplateService.insertMarkPlatformTemplate(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("模板名称已存在");

        verify(markPlatformTemplateMapper, never()).insertMarkPlatformTemplate(request);
    }

    @Test
    void insertShouldTrimTemplateNameBeforePersist() {
        MarkPlatformTemplate request = new MarkPlatformTemplate();
        request.setTemplateName("  测试模板  ");
        request.setStatus("0");
        request.setIsDefault("0");

        MarkPlatformTemplate ownerDefault = new MarkPlatformTemplate();
        ownerDefault.setId(88L);
        ownerDefault.setOwnerUserId(OWNER_USER_ID);

        when(markPlatformTemplateMapper.selectMarkPlatformTemplateByOwnerAndName(OWNER_USER_ID, "测试模板"))
                .thenReturn(null);
        when(markPlatformTemplateMapper.selectOwnerDefaultTemplate(OWNER_USER_ID)).thenReturn(ownerDefault);
        when(markPlatformTemplateMapper.insertMarkPlatformTemplate(request)).thenReturn(1);

        int rows = markPlatformTemplateService.insertMarkPlatformTemplate(request);

        assertThat(rows).isEqualTo(1);
        ArgumentCaptor<MarkPlatformTemplate> captor = ArgumentCaptor.forClass(MarkPlatformTemplate.class);
        verify(markPlatformTemplateMapper).insertMarkPlatformTemplate(captor.capture());
        assertThat(captor.getValue().getTemplateName()).isEqualTo("测试模板");
    }

    @Test
    void updateShouldRejectRenameToExistingTemplateNameForSameOwner() {
        MarkPlatformTemplate request = new MarkPlatformTemplate();
        request.setId(11L);
        request.setTemplateName("测试");

        MarkPlatformTemplate stored = new MarkPlatformTemplate();
        stored.setId(11L);
        stored.setTemplateName("旧模板");
        stored.setOwnerUserId(OWNER_USER_ID);
        stored.setIsDefault("0");
        when(markPlatformTemplateMapper.selectMarkPlatformTemplateById(11L)).thenReturn(stored);

        MarkPlatformTemplate existing = new MarkPlatformTemplate();
        existing.setId(3L);
        existing.setTemplateName("测试");
        existing.setOwnerUserId(OWNER_USER_ID);
        when(markPlatformTemplateMapper.selectMarkPlatformTemplateByOwnerAndName(OWNER_USER_ID, "测试"))
                .thenReturn(existing);

        assertThatThrownBy(() -> markPlatformTemplateService.updateMarkPlatformTemplate(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("模板名称已存在");

        verify(markPlatformTemplateMapper, never()).updateMarkPlatformTemplate(request);
    }

    @Test
    void insertShouldConvertDuplicateKeyExceptionToServiceException() {
        MarkPlatformTemplate request = new MarkPlatformTemplate();
        request.setTemplateName("测试");

        MarkPlatformTemplate ownerDefault = new MarkPlatformTemplate();
        ownerDefault.setId(88L);
        ownerDefault.setOwnerUserId(OWNER_USER_ID);

        when(markPlatformTemplateMapper.selectMarkPlatformTemplateByOwnerAndName(OWNER_USER_ID, "测试"))
                .thenReturn(null);
        when(markPlatformTemplateMapper.selectOwnerDefaultTemplate(OWNER_USER_ID)).thenReturn(ownerDefault);
        when(markPlatformTemplateMapper.insertMarkPlatformTemplate(request))
                .thenThrow(new DuplicateKeyException(
                        "Duplicate entry '测试' for key 'mark_platform_template.uk_mark_platform_template_owner_name'"));

        assertThatThrownBy(() -> markPlatformTemplateService.insertMarkPlatformTemplate(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("模板名称已存在");
    }

    @Test
    void deleteShouldRejectWhenTemplateIsBoundByUsers() {
        MarkPlatformTemplate stored = new MarkPlatformTemplate();
        stored.setId(3L);
        stored.setTemplateName("测试模板");
        stored.setOwnerUserId(OWNER_USER_ID);

        when(markPlatformTemplateMapper.selectMarkPlatformTemplateById(3L)).thenReturn(stored);
        when(markPlatformTemplateMapper.countActiveUserBindingsByTemplateId(3L)).thenReturn(2L);

        assertThatThrownBy(() -> markPlatformTemplateService.deleteMarkPlatformTemplateByIds(new Long[]{3L}))
                .isInstanceOf(ServiceException.class)
                .hasMessage("模板【测试模板】已绑定2个用户，请先迁移用户模板后再删除");
        verify(markPlatformTemplateMapper, never()).deleteMarkPlatformTemplateByIds(any(Long[].class));
    }

    private void setLoginUser(Long userId, String username, String roleKey) {
        SysRole role = new SysRole();
        role.setRoleKey(roleKey);

        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName(username);
        user.setRoles(Collections.singletonList(role));

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setUser(user);
        loginUser.setPermissions(new HashSet<>());

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
