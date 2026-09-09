-- Split the three teddy mappings into independent user and agent platforms.
START TRANSACTION;

UPDATE mark_platform_mapping
SET source_platform_name = '泰迪熊', source_platform_code = 'taidixiong'
WHERE target_platform_code = 'taidixiong';

UPDATE mark_platform_mapping
SET source_platform_name = 'TdGaopin高频', source_platform_code = 'td_gaopin'
WHERE target_platform_code = 'td_gaopin';

UPDATE mark_platform_mapping
SET source_platform_name = 'Taidixiong二次', source_platform_code = 'td_second'
WHERE target_platform_code = 'td_second';

UPDATE sys_menu
SET menu_name = '泰迪熊', order_num = 1,
    query = '{"platformCode":"taidixiong","platformName":"泰迪熊"}',
    visible = '0', status = '0'
WHERE menu_id IN (900100000204, 900100000302);

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
    (900100000213, 'TdGaopin高频', 900100000102, 2, 'tdGaopin',
     'server/mark/user/index', '{"platformCode":"td_gaopin","platformName":"TdGaopin高频"}',
     'MarkUserTdGaopin', 1, 1, 'C', '0', '0', 'server:markUser:order:list', 'user', 'admin', NOW(), '用户端独立平台'),
    (900100000214, 'Taidixiong二次', 900100000102, 3, 'tdSecond',
     'server/mark/user/tencent', '{"platformCode":"td_second","platformName":"Taidixiong二次"}',
     'MarkUserTdSecond', 1, 1, 'C', '0', '0', 'server:markUser:order:list', 'user', 'admin', NOW(), '用户端独立平台')
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name), parent_id = VALUES(parent_id), order_num = VALUES(order_num),
    path = VALUES(path), component = VALUES(component), query = VALUES(query),
    route_name = VALUES(route_name), is_cache = VALUES(is_cache), visible = '0', status = '0';

UPDATE sys_menu
SET menu_name = 'TdGaopin高频', order_num = 2,
    query = '{"platformCode":"td_gaopin","platformName":"TdGaopin高频"}',
    visible = '0', status = '0'
WHERE menu_id = 900100000311;

UPDATE sys_menu
SET menu_name = 'Taidixiong二次', order_num = 3,
    query = '{"platformCode":"td_second","platformName":"Taidixiong二次"}',
    visible = '0', status = '0'
WHERE menu_id = 900100000312;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, menu_id
FROM sys_role
CROSS JOIN (
    SELECT 900100000213 AS menu_id
    UNION ALL SELECT 900100000214
) user_menu
WHERE role_key IN ('admin', 'user', 'mark_user');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, menu_id
FROM sys_role
CROSS JOIN (
    SELECT 900100000302 AS menu_id
    UNION ALL SELECT 900100000311
    UNION ALL SELECT 900100000312
) agent_menu
WHERE role_key IN ('admin', 'agent', 'mark_agent');

COMMIT;
