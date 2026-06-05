package com.geek.server.service.impl;

import com.geek.common.exception.ServiceException;
import com.geek.common.utils.DateUtils;
import com.geek.common.utils.StringUtils;
import com.geek.server.domain.MobilePageConfig;
import com.geek.server.mapper.MobilePageConfigMapper;
import com.geek.server.service.IMobilePageConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MobilePageConfigServiceImpl implements IMobilePageConfigService {

    private static final String DEFAULT_PAGE_CODE = "mobile-h5";
    private static final String DEFAULT_STATUS = "0";
    private static final Set<String> VALID_STATUS = Set.of("0", "1");
    private static final Pattern PAGE_CODE_PATTERN = Pattern.compile("^[a-z0-9-]{2,32}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(1[3-9]\\d{9}|(0\\d{2,3}-?)?\\d{7,8}|400-?\\d{7}|800-?\\d{7}|1[0-9]{1,4})$");

    private final MobilePageConfigMapper mobilePageConfigMapper;

    @Override
    public MobilePageConfig selectMobilePageConfigById(Long id) {
        return mobilePageConfigMapper.selectMobilePageConfigById(id);
    }

    @Override
    public List<MobilePageConfig> selectMobilePageConfigList(MobilePageConfig mobilePageConfig) {
        return mobilePageConfigMapper.selectMobilePageConfigList(mobilePageConfig);
    }

    @Override
    public int insertMobilePageConfig(MobilePageConfig mobilePageConfig) {
        validateAndNormalize(mobilePageConfig);
        checkPageCodeUnique(mobilePageConfig.getPageCode(), null);
        mobilePageConfig.setDelFlag("0");
        mobilePageConfig.setCreateTime(DateUtils.getNowDate());
        return mobilePageConfigMapper.insertMobilePageConfig(mobilePageConfig);
    }

    @Override
    public int updateMobilePageConfig(MobilePageConfig mobilePageConfig) {
        if (mobilePageConfig == null || mobilePageConfig.getId() == null) {
            throw new ServiceException("页面配置ID不能为空");
        }
        MobilePageConfig db = mobilePageConfigMapper.selectMobilePageConfigById(mobilePageConfig.getId());
        if (db == null) {
            throw new ServiceException("页面配置不存在");
        }
        validateAndNormalize(mobilePageConfig);
        checkPageCodeUnique(mobilePageConfig.getPageCode(), mobilePageConfig.getId());
        mobilePageConfig.setDelFlag("0");
        mobilePageConfig.setUpdateTime(DateUtils.getNowDate());
        return mobilePageConfigMapper.updateMobilePageConfig(mobilePageConfig);
    }

    @Override
    public int deleteMobilePageConfigByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        return mobilePageConfigMapper.deleteMobilePageConfigByIds(ids);
    }

    @Override
    public int deleteMobilePageConfigById(Long id) {
        if (id == null) {
            return 0;
        }
        return mobilePageConfigMapper.deleteMobilePageConfigById(id);
    }

    @Override
    public Map<String, Object> selectPublicConfig(String pageCode) {
        String requestedPage = normalizePublicPageCode(pageCode);
        MobilePageConfig resolved = mobilePageConfigMapper.selectPublicConfigByPageCode(requestedPage);
        if (resolved == null && !DEFAULT_PAGE_CODE.equals(requestedPage)) {
            resolved = mobilePageConfigMapper.selectPublicConfigByPageCode(DEFAULT_PAGE_CODE);
        }
        if (resolved == null) {
            resolved = mobilePageConfigMapper.selectFirstPublicConfig();
        }
        if (resolved == null) {
            throw new ServiceException("暂无可用手机页面配置");
        }
        return buildPublicConfig(requestedPage, resolved);
    }

    private void checkPageCodeUnique(String pageCode, Long currentId) {
        MobilePageConfig same = mobilePageConfigMapper.selectMobilePageConfigByPageCode(pageCode);
        if (same == null) {
            return;
        }
        if (currentId == null || !same.getId().equals(currentId)) {
            throw new ServiceException("页面编码已存在");
        }
    }

    private void validateAndNormalize(MobilePageConfig config) {
        if (config == null) {
            throw new ServiceException("页面配置参数不能为空");
        }

        String pageCode = StringUtils.trimToEmpty(config.getPageCode()).toLowerCase();
        if (StringUtils.isEmpty(pageCode)) {
            throw new ServiceException("页面编码不能为空");
        }
        if (!PAGE_CODE_PATTERN.matcher(pageCode).matches()) {
            throw new ServiceException("页面编码仅支持小写字母、数字、中划线，长度2-32");
        }
        config.setPageCode(pageCode);

        String pageName = StringUtils.trimToEmpty(config.getPageName());
        if (StringUtils.isEmpty(pageName)) {
            throw new ServiceException("页面名称不能为空");
        }
        if (pageName.length() > 64) {
            throw new ServiceException("页面名称长度不能超过64");
        }
        config.setPageName(pageName);

        String servicePhone = StringUtils.trimToEmpty(config.getServicePhone());
        if (StringUtils.isEmpty(servicePhone)) {
            throw new ServiceException("客服电话不能为空");
        }
        if (!PHONE_PATTERN.matcher(servicePhone).matches()) {
            throw new ServiceException("客服电话格式不正确");
        }
        config.setServicePhone(servicePhone);

        config.setWechatQrUrl(normalizeUrlOrPath(config.getWechatQrUrl(), true, "客服二维码地址"));
        config.setNavHomeUrl(normalizeUrlOrPathWithDefault(config.getNavHomeUrl(), "/", "首页链接"));
        config.setNavQueryUrl(normalizeUrlOrPathWithDefault(config.getNavQueryUrl(), "/?tab=query", "免费查询链接"));
        config.setNavBatchUrl(normalizeUrlOrPathWithDefault(config.getNavBatchUrl(), "/batch/", "批量查询链接"));
        config.setNavProfileUrl(normalizeUrlOrPathWithDefault(config.getNavProfileUrl(), "/profile/", "个人中心链接"));
        config.setResultBackUrl(normalizeUrlOrPathWithDefault(config.getResultBackUrl(), "/", "结果返回链接"));

        String status = StringUtils.trimToEmpty(config.getStatus());
        if (StringUtils.isEmpty(status)) {
            status = DEFAULT_STATUS;
        }
        if (!VALID_STATUS.contains(status)) {
            throw new ServiceException("状态值不合法");
        }
        config.setStatus(status);

        Integer sort = config.getSort();
        if (sort == null || sort < 0) {
            sort = 0;
        }
        config.setSort(sort);

        config.setRemark(trimToLength(config.getRemark(), 500));
    }

    private String normalizePublicPageCode(String pageCode) {
        String normalized = StringUtils.trimToEmpty(pageCode).toLowerCase();
        if (StringUtils.isEmpty(normalized)) {
            return DEFAULT_PAGE_CODE;
        }
        if (!PAGE_CODE_PATTERN.matcher(normalized).matches()) {
            return DEFAULT_PAGE_CODE;
        }
        return normalized;
    }

    private Map<String, Object> buildPublicConfig(String requestedPage, MobilePageConfig resolved) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestedPage", requestedPage);
        data.put("pageCode", resolved.getPageCode());
        data.put("pageName", resolved.getPageName());
        data.put("servicePhone", resolved.getServicePhone());
        data.put("wechatQrUrl", resolved.getWechatQrUrl());
        data.put("navHomeUrl", resolved.getNavHomeUrl());
        data.put("navQueryUrl", resolved.getNavQueryUrl());
        data.put("navBatchUrl", resolved.getNavBatchUrl());
        data.put("navProfileUrl", resolved.getNavProfileUrl());
        data.put("resultBackUrl", resolved.getResultBackUrl());
        data.put("entryUrl", "/mobile-h5/?page=" + resolved.getPageCode());
        return data;
    }

    private String normalizeUrlOrPathWithDefault(String value, String defaultValue, String fieldName) {
        String normalized = StringUtils.trimToEmpty(value);
        if (StringUtils.isEmpty(normalized)) {
            normalized = defaultValue;
        }
        if (!isValidUrlOrPath(normalized)) {
            throw new ServiceException(fieldName + "格式不正确");
        }
        return normalized;
    }

    private String normalizeUrlOrPath(String value, boolean required, String fieldName) {
        String normalized = StringUtils.trimToEmpty(value);
        if (StringUtils.isEmpty(normalized)) {
            if (required) {
                throw new ServiceException(fieldName + "不能为空");
            }
            return "";
        }
        if (!isValidUrlOrPath(normalized)) {
            throw new ServiceException(fieldName + "格式不正确");
        }
        return normalized;
    }

    private boolean isValidUrlOrPath(String value) {
        return StringUtils.startsWith(value, "/")
                || StringUtils.startsWithIgnoreCase(value, "http://")
                || StringUtils.startsWithIgnoreCase(value, "https://");
    }

    private String trimToLength(String value, int maxLength) {
        String trimmed = StringUtils.trimToEmpty(value);
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
