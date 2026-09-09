package com.geek.server.service.impl;

import com.geek.common.utils.DateUtils;
import com.geek.server.domain.FreeQueryScheduleConfig;
import com.geek.server.mapper.FreeQueryScheduleConfigMapper;
import com.geek.server.service.IFreeQueryScheduleConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class FreeQueryScheduleConfigServiceImpl implements IFreeQueryScheduleConfigService {
    private static final String QUERY_WINDOW = "QUERY_WINDOW";
    private static final String DATA_IMPORT = "DATA_IMPORT";
    private static final String DATA_CLEANUP = "DATA_CLEANUP";
    private static final Set<String> TASK_TYPES = Set.of(QUERY_WINDOW, DATA_IMPORT, DATA_CLEANUP);

    @Autowired
    private FreeQueryScheduleConfigMapper mapper;

    @Override
    public FreeQueryScheduleConfig selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<FreeQueryScheduleConfig> selectList(FreeQueryScheduleConfig config) {
        return mapper.selectList(config);
    }

    @Override
    public List<FreeQueryScheduleConfig> selectEnabledList() {
        return mapper.selectEnabledList();
    }

    @Override
    public String validate(FreeQueryScheduleConfig config) {
        if (!TASK_TYPES.contains(config.getTaskType())) {
            return "不支持的任务类型";
        }
        if (mapper.countByTaskName(config.getTaskName(), config.getId()) > 0) {
            return "任务名称已存在";
        }
        if (QUERY_WINDOW.equals(config.getTaskType())) {
            if (config.getStartTime() == null || config.getEndTime() == null) {
                return "查询开放任务必须设置开始和停止时间";
            }
            config.setCronExpression(null);
            config.setDataSource(null);
            config.setRetentionDays(null);
        } else {
            if (StringUtils.isBlank(config.getCronExpression())
                    || !CronExpression.isValidExpression(config.getCronExpression())) {
                return "请输入有效的6位Cron表达式";
            }
            config.setStartTime(null);
            config.setEndTime(null);
            if (DATA_IMPORT.equals(config.getTaskType())) {
                if (StringUtils.isBlank(config.getDataSource())) {
                    return "定时导入任务必须填写数据源地址";
                }
                config.setRetentionDays(null);
            } else {
                if (config.getRetentionDays() == null || config.getRetentionDays() < 1) {
                    return "数据保留天数必须大于0";
                }
                config.setDataSource(null);
            }
        }
        if (!StringUtils.equalsAny(config.getStatus(), "0", "1")) {
            config.setStatus("0");
        }
        return null;
    }

    @Override
    public int insert(FreeQueryScheduleConfig config) {
        config.setCreateTime(DateUtils.getNowDate());
        return mapper.insert(config);
    }

    @Override
    public int update(FreeQueryScheduleConfig config) {
        config.setUpdateTime(DateUtils.getNowDate());
        return mapper.update(config);
    }

    @Override
    public int changeStatus(FreeQueryScheduleConfig config) {
        FreeQueryScheduleConfig update = new FreeQueryScheduleConfig();
        update.setId(config.getId());
        update.setStatus(config.getStatus());
        update.setUpdateBy(config.getUpdateBy());
        update.setUpdateTime(DateUtils.getNowDate());
        return mapper.changeStatus(update);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return mapper.deleteByIds(ids);
    }
}
