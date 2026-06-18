package com.geek.server.mapper;

import com.geek.server.domain.MarkWalletLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 钱包流水 Mapper
 */
public interface MarkWalletLogMapper {

    List<MarkWalletLog> selectMarkWalletLogList(MarkWalletLog markWalletLog);
    List<MarkWalletLog> selectAgentWalletLogList(@Param("query") MarkWalletLog markWalletLog,
                                                  @Param("agentUsername") String agentUsername,
                                                  @Param("isAdmin") boolean isAdmin);

    int insertMarkWalletLog(MarkWalletLog markWalletLog);
}
