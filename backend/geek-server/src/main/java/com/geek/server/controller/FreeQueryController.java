package com.geek.server.controller;

import com.geek.common.annotation.Anonymous;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.utils.SecurityUtils;
import com.geek.common.utils.ip.IpUtils;
import com.geek.server.domain.UserApiQueryRecord;
import com.geek.server.domain.vo.FreeBatchQueryRequest;
import com.geek.server.domain.vo.FreeLoginRequest;
import com.geek.server.domain.vo.FreeSingleQueryRequest;
import com.geek.server.service.IFreeQueryService;
import com.geek.system.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "free-query")
@RestController
@RequestMapping(value = "/server/freeQuery", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@RequiredArgsConstructor
public class FreeQueryController extends BaseController {

    private final IFreeQueryService freeQueryService;
    private final ISysUserService sysUserService;

    @Operation(summary = "获取操作IP的查询次数")
    @Anonymous
    @GetMapping("/quota")
    public AjaxResult quota(HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        return AjaxResult.success(freeQueryService.getQuota(ip));
    }

    @Operation(summary = "提交查询")
    @Anonymous
    @PostMapping("/single")
    public AjaxResult single(@RequestBody FreeSingleQueryRequest body, HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        try {
            Map<String, Object> result = freeQueryService.singleQuery(body, ip);
            Integer code = (Integer) result.get("code");
            if (code != null && code == 42901) {
                AjaxResult rsp = AjaxResult.error(42901, String.valueOf(result.get("message")));
                rsp.put("quota", result.get("quota"));
                return rsp;
            }
            if (code != null && code == 42902) {
                AjaxResult rsp = AjaxResult.error(42902, String.valueOf(result.get("message")));
                rsp.put("quota", result.get("quota"));
                return rsp;
            }
            if (code != null && code == 42903) {
                AjaxResult rsp = AjaxResult.error(42903, String.valueOf(result.get("message")));
                rsp.put("quota", result.get("quota"));
                return rsp;
            }
            if (code != null && code != 0) {
                AjaxResult rsp = AjaxResult.error(String.valueOf(result.get("message")));
                rsp.put("quota", result.get("quota"));
                return rsp;
            }
            AjaxResult ok = AjaxResult.success(String.valueOf(result.get("message")), result.get("data"));
            ok.put("quota", result.get("quota"));
            return ok;
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error("查询失败" + e.getMessage());
        }
    }

    @Operation(summary = "账号登录校验")
    @Anonymous
    @PostMapping("/login")
    public AjaxResult login(@RequestBody FreeLoginRequest body) {
        try {
            String loginAccount = normalizeLoginAccount(body != null
                    ? (body.getAccount() != null ? body.getAccount() : body.getPhone())
                    : null);
            if (loginAccount.isEmpty()) {
                return AjaxResult.error("请输入用户名");
            }

            SysUser user = resolveLoginUser(loginAccount);
            if (!isOpenedUser(user)) {
                return AjaxResult.error(40101, "该账号未开通，请先联系后台开通");
            }
            if (!isEnabledUser(user)) {
                return AjaxResult.error(40101, "该账号已停用，请联系管理员");
            }
            String rawPassword = body == null ? "" : String.valueOf(body.getPassword() == null ? "" : body.getPassword()).trim();
            if (StringUtils.hasText(rawPassword) && !isPasswordMatched(rawPassword, user.getPassword())) {
                return AjaxResult.error(40101, "账号或密码错误");
            }

            String phone = normalizeLoginPhone(user.getPhonenumber());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userId", user.getUserId());
            data.put("account", user.getUserName());
            data.put("userName", user.getUserName());
            data.put("phone", isValidLoginPhone(phone) ? phone : "");
            data.put("nickName", user.getNickName());
            data.put("points", user.getPoints() == null ? 0 : Math.max(0, user.getPoints()));
            data.put("status", user.getStatus());
            data.put("statusText", "已开通");
            return AjaxResult.success("登录成功", data);
        } catch (Exception e) {
            return AjaxResult.error("登录失败" + e.getMessage());
        }
    }

    @Operation(summary = "批量提交查询")
    @Anonymous
    @PostMapping("/batch")
    public AjaxResult batch(@RequestBody FreeBatchQueryRequest body, HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        try {
            if (body == null || body.getPhones() == null || body.getPhones().isEmpty()) {
                return AjaxResult.error("请输入至少一个号码");
            }

            String loginAccount = normalizeLoginAccount(body.getDeviceId());
            if (loginAccount.isEmpty()) {
                return AjaxResult.error(40101, "请先登录后再使用批量查询");
            }

            SysUser loginUser = resolveLoginUser(loginAccount);
            if (!isOpenedUser(loginUser)) {
                return AjaxResult.error(40101, "该账号未开通，请先联系后台开通");
            }
            if (!isEnabledUser(loginUser)) {
                return AjaxResult.error(40101, "该账号已停用，请联系管理员");
            }
            String loginDeviceId = resolveBatchDeviceId(loginUser, loginAccount);

            List<String> phones = body.getPhones().stream()
                    .filter(java.util.Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.replaceAll("[^\\d]", ""))
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .limit(20)
                    .toList();

            if (phones.isEmpty()) {
                return AjaxResult.error("请输入正确的号码");
            }

            List<Map<String, Object>> itemResults = new ArrayList<>();
            int successCount = 0;
            int failedCount = 0;
            Object latestQuota = null;
            boolean quotaReached = false;
            int quotaCode = 42901;
            String quotaMsg = "查询次数已达上限";

            for (String phone : phones) {
                if (quotaReached) {
                    failedCount++;
                    itemResults.add(buildBatchItem(phone, quotaCode, quotaMsg, null, latestQuota));
                    continue;
                }

                FreeSingleQueryRequest req = new FreeSingleQueryRequest();
                req.setPhone(phone);
                req.setDeviceId(loginDeviceId);

                try {
                    Map<String, Object> result = freeQueryService.singleQuery(req, ip);
                    Integer code = asInteger(result.get("code"));
                    String message = String.valueOf(result.getOrDefault("message", "查询失败"));
                    Object data = result.get("data");
                    Object quota = result.get("quota");
                    latestQuota = quota != null ? quota : latestQuota;

                    itemResults.add(buildBatchItem(phone, code != null ? code : 500, message, data, quota));

                    if (code != null && code == 0) {
                        successCount++;
                    } else {
                        failedCount++;
                    }

                    if (code != null && (code == 42901 || code == 42902)) {
                        quotaReached = true;
                        quotaCode = code;
                        quotaMsg = message;
                    }
                } catch (IllegalArgumentException e) {
                    failedCount++;
                    itemResults.add(buildBatchItem(phone, 400, e.getMessage(), null, latestQuota));
                } catch (Exception e) {
                    failedCount++;
                    itemResults.add(buildBatchItem(phone, 500, "查询失败", null, latestQuota));
                }
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("total", phones.size());
            data.put("successCount", successCount);
            data.put("failedCount", failedCount);
            data.put("results", itemResults);
            if (latestQuota != null) {
                data.put("quota", latestQuota);
            }

            return AjaxResult.success("批量查询完成", data);
        } catch (Exception e) {
            return AjaxResult.error("批量查询失败" + e.getMessage());
        }
    }

    @Operation(summary = "查询操作IP的查询记录")
    @GetMapping("/logs")
    public TableDataInfo<UserApiQueryRecord> logs(@RequestParam(required = false) String ip,
                                                  @RequestParam(required = false) String beginTime,
                                                  @RequestParam(required = false) String endTime) {
        startPage();
        List<UserApiQueryRecord> list = freeQueryService.listIpLogs(ip, beginTime, endTime);
        return getDataTable(list);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildBatchItem(String phone, Integer code, String message, Object data, Object quota) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("phone", phone);
        row.put("code", code);
        row.put("message", message);
        row.put("data", data);
        row.put("quota", quota);
        return row;
    }

    private String normalizeLoginAccount(String val) {
        return val == null ? "" : val.trim();
    }

    private SysUser resolveLoginUser(String account) {
        String loginAccount = normalizeLoginAccount(account);
        if (loginAccount.isEmpty()) {
            return null;
        }
        SysUser user = sysUserService.selectUserByUserName(loginAccount);
        if (user != null) {
            return user;
        }
        String phone = normalizeLoginPhone(loginAccount);
        if (isValidLoginPhone(phone)) {
            return sysUserService.selectUserByPhone(phone);
        }
        return null;
    }

    private String resolveBatchDeviceId(SysUser user, String fallbackAccount) {
        String phone = normalizeLoginPhone(user != null ? user.getPhonenumber() : null);
        if (isValidLoginPhone(phone)) {
            return phone;
        }
        if (user != null && user.getUserId() != null) {
            return "uid#" + user.getUserId();
        }
        String account = normalizeLoginAccount(fallbackAccount);
        return account.isEmpty() ? "free-query-user" : "acc#" + account;
    }

    private String normalizeLoginPhone(String val) {
        return val == null ? "" : val.trim().replaceAll("[^\\d]", "");
    }

    private boolean isValidLoginPhone(String phone) {
        return phone != null && phone.matches("^1\\d{10}$");
    }

    private boolean isOpenedUser(SysUser user) {
        if (user == null) {
            return false;
        }
        Integer delFlag = user.getDelFlag();
        return delFlag == null || delFlag == 0;
    }

    private boolean isEnabledUser(SysUser user) {
        return user != null && "0".equals(user.getStatus());
    }

    private boolean isPasswordMatched(String rawPassword, String encodedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(encodedPassword)) {
            return false;
        }
        try {
            return SecurityUtils.matchesPassword(rawPassword, encodedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}