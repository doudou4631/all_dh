package com.geek.server.service.impl;

import com.geek.common.exception.ServiceException;
import com.geek.common.utils.DateUtils;
import com.geek.common.utils.SecurityUtils;
import com.geek.server.domain.MarkUserNotice;
import com.geek.server.domain.vo.MarkAgentAuditStatsVO;
import com.geek.server.mapper.MarkUserNoticeMapper;
import com.geek.server.service.IMarkUserNoticeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * User notice service implementation.
 */
@Service
public class MarkUserNoticeServiceImpl implements IMarkUserNoticeService {

    private static final String NOTICE_TYPE_ORDER_AUDIT = "ORDER_AUDIT";
    private static final String BIZ_TYPE_MARK_ORDER = "MARK_ORDER";

    @Autowired
    private MarkUserNoticeMapper markUserNoticeMapper;

    @Override
    public List<MarkUserNotice> selectMyNoticeList(MarkUserNotice query) {
        MarkUserNotice noticeQuery = query == null ? new MarkUserNotice() : query;
        noticeQuery.setUserId(SecurityUtils.getUserId());
        return markUserNoticeMapper.selectMarkUserNoticeList(noticeQuery);
    }

    @Override
    public int countMyUnread() {
        return markUserNoticeMapper.countUnreadByUserId(SecurityUtils.getUserId());
    }

    @Override
    public MarkUserNotice selectMyNoticeDetail(Long noticeId) {
        MarkUserNotice notice = markUserNoticeMapper.selectMarkUserNoticeById(noticeId);
        if (notice == null) {
            throw new ServiceException("\u6d88\u606f\u4e0d\u5b58\u5728");
        }
        if (!SecurityUtils.getUserId().equals(notice.getUserId())) {
            throw new ServiceException("\u65e0\u6743\u67e5\u770b\u8be5\u6d88\u606f");
        }
        return notice;
    }

    @Override
    public int markMyNoticeRead(Long noticeId) {
        selectMyNoticeDetail(noticeId);
        return markUserNoticeMapper.markReadById(noticeId, SecurityUtils.getUserId(), SecurityUtils.getUsername());
    }

    @Override
    public int markAllMyNoticeRead() {
        return markUserNoticeMapper.markAllReadByUserId(SecurityUtils.getUserId(), SecurityUtils.getUsername());
    }

    @Override
    public void sendOrderAuditNotice(Long userId, Long orderId, String orderNo, String platformName,
                                     String auditStatus, String auditOpinion, String operator) {
        if (userId == null || orderId == null) {
            return;
        }
        String safeOrderNo = StringUtils.defaultIfBlank(orderNo, String.valueOf(orderId));
        String safePlatform = StringUtils.defaultIfBlank(platformName, "-");
        String noneText = "\u65e0";
        String title;
        String content;
        switch (StringUtils.defaultString(auditStatus)) {
            case "1" -> {
                title = "\u8ba2\u5355\u5ba1\u6838\u901a\u8fc7";
                content = String.format("\u8ba2\u5355 %s\uff08%s\uff09\u5df2\u5ba1\u6838\u901a\u8fc7\uff0c\u4ee3\u7406\u5c06\u5f00\u59cb\u5904\u7406\u3002",
                        safeOrderNo, safePlatform);
            }
            case "2" -> {
                title = "\u8ba2\u5355\u5ba1\u6838\u62d2\u7edd";
                content = String.format("\u8ba2\u5355 %s\uff08%s\uff09\u5df2\u88ab\u62d2\u7edd\u3002\u610f\u89c1\uff1a%s",
                        safeOrderNo, safePlatform, StringUtils.defaultIfBlank(auditOpinion, noneText));
            }
            case "3" -> {
                title = "\u8ba2\u5355\u5ba1\u6838\u6253\u56de";
                content = String.format("\u8ba2\u5355 %s\uff08%s\uff09\u5df2\u88ab\u6253\u56de\uff0c\u8bf7\u4fee\u6539\u540e\u91cd\u65b0\u63d0\u4ea4\u3002\u610f\u89c1\uff1a%s",
                        safeOrderNo, safePlatform, StringUtils.defaultIfBlank(auditOpinion, noneText));
            }
            default -> {
                return;
            }
        }
        insertNotice(userId, title, content, orderId, operator);
    }

