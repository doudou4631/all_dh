package com.geek.server.domain;

import com.geek.common.annotation.Excel;
import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User notice mark_user_notice
 */
@Schema(description = "User notice")
@Data
@EqualsAndHashCode(callSuper = true)
public class MarkUserNotice extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(title = "ID")
    private Long id;

    @Schema(title = "User ID")
    private Long userId;

    @Schema(title = "Notice type")
    private String noticeType;

    @Schema(title = "Title")
    private String title;

    @Schema(title = "Content")
    private String content;

    @Schema(title = "Biz type")
    private String bizType;

    @Schema(title = "Biz ID")
    private Long bizId;

    @Schema(title = "Read flag 0 unread 1 read")
    private String readFlag;
}
