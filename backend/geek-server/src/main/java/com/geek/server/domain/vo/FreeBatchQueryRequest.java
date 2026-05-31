package com.geek.server.domain.vo;

import java.util.List;

public class FreeBatchQueryRequest {

    private List<String> phones;
    private String deviceId;

    public List<String> getPhones() {
        return phones;
    }

    public void setPhones(List<String> phones) {
        this.phones = phones;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