    @Override
    public void sendOrderSubmitNotice(Long userId, Long orderId, String orderNo, String platformName, String operator) {
        if (userId == null || orderId == null) {
            return;
        }
        String safeOrderNo = StringUtils.defaultIfBlank(orderNo, String.valueOf(orderId));
        String safePlatform = StringUtils.defaultIfBlank(platformName, "-");
        String title = "\u8ba2\u5355\u63d0\u4ea4\u6210\u529f";
        String content = String.format("\u8ba2\u5355 %s\uff08%s\uff09\u5df2\u63d0\u4ea4\uff0c\u5df2\u540c\u6b65\u81f3\u4ee3\u7406\u5904\u7406\u5217\u8868\u3002",
                safeOrderNo, safePlatform);
        insertNotice(userId, title, content, orderId, operator);
    }

    @Override
    public void sendTdGaopinProcessNotice(Long userId, Long orderId, String orderNo, String phone,
                                          String detail, String operator) {
        if (userId == null || orderId == null) {
            return;
        }
        String safeOrderNo = StringUtils.defaultIfBlank(orderNo, String.valueOf(orderId));
        String safePhone = StringUtils.defaultIfBlank(phone, "-");
        String safeDetail = StringUtils.defaultIfBlank(detail, "\u6709\u6807\u8bb0");
        String title = "\u6cf0\u8fea\u9ad8\u9891\u5904\u7406\u5b8c\u6210";
        String content = String.format("\u53f7\u7801 %s\uff08\u8ba2\u5355 %s\uff09\u5df2\u81ea\u52a8\u68c0\u6d4b\u5b8c\u6210\uff0c\u5f53\u524d\u72b6\u6001\uff1a%s\u3002",
                safePhone, safeOrderNo, safeDetail);
        insertNotice(userId, title, content, orderId, operator);
    }

    @Override
    public void sendTencentProcessNotice(Long userId, Long orderId, String orderNo, String phone,
                                         boolean accepted, String failMessage, String operator) {
        if (userId == null || orderId == null) {
            return;
        }
        String safeOrderNo = StringUtils.defaultIfBlank(orderNo, String.valueOf(orderId));
        String safePhone = StringUtils.defaultIfBlank(phone, "-");
        String title = accepted ? "\u817e\u8baf\u63d0\u4ea4\u6210\u529f" : "\u817e\u8baf\u63d0\u4ea4\u5931\u8d25";
        String content = accepted
                ? String.format("\u53f7\u7801 %s\uff08\u8ba2\u5355 %s\uff09\u5df2\u5904\u7406\u6210\u529f\u3002", safePhone, safeOrderNo)
                : String.format("\u53f7\u7801 %s\uff08\u8ba2\u5355 %s\uff09\u5904\u7406\u5931\u8d25\uff1a%s",
                safePhone, safeOrderNo, StringUtils.defaultIfBlank(failMessage, "\u63d0\u4ea4\u5931\u8d25\uff0c\u9a8c\u8bc1\u7801\u9519\u8bef\u6216\u8005\u5931\u6548"));
        insertNotice(userId, title, content, orderId, operator);
    }

    @Override
    public MarkAgentAuditStatsVO selectAgentAuditStats() {
        Long currentUserId = SecurityUtils.getUserId();
        String currentUsername = SecurityUtils.getUsername();
        boolean isAdmin = SecurityUtils.isAdmin() || SecurityUtils.hasRole("admin");
        if (!isAdmin && !SecurityUtils.hasRole("agent")) {
            throw new ServiceException("\u4ec5\u4ee3\u7406\u6216\u7ba1\u7406\u5458\u53ef\u64cd\u4f5c");
        }
        MarkAgentAuditStatsVO stats = markUserNoticeMapper.selectAgentAuditStats(currentUserId, currentUsername, isAdmin);
        if (stats == null) {
            stats = new MarkAgentAuditStatsVO();
        }
        long totalAudit = stats.getTotalAuditCount() == null ? 0L : stats.getTotalAuditCount();
        long totalPass = stats.getTotalPassCount() == null ? 0L : stats.getTotalPassCount();
        stats.setPassRate(totalAudit <= 0 ? 0D : Math.round(totalPass * 10000D / totalAudit) / 100D);
        return stats;
    }

    private void insertNotice(Long userId, String title, String content, Long orderId, String operator) {
        Date now = DateUtils.getNowDate();
        MarkUserNotice notice = new MarkUserNotice();
        notice.setUserId(userId);
        notice.setNoticeType(NOTICE_TYPE_ORDER_AUDIT);
        notice.setTitle(StringUtils.abbreviate(title, 128));
        notice.setContent(StringUtils.abbreviate(content, 500));
        notice.setBizType(BIZ_TYPE_MARK_ORDER);
        notice.setBizId(orderId);
        notice.setReadFlag("0");
        notice.setCreateBy(operator);
        notice.setCreateTime(now);
        notice.setUpdateBy(operator);
        notice.setUpdateTime(now);
        markUserNoticeMapper.insertMarkUserNotice(notice);
    }
}
