package com.geek.server.service;
import com.geek.common.core.domain.entity.SysMenu;
import com.geek.server.domain.dto.*;
import com.geek.server.domain.vo.MarkAgentManagementVO;
import java.util.List;
import java.util.Map;

public interface IMarkAgentManagementService {
    List<MarkAgentManagementVO> selectList(Integer agentLevel,String keyword,String status);
    Map<String,Object> capabilities();
    Long createAgent(MarkAgentCreateRequest request);
    int updateStatus(MarkAgentAccountStatusRequest request);
    int resetPassword(MarkAgentResetPasswordRequest request);
    Map<String,Object> platformOptions(Long targetUserId);
    Map<String,Object> adminRecharge(MarkAgentPointOperationRequest request);
    Map<String,Object> agentTransfer(MarkAgentPointOperationRequest request);
    List<SysMenu> filterMenuTree(List<SysMenu> menus);
}
