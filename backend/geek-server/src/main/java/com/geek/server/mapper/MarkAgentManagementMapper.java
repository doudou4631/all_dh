package com.geek.server.mapper;
import com.geek.server.domain.MarkAgentPointTransaction;
import com.geek.server.domain.vo.MarkAgentManagementVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface MarkAgentManagementMapper {
    List<MarkAgentManagementVO> selectAgentList(@Param("agentLevel") Integer agentLevel,
        @Param("parentUserId") Long parentUserId, @Param("keyword") String keyword, @Param("status") String status);
    MarkAgentPointTransaction selectTransactionByRequestNo(String requestNo);
    int insertTransaction(MarkAgentPointTransaction transaction);
    Long selectRoleIdByRoleKey(@Param("roleKey") String roleKey);
}
