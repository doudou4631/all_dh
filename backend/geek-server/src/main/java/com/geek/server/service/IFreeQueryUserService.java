package com.geek.server.service;

import com.geek.server.domain.FreeQueryUser;
import com.geek.server.domain.vo.FreeQueryPointAdjustRequest;

import java.util.List;

public interface IFreeQueryUserService {

    FreeQueryUser selectFreeQueryUserById(Long id);

    List<FreeQueryUser> selectFreeQueryUserList(FreeQueryUser query);

    int insertFreeQueryUser(FreeQueryUser user);

    int updateFreeQueryUser(FreeQueryUser user);

    int deleteFreeQueryUserByIds(Long[] ids, String operator);

    FreeQueryUser authenticate(String loginAccount, String rawPassword, String loginIp);

    FreeQueryUser requireEnabledUser(Long userId);

    int adjustPoints(FreeQueryPointAdjustRequest request, Long operatorId, String operatorName);

    int resetPassword(Long userId, String rawPassword, String operatorName);

    void consumePointsForBatch(Long userId, Integer pointAmount, String bizNo, String operatorName);

    void refundPointsForBatch(Long userId, Integer pointAmount, String bizNo, String operatorName, String reason);
}
