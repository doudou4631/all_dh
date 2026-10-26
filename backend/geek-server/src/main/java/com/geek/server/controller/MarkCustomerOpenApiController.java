package com.geek.server.controller;

import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.common.exception.ServiceException;
import com.geek.server.domain.MarkOpenApiPrincipal;
import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.MarkUserPlatformPrice;
import com.geek.server.domain.dto.MarkCustomerApiOrderRequest;
import com.geek.server.domain.dto.MarkOrderCreateRequest;
import com.geek.server.domain.vo.MarkOrderDetailVO;
import com.geek.server.service.IMarkOrderService;
import com.geek.server.service.MarkOpenApiCredentialService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Customer-facing mark API. A customer credential can only submit and read its own orders. */
@RestController
@RequestMapping("/open/v1/mark/customer")
@RequiredArgsConstructor
public class MarkCustomerOpenApiController extends BaseController {
    private final IMarkOrderService markOrderService;
    private final MarkOpenApiCredentialService credentialService;

    @GetMapping("/platforms")
    public AjaxResult platforms(HttpServletRequest request) {
        MarkOpenApiPrincipal principal = customerPrincipal(request, MarkOpenApiCredentialService.SCOPE_CUSTOMER_READ);
        long start = System.currentTimeMillis();
        try {
            Set<String> granted = principal.getPlatformCodes();
            List<MarkUserPlatformPrice> prices = markOrderService.selectMyPlatformPriceList().stream()
                    .filter(price -> granted.contains(StringUtils.lowerCase(StringUtils.trimToEmpty(price.getPlatformCode()))))
                    .toList();
            log(principal, request, null, true, "OK", start);
            return AjaxResult.success(prices);
        } catch (RuntimeException ex) {
            log(principal, request, null, false, ex.getMessage(), start);
            throw ex;
        }
    }

    @GetMapping("/{platformCode}/balance")
    public AjaxResult balance(@PathVariable String platformCode, HttpServletRequest request) {
        MarkOpenApiPrincipal principal = customerPrincipal(request, MarkOpenApiCredentialService.SCOPE_CUSTOMER_READ);
        String platform = requirePlatform(principal, platformCode);
        long start = System.currentTimeMillis();
        try {
            MarkUserPlatformPrice price = markOrderService.selectMyPlatformPriceList().stream()
                    .filter(item -> platform.equalsIgnoreCase(item.getPlatformCode()))
                    .findFirst()
                    .orElseThrow(() -> new ServiceException("当前客户未开通该平台"));
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("platformCode", platform);
            data.put("platformName", price.getPlatformName());
            data.put("remain", price.getRemainCount());
            data.put("unitPrice", price.getUnitPrice());
            log(principal, request, platform, true, "OK", start);
            return AjaxResult.success(data);
        } catch (RuntimeException ex) {
            log(principal, request, platform, false, ex.getMessage(), start);
            throw ex;
        }
    }

    @PostMapping("/{platformCode}/check")
    public AjaxResult check(@PathVariable String platformCode, @Valid @RequestBody MarkCustomerApiOrderRequest body, HttpServletRequest request) {
        MarkOpenApiPrincipal principal = customerPrincipal(request, MarkOpenApiCredentialService.SCOPE_CUSTOMER_READ);
        String platform = requirePlatform(principal, platformCode);
        MarkOrderCreateRequest orderRequest = toOrderRequest(platform, body);
        long start = System.currentTimeMillis();
        try {
            Object data = markOrderService.precheckOrder(orderRequest);
            log(principal, request, platform, true, "OK", start);
            return AjaxResult.success("检测完成", data);
        } catch (RuntimeException ex) {
            log(principal, request, platform, false, ex.getMessage(), start);
            throw ex;
        }
    }

