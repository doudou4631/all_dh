package com.geek.server.service.impl;

import com.geek.server.domain.FreeQueryPlatform;
import com.geek.server.domain.UserPlatformUrlConfig;
import com.geek.server.service.IFreeQueryPlatformService;
import com.geek.server.service.IUserPlatformUrlConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FreeQueryPlatformServiceImpl implements IFreeQueryPlatformService {
    private static final Set<String> ALLOWED_ICONS = Set.of(
            "tdx.png", "sll.png", "tx.png", "bd.png", "lt.png",
            "ydhigh.png", "dhb.png", "sghmt.png", "xm.png", "114.png");

    @Autowired
    private IUserPlatformUrlConfigService platformService;

    @Override
    public List<FreeQueryPlatform> selectList() {
        List<UserPlatformUrlConfig> platforms = platformService
                .selectUserPlatformUrlConfigList(new UserPlatformUrlConfig());
        return platforms.stream()
                .sorted(Comparator.comparingInt(this::priority).thenComparing(UserPlatformUrlConfig::getId))
                .map(this::toView)
                .toList();
    }

    @Override
    public int updateDisplay(FreeQueryPlatform platform) {
        UserPlatformUrlConfig current = requirePlatform(platform.getId());
        String icon = platform.getIcon() == null ? "" : platform.getIcon().trim();
        if (!ALLOWED_ICONS.contains(icon)) {
            throw new IllegalArgumentException("请选择有效的平台图标");
        }
        String description = platform.getSourceDescription() == null
                ? ""
                : platform.getSourceDescription().trim();
        if (description.length() > 500) {
            throw new IllegalArgumentException("数据来源描述不能超过500个字符");
        }

        UserPlatformUrlConfig update = new UserPlatformUrlConfig();
        update.setId(current.getId());
        update.setFreeQueryIcon(icon);
        update.setFreeQueryDescription(description);
        return platformService.updateUserPlatformUrlConfig(update);
    }

    @Override
    public int changeStatus(Long id, String status) {
        requirePlatform(id);
        if (!"0".equals(status) && !"1".equals(status)) {
            throw new IllegalArgumentException("平台状态无效");
        }
        UserPlatformUrlConfig update = new UserPlatformUrlConfig();
        update.setId(id);
        update.setFreeQueryStatus(status);
        return platformService.updateUserPlatformUrlConfig(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSort(List<Long> ids) {
        List<FreeQueryPlatform> current = selectList();
        Set<Long> currentIds = current.stream().map(FreeQueryPlatform::getId).collect(java.util.stream.Collectors.toSet());
        if (ids == null || ids.size() != currentIds.size()
                || new HashSet<>(ids).size() != ids.size()
                || !currentIds.equals(new HashSet<>(ids))) {
            throw new IllegalArgumentException("平台排序数据不完整，请刷新后重试");
        }

        int updated = 0;
        for (int index = 0; index < ids.size(); index++) {
            UserPlatformUrlConfig update = new UserPlatformUrlConfig();
            update.setId(ids.get(index));
            update.setFreeQuerySort(index + 1);
            updated += platformService.updateUserPlatformUrlConfig(update);
        }
        return updated;
    }

    private UserPlatformUrlConfig requirePlatform(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("平台ID不能为空");
        }
        UserPlatformUrlConfig platform = platformService.selectUserPlatformUrlConfigById(id);
        if (platform == null) {
            throw new IllegalArgumentException("平台不存在");
        }
        return platform;
    }

    private int priority(UserPlatformUrlConfig item) {
        if (item.getFreeQuerySort() != null) {
            return item.getFreeQuerySort();
        }
        if (item.getSort() != null) {
            return item.getSort();
        }
        return Integer.MAX_VALUE;
    }

    private FreeQueryPlatform toView(UserPlatformUrlConfig item) {
        FreeQueryPlatform view = new FreeQueryPlatform();
        view.setId(item.getId());
        view.setPlatformName(item.getPlatformName());
        view.setIcon(item.getFreeQueryIcon());
        view.setSourceDescription(item.getFreeQueryDescription());
        view.setStatus(item.getFreeQueryStatus());
        view.setGlobalStatus(item.getStatus());
        view.setPriority(priority(item));
        return view;
    }
}
