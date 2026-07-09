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
