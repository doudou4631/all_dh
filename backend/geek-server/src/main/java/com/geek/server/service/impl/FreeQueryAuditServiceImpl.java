package com.geek.server.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.server.domain.FreeQueryAuditQuery;
import com.geek.server.domain.FreeQueryAuditRecord;
import com.geek.server.mapper.FreeQueryStatisticsMapper;
import com.geek.server.service.IFreeQueryAuditService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FreeQueryAuditServiceImpl implements IFreeQueryAuditService {
    @Autowired
    private FreeQueryStatisticsMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<FreeQueryAuditRecord> selectList(FreeQueryAuditQuery query) {
        return mapper.selectAuditList(query);
    }

    @Override
    public List<Map<String, String>> selectDetails(Long id) {
        String details = mapper.selectResultDetailsById(id);
        if (StringUtils.isBlank(details)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(details, new TypeReference<List<Map<String, String>>>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
