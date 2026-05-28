package com.geek.server.mapper;

import com.geek.server.domain.MarkOrderItem;

import java.util.List;

/**
 * 迁移订单明细 Mapper
 */
public interface MarkOrderItemMapper {

    MarkOrderItem selectMarkOrderItemById(Long id);

    List<MarkOrderItem> selectMarkOrderItemList(MarkOrderItem markOrderItem);

    List<MarkOrderItem> selectMarkOrderItemsByOrderId(Long orderId);

    int insertMarkOrderItem(MarkOrderItem markOrderItem);

    int updateMarkOrderItem(MarkOrderItem markOrderItem);
}
