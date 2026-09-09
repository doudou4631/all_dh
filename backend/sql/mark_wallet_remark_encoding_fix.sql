-- 修复历史上因错误客户端编码写入的标记钱包流水备注。
-- 原备注已不可逆变为问号，根据调整流水正负号恢复业务含义。
UPDATE mark_wallet_log
SET remark = CASE WHEN change_amount < 0 THEN '账户扣减' ELSE '账户充值' END
WHERE biz_type = 'ADJUST'
  AND remark REGEXP '^[?]+$';
