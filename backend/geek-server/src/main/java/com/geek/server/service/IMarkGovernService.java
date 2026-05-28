package com.geek.server.service;

import com.geek.server.domain.MarkArbitrationCase;
import com.geek.server.domain.MarkGovernRule;

import java.util.List;

/**
 * 治理/仲裁服务
 */
public interface IMarkGovernService {

    MarkGovernRule selectMarkGovernRuleById(Long id);

    List<MarkGovernRule> selectMarkGovernRuleList(MarkGovernRule query);

    int insertMarkGovernRule(MarkGovernRule markGovernRule);

    int updateMarkGovernRule(MarkGovernRule markGovernRule);

    int deleteMarkGovernRuleByIds(Long[] ids);

    MarkArbitrationCase selectMarkArbitrationCaseById(Long id);

    List<MarkArbitrationCase> selectMarkArbitrationCaseList(MarkArbitrationCase query);

    int insertMarkArbitrationCase(MarkArbitrationCase markArbitrationCase);

    int updateMarkArbitrationCase(MarkArbitrationCase markArbitrationCase);
}
