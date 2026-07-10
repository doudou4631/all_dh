SET NAMES utf8mb4;

-- M49：代理端按用户开启/关闭平台
-- 1) mark_user_platform_price 增加 status 字段：0 开启，1 关闭
-- 2) 将“下级账户/下线账户”菜单文案统一为“用户管理”

SET @schema_name := DATABASE();

SET @has_status := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'mark_user_platform_price'
    AND COLUMN_NAME = 'status'
);

SET @sql := IF(
  @has_status = 0,
  'ALTER TABLE `mark_user_platform_price` ADD COLUMN `status` char(1) NOT NULL DEFAULT ''0'' COMMENT ''状态（0开启 1关闭）'' AFTER `unit_price`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `mark_user_platform_price`
SET `status` = '0'
WHERE `status` IS NULL OR `status` = '';

UPDATE `sys_menu`
SET `menu_name` = CONVERT(UNHEX('E794A8E688B7E7AEA1E79086') USING utf8mb4),
    `remark` = 'M49-代理端用户管理',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `path` = 'agentDownstream'
  AND `component` = 'server/mark/agent/downstream/index';
