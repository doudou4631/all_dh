SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- M17: mark order audit workflow (Phase 1)

SET @schema_name := DATABASE();

SET @exists_audit_status := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'mark_order'
    AND column_name = 'audit_status'
);
SET @sql_audit_status := IF(
  @exists_audit_status = 0,
  'ALTER TABLE `mark_order` ADD COLUMN `audit_status` char(1) NOT NULL DEFAULT ''0'' COMMENT ''audit status'' AFTER `order_status`',
  'SELECT 1'
);
PREPARE stmt FROM @sql_audit_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists_audit_opinion := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'mark_order'
    AND column_name = 'audit_opinion'
);
SET @sql_audit_opinion := IF(
  @exists_audit_opinion = 0,
  'ALTER TABLE `mark_order` ADD COLUMN `audit_opinion` varchar(500) DEFAULT NULL COMMENT ''audit opinion'' AFTER `audit_status`',
  'SELECT 1'
);
PREPARE stmt FROM @sql_audit_opinion;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists_audit_by := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'mark_order'
    AND column_name = 'audit_by'
);
SET @sql_audit_by := IF(
  @exists_audit_by = 0,
  'ALTER TABLE `mark_order` ADD COLUMN `audit_by` varchar(64) DEFAULT NULL COMMENT ''audit by'' AFTER `audit_opinion`',
  'SELECT 1'
);
PREPARE stmt FROM @sql_audit_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists_audit_time := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'mark_order'
    AND column_name = 'audit_time'
);
SET @sql_audit_time := IF(
  @exists_audit_time = 0,
  'ALTER TABLE `mark_order` ADD COLUMN `audit_time` datetime DEFAULT NULL COMMENT ''audit time'' AFTER `audit_by`',
  'SELECT 1'
);
PREPARE stmt FROM @sql_audit_time;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists_idx_audit_status := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @schema_name
    AND table_name = 'mark_order'
    AND index_name = 'idx_mark_order_audit_status'
);
SET @sql_idx_audit_status := IF(
  @exists_idx_audit_status = 0,
  'ALTER TABLE `mark_order` ADD KEY `idx_mark_order_audit_status` (`audit_status`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql_idx_audit_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_backfill_audit := IF(
  @exists_audit_status = 0,
  'UPDATE `mark_order` SET `audit_status` = ''1'' WHERE `audit_status` IS NULL OR `audit_status` = '''' OR `audit_status` = ''0''',
  'SELECT 1'
);
PREPARE stmt FROM @sql_backfill_audit;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000110, CONVERT(UNHEX('E8AEA2E59595E5AEA1E6A0B8') USING utf8mb4), 900100000001, 2, 'agentAudit', 'server/mark/agent/audit', '', '',
       1, 0, 'C', '0', '0', 'server:markAgent:audit:list', 'edit', 'admin', NOW(), 'M17-agent-audit-menu'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000110);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100001304, CONVERT(UNHEX('E5AEA1E6A0B8E58897E8A1A8E69FA5E8AFA2') USING utf8mb4), 900100000110, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAgent:audit:list', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001304);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100001305, CONVERT(UNHEX('E5AEA1E6A0B8E9809AE8BF87') USING utf8mb4), 900100000110, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAgent:audit:pass', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001305);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100001306, CONVERT(UNHEX('E5AEA1E6A0B8E69292E7BB9D') USING utf8mb4), 900100000110, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAgent:audit:reject', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001306);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100001307, CONVERT(UNHEX('E5AEA1E6A0B8E68993E59B9E') USING utf8mb4), 900100000110, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAgent:audit:return', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001307);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN (
  SELECT 900100000110 AS menu_id UNION ALL
  SELECT 900100001304 UNION ALL
  SELECT 900100001305 UNION ALL
  SELECT 900100001306 UNION ALL
  SELECT 900100001307
) m
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key IN ('admin', 'agent', 'mark_agent')
  AND rm.role_id IS NULL;

UPDATE sys_menu SET menu_name = CONVERT(UNHEX('E8AEA2E59595E5AEA1E6A0B8') USING utf8mb4), remark = 'M17-agent-audit-menu'
WHERE menu_id = 900100000110;

UPDATE sys_menu SET menu_name = CONVERT(UNHEX('E5AEA1E6A0B8E58897E8A1A8E69FA5E8AFA2') USING utf8mb4)
WHERE menu_id = 900100001304;

UPDATE sys_menu SET menu_name = CONVERT(UNHEX('E5AEA1E6A0B8E9809AE8BF87') USING utf8mb4)
WHERE menu_id = 900100001305;

UPDATE sys_menu SET menu_name = CONVERT(UNHEX('E5AEA1E6A0B8E69292E7BB9D') USING utf8mb4)
WHERE menu_id = 900100001306;

UPDATE sys_menu SET menu_name = CONVERT(UNHEX('E5AEA1E6A0B8E68993E59B9E') USING utf8mb4)
WHERE menu_id = 900100001307;

SET FOREIGN_KEY_CHECKS = 1;
