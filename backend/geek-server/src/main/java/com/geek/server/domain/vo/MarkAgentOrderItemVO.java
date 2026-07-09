package com.geek.server.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.geek.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * Agent processing item list view.
 */
@Schema(description = "Agent processing item list view")
@Data
@EqualsAndHashCode(callSuper = true)
public class MarkAgentOrderItemVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(title = "Item ID")
    private Long id;

    @Schema(title = "Order ID")
    private Long orderId;

    @Schema(title = "Batch number")
    private String orderNo;

    @Schema(title = "Platform code")
    private String platformCode;

    @Schema(title = "Platform name")
    private String platformName;

    @Schema(title = "Phone number")
    private String phone;

    @Schema(title = "Process status: 0 pending, 1 success, 2 failed")
    private String processStatus;

    @Schema(title = "Verify code / process result")
    private String processResult;

    @Schema(title = "Process note")
    private String processNote;

    @Schema(title = "Processed by")
    private String processedBy;

    @Schema(title = "Processed time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date processedTime;

    @Schema(title = "Submit user name")
    private String userName;

    @Schema(title = "Order remark")
    private String orderRemark;

    @Schema(title = "Platform codes filter, comma separated")
    private String platformCodes;
}
