-- Three independent manual platforms. Run after mark_business_minimal.sql and mark_platform_management.sql.
-- Menu IDs use sys_menu AUTO_INCREMENT; repeat execution preserves platform switches and balances.
SET NAMES utf8mb4;
START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_mark_wuma_platform;
CREATE TEMPORARY TABLE tmp_mark_wuma_platform (
  platform_code varchar(64) NOT NULL PRIMARY KEY,
  platform_name varchar(50) NOT NULL,
  route_suffix varchar(32) NOT NULL,
  sort_num int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO tmp_mark_wuma_platform VALUES
  ('wuma_taidixiong', 'Taidixiong无码', 'Taidixiong', 20),
  ('wuma_tengxun', 'Tengxun无码', 'Tengxun', 21),
  ('wuma_360', '360无码', '360', 22);

INSERT IGNORE INTO mark_platform_mapping
  (source_platform_name, source_platform_code, source_enabled,
   target_platform_name, target_platform_code, target_enabled, mapping_enabled, sort_num)
SELECT platform_name, platform_code, '0', platform_name, platform_code, '0', '0', sort_num
FROM tmp_mark_wuma_platform;

UPDATE mark_platform_mapping m
-- Compare ASCII identifiers as bytes: existing tables may use different utf8mb4 collations.
JOIN tmp_mark_wuma_platform p ON BINARY m.source_platform_code = BINARY p.platform_code
  AND BINARY m.target_platform_code = BINARY p.platform_code
SET m.source_platform_name = p.platform_name, m.target_platform_name = p.platform_name, m.sort_num = p.sort_num;

-- User entries are direct children of the existing user menu.
INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT p.platform_name, 900100000102, p.sort_num, CONCAT('wuma', p.route_suffix),
  'server/mark/user/wuma', JSON_OBJECT('platformCode', p.platform_code, 'platformName', p.platform_name),
  CONCAT('MarkUserWuma', p.route_suffix), 1, 1, 'C', '0', '0', 'server:markUser:order:list',
  'user', 'admin', NOW(), '无码号码直接提交与任务记录'
FROM tmp_mark_wuma_platform p
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m WHERE m.parent_id = 900100000102 AND BINARY m.path = BINARY CONCAT('wuma', p.route_suffix)
);

-- Agent entries appear next to the existing platforms under 标记处理.
INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT p.platform_name, 900100000103, p.sort_num, CONCAT('agentWuma', p.route_suffix),
  'server/mark/agent/process/platform', JSON_OBJECT('platformCode', p.platform_code, 'platformName', p.platform_name),
  CONCAT('MarkAgentWuma', p.route_suffix), 1, 1, 'C', '0', '0', 'server:markAgent:order:query',
  'user', 'admin', NOW(), '无码代理人工处理独立队列'
FROM tmp_mark_wuma_platform p
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m WHERE m.parent_id = 900100000103 AND BINARY m.path = BINARY CONCAT('agentWuma', p.route_suffix)
);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id IN (900100000001, 900100000102)
  OR (m.parent_id = 900100000102 AND m.component = 'server/mark/user/wuma')
  OR m.perms IN ('server:markUser:order:add', 'server:markUser:order:query', 'server:markUser:price:list')
WHERE r.role_key IN ('admin', 'user', 'mark_user', 'mark_sub_agent') AND r.del_flag = '0';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id IN (900100000001, 900100000103)
  OR (m.parent_id = 900100000103 AND m.path IN ('agentWumaTaidixiong', 'agentWumaTengxun', 'agentWuma360'))
  OR m.perms = 'server:markAgent:item:feedback'
WHERE r.role_key IN ('admin', 'agent', 'mark_agent', 'mark_sub_agent') AND r.del_flag = '0';

DROP TEMPORARY TABLE tmp_mark_wuma_platform;
COMMIT;

-- Existing templates are preserved. Add these entries to the intended users' templates:
-- {"platformCode":"wuma_taidixiong","platformName":"Taidixiong无码","unitPrice":1}
-- {"platformCode":"wuma_tengxun","platformName":"Tengxun无码","unitPrice":1}
-- {"platformCode":"wuma_360","platformName":"360无码","unitPrice":1}
-- Recharge each platform separately through the existing agent-management workflow.
