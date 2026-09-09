-- Minimal migration for the mark user/agent workflow.
-- Scope: user submission for 9 brands and agent processing only.
-- This script is idempotent and intentionally does not create audit,
-- governance, arbitration, downstream-account, wallet-management, or
-- template-management menus.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS mark_order (
  id bigint NOT NULL AUTO_INCREMENT,
  order_no varchar(64) NOT NULL,
  request_no varchar(64) DEFAULT NULL,
  user_id bigint NOT NULL,
  assigned_agent_id bigint DEFAULT NULL,
  platform_code varchar(64) NOT NULL,
  platform_name varchar(128) DEFAULT NULL,
  total_count int NOT NULL DEFAULT 0,
  success_count int NOT NULL DEFAULT 0,
  failed_count int NOT NULL DEFAULT 0,
  total_amount bigint NOT NULL DEFAULT 0,
  refund_amount bigint NOT NULL DEFAULT 0,
  order_status char(1) NOT NULL DEFAULT '0',
  audit_status char(1) NOT NULL DEFAULT '0',
  audit_opinion varchar(500) DEFAULT NULL,
  audit_by varchar(64) DEFAULT NULL,
  audit_time datetime DEFAULT NULL,
  completed_time datetime DEFAULT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  user_remark varchar(500) DEFAULT NULL COMMENT '用户自定义备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_mark_order_order_no (order_no),
  UNIQUE KEY uk_mark_order_user_request (user_id, request_no),
  KEY idx_mark_order_user_id (user_id),
  KEY idx_mark_order_assigned_agent_id (assigned_agent_id),
  KEY idx_mark_order_order_status (order_status),
  KEY idx_mark_order_platform_code (platform_code),
  KEY idx_mark_order_create_time (create_time),
  KEY idx_mark_order_audit_status (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标记订单';

CREATE TABLE IF NOT EXISTS mark_order_item (
  id bigint NOT NULL AUTO_INCREMENT,
  order_id bigint NOT NULL,
  phone varchar(32) NOT NULL,
  unit_price bigint NOT NULL DEFAULT 0,
  item_amount bigint NOT NULL DEFAULT 0,
  process_status char(1) NOT NULL DEFAULT '0',
  process_result varchar(255) DEFAULT NULL,
  process_note varchar(500) DEFAULT NULL,
  processed_by varchar(64) DEFAULT NULL,
  processed_time datetime DEFAULT NULL,
  refunded char(1) NOT NULL DEFAULT '0',
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  user_remark varchar(500) DEFAULT NULL COMMENT '用户自定义备注',
  PRIMARY KEY (id),
  KEY idx_mark_order_item_order_id (order_id),
  KEY idx_mark_order_item_phone (phone),
  KEY idx_mark_order_item_process_status (process_status),
  KEY idx_mark_order_item_order_status (order_id, process_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标记订单明细';

CREATE TABLE IF NOT EXISTS mark_wallet_log (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  order_id bigint DEFAULT NULL,
  order_item_id bigint DEFAULT NULL,
  platform_code varchar(64) DEFAULT NULL,
  platform_name varchar(128) DEFAULT NULL,
  biz_type varchar(32) NOT NULL,
  change_amount bigint NOT NULL,
  balance_before bigint NOT NULL,
  balance_after bigint NOT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_mark_wallet_log_user_id (user_id),
  KEY idx_mark_wallet_log_order_id (order_id),
  KEY idx_mark_wallet_log_order_item_id (order_item_id),
  KEY idx_mark_wallet_log_biz_type (biz_type),
  KEY idx_mark_wallet_log_create_time (create_time),
  KEY idx_mark_wallet_log_platform_code (platform_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标记钱包流水';

CREATE TABLE IF NOT EXISTS mark_user_platform_price (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  platform_code varchar(64) NOT NULL,
  platform_name varchar(128) DEFAULT NULL,
  unit_price bigint NOT NULL DEFAULT 1,
  status char(1) NOT NULL DEFAULT '0',
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mark_user_platform_price (user_id, platform_code),
  KEY idx_mark_user_platform_price_platform_code (platform_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户平台单价';

CREATE TABLE IF NOT EXISTS mark_user_platform_quota (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  platform_code varchar(64) NOT NULL,
  platform_name varchar(128) DEFAULT NULL,
  remain_count bigint NOT NULL DEFAULT 0,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mark_user_platform_quota (user_id, platform_code),
  KEY idx_mark_user_platform_quota_platform_code (platform_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户平台余量';

-- Kept as an internal compatibility table because order notices and legacy
-- ownership checks still reference it. There is no menu for managing it.
CREATE TABLE IF NOT EXISTS mark_platform_template (
  id bigint NOT NULL AUTO_INCREMENT,
  template_name varchar(100) NOT NULL,
  template_info text,
  status char(1) NOT NULL DEFAULT '0',
  is_default char(1) NOT NULL DEFAULT '0',
  owner_user_id bigint NOT NULL DEFAULT 0,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mark_platform_template_owner_name (owner_user_id, template_name),
  KEY idx_mark_platform_template_status (status),
  KEY idx_mark_platform_template_owner_user_id (owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标记内部平台配置';

CREATE TABLE IF NOT EXISTS mark_user_notice (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  notice_type varchar(32) NOT NULL DEFAULT 'ORDER_AUDIT',
  title varchar(128) NOT NULL,
  content varchar(500) DEFAULT NULL,
  biz_type varchar(32) DEFAULT NULL,
  biz_id bigint DEFAULT NULL,
  read_flag char(1) NOT NULL DEFAULT '0',
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_mark_user_notice_user_read (user_id, read_flag),
  KEY idx_mark_user_notice_user_time (user_id, create_time),
  KEY idx_mark_user_notice_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标记用户通知';

SET @has_mark_template_col := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'rel_mark_template'
);
SET @mark_template_alter := IF(@has_mark_template_col = 0,
  'ALTER TABLE sys_user ADD COLUMN rel_mark_template bigint DEFAULT NULL COMMENT ''标记内部配置ID''',
  'SELECT 1');
PREPARE mark_template_stmt FROM @mark_template_alter;
EXECUTE mark_template_stmt;
DEALLOCATE PREPARE mark_template_stmt;

INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly,
  status, del_flag, create_by, create_time, remark)
SELECT '标记用户', 'mark_user', 30, '1', 1, 1, '0', '0', 'admin', NOW(), '最小标记用户角色'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'mark_user');
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly,
  status, del_flag, create_by, create_time, remark)
SELECT '标记代理', 'mark_agent', 31, '1', 1, 1, '0', '0', 'admin', NOW(), '最小标记代理角色'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'mark_agent');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 900100000001, '标记业务管理', 0, 8, 'mark', '', '', '', 1, 0, 'M', '0', '0', '', 'guide', 'admin', NOW(), '最小标记业务入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000001);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 900100000103, '代理处理', 900100000001, 1, 'agentOrder', '', '', 'MarkAgentOrder',
  1, 0, 'M', '0', '0', '', 'edit', 'admin', NOW(), '9平台代理处理目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000103);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 900100000101, '用户标记', 900100000001, 2, 'userOrder', 'server/mark/user/index', '', 'MarkUserOrder',
  1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), '用户标记入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000101);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 900100000102, '用户端', 900100000001, 2, 'userClient', '', '', 'MarkUserClient',
  1, 0, 'M', '0', '0', '', 'user', 'admin', NOW(), '用户端9平台目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000102);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 900100000201 + p.order_num, p.menu_name, 900100000001, p.order_num, p.path, p.component, p.query, p.route_name,
  1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), '用户9平台入口'
FROM (
  SELECT '泰迪熊' menu_name, 3 order_num, 'taidixiong' path, 'server/mark/user/index' component, '{"platformCode":"taidixiong"}' query, 'MarkUserTaidixiong' route_name
  UNION ALL SELECT '腾讯', 4, 'tengxun', 'server/mark/user/tencent', '{"platformCode":"tengxun","platformName":"腾讯"}', 'MarkUserTengxun'
  UNION ALL SELECT '360', 5, '360fugai', 'server/mark/user/qihu360', '{"platformCode":"360fugai","platformName":"360"}', 'MarkUser360'
  UNION ALL SELECT '百度', 6, 'baidu', 'server/mark/user/baidu', '{"platformCode":"baidu","platformName":"百度"}', 'MarkUserBaidu'
  UNION ALL SELECT '搜狗', 7, 'sogou', 'server/mark/user/xiaomi', '{"platformCode":"sogou","platformName":"搜狗"}', 'MarkUserSogou'
  UNION ALL SELECT '移动高频', 8, 'yidonggaopin', 'server/mark/user/xiaomi', '{"platformCode":"yidonggaopin","platformName":"移动高频"}', 'MarkUserYidong'
  UNION ALL SELECT '电话邦', 9, 'dianhuabang', 'server/mark/user/xiaomi', '{"platformCode":"dianhuabang","platformName":"电话邦"}', 'MarkUserDianhuabang'
  UNION ALL SELECT '联通管家', 10, 'ltgj', 'server/mark/user/xiaomi', '{"platformCode":"ltgj","platformName":"联通管家"}', 'MarkUserLtgj'
  UNION ALL SELECT '小米手机', 11, 'xiaomi', 'server/mark/user/xiaomi', '{"platformCode":"xiaomi","platformName":"小米手机"}', 'MarkUserXiaomi'
) p
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000201 + p.order_num);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 900100000301 + p.order_num, p.menu_name, 900100000103, p.order_num, p.path,
  'server/mark/agent/process/platform', p.query, p.route_name,
  1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), '代理9平台处理入口'
FROM (
  SELECT '泰迪熊' menu_name, 1 order_num, 'agentTaidixiong' path, '{"platformCodes":"taidixiong,td_gaopin,td_second","platformName":"泰迪熊"}' query, 'MarkAgentTaidixiong' route_name
  UNION ALL SELECT '腾讯', 2, 'agentTengxun', '{"platformCode":"tengxun","platformName":"腾讯"}', 'MarkAgentTengxun'
  UNION ALL SELECT '360', 3, 'agent360', '{"platformCode":"360fugai","platformName":"360"}', 'MarkAgent360'
  UNION ALL SELECT '百度', 4, 'agentBaidu', '{"platformCode":"baidu","platformName":"百度"}', 'MarkAgentBaidu'
  UNION ALL SELECT '搜狗', 5, 'agentSogou', '{"platformCode":"sogou","platformName":"搜狗"}', 'MarkAgentSogou'
  UNION ALL SELECT '移动高频', 6, 'agentYidong', '{"platformCode":"yidonggaopin","platformName":"移动高频"}', 'MarkAgentYidong'
  UNION ALL SELECT '电话邦', 7, 'agentDianhuabang', '{"platformCode":"dianhuabang","platformName":"电话邦"}', 'MarkAgentDianhuabang'
  UNION ALL SELECT '联通管家', 8, 'agentLtgj', '{"platformCode":"ltgj","platformName":"联通管家"}', 'MarkAgentLtgj'
  UNION ALL SELECT '小米手机', 9, 'agentXiaomi', '{"platformCode":"xiaomi","platformName":"小米手机"}', 'MarkAgentXiaomi'
) p
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000301 + p.order_num);

UPDATE sys_menu SET menu_name = '标记业务管理', path = 'mark', component = '', menu_type = 'M', perms = '', visible = '0', status = '0'
WHERE menu_id = 900100000001;
UPDATE sys_menu SET menu_name = '代理处理', component = '', menu_type = 'M', perms = '', visible = '0', status = '0'
WHERE menu_id = 900100000103;
UPDATE sys_menu SET menu_name = '用户标记', component = 'server/mark/user/index', menu_type = 'C', perms = 'server:markUser:order:list', visible = '1', status = '0'
WHERE menu_id = 900100000101;
UPDATE sys_menu SET menu_name = '用户端', parent_id = 900100000001, order_num = 2, path = 'userClient',
  component = '', menu_type = 'M', perms = '', visible = '0', status = '0', icon = 'user'
WHERE menu_id = 900100000102;
UPDATE sys_menu SET parent_id = 900100000102
WHERE menu_id BETWEEN 900100000204 AND 900100000212;
UPDATE sys_menu SET menu_name = '搜狗', path = 'sogou',
  query = '{"platformCode":"sogou","platformName":"搜狗"}', route_name = 'MarkUserSogou'
WHERE menu_id = 900100000208;
UPDATE sys_menu SET menu_name = '360', path = '360fugai',
  query = '{"platformCode":"360fugai","platformName":"360"}', route_name = 'MarkUser360'
WHERE menu_id = 900100000206;

-- Agent processing exposes every internal platform code as an independent
-- menu. User-facing menus remain grouped as the original nine brands.
UPDATE sys_menu SET order_num = 1, menu_name = '泰迪熊', path = 'agentTaidixiong',
  query = '{"platformCode":"taidixiong","platformName":"泰迪熊"}', route_name = 'MarkAgentTaidixiong'
WHERE menu_id = 900100000302;
UPDATE sys_menu SET order_num = 4, menu_name = '腾讯', path = 'agentTengxun',
  query = '{"platformCode":"tengxun","platformName":"腾讯"}', route_name = 'MarkAgentTengxun'
WHERE menu_id = 900100000303;
UPDATE sys_menu SET order_num = 5, menu_name = '360', path = 'agent360',
  query = '{"platformCode":"360fugai","platformName":"360"}', route_name = 'MarkAgent360'
WHERE menu_id = 900100000304;
UPDATE sys_menu SET order_num = 6, menu_name = '360二次', path = 'agentQihuSecond',
  query = '{"platformCode":"qihu_second","platformName":"360二次"}', route_name = 'MarkAgentQihuSecond'
WHERE menu_id = 900100000314;
UPDATE sys_menu SET order_num = 7 WHERE menu_id = 900100000305;
UPDATE sys_menu SET order_num = 8 WHERE menu_id = 900100000306;
UPDATE sys_menu SET menu_name = '搜狗', path = 'agentSogou',
  query = '{"platformCode":"sogou","platformName":"搜狗"}', route_name = 'MarkAgentSogou'
WHERE menu_id = 900100000306;
UPDATE sys_menu SET order_num = 9 WHERE menu_id = 900100000307;
UPDATE sys_menu SET order_num = 10 WHERE menu_id = 900100000308;
UPDATE sys_menu SET order_num = 11 WHERE menu_id = 900100000309;
UPDATE sys_menu SET order_num = 12 WHERE menu_id = 900100000310;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT p.menu_id, p.menu_name, 900100000103, p.order_num, p.path,
  'server/mark/agent/process/platform', p.query, p.route_name,
  1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), '代理内部平台独立处理入口'
FROM (
  SELECT 900100000311 menu_id, '泰迪高频' menu_name, 2 order_num, 'agentTdGaopin' path,
    '{"platformCode":"td_gaopin","platformName":"泰迪高频"}' query, 'MarkAgentTdGaopin' route_name
  UNION ALL SELECT 900100000312, '泰迪二次', 3, 'agentTdSecond',
    '{"platformCode":"td_second","platformName":"泰迪二次"}', 'MarkAgentTdSecond'
  UNION ALL SELECT 900100000314, '360二次', 6, 'agentQihuSecond',
    '{"platformCode":"qihu_second","platformName":"360二次"}', 'MarkAgentQihuSecond'
) p
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = p.menu_id);

DELETE FROM sys_role_menu WHERE menu_id = 900100000313;
DELETE FROM sys_menu WHERE menu_id = 900100000313;
DELETE FROM mark_user_platform_price WHERE platform_code = 'qihu_first';
DELETE FROM mark_user_platform_quota WHERE platform_code = 'qihu_first';

UPDATE mark_order SET platform_code = 'sogou' WHERE platform_code = 'sghmt';
UPDATE mark_user_platform_price SET platform_code = 'sogou' WHERE platform_code = 'sghmt';
UPDATE mark_user_platform_quota SET platform_code = 'sogou' WHERE platform_code = 'sghmt';
UPDATE mark_wallet_log SET platform_code = 'sogou' WHERE platform_code = 'sghmt';
UPDATE mark_order SET platform_code = '360fugai' WHERE platform_code = 'sanliuling';
UPDATE mark_user_platform_price SET platform_code = '360fugai' WHERE platform_code = 'sanliuling';
UPDATE mark_user_platform_quota SET platform_code = '360fugai' WHERE platform_code = 'sanliuling';
UPDATE mark_wallet_log SET platform_code = '360fugai' WHERE platform_code = 'sanliuling';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id BETWEEN 900100000311 AND 900100000314
WHERE r.role_key IN ('admin', 'agent', 'mark_agent');

-- Permissions used by the two migrated interfaces.
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 900100001000 + p.n, p.label, p.parent_id, p.n, '#', '', '', '', 1, 0, 'F', '0', '0', p.perm, '#', 'admin', NOW()
FROM (
  SELECT 1 n, '用户查询' label, 900100000101 parent_id, 'server:markUser:order:query' perm
  UNION ALL SELECT 2, '用户新增', 900100000101, 'server:markUser:order:add'
  UNION ALL SELECT 3, '用户预查询', 900100000101, 'server:markUser:order:precheck'
  UNION ALL SELECT 4, '用户提交消除', 900100000101, 'server:markUser:order:clear'
  UNION ALL SELECT 5, '用户钱包', 900100000101, 'server:markUser:wallet:list'
  UNION ALL SELECT 6, '用户流水', 900100000101, 'server:markUser:wallet:log:list'
  UNION ALL SELECT 7, '用户单价', 900100000101, 'server:markUser:price:list'
  UNION ALL SELECT 8, '代理查询', 900100000103, 'server:markAgent:order:query'
  UNION ALL SELECT 9, '代理处理', 900100000103, 'server:markAgent:item:feedback'
) p
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100001000 + p.n);

