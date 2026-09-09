package com.geek.server.controller;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.geek.common.annotation.Log;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.enums.BusinessType;
import com.geek.server.domain.UserPlatformUrlConfig;
import com.geek.server.domain.MarkPlatformMapping;
import com.geek.server.service.IMarkPlatformMappingService;
import com.geek.server.service.IUserPlatformUrlConfigService;
import com.geek.common.utils.poi.ExcelUtil;
import com.geek.common.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

/**
 * 查询平台url配置Controller
 * 
 * @author geek
 * @date 2026-03-09
 */
@RestController
@RequestMapping("/server/platformConfig")
@Tag(name = "【查询平台url配置】管理")
public class UserPlatformUrlConfigController extends BaseController
{
    @Autowired
    private IUserPlatformUrlConfigService userPlatformUrlConfigService;

    @Autowired
    private IMarkPlatformMappingService markPlatformMappingService;

    /**
     * 查询查询平台url配置列表
     */
    @Operation(summary = "查询平台url配置列表")
    @PreAuthorize("@ss.hasPermi('server:platformConfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(UserPlatformUrlConfig userPlatformUrlConfig)
    {
        startPage();
        List<UserPlatformUrlConfig> list = userPlatformUrlConfigService.selectUserPlatformUrlConfigList(userPlatformUrlConfig);
        return getDataTable(list);
    }

    @Operation(summary = "获取代理处理平台绑定选项")
    @PreAuthorize("@ss.hasPermi('server:platformConfig:list')")
    @GetMapping("/bindingOptions")
    public AjaxResult bindingOptions()
    {
        List<MarkPlatformMapping> mappings = markPlatformMappingService.selectList(new MarkPlatformMapping());
        Map<String, Map<String, Object>> distinctOptions = new LinkedHashMap<>();
        for (MarkPlatformMapping mapping : mappings) {
            String platformKey = mapping.getTargetPlatformCode();
            if (platformKey == null || platformKey.isBlank()) continue;
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("platformKey", platformKey);
            option.put("platformName", mapping.getTargetPlatformName());
            option.put("sourcePlatformName", mapping.getSourcePlatformName());
            option.put("sortNum", mapping.getSortNum());
            option.put("enabled", "0".equals(mapping.getSourceEnabled())
                    && "0".equals(mapping.getTargetEnabled())
                    && "0".equals(mapping.getMappingEnabled()));
            distinctOptions.putIfAbsent(platformKey, option);
        }
        return success(new ArrayList<>(distinctOptions.values()));
    }

    /**
     * 查询用户可用平台url列表
     */
    @Operation(summary = "查询平台url配置列表")
    @PreAuthorize("@ss.hasAnyPermi('server:apiServer:singleQuery,server:apiServer:batchQuery')")
    @GetMapping("/userList")
    public TableDataInfo userList(UserPlatformUrlConfig userPlatformUrlConfig)
    {
        startPage();
        List<UserPlatformUrlConfig> list = userPlatformUrlConfigService.selectUserPlatformUrlUserList(userPlatformUrlConfig);
        return getDataTable(list);
    }

    /**
     * 导出查询平台url配置列表
     */
    @Operation(summary = "导出查询平台url配置列表")
    @PreAuthorize("@ss.hasPermi('server:platformConfig:export')")
    @Log(title = "查询平台url配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, UserPlatformUrlConfig userPlatformUrlConfig)
    {
        List<UserPlatformUrlConfig> list = userPlatformUrlConfigService.selectUserPlatformUrlConfigList(userPlatformUrlConfig);
        ExcelUtil<UserPlatformUrlConfig> util = new ExcelUtil<>(UserPlatformUrlConfig.class);
        util.exportExcel(response, list, "查询平台url配置数据");
    }

    /**
     * 获取查询平台url配置详细信息
     */
    @Operation(summary = "获取查询平台url配置详细信息")
    @PreAuthorize("@ss.hasPermi('server:platformConfig:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(userPlatformUrlConfigService.selectUserPlatformUrlConfigById(id));
    }

    /**
     * 新增查询平台url配置
     */
    @Operation(summary = "新增查询平台url配置")
    @PreAuthorize("@ss.hasPermi('server:platformConfig:add')")
    @Log(title = "查询平台url配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody UserPlatformUrlConfig userPlatformUrlConfig)
    {
        normalizePlatform(userPlatformUrlConfig);
        return toAjax(userPlatformUrlConfigService.insertUserPlatformUrlConfig(userPlatformUrlConfig));
    }

    /**
     * 修改查询平台url配置
     */
    @Operation(summary = "修改查询平台url配置")
    @PreAuthorize("@ss.hasPermi('server:platformConfig:edit')")
    @Log(title = "查询平台url配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UserPlatformUrlConfig userPlatformUrlConfig)
    {
        normalizePlatform(userPlatformUrlConfig);
        UserPlatformUrlConfig saved = userPlatformUrlConfigService.selectUserPlatformUrlConfigById(userPlatformUrlConfig.getId());
        if (saved == null) {
            return error("接口配置不存在");
        }
        userPlatformUrlConfig.setPlatformName(saved.getPlatformName());
        return toAjax(userPlatformUrlConfigService.updateUserPlatformUrlConfig(userPlatformUrlConfig));
    }

    @Operation(summary = "测试平台接口连接")
    @PreAuthorize("@ss.hasPermi('server:platformConfig:test')")
    @PostMapping("/test/{id}")
    public AjaxResult testConnection(@PathVariable Long id)
    {
        UserPlatformUrlConfig config = userPlatformUrlConfigService.selectUserPlatformUrlConfigById(id);
        if (config == null || config.getUrl() == null || config.getUrl().isBlank()) {
            return error("接口配置不存在或URL为空");
        }
        URI uri;
        try {
            uri = URI.create(config.getUrl().trim());
        } catch (IllegalArgumentException ex) {
            return error("接口URL格式不正确");
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            return error("接口URL仅支持HTTP或HTTPS协议");
        }

        long timeoutMs = config.getTimeoutMs() == null ? 5000L : config.getTimeoutMs();
        timeoutMs = Math.max(1000L, Math.min(timeoutMs, 15000L));
        long start = System.currentTimeMillis();
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("User-Agent", "Geek-Platform-Connection-Test/1.0")
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("platformKey", config.getPlatformKey());
            data.put("statusCode", response.statusCode());
            data.put("elapsedMs", System.currentTimeMillis() - start);
            return AjaxResult.success("连接成功，HTTP " + response.statusCode(), data);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return error("连接测试已中断");
        } catch (Exception ex) {
            return error("连接失败：" + ex.getMessage());
        }
    }

    /**
     * 删除查询平台url配置
     */
    @Operation(summary = "删除查询平台url配置")
    @PreAuthorize("@ss.hasPermi('server:platformConfig:remove')")
    @Log(title = "查询平台url配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable( name = "ids" ) Long[] ids) 
    {
        return toAjax(userPlatformUrlConfigService.deleteUserPlatformUrlConfigByIds(ids));
    }

    private void normalizePlatform(UserPlatformUrlConfig config)
    {
        if (config.getPlatformKey() != null) {
            config.setPlatformKey(config.getPlatformKey().trim().toLowerCase());
        }
        if (config.getPlatformName() != null) {
            config.setPlatformName(config.getPlatformName().trim());
        }
        if (config.getUrl() != null) {
            config.setUrl(config.getUrl().trim());
        }
        if (config.getStatus() == null) {
            config.setStatus("0");
        }
    }

}
