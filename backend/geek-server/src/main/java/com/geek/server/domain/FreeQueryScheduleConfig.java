package com.geek.server.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "免费查询定时任务配置")
public class FreeQueryScheduleConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称不能超过100个字符")
    private String taskName;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    @Size(max = 100, message = "Cron表达式不能超过100个字符")
    private String cronExpression;

    @Size(max = 500, message = "数据源地址不能超过500个字符")
    private String dataSource;

    private Integer retentionDays;

    private String status;
}
