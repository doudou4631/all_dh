SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M11：查询日志二期（结构化高频字段）
-- 1) user_api_query_record 增加结构化字段
-- 2) 增加二期检索/趋势相关索引
-- =========================================================

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_api_query_record'
          AND COLUMN_NAME = 'source_type'
    ),
    'SELECT 1',
    'ALTER TABLE `user_api_query_record` ADD COLUMN `source_type` varchar(32) NULL DEFAULT NULL COMMENT ''查询来源（FREE_SINGLE/FREE_BATCH）'' AFTER `phone`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_api_query_record'
          AND COLUMN_NAME = 'device_id'
    ),
    'SELECT 1',
    'ALTER TABLE `user_api_query_record` ADD COLUMN `device_id` varchar(128) NULL DEFAULT NULL COMMENT ''设备ID'' AFTER `source_type`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_api_query_record'
          AND COLUMN_NAME = 'device_source'
    ),
    'SELECT 1',
    'ALTER TABLE `user_api_query_record` ADD COLUMN `device_source` varchar(32) NULL DEFAULT NULL COMMENT ''设备来源（client/ip-fallback）'' AFTER `device_id`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_api_query_record'
          AND COLUMN_NAME = 'used_before'
    ),
    'SELECT 1',
    'ALTER TABLE `user_api_query_record` ADD COLUMN `used_before` int(11) NULL DEFAULT NULL COMMENT ''使用前次数'' AFTER `device_source`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_api_query_record'
          AND COLUMN_NAME = 'used_after'
    ),
    'SELECT 1',
    'ALTER TABLE `user_api_query_record` ADD COLUMN `used_after` int(11) NULL DEFAULT NULL COMMENT ''使用后次数'' AFTER `used_before`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_api_query_record'
          AND COLUMN_NAME = 'limit_value'
    ),
    'SELECT 1',
    'ALTER TABLE `user_api_query_record` ADD COLUMN `limit_value` int(11) NULL DEFAULT NULL COMMENT ''限额值'' AFTER `used_after`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_api_query_record'
          AND COLUMN_NAME = 'error_code'
    ),
    'SELECT 1',
    'ALTER TABLE `user_api_query_record` ADD COLUMN `error_code` varchar(64) NULL DEFAULT NULL COMMENT ''错误码（结构化）'' AFTER `limit_value`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_api_query_record'
          AND COLUMN_NAME = 'ip_addr'
    ),
    'SELECT 1',
    'ALTER TABLE `user_api_query_record` ADD COLUMN `ip_addr` varchar(64) NULL DEFAULT NULL COMMENT ''来源IP'' AFTER `error_code`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_api_query_record'
          AND INDEX_NAME = 'idx_uaqr_source_time'
    ),
    'SELECT 1',
    'ALTER TABLE `user_api_query_record` ADD INDEX `idx_uaqr_source_time` (`source_type`, `create_time`)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_api_query_record'
          AND INDEX_NAME = 'idx_uaqr_device_time'
    ),
    'SELECT 1',
    'ALTER TABLE `user_api_query_record` ADD INDEX `idx_uaqr_device_time` (`device_id`, `create_time`)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_api_query_record'
          AND INDEX_NAME = 'idx_uaqr_error_time'
    ),
    'SELECT 1',
    'ALTER TABLE `user_api_query_record` ADD INDEX `idx_uaqr_error_time` (`error_code`, `create_time`)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_api_query_record'
          AND INDEX_NAME = 'idx_uaqr_task_id'
    ),
    'SELECT 1',
    'ALTER TABLE `user_api_query_record` ADD INDEX `idx_uaqr_task_id` (`task_id`)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_api_query_record'
          AND INDEX_NAME = 'idx_uaqr_ip_time'
    ),
    'SELECT 1',
    'ALTER TABLE `user_api_query_record` ADD INDEX `idx_uaqr_ip_time` (`ip_addr`, `create_time`)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
