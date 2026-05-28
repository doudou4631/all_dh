package com.geek.server.mapper;

import com.geek.server.domain.MarkArbitrationCase;

import java.util.List;

/**
 * 仲裁工单 Mapper
 */
public interface MarkArbitrationCaseMapper {

    MarkArbitrationCase selectMarkArbitrationCaseById(Long id);

    List<MarkArbitrationCase> selectMarkArbitrationCaseList(MarkArbitrationCase markArbitrationCase);

    int insertMarkArbitrationCase(MarkArbitrationCase markArbitrationCase);

    int updateMarkArbitrationCase(MarkArbitrationCase markArbitrationCase);
}
