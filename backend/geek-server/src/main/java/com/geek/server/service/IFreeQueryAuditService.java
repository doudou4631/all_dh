package com.geek.server.service;

import com.geek.server.domain.FreeQueryAuditQuery;
import com.geek.server.domain.FreeQueryAuditRecord;

import java.util.List;
import java.util.Map;

public interface IFreeQueryAuditService {
    List<FreeQueryAuditRecord> selectList(FreeQueryAuditQuery query);

    List<Map<String, String>> selectDetails(Long id);
}
