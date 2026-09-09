ALTER TABLE `mark_order`
  ADD COLUMN IF NOT EXISTS `user_remark` varchar(500) DEFAULT NULL COMMENT '用户自定义备注' AFTER `remark`;

UPDATE `mark_order`
SET `user_remark` = `remark`
WHERE (`user_remark` IS NULL OR `user_remark` = '')
  AND LOWER(`platform_code`) NOT IN (
    '360', '360fugai', 'qihu_second',
    'tengxun', 'tencent', 'tx', 'txwz',
    'td_second'
  )
  AND `remark` IS NOT NULL
  AND `remark` <> '';

UPDATE `mark_order`
SET `user_remark` = NULL
WHERE LOWER(`platform_code`) IN (
  'tengxun', 'tencent', 'tx', 'txwz',
  'td_second'
);
