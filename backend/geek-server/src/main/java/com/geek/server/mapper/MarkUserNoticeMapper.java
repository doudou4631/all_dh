package com.geek.server.mapper;

import com.geek.server.domain.MarkUserNotice;
import com.geek.server.domain.vo.MarkAgentAuditStatsVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * User notice mapper.
 */
public interface MarkUserNoticeMapper {

    MarkUserNotice selectMarkUserNoticeById(Long id);

    List<MarkUserNotice> selectMarkUserNoticeList(MarkUserNotice notice);

    int countUnreadByUserId(@Param("userId") Long userId);

    int insertMarkUserNotice(MarkUserNotice notice);

    int markReadById(@Param("id") Long id, @Param("userId") Long userId, @Param("updateBy") String updateBy);

    int markAllReadByUserId(@Param("userId") Long userId, @Param("updateBy") String updateBy);

    MarkAgentAuditStatsVO selectAgentAuditStats(@Param("agentId") Long agentId,
                                                @Param("agentUsername") String agentUsername,
                                                @Param("isAdmin") boolean isAdmin);
}
