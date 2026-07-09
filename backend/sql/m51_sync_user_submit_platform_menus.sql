SET NAMES utf8mb4;

-- M51: sync generic user submit-page route templates for sghmt,
-- dianhuabang, yidonggaopin and ltgj.

START TRANSACTION;

-- Existing 900100000124 was labelled as sghmt, but the route is qihu_second.
UPDATE sys_menu
SET menu_name = CONVERT(UNHEX('333630E4BA8CE6ACA1') USING utf8mb4),
    parent_id = 900100000001,
    order_num = 7,
    path = 'qihuSecondMark',
    component = 'server/mark/user/qihu360',
    query = CONCAT('{"platformCode":"qihu_second","platformName":"', CONVERT(UNHEX('333630E4BA8CE6ACA1') USING utf8mb4), '"}'),
    route_name = 'MarkUser__qihu_second',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'C',
    visible = '1',
    status = '0',
    perms = 'server:markUser:order:clear',
    icon = 'phone',
    update_by = 'admin',
    update_time = NOW(),
    remark = CASE
      WHEN IFNULL(remark, '') LIKE '%M51-qihu-second-name-fix%' THEN remark
      ELSE CONCAT(IFNULL(remark, ''), ' | M51-qihu-second-name-fix')
    END
WHERE menu_id = 900100000124;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT
  900100000154,
  CONVERT(UNHEX('E6909CE78B97') USING utf8mb4),
  900100000001,
  14,
  'sghmt',
  'server/mark/user/index',
  CONCAT('{"platformCode":"sghmt","platformName":"', CONVERT(UNHEX('E6909CE78B97') USING utf8mb4), '"}'),
  'MarkUser__sghmt',
  1, 0, 'C', '0', '0',
  'server:markUser:order:list',
  'list',
  'admin',
  NOW(),
  'M51-user-platform-sghmt'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000154);

UPDATE sys_menu
SET menu_name = CONVERT(UNHEX('E6909CE78B97') USING utf8mb4),
    parent_id = 900100000001,
    order_num = 14,
    path = 'sghmt',
    component = 'server/mark/user/index',
    query = CONCAT('{"platformCode":"sghmt","platformName":"', CONVERT(UNHEX('E6909CE78B97') USING utf8mb4), '"}'),
    route_name = 'MarkUser__sghmt',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'C',
    visible = '0',
    status = '0',
    perms = 'server:markUser:order:list',
    icon = 'list',
    update_by = 'admin',
    update_time = NOW(),
    remark = 'M51-user-platform-sghmt'
WHERE menu_id = 900100000154;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT
  900100000155,
  CONVERT(UNHEX('E794B5E8AF9DE982A6') USING utf8mb4),
  900100000001,
  15,
  'dianhuabang',
  'server/mark/user/index',
  CONCAT('{"platformCode":"dianhuabang","platformName":"', CONVERT(UNHEX('E794B5E8AF9DE982A6') USING utf8mb4), '"}'),
  'MarkUser__dianhuabang',
  1, 0, 'C', '0', '0',
  'server:markUser:order:list',
  'list',
  'admin',
  NOW(),
  'M51-user-platform-dianhuabang'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000155);

UPDATE sys_menu
SET menu_name = CONVERT(UNHEX('E794B5E8AF9DE982A6') USING utf8mb4),
    parent_id = 900100000001,
    order_num = 15,
    path = 'dianhuabang',
    component = 'server/mark/user/index',
    query = CONCAT('{"platformCode":"dianhuabang","platformName":"', CONVERT(UNHEX('E794B5E8AF9DE982A6') USING utf8mb4), '"}'),
    route_name = 'MarkUser__dianhuabang',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'C',
    visible = '0',
    status = '0',
    perms = 'server:markUser:order:list',
    icon = 'list',
    update_by = 'admin',
    update_time = NOW(),
    remark = 'M51-user-platform-dianhuabang'
WHERE menu_id = 900100000155;

UPDATE sys_menu
SET menu_name = CONVERT(UNHEX('E7A7BBE58AA8E9AB98E9A291') USING utf8mb4),
    parent_id = 900100000001,
    order_num = 8,
    path = 'yidonggaopin',
    component = 'server/mark/user/index',
    query = CONCAT('{"platformCode":"yidonggaopin","platformName":"', CONVERT(UNHEX('E7A7BBE58AA8E9AB98E9A291') USING utf8mb4), '"}'),
    route_name = 'MarkUser__yidonggaopin',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'C',
    visible = '0',
    status = '0',
    perms = 'server:markUser:order:list',
    icon = 'list',
    update_by = 'admin',
    update_time = NOW(),
    remark = CASE
      WHEN IFNULL(remark, '') LIKE '%M51-user-generic-submit%' THEN remark
      ELSE CONCAT(IFNULL(remark, ''), ' | M51-user-generic-submit')
    END
WHERE menu_id = 900100000125;

UPDATE sys_menu
SET menu_name = CONVERT(UNHEX('E88194E9809AE7AEA1E5AEB6') USING utf8mb4),
    parent_id = 900100000001,
    order_num = 13,
    path = 'ltgj',
    component = 'server/mark/user/index',
    query = CONCAT('{"platformCode":"ltgj","platformName":"', CONVERT(UNHEX('E88194E9809AE7AEA1E5AEB6') USING utf8mb4), '"}'),
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
    remark = CASE
      WHEN IFNULL(remark, '') LIKE '%M51-user-generic-submit%' THEN remark
      ELSE CONCAT(IFNULL(remark, ''), ' | M51-user-generic-submit')
    END
WHERE menu_id = 900100000153;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN (
  SELECT 900100000154 AS menu_id UNION ALL
  SELECT 900100000155 UNION ALL
  SELECT 900100000125 UNION ALL
  SELECT 900100000153
) m
LEFT JOIN sys_role_menu rm
       ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key IN ('user', 'mark_user', 'common', 'admin')
  AND rm.role_id IS NULL;

-- Keep permissions needed by the generic submit page and dynamic platform nav.
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN (
  SELECT 900100000101 AS menu_id UNION ALL
  SELECT 900100001102 UNION ALL
  SELECT 900100001103 UNION ALL
  SELECT 900100001104 UNION ALL
  SELECT 900100001202
) m
LEFT JOIN sys_role_menu rm
       ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key IN ('user', 'mark_user', 'common')
  AND rm.role_id IS NULL;

COMMIT;
