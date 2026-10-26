package com.geek.server.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.common.core.domain.entity.SysMenu;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.common.exception.ServiceException;
import com.geek.common.utils.SecurityUtils;
import com.geek.server.domain.*;
import com.geek.server.domain.dto.MarkOrderCreateRequest;
import com.geek.server.domain.dto.MarkOrderItemProcessRequest;
import com.geek.server.domain.dto.OptimizedBatchItemOutcome;
import com.geek.server.domain.entity.BatchTask;
import com.geek.server.domain.vo.ApiRequestVO;
import com.geek.server.domain.vo.MarkOrderPrecheckResultVO;
import com.geek.server.mapper.*;
import com.geek.server.service.IMarkPlatformMappingService;
import com.geek.server.service.IMarkUserNoticeService;
import com.geek.server.service.IUserPlatformUrlConfigService;
import com.geek.server.service.IUserAggregateConfigService;
import com.geek.server.service.IOptimizedBatchApiExecutor;
import com.geek.system.mapper.SysUserMapper;
import com.mybatisflex.core.query.QueryChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MarkWumaProcessingTest {
    private final MarkOrderServiceImpl service = new MarkOrderServiceImpl();
    private final MarkOrderMapper orders = mock(MarkOrderMapper.class);
    private final MarkOrderItemMapper items = mock(MarkOrderItemMapper.class);
    private final MarkUserPlatformQuotaMapper quotas = mock(MarkUserPlatformQuotaMapper.class);
    private final MarkUserPlatformPriceMapper prices = mock(MarkUserPlatformPriceMapper.class);
    private final MarkWalletLogMapper wallets = mock(MarkWalletLogMapper.class);
    private final IMarkPlatformMappingService mappings = mock(IMarkPlatformMappingService.class);
    private final IUserPlatformUrlConfigService queryApis = mock(IUserPlatformUrlConfigService.class);
    private final IOptimizedBatchApiExecutor executor = mock(IOptimizedBatchApiExecutor.class);
    private final Map<String, MarkUserPlatformQuota> balances = new LinkedHashMap<>();
    private final List<MarkOrderItem> savedItems = new ArrayList<>();
    private MarkOrder savedOrder;
    private MockedStatic<SecurityUtils> security;
    private MockedStatic<QueryChain> queries;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        SysUserMapper users = mock(SysUserMapper.class);
        MarkPlatformTemplateMapper templates = mock(MarkPlatformTemplateMapper.class);
        ReflectionTestUtils.setField(service, "markOrderMapper", orders);
        ReflectionTestUtils.setField(service, "markOrderItemMapper", items);
        ReflectionTestUtils.setField(service, "markUserPlatformQuotaMapper", quotas);
        ReflectionTestUtils.setField(service, "markUserPlatformPriceMapper", prices);
        ReflectionTestUtils.setField(service, "markWalletLogMapper", wallets);
        ReflectionTestUtils.setField(service, "markPlatformMappingService", mappings);
        ReflectionTestUtils.setField(service, "markPlatformTemplateMapper", templates);
        ReflectionTestUtils.setField(service, "sysUserMapper", users);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(service, "markUserNoticeService", mock(IMarkUserNoticeService.class));
        ReflectionTestUtils.setField(service, "userPlatformUrlConfigService", queryApis);
        ReflectionTestUtils.setField(service, "optimizedBatchApiExecutor", executor);
        IUserAggregateConfigService aggregates = mock(IUserAggregateConfigService.class);
        ReflectionTestUtils.setField(service, "userAggregateConfigService", aggregates);
        when(aggregates.selectUserAggregateConfigById(1L)).thenReturn(new UserAggregateConfig());
        List<UserPlatformUrlConfig> providers = new ArrayList<>();
        for (MarkWumaPlatform platform : MarkWumaPlatform.values()) {
            UserPlatformUrlConfig provider = new UserPlatformUrlConfig();
            provider.setId(14L + providers.size());
            provider.setPlatformKey(platform.getCode());
            provider.setPlatformName(platform.getDisplayName());
            provider.setStatus("0");
            provider.setUrl("https://example.invalid/query");
            providers.add(provider);
        }
        when(queryApis.selectUserPlatformUrlConfigList(any())).thenReturn(providers);
        when(executor.execute(any(), any(), anyString())).thenReturn(outcome("yes-骚扰电话"));

        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::getUserId).thenReturn(100L);
        security.when(SecurityUtils::getUsername).thenReturn("wuma-user");
        security.when(SecurityUtils::isAdmin).thenReturn(true);
        QueryChain<SysMenu> menus = mock(QueryChain.class, RETURNS_SELF);
        when(menus.list()).thenReturn(List.of());
        queries = mockStatic(QueryChain.class);
        queries.when(() -> QueryChain.of(SysMenu.class)).thenReturn(menus);

        SysUser user = new SysUser();
        user.setUserId(100L);
        user.setRelMarkTemplate(1L);
        when(users.selectOneById(100L)).thenReturn(user);
        MarkPlatformTemplate template = new MarkPlatformTemplate();
        template.setStatus("0");
        List<Map<String, Object>> templateEntries = new ArrayList<>();
        List<MarkPlatformMapping> platformMappings = new ArrayList<>();
        List<MarkUserPlatformPrice> platformPrices = new ArrayList<>();
        for (MarkWumaPlatform platform : MarkWumaPlatform.values()) {
            String code = platform.getCode();
            templateEntries.add(Map.of("platformCode", code, "platformName", platform.getDisplayName(), "unitPrice", 2));
            MarkPlatformMapping mapping = new MarkPlatformMapping();
            mapping.setSourcePlatformCode(code); mapping.setTargetPlatformCode(code);
            mapping.setSourcePlatformName(platform.getDisplayName()); mapping.setTargetPlatformName(platform.getDisplayName());
            mapping.setSourceEnabled("0"); mapping.setTargetEnabled("0"); mapping.setMappingEnabled("0"); mapping.setSortNum(20);
            platformMappings.add(mapping);
            MarkUserPlatformQuota quota = new MarkUserPlatformQuota();
            quota.setUserId(100L); quota.setPlatformCode(code); quota.setRemainCount(10L);
            balances.put(code, quota);
            when(quotas.selectByUserAndPlatform(100L, code)).thenReturn(quota);
            when(quotas.selectByUserAndPlatformForUpdate(100L, code)).thenReturn(quota);
            MarkUserPlatformPrice price = new MarkUserPlatformPrice();
            price.setPlatformCode(code); price.setStatus("0"); price.setUnitPrice(2L);
            platformPrices.add(price);
            when(prices.selectByUserAndPlatform(100L, code)).thenReturn(price);
        }
        try { template.setTemplateInfo(new ObjectMapper().writeValueAsString(templateEntries)); }
        catch (Exception ex) { throw new AssertionError(ex); }
        when(templates.selectMarkPlatformTemplateById(1L)).thenReturn(template);
        when(prices.selectMarkUserPlatformPriceList(any())).thenReturn(platformPrices);
        when(quotas.selectByUserId(100L)).thenReturn(new ArrayList<>(balances.values()));
        when(mappings.selectList(any())).thenReturn(platformMappings);
        when(mappings.selectEnabledSourceCodes()).thenReturn(balances.keySet());
        when(orders.selectAgentUserIdByMarkTemplate(1L)).thenReturn(200L);
        when(orders.selectOrderIdByUserAndRequestNo(anyLong(), anyString())).thenReturn(null);
        when(orders.insertMarkOrder(any())).thenAnswer(invocation -> {
            savedOrder = invocation.getArgument(0); savedOrder.setId(22L); return 1;
        });
        when(orders.selectMarkOrderById(22L)).thenAnswer(invocation -> savedOrder);
        when(orders.lockMarkOrderById(22L)).thenReturn(22L);
        when(items.insertMarkOrderItem(any())).thenAnswer(invocation -> {
            MarkOrderItem item = invocation.getArgument(0);
            item.setId(11L + savedItems.size()); savedItems.add(item); return 1;
        });
        when(items.selectMarkOrderItemsByOrderId(22L)).thenReturn(savedItems);
        when(items.selectMarkOrderItemsByOrderIdForUpdate(22L)).thenReturn(savedItems);
        when(items.selectMarkOrderItemById(11L)).thenAnswer(invocation -> savedItems.isEmpty() ? null : savedItems.get(0));
    }

    @AfterEach
    void tearDown() { queries.close(); security.close(); }

    @ParameterizedTest
    @ValueSource(strings = {"wuma_taidixiong", "wuma_tengxun", "wuma_360"})
    void checksDeduplicatedPhonesBeforeSubmissionAndOnlyDebitsItsOwnPlatform(String code) {
        service.createOrder(request(code));
        assertEquals(code, savedOrder.getPlatformCode());
        assertEquals(200L, savedOrder.getAssignedAgentId());
        assertEquals(4L, savedOrder.getTotalAmount());
        assertEquals(2, savedItems.size());
        assertEquals("用户处理说明", savedItems.get(0).getUserRemark());
        assertEquals("0", savedItems.get(0).getProcessStatus());
        assertEquals(6L, balances.get(code).getRemainCount());
        balances.forEach((otherCode, quota) -> { if (!otherCode.equals(code)) assertEquals(10L, quota.getRemainCount()); });
        ArgumentCaptor<MarkWalletLog> log = ArgumentCaptor.forClass(MarkWalletLog.class);
        verify(wallets).insertMarkWalletLog(log.capture());
        assertEquals(code, log.getValue().getPlatformCode());
        assertEquals(-4L, log.getValue().getChangeAmount());
        ArgumentCaptor<ApiRequestVO> calls = ArgumentCaptor.forClass(ApiRequestVO.class);
        verify(executor, times(2)).execute(calls.capture(), any(), anyString());
        assertEquals(List.of("13800138000", "01012345678"), calls.getAllValues().stream().map(ApiRequestVO::getPhoneNumber).toList());
        String expectedName = Arrays.stream(MarkWumaPlatform.values()).filter(platform -> platform.getCode().equals(code)).findFirst().orElseThrow().getDisplayName();
        assertTrue(calls.getAllValues().stream().allMatch(call -> expectedName.equals(call.getPlatformName())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"wuma_taidixiong", "wuma_tengxun", "wuma_360"})
    void refundsFailureOnceAndRechargesSuccessEvenAfterResetToPending(String code) {
        service.createOrder(request(code));
        clearInvocations(queryApis, executor);
        savedOrder.setAuditStatus("1");
        service.feedbackOrderItem(11L, feedback("2"));
        service.feedbackOrderItem(11L, feedback("2"));
        assertEquals(8L, balances.get(code).getRemainCount());
        assertEquals("1", savedItems.get(0).getRefunded());
        service.feedbackOrderItem(11L, feedback("0"));
        service.feedbackOrderItem(11L, feedback("1"));
        assertEquals(6L, balances.get(code).getRemainCount());
        assertEquals("0", savedItems.get(0).getRefunded());
        assertEquals("wuma-user", savedItems.get(0).getProcessedBy());
        assertNotNull(savedItems.get(0).getProcessedTime());
        ArgumentCaptor<MarkWalletLog> log = ArgumentCaptor.forClass(MarkWalletLog.class);
        verify(wallets, times(3)).insertMarkWalletLog(log.capture());
        assertEquals(List.of("DEDUCT", "REFUND", "DEDUCT"), log.getAllValues().stream().map(MarkWalletLog::getBizType).toList());
        assertTrue(log.getAllValues().stream().allMatch(entry -> code.equals(entry.getPlatformCode())));
        verify(orders, times(4)).lockMarkOrderById(22L);
        verifyNoInteractions(queryApis);
        verifyNoInteractions(executor);
    }

    @Test
    void returnsExistingOrderOnRetryEvenWhenFirstSubmissionUsedAllQuota() {
        MarkOrderCreateRequest request = request("wuma_360");
        balances.get("wuma_360").setRemainCount(4L);
        service.createOrder(request);
        when(orders.selectOrderIdByUserAndRequestNo(100L, "wuma-request")).thenReturn(22L);
        assertEquals(22L, service.createOrder(request).getOrder().getId());
        assertEquals(0L, balances.get("wuma_360").getRemainCount());
        verify(orders, times(1)).insertMarkOrder(any());
        verify(wallets, times(1)).insertMarkWalletLog(any());
        verify(executor, times(2)).execute(any(), any(), anyString());
    }

    @Test
    void refusesSubmissionWhenQuotaIsInsufficient() {
        balances.get("wuma_tengxun").setRemainCount(1L);
        assertThrows(ServiceException.class, () -> service.createOrder(request("wuma_tengxun")));
        verify(orders, never()).insertMarkOrder(any());
        verifyNoInteractions(wallets, queryApis);
    }

    @Test
    void usesLatestRefundFlagAfterWaitingForAnotherFeedbackTransaction() {
        service.createOrder(request("wuma_360"));
        savedOrder.setAuditStatus("1");
        service.feedbackOrderItem(11L, feedback("2"));
        MarkOrderItem staleItem = new MarkOrderItem();
        staleItem.setId(11L); staleItem.setOrderId(22L);
        staleItem.setProcessStatus("0"); staleItem.setRefunded("0");
        when(items.selectMarkOrderItemById(11L)).thenReturn(staleItem);

        service.feedbackOrderItem(11L, feedback("2"));

        assertEquals(8L, balances.get("wuma_360").getRemainCount());
        verify(wallets, times(2)).insertMarkWalletLog(any());
        verify(items, times(2)).selectMarkOrderItemsByOrderIdForUpdate(22L);
    }

    @Test
    void rechargesRefundedPendingItemWhenCompletingWholeOrderAsSuccess() {
        service.createOrder(request("wuma_tengxun"));
        savedOrder.setAuditStatus("1");
        service.feedbackOrderItem(11L, feedback("2"));
        service.feedbackOrderItem(11L, feedback("0"));

        service.completeOrder(22L, feedback("1"));

        assertEquals(6L, balances.get("wuma_tengxun").getRemainCount());
        assertTrue(savedItems.stream().allMatch(item -> "1".equals(item.getProcessStatus()) && "0".equals(item.getRefunded())));
        verify(wallets, times(3)).insertMarkWalletLog(any());
    }

    @Test
    void excludesDisabledPlatformFromUserOptionsAndRejectsSubmission() {
        List<MarkPlatformMapping> configured = mappings.selectList(new MarkPlatformMapping());
        configured.stream().filter(mapping -> "wuma_360".equals(mapping.getSourcePlatformCode())).forEach(mapping -> mapping.setMappingEnabled("1"));
        assertFalse(service.selectMyPlatformPriceList().stream().anyMatch(price -> "wuma_360".equals(price.getPlatformCode())));
        assertThrows(ServiceException.class, () -> service.createOrder(request("wuma_360")));
        verify(orders, never()).insertMarkOrder(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"wuma_taidixiong", "wuma_tengxun", "wuma_360"})
    void agentStartsWumaProcessingBeforeAutomaticInspection(String code) {
        service.createOrder(request(code));
        savedOrder.setAuditStatus("1");

        MarkOrderItemProcessRequest start = new MarkOrderItemProcessRequest();
        start.setProcessStatus("3");
        service.feedbackOrderItem(11L, start);

        assertEquals("3", savedItems.get(0).getProcessStatus());
        assertEquals("代理已开始处理", savedItems.get(0).getProcessResult());
        when(items.selectPendingProcessItems(code, 1)).thenReturn(List.of(savedItems.get(0)));
        assertTrue(service.hasPendingInspection(code));

        // A provider disabled after submission must not complete or refund an agent-started item.
        clearInvocations(queryApis);
        when(queryApis.selectUserPlatformUrlConfigList(any())).thenReturn(List.of());
        assertEquals("FAILED", service.processGenericPendingOrderItemAuto(11L, code, "SUCCESS"));
        assertEquals("3", savedItems.get(0).getProcessStatus());
        assertEquals("0", savedItems.get(0).getRefunded());
        verify(queryApis).selectUserPlatformUrlConfigList(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"wuma_taidixiong", "wuma_tengxun", "wuma_360"})
    void rejectsUnmarkedNumbersEvenWhenCallingCreateDirectly(String code) {
        when(executor.execute(any(), any(), anyString())).thenReturn(outcome("no"));
        ServiceException error = assertThrows(ServiceException.class, () -> service.createOrder(request(code)));
        assertTrue(error.getMessage().contains("无标记"));
        assertRejectedWithoutWrites(code);
    }

    @ParameterizedTest
    @ValueSource(strings = {"wuma_taidixiong", "wuma_tengxun", "wuma_360"})
    void rejectsFailedQueryAndEntireMixedBatch(String code) {
        BatchTask.ApiResult failure = BatchTask.ApiResult.builder().success(false).error("检测服务超时").build();
        when(executor.execute(any(), any(), anyString())).thenReturn(outcome("yes-骚扰电话"),
                OptimizedBatchItemOutcome.builder().apiResult(failure).build());
        ServiceException error = assertThrows(ServiceException.class, () -> service.createOrder(request(code)));
        assertTrue(error.getMessage().contains("检测失败"));
        assertTrue(error.getMessage().contains("01012345678"));
        assertRejectedWithoutWrites(code);
    }

    @ParameterizedTest
    @ValueSource(strings = {"wuma_taidixiong", "wuma_tengxun", "wuma_360"})
    void rejectsMixedMarkedAndUnmarkedBatch(String code) {
        when(executor.execute(any(), any(), anyString())).thenReturn(outcome("yes-骚扰电话"), outcome("no"));
        assertThrows(ServiceException.class, () -> service.createOrder(request(code)));
        assertRejectedWithoutWrites(code);
    }

    @ParameterizedTest
    @ValueSource(strings = {"wuma_taidixiong", "wuma_tengxun", "wuma_360"})
    void rejectsMissingOrDisabledProvider(String code) {
        queryApis.selectUserPlatformUrlConfigList(new UserPlatformUrlConfig()).forEach(provider -> provider.setStatus("1"));
        assertThrows(ServiceException.class, () -> service.createOrder(request(code)));
        assertRejectedWithoutWrites(code);
        verifyNoInteractions(executor);
    }

    @Test
    void rejectsProviderResponseWithoutMarkResults() {
        BatchTask.ApiResult empty = BatchTask.ApiResult.builder().success(true).data(Map.of()).build();
        when(executor.execute(any(), any(), anyString())).thenReturn(OptimizedBatchItemOutcome.builder().apiResult(empty).build());
        assertThrows(ServiceException.class, () -> service.createOrder(request("wuma_360")));
        assertRejectedWithoutWrites("wuma_360");
    }

    @Test
    void cannotUseSubmittedPlatformNameToBypassMissingWumaProvider() {
        UserPlatformUrlConfig other = new UserPlatformUrlConfig();
        other.setId(1L); other.setPlatformKey("tengxun"); other.setPlatformName("腾讯"); other.setStatus("0");
        when(queryApis.selectUserPlatformUrlConfigList(any())).thenReturn(List.of(other));
        MarkOrderCreateRequest request = request("wuma_360");
        request.setPlatformName("腾讯");
        assertThrows(ServiceException.class, () -> service.createOrder(request));
        assertRejectedWithoutWrites("wuma_360");
        verifyNoInteractions(executor);
    }

    @Test
    void repeatsValidationAtCreationAfterSuccessfulWebPrecheck() {
        MarkOrderCreateRequest request = request("wuma_360");
        assertEquals(2, service.precheckOrder(request).getMarkedCount());
        when(executor.execute(any(), any(), anyString())).thenReturn(outcome("no"));
        assertThrows(ServiceException.class, () -> service.createOrder(request));
        assertRejectedWithoutWrites("wuma_360");
    }

    @Test
    void customerApiChecksOnceAndRetainsCredentialAgentAndIdempotency() {
        MarkOrderCreateRequest request = request("wuma_360");
        service.createCustomerApiOrder(request, 300L);
        assertEquals(300L, savedOrder.getAssignedAgentId());
        when(orders.selectOrderIdByUserAndRequestNo(100L, "wuma-request")).thenReturn(22L);
        service.createCustomerApiOrder(request, 300L);
        verify(executor, times(2)).execute(any(), any(), anyString());
        verify(orders).insertMarkOrder(any());
        verify(wallets).insertMarkWalletLog(any());
    }

    private void assertRejectedWithoutWrites(String code) {
        assertEquals(10L, balances.get(code).getRemainCount());
        verify(quotas, never()).selectByUserAndPlatformForUpdate(anyLong(), anyString());
        verify(quotas, never()).updateMarkUserPlatformQuota(any());
        verify(orders, never()).insertMarkOrder(any());
        verify(items, never()).insertMarkOrderItem(any());
        verifyNoInteractions(wallets);
    }

    private OptimizedBatchItemOutcome outcome(String status) {
        BatchTask.ApiResult result = BatchTask.ApiResult.builder().success(true)
                .data(Map.of("platformResults", List.of(Map.of("platform", "测试平台", "status", status)))).build();
        return OptimizedBatchItemOutcome.builder().apiResult(result).build();
    }

    private MarkOrderCreateRequest request(String code) {
        MarkOrderCreateRequest request = new MarkOrderCreateRequest();
        request.setPlatformCode(code); request.setPlatformName("无码测试");
        request.setPhones(List.of("13800138000", "13800138000", "01012345678"));
        request.setRequestNo("wuma-request"); request.setRemark("用户处理说明");
        return request;
    }

    private MarkOrderItemProcessRequest feedback(String status) {
        MarkOrderItemProcessRequest request = new MarkOrderItemProcessRequest();
        request.setProcessStatus(status);
        return request;
    }
}
