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
