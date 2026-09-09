package com.geek.server.domain.vo;


import java.util.Map;

// 使用类型安全的DTO类来解析JSON

public class PreActionConfig {
    private String url;
    private Map<String, Object> params;

    // 构造函数、getter和setter方法
    public PreActionConfig() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
