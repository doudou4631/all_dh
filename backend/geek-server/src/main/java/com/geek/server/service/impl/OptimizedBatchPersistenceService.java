package com.geek.server.service.impl;

import com.geek.common.core.domain.entity.SysUser;
import com.geek.server.domain.UserApiQueryRecord;
import com.geek.server.domain.UserPointRecord;
import com.geek.server.mapper.UserApiQueryRecordMapper;
import com.geek.server.mapper.UserPointRecordMapper;
import com.geek.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Batch insert query records, batch insert point rows, single user points update.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizedBatchPersistenceService {

    private static final int BATCH_SIZE = 200;

    private final UserApiQueryRecordMapper userApiQueryRecordMapper;
    private final UserPointRecordMapper userPointRecordMapper;
    private final ISysUserService sysUserService;

    @Transactional(rollbackFor = Exception.class)
    public void persistAfterOptimizedBatch(List<UserApiQueryRecord> records,
                                           Long userId,
                                           List<UserPointRecord> pointRecords) {
        if (records != null && !records.isEmpty()) {
            for (int i = 0; i < records.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, records.size());
                userApiQueryRecordMapper.insertUserApiQueryRecordBatch(records.subList(i, end));
            }
        }

        if (pointRecords == null || pointRecords.isEmpty()) {
            return;
        }

        SysUser user = sysUserService.selectUserById(userId);
        if (user == null) {
            throw new IllegalStateException("user not found");
        }
        int deduct = pointRecords.size();
        Integer cur = user.getPoints();
        if (cur == null || cur < deduct) {
            throw new IllegalStateException("insufficient points, need " + deduct);
        }
        user.setPoints(cur - deduct);
        if (!sysUserService.updateUser(user)) {
            throw new IllegalStateException("update user points failed");
        }

        for (int i = 0; i < pointRecords.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, pointRecords.size());
            userPointRecordMapper.insertUserPointRecordBatch(pointRecords.subList(i, end));
        }
        log.info("optimized batch persist records={} pointRows={} deducted={}",
                records != null ? records.size() : 0, pointRecords.size(), deduct);
    }
}
