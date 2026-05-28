package com.geek.server.mapper;

import com.geek.server.domain.MarkGovernRule;

import java.util.List;

/**
 * 治理规则 Mapper
 */
public interface MarkGovernRuleMapper {

    MarkGovernRule selectMarkGovernRuleById(Long id);

    List<MarkGovernRule> selectMarkGovernRuleList(MarkGovernRule markGovernRule);

    int insertMarkGovernRule(MarkGovernRule markGovernRule);

    int updateMarkGovernRule(MarkGovernRule markGovernRule);

    int deleteMarkGovernRuleByIds(Long[] ids);
}
