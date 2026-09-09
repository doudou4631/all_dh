package com.geek.server.mapper;

import com.geek.server.domain.FreeQueryRecord;
import com.geek.server.domain.FreeQueryAuditQuery;
import com.geek.server.domain.FreeQueryAuditRecord;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface FreeQueryStatisticsMapper {
    int insertRecord(FreeQueryRecord record);

    int deleteExpiredRecords(@Param("beforeTime") Date beforeTime);

    Map<String, Object> selectSummary();

    List<Map<String, Object>> selectSevenDayTrend();

    List<FreeQueryAuditRecord> selectAuditList(FreeQueryAuditQuery query);

    String selectResultDetailsById(Long id);
}
