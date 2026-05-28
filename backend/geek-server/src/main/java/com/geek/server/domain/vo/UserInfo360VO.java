package com.geek.server.domain.vo;

import lombok.Data;

public class UserInfo360VO {
    private String username;
    private String password;

    // 构造函数、getter和setter方法
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
