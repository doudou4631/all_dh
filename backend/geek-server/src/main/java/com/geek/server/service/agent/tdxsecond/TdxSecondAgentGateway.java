package com.geek.server.service.agent.tdxsecond;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.common.exception.ServiceException;
import com.geek.server.domain.vo.MarkTdxSecondPrecheckVO;
import com.geek.server.domain.vo.MarkTdxSecondSendCodeResultVO;
import com.geek.server.domain.vo.MarkTdxSecondSubmitResultVO;
import com.geek.server.domain.vo.MarkTdxSecondTaskStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** 后台代理处理：封装Taidixiong二次全部第三方调用。 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TdxSecondAgentGateway {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final String PHONE_CHECK_URL = "https://www.teddymobile.cn/api/phone/check";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${tdx.second.enabled:false}")
    private boolean enabled;

    @Value("${tdx.second.base-url:http://43.143.254.68:4300}")
    private String baseUrl;

    @Value("${tdx.second.integration-token:}")
    private String integrationToken;

    @Value("${tdx.second.timeout-ms:120000}")
    private long timeoutMs;

    public MarkTdxSecondPrecheckVO precheck(String phone) {
        ensureReady();
        Map<String, Object> appealResponse = postTdx("/api/tdx/sms-receive/precheck", Map.of("phone", phone));
        Map<String, Object> appealData = asMap(appealResponse.get("data"));
        if (!asBoolean(appealResponse.get("success")) || !asBoolean(appealData.get("allowed"))) {
            throw new ServiceException(messageOf(appealResponse, "号码暂不符合二次申诉条件"));
        }

        Map<String, Object> phoneCheck = postJson(PHONE_CHECK_URL, Map.of("phone", phone), false);
        int phoneCheckCode = asInt(phoneCheck.get("code"), -1);
        String phoneCheckMessage = messageOf(phoneCheck, "号码状态检查未通过");
        if (phoneCheckCode != 200 && !isSameDayRepeatAppealAllowed(phoneCheckMessage)) {
            throw new ServiceException(phoneCheckMessage(phoneCheckCode, phoneCheckMessage));
        }

        MarkTdxSecondPrecheckVO result = new MarkTdxSecondPrecheckVO();
        result.setPhone(phone);
        result.setOk(true);
        result.setAllowed(true);
        result.setStatus(isSameDayRepeatAppealAllowed(phoneCheckMessage) ? "可继续处理" : "已标记，可申诉");
        result.setMessage(isSameDayRepeatAppealAllowed(phoneCheckMessage) ? "号码当天已提交过申诉，可继续处理" : "号码预检查通过");
        result.setPhoneCheckCode(phoneCheckCode);
        return result;
    }

    public MarkTdxSecondSendCodeResultVO sendCode(String phone, String line) {
        MarkTdxSecondPrecheckVO precheck = precheck(phone);
        String targetLine = normalizeLine(line);
        Map<String, Object> response = postTdx("/api/tdx/sms-receive/send-code", Map.of(
                "phone", phone,
                "line", targetLine
        ));
        if (!asBoolean(response.get("success"))) {
            throw new ServiceException(messageOf(response, "验证码发送失败，请稍后重试"));
        }
        MarkTdxSecondSendCodeResultVO result = new MarkTdxSecondSendCodeResultVO();
        result.setPhone(phone);
        result.setLine(targetLine);
        result.setMessage(messageOf(response, "短信验证码已发起发送"));
        result.setPhoneCheckCode(precheck.getPhoneCheckCode());
        return result;
    }

    public MarkTdxSecondSubmitResultVO submit(String phone, String smsCode, String line, Boolean rotate) {
        ensureReady();
        Map<String, Object> response = postTdx("/api/tdx/sms-receive/login", Map.of(
                "phone", phone,
                "smsCode", smsCode,
                "line", normalizeLine(line),
                "rotate", Boolean.TRUE.equals(rotate)
        ));
        if (!asBoolean(response.get("success"))) {
            throw new ServiceException(messageOf(response, "验证登录失败，请核对手机号和验证码后重试"));
        }
        Map<String, Object> data = asMap(response.get("data"));
        MarkTdxSecondSubmitResultVO result = new MarkTdxSecondSubmitResultVO();
        result.setPhone(StringUtils.defaultIfBlank(stringValue(data.get("phone")), phone));
        result.setTdxId(asLong(data.get("id")));
        result.setOrderpicinumber(stringValue(data.get("orderpicinumber")));
        result.setTaskId(stringValue(data.get("autoCleanupTaskId")));
        result.setUpstreamStatus(StringUtils.defaultIfBlank(stringValue(data.get("autoCleanupStatus")), "processing"));
        result.setRefundSuggested(asBoolean(data.get("autoCleanupRefunded")));
        result.setMessage(StringUtils.defaultIfBlank(stringValue(data.get("autoCleanupMessage")),
                messageOf(response, "验证码登录验证通过，已开始后台申诉清理")));
        result.setProcessStatus("completed".equalsIgnoreCase(result.getUpstreamStatus()) ? "1"
                : "failed".equalsIgnoreCase(result.getUpstreamStatus()) ? "2" : "3");
        return result;
    }

    public MarkTdxSecondTaskStatusVO syncTask(String taskId) {
        ensureReady();
        if (StringUtils.isBlank(taskId)) throw new ServiceException("Taidixiong二次任务号缺失");
        Map<String, Object> response = postTdx("/api/tdx/sms-receive/tasks/" + taskId.trim() + "/sync", Map.of());
        if (!asBoolean(response.get("success"))) {
            throw new ServiceException(messageOf(response, "Taidixiong二次任务状态同步失败"));
        }
        Map<String, Object> data = asMap(response.get("data"));
        MarkTdxSecondTaskStatusVO result = new MarkTdxSecondTaskStatusVO();
        result.setTaskId(StringUtils.defaultIfBlank(stringValue(data.get("taskId")), taskId));
        result.setStatus(StringUtils.defaultIfBlank(stringValue(data.get("status")), "processing"));
        result.setRawStatus(stringValue(data.get("rawStatus")));
        result.setMessage(StringUtils.defaultIfBlank(stringValue(data.get("message")), "申诉已提交，等待处理"));
        result.setRefundSuggested(asBoolean(data.get("refundSuggested")));
        result.setAttempts(asInt(data.get("attempts"), 0));
        return result;
    }

    public MarkTdxSecondTaskStatusVO syncTaskImmediately(String taskId) {
        return syncTask(taskId);
    }

    private Map<String, Object> postTdx(String path, Map<String, Object> payload) {
        ensureReady();
        return postJson(StringUtils.removeEnd(baseUrl.trim(), "/") + path, payload, true);
    }

    private Map<String, Object> postJson(String url, Map<String, Object> payload, boolean authenticated) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(Math.max(1000L, timeoutMs)))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8));
            if (authenticated) {
                builder.header("X-TDX-Integration-Token", integrationToken.trim());
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Map<String, Object> body = parseBody(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ServiceException(messageOf(body, "Taidixiong二次上游返回异常（" + response.statusCode() + "）"));
            }
            return body;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Taidixiong二次后台代理请求失败 url={}", url, e);
            throw new ServiceException("Taidixiong二次服务暂时不可用，请稍后重试");
        }
    }

    private void ensureReady() {
        if (!enabled) throw new ServiceException("Taidixiong二次后台代理未启用");
        if (StringUtils.isBlank(integrationToken)) {
            throw new ServiceException("Taidixiong二次后台代理集成Token未配置");
        }
    }

    private String normalizeLine(String line) {
        String target = StringUtils.defaultIfBlank(StringUtils.trimToNull(line), "line1");
        if (!"line1".equals(target) && !"line2".equals(target)) throw new ServiceException("通道线路仅支持line1或line2");
        return target;
    }

    private String phoneCheckMessage(int code, String upstream) {
        return switch (code) {
            case 1004, 1005 -> "该号码属于泰迪熊高频，暂不支持二次申诉";
            case 2 -> "泰迪未来标记已取消，部分手机端可能需要3至5个工作日同步，请耐心等待";
            default -> StringUtils.defaultIfBlank(upstream, "该号码当前不满足二次申诉条件");
        };
    }

    private boolean isSameDayRepeatAppealAllowed(String message) {
        String text = StringUtils.trimToEmpty(message);
        return StringUtils.containsAny(text,
                "当天已提交过申诉",
                "今天已提交过申诉",
                "今日已提交过申诉",
                "不可重复提交");
    }

    private Map<String, Object> parseBody(String body) throws Exception {
        if (StringUtils.isBlank(body)) return new LinkedHashMap<>();
        return objectMapper.readValue(body, MAP_TYPE);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : new LinkedHashMap<>();
    }

    private String messageOf(Map<String, Object> body, String fallback) {
        return StringUtils.defaultIfBlank(stringValue(body.get("message")),
                StringUtils.defaultIfBlank(stringValue(body.get("msg")), fallback));
    }

    private String stringValue(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private boolean asBoolean(Object value) { return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(stringValue(value)); }
    private int asInt(Object value, int fallback) { try { return Integer.parseInt(stringValue(value)); } catch (Exception e) { return fallback; } }
    private Long asLong(Object value) { try { return Long.parseLong(stringValue(value)); } catch (Exception e) { return null; } }
}
