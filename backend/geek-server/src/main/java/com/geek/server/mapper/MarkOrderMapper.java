package com.geek.server.mapper;

import com.geek.server.domain.MarkOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 迁移订单 Mapper
 */
public interface MarkOrderMapper {

    MarkOrder selectMarkOrderById(Long id);

    List<MarkOrder> selectMarkOrderList(MarkOrder markOrder);

    List<MarkOrder> selectAgentOrderList(@Param("query") MarkOrder markOrder,
                                         @Param("agentId") Long agentId,
                                         @Param("agentUsername") String agentUsername,
                                         @Param("isAdmin") boolean isAdmin);

    Long selectOrderIdByUserAndRequestNo(@Param("userId") Long userId, @Param("requestNo") String requestNo);

    int insertMarkOrder(MarkOrder markOrder);

    int updateMarkOrder(MarkOrder markOrder);
}
