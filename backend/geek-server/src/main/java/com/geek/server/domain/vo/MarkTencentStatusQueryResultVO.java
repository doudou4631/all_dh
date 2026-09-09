package com.geek.server.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 腾讯号码实时状态查询结果
 */
@Data
public class MarkTencentStatusQueryResultVO {

    private Integer totalCount;

    private Integer successCount;

    private Integer failedCount;

    private List<MarkTencentStatusItemVO> items;
}
