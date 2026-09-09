package com.geek.server.controller;

import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.exception.ServiceException;
import com.geek.server.domain.MarkOpenApiPrincipal;
import com.geek.server.domain.dto.MarkOrderItemProcessRequest;
import com.geek.server.domain.vo.MarkAgentOrderItemVO;
import com.geek.server.service.IMarkOrderService;
import com.geek.server.service.MarkOpenApiCredentialService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/open/v1/mark")
@RequiredArgsConstructor
public class MarkOpenApiController extends BaseController {
    private final IMarkOrderService markOrderService;
    private final MarkOpenApiCredentialService credentialService;
    @GetMapping("/items")
    public TableDataInfo items(@RequestParam String platformCode, MarkAgentOrderItemVO query, HttpServletRequest request) {
        MarkOpenApiPrincipal principal = principal(request); credentialService.requireScope(principal, MarkOpenApiCredentialService.SCOPE_READ); credentialService.requirePlatform(principal, platformCode);
        long start = System.currentTimeMillis(); try { query.setPlatformCode(platformCode.toLowerCase()); query.setPhone(request.getParameter("phone")); query.setProcessStatus(request.getParameter("processStatus")); query.setPendingOnly(request.getParameter("pendingOnly")); query.setProcessingOnly(request.getParameter("processingOnly")); List<MarkAgentOrderItemVO> rows = markOrderService.selectAgentOrderItemList(query); credentialService.log(principal, request.getRequestURI(), request.getMethod(), platformCode, true, "OK", System.currentTimeMillis()-start); return getDataTable(rows); } catch (RuntimeException ex) { credentialService.log(principal, request.getRequestURI(), request.getMethod(), platformCode, false, ex.getMessage(), System.currentTimeMillis()-start); throw ex; }
    }
    @PostMapping("/items/{itemId}/feedback")
    public AjaxResult feedback(@PathVariable Long itemId, @Valid @RequestBody MarkOrderItemProcessRequest body, HttpServletRequest request) {
        MarkOpenApiPrincipal principal = principal(request); String platform = credentialService.itemPlatform(itemId); credentialService.requireScope(principal, MarkOpenApiCredentialService.SCOPE_FEEDBACK); credentialService.requirePlatform(principal, platform);
        long start = System.currentTimeMillis(); try { Object data = markOrderService.feedbackOrderItem(itemId, body); credentialService.log(principal, request.getRequestURI(), request.getMethod(), platform, true, "OK", System.currentTimeMillis()-start); return AjaxResult.success("回填成功", data); } catch (RuntimeException ex) { credentialService.log(principal, request.getRequestURI(), request.getMethod(), platform, false, ex.getMessage(), System.currentTimeMillis()-start); throw ex; }
    }
    private MarkOpenApiPrincipal principal(HttpServletRequest request) { Object value=request.getAttribute("markOpenApiPrincipal"); if (!(value instanceof MarkOpenApiPrincipal p)) throw new ServiceException("API 身份认证失败"); return p; }
}
