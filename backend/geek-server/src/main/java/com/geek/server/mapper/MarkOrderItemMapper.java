package com.geek.server.mapper;

import com.geek.server.domain.MarkOrderItem;
import com.geek.server.domain.vo.MarkAgentOrderItemVO;
import org.apache.ibatis.annotations.Param;

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

    List<MarkAgentOrderItemVO> selectAgentOrderItemList(@Param("query") MarkAgentOrderItemVO query,
                                                          @Param("agentId") Long agentId,
                                                          @Param("agentUsername") String agentUsername,
                                                          @Param("isAdmin") boolean isAdmin);

    List<MarkOrderItem> selectPendingTdGaopinProcessItems(@Param("limit") int limit);

    List<MarkOrderItem> selectPendingXiaomiFirstDetectItems(@Param("limit") int limit);

    List<MarkOrderItem> selectPendingXiaomiRecheckItems(@Param("limit") int limit);

    List<String> selectUserPendingTdGaopinPhones(@Param("userId") Long userId, @Param("phones") List<String> phones);
}
