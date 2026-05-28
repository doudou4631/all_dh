package com.geek.server.service;

import com.geek.server.domain.vo.FreeSingleQueryRequest;

import com.geek.server.domain.UserApiQueryRecord;
import java.util.Map;
import java.util.List;

public interface IFreeQueryService {

    Map<String, Object> getQuota(String ip);

    Map<String, Object> singleQuery(FreeSingleQueryRequest request, String ip);

    List<UserApiQueryRecord> listIpLogs(String ip, String beginTime, String endTime);
}

