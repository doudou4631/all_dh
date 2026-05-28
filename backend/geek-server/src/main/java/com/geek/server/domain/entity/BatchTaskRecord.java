package com.geek.server.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

/**
 * 批量任务记录实体类 - 记录任务全生命周期
 */
@Schema(description = "批量任务记录实体类")
@Table("batch_task_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BatchTaskRecord extends BaseEntity {

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    @Schema(title = "主键ID")
    @Excel(name = "主键ID")
    private Long id;

    /**
     * 任务ID
     */
    @Schema(title = "任务ID")
    @Excel(name = "任务ID")
    private String taskId;

    /**
     * 用户ID
     */
    @Schema(title = "用户ID")
    @Excel(name = "用户ID")
    private Long userId;

    /**
     * 任务名称
     */
    @Schema(title = "任务名称")
    @Excel(name = "任务名称")
    private String taskName;

    /**
     * 任务状态：RUNNING, COMPLETED, FAILED, CANCELLED
     */
    @Schema(title = "任务状态")
    @Excel(name = "任务状态")
    private String status;

    /**
     * 总查询数量
     */
    @Schema(title = "总查询数量")
    @Excel(name = "总查询数量")
    private Integer totalCount;

    /**
     * 已完成数量
     */
    @Schema(title = "已完成数量")
    @Excel(name = "已完成数量")
    private Integer completedCount;

    /**
     * 成功数量
     */
    @Schema(title = "成功数量")
    @Excel(name = "成功数量")
    private Integer successCount;

    /**
     * 失败数量
     */
    @Schema(title = "失败数量")
    @Excel(name = "失败数量")
    private Integer failedCount;

    /**
     * 进度百分比
     */
    @Schema(title = "进度百分比")
    @Excel(name = "进度百分比")
    private Integer percentage;

    /**
     * 查询号码列表（JSON格式）
     */
    @Schema(title = "查询号码列表")
    @Excel(name = "查询号码列表")
    private String phoneNumbers;

    /**
     * 平台配置列表（JSON格式）
     */
    @Schema(title = "平台配置列表")
    @Excel(name = "平台配置列表")
    private String platformConfigs;

    /**
     * 任务描述
     */
    @Schema(title = "任务描述")
    @Excel(name = "任务描述")
    private String description;

    /**
     * 错误信息
     */
    @Schema(title = "错误信息")
    @Excel(name = "错误信息")
    private String errorMessage;

    /**
     * 开始时间
     */
    @Schema(title = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 结束时间
     */
    @Schema(title = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /**
     * 删除标志（0：正常，1：删除）
     */
    @Schema(title = "删除标志")
    @Excel(name = "删除标志")
    private String delFlag;
}
