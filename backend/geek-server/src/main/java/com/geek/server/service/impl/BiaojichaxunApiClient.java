package com.geek.server.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.common.utils.http.HttpUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Client for the unified biaojichaxun.top platform API. */
final class BiaojichaxunApiClient {
    private static final String BASE = "https://biaojichaxun.top/v1/platforms/";
    private static final String API_KEY = "agg_bri4zJTB-ksCpoGFnx4SfGT2RaBk7Oc8cR4oXFKeIDo";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BiaojichaxunApiClient() {}

    static boolean supports(String platformKey, String platformName) {
        return endpoint(platformKey, platformName) != null;
    }

    static String query(String platformKey, String platformName, String phone) throws Exception {
        String endpoint = endpoint(platformKey, platformName);
        if (endpoint == null) {
            return null;
        }
        Map<String, Object> body = Map.of("phone", phone);
        Map<String, String> headers = Map.of("X-API-Key", API_KEY, "Content-Type", "application/json");
        String raw = HttpUtils.postJson(BASE + endpoint + "/query", body, headers);
        return normalize(raw, platformKey, platformName);
    }

    static String normalize(String raw, String platformKey, String platformName) throws Exception {
        String endpoint = endpoint(platformKey, platformName);
        Map<String, Object> source = MAPPER.readValue(raw, new TypeReference<Map<String, Object>>() {});
        String status = String.valueOf(source.getOrDefault("status", "failed"));
        boolean success = "tagged".equalsIgnoreCase(status) || "clean".equalsIgnoreCase(status);
        boolean marked = "tagged".equalsIgnoreCase(status);
        String message = String.valueOf(source.getOrDefault("message", marked ? "存在标记" : "未查询到标记"));
        if (marked && "baidu".equals(endpoint)) {
            message = "商业营销";
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", success);
        data.put("marked", marked);
        data.put("status", marked ? "yes-" + message : "no");
        data.put("platform", canonicalPlatform(platformKey, platformName, endpoint));
        data.put(endpoint, marked ? "yes-" + message : "no");
        data.put("message", message);
        data.put("result", data.get("message"));
        data.put("phone", source.getOrDefault("phone", ""));
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("data", data);
        return MAPPER.writeValueAsString(normalized);
    }

    static String url(String platformKey, String platformName) {
        String endpoint = endpoint(platformKey, platformName);
        return endpoint == null ? null : BASE + endpoint + "/query";
    }

    static String apiKey() { return API_KEY; }

    private static String endpoint(String key, String name) {
        String value = (StringUtils.defaultIfBlank(key, "") + " " + StringUtils.defaultIfBlank(name, ""))
                .toLowerCase(Locale.ROOT).replace(" ", "");
        if (value.contains("baidu") || value.contains("百度") || value.contains("bd")) return "baidu";
        if (value.contains("360") || value.contains("qihu")) return "360";
        if (value.contains("tencent") || value.contains("tengxun") || value.contains("腾讯") || value.equals("tx")) return "tencent";
        if (value.contains("xiaomi") || value.contains("小米") || value.contains("xm")) return "xiaomi";
        if (value.contains("oppo")) return "oppo";
        if (value.contains("teddy") || value.contains("taidixiong") || value.contains("泰迪熊")) return "teddy";
        if (value.contains("dianhuabang") || value.contains("电话邦") || value.contains("dhb")) return "dianhuabang";
        if (value.contains("sogou") || value.contains("搜狗") || value.contains("sghmt")) return "sogou";
        if (value.contains("mobile-hf") || value.contains("yidonggaopin") || value.contains("移动高频") || value.contains("ydhigh")) return "mobile-hf";
        if (value.contains("unicom") || value.contains("ltgj") || value.contains("联通")) return "unicom";
        return null;
    }

    private static String canonicalPlatform(String key, String name, String endpoint) {
        String value = StringUtils.defaultIfBlank(key, "").toLowerCase(Locale.ROOT);
        if (value.contains("tengxun")) return "tengxun";
        if (value.contains("taidixiong") || value.startsWith("td_")) return "taidixiong";
        if (value.contains("yidonggaopin")) return "yidonggaopin";
        if (value.contains("360") || value.contains("qihu") || "360".equals(endpoint)) return "sanliuling";
        if (value.contains("ltgj")) return "ltgj";
        if (value.contains("sghmt")) return "sghmt";
        if ("tencent".equals(endpoint)) return "tengxun";
        if ("teddy".equals(endpoint)) return "taidixiong";
        if ("mobile-hf".equals(endpoint)) return "yidonggaopin";
        if ("unicom".equals(endpoint)) return "ltgj";
        if ("sogou".equals(endpoint)) return "sghmt";
        return endpoint;
    }
}
