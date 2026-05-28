package com.geek.server.mapper;

import com.geek.server.domain.MarkWalletLog;

import java.util.List;

/**
 * 钱包流水 Mapper
 */
public interface MarkWalletLogMapper {

    List<MarkWalletLog> selectMarkWalletLogList(MarkWalletLog markWalletLog);

    int insertMarkWalletLog(MarkWalletLog markWalletLog);
}
