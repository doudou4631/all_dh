package com.geek.server.mapper;

import com.geek.server.domain.MarkOrderItem;
import com.geek.server.domain.vo.MarkAgentDailyOverviewVO;
import com.geek.server.domain.vo.MarkAgentOrderItemVO;
import com.geek.server.domain.vo.MarkAgentProcessUserOptionVO;
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

    int updateMyOrderItemRemarks(@Param("itemIds") List<Long> itemIds,
                                 @Param("userId") Long userId,
                                 @Param("userRemark") String userRemark,
                                 @Param("updateBy") String updateBy);

    List<MarkAgentOrderItemVO> selectAgentOrderItemList(@Param("query") MarkAgentOrderItemVO query,
                                                          @Param("agentId") Long agentId,
                                                          @Param("agentUsername") String agentUsername,
                                                          @Param("isAdmin") boolean isAdmin);

    List<MarkAgentProcessUserOptionVO> selectAgentProcessUserOptions(@Param("agentId") Long agentId,
                                                                      @Param("agentUsername") String agentUsername,
                                                                      @Param("isAdmin") boolean isAdmin);

    int countAgentReadableUser(@Param("userId") Long userId,
                               @Param("agentId") Long agentId,
                               @Param("agentUsername") String agentUsername,
                               @Param("isAdmin") boolean isAdmin);

    int insertAgentHiddenUser(@Param("agentId") Long agentId, @Param("userId") Long userId,
                              @Param("createBy") String createBy);

    int deleteAgentHiddenUser(@Param("agentId") Long agentId, @Param("userId") Long userId);

    List<MarkAgentDailyOverviewVO> selectAgentDailyOverviewList(@Param("query") MarkAgentDailyOverviewVO query,
                                                                 @Param("agentId") Long agentId,
                                                                 @Param("agentUsername") String agentUsername,
                                                                 @Param("isAdmin") boolean isAdmin);

    List<MarkOrderItem> selectPendingTdGaopinProcessItems(@Param("limit") int limit);

    List<MarkOrderItem> selectPendingProcessItems(@Param("platformCode") String platformCode, @Param("limit") int limit);

    List<MarkOrderItem> selectPendingTdxSecondItems(@Param("checkIntervalMinutes") int checkIntervalMinutes,
                                                     @Param("limit") int limit);

    List<String> selectUserTdxSecondProcessNotes(@Param("userId") Long userId, @Param("phone") String phone);

    List<String> selectUserPendingTdGaopinPhones(@Param("userId") Long userId, @Param("phones") List<String> phones);
}
