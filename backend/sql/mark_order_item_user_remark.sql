ALTER TABLE `mark_order_item`
  ADD COLUMN IF NOT EXISTS `user_remark` varchar(500) DEFAULT NULL COMMENT 'User-defined remark' AFTER `remark`;

UPDATE `mark_order_item` moi
INNER JOIN `mark_order` mo ON mo.id = moi.order_id
SET moi.user_remark = mo.user_remark
WHERE (moi.user_remark IS NULL OR moi.user_remark = '')
  AND mo.user_remark IS NOT NULL
  AND mo.user_remark <> '';
