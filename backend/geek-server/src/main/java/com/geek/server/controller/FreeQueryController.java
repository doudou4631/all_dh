package com.geek.server.controller;

import com.geek.common.annotation.Anonymous;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.utils.ip.IpUtils;
import com.geek.server.service.IFreeQueryService;
import com.geek.server.service.IFreeQueryStatisticsService;
import com.geek.server.service.IFreeQueryIpPolicyService;
import com.geek.server.service.IFreeQueryPageConfigService;
import com.geek.server.service.FreeQuerySecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

@RestController
@RequestMapping("/server/freeQuery")
@Tag(name = "免费号码查询")
public class FreeQueryController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(FreeQueryController.class);

    @Autowired
    private IFreeQueryService freeQueryService;

    @Autowired
    private IFreeQueryStatisticsService statisticsService;

    @Autowired
    private IFreeQueryIpPolicyService ipPolicyService;

    @Autowired
    private IFreeQueryPageConfigService pageConfigService;

    @Autowired
    private FreeQuerySecurityService securityService;

    @Value("${free-query.internal-key}")
    private String internalKey;

    @Value("${free-query.max-concurrent:8}")
    private int maxConcurrent;

    private Semaphore queryPermits;

    @PostConstruct
    public void initializeQueryPermits() {
        queryPermits = new Semaphore(Math.max(1, maxConcurrent), true);
    }

    @Anonymous
    @GetMapping("/policy")
    @Operation(summary = "获取免费查询展示延时")
    public AjaxResult policy(HttpServletRequest request, HttpServletResponse response) {
        if (!hasValidInternalKey(request)) {
            return forbidden(response);
        }
        Integer delayMs = ipPolicyService.getConfig().getIntervalMs();
        return success(Map.of("resultDelayMs", delayMs == null ? 0 : delayMs));
    }

    @Anonymous
    @GetMapping("/page-config")
    @Operation(summary = "获取免费查询页面联系方式")
    public AjaxResult pageConfig(@RequestParam(defaultValue = "1") String code,
                                 HttpServletRequest request, HttpServletResponse response) {
        if (!hasValidInternalKey(request)) {
            return forbidden(response);
        }
        try {
            var config = pageConfigService.getConfig(code);
            return success(Map.of(
                    "pageCode", config.getPageCode(),
                    "phone", config.getPhone(),
                    "qrCode", config.getQrCode(),
                    "showOnlineService", Boolean.TRUE.equals(config.getShowOnlineService())));
        } catch (IllegalArgumentException ex) {
            return error(ex.getMessage());
        }
    }

    @Anonymous
    @PostMapping("/challenge")
    @Operation(summary = "获取免费查询一次性安全令牌")
    public AjaxResult challenge(@RequestBody Map<String, String> request, HttpServletRequest servletRequest,
                                HttpServletResponse servletResponse) {
        if (!hasValidInternalKey(servletRequest)) {
            return forbidden(servletResponse);
        }
        String fingerprint = request == null ? null : request.get("fingerprint");
        String visitorId = servletRequest.getHeader("X-Visitor-Id");
        if (!isValidFingerprint(fingerprint) || !isValidVisitorId(visitorId)) {
            return error("无法识别查询设备，请刷新页面后重试");
        }
        FreeQuerySecurityService.IssueResult issued = securityService.issueToken(
                securityContext(servletRequest, visitorId, fingerprint));
        if (issued.error() != null) {
            return error(issued.error());
        }
        return success(Map.of(
                "token", issued.token(),
                "expiresAt", issued.expiresAt(),
                "expiresIn", Math.max(0, (issued.expiresAt() - System.currentTimeMillis()) / 1000)));
    }

    @Anonymous
    @PostMapping("/single")
    @Operation(summary = "免费查询号码标记")
    public AjaxResult single(@RequestBody Map<String, String> request, HttpServletRequest servletRequest,
                             HttpServletResponse servletResponse) {
        if (!hasValidInternalKey(servletRequest)) {
            return forbidden(servletResponse);
        }
        String phoneNumber = request == null ? null : request.get("phoneNumber");
        if (phoneNumber == null || !phoneNumber.matches("^(1\\d{10}|0\\d{2,3}\\d{7,8})$")) {
            return error("请输入正确的手机号码或座机号码");
        }

        String ipAddr = resolveClientIp(servletRequest);
        String visitorId = servletRequest.getHeader("X-Visitor-Id");
        String fingerprint = servletRequest.getHeader("X-Device-Fingerprint");
        String queryToken = servletRequest.getHeader("X-Free-Query-Token");
        if (!isValidFingerprint(fingerprint) || !isValidVisitorId(visitorId)) {
            return error("无法识别查询设备，请刷新页面后重试");
        }
        String visitorKey = visitorId != null && visitorId.matches("^[A-Za-z0-9_-]{8,64}$")
                ? "device:" + visitorId
                : "ip:" + ipAddr;
        String userAgent = servletRequest.getHeader("User-Agent");
        long startTime = System.currentTimeMillis();
        ReentrantLock accessLock = ipPolicyService.lockFor(ipAddr);
        boolean permitAcquired = false;
        accessLock.lock();

        try {
            String denialMessage = ipPolicyService.validateAccess(ipAddr);
            if (denialMessage != null) {
                return error(denialMessage);
            }
            Integer configuredDailyLimit = ipPolicyService.getConfig().getDailyLimit();
            int dailyLimit = configuredDailyLimit == null ? 20 : configuredDailyLimit;
            String securityError = securityService.validateAndConsume(
                    queryToken,
                    new FreeQuerySecurityService.SecurityContext(ipAddr, visitorId, fingerprint, userAgent),
                    phoneNumber,
                    dailyLimit);
            if (securityError != null) {
                return error(securityError);
            }
            if (!queryPermits.tryAcquire()) {
                return error("当前查询人数较多，请稍后再试");
            }
            permitAcquired = true;
            Map<String, Object> result = freeQueryService.query(phoneNumber);
            String errorMessage = Boolean.TRUE.equals(result.get("success"))
                    ? null
                    : String.valueOf(result.getOrDefault("message", "查询失败"));
            ipPolicyService.waitForConfiguredDelay(startTime);
            recordStatistics(visitorKey, ipAddr, userAgent, phoneNumber, result,
                    System.currentTimeMillis() - startTime, errorMessage);
            return success(result);
        } catch (Exception ex) {
            ipPolicyService.waitForConfiguredDelay(startTime);
            recordStatistics(visitorKey, ipAddr, userAgent, phoneNumber, null,
                    System.currentTimeMillis() - startTime, ex.getMessage());
            log.error("免费号码查询失败", ex);
            return error("查询服务异常，请稍后重试");
        } finally {
            if (permitAcquired) {
                queryPermits.release();
            }
            accessLock.unlock();
        }
    }

    private boolean hasValidInternalKey(HttpServletRequest request) {
        String provided = request.getHeader("X-Free-Query-Key");
        if (provided == null || internalKey == null) {
            return false;
        }
        return MessageDigest.isEqual(
                internalKey.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        boolean trustedLocalProxy = "127.0.0.1".equals(remoteAddr)
                || "0:0:0:0:0:0:0:1".equals(remoteAddr)
                || "::1".equals(remoteAddr);
        if (trustedLocalProxy) {
            String forwarded = request.getHeader("X-Free-Query-Client-IP");
            if (isValidClientIp(forwarded)) {
                return forwarded;
            }
        }
        return "0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)
                ? "127.0.0.1" : remoteAddr;
    }

    private boolean isValidClientIp(String value) {
        return value != null && value.length() <= 64
                && (IpUtils.isIP(value) || value.matches("^[0-9A-Fa-f:]+$"));
    }

    private boolean isValidFingerprint(String value) {
        return value != null && value.matches("^[0-9a-f]{64}$");
    }

    private boolean isValidVisitorId(String value) {
        return value != null && value.matches("^[A-Za-z0-9_-]{8,64}$");
    }

    private FreeQuerySecurityService.SecurityContext securityContext(HttpServletRequest request,
                                                                      String visitorId,
                                                                      String fingerprint) {
        return new FreeQuerySecurityService.SecurityContext(
                resolveClientIp(request), visitorId, fingerprint, request.getHeader("User-Agent"));
    }

    private AjaxResult forbidden(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return error("禁止直接调用免费查询接口");
    }

    private void recordStatistics(String visitorKey, String ipAddr, String userAgent, String queryPhone,
                                  Map<String, Object> result, long responseTime, String errorMessage) {
        try {
            statisticsService.record(visitorKey, ipAddr, userAgent, queryPhone, result, responseTime, errorMessage);
        } catch (Exception ex) {
            log.error("记录免费查询统计失败", ex);
        }
    }
}
