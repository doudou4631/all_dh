package com.geek.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.common.exception.ServiceException;
import com.geek.common.utils.StringUtils;
import com.geek.server.domain.vo.MarkTdxSecondSendCodeResultVO;
import com.geek.server.domain.vo.MarkTdxSecondSubmitResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Taidixiong二次申诉外部API调用服务。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TdxSecondAppealService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    @Value("${tdx.second.base-url:http://47.108.132.88:4300}")
    private String baseUrl;

    @Value("${tdx.second.token:}")
    private String token;

    @Value("${tdx.second.default-line:line1}")
    private String defaultLine;

    @Value("${tdx.second.timeout-ms:10000}")
    private long timeoutMs;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public MarkTdxSecondSendCodeResultVO sendCode(String phone, String line) {
        String targetPhone = normalizePhone(phone);
        String targetLine = normalizeLine(line);

        TdxApiResponse precheck = post("/api/tdx/sms-receive/precheck", Map.of("phone", targetPhone));
        Map<String, Object> precheckData = asMap(precheck.body.get("data"));
        boolean allowed = Boolean.TRUE.equals(precheckData.get("allowed"));
        if (!precheck.success || !allowed) {
            String message = StringUtils.defaultIfBlank(asString(precheck.body.get("message")), "该号码暂不可申诉");
            throw new ServiceException(message);
        }

        TdxApiResponse send = post("/api/tdx/sms-receive/send-code", Map.of(
                "phone", targetPhone,
                "line", targetLine
        ));
        if (!send.success) {
            String message = StringUtils.defaultIfBlank(asString(send.body.get("message")), "验证码发送失败，请稍后重试。");
            throw new ServiceException(message);
        }

        MarkTdxSecondSendCodeResultVO result = new MarkTdxSecondSendCodeResultVO();
        result.setPhone(targetPhone);
        result.setAllowed(true);
        result.setStatus(StringUtils.defaultIfBlank(asString(precheckData.get("status")), "可申诉"));
        result.setLine(targetLine);
        result.setMessage(StringUtils.defaultIfBlank(asString(send.body.get("message")), "短信验证码已发送，请查收"));
        result.setPrecheckResponse(precheck.body);
        result.setSendCodeResponse(send.body);
        return result;
    }

    public MarkTdxSecondSubmitResultVO submit(String phone, String smsCode, String line, Boolean rotate) {
        String targetPhone = normalizePhone(phone);
        String targetCode = StringUtils.trimToEmpty(smsCode);
        if (!targetCode.matches("\\d{6}")) {
            throw new ServiceException("验证码应为6位数字");
        }
        String targetLine = normalizeLine(line);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("phone", targetPhone);
        payload.put("smsCode", targetCode);
        payload.put("line", targetLine);
        payload.put("rotate", Boolean.TRUE.equals(rotate));

        TdxApiResponse submit = post("/api/tdx/sms-receive/login", payload);
        if (!submit.success) {
            String message = StringUtils.defaultIfBlank(asString(submit.body.get("message")), "验证登录失败，请核对手机号和验证码后重试。");
            throw new ServiceException(message);
        }

        Map<String, Object> data = asMap(submit.body.get("data"));
        MarkTdxSecondSubmitResultVO result = new MarkTdxSecondSubmitResultVO();
        result.setPhone(StringUtils.defaultIfBlank(asString(data.get("phone")), targetPhone));
        result.setTdxId(asLong(data.get("id")));
        result.setOrderpicinumber(asString(data.get("orderpicinumber")));
        result.setAccepted(true);
        result.setMessage(StringUtils.defaultIfBlank(asString(submit.body.get("message")), "验证码登录验证通过，已开始后台申诉清理"));
        result.setProcessStatus("1");
        result.setSubmitResponse(submit.body);
        return result;
    }

    private TdxApiResponse post(String path, Map<String, Object> payload) {
        ensureConfigured();
        try {
            String url = normalizeBaseUrl() + path;
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(Math.max(timeoutMs, 1000L)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", normalizeAuthorization())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> responseBody = parseBody(response.body());
            boolean bodySuccess = Boolean.TRUE.equals(responseBody.get("success"));
            return new TdxApiResponse(response.statusCode(), bodySuccess, responseBody);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("TDX二次接口调用异常 path={}", path, e);
            throw new ServiceException("TDX二次申诉接口请求失败，请稍后重试");
        }
    }

    private void ensureConfigured() {
        if (StringUtils.isBlank(baseUrl)) {
            throw new ServiceException("TDX二次接口地址未配置");
        }
        if (StringUtils.isBlank(token)) {
            throw new ServiceException("TDX二次接口Token未配置");
        }
    }

    private String normalizeBaseUrl() {
        String text = StringUtils.trimToEmpty(baseUrl);
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private String normalizeAuthorization() {
        String text = StringUtils.trimToEmpty(token);
        return StringUtils.startsWithIgnoreCase(text, "Bearer ") ? text : "Bearer " + text;
    }

    private String normalizePhone(String phone) {
        String targetPhone = StringUtils.trimToEmpty(phone);
        if (!targetPhone.matches("\\d{11}")) {
            throw new ServiceException("手机号应为11位数字");
        }
        return targetPhone;
    }

    private String normalizeLine(String line) {
        String targetLine = StringUtils.defaultIfBlank(StringUtils.trimToNull(line), defaultLine);
        if (!"line1".equals(targetLine) && !"line2".equals(targetLine)) {
            throw new ServiceException("通道线路仅支持line1或line2");
        }
        return targetLine;
    }

    private Map<String, Object> parseBody(String body) throws Exception {
        if (StringUtils.isBlank(body)) {
            return new LinkedHashMap<>();
        }
        return objectMapper.readValue(body, MAP_TYPE);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : new LinkedHashMap<>();
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = asString(value);
        if (!text.matches("\\d+")) {
            return null;
        }
        return Long.parseLong(text);
    }

    private record TdxApiResponse(int httpStatus, boolean success, Map<String, Object> body) {
    }
}
