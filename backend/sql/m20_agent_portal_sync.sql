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
