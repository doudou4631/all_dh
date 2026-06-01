package com.geek.server.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户下单前预查询汇总
 */
@Data
public class MarkOrderPrecheckResultVO {

    private String platformCode;

    private String platformName;

    private Integer totalCount;

    private Integer markedCount;

    private Integer unmarkedCount;

    private Integer failedCount;

    private List<String> markedPhones;

    private List<String> unmarkedPhones;

    private List<String> failedPhones;

    private List<MarkPhoneCheckItemVO> items;
}
