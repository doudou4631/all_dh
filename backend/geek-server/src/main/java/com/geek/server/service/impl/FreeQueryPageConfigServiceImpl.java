package com.geek.server.service.impl;

import com.geek.common.utils.DateUtils;
import com.geek.common.utils.SecurityUtils;
import com.geek.server.domain.FreeQueryPageConfig;
import com.geek.server.mapper.FreeQueryPageConfigMapper;
import com.geek.server.service.IFreeQueryPageConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Service
public class FreeQueryPageConfigServiceImpl implements IFreeQueryPageConfigService {
    private static final String DEFAULT_PAGE_CODE = "1";
    private static final String DEFAULT_PHONE = "13027616171";
    private static final String DEFAULT_QR_CODE = "/static/img/contact-qr.png";

    @Autowired
    private FreeQueryPageConfigMapper mapper;

    @Override
    public FreeQueryPageConfig getConfig(String pageCode) {
        String normalizedCode = normalizePageCode(pageCode);
        FreeQueryPageConfig config = mapper.selectByPageCode(normalizedCode);
        if (config != null) {
            return config;
        }
        FreeQueryPageConfig defaults = new FreeQueryPageConfig();
        defaults.setPageCode(normalizedCode);
        defaults.setPhone(DEFAULT_PHONE);
        defaults.setQrCode(DEFAULT_QR_CODE);
        defaults.setShowOnlineService(false);
        return defaults;
    }

    @Override
    public List<FreeQueryPageConfig> listConfigs() {
        return mapper.selectAll();
    }

    @Override
    public int saveConfig(FreeQueryPageConfig config) {
        validate(config);
        config.setPageCode(normalizePageCode(config.getPageCode()));
        config.setPhone(StringUtils.trimToEmpty(config.getPhone()));
        config.setQrCode(normalizeQrCode(config.getQrCode()));
        if (config.getShowOnlineService() == null) {
            config.setShowOnlineService(false);
        }
        config.setUpdateBy(SecurityUtils.getUsername());
        config.setUpdateTime(DateUtils.getNowDate());
        return mapper.saveConfig(config);
    }

    private void validate(FreeQueryPageConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("页面配置不能为空");
        }
        normalizePageCode(config.getPageCode());
        String phone = StringUtils.trimToEmpty(config.getPhone());
        if (StringUtils.isNotBlank(phone)
                && (phone.length() > 32 || !phone.matches("^[0-9+() -]{5,32}$"))) {
            throw new IllegalArgumentException("联系电话格式不正确");
        }
        if (StringUtils.isBlank(config.getQrCode()) || config.getQrCode().trim().length() > 500) {
            throw new IllegalArgumentException("请上传客服二维码");
        }
        normalizeQrCode(config.getQrCode());
    }

    private String normalizeQrCode(String qrCode) {
        String normalized = StringUtils.trimToEmpty(qrCode);
        try {
            URI uri = URI.create(normalized);
            if (uri.isAbsolute()) {
                normalized = uri.getPath();
            }
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("二维码地址格式不正确");
        }
        if (!(normalized.startsWith("/static/") || normalized.startsWith("/files/")
                || normalized.startsWith("/profile/"))) {
            throw new IllegalArgumentException("二维码必须使用本站上传的图片");
        }
        return normalized;
    }

    private String normalizePageCode(String pageCode) {
        String normalized = StringUtils.defaultIfBlank(pageCode, DEFAULT_PAGE_CODE).trim().toLowerCase();
        if (!normalized.matches("^[a-z0-9][a-z0-9_-]{0,31}$")) {
            throw new IllegalArgumentException("页面编号格式不正确");
        }
        return normalized;
    }
}