    @PostMapping("/{platformCode}/submit")
    public AjaxResult submit(@PathVariable String platformCode, @Valid @RequestBody MarkCustomerApiOrderRequest body, HttpServletRequest request) {
        MarkOpenApiPrincipal principal = customerPrincipal(request, MarkOpenApiCredentialService.SCOPE_CUSTOMER_SUBMIT);
        String platform = requirePlatform(principal, platformCode);
        if (StringUtils.isBlank(body.getRequestNo())) {
            throw new ServiceException("requestNo不能为空，重试时必须使用相同请求编号");
        }
        credentialService.requireActiveAssignedAgent(principal);
        MarkOrderCreateRequest orderRequest = toOrderRequest(platform, body);
        long start = System.currentTimeMillis();
        try {
            Long assignedAgentId = principal.getCredential().getAssignedAgentId();
            MarkOrderDetailVO data = markOrderService.createCustomerApiOrder(orderRequest, assignedAgentId);
            log(principal, request, platform, true, "OK", start);
            return AjaxResult.success("已受理，订单已分配给责任代理", data);
        } catch (RuntimeException ex) {
            log(principal, request, platform, false, ex.getMessage(), start);
            throw ex;
        }
    }

    @GetMapping("/orders")
    public TableDataInfo orders(@RequestParam String platformCode, MarkOrder query, HttpServletRequest request) {
        MarkOpenApiPrincipal principal = customerPrincipal(request, MarkOpenApiCredentialService.SCOPE_CUSTOMER_READ);
        String platform = requirePlatform(principal, platformCode);
        query.setPlatformCode(platform);
        long start = System.currentTimeMillis();
        try {
            startPage();
            List<MarkOrder> rows = markOrderService.selectMyOrderList(query);
            log(principal, request, platform, true, "OK", start);
            return getDataTable(rows);
        } catch (RuntimeException ex) {
            log(principal, request, platform, false, ex.getMessage(), start);
            throw ex;
        }
    }

    @GetMapping("/orders/{orderId}")
    public AjaxResult order(@PathVariable Long orderId, HttpServletRequest request) {
        MarkOpenApiPrincipal principal = customerPrincipal(request, MarkOpenApiCredentialService.SCOPE_CUSTOMER_READ);
        long start = System.currentTimeMillis();
        try {
            MarkOrderDetailVO detail = markOrderService.selectMyOrderDetail(orderId);
            String platform = detail.getOrder() == null ? null : detail.getOrder().getPlatformCode();
            requirePlatform(principal, platform);
            log(principal, request, platform, true, "OK", start);
            return AjaxResult.success(detail);
        } catch (RuntimeException ex) {
            log(principal, request, null, false, ex.getMessage(), start);
            throw ex;
        }
    }

    private MarkOpenApiPrincipal customerPrincipal(HttpServletRequest request, String scope) {
        Object value = request.getAttribute("markOpenApiPrincipal");
        if (!(value instanceof MarkOpenApiPrincipal principal)) {
            throw new ServiceException("API 身份认证失败");
        }
        credentialService.requireCredentialType(principal, MarkOpenApiCredentialService.TYPE_CUSTOMER);
        credentialService.requireScope(principal, scope);
        return principal;
    }

    private String requirePlatform(MarkOpenApiPrincipal principal, String platformCode) {
        String platform = StringUtils.lowerCase(StringUtils.trimToEmpty(platformCode));
        credentialService.requirePlatform(principal, platform);
        return platform;
    }

    private MarkOrderCreateRequest toOrderRequest(String platformCode, MarkCustomerApiOrderRequest request) {
        MarkOrderCreateRequest orderRequest = new MarkOrderCreateRequest();
        orderRequest.setPlatformCode(platformCode);
        orderRequest.setPhones(request.getPhones());
        orderRequest.setRequestNo(request.getRequestNo());
        orderRequest.setRemark(request.getRemark());
        return orderRequest;
    }

    private void log(MarkOpenApiPrincipal principal, HttpServletRequest request, String platform, boolean success, String message, long start) {
        credentialService.log(principal, request.getRequestURI(), request.getMethod(), platform, success, message, System.currentTimeMillis() - start);
    }
}
