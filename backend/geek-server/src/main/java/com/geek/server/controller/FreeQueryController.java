package com.geek.server.controller;

import com.geek.common.annotation.Anonymous;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.utils.ip.IpUtils;
import com.geek.server.domain.UserApiQueryRecord;
import com.geek.server.domain.vo.FreeSingleQueryRequest;
import com.geek.server.service.IFreeQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Tag(name = "free-query")
@RestController
@RequestMapping(value = "/server/freeQuery", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@RequiredArgsConstructor
public class FreeQueryController extends BaseController {

    private final IFreeQueryService freeQueryService;

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

    @Operation(summary = "查询操作IP的查询记录")
    @GetMapping("/logs")
    public TableDataInfo<UserApiQueryRecord> logs(@RequestParam(required = false) String ip,
                              @RequestParam(required = false) String beginTime,
                              @RequestParam(required = false) String endTime) {
        startPage();
        List<UserApiQueryRecord> list = freeQueryService.listIpLogs(ip, beginTime, endTime);
        return getDataTable(list);
    }
}

