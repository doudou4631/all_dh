package com.geek.server.service;

import com.geek.server.domain.MarkUserNotice;
import com.geek.server.domain.vo.MarkAgentAuditStatsVO;

import java.util.List;

/**
 * User notice service.
 */
public interface IMarkUserNoticeService {

    List<MarkUserNotice> selectMyNoticeList(MarkUserNotice query);

    int countMyUnread();

    MarkUserNotice selectMyNoticeDetail(Long noticeId);

    int markMyNoticeRead(Long noticeId);

    int markAllMyNoticeRead();

    void sendOrderAuditNotice(Long userId, Long orderId, String orderNo, String platformName,
                              String auditStatus, String auditOpinion, String operator);

    void sendOrderSubmitNotice(Long userId, Long orderId, String orderNo, String platformName, String operator);

    void sendTencentProcessNotice(Long userId, Long orderId, String orderNo, String phone,
                                  boolean accepted, String failMessage, String operator);

    void sendTdGaopinProcessNotice(Long userId, Long orderId, String orderNo, String phone,
                                   String detail, String operator);

    MarkAgentAuditStatsVO selectAgentAuditStats();
}
