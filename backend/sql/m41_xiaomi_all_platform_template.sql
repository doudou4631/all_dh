SET NAMES utf8mb4;

-- M41: ensure "全平台模版" includes xiaomi (restore if removed by M39)
UPDATE mark_platform_template
SET template_info = CONCAT(
        TRIM(TRAILING ']' FROM TRIM(template_info)),
        ',{"platformCode":"xiaomi","platformName":"小米手机","unitPrice":1}]'
    ),
    update_by = 'admin',
    update_time = NOW()
WHERE status = '0'
  AND template_name LIKE '%全平台%'
  AND IFNULL(template_info, '') <> ''
  AND template_info NOT LIKE '%"platformCode":"xiaomi"%';

-- Ensure xiaomi dedicated route exists and is available for template-driven nav injection
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT
  900100000150,
  CONVERT(UNHEX('E5B08FE7B1B3E6898BE69CBA') USING utf8mb4),
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
  'M41-xiaomi-all-platform-template'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000150);

UPDATE sys_menu
SET visible = '1',
    component = 'server/mark/user/xiaomi',
    route_name = 'MarkUserXiaomi',
    status = '0',
    remark = CONCAT(IFNULL(remark, ''), ' | M41-xiaomi-all-platform-template')
WHERE menu_id = 900100000150;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 900100000150
FROM sys_role r
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = 900100000150
WHERE r.role_key IN ('mark_user', 'common', 'admin')
  AND rm.role_id IS NULL;
