package com.geek.server.domain;

import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 手机端页面配置对象 mobile_page_config
 */
@Schema(description = "手机端页面配置对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class MobilePageConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "页面编码")
    private String pageCode;

    @Schema(title = "页面名称")
    private String pageName;

    @Schema(title = "客服电话")
    private String servicePhone;

    @Schema(title = "客服二维码地址")
    private String wechatQrUrl;

    @Schema(title = "底部-首页链接")
    private String navHomeUrl;

    @Schema(title = "底部-免费查询链接")
    private String navQueryUrl;

    @Schema(title = "底部-批量查询链接")
    private String navBatchUrl;

    @Schema(title = "底部-个人中心链接")
    private String navProfileUrl;

    @Schema(title = "结果页返回链接")
    private String resultBackUrl;

    @Schema(title = "状态（0正常 1停用）")
    private String status;

    @Schema(title = "排序")
    private Integer sort;

    @Schema(title = "删除标记（0存在 2删除）")
    private String delFlag;
}
