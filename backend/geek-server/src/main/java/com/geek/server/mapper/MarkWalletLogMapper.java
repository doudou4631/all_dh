package com.geek.server.mapper;

import com.geek.server.domain.MarkWalletLog;
import com.geek.server.domain.vo.MarkFundTotalSummaryVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 钱包流水 Mapper
 */
public interface MarkWalletLogMapper {

    List<MarkWalletLog> selectMarkWalletLogList(MarkWalletLog markWalletLog);
    List<MarkWalletLog> selectAgentWalletLogList(@Param("query") MarkWalletLog markWalletLog,
                                                 @Param("agentUsername") String agentUsername,
                                                 @Param("agentId") Long agentId,
                                                 @Param("isAdmin") boolean isAdmin);
    MarkFundTotalSummaryVO selectMarkWalletLogSummary(MarkWalletLog markWalletLog);
    List<MarkWalletLog> selectMarkWalletPlatformOptions();

    int insertMarkWalletLog(MarkWalletLog markWalletLog);
}
