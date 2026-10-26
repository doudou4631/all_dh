package com.geek.server.service;

import com.geek.common.core.domain.model.LoginUser;
import com.geek.common.exception.ServiceException;
import com.geek.server.domain.MarkOpenApiCredential;
import com.geek.server.domain.MarkOpenApiPrincipal;
import com.geek.server.domain.dto.MarkCustomerApiCredentialCreateRequest;
import com.geek.server.domain.dto.MarkCustomerApiCredentialUpdateRequest;
import com.geek.server.domain.dto.MarkOpenApiCredentialCreateRequest;
import com.geek.server.domain.vo.MarkCustomerApiAgentOptionVO;
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
    public static final String TYPE_PROCESSING = "PROCESSING";
    public static final String TYPE_CUSTOMER = "CUSTOMER";
    public static final String SCOPE_READ = "MARK_READ";
    public static final String SCOPE_FEEDBACK = "MARK_FEEDBACK";
    public static final String SCOPE_CUSTOMER_SUBMIT = "MARK_CUSTOMER_SUBMIT";
    public static final String SCOPE_CUSTOMER_READ = "MARK_CUSTOMER_READ";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private final MarkOpenApiCredentialMapper mapper;
    private final ISysUserService userService;
    private final ISysMenuService menuService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<MarkOpenApiCredential> list(String name, String status) {
        return withPlatforms(mapper.selectList(StringUtils.trimToNull(name), StringUtils.trimToNull(status), TYPE_PROCESSING));
    }

    public List<MarkOpenApiCredential> listCustomer(String name, String status) {
        return withPlatforms(mapper.selectList(StringUtils.trimToNull(name), StringUtils.trimToNull(status), TYPE_CUSTOMER));
    }

    public List<MarkCustomerApiAgentOptionVO> listActiveAgents() {
        return mapper.selectActiveAgents();
    }

    @Transactional(rollbackFor = Exception.class)
    public MarkOpenApiSecretVO create(MarkOpenApiCredentialCreateRequest request) {
        SysUser user = userService.selectUserByUserName(StringUtils.trim(request.getUserName()));
        if (user == null) throw new ServiceException("绑定用户不存在");
        MarkOpenApiCredential c = new MarkOpenApiCredential();
        c.setName(StringUtils.trim(request.getName())); c.setApiKey("ak_" + random(18));
        // The secret is returned once and only its BCrypt hash is persisted.
        String secret = random(32); c.setSecretHash(passwordEncoder.encode(secret)); c.setUserId(user.getUserId()); c.setCredentialType(TYPE_PROCESSING); c.setStatus("0");
        c.setScopes(String.join(",", normalizeScopes(request.getScopes()))); c.setExpireTime(request.getExpireTime()); mapper.insert(c);
        replacePlatforms(c.getId(), request.getPlatformCodes()); return new MarkOpenApiSecretVO(c.getId(), c.getApiKey(), secret);
    }

    @Transactional(rollbackFor = Exception.class)
    public MarkOpenApiSecretVO createCustomer(MarkCustomerApiCredentialCreateRequest request) {
        SysUser customer = requireEnabledUser(request.getUserName(), "绑定客户账号不存在或已停用");
        validateActiveAgent(request.getAssignedAgentId());
        MarkOpenApiCredential credential = new MarkOpenApiCredential();
        credential.setName(StringUtils.trim(request.getName()));
        credential.setApiKey("ak_" + random(18));
        String secret = random(32);
        credential.setSecretHash(passwordEncoder.encode(secret));
        credential.setUserId(customer.getUserId());
        credential.setCredentialType(TYPE_CUSTOMER);
        credential.setAssignedAgentId(request.getAssignedAgentId());
        credential.setStatus("0");
        credential.setScopes(String.join(",", List.of(SCOPE_CUSTOMER_SUBMIT, SCOPE_CUSTOMER_READ)));
        credential.setExpireTime(request.getExpireTime());
        mapper.insert(credential);
        replacePlatforms(credential.getId(), request.getPlatformCodes());
        return new MarkOpenApiSecretVO(credential.getId(), credential.getApiKey(), secret);
    }

    @Transactional(rollbackFor = Exception.class)
    public int updateCustomer(MarkCustomerApiCredentialUpdateRequest request) {
        MarkOpenApiCredential credential = requireType(request.getId(), TYPE_CUSTOMER);
        validateActiveAgent(request.getAssignedAgentId());
        credential.setName(StringUtils.trim(request.getName()));
        credential.setAssignedAgentId(request.getAssignedAgentId());
        credential.setExpireTime(request.getExpireTime());
        int updated = mapper.updateCustomer(credential);
        replacePlatforms(credential.getId(), request.getPlatformCodes());
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteCustomer(Long id) {
        requireType(id, TYPE_CUSTOMER);
        mapper.deletePlatforms(id);
        int deleted = mapper.deleteCustomer(id);
        if (deleted != 1) throw new ServiceException("API Key 不存在或已删除");
        return deleted;
    }

    @Transactional(rollbackFor = Exception.class)
    public MarkOpenApiSecretVO rotate(Long id) {
        return rotate(id, TYPE_PROCESSING);
    }
    public MarkOpenApiSecretVO rotateCustomer(Long id) {
        return rotate(id, TYPE_CUSTOMER);
    }
    private MarkOpenApiSecretVO rotate(Long id, String type) {
        MarkOpenApiCredential c = requireType(id, type); String secret = random(32); mapper.updateSecret(id, passwordEncoder.encode(secret)); return new MarkOpenApiSecretVO(id, c.getApiKey(), secret);
    }
    public int updateStatus(Long id, String status) { return updateStatus(id, status, TYPE_PROCESSING); }
    public int updateCustomerStatus(Long id, String status) { return updateStatus(id, status, TYPE_CUSTOMER); }
    private int updateStatus(Long id, String status, String type) { requireType(id, type); if (!"0".equals(status) && !"1".equals(status)) throw new ServiceException("状态无效"); return mapper.updateStatus(id, status); }
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
    public void requireCredentialType(MarkOpenApiPrincipal principal, String type) { if (principal == null || !StringUtils.equals(type, principal.getCredential().getCredentialType())) throw new ServiceException("API Key 类型无权调用该接口"); }
    public void requireActiveAssignedAgent(MarkOpenApiPrincipal principal) {
        if (principal == null || principal.getCredential() == null) throw new ServiceException("API 身份认证失败");
        validateActiveAgent(principal.getCredential().getAssignedAgentId());
    }
    public void requirePlatform(MarkOpenApiPrincipal principal, String platform) { if (StringUtils.isBlank(platform) || !principal.getPlatformCodes().contains(platform.toLowerCase())) throw new ServiceException("API Key 无权处理该平台"); }
    public String itemPlatform(Long itemId) { return mapper.selectItemPlatform(itemId); }
    public void log(MarkOpenApiPrincipal p, String path, String method, String platform, boolean success, String message, long elapsed) { mapper.insertAccessLog(p.getCredential().getId(), p.getCredential().getApiKey(), path, method, platform, success, StringUtils.left(message, 255), elapsed); }
    private MarkOpenApiCredential require(Long id) { MarkOpenApiCredential c = mapper.selectById(id); if (c == null) throw new ServiceException("API Key 不存在"); return c; }
    private MarkOpenApiCredential requireType(Long id, String type) { MarkOpenApiCredential credential = require(id); if (!StringUtils.equals(type, credential.getCredentialType())) throw new ServiceException("API Key 类型不匹配"); return credential; }
    private SysUser requireEnabledUser(String username, String message) { SysUser user = userService.selectUserByUserName(StringUtils.trim(username)); if (user == null || Integer.valueOf(2).equals(user.getDelFlag()) || !"0".equals(user.getStatus())) throw new ServiceException(message); return user; }
    private void validateActiveAgent(Long agentUserId) { if (agentUserId == null || mapper.countActiveAgent(agentUserId) <= 0) throw new ServiceException("责任代理不存在、已停用或不是代理账号"); }
    private List<MarkOpenApiCredential> withPlatforms(List<MarkOpenApiCredential> credentials) { for (MarkOpenApiCredential credential : credentials) credential.setPlatformCodes(mapper.selectPlatforms(credential.getId())); return credentials; }
    private void replacePlatforms(Long id, List<String> platforms) { Set<String> codes = normalizePlatformCodes(platforms); mapper.deletePlatforms(id); for (String p : codes) mapper.insertPlatform(id, p); }
    private Set<String> normalizePlatformCodes(List<String> platforms) { Set<String> codes = new LinkedHashSet<>(); if (platforms != null) for (String platform : platforms) if (StringUtils.isNotBlank(platform)) codes.add(platform.trim().toLowerCase()); if (codes.isEmpty()) throw new ServiceException("至少选择一个平台"); return codes; }
    private Set<String> normalizeScopes(List<String> scopes) { Set<String> set = new LinkedHashSet<>(); for (String s : scopes) if (SCOPE_READ.equals(s) || SCOPE_FEEDBACK.equals(s)) set.add(s); if (set.isEmpty()) throw new ServiceException("至少选择一个权限范围"); return set; }
    private Set<String> split(String value) { return new LinkedHashSet<>(Arrays.asList(StringUtils.defaultString(value).split(","))); }
    private String random(int bytes) { byte[] value = new byte[bytes]; RANDOM.nextBytes(value); return URL_ENCODER.encodeToString(value); }
}
