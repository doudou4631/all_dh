-- Add the dedicated subordinate-agent role and its business/management permissions.
-- Safe to run repeatedly.
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly,
  dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT '下级代理', 'mark_sub_agent', 32, '1', 1, 1, '0', '0', 'admin', NOW(),
  '标记业务下级代理角色'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'mark_sub_agent');

-- Inherit the ordinary mark-user business menus.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT target.role_id, rm.menu_id
FROM sys_role target
JOIN sys_role source ON source.role_key = 'mark_user' AND source.del_flag = '0'
JOIN sys_role_menu rm ON rm.role_id = source.role_id
WHERE target.role_key = 'mark_sub_agent' AND target.del_flag = '0';

-- Add the agent-management directory, ordinary-account page, and operations.
-- A level-2 agent manages its direct ordinary accounts (the level-3 UI view).
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id IN (
  900100000107, 900100000108,
  900100000109, 900100000112, 900100001015, 900100001016,
  900100001017, 900100001018
)
WHERE r.role_key = 'mark_sub_agent' AND r.del_flag = '0';

DELETE rm
FROM sys_role_menu rm
JOIN sys_role r ON r.role_id = rm.role_id AND r.role_key = 'mark_sub_agent'
WHERE rm.menu_id = 900100000111;

-- Existing level-2 agents use the dedicated role going forward.
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u
JOIN sys_role r ON r.role_key = 'mark_sub_agent' AND r.status = '0' AND r.del_flag = '0'
WHERE u.agent_level = 2 AND u.del_flag = 0;

DELETE ur
FROM sys_user_role ur
JOIN sys_user u ON u.user_id = ur.user_id AND u.agent_level = 2 AND u.del_flag = 0
JOIN sys_role old_role ON old_role.role_id = ur.role_id AND old_role.role_key = 'mark_agent';
