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