-- Admin sees the two entry menus and all nine user-platform children.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r JOIN sys_menu m
  ON m.menu_id = 900100000001 OR m.menu_id = 900100000101 OR m.menu_id = 900100000103
  OR m.menu_id = 900100000102
  OR (m.parent_id = 900100000102 AND m.menu_id BETWEEN 900100000204 AND 900100000212)
  OR (m.parent_id = 900100000103 AND m.menu_id BETWEEN 900100000302 AND 900100000310)
  OR m.menu_id BETWEEN 900100001001 AND 900100001009
WHERE r.role_key = 'admin';

-- Role bindings are deliberately limited to user/agent workflow permissions.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r JOIN sys_menu m
  ON m.menu_id = 900100000001 OR m.menu_id = 900100000101 OR m.menu_id = 900100000102
  OR (m.parent_id = 900100000102 AND m.menu_id BETWEEN 900100000204 AND 900100000212)
  OR m.menu_id BETWEEN 900100001001 AND 900100001007
WHERE r.role_key IN ('user', 'mark_user');
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r JOIN sys_menu m
  ON m.menu_id = 900100000001 OR m.menu_id = 900100000103
  OR (m.parent_id = 900100000103 AND m.menu_id BETWEEN 900100000302 AND 900100000310)
  OR m.menu_id BETWEEN 900100001008 AND 900100001009
