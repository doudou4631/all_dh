package com.geek.server.service.impl;

import com.geek.common.exception.ServiceException;
import com.geek.common.utils.DateUtils;
import com.geek.common.utils.SecurityUtils;
import com.geek.common.utils.StringUtils;
import com.geek.server.domain.FreeQueryPointRecord;
import com.geek.server.domain.FreeQueryUser;
import com.geek.server.domain.vo.FreeQueryPointAdjustRequest;
import com.geek.server.mapper.FreeQueryPointRecordMapper;
import com.geek.server.mapper.FreeQueryUserMapper;
import com.geek.server.service.IFreeQueryUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FreeQueryUserServiceImpl implements IFreeQueryUserService {

    private static final String STATUS_NORMAL = "0";
    private static final String POINT_TYPE_INCREASE = "1";
    private static final String POINT_TYPE_DEDUCT = "2";
    private static final String BIZ_TYPE_MANUAL = "MANUAL_ADJUST";
    private static final String BIZ_TYPE_BATCH_DEDUCT = "BATCH_DEDUCT";
    private static final String BIZ_TYPE_BATCH_REFUND = "BATCH_REFUND";

    private final FreeQueryUserMapper freeQueryUserMapper;
    private final FreeQueryPointRecordMapper freeQueryPointRecordMapper;

    @Override
    public FreeQueryUser selectFreeQueryUserById(Long id) {
        return freeQueryUserMapper.selectFreeQueryUserById(id);
    }

    @Override
    public List<FreeQueryUser> selectFreeQueryUserList(FreeQueryUser query) {
        return freeQueryUserMapper.selectFreeQueryUserList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertFreeQueryUser(FreeQueryUser user) {
        if (user == null) {
            throw new ServiceException("用户参数不能为空");
        }
        String account = normalizeAccount(user.getAccount());
        if (StringUtils.isEmpty(account)) {
            throw new ServiceException("账号不能为空");
        }
        if (freeQueryUserMapper.selectFreeQueryUserByAccount(account) != null) {
            throw new ServiceException("账号已存在");
        }
        String phone = normalizePhone(user.getPhone());
        if (StringUtils.isNotEmpty(phone)) {
            FreeQueryUser samePhone = freeQueryUserMapper.selectFreeQueryUserByPhone(phone);
            if (samePhone != null) {
                throw new ServiceException("手机号已存在");
            }
        }
        String rawPassword = StringUtils.trimToEmpty(user.getPassword());
        if (StringUtils.isEmpty(rawPassword)) {
            throw new ServiceException("密码不能为空");
        }
        if (rawPassword.length() < 6) {
            throw new ServiceException("密码长度至少6位");
        }
        user.setAccount(account);
        user.setPhone(phone);
        user.setPassword(SecurityUtils.encryptPassword(rawPassword));
        user.setStatus(StringUtils.isEmpty(user.getStatus()) ? STATUS_NORMAL : user.getStatus());
        user.setPoints(normalizePoints(user.getPoints()));
        Date now = DateUtils.getNowDate();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        int rows;
        try {
            rows = freeQueryUserMapper.insertFreeQueryUser(user);
        } catch (DuplicateKeyException | DataIntegrityViolationException e) {
            throw convertPersistenceException(e);
        }
        if (rows > 0 && user.getPoints() != null && user.getPoints() > 0) {
            insertPointRecord(user.getId(), user.getPoints(), POINT_TYPE_INCREASE, BIZ_TYPE_MANUAL, null,
                    "后台初始化积分", null, user.getPoints(), user.getCreateBy(), null);
        }
        return rows;
    }

    @Override
    public int updateFreeQueryUser(FreeQueryUser user) {
        if (user == null || user.getId() == null) {
            throw new ServiceException("用户参数不能为空");
        }
        FreeQueryUser old = freeQueryUserMapper.selectFreeQueryUserById(user.getId());
        if (old == null) {
            throw new ServiceException("用户不存在");
        }

        String account = normalizeAccount(user.getAccount());
        if (StringUtils.isEmpty(account)) {
            throw new ServiceException("账号不能为空");
        }
        FreeQueryUser sameAccount = freeQueryUserMapper.selectFreeQueryUserByAccount(account);
        if (sameAccount != null && !sameAccount.getId().equals(user.getId())) {
            throw new ServiceException("账号已存在");
        }

        String phone = normalizePhone(user.getPhone());
        if (StringUtils.isNotEmpty(phone)) {
            FreeQueryUser samePhone = freeQueryUserMapper.selectFreeQueryUserByPhone(phone);
            if (samePhone != null && !samePhone.getId().equals(user.getId())) {
                throw new ServiceException("手机号已存在");
            }
        }

        user.setAccount(account);
        user.setPhone(phone);
        user.setPoints(null);
        user.setPassword(null);
        user.setUpdateTime(DateUtils.getNowDate());
        try {
            return freeQueryUserMapper.updateFreeQueryUser(user);
        } catch (DuplicateKeyException | DataIntegrityViolationException e) {
            throw convertPersistenceException(e);
        }
    }

    @Override
    public int deleteFreeQueryUserByIds(Long[] ids, String operator) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        return freeQueryUserMapper.deleteFreeQueryUserByIds(ids, operator, DateUtils.getNowDate());
    }

    @Override
    public FreeQueryUser authenticate(String loginAccount, String rawPassword, String loginIp) {
        String account = normalizeAccount(loginAccount);
        if (StringUtils.isEmpty(account)) {
            throw new ServiceException("请输入账号");
        }
        if (StringUtils.isEmpty(rawPassword)) {
            throw new ServiceException("请输入密码");
        }
        FreeQueryUser user = resolveByAccountOrPhone(account);
        if (user == null) {
            throw new ServiceException("账号或密码错误");
        }
        if (!STATUS_NORMAL.equals(user.getStatus())) {
            throw new ServiceException("该账号已停用，请联系管理员");
        }
        if (!matchesPassword(rawPassword, user.getPassword())) {
            throw new ServiceException("账号或密码错误");
        }

        Date now = DateUtils.getNowDate();
        freeQueryUserMapper.updateFreeQueryUserLoginInfo(user.getId(), StringUtils.substring(loginIp, 0, 128), now,
                user.getAccount(), now);
        FreeQueryUser latest = freeQueryUserMapper.selectFreeQueryUserById(user.getId());
        return latest == null ? user : latest;
    }

    @Override
    public FreeQueryUser requireEnabledUser(Long userId) {
        FreeQueryUser user = freeQueryUserMapper.selectFreeQueryUserById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在或已被删除");
        }
        if (!STATUS_NORMAL.equals(user.getStatus())) {
            throw new ServiceException("该账号已停用，请联系管理员");
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int adjustPoints(FreeQueryPointAdjustRequest request, Long operatorId, String operatorName) {
        if (request == null || request.getUserId() == null) {
            throw new ServiceException("用户ID不能为空");
        }
        Integer pointAmount = request.getPointAmount();
        if (pointAmount == null || pointAmount <= 0) {
            throw new ServiceException("积分值必须大于0");
        }
        String pointType = StringUtils.trimToEmpty(request.getPointType());
        if (!POINT_TYPE_INCREASE.equals(pointType) && !POINT_TYPE_DEDUCT.equals(pointType)) {
            throw new ServiceException("积分变动类型不合法");
        }
        FreeQueryUser target = freeQueryUserMapper.selectFreeQueryUserById(request.getUserId());
        if (target == null) {
            throw new ServiceException("用户不存在");
        }
        Date now = DateUtils.getNowDate();
        int rows;
        if (POINT_TYPE_INCREASE.equals(pointType)) {
            rows = freeQueryUserMapper.increasePoints(target.getId(), pointAmount, operatorName, now);
        } else {
            rows = freeQueryUserMapper.deductPoints(target.getId(), pointAmount, operatorName, now);
        }
        if (rows <= 0) {
            throw new ServiceException("用户积分不足，无法扣减");
        }
        FreeQueryUser latest = freeQueryUserMapper.selectFreeQueryUserById(target.getId());
        String reason = StringUtils.defaultIfEmpty(request.getReason(),
                POINT_TYPE_INCREASE.equals(pointType) ? "后台充值积分" : "后台扣减积分");
        insertPointRecord(target.getId(), pointAmount, pointType, BIZ_TYPE_MANUAL, null, reason, operatorId,
                latest == null ? null : latest.getPoints(), operatorName, null);
        return rows;
    }

    @Override
    public int resetPassword(Long userId, String rawPassword, String operatorName) {
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        FreeQueryUser target = freeQueryUserMapper.selectFreeQueryUserById(userId);
        if (target == null) {
            throw new ServiceException("用户不存在");
        }
        String password = StringUtils.trimToEmpty(rawPassword);
        if (StringUtils.isEmpty(password)) {
            throw new ServiceException("新密码不能为空");
        }
        if (password.length() < 6) {
            throw new ServiceException("新密码长度至少6位");
        }
        return freeQueryUserMapper.updateFreeQueryUserPassword(userId, SecurityUtils.encryptPassword(password),
                operatorName, DateUtils.getNowDate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consumePointsForBatch(Long userId, Integer pointAmount, String bizNo, String operatorName) {
        if (userId == null || pointAmount == null || pointAmount <= 0) {
            throw new ServiceException("扣减积分参数不合法");
        }
        FreeQueryUser user = requireEnabledUser(userId);
        Date now = DateUtils.getNowDate();
        int rows = freeQueryUserMapper.deductPoints(userId, pointAmount, operatorName, now);
        if (rows <= 0) {
            throw new ServiceException("积分不足，无法完成批量查询");
        }
        FreeQueryUser latest = freeQueryUserMapper.selectFreeQueryUserById(userId);
        insertPointRecord(userId, pointAmount, POINT_TYPE_DEDUCT, BIZ_TYPE_BATCH_DEDUCT, bizNo,
                "批量查询预扣积分", user.getId(), latest == null ? null : latest.getPoints(), operatorName, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundPointsForBatch(Long userId, Integer pointAmount, String bizNo, String operatorName, String reason) {
        if (userId == null || pointAmount == null || pointAmount <= 0) {
            return;
        }
        FreeQueryUser target = freeQueryUserMapper.selectFreeQueryUserById(userId);
        if (target == null) {
            throw new ServiceException("用户不存在");
        }
        Date now = DateUtils.getNowDate();
        int rows = freeQueryUserMapper.increasePoints(userId, pointAmount, operatorName, now);
        if (rows <= 0) {
            throw new ServiceException("积分退回失败");
        }
        FreeQueryUser latest = freeQueryUserMapper.selectFreeQueryUserById(userId);
        insertPointRecord(userId, pointAmount, POINT_TYPE_INCREASE, BIZ_TYPE_BATCH_REFUND, bizNo,
                StringUtils.defaultIfEmpty(reason, "批量查询失败自动退回积分"), userId,
                latest == null ? null : latest.getPoints(), operatorName, null);
    }

    private void insertPointRecord(Long freeUserId, Integer pointAmount, String pointType, String businessType,
                                   String bizNo, String reason, Long operatorId, Integer balanceAfter,
                                   String createBy, String remark) {
        FreeQueryPointRecord record = new FreeQueryPointRecord();
        record.setFreeUserId(freeUserId);
        record.setPointAmount(pointAmount);
        record.setPointType(pointType);
        record.setBusinessType(businessType);
        record.setBizNo(bizNo);
        record.setReason(reason);
        record.setOperatorId(operatorId);
        record.setBalanceAfter(balanceAfter);
        record.setCreateBy(createBy);
        record.setCreateTime(DateUtils.getNowDate());
        record.setRemark(remark);
        freeQueryPointRecordMapper.insertFreeQueryPointRecord(record);
    }

    private FreeQueryUser resolveByAccountOrPhone(String accountOrPhone) {
        FreeQueryUser byAccount = freeQueryUserMapper.selectFreeQueryUserByAccount(accountOrPhone);
        if (byAccount != null) {
            return byAccount;
        }
        String phone = normalizePhone(accountOrPhone);
        if (StringUtils.isEmpty(phone)) {
            return null;
        }
        return freeQueryUserMapper.selectFreeQueryUserByPhone(phone);
    }

    private boolean matchesPassword(String rawPassword, String encodedPassword) {
        if (StringUtils.isEmpty(rawPassword) || StringUtils.isEmpty(encodedPassword)) {
            return false;
        }
        try {
            return SecurityUtils.matchesPassword(rawPassword, encodedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizeAccount(String account) {
        return StringUtils.trimToEmpty(account);
    }

    private String normalizePhone(String phone) {
        if (StringUtils.isEmpty(phone)) {
            return "";
        }
        return phone.replaceAll("[^\\d]", "");
    }

    private int normalizePoints(Integer points) {
        if (points == null || points < 0) {
            return 0;
        }
        return points;
    }

    private RuntimeException convertPersistenceException(RuntimeException exception) {
        if (isDuplicateAccountException(exception)) {
            return new ServiceException("账号已存在");
        }
        return exception;
    }

    private boolean isDuplicateAccountException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        String message = StringUtils.defaultString(throwable.getMessage());
        if (StringUtils.containsIgnoreCase(message, "uk_free_query_user_account")
                || StringUtils.containsIgnoreCase(message, "duplicate entry")) {
            return true;
        }
        return isDuplicateAccountException(throwable.getCause());
    }
}
