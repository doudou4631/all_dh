SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M10：标记模板代理隔离与权限补齐
-- 1) mark_platform_template 新增 owner_user_id（若不存在）
-- 2) mark_platform_template 新增 is_default（若不存在）并回填每个 owner 的默认模板
-- 3) 依据 create_by -> sys_user.user_name 回填 owner_user_id
-- 4) agent 角色补齐标记模板菜单与按钮权限
-- 说明：unitPrice 存在 template_info JSON 内，无需新增物理字段
-- =========================================================

SET @has_owner_user_id := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mark_platform_template'
    AND COLUMN_NAME = 'owner_user_id'
);
SET @alter_owner_sql := IF(
  @has_owner_user_id = 0,
  'ALTER TABLE `mark_platform_template` ADD COLUMN `owner_user_id` bigint(20) NULL COMMENT ''模板归属用户ID'' AFTER `status`',
  'SELECT 1'
);
PREPARE stmt_owner FROM @alter_owner_sql;
EXECUTE stmt_owner;
DEALLOCATE PREPARE stmt_owner;

SET @has_owner_idx := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mark_platform_template'
    AND INDEX_NAME = 'idx_mark_platform_template_owner_user_id'
);
SET @alter_owner_idx_sql := IF(
  @has_owner_idx = 0,
  'ALTER TABLE `mark_platform_template` ADD INDEX `idx_mark_platform_template_owner_user_id` (`owner_user_id`)',
  'SELECT 1'
);
PREPARE stmt_owner_idx FROM @alter_owner_idx_sql;
EXECUTE stmt_owner_idx;
DEALLOCATE PREPARE stmt_owner_idx;

SET @has_is_default := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mark_platform_template'
    AND COLUMN_NAME = 'is_default'
);
SET @alter_is_default_sql := IF(
  @has_is_default = 0,
  'ALTER TABLE `mark_platform_template` ADD COLUMN `is_default` char(1) NOT NULL DEFAULT ''0'' COMMENT ''是否默认模板（0否 1是）'' AFTER `status`',
  'SELECT 1'
);
PREPARE stmt_is_default FROM @alter_is_default_sql;
EXECUTE stmt_is_default;
DEALLOCATE PREPARE stmt_is_default;

UPDATE `mark_platform_template` t
LEFT JOIN `sys_user` u ON CONVERT(u.user_name USING utf8mb4) COLLATE utf8mb4_general_ci
                       = CONVERT(t.create_by USING utf8mb4) COLLATE utf8mb4_general_ci
SET t.owner_user_id = u.user_id
WHERE t.owner_user_id IS NULL
  AND u.user_id IS NOT NULL;

UPDATE `mark_platform_template`
SET `is_default` = '0'
WHERE `is_default` IS NULL
   OR `is_default` NOT IN ('0', '1');

UPDATE `mark_platform_template` t
JOIN `sys_user` u ON u.user_id = t.owner_user_id
SET t.is_default = '1'
WHERE u.rel_mark_template IS NOT NULL
  AND u.rel_mark_template = t.id;

UPDATE `mark_platform_template` t
JOIN (
  SELECT owner_user_id, MIN(id) AS keep_id
  FROM `mark_platform_template`
  WHERE owner_user_id IS NOT NULL
    AND is_default = '1'
  GROUP BY owner_user_id
  HAVING COUNT(*) > 1
) d ON d.owner_user_id = t.owner_user_id
SET t.is_default = CASE WHEN t.id = d.keep_id THEN '1' ELSE '0' END
WHERE t.is_default = '1';

UPDATE `mark_platform_template` t
JOIN (
  SELECT owner_user_id, MIN(id) AS default_id
  FROM `mark_platform_template`
  WHERE owner_user_id IS NOT NULL
    AND status = '0'
  GROUP BY owner_user_id
  HAVING SUM(CASE WHEN is_default = '1' THEN 1 ELSE 0 END) = 0
) d ON d.owner_user_id = t.owner_user_id AND d.default_id = t.id
SET t.is_default = '1';

SET @has_owner_default_idx := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mark_platform_template'
    AND INDEX_NAME = 'idx_mark_platform_template_owner_default'
);
SET @alter_owner_default_idx_sql := IF(
  @has_owner_default_idx = 0,
  'ALTER TABLE `mark_platform_template` ADD INDEX `idx_mark_platform_template_owner_default` (`owner_user_id`, `is_default`, `status`)',
  'SELECT 1'
);
PREPARE stmt_owner_default_idx FROM @alter_owner_default_idx_sql;
EXECUTE stmt_owner_default_idx;
DEALLOCATE PREPARE stmt_owner_default_idx;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN (
  SELECT 900100000127 AS menu_id UNION ALL
  SELECT 900100001701 UNION ALL
  SELECT 900100001702 UNION ALL
  SELECT 900100001703 UNION ALL
  SELECT 900100001704
) m
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key = 'agent'
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;
