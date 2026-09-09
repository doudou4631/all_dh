package com.geek.server.domain.vo;

import com.geek.server.domain.MarkOrder;
import com.geek.server.domain.MarkOrderItem;
import lombok.Data;

import java.util.List;

/**
 * 订单详情
 */
@Data
public class MarkOrderDetailVO {

    private MarkOrder order;

    private List<MarkOrderItem> items;
}
