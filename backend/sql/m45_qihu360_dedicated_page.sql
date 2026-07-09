SET NAMES utf8mb4;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT
  900100000152,
  CONVERT(UNHEX('333630E68F90E58F91') USING utf8mb4),
  900100000001,
  12,
  'sanliulingMark',
  'server/mark/user/qihu360',
  '{"platformCode":"sanliuling","platformName":"360"}',
  'MarkUser__sanliuling',
  1, 0, 'C', '1', '0',
  'server:markUser:order:clear',
  'phone',
  'admin',
  NOW(),
  'M45-qihu360-dedicated-page'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000152);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 900100000152
FROM sys_role r
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = 900100000152
WHERE r.role_key IN ('mark_user', 'common', 'admin')
  AND rm.role_id IS NULL;

UPDATE sys_menu
SET
  component = 'server/mark/user/qihu360',
  perms = 'server:markUser:order:clear',
  visible = '1',
  remark = 'M45-qihu360-dedicated-page'
WHERE menu_id IN (900100000123, 900100000124, 900100000152)
   OR (
     parent_id = 900100000001
     AND CASE
           WHEN JSON_VALID(query) THEN LOWER(JSON_UNQUOTE(JSON_EXTRACT(query, '$.platformCode')))
           ELSE NULL
         END IN ('sanliuling', '360', 'qihu_first', 'qihu_second')
     AND component = 'server/mark/user/index'
   );

UPDATE sys_menu
SET
  path = 'qihuFirstMark',
  component = 'server/mark/user/qihu360',
  query = '{"platformCode":"qihu_first","platformName":"360首次"}',
  route_name = 'MarkUser__qihu_first',
  perms = 'server:markUser:order:clear',
  visible = '1'
WHERE menu_id = 900100000123;

UPDATE sys_menu
SET
  path = 'qihuSecondMark',
  component = 'server/mark/user/qihu360',
  query = '{"platformCode":"qihu_second","platformName":"360二次"}',
  route_name = 'MarkUser__qihu_second',
  perms = 'server:markUser:order:clear',
  visible = '1'
WHERE menu_id = 900100000124;

UPDATE sys_menu
SET
  path = 'sanliulingMark',
  component = 'server/mark/user/qihu360',
  query = '{"platformCode":"sanliuling","platformName":"360"}',
  route_name = 'MarkUser__sanliuling',
  perms = 'server:markUser:order:clear',
  visible = '1'
WHERE menu_id = 900100000152;
