SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- M47: fix menu_id conflict between agent hierarchy and LTGJ user platform.
-- 900100000129 belongs to the agent hierarchy directory. Some databases already
-- used it as the LTGJ user platform menu, which breaks the agent sidebar tree.

-- Keep the LTGJ user platform as a separate menu id before repurposing 900100000129.
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT
  900100000153,
  CONVERT(UNHEX('E88194E9809AE7AEA1E5AEB6') USING utf8mb4),
  900100000001,
  13,
  'ltgj',
  'server/mark/user/index',
  '{"platformCode":"ltgj"}',
  'MarkUser__ltgj',
  1, 0, 'C', '0', '0',
  'server:markUser:order:list',
  'list',
  'admin',
  NOW(),
  'M47-user-platform-ltgj'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000153);

UPDATE sys_menu
SET menu_name = CONVERT(UNHEX('E88194E9809AE7AEA1E5AEB6') USING utf8mb4),
    parent_id = 900100000001,
    order_num = 13,
    path = 'ltgj',
    component = 'server/mark/user/index',
    query = '{"platformCode":"ltgj"}',
    route_name = 'MarkUser__ltgj',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'C',
    visible = '0',
    status = '0',
    perms = 'server:markUser:order:list',
    icon = 'list',
    update_by = 'admin',
    update_time = NOW(),
    remark = 'M47-user-platform-ltgj'
WHERE menu_id = 900100000153;

-- Preserve user-side LTGJ authorization from the conflicted id.
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 900100000153
FROM sys_role r
LEFT JOIN sys_role_menu existed
       ON existed.role_id = r.role_id AND existed.menu_id = 900100000153
WHERE r.role_key IN ('user', 'mark_user', 'common', 'admin')
  AND existed.role_id IS NULL
  AND (
    EXISTS (
      SELECT 1
      FROM sys_role_menu old_rm
      WHERE old_rm.role_id = r.role_id
        AND old_rm.menu_id = 900100000129
    )
    OR r.role_key IN ('mark_user', 'common', 'admin')
  );

-- Restore 900100000129 as the agent hierarchy directory.
UPDATE sys_menu
SET menu_name = CONVERT(UNHEX('E4BBA3E79086E59586E4BD93E7B3BB') USING utf8mb4),
    parent_id = 900100000001,
    order_num = 3,
    path = 'agentHierarchy',
    component = '',
    query = '',
    route_name = '',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'M',
    visible = '0',
    status = '0',
    perms = '',
    icon = 'peoples',
    update_by = 'admin',
    update_time = NOW(),
    remark = 'M47-agent-hierarchy-dir'
WHERE menu_id = 900100000129;

-- Ensure agent hierarchy children are under the restored parent.
UPDATE sys_menu
SET parent_id = 900100000129,
    order_num = 1,
    menu_name = CONVERT(UNHEX('E680BBE4BBA3E79086E4BFA1E681AF') USING utf8mb4),
    path = 'agentSummary',
    component = 'server/mark/agent/summary/index',
    menu_type = 'C',
    visible = '0',
    status = '0',
    perms = 'server:markAgent:order:query',
    update_by = 'admin',
    update_time = NOW()
WHERE menu_id = 900100000130;

UPDATE sys_menu
SET parent_id = 900100000129,
    order_num = 2,
    update_by = 'admin',
    update_time = NOW()
WHERE menu_id = 900100000114;

UPDATE sys_menu
SET parent_id = 900100000129,
    order_num = 3,
    menu_name = CONVERT(UNHEX('E8B4A6E688B7E7AEA1E79086') USING utf8mb4),
    path = 'agentAccount',
    component = 'system/user/index',
    menu_type = 'C',
    visible = '0',
    status = '0',
    perms = 'system:user:list',
    update_by = 'admin',
    update_time = NOW()
WHERE menu_id = 900100000109;

UPDATE sys_menu
SET parent_id = 900100000129,
    order_num = 4,
    menu_name = CONVERT(UNHEX('E8B584E98791E8BDACE8B4A6E6988EE7BB86') USING utf8mb4),
    path = 'agentTransfer',
    component = 'server/mark/agent/transfer/index',
    menu_type = 'C',
    visible = '0',
    status = '0',
    perms = 'server:markAgent:wallet:list',
    update_by = 'admin',
    update_time = NOW()
WHERE menu_id = 900100000131;

-- Agent roles should keep the hierarchy; user roles should use the LTGJ leaf.
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN (
  SELECT 900100000129 AS menu_id UNION ALL
  SELECT 900100000130 UNION ALL
  SELECT 900100000114 UNION ALL
  SELECT 900100000109 UNION ALL
  SELECT 900100000131
) m
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key IN ('agent', 'mark_agent')
  AND rm.role_id IS NULL;

DELETE rm
FROM sys_role_menu rm
JOIN sys_role r ON r.role_id = rm.role_id
WHERE rm.menu_id = 900100000129
  AND r.role_key IN ('user', 'mark_user', 'common');

SET FOREIGN_KEY_CHECKS = 1;
