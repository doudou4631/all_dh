package com.geek.server.service;

import com.geek.common.core.domain.model.LoginUser;
import com.geek.common.exception.ServiceException;
import com.geek.server.domain.MarkOpenApiCredential;
import com.geek.server.domain.MarkOpenApiPrincipal;
import com.geek.server.domain.dto.MarkOpenApiCredentialCreateRequest;
import com.geek.server.domain.vo.MarkOpenApiSecretVO;
import com.geek.server.mapper.MarkOpenApiCredentialMapper;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.system.service.ISysMenuService;
import com.geek.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MarkOpenApiCredentialService {
    public static final String SCOPE_READ = "MARK_READ";
    public static final String SCOPE_FEEDBACK = "MARK_FEEDBACK";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private final MarkOpenApiCredentialMapper mapper;
    private final ISysUserService userService;
    private final ISysMenuService menuService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<MarkOpenApiCredential> list(String name, String status) { return mapper.selectList(StringUtils.trimToNull(name), StringUtils.trimToNull(status)); }

    @Transactional(rollbackFor = Exception.class)
    public MarkOpenApiSecretVO create(MarkOpenApiCredentialCreateRequest request) {
        SysUser user = userService.selectUserByUserName(StringUtils.trim(request.getUserName()));
        if (user == null) throw new ServiceException("绑定用户不存在");
        MarkOpenApiCredential c = new MarkOpenApiCredential();
        c.setName(StringUtils.trim(request.getName())); c.setApiKey("ak_" + random(18));
        // The secret is returned once and only its BCrypt hash is persisted.
        String secret = random(32); c.setSecretHash(passwordEncoder.encode(secret)); c.setUserId(user.getUserId()); c.setStatus("0");
        c.setScopes(String.join(",", normalizeScopes(request.getScopes()))); c.setExpireTime(request.getExpireTime()); mapper.insert(c);
        replacePlatforms(c.getId(), request.getPlatformCodes()); return new MarkOpenApiSecretVO(c.getId(), c.getApiKey(), secret);
    }

    @Transactional(rollbackFor = Exception.class)
    public MarkOpenApiSecretVO rotate(Long id) {
        MarkOpenApiCredential c = require(id); String secret = random(32); mapper.updateSecret(id, passwordEncoder.encode(secret)); return new MarkOpenApiSecretVO(id, c.getApiKey(), secret);
    }
    public int updateStatus(Long id, String status) { require(id); if (!"0".equals(status) && !"1".equals(status)) throw new ServiceException("状态无效"); return mapper.updateStatus(id, status); }
    public MarkOpenApiPrincipal authenticate(String apiKey, String secret) {
        if (StringUtils.isBlank(apiKey) || StringUtils.isBlank(secret)) throw new ServiceException("缺少 API Key 或 Secret");
        MarkOpenApiCredential c = mapper.selectByApiKey(apiKey); if (c == null || !"0".equals(c.getStatus())) throw new ServiceException("API Key 无效或已停用");
        if (c.getExpireTime() != null && c.getExpireTime().before(new Date())) throw new ServiceException("API Key 已过期");
        if (!passwordEncoder.matches(secret, c.getSecretHash())) throw new ServiceException("API Secret 无效");
        SysUser boundUser = userService.selectUserById(c.getUserId());
        if (boundUser == null || Integer.valueOf(2).equals(boundUser.getDelFlag()) || !"0".equals(boundUser.getStatus())) {
            throw new ServiceException("绑定账号不可用");
        }
        Set<String> permissions = boundUser.isAdmin()
                ? new LinkedHashSet<>(Set.of("*:*:*"))
                : new LinkedHashSet<>(menuService.selectMenuPermsByUserId(boundUser.getUserId()));
        LoginUser user = new LoginUser(boundUser.getUserId(), boundUser.getDeptId(), boundUser, permissions);
        mapper.updateLastUsed(c.getId());
        return new MarkOpenApiPrincipal(c, user, split(c.getScopes()), new LinkedHashSet<>(mapper.selectPlatforms(c.getId())));
    }
    public void requireScope(MarkOpenApiPrincipal principal, String scope) { if (!principal.getScopes().contains(scope)) throw new ServiceException("API Key 没有该接口权限"); }
    public void requirePlatform(MarkOpenApiPrincipal principal, String platform) { if (StringUtils.isBlank(platform) || !principal.getPlatformCodes().contains(platform.toLowerCase())) throw new ServiceException("API Key 无权处理该平台"); }
    public String itemPlatform(Long itemId) { return mapper.selectItemPlatform(itemId); }
    public void log(MarkOpenApiPrincipal p, String path, String method, String platform, boolean success, String message, long elapsed) { mapper.insertAccessLog(p.getCredential().getId(), p.getCredential().getApiKey(), path, method, platform, success, StringUtils.left(message, 255), elapsed); }
    private MarkOpenApiCredential require(Long id) { MarkOpenApiCredential c = mapper.selectById(id); if (c == null) throw new ServiceException("API Key 不存在"); return c; }
    private void replacePlatforms(Long id, List<String> platforms) { mapper.deletePlatforms(id); for (String p : platforms) { if (StringUtils.isNotBlank(p)) mapper.insertPlatform(id, p.trim().toLowerCase()); } }
    private Set<String> normalizeScopes(List<String> scopes) { Set<String> set = new LinkedHashSet<>(); for (String s : scopes) if (SCOPE_READ.equals(s) || SCOPE_FEEDBACK.equals(s)) set.add(s); if (set.isEmpty()) throw new ServiceException("至少选择一个权限范围"); return set; }
    private Set<String> split(String value) { return new LinkedHashSet<>(Arrays.asList(StringUtils.defaultString(value).split(","))); }
    private String random(int bytes) { byte[] value = new byte[bytes]; RANDOM.nextBytes(value); return URL_ENCODER.encodeToString(value); }
}
