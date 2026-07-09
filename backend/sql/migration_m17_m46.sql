SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- ===== BEGIN m17_mark_order_audit.sql =====
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

-- ===== END m17_mark_order_audit.sql =====


-- ===== BEGIN m17_mark_order_audit_menu_fix.sql =====
SET NAMES utf8mb4;

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

-- ===== END m17_mark_order_audit_menu_fix.sql =====


-- ===== BEGIN m18_mark_user_notice.sql =====
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- M18: mark user notice (Phase 1.5)

CREATE TABLE IF NOT EXISTS `mark_user_notice` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `user_id` bigint(20) NOT NULL COMMENT 'user id',
  `notice_type` varchar(32) NOT NULL DEFAULT 'ORDER_AUDIT' COMMENT 'notice type',
  `title` varchar(128) NOT NULL COMMENT 'title',
  `content` varchar(500) DEFAULT NULL COMMENT 'content',
  `biz_type` varchar(32) DEFAULT NULL COMMENT 'biz type',
  `biz_id` bigint(20) DEFAULT NULL COMMENT 'biz id',
  `read_flag` char(1) NOT NULL DEFAULT '0' COMMENT '0 unread 1 read',
  `create_by` varchar(64) DEFAULT '' COMMENT 'creator',
  `create_time` datetime DEFAULT NULL COMMENT 'create time',
  `update_by` varchar(64) DEFAULT '' COMMENT 'updater',
  `update_time` datetime DEFAULT NULL COMMENT 'update time',
  `remark` varchar(500) DEFAULT NULL COMMENT 'remark',
  PRIMARY KEY (`id`),
  KEY `idx_mark_user_notice_user_read` (`user_id`,`read_flag`),
  KEY `idx_mark_user_notice_user_time` (`user_id`,`create_time`),
  KEY `idx_mark_user_notice_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mark user notice';

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000112, CONVERT(UNHEX('E68891E79A84E6B688E681AF') USING utf8mb4), 900100000001, 5, 'userNotice', 'server/mark/user/notice', '', '',
       1, 0, 'C', '0', '0', 'server:markUser:notice:list', 'message', 'admin', NOW(), 'M18-user-notice-menu'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000112);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100001310, CONVERT(UNHEX('E6B688E681AFE58897E8A1A8') USING utf8mb4), 900100000112, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markUser:notice:list', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001310);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100001311, CONVERT(UNHEX('E6B688E681AFE5B7B2E8AFBB') USING utf8mb4), 900100000112, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markUser:notice:read', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001311);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100001312, CONVERT(UNHEX('E5AEA1E6A0B8E7BB9FE8AEA1') USING utf8mb4), 900100000110, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAgent:audit:stats', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001312);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN (
  SELECT 900100000112 AS menu_id UNION ALL
  SELECT 900100001310 UNION ALL
  SELECT 900100001311
) m
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key IN ('admin', 'common', 'user', 'mark_user')
  AND rm.role_id IS NULL;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, 900100001312
FROM `sys_role` r
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = 900100001312
WHERE r.role_key IN ('admin', 'agent', 'mark_agent')
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- ===== END m18_mark_user_notice.sql =====


-- ===== BEGIN m19_agent_portal_optimize.sql =====
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- M19: agent portal menu optimize

UPDATE sys_menu
SET menu_name = CONVERT(UNHEX('E5A484E79086E8AEA2E58D95') USING utf8mb4),
    order_num = 3,
    remark = 'M19-agent-process-menu'
WHERE menu_id = 900100000103;

UPDATE sys_menu
SET menu_name = CONVERT(UNHEX('E8B4A6E688B7E7AEA1E79086') USING utf8mb4),
    order_num = 4,
    remark = 'M19-agent-account-menu'
WHERE menu_id = 900100000109;

UPDATE sys_menu
SET order_num = 2
WHERE menu_id = 900100000110;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 900100000114, CONVERT(UNHEX('E4B8BEE7BBA7E8B4A6E688B7') USING utf8mb4), 900100000001, 1, 'agentDownstream', 'server/mark/agent/downstream/index', '', '',
       1, 0, 'C', '0', '0', 'system:user:list', 'peoples', 'admin', NOW(), 'M19-agent-downstream-menu'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000114);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 900100000113, CONVERT(UNHEX('E8B584E98791E6B581E6B0B4E6988EE7BB86') USING utf8mb4), 900100000001, 5, 'agentWallet', 'server/mark/agent/wallet', '', '',
       1, 0, 'C', '0', '0', 'server:markAgent:wallet:list', 'money', 'admin', NOW(), 'M19-agent-wallet-menu'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000113);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN (
  SELECT 900100000113 AS menu_id UNION ALL
  SELECT 900100000114
) m
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key IN ('agent', 'mark_agent')
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- ===== END m19_agent_portal_optimize.sql =====


-- ===== BEGIN m20_agent_portal_sync.sql =====
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- M20: agent portal menu tree sync (reference: 175.178.222.176)

-- Directory: ??????
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 900100000115, CONVERT(UNHEX('E7B3BBE7BB9FE8AEBEE7BDAE') USING utf8mb4), 900100000001, 1, 'agentSetting', '', '', '',
       1, 0, 'M', '0', '0', '', 'system', 'admin', NOW(), 'M20-agent-setting-dir'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000115);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 900100000116, CONVERT(UNHEX('E59FBAE69CACE8B584E69699') USING utf8mb4), 900100000115, 1, 'agentProfile', 'system/user/profile/index', '{"activeTab":"userinfo"}', '',
       1, 0, 'C', '0', '0', '', 'user', 'admin', NOW(), 'M20-agent-profile'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000116);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 900100000117, CONVERT(UNHEX('E4BFAEE694B9E5AF86E7A081') USING utf8mb4), 900100000115, 2, 'agentProfilePwd', 'system/user/profile/index', '{"activeTab":"resetPwd"}', '',
       1, 0, 'C', '0', '0', '', 'password', 'admin', NOW(), 'M20-agent-reset-pwd'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000117);

-- Directory: ??????
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 900100000118, CONVERT(UNHEX('E6A087E8AEB0E5A484E79086') USING utf8mb4), 900100000001, 2, 'agentProcess', '', '', '',
       1, 0, 'M', '0', '0', '', 'edit', 'admin', NOW(), 'M20-agent-process-dir'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000118);

UPDATE sys_menu
SET parent_id = 900100000118,
    order_num = 1,
    menu_name = CONVERT(UNHEX('E5A484E79086E8AEA2E58D95') USING utf8mb4)
WHERE menu_id = 900100000103;

UPDATE sys_menu
SET parent_id = 900100000118,
    order_num = 4
WHERE menu_id = 900100000110;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 900100000119, CONVERT(UNHEX('E5A484E79086E8AEA2E58D9528E4B88BE7BAA729') USING utf8mb4), 900100000118, 2, 'agentProcessDownstream', 'server/mark/agent/process/downstream', '', '',
       1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'list', 'admin', NOW(), 'M20-agent-process-downstream'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000119);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 900100000128, CONVERT(UNHEX('E6B8855444E694B6E99481E8AF81E7A081') USING utf8mb4), 900100000118, 3, 'agentTdCaptcha', 'server/mark/agent/td/index', '', '',
       1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'message', 'admin', NOW(), 'M20-agent-td-captcha'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000128);

-- Directory: ?????????
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 900100000129, CONVERT(UNHEX('E4BBA3E79086E59586E4BD93E7B3BB') USING utf8mb4), 900100000001, 3, 'agentHierarchy', '', '', '',
       1, 0, 'M', '0', '0', '', 'peoples', 'admin', NOW(), 'M20-agent-hierarchy-dir'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000129);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 900100000130, CONVERT(UNHEX('E680BBE4BBA3E79086E4BFA1E681AF') USING utf8mb4), 900100000129, 1, 'agentSummary', 'server/mark/agent/summary/index', '', '',
       1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'dashboard', 'admin', NOW(), 'M20-agent-summary'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000130);

UPDATE sys_menu
SET parent_id = 900100000129,
    order_num = 2
WHERE menu_id = 900100000114;

UPDATE sys_menu
SET parent_id = 900100000129,
    order_num = 3,
    menu_name = CONVERT(UNHEX('E8B4A6E688B7E7AEA1E79086') USING utf8mb4)
WHERE menu_id = 900100000109;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 900100000131, CONVERT(UNHEX('E8B584E98791E8BDACE8B4A6E6988EE7BB86') USING utf8mb4), 900100000129, 4, 'agentTransfer', 'server/mark/agent/transfer/index', '', '',
       1, 0, 'C', '0', '0', 'server:markAgent:wallet:list', 'money', 'admin', NOW(), 'M20-agent-transfer'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000131);

-- Directory: ??????
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 900100000132, CONVERT(UNHEX('E8B584E98791E7AEA1E79086') USING utf8mb4), 900100000001, 4, 'agentFunds', '', '', '',
       1, 0, 'M', '0', '0', '', 'wallet', 'admin', NOW(), 'M20-agent-funds-dir'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000132);

UPDATE sys_menu
SET parent_id = 900100000132,
    order_num = 1
WHERE menu_id = 900100000113;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 900100000133, CONVERT(UNHEX('E8B584E98791E6988EE7BB8628E4B88BE7BAA729') USING utf8mb4), 900100000132, 2, 'agentWalletDownstream', 'server/mark/agent/wallet/downstream', '', '',
       1, 0, 'C', '0', '0', 'server:markAgent:wallet:list', 'documentation', 'admin', NOW(), 'M20-agent-wallet-downstream'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000133);

-- Grant agent role all new menus
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN (
  SELECT 900100000115 AS menu_id UNION ALL
  SELECT 900100000116 UNION ALL SELECT 900100000117 UNION ALL
  SELECT 900100000118 UNION ALL SELECT 900100000119 UNION ALL SELECT 900100000128 UNION ALL
  SELECT 900100000129 UNION ALL SELECT 900100000130 UNION ALL SELECT 900100000131 UNION ALL
  SELECT 900100000132 UNION ALL SELECT 900100000133
) m
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key IN ('agent', 'mark_agent')
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- ===== END m20_agent_portal_sync.sql =====


-- ===== BEGIN m21_agent_encoding_fix.sql =====
SET NAMES utf8mb4;

UPDATE sys_menu
SET menu_name = CONVERT(UNHEX('E5A484E79086E8AEA2E58D95') USING utf8mb4)
WHERE menu_id = 900100000103;

UPDATE sys_menu
SET menu_name = CONVERT(UNHEX('E5A484E79086E8AEA2E58D9528E4B88BE7BAA729') USING utf8mb4)
WHERE menu_id = 900100000119;

-- ===== END m21_agent_encoding_fix.sql =====


-- ===== BEGIN m22_agent_process_platform_menus.sql =====
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
UPDATE sys_menu SET visible = '1', order_num = 99 WHERE menu_id = 900100000103;
UPDATE sys_menu SET order_num = 90 WHERE menu_id = 900100000119;
UPDATE sys_menu SET order_num = 91 WHERE menu_id = 900100000128;
UPDATE sys_menu SET order_num = 92 WHERE menu_id = 900100000110;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000134, CONVERT(UNHEX('E7A7BBE58AA8E9AB98E9A291') USING utf8mb4), 900100000118, 1, 'agentProcessMobileGaopin', 'server/mark/agent/process/platform', '{"platformCode":"mobile_gaopin"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000134);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000135, CONVERT(UNHEX('E7A7BBE58AA8E9AB98E9A291') USING utf8mb4), 900100000118, 2, 'agentProcessYidonggaopin', 'server/mark/agent/process/platform', '{"platformCode":"yidonggaopin"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000135);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000136, CONVERT(UNHEX('E6B3B0E8BFAAE9AB98E9A291') USING utf8mb4), 900100000118, 3, 'agentProcessTdGaopin', 'server/mark/agent/process/platform', '{"platformCode":"td_gaopin"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000136);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000137, CONVERT(UNHEX('E6B3B0E8BFAAE7868A') USING utf8mb4), 900100000118, 4, 'agentProcessTaidixiong', 'server/mark/agent/process/platform', '{"platformCode":"taidixiong"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000137);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000138, CONVERT(UNHEX('E6B3B0E8BFAAE4BA8CE6ACA1') USING utf8mb4), 900100000118, 5, 'agentProcessTdSecond', 'server/mark/agent/process/platform', '{"platformCode":"td_second"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000138);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000139, CONVERT(UNHEX('333630E9A696E6ACA1') USING utf8mb4), 900100000118, 6, 'agentProcessQihuFirst', 'server/mark/agent/process/platform', '{"platformCode":"qihu_first"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000139);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000140, CONVERT(UNHEX('333630E4BA8CE6ACA1') USING utf8mb4), 900100000118, 7, 'agentProcessQihuSecond', 'server/mark/agent/process/platform', '{"platformCode":"qihu_second"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000140);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000141, CONVERT(UNHEX('333630') USING utf8mb4), 900100000118, 8, 'agentProcessSanliuling', 'server/mark/agent/process/platform', '{"platformCode":"sanliuling"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000141);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000142, CONVERT(UNHEX('E885BEE8AEAF') USING utf8mb4), 900100000118, 9, 'agentProcessTencent', 'server/mark/agent/process/platform', '{"platformCode":"tencent_mark"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000142);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000143, CONVERT(UNHEX('E885BEE8AEAF') USING utf8mb4), 900100000118, 10, 'agentProcessTengxun', 'server/mark/agent/process/platform', '{"platformCode":"tengxun"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000143);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000144, CONVERT(UNHEX('E794B5E8AF9DE982A6') USING utf8mb4), 900100000118, 11, 'agentProcessDianhuabang', 'server/mark/agent/process/platform', '{"platformCode":"dianhuabang"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000144);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000145, CONVERT(UNHEX('E799BEE5BAA6') USING utf8mb4), 900100000118, 12, 'agentProcessBaidu', 'server/mark/agent/process/platform', '{"platformCode":"baidu"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000145);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000146, CONVERT(UNHEX('E6909CE78B97') USING utf8mb4), 900100000118, 13, 'agentProcessSghmt', 'server/mark/agent/process/platform', '{"platformCode":"sghmt"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000146);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000147, CONVERT(UNHEX('E5B08FE7B1B3E6898BE69CBA') USING utf8mb4), 900100000118, 14, 'agentProcessXiaomi', 'server/mark/agent/process/platform', '{"platformCode":"xiaomi"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000147);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) SELECT 900100000148, CONVERT(UNHEX('E88194E9809AE7AEA1E5AEB6') USING utf8mb4), 900100000118, 15, 'agentProcessLtgj', 'server/mark/agent/process/platform', '{"platformCode":"ltgj"}', '', 1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000148);
INSERT INTO sys_role_menu (role_id, menu_id) SELECT r.role_id, m.menu_id FROM sys_role r JOIN (SELECT 900100000134 AS menu_id UNION ALL SELECT 900100000135 AS menu_id UNION ALL SELECT 900100000136 AS menu_id UNION ALL SELECT 900100000137 AS menu_id UNION ALL SELECT 900100000138 AS menu_id UNION ALL SELECT 900100000139 AS menu_id UNION ALL SELECT 900100000140 AS menu_id UNION ALL SELECT 900100000141 AS menu_id UNION ALL SELECT 900100000142 AS menu_id UNION ALL SELECT 900100000143 AS menu_id UNION ALL SELECT 900100000144 AS menu_id UNION ALL SELECT 900100000145 AS menu_id UNION ALL SELECT 900100000146 AS menu_id UNION ALL SELECT 900100000147 AS menu_id UNION ALL SELECT 900100000148 AS menu_id) m LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id WHERE r.role_key IN ('agent', 'mark_agent') AND rm.role_id IS NULL;
SET FOREIGN_KEY_CHECKS = 1;

-- ===== END m22_agent_process_platform_menus.sql =====


-- ===== BEGIN m23_sync_user_orders_to_agent.sql =====
-- Sync user-submitted orders to agent processing queue.
UPDATE mark_order mo
INNER JOIN sys_user su ON su.user_id = mo.user_id
SET mo.audit_status = '1',
    mo.order_status = '1',
    mo.audit_opinion = CONVERT(UNHEX('E794A8E688B7E68F90E4BAA4E887AAE58AA8E5AEA1E6A0B8') USING utf8mb4),
    mo.audit_time = NOW(),
    mo.audit_by = IFNULL(su.create_by, 'system'),
    mo.assigned_agent_id = (
        SELECT agent.user_id
        FROM sys_user agent
        INNER JOIN sys_user_role aur ON aur.user_id = agent.user_id
        INNER JOIN sys_role ar ON ar.role_id = aur.role_id AND ar.role_key IN ('agent', 'mark_agent')
        WHERE agent.del_flag = '0'
          AND agent.rel_mark_template = su.rel_mark_template
        ORDER BY agent.user_id ASC
        LIMIT 1
    ),
    mo.update_time = NOW()
WHERE mo.audit_status = '0';

UPDATE mark_order mo
INNER JOIN sys_user su ON su.user_id = mo.user_id
SET mo.assigned_agent_id = (
        SELECT agent.user_id
        FROM sys_user agent
        INNER JOIN sys_user_role aur ON aur.user_id = agent.user_id
        INNER JOIN sys_role ar ON ar.role_id = aur.role_id AND ar.role_key IN ('agent', 'mark_agent')
        WHERE agent.del_flag = '0'
          AND agent.rel_mark_template = su.rel_mark_template
        ORDER BY agent.user_id ASC
        LIMIT 1
    ),
    mo.update_time = NOW()
WHERE mo.platform_code = 'td_gaopin'
  AND (mo.assigned_agent_id IS NULL OR mo.assigned_agent_id = 1);

-- ===== END m23_sync_user_orders_to_agent.sql =====


-- ===== BEGIN m24_agent_mark_template_manage.sql =====
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M24：开放代理端标记模板创建与编辑
-- 1) 为 agent / mark_agent 绑定标记模板菜单与功能点
-- 2) 代理仅可管理本人 owner_user_id 下的模板（服务层已做范围限制）
-- =========================================================

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
WHERE r.role_key IN ('agent', 'mark_agent')
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- ===== END m24_agent_mark_template_manage.sql =====


-- ===== BEGIN m25_agent_user_edit_permission.sql =====
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M25：代理端用户修改权限
-- 代理需要 system:user:edit 才能绑定模板、修改下游账号等
-- =========================================================

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN (
  SELECT 1002 AS menu_id
) m
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key IN ('agent', 'mark_agent')
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- ===== END m25_agent_user_edit_permission.sql =====


-- ===== BEGIN m26_tencent_dedicated_page.sql =====
SET NAMES utf8mb4;

-- M26：腾讯用户端独立页面（方案 B）
UPDATE `sys_menu`
SET `component` = 'server/mark/user/tencent',
    `route_name` = 'MarkUserTencent',
    `is_cache` = '0',
    `remark` = CONCAT(IFNULL(`remark`, ''), ' | M26-腾讯独立页面')
WHERE `menu_id` = 900100000126;

-- ===== END m26_tencent_dedicated_page.sql =====


-- ===== BEGIN m27_tencent_dedicated_replace_legacy.sql =====
SET NAMES utf8mb4;

-- M27: 腾讯用户端全面切换为专用页面，停用旧 index 批量流程
UPDATE `sys_menu`
SET `menu_name` = CONVERT(UNHEX('E885BEE8AEAF') USING utf8mb4),
    `component` = 'server/mark/user/tencent',
    `route_name` = 'MarkUserTencent',
    `path` = 'tencentMark',
    `query` = '',
    `is_cache` = '0',
    `status` = '0',
    `visible` = '0',
    `remark` = CONCAT(IFNULL(`remark`, ''), ' | M27-tencent-dedicated-replace-legacy')
WHERE `menu_id` = 900100000126;

-- ===== END m27_tencent_dedicated_replace_legacy.sql =====


-- ===== BEGIN m28_remove_duplicate_tencent_menu.sql =====
SET NAMES utf8mb4;

-- M28: 移除用户端旧 index 腾讯入口，仅保留专用页菜单 900100000126
UPDATE `sys_menu`
SET `menu_name` = CONVERT(UNHEX('E885BEE8AEAF') USING utf8mb4),
    `component` = 'server/mark/user/tencent',
    `route_name` = 'MarkUserTencent',
    `path` = 'tencentMark',
    `query` = '',
    `is_cache` = '0',
    `status` = '0',
    `visible` = '0',
    `remark` = CONCAT(IFNULL(`remark`, ''), ' | M28-remove-legacy-tencent-nav')
WHERE `menu_id` = 900100000126;

-- 若存在误插入的 index 腾讯菜单，则停用
UPDATE `sys_menu`
SET `status` = '1',
    `visible` = '1',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = CONCAT(IFNULL(`remark`, ''), ' | M28-disabled-legacy-index-tencent')
WHERE `parent_id` = 900100000001
  AND `menu_id` <> 900100000126
  AND `component` = 'server/mark/user/index'
  AND (
    `query` LIKE '%tencent_mark%'
    OR `query` LIKE '%tengxun%'
    OR `path` IN ('tencentMark', 'tengxunMark')
  );

-- ===== END m28_remove_duplicate_tencent_menu.sql =====


-- ===== BEGIN m29_fix_agent_order_sync.sql =====
-- Fix agent order sync: assign downstream orders to owning agent and auto-pass pending audit.
SET NAMES utf8mb4;

UPDATE mark_order mo
INNER JOIN sys_user su ON su.user_id = mo.user_id
INNER JOIN sys_user agent ON agent.user_name = su.create_by AND agent.del_flag = '0'
INNER JOIN sys_user_role aur ON aur.user_id = agent.user_id
INNER JOIN sys_role ar ON ar.role_id = aur.role_id AND ar.role_key IN ('agent', 'mark_agent')
SET mo.assigned_agent_id = agent.user_id,
    mo.update_time = NOW()
WHERE mo.assigned_agent_id IS NULL;

UPDATE mark_order mo
INNER JOIN sys_user su ON su.user_id = mo.user_id
INNER JOIN mark_platform_template mpt ON mpt.id = su.rel_mark_template AND mpt.status = '0'
INNER JOIN sys_user agent ON agent.user_id = mpt.owner_user_id AND agent.del_flag = '0'
INNER JOIN sys_user_role aur ON aur.user_id = agent.user_id
INNER JOIN sys_role ar ON ar.role_id = aur.role_id AND ar.role_key IN ('agent', 'mark_agent')
SET mo.assigned_agent_id = agent.user_id,
    mo.update_time = NOW()
WHERE mo.assigned_agent_id IS NULL;

UPDATE mark_order mo
INNER JOIN sys_user su ON su.user_id = mo.user_id
SET mo.assigned_agent_id = (
        SELECT agent.user_id
        FROM sys_user agent
        INNER JOIN sys_user_role aur ON aur.user_id = agent.user_id
        INNER JOIN sys_role ar ON ar.role_id = aur.role_id AND ar.role_key IN ('agent', 'mark_agent')
        WHERE agent.del_flag = '0'
          AND agent.rel_mark_template = su.rel_mark_template
        ORDER BY agent.user_id ASC
        LIMIT 1
    ),
    mo.update_time = NOW()
WHERE mo.assigned_agent_id IS NULL
  AND su.rel_mark_template IS NOT NULL
  AND su.rel_mark_template > 0;

UPDATE mark_order mo
INNER JOIN sys_user su ON su.user_id = mo.user_id
SET mo.audit_status = '1',
    mo.order_status = CASE WHEN mo.order_status = '0' THEN '1' ELSE mo.order_status END,
    mo.audit_opinion = CONVERT(UNHEX('E794A8E688B7E68F90E4BAA4E887AAE58AA8E5AEA1E6A0B8') USING utf8mb4),
    mo.audit_time = IFNULL(mo.audit_time, NOW()),
    mo.audit_by = IFNULL(mo.audit_by, IFNULL(su.create_by, 'system')),
    mo.update_time = NOW()
WHERE mo.audit_status = '0';

-- ===== END m29_fix_agent_order_sync.sql =====


-- ===== BEGIN m30_mark_menu_rename.sql =====
-- M30: rename mark root menu and keep remark in sync
UPDATE `sys_menu`
SET `menu_name` = '标记业务管理',
    `remark` = '标记业务管理目录',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 900100000001;

-- ===== END m30_mark_menu_rename.sql =====


-- ===== BEGIN m31_hide_user_notice_menu.sql =====
-- M31: hide "My Messages" from sidebar navigation (keep navbar bell + permissions)
UPDATE `sys_menu`
SET `visible` = '1',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 900100000112;

-- ===== END m31_hide_user_notice_menu.sql =====


-- ===== BEGIN m32_rename_user_wallet_menu.sql =====
-- M32: rename user wallet sidebar menu to consumption details
UPDATE `sys_menu`
SET `menu_name` = '消费明细',
    `remark` = '消费明细菜单',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 900100000102;

-- ===== END m32_rename_user_wallet_menu.sql =====


-- ===== BEGIN m33_tencent_display_name.sql =====
-- M33: unify Tencent platform display name to 腾讯速解 (menu + template JSON)
UPDATE `sys_menu`
SET `menu_name` = CONVERT(UNHEX('E885BEE8AEAFE9809FE8A7A3') USING utf8mb4),
    `remark` = '腾讯速解菜单',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 900100000126;

-- Align stored template platform names with sidebar label
UPDATE `mark_platform_template`
SET `template_info` = REPLACE(`template_info`, '"platformName":"腾讯"', '"platformName":"腾讯速解"'),
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `template_info` LIKE '%"platformName":"腾讯"%';

-- ===== END m33_tencent_display_name.sql =====


-- ===== BEGIN m34_user_order_detail_menu.sql =====
SET NAMES utf8mb4;

-- M34: user-side unified order detail page (all platforms)
INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000149,
       CONVERT(UNHEX('E8AEA2E58D95E8AFA6E68385') USING utf8mb4),
       900100000001,
       2,
       'userOrderDetail',
       'server/mark/user/orderDetail',
       '',
       '',
       1, 0, 'C', '0', '0',
       'server:markUser:order:list',
       'documentation',
       'admin',
       NOW(),
       '用户端订单详情菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000149);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, 900100000149
FROM `sys_role` r
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = 900100000149
WHERE r.role_key IN ('admin', 'common', 'user', 'mark_user')
  AND rm.role_id IS NULL;

-- ===== END m34_user_order_detail_menu.sql =====


-- ===== BEGIN m35_agent_user_reset_pwd_permission.sql =====
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- M35: grant reset password permission to mark agents
-- menu 1006 -> system:user:resetPwd

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, 1006
FROM `sys_role` r
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = 1006
WHERE r.role_key IN ('agent', 'mark_agent')
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- ===== END m35_agent_user_reset_pwd_permission.sql =====


-- ===== BEGIN m36_agent_process_detail_menu.sql =====
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- M36: hide legacy agent secondary nav, add process detail page

UPDATE sys_menu
SET visible = '1',
    update_by = 'admin',
    update_time = NOW()
WHERE menu_id IN (900100000128, 900100000110);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 900100000149,
       CONVERT(UNHEX('E5A484E79086E8AFA6E68385') USING utf8mb4),
       900100000118, 93, 'agentProcessDetail', 'server/mark/agent/process/detail', '', '',
       1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'documentation', 'admin', NOW(), 'M36-agent-process-detail'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000149);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 900100000149
FROM sys_role r
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = 900100000149
WHERE r.role_key IN ('agent', 'mark_agent')
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- ===== END m36_agent_process_detail_menu.sql =====


-- ===== BEGIN m37_agent_process_overview_menu.sql =====
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- M37: agent downstream page becomes processed-order overview

UPDATE sys_menu
SET menu_name = CONVERT(UNHEX('E5A484E79086E680BBE8A788') USING utf8mb4),
    update_by = 'admin',
    update_time = NOW(),
    remark = 'M37-agent-process-overview'
WHERE menu_id = 900100000119;

UPDATE sys_menu
SET visible = '1',
    update_by = 'admin',
    update_time = NOW()
WHERE menu_id = 900100000149;

SET FOREIGN_KEY_CHECKS = 1;

-- ===== END m37_agent_process_overview_menu.sql =====


-- ===== BEGIN m38_xiaomi_dedicated_page.sql =====
SET NAMES utf8mb4;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT
  900100000150,
  '小米手机',
  900100000001,
  10,
  'xiaomiMark',
  'server/mark/user/xiaomi',
  '',
  'MarkUserXiaomi',
  1, 0, 'C', '1', '0',
  'server:markUser:order:clear',
  'phone',
  'admin',
  NOW(),
  'M38-xiaomi-dedicated-page'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000150);

-- ��??????��????????????/???????????????????????????
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 900100000150
FROM sys_role r
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = 900100000150
WHERE r.role_key IN ('mark_user', 'common', 'admin')
  AND rm.role_id IS NULL;

-- ===== END m38_xiaomi_dedicated_page.sql =====


-- ===== BEGIN m39_xiaomi_hide_user_nav.sql =====
SET NAMES utf8mb4;

-- Deprecated: superseded by M40 (template-driven user nav). Kept as no-op for idempotent runs.
SELECT 1;

-- ===== END m39_xiaomi_hide_user_nav.sql =====


-- ===== BEGIN m40_template_driven_user_nav.sql =====
SET NAMES utf8mb4;

-- M40: user nav is template-driven; static dedicated menus stay as route templates only (sidebar hidden)
UPDATE sys_menu
SET visible = '1',
    remark = CONCAT(IFNULL(remark, ''), ' | M40-template-driven-nav')
WHERE menu_id IN (900100000126, 900100000150);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 900100000150
FROM sys_role r
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = 900100000150
WHERE r.role_key IN ('mark_user', 'common')
  AND rm.role_id IS NULL;

-- ===== END m40_template_driven_user_nav.sql =====


-- ===== BEGIN m41_xiaomi_all_platform_template.sql =====
SET NAMES utf8mb4;

-- M41: ensure "全平台模版" includes xiaomi (restore if removed by M39)
UPDATE mark_platform_template
SET template_info = CONCAT(
        TRIM(TRAILING ']' FROM TRIM(template_info)),
        ',{"platformCode":"xiaomi","platformName":"小米手机","unitPrice":1}]'
    ),
    update_by = 'admin',
    update_time = NOW()
WHERE status = '0'
  AND template_name LIKE '%全平台%'
  AND IFNULL(template_info, '') <> ''
  AND template_info NOT LIKE '%"platformCode":"xiaomi"%';

-- Ensure xiaomi dedicated route exists and is available for template-driven nav injection
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT
  900100000150,
  CONVERT(UNHEX('E5B08FE7B1B3E6898BE69CBA') USING utf8mb4),
  900100000001,
  10,
  'xiaomiMark',
  'server/mark/user/xiaomi',
  '',
  'MarkUserXiaomi',
  1, 0, 'C', '1', '0',
  'server:markUser:order:clear',
  'phone',
  'admin',
  NOW(),
  'M41-xiaomi-all-platform-template'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000150);

UPDATE sys_menu
SET visible = '1',
    component = 'server/mark/user/xiaomi',
    route_name = 'MarkUserXiaomi',
    status = '0',
    remark = CONCAT(IFNULL(remark, ''), ' | M41-xiaomi-all-platform-template')
WHERE menu_id = 900100000150;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 900100000150
FROM sys_role r
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = 900100000150
WHERE r.role_key IN ('mark_user', 'common', 'admin')
  AND rm.role_id IS NULL;

-- ===== END m41_xiaomi_all_platform_template.sql =====


-- ===== BEGIN m42_xiaomi_process_status_3.sql =====
-- 小米订单明细处理状态扩展：3=处理中(代理已手动提交)
ALTER TABLE mark_order_item
    MODIFY COLUMN process_status char(1) NOT NULL DEFAULT '0'
        COMMENT '处理状态（0待处理 1成功 2失败 3处理中/已手动提交）';

-- ===== END m42_xiaomi_process_status_3.sql =====


-- ===== BEGIN m43_platform_code_lowercase.sql =====
-- M43: normalize mark platform codes to lowercase and merge duplicate quota rows

SET NAMES utf8mb4;

UPDATE mark_user_platform_quota
SET platform_code = LOWER(TRIM(platform_code))
WHERE BINARY platform_code <> LOWER(TRIM(platform_code));

UPDATE mark_user_platform_price
SET platform_code = LOWER(TRIM(platform_code))
WHERE BINARY platform_code <> LOWER(TRIM(platform_code));

-- Merge duplicate quota rows created by mixed-case platform codes
DELETE q1
FROM mark_user_platform_quota q1
INNER JOIN mark_user_platform_quota q2
    ON q1.user_id = q2.user_id
   AND q1.platform_code = q2.platform_code
   AND q1.id > q2.id;

UPDATE mark_platform_template
SET template_info = REPLACE(
        REPLACE(template_info, '"platformCode":"Baidu"', '"platformCode":"baidu"'),
        '"platformCode":"Xiaomi"', '"platformCode":"xiaomi"'
    )
WHERE template_info LIKE '%"platformCode":"Baidu"%'
   OR template_info LIKE '%"platformCode":"Xiaomi"%';

-- ===== END m43_platform_code_lowercase.sql =====


-- ===== BEGIN m44_baidu_dedicated_page.sql =====
SET NAMES utf8mb4;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT
  900100000151,
  '百度',
  900100000001,
  11,
  'baiduMark',
  'server/mark/user/baidu',
  '',
  'MarkUserBaidu',
  1, 0, 'C', '1', '0',
  'server:markUser:order:clear',
  'phone',
  'admin',
  NOW(),
  'M44-baidu-dedicated-page'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000151);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 900100000151
FROM sys_role r
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = 900100000151
WHERE r.role_key IN ('mark_user', 'common', 'admin')
  AND rm.role_id IS NULL;

UPDATE sys_menu
SET
  component = 'server/mark/user/baidu',
  path = 'baiduMark',
  route_name = 'MarkUserBaidu',
  perms = 'server:markUser:order:clear',
  visible = '1',
  remark = 'M44-baidu-dedicated-page'
WHERE menu_id = 900100000151;

UPDATE sys_menu
SET
  component = 'server/mark/user/baidu',
  path = 'baiduMark',
  route_name = 'MarkUserBaidu',
  perms = 'server:markUser:order:clear',
  visible = '1'
WHERE parent_id = 900100000001
  AND CASE
        WHEN JSON_VALID(query) THEN LOWER(JSON_UNQUOTE(JSON_EXTRACT(query, '$.platformCode')))
        ELSE NULL
      END = 'baidu'
  AND component = 'server/mark/user/index'
  AND menu_id <> 900100000151;

-- ===== END m44_baidu_dedicated_page.sql =====


-- ===== BEGIN m45_qihu360_dedicated_page.sql =====
SET NAMES utf8mb4;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT
  900100000152,
  CONVERT(UNHEX('333630E68F90E58F91') USING utf8mb4),
  900100000001,
  12,
  'sanliulingMark',
  'server/mark/user/qihu360',
  '{"platformCode":"sanliuling","platformName":"360"}',
  'MarkUser__sanliuling',
  1, 0, 'C', '1', '0',
  'server:markUser:order:clear',
  'phone',
  'admin',
  NOW(),
  'M45-qihu360-dedicated-page'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000152);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 900100000152
FROM sys_role r
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = 900100000152
WHERE r.role_key IN ('mark_user', 'common', 'admin')
  AND rm.role_id IS NULL;

UPDATE sys_menu
SET
  component = 'server/mark/user/qihu360',
  perms = 'server:markUser:order:clear',
  visible = '1',
  remark = 'M45-qihu360-dedicated-page'
WHERE menu_id IN (900100000123, 900100000124, 900100000152)
   OR (
     parent_id = 900100000001
     AND CASE
           WHEN JSON_VALID(query) THEN LOWER(JSON_UNQUOTE(JSON_EXTRACT(query, '$.platformCode')))
           ELSE NULL
         END IN ('sanliuling', '360', 'qihu_first', 'qihu_second')
     AND component = 'server/mark/user/index'
   );

UPDATE sys_menu
SET
  path = 'qihuFirstMark',
  component = 'server/mark/user/qihu360',
  query = '{"platformCode":"qihu_first","platformName":"360首次"}',
  route_name = 'MarkUser__qihu_first',
  perms = 'server:markUser:order:clear',
  visible = '1'
WHERE menu_id = 900100000123;

UPDATE sys_menu
SET
  path = 'qihuSecondMark',
  component = 'server/mark/user/qihu360',
  query = '{"platformCode":"qihu_second","platformName":"360二次"}',
  route_name = 'MarkUser__qihu_second',
  perms = 'server:markUser:order:clear',
  visible = '1'
WHERE menu_id = 900100000124;

UPDATE sys_menu
SET
  path = 'sanliulingMark',
  component = 'server/mark/user/qihu360',
  query = '{"platformCode":"sanliuling","platformName":"360"}',
  route_name = 'MarkUser__sanliuling',
  perms = 'server:markUser:order:clear',
  visible = '1'
WHERE menu_id = 900100000152;

-- ===== END m45_qihu360_dedicated_page.sql =====


-- ===== BEGIN m46_fix_english_all_platform_codes.sql =====
SET NAMES utf8mb4;

-- M46: fix English "ȫƽ̨" template invalid platform codes
-- Sougou -> sghmt, LiantongGuanjia -> ltgj
-- Also rename any existing quota/price rows using the wrong codes.

-- 1) Fix template id=4 (Ӣ�İ�ȫƽ̨) and any other templates with the same bad codes
UPDATE mark_platform_template
SET template_info = REPLACE(
        REPLACE(
            REPLACE(
                REPLACE(template_info,
                    '"platformCode":"Sougou"', '"platformCode":"sghmt"'),
                '"platformCode":"sougou"', '"platformCode":"sghmt"'),
            '"platformCode":"LiantongGuanjia"', '"platformCode":"ltgj"'),
        '"platformCode":"liantongguanjia"', '"platformCode":"ltgj"'),
    update_by = 'admin',
    update_time = NOW()
WHERE status = '0'
  AND (
    template_info LIKE '%"platformCode":"Sougou"%'
    OR template_info LIKE '%"platformCode":"sougou"%'
    OR template_info LIKE '%"platformCode":"LiantongGuanjia"%'
    OR template_info LIKE '%"platformCode":"liantongguanjia"%'
  );

-- 2) Rename quota rows: wrong code -> correct code (skip if target already exists)
UPDATE mark_user_platform_quota q
LEFT JOIN mark_user_platform_quota exist
  ON exist.user_id = q.user_id
 AND exist.platform_code = 'sghmt'
SET q.platform_code = 'sghmt',
    q.platform_name = CASE
      WHEN q.platform_name IN ('Sougou', 'sougou') THEN 'Sougou'
      ELSE q.platform_name
    END,
    q.update_by = 'admin',
    q.update_time = NOW()
WHERE LOWER(q.platform_code) = 'sougou'
  AND exist.id IS NULL;

UPDATE mark_user_platform_quota q
LEFT JOIN mark_user_platform_quota exist
  ON exist.user_id = q.user_id
 AND exist.platform_code = 'ltgj'
SET q.platform_code = 'ltgj',
    q.platform_name = CASE
      WHEN q.platform_name IN ('LiantongGuanjia', 'liantongguanjia') THEN 'LiantongGuanjia'
      ELSE q.platform_name
    END,
    q.update_by = 'admin',
    q.update_time = NOW()
WHERE LOWER(q.platform_code) = 'liantongguanjia'
  AND exist.id IS NULL;

-- Drop leftover wrong-code quota rows if correct-code row already exists
DELETE q
FROM mark_user_platform_quota q
INNER JOIN mark_user_platform_quota exist
  ON exist.user_id = q.user_id
 AND exist.platform_code = 'sghmt'
WHERE LOWER(q.platform_code) = 'sougou';

DELETE q
FROM mark_user_platform_quota q
INNER JOIN mark_user_platform_quota exist
  ON exist.user_id = q.user_id
 AND exist.platform_code = 'ltgj'
WHERE LOWER(q.platform_code) = 'liantongguanjia';

-- 3) Rename price rows the same way
UPDATE mark_user_platform_price p
LEFT JOIN mark_user_platform_price exist
  ON exist.user_id = p.user_id
 AND exist.platform_code = 'sghmt'
SET p.platform_code = 'sghmt',
    p.platform_name = CASE
      WHEN p.platform_name IN ('Sougou', 'sougou') THEN 'Sougou'
      ELSE p.platform_name
    END,
    p.update_by = 'admin',
    p.update_time = NOW()
WHERE LOWER(p.platform_code) = 'sougou'
  AND exist.id IS NULL;

UPDATE mark_user_platform_price p
LEFT JOIN mark_user_platform_price exist
  ON exist.user_id = p.user_id
 AND exist.platform_code = 'ltgj'
SET p.platform_code = 'ltgj',
    p.platform_name = CASE
      WHEN p.platform_name IN ('LiantongGuanjia', 'liantongguanjia') THEN 'LiantongGuanjia'
      ELSE p.platform_name
    END,
    p.update_by = 'admin',
    p.update_time = NOW()
WHERE LOWER(p.platform_code) = 'liantongguanjia'
  AND exist.id IS NULL;

DELETE p
FROM mark_user_platform_price p
INNER JOIN mark_user_platform_price exist
  ON exist.user_id = p.user_id
 AND exist.platform_code = 'sghmt'
WHERE LOWER(p.platform_code) = 'sougou';

DELETE p
FROM mark_user_platform_price p
INNER JOIN mark_user_platform_price exist
  ON exist.user_id = p.user_id
 AND exist.platform_code = 'ltgj'
WHERE LOWER(p.platform_code) = 'liantongguanjia';

-- ===== END m46_fix_english_all_platform_codes.sql =====


SET FOREIGN_KEY_CHECKS = 1;
