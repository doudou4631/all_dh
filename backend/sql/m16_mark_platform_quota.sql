SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M16：标记业务按平台余额模型
-- 1) 新增用户-平台余额表 mark_user_platform_quota
-- 2) 扩展 mark_wallet_log 增加平台维度字段
-- 注意：本脚本不修改任何菜单/角色授权数据
-- =========================================================

CREATE TABLE IF NOT EXISTS `mark_user_platform_quota` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `platform_code` varchar(64) NOT NULL COMMENT '平台编码',
  `platform_name` varchar(128) DEFAULT NULL COMMENT '平台名称',
  `remain_count` bigint(20) NOT NULL DEFAULT '0' COMMENT '剩余次数',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mark_user_platform_quota` (`user_id`,`platform_code`),
  KEY `idx_mark_user_platform_quota_platform_code` (`platform_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标记用户平台余额表';

SET @schema_name := DATABASE();

SET @exists_platform_code := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'mark_wallet_log'
    AND column_name = 'platform_code'
);
SET @sql_platform_code := IF(
  @exists_platform_code = 0,
  'ALTER TABLE `mark_wallet_log` ADD COLUMN `platform_code` varchar(64) DEFAULT NULL COMMENT ''平台编码'' AFTER `order_item_id`',
  'SELECT 1'
);
PREPARE stmt_platform_code FROM @sql_platform_code;
EXECUTE stmt_platform_code;
DEALLOCATE PREPARE stmt_platform_code;

SET @exists_platform_name := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'mark_wallet_log'
    AND column_name = 'platform_name'
);
SET @sql_platform_name := IF(
  @exists_platform_name = 0,
  'ALTER TABLE `mark_wallet_log` ADD COLUMN `platform_name` varchar(128) DEFAULT NULL COMMENT ''平台名称'' AFTER `platform_code`',
  'SELECT 1'
);
PREPARE stmt_platform_name FROM @sql_platform_name;
EXECUTE stmt_platform_name;
DEALLOCATE PREPARE stmt_platform_name;

SET @exists_idx_platform_code := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @schema_name
    AND table_name = 'mark_wallet_log'
    AND index_name = 'idx_mark_wallet_log_platform_code'
);
SET @sql_idx_platform_code := IF(
  @exists_idx_platform_code = 0,
  'ALTER TABLE `mark_wallet_log` ADD KEY `idx_mark_wallet_log_platform_code` (`platform_code`)',
  'SELECT 1'
);
PREPARE stmt_idx_platform_code FROM @sql_idx_platform_code;
EXECUTE stmt_idx_platform_code;
DEALLOCATE PREPARE stmt_idx_platform_code;

SET FOREIGN_KEY_CHECKS = 1;
