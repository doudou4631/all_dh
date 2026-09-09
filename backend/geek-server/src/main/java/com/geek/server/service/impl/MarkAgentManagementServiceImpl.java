package com.geek.server.service.impl;

import com.geek.common.core.domain.entity.SysMenu;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.common.exception.ServiceException;
import com.geek.common.utils.DateUtils;
import com.geek.common.utils.SecurityUtils;
import com.geek.server.domain.MarkAgentPointTransaction;
import com.geek.server.domain.MarkPlatformMapping;
import com.geek.server.domain.MarkUserPlatformQuota;
import com.geek.server.domain.MarkWalletLog;
import com.geek.server.domain.dto.*;
import com.geek.server.domain.vo.MarkAgentManagementVO;
import com.geek.server.mapper.MarkAgentManagementMapper;
import com.geek.server.mapper.MarkUserPlatformQuotaMapper;
import com.geek.server.mapper.MarkWalletLogMapper;
import com.geek.server.service.IMarkAgentManagementService;
import com.geek.server.service.IMarkPlatformMappingService;
import com.geek.system.service.ISysUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MarkAgentManagementServiceImpl implements IMarkAgentManagementService {
    private static final long AGENT_MENU_ID = 900100000109L;
    @Autowired private MarkAgentManagementMapper mapper;
    @Autowired private MarkUserPlatformQuotaMapper quotaMapper;
    @Autowired private MarkWalletLogMapper walletLogMapper;
    @Autowired private ISysUserService userService;
    @Autowired private IMarkPlatformMappingService platformMappingService;

    @Override
    public List<MarkAgentManagementVO> selectList(Integer agentLevel, String keyword, String status) {
        int level = requireLevel(agentLevel);
        SysUser current = currentUser();
        Long parentId = null;
        if (!SecurityUtils.isAdmin()) {
            int currentLevel = requireAgentLevel(current);
            if (currentLevel >= 3 || level != currentLevel + 1) throw new ServiceException("只能查看直属下级账号");
            parentId = current.getUserId();
        }
        return mapper.selectAgentList(level, parentId, StringUtils.trimToNull(keyword), StringUtils.trimToNull(status));
    }

    @Override
    public Map<String, Object> capabilities() {
        SysUser user = currentUser();
        boolean admin = SecurityUtils.isAdmin();
        int level = admin ? 0 : requireAgentLevel(user);
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("agentLevel", level);
        result.put("isAdmin", admin);
        result.put("canCreate", admin || level <= 2);
        // Level 3 is a UI view type for ordinary accounts, not an agent level.
        result.put("createLevel", admin ? 1 : (level <= 2 ? level + 1 : null));
        result.put("canAllocate", admin || level <= 2);
        result.put("operationType", admin ? "RECHARGE" : (level <= 2 ? "TRANSFER" : "NONE"));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAgent(MarkAgentCreateRequest request) {
        SysUser current = currentUser();
        boolean admin = SecurityUtils.isAdmin();
        int currentLevel = admin ? 0 : requireAgentLevel(current);
        int targetViewLevel = admin ? 1 : currentLevel + 1;
        if (targetViewLevel > 3) throw new ServiceException("普通账户不能创建下级账号");
        if (request.getAgentLevel() != null && request.getAgentLevel() != targetViewLevel) throw new ServiceException("不能跨级创建账号");
        boolean ordinaryUser = currentLevel == 2;
        SysUser user = new SysUser();
        user.setUserName(StringUtils.trim(request.getUserName()));
        user.setNickName(StringUtils.trim(request.getNickName()));
        user.setPhonenumber(StringUtils.trimToNull(request.getPhonenumber()));
        user.setPassword(SecurityUtils.encryptPassword(request.getPassword()));
        user.setStatus("0"); user.setDelFlag(0); user.setAgentLevel(ordinaryUser ? null : targetViewLevel);
        user.setParentUserId(admin ? null : current.getUserId()); user.setCreateBy(SecurityUtils.getUsername());
        user.setCreateTime(DateUtils.getNowDate());
        user.setRoleIds(List.of(ordinaryUser ? markUserRoleId() : markAgentRoleId(targetViewLevel)));
        userService.checkUserAllowedBeforeUpdate(user);
        if (!userService.insertUser(user)) throw new ServiceException("账号创建失败");
        return user.getUserId();
    }

    @Override public int updateStatus(MarkAgentAccountStatusRequest request) {
        SysUser target = requireManageableTarget(request.getUserId());
        if (!"0".equals(request.getStatus()) && !"1".equals(request.getStatus())) throw new ServiceException("账号状态无效");
        target.setStatus(request.getStatus()); target.setUpdateBy(SecurityUtils.getUsername()); target.setUpdateTime(DateUtils.getNowDate());
        return userService.updateUserStatus(target) ? 1 : 0;
    }

    @Override public int resetPassword(MarkAgentResetPasswordRequest request) {
        SysUser target = requireManageableTarget(request.getUserId());
        return userService.resetUserPwd(target.getUserName(), SecurityUtils.encryptPassword(request.getPassword())) ? 1 : 0;
    }

    @Override
    public Map<String,Object> platformOptions(Long targetUserId) {
        SysUser target = requireManageableTarget(targetUserId);
        Map<String,MarkUserPlatformQuota> targetQuotas = quotaMap(target.getUserId());
        Map<String,MarkUserPlatformQuota> operatorQuotas = quotaMap(SecurityUtils.getUserId());
        LinkedHashMap<String,Map<String,Object>> rows = new LinkedHashMap<>();
        for (MarkPlatformMapping mapping : platformMappingService.selectList(new MarkPlatformMapping())) {
            if (!"0".equals(mapping.getSourceEnabled()) || !"0".equals(mapping.getMappingEnabled()) || !"0".equals(mapping.getTargetEnabled())) continue;
            String code = mapping.getSourcePlatformCode().toLowerCase();
            rows.computeIfAbsent(code, key -> {
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("platformCode", code); row.put("platformName", mapping.getSourcePlatformName());
                row.put("targetBalance", balance(targetQuotas.get(code)));
                row.put("operatorBalance", SecurityUtils.isAdmin() ? null : balance(operatorQuotas.get(code)));
                return row;
            });
        }
        return Map.of("targetUserId", target.getUserId(), "platforms", new ArrayList<>(rows.values()));
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> adminRecharge(MarkAgentPointOperationRequest request) {
        if (!SecurityUtils.isAdmin()) throw new ServiceException("仅超级管理员可以执行平台充值");
        SysUser target = requireUser(request.getTargetUserId());
        if (!Integer.valueOf(1).equals(target.getAgentLevel())) throw new ServiceException("超级管理员只能给一级代理充值");
        validateOperation(request, target);
        Date now=DateUtils.getNowDate(); String operator=SecurityUtils.getUsername();
        MarkUserPlatformQuota targetQuota=lockQuota(target.getUserId(),request.getPlatformCode(),platformName(request),operator,now);
        long before=balance(targetQuota), after=safeAdd(before,request.getAmount());
        updateQuota(targetQuota,after,operator,now);
        insertWallet(target.getUserId(),request,"ADMIN_RECHARGE",request.getAmount(),before,after,operator,now);
        insertTransaction(request,"ADMIN_RECHARGE",null,target.getUserId(),now);
        return result(before,after,request.getAmount());
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> agentTransfer(MarkAgentPointOperationRequest request) {
        if (SecurityUtils.isAdmin()) throw new ServiceException("超级管理员请使用平台充值");
        SysUser source=currentUser(); int level=requireAgentLevel(source);
        if (level > 2) throw new ServiceException("当前账号没有次数分配权限");
        SysUser target=requireUser(request.getTargetUserId());
        if (!isDirectTransferTarget(source, target)) throw new ServiceException("只能给直属下级账号充值次数");
        validateOperation(request,target);
        Date now=DateUtils.getNowDate(); String operator=SecurityUtils.getUsername(); String platformName=platformName(request);
        MarkUserPlatformQuota sourceQuota=lockQuota(source.getUserId(),request.getPlatformCode(),platformName,operator,now);
        long sourceBefore=balance(sourceQuota);
        if(sourceBefore<request.getAmount()) throw new ServiceException("当前平台剩余次数不足，无法充值");
        MarkUserPlatformQuota targetQuota=lockQuota(target.getUserId(),request.getPlatformCode(),platformName,operator,now);
        long targetBefore=balance(targetQuota), sourceAfter=sourceBefore-request.getAmount(), targetAfter=safeAdd(targetBefore,request.getAmount());
        updateQuota(sourceQuota,sourceAfter,operator,now); updateQuota(targetQuota,targetAfter,operator,now);
        insertWallet(source.getUserId(),request,"TRANSFER_OUT",-request.getAmount(),sourceBefore,sourceAfter,operator,now);
        insertWallet(target.getUserId(),request,"TRANSFER_IN",request.getAmount(),targetBefore,targetAfter,operator,now);
        insertTransaction(request,"AGENT_TRANSFER",source.getUserId(),target.getUserId(),now);
        Map<String,Object> result=result(sourceBefore,sourceAfter,-request.getAmount()); result.put("targetBalanceAfter",targetAfter); return result;
    }

    @Override public List<SysMenu> filterMenuTree(List<SysMenu> menus) {
        if (menus==null || SecurityUtils.isAdmin()) return menus;
        SysUser current=currentUser(); Integer visibleLevel=current.getAgentLevel()==null?null:current.getAgentLevel()+1;
        Iterator<SysMenu> it=menus.iterator();
        while(it.hasNext()){
            SysMenu menu=it.next();
            if(menu.getMenuId()!=null && menu.getMenuId().equals(AGENT_MENU_ID)){
                if(visibleLevel==null || visibleLevel>3){it.remove();continue;}
                menu.getChildren().removeIf(child -> !String.valueOf(visibleLevel).equals(levelFromPath(child.getPath())));
                if(menu.getChildren().isEmpty()) it.remove();
            } else if(menu.getChildren()!=null) filterMenuTree(menu.getChildren());
        }
        return menus;
    }

    private String levelFromPath(String path){ if(path==null)return ""; return path.replace("level",""); }
    private SysUser currentUser(){return requireUser(SecurityUtils.getUserId());}
    private SysUser requireUser(Long id){SysUser u=id==null?null:userService.selectUserById(id);if(u==null||Integer.valueOf(2).equals(u.getDelFlag()))throw new ServiceException("用户不存在");return u;}
    private int requireAgentLevel(SysUser u){Integer l=u.getAgentLevel();if(l==null||l<1||l>2)throw new ServiceException("当前账号不是代理账号");return l;}
    private int requireLevel(Integer l){if(l==null||l<1||l>3)throw new ServiceException("代理等级无效");return l;}
    private SysUser requireManageableTarget(Long id){SysUser t=requireUser(id);if(SecurityUtils.isAdmin())return t;SysUser c=currentUser();if(!isDirectTransferTarget(c,t))throw new ServiceException("只能操作直属下级账号");return t;}
    private boolean isDirectTransferTarget(SysUser source,SysUser target){
        if(!Objects.equals(target.getParentUserId(),source.getUserId()))return false;
        int sourceLevel=requireAgentLevel(source);
        return sourceLevel==1?Integer.valueOf(2).equals(target.getAgentLevel()):sourceLevel==2&&target.getAgentLevel()==null;
    }
    private Long markAgentRoleId(int level){
        return requireRoleId(level == 2 ? "mark_sub_agent" : "mark_agent",
                level == 2 ? "缺少下级代理角色" : "缺少标记代理角色");
    }
    private Long markUserRoleId(){return requireRoleId("mark_user","缺少普通用户角色");}
    private Long requireRoleId(String roleKey,String message){Long roleId=mapper.selectRoleIdByRoleKey(roleKey);if(roleId==null)throw new ServiceException(message);return roleId;}
    private void validateOperation(MarkAgentPointOperationRequest r,SysUser target){if(r==null||StringUtils.isBlank(r.getRequestNo())||r.getAmount()==null||r.getAmount()<=0)throw new ServiceException("请求号和充值次数不能为空");if(!"0".equals(target.getStatus()))throw new ServiceException("目标账号已停用");if(mapper.selectTransactionByRequestNo(r.getRequestNo())!=null)throw new ServiceException("该请求已处理，请勿重复提交");if(StringUtils.isBlank(r.getPlatformCode())||!platformMappingService.isSourceEnabled(r.getPlatformCode()))throw new ServiceException("平台未启用");}
    private String platformName(MarkAgentPointOperationRequest r){String name=StringUtils.trimToNull(r.getPlatformName());if(name!=null)return name;return platformMappingService.selectList(new MarkPlatformMapping()).stream().filter(x->StringUtils.equalsIgnoreCase(x.getSourcePlatformCode(),r.getPlatformCode())).map(MarkPlatformMapping::getSourcePlatformName).filter(StringUtils::isNotBlank).findFirst().orElse(r.getPlatformCode());}
    private Map<String,MarkUserPlatformQuota> quotaMap(Long userId){Map<String,MarkUserPlatformQuota> map=new HashMap<>();for(MarkUserPlatformQuota q:quotaMapper.selectByUserId(userId))map.put(q.getPlatformCode().toLowerCase(),q);return map;}
    private long balance(MarkUserPlatformQuota q){return q==null||q.getRemainCount()==null?0:Math.max(0,q.getRemainCount());}
    private MarkUserPlatformQuota lockQuota(Long userId,String code,String name,String operator,Date now){MarkUserPlatformQuota q=quotaMapper.selectByUserAndPlatformForUpdate(userId,code);if(q!=null)return q;q=new MarkUserPlatformQuota();q.setUserId(userId);q.setPlatformCode(code.toLowerCase());q.setPlatformName(name);q.setRemainCount(0L);q.setCreateBy(operator);q.setCreateTime(now);quotaMapper.insertMarkUserPlatformQuota(q);return quotaMapper.selectByUserAndPlatformForUpdate(userId,code);}
    private void updateQuota(MarkUserPlatformQuota q,long amount,String operator,Date now){q.setRemainCount(amount);q.setUpdateBy(operator);q.setUpdateTime(now);quotaMapper.updateMarkUserPlatformQuota(q);}
    private long safeAdd(long a,long b){try{return Math.addExact(a,b);}catch(ArithmeticException e){throw new ServiceException("次数超出范围");}}
    private void insertWallet(Long userId,MarkAgentPointOperationRequest r,String type,long change,long before,long after,String operator,Date now){MarkWalletLog log=new MarkWalletLog();log.setUserId(userId);log.setPlatformCode(r.getPlatformCode().toLowerCase());log.setPlatformName(platformName(r));log.setBizType(type);log.setChangeAmount(change);log.setBalanceBefore(before);log.setBalanceAfter(after);log.setRemark(StringUtils.defaultIfBlank(r.getReason(),type));log.setCreateBy(operator);log.setCreateTime(now);walletLogMapper.insertMarkWalletLog(log);}
    private void insertTransaction(MarkAgentPointOperationRequest r,String type,Long from,Long to,Date now){MarkAgentPointTransaction t=new MarkAgentPointTransaction();t.setTransactionNo(UUID.randomUUID().toString().replace("-",""));t.setRequestNo(r.getRequestNo());t.setTransactionType(type);t.setPlatformCode(r.getPlatformCode().toLowerCase());t.setPlatformName(platformName(r));t.setAmount(r.getAmount());t.setFromUserId(from);t.setToUserId(to);t.setOperatorUserId(SecurityUtils.getUserId());t.setReason(r.getReason());t.setStatus("SUCCESS");t.setCreateTime(now);mapper.insertTransaction(t);}
    private Map<String,Object> result(long before,long after,long change){Map<String,Object> m=new LinkedHashMap<>();m.put("balanceBefore",before);m.put("balanceAfter",after);m.put("changeAmount",change);return m;}
}
