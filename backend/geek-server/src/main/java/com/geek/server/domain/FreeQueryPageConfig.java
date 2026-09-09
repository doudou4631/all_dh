package com.geek.server.domain;

import com.geek.common.core.domain.BaseEntity;

public class FreeQueryPageConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String pageCode;
    private String phone;
    private String qrCode;
    private Boolean showOnlineService;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPageCode() {
        return pageCode;
    }

    public void setPageCode(String pageCode) {
        this.pageCode = pageCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public Boolean getShowOnlineService() {
        return showOnlineService;
    }

    public void setShowOnlineService(Boolean showOnlineService) {
        this.showOnlineService = showOnlineService;
    }
}
