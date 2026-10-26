package com.geek.server.service.impl;

import com.geek.common.exception.ServiceException;
import com.geek.server.domain.MarkInspectionPolicy;
import com.geek.server.domain.MarkInspectionRunLog;
import com.geek.server.domain.MarkPlatformMapping;
import com.geek.server.mapper.MarkInspectionMapper;
import com.geek.server.service.IMarkOrderService;
import com.geek.server.service.IMarkPlatformMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Date;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MarkInspectionServiceTest {
    private final MarkInspectionMapper mapper = mock(MarkInspectionMapper.class);
    private final IMarkOrderService orders = mock(IMarkOrderService.class);
    private final IMarkPlatformMappingService platforms = mock(IMarkPlatformMappingService.class);
    private final MarkInspectionServiceImpl service = new MarkInspectionServiceImpl(mapper, orders, platforms);

    @BeforeEach
    void setup() {
        MarkPlatformMapping wuma = new MarkPlatformMapping();
        wuma.setTargetPlatformCode("wuma_360"); wuma.setTargetPlatformName("360无码");
        MarkPlatformMapping td = new MarkPlatformMapping();
        td.setTargetPlatformCode("td_gaopin"); td.setTargetPlatformName("泰迪高频");
        when(platforms.selectList(any())).thenReturn(List.of(wuma, td));
        when(platforms.isTargetEnabled(anyString())).thenReturn(true);
        when(mapper.insertPolicy(any())).thenReturn(1);
        when(mapper.updatePolicy(any())).thenReturn(1);
    }

    private MarkInspectionPolicy policy() {
        MarkInspectionPolicy p = new MarkInspectionPolicy();
        p.setId(10L); p.setPolicyName("360自动巡检"); p.setPlatformCode("wuma_360");
        p.setOrderScope("BOTH"); p.setAutoTakeover(true); p.setAutoAction("SUCCESS");
        p.setIntervalValue(1); p.setIntervalUnit("HOUR"); p.setIntervalSeconds(3600); p.setBatchLimit(100);
        p.setStatus("0"); p.setRunning("0");
        return p;
    }

    @Test
    void createsPolicyWithSelectedSwitchScopeAndTakeover() {
        MarkInspectionPolicy p = policy(); p.setId(null);
        long before = System.currentTimeMillis();
        assertEquals(1, service.savePolicy(p));
        assertEquals("0", p.getStatus()); assertEquals("BOTH", p.getOrderScope());
        assertTrue(p.getAutoTakeover()); assertEquals(3600, p.getIntervalSeconds());
        assertTrue(p.getNextRunTime().getTime() >= before + 3600000);
        verify(mapper).insertPolicy(p);
    }

    @Test
    void oldClientsUpdatingSwitchKeepExistingScopeAndDisabledTakeover() {
        MarkInspectionPolicy stored = policy(); stored.setOrderScope("PROCESSING"); stored.setAutoTakeover(false);
        when(mapper.selectPolicyById(10L)).thenReturn(stored);
        MarkInspectionPolicy request = policy(); request.setOrderScope(null); request.setAutoTakeover(null); request.setStatus("1");
        service.updatePolicy(request);
        assertEquals("PROCESSING", request.getOrderScope()); assertFalse(request.getAutoTakeover());
        assertEquals("1", request.getStatus());
    }

    @Test
    void persistsExplicitlyDisabledTakeover() {
        when(mapper.selectPolicyById(10L)).thenReturn(policy());
        MarkInspectionPolicy request = policy(); request.setOrderScope("PROCESSING"); request.setAutoTakeover(false);
        service.updatePolicy(request);
        assertFalse(request.getAutoTakeover()); verify(mapper).updatePolicy(request);
    }

    @Test
    void rejectsWumaPendingScopeWithoutTakeoverAndInvalidScope() {
        MarkInspectionPolicy p = policy(); p.setAutoTakeover(false);
        assertThrows(ServiceException.class, () -> service.savePolicy(p));
        p.setAutoTakeover(true); p.setOrderScope("COMPLETED");
        assertThrows(ServiceException.class, () -> service.savePolicy(p));
        verify(mapper, never()).insertPolicy(any());
    }

    @Test
    void preservesSpecialTeddyHighFrequencyQueueRules() {
        MarkInspectionPolicy p = policy(); p.setPlatformCode("td_gaopin");
        assertThrows(ServiceException.class, () -> service.savePolicy(p));
        p.setOrderScope("PENDING"); p.setAutoTakeover(false);
        assertEquals(1, service.savePolicy(p));
    }

    @Test
    void cannotEditRunningPolicyOrLoseRaceAgainstStartingRun() {
        MarkInspectionPolicy stored = policy(); stored.setRunning("1");
        when(mapper.selectPolicyById(10L)).thenReturn(stored);
        assertThrows(ServiceException.class, () -> service.updatePolicy(policy()));
        verify(mapper, never()).updatePolicy(any());
        stored.setRunning("0"); when(mapper.updatePolicy(any())).thenReturn(0);
        assertThrows(ServiceException.class, () -> service.updatePolicy(policy()));
    }

    @Test
    void scheduledAndManualRunsUseTheSameSavedSettingsAndDetailedCounts() {
        MarkInspectionPolicy p = policy();
        when(mapper.selectPolicyById(10L)).thenReturn(p);
        when(mapper.selectDuePolicies(any(Date.class))).thenReturn(List.of(p));
        when(mapper.claimPolicy(10L)).thenReturn(1);
        when(orders.hasPendingInspection(p)).thenReturn(true);
        when(mapper.insertLog(any())).thenAnswer(call -> { ((MarkInspectionRunLog) call.getArgument(0)).setId(30L); return 1; });
        when(orders.processPendingInspection(p, 30L)).thenReturn(Map.of(
                "scannedCount", 4, "checkedCount", 4, "successCount", 1, "matchedCount", 1,
                "markedCount", 2, "failedCount", 1, "skippedCount", 0));
        service.runDuePolicies();
        MarkInspectionRunLog log = service.runPolicy(10L, "MANUAL");
        assertEquals(4, log.getCheckedCount()); assertEquals(2, log.getMarkedCount());
        assertEquals(1, log.getSuccessCount()); assertEquals("PARTIAL", log.getRunStatus());
        verify(orders, times(2)).processPendingInspection(p, 30L);
        verify(mapper, times(2)).releasePolicy(eq(10L), any(), any());
    }

    @Test
    void noEligibleOrdersDefersPolicyWithoutRunningQueries() {
        MarkInspectionPolicy p = policy(); when(mapper.selectDuePolicies(any())).thenReturn(List.of(p));
        service.runDuePolicies();
        verify(orders).hasPendingInspection(p); verify(mapper).deferPolicy(eq(10L), any());
        verify(mapper, never()).claimPolicy(anyLong());
    }

    @Test
    void duplicateRunCannotExecuteWhenClaimFails() {
        when(mapper.selectPolicyById(10L)).thenReturn(policy()); when(mapper.claimPolicy(10L)).thenReturn(0);
        assertThrows(ServiceException.class, () -> service.runPolicy(10L, "MANUAL"));
        verifyNoInteractions(orders);
    }
}
