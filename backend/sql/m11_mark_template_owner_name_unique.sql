SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M11：标记模板唯一键升级（全局模板名 -> 同用户模板名）
-- 1) 预检查 owner_user_id 列存在
-- 2) 回填 owner_user_id（create_by -> sys_user.user_name）
-- 3) 阻断脏数据：owner_user_id 为空 / 同 owner+name 重复
-- 4) 删除旧唯一键 uk_mark_platform_template_name
-- 5) 新增唯一键 uk_mark_platform_template_owner_name(owner_user_id, template_name)
-- 6) 收紧 owner_user_id 为 NOT NULL（前置检查通过后）
-- =========================================================

SET @has_owner_user_id := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mark_platform_template'
    AND COLUMN_NAME = 'owner_user_id'
);
SET @assert_owner_column_sql := IF(
  @has_owner_user_id > 0,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''m11 aborted: owner_user_id column is missing in mark_platform_template'''
);
PREPARE stmt_assert_owner_column FROM @assert_owner_column_sql;
EXECUTE stmt_assert_owner_column;
DEALLOCATE PREPARE stmt_assert_owner_column;

UPDATE `mark_platform_template` t
LEFT JOIN `sys_user` u ON CONVERT(u.user_name USING utf8mb4) COLLATE utf8mb4_general_ci
                       = CONVERT(t.create_by USING utf8mb4) COLLATE utf8mb4_general_ci
SET t.owner_user_id = u.user_id
WHERE t.owner_user_id IS NULL
  AND u.user_id IS NOT NULL;

SET @null_owner_count := (
  SELECT COUNT(1)
  FROM `mark_platform_template`
  WHERE owner_user_id IS NULL
);
SET @assert_null_owner_sql := IF(
  @null_owner_count = 0,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''m11 aborted: mark_platform_template.owner_user_id still has NULL rows'''
);
PREPARE stmt_assert_null_owner FROM @assert_null_owner_sql;
EXECUTE stmt_assert_null_owner;
DEALLOCATE PREPARE stmt_assert_null_owner;

SET @owner_name_dup_count := (
  SELECT COUNT(1)
  FROM (
    SELECT owner_user_id, template_name, COUNT(1) AS dup_cnt
    FROM `mark_platform_template`
    GROUP BY owner_user_id, template_name
    HAVING COUNT(1) > 1
  ) t
);
SET @assert_owner_name_dup_sql := IF(
  @owner_name_dup_count = 0,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''m11 aborted: duplicate rows found for (owner_user_id, template_name)'''
);
PREPARE stmt_assert_owner_name_dup FROM @assert_owner_name_dup_sql;
EXECUTE stmt_assert_owner_name_dup;
DEALLOCATE PREPARE stmt_assert_owner_name_dup;

SET @has_old_unique := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mark_platform_template'
    AND INDEX_NAME = 'uk_mark_platform_template_name'
);
SET @drop_old_unique_sql := IF(
  @has_old_unique > 0,
  'ALTER TABLE `mark_platform_template` DROP INDEX `uk_mark_platform_template_name`',
  'SELECT 1'
);
PREPARE stmt_drop_old_unique FROM @drop_old_unique_sql;
EXECUTE stmt_drop_old_unique;
DEALLOCATE PREPARE stmt_drop_old_unique;

SET @has_new_unique := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mark_platform_template'
    AND INDEX_NAME = 'uk_mark_platform_template_owner_name'
);
SET @add_new_unique_sql := IF(
  @has_new_unique = 0,
  'ALTER TABLE `mark_platform_template` ADD UNIQUE INDEX `uk_mark_platform_template_owner_name` (`owner_user_id`, `template_name`)',
  'SELECT 1'
);
PREPARE stmt_add_new_unique FROM @add_new_unique_sql;
EXECUTE stmt_add_new_unique;
DEALLOCATE PREPARE stmt_add_new_unique;

SET @owner_user_id_nullable := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mark_platform_template'
    AND COLUMN_NAME = 'owner_user_id'
    AND IS_NULLABLE = 'YES'
);
SET @set_owner_not_null_sql := IF(
  @owner_user_id_nullable > 0,
  'ALTER TABLE `mark_platform_template` MODIFY COLUMN `owner_user_id` bigint(20) NOT NULL COMMENT ''模板归属用户ID''',
  'SELECT 1'
);
PREPARE stmt_set_owner_not_null FROM @set_owner_not_null_sql;
EXECUTE stmt_set_owner_not_null;
DEALLOCATE PREPARE stmt_set_owner_not_null;

SET FOREIGN_KEY_CHECKS = 1;
