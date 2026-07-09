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
