package com.geek.server.controller;

import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.server.domain.vo.ApiRequestVO;
import com.geek.server.service.IApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * API-Controller
 *
 * @author geek
 * @date 2026-03-09
 */
@RestController
@RequestMapping("/server/apiServer")
@Tag(name = "【API】管理")
public class ApiController extends BaseController {

    @Autowired
    private IApiService apiService;

    /**
     * 单次查询号码
     */
    @Operation(summary = "单次查询号码")
    @PreAuthorize("@ss.hasPermi('server:apiServer:singleQuery')")
    @PostMapping(value = "/single")
    public AjaxResult single(@RequestBody ApiRequestVO apiRequestVO)
    {
        try {
                return success(apiService.single(apiRequestVO));
        } catch (UnsupportedEncodingException e) {
            return error(e.getMessage());
        }
    }
}
