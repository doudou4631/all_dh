package com.geek.server.service.impl;

import com.geek.common.utils.DateUtils;
import com.geek.common.utils.SecurityUtils;
import com.geek.server.domain.MarkArbitrationCase;
import com.geek.server.domain.MarkGovernRule;
import com.geek.server.mapper.MarkArbitrationCaseMapper;
import com.geek.server.mapper.MarkGovernRuleMapper;
import com.geek.server.service.IMarkGovernService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 治理/仲裁服务实现
 */
@Service
public class MarkGovernServiceImpl implements IMarkGovernService {

    @Autowired
    private MarkGovernRuleMapper markGovernRuleMapper;

    @Autowired
    private MarkArbitrationCaseMapper markArbitrationCaseMapper;

    @Override
    public MarkGovernRule selectMarkGovernRuleById(Long id) {
        return markGovernRuleMapper.selectMarkGovernRuleById(id);
    }

    @Override
    public List<MarkGovernRule> selectMarkGovernRuleList(MarkGovernRule query) {
        return markGovernRuleMapper.selectMarkGovernRuleList(query);
    }

    @Override
    public int insertMarkGovernRule(MarkGovernRule markGovernRule) {
        markGovernRule.setStatus(StringUtils.defaultIfBlank(markGovernRule.getStatus(), "0"));
        markGovernRule.setCreateBy(SecurityUtils.getUsername());
        markGovernRule.setCreateTime(DateUtils.getNowDate());
        return markGovernRuleMapper.insertMarkGovernRule(markGovernRule);
    }

    @Override
    public int updateMarkGovernRule(MarkGovernRule markGovernRule) {
        markGovernRule.setUpdateBy(SecurityUtils.getUsername());
        markGovernRule.setUpdateTime(DateUtils.getNowDate());
        return markGovernRuleMapper.updateMarkGovernRule(markGovernRule);
    }

    @Override
    public int deleteMarkGovernRuleByIds(Long[] ids) {
        return markGovernRuleMapper.deleteMarkGovernRuleByIds(ids);
    }

    @Override
    public MarkArbitrationCase selectMarkArbitrationCaseById(Long id) {
        return markArbitrationCaseMapper.selectMarkArbitrationCaseById(id);
    }

    @Override
    public List<MarkArbitrationCase> selectMarkArbitrationCaseList(MarkArbitrationCase query) {
        return markArbitrationCaseMapper.selectMarkArbitrationCaseList(query);
    }

    @Override
    public int insertMarkArbitrationCase(MarkArbitrationCase markArbitrationCase) {
        markArbitrationCase.setCaseStatus(StringUtils.defaultIfBlank(markArbitrationCase.getCaseStatus(), "0"));
        markArbitrationCase.setCreateBy(SecurityUtils.getUsername());
        markArbitrationCase.setCreateTime(DateUtils.getNowDate());
        return markArbitrationCaseMapper.insertMarkArbitrationCase(markArbitrationCase);
    }

    @Override
    public int updateMarkArbitrationCase(MarkArbitrationCase markArbitrationCase) {
        if ("1".equals(markArbitrationCase.getCaseStatus()) || "2".equals(markArbitrationCase.getCaseStatus())) {
            markArbitrationCase.setDecidedBy(SecurityUtils.getUsername());
            markArbitrationCase.setDecidedTime(DateUtils.getNowDate());
        }
        markArbitrationCase.setUpdateBy(SecurityUtils.getUsername());
        markArbitrationCase.setUpdateTime(DateUtils.getNowDate());
        return markArbitrationCaseMapper.updateMarkArbitrationCase(markArbitrationCase);
    }
}