WHERE r.role_key IN ('agent', 'mark_agent');

-- Disable the legacy business/* placeholder tree after the new server/mark/*
-- tree is installed. Records are retained for rollback, but no role can load it.
UPDATE sys_menu
SET visible = '1', status = '1', update_by = 'admin', update_time = NOW()
WHERE menu_id >= 95000000000000001 AND menu_id < 96000000000000000;

DELETE rm
FROM sys_role_menu rm
WHERE rm.menu_id >= 95000000000000001 AND rm.menu_id < 96000000000000000;

-- Interface management reuses the existing query URL configuration and adds
-- a stable business key that is independent from legacy numeric platform IDs.
SET @has_platform_key := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'user_platform_url_config'
    AND column_name = 'platform_key'
);
SET @platform_key_alter := IF(@has_platform_key = 0,
  'ALTER TABLE user_platform_url_config ADD COLUMN platform_key varchar(50) DEFAULT NULL COMMENT ''平台编码'' AFTER platform_id, ADD KEY idx_platform_key (platform_key)',
  'SELECT 1');
PREPARE platform_key_stmt FROM @platform_key_alter;
EXECUTE platform_key_stmt;
DEALLOCATE PREPARE platform_key_stmt;

SET @has_platform_key_unique := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'user_platform_url_config'
    AND index_name = 'uk_platform_key' AND non_unique = 0
);
SET @drop_platform_key_unique := IF(@has_platform_key_unique > 0,
  'ALTER TABLE user_platform_url_config DROP INDEX uk_platform_key', 'SELECT 1');
PREPARE drop_platform_key_unique_stmt FROM @drop_platform_key_unique;
EXECUTE drop_platform_key_unique_stmt;
DEALLOCATE PREPARE drop_platform_key_unique_stmt;

SET @has_platform_key_index := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'user_platform_url_config'
    AND index_name = 'idx_platform_key'
);
SET @add_platform_key_index := IF(@has_platform_key_index = 0,
  'ALTER TABLE user_platform_url_config ADD KEY idx_platform_key (platform_key)', 'SELECT 1');
PREPARE add_platform_key_index_stmt FROM @add_platform_key_index;
EXECUTE add_platform_key_index_stmt;
DEALLOCATE PREPARE add_platform_key_index_stmt;

UPDATE user_platform_url_config SET platform_key = 'taidixiong' WHERE platform_name = '泰迪熊' AND (platform_key IS NULL OR platform_key = '');
UPDATE user_platform_url_config SET platform_key = 'tengxun' WHERE platform_name = '腾讯' AND (platform_key IS NULL OR platform_key = '');
UPDATE user_platform_url_config SET platform_key = '360fugai' WHERE platform_key = 'sanliuling';
UPDATE user_platform_url_config SET platform_key = '360fugai' WHERE platform_name = '360' AND status = '0' AND (platform_key IS NULL OR platform_key = '');
UPDATE user_platform_url_config SET platform_key = 'baidu' WHERE platform_name = '百度' AND (platform_key IS NULL OR platform_key = '');
UPDATE user_platform_url_config SET platform_key = 'sogou' WHERE platform_key = 'sghmt';
UPDATE user_platform_url_config SET platform_key = 'sogou' WHERE platform_name = '搜狗' AND (platform_key IS NULL OR platform_key = '');
UPDATE user_platform_url_config SET platform_key = 'yidonggaopin' WHERE platform_name = '移动高频' AND (platform_key IS NULL OR platform_key = '');
UPDATE user_platform_url_config SET platform_key = 'dianhuabang' WHERE platform_name = '电话邦' AND (platform_key IS NULL OR platform_key = '');
UPDATE user_platform_url_config SET platform_key = 'ltgj' WHERE platform_name = '联通管家' AND (platform_key IS NULL OR platform_key = '');
UPDATE user_platform_url_config SET platform_key = 'xiaomi' WHERE platform_name = '小米手机' AND (platform_key IS NULL OR platform_key = '');

UPDATE sys_menu
SET menu_name = '平台API配置', parent_id = 92281941572000140, order_num = 2,
    path = 'platformConfig', component = 'server/numberPlatformConfig/index',
    route_name = 'NumberPlatformConfig', visible = '0', status = '0',
    perms = 'server:platformConfig:list', icon = 'api_manager',
    update_by = 'admin', update_time = NOW(), remark = '号码查询平台API配置'
WHERE menu_id = 92281941572000177;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 900100000104, '接口管理', 900100000001, 3, 'interfaceManagement',
  'server/platformConfig/index', '', 'MarkInterfaceManagement',
  1, 0, 'C', '0', '0', 'server:platformConfig:list', 'connection', 'admin', NOW(), '标记查询接口与平台编码绑定管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000104);

UPDATE sys_menu
SET menu_name = '接口管理', parent_id = 900100000001, order_num = 3,
    path = 'interfaceManagement', component = 'server/platformConfig/index',
    route_name = 'MarkInterfaceManagement', visible = '0', status = '0',
    perms = 'server:platformConfig:list', icon = 'connection',
    update_by = 'admin', update_time = NOW(), remark = '标记查询接口与平台编码绑定管理'
WHERE menu_id = 900100000104;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 900100001010, '接口连接测试', 900100000104, 6, '#', '', '', '',
  1, 0, 'F', '0', '0', 'server:platformConfig:test', '#', 'admin', NOW(), '查询接口连接测试权限'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100001010);
UPDATE sys_menu SET parent_id = 900100000104 WHERE menu_id = 900100001010;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 92281941572000177 FROM sys_role WHERE role_key = 'admin';
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 900100000104 FROM sys_role WHERE role_key = 'admin';
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, p.menu_id
FROM sys_role r
CROSS JOIN (
  SELECT 92281941572000178 menu_id UNION ALL SELECT 92281941572000179
  UNION ALL SELECT 92281941572000180 UNION ALL SELECT 92281941572000181
  UNION ALL SELECT 92281941572000182 UNION ALL SELECT 900100001010
) p
WHERE r.role_key = 'admin';

-- Keep the three teddy platforms independent on both the user and agent sides.
UPDATE sys_menu SET order_num = 1,
  query = '{"platformCode":"taidixiong","platformName":"泰迪熊"}', visible = '0', status = '0'
WHERE menu_id = 900100000302;
UPDATE sys_menu SET order_num = 2 WHERE menu_id = 900100000303;
UPDATE sys_menu SET order_num = 3,
  query = '{"platformCodes":"360fugai,qihu_second","platformName":"360"}', visible = '0', status = '0'
WHERE menu_id = 900100000304;
UPDATE sys_menu SET order_num = 4 WHERE menu_id = 900100000305;
UPDATE sys_menu SET order_num = 5 WHERE menu_id = 900100000306;
UPDATE sys_menu SET order_num = 6 WHERE menu_id = 900100000307;
UPDATE sys_menu SET order_num = 7 WHERE menu_id = 900100000308;
UPDATE sys_menu SET order_num = 8 WHERE menu_id = 900100000309;
UPDATE sys_menu SET order_num = 9 WHERE menu_id = 900100000310;
UPDATE sys_menu SET visible = '0', status = '0'
WHERE menu_id IN (900100000311, 900100000312);
UPDATE sys_menu SET visible = '1', status = '1'
WHERE menu_id = 900100000314;

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
  (900100000213, 'TdGaopin高频', 900100000102, 2, 'tdGaopin', 'server/mark/user/index',
   '{"platformCode":"td_gaopin","platformName":"TdGaopin高频"}', 'MarkUserTdGaopin',
   1, 1, 'C', '0', '0', 'server:markUser:order:list', 'user', 'admin', NOW(), '用户端独立平台'),
  (900100000214, 'Taidixiong二次', 900100000102, 3, 'tdSecond', 'server/mark/user/tencent',
   '{"platformCode":"td_second","platformName":"Taidixiong二次"}', 'MarkUserTdSecond',
   1, 1, 'C', '0', '0', 'server:markUser:order:list', 'user', 'admin', NOW(), '用户端独立平台')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), parent_id=VALUES(parent_id),
  order_num=VALUES(order_num), path=VALUES(path), component=VALUES(component),
  query=VALUES(query), route_name=VALUES(route_name), visible='0', status='0';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r
JOIN sys_menu m ON m.menu_id IN (900100000213, 900100000214)
WHERE r.role_key IN ('admin', 'user', 'mark_user');

-- Keep user mark pages alive when switching platform menus.
UPDATE sys_menu SET is_cache = 1
WHERE menu_id IN (900100000101, 900100000204, 900100000205, 900100000206,
                  900100000207, 900100000208, 900100000209, 900100000210,
                  900100000211, 900100000212);

SET FOREIGN_KEY_CHECKS = 1;
