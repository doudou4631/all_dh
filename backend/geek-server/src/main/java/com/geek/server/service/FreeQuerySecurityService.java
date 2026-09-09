package com.geek.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FreeQuerySecurityService {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final long TEN_MINUTES_MS = 10 * 60 * 1000L;
    private static final long ONE_HOUR_MS = 60 * 60 * 1000L;
    private static final long ONE_DAY_MS = 24 * 60 * 60 * 1000L;

    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, WindowCounter> counters = new HashMap<>();
    private final Map<String, Long> consumedTokens = new HashMap<>();

    @Value("${free-query.signing-key}")
    private String signingKey;

    @Value("${free-query.token-ttl-seconds:90}")
    private long tokenTtlSeconds;

    public FreeQuerySecurityService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized IssueResult issueToken(SecurityContext context) {
        long now = System.currentTimeMillis();
        cleanup(now);
        Limit[] limits = {
                new Limit("challenge:ip:" + context.ipAddr(), 30, TEN_MINUTES_MS,
                        "获取查询令牌过于频繁，请稍后再试"),
                new Limit("challenge:subnet:" + subnet(context.ipAddr()), 120, TEN_MINUTES_MS,
                        "当前网络请求过于频繁，请稍后再试"),
                new Limit("challenge:device:" + context.deviceId(), 30, TEN_MINUTES_MS,
                        "当前设备请求过于频繁，请稍后再试"),
                new Limit("challenge:fingerprint:" + context.fingerprint(), 30, TEN_MINUTES_MS,
                        "当前设备请求过于频繁，请稍后再试")
        };
        String error = checkLimits(limits, now);
        if (error != null) {
            return new IssueResult(null, 0, error);
        }
        incrementLimits(limits, now);

        long expiresAt = now + Math.max(30, tokenTtlSeconds) * 1000L;
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("version", 1);
        claims.put("jti", randomId());
        claims.put("issuedAt", now);
        claims.put("expiresAt", expiresAt);
        claims.put("purpose", "free-query");
        claims.put("ip", context.ipAddr());
        claims.put("device", context.deviceId());
        claims.put("fingerprint", context.fingerprint());
        claims.put("userAgent", digest(context.userAgent()));
        try {
            String payload = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(claims));
            return new IssueResult(payload + "." + sign(payload), expiresAt, null);
        } catch (Exception ex) {
            return new IssueResult(null, 0, "安全令牌生成失败，请稍后重试");
        }
    }

    public synchronized String validateAndConsume(String token, SecurityContext context,
                                                   String phoneNumber, int dailyLimit) {
        long now = System.currentTimeMillis();
        cleanup(now);
        Map<String, Object> claims = verify(token);
        if (claims == null || !"free-query".equals(stringValue(claims.get("purpose")))) {
            return "安全验证已失效，请重新查询";
        }
        long expiresAt = longValue(claims.get("expiresAt"));
        if (expiresAt < now || longValue(claims.get("issuedAt")) > now + 5000L) {
            return "安全验证已过期，请重新查询";
        }
        if (!constantEquals(stringValue(claims.get("ip")), context.ipAddr())
                || !constantEquals(stringValue(claims.get("device")), context.deviceId())
                || !constantEquals(stringValue(claims.get("fingerprint")), context.fingerprint())
                || !constantEquals(stringValue(claims.get("userAgent")), digest(context.userAgent()))) {
            return "查询环境发生变化，请刷新页面后重试";
        }
        String jti = stringValue(claims.get("jti"));
        if (jti.isEmpty() || consumedTokens.containsKey(jti)) {
            return "查询令牌已使用，请勿重复提交";
        }
        consumedTokens.put(jti, expiresAt);

        int deviceLimit = Math.max(1, dailyLimit);
        int subnetLimit = Math.max(100, deviceLimit * 5);
        Limit[] limits = {
                new Limit("query:device:" + context.deviceId(), deviceLimit, ONE_DAY_MS,
                        "当前设备今日查询次数已用完"),
                new Limit("query:fingerprint:" + context.fingerprint(), deviceLimit, ONE_DAY_MS,
                        "当前设备今日查询次数已用完"),
                new Limit("query:subnet:" + subnet(context.ipAddr()), subnetLimit, ONE_DAY_MS,
                        "当前网络今日查询次数已用完"),
                new Limit("query:phone:" + phoneNumber, 5, ONE_HOUR_MS,
                        "该号码短时间内查询次数过多，请稍后再试")
        };
        String error = checkLimits(limits, now);
        if (error != null) {
            return error;
        }
        incrementLimits(limits, now);
        return null;
    }

    private Map<String, Object> verify(String token) {
        if (token == null || token.length() > 4096) {
            return null;
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2 || !constantEquals(sign(parts[0]), parts[1])) {
            return null;
        }
        try {
            return objectMapper.readValue(URL_DECODER.decode(parts[0]), new TypeReference<>() { });
        } catch (Exception ex) {
            return null;
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("无法签名免费查询令牌", ex);
        }
    }

    private String checkLimits(Limit[] limits, long now) {
        for (Limit limit : limits) {
            WindowCounter counter = currentCounter(limit, now);
            if (counter.count >= limit.maxCount()) {
                return limit.message();
            }
        }
        return null;
    }

    private void incrementLimits(Limit[] limits, long now) {
        for (Limit limit : limits) {
            currentCounter(limit, now).count++;
        }
    }

    private WindowCounter currentCounter(Limit limit, long now) {
        WindowCounter counter = counters.get(limit.key());
        if (counter == null || now - counter.startedAt >= limit.windowMs()) {
            counter = new WindowCounter(now);
            counters.put(limit.key(), counter);
        }
        return counter;
    }

    private void cleanup(long now) {
        Iterator<Map.Entry<String, Long>> tokens = consumedTokens.entrySet().iterator();
        while (tokens.hasNext()) {
            if (tokens.next().getValue() < now) {
                tokens.remove();
            }
        }
        Iterator<Map.Entry<String, WindowCounter>> counts = counters.entrySet().iterator();
        while (counts.hasNext()) {
            WindowCounter counter = counts.next().getValue();
            if (now - counter.startedAt > ONE_DAY_MS) {
                counts.remove();
            }
        }
    }

    private String subnet(String ipAddr) {
        try {
            byte[] bytes = InetAddress.getByName(ipAddr).getAddress();
            if (bytes.length == 4) {
                return (bytes[0] & 255) + "." + (bytes[1] & 255) + "." + (bytes[2] & 255) + ".0/24";
            }
            StringBuilder value = new StringBuilder();
            for (int index = 0; index < 7; index++) {
                value.append(String.format("%02x", bytes[index] & 255));
            }
            return value + "/56";
        } catch (Exception ex) {
            return ipAddr;
        }
    }

    private String randomId() {
        byte[] value = new byte[24];
        secureRandom.nextBytes(value);
        return URL_ENCODER.encodeToString(value);
    }

    private String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(stringValue(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                result.append(String.format("%02x", item & 255));
            }
            return result.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private boolean constantEquals(String left, String right) {
        return MessageDigest.isEqual(stringValue(left).getBytes(StandardCharsets.UTF_8),
                stringValue(right).getBytes(StandardCharsets.UTF_8));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private long longValue(Object value) {
        try {
            return Long.parseLong(stringValue(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public record SecurityContext(String ipAddr, String deviceId, String fingerprint, String userAgent) { }

    public record IssueResult(String token, long expiresAt, String error) { }

    private record Limit(String key, int maxCount, long windowMs, String message) { }

    private static final class WindowCounter {
        private final long startedAt;
        private int count;

        private WindowCounter(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
