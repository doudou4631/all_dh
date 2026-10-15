package com.geek.server.service.impl;

import com.geek.common.exception.ServiceException;
import com.geek.common.utils.SecurityUtils;
import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.MarkOrderItem;
import com.geek.server.domain.dto.MarkOrderRemarkUpdateRequest;
import com.geek.server.mapper.MarkOrderItemMapper;
import com.geek.server.mapper.MarkOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MarkAgentOrderItemRemarkTest {

    private final MarkOrderItemMapper itemMapper = mock(MarkOrderItemMapper.class);
    private final MarkOrderMapper orderMapper = mock(MarkOrderMapper.class);
    private final MarkOrderServiceImpl service = new MarkOrderServiceImpl();
    private final MarkOrderItem item = new MarkOrderItem();
    private final MarkOrder order = new MarkOrder();
    private MockedStatic<SecurityUtils> security;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "markOrderItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "markOrderMapper", orderMapper);
        item.setId(11L);
        item.setOrderId(22L);
        item.setProcessStatus("3");
        item.setProcessResult("申诉处理中");
        item.setProcessNote("line=line2｜taskId=task-123");
        item.setUserRemark("用户备注");
        item.setRefunded("0");
        item.setProcessedTime(new Date(1000L));
        order.setId(22L);
        order.setPlatformCode("td_second");
        order.setAuditStatus("1");
        when(itemMapper.selectMarkOrderItemById(11L)).thenReturn(item);
        when(orderMapper.selectMarkOrderById(22L)).thenReturn(order);
        security = mockStatic(SecurityUtils.class);
        security.when(() -> SecurityUtils.hasRole("mark_agent")).thenReturn(true);
        security.when(SecurityUtils::getUsername).thenReturn("test-agent");
    }

    @AfterEach
    void tearDown() {
        security.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "3"})
    void savesRemarkForEveryProcessStatusWithoutChangingProcessingOrUserRemark(String status) {
        item.setProcessStatus(status);

        assertEquals("已联系客户", service.updateAgentOrderItemRemark(11L, request("  已联系客户  ")));

        verify(itemMapper).updateAgentOrderItemRemark(11L, "已联系客户", "test-agent");
        verify(itemMapper, never()).updateMarkOrderItem(any());
        verify(orderMapper, never()).updateMarkOrder(any());
        assertEquals(status, item.getProcessStatus());
        assertEquals("申诉处理中", item.getProcessResult());
        assertEquals("line=line2｜taskId=task-123", item.getProcessNote());
        assertEquals("用户备注", item.getUserRemark());
        assertEquals("0", item.getRefunded());
        assertEquals(new Date(1000L), item.getProcessedTime());
    }

    @Test
    void clearsRemarkBySavingEmptyText() {
        assertEquals("", service.updateAgentOrderItemRemark(11L, request("   ")));
        verify(itemMapper).updateAgentOrderItemRemark(11L, "", "test-agent");
    }

    @Test
    void accepts500CharactersAndRejectsLongerText() {
        String remark = "备".repeat(500);
        assertEquals(remark, service.updateAgentOrderItemRemark(11L, request(remark)));
        assertThrows(ServiceException.class,
                () -> service.updateAgentOrderItemRemark(11L, request(remark + "注")));
        verify(itemMapper, times(1)).updateAgentOrderItemRemark(anyLong(), anyString(), anyString());
    }

    @Test
    void rejectsMissingItem() {
        assertThrows(ServiceException.class,
                () -> service.updateAgentOrderItemRemark(99L, request("备注")));
        verify(itemMapper, never()).updateAgentOrderItemRemark(anyLong(), anyString(), anyString());
    }

    @Test
    void rejectsUsersWithoutAgentOrAdminRole() {
        security.when(() -> SecurityUtils.hasRole("mark_agent")).thenReturn(false);
        assertThrows(ServiceException.class,
                () -> service.updateAgentOrderItemRemark(11L, request("备注")));
        verify(itemMapper, never()).updateAgentOrderItemRemark(anyLong(), anyString(), anyString());
    }

    @Test
    void rejectsOrdersThatHaveNotPassedAudit() {
        order.setAuditStatus("0");
        assertThrows(ServiceException.class,
                () -> service.updateAgentOrderItemRemark(11L, request("备注")));
        verify(itemMapper, never()).updateAgentOrderItemRemark(anyLong(), anyString(), anyString());
    }

    private MarkOrderRemarkUpdateRequest request(String remark) {
        MarkOrderRemarkUpdateRequest request = new MarkOrderRemarkUpdateRequest();
        request.setRemark(remark);
        return request;
    }
}
