SET NAMES utf8mb4;

-- M40: user nav is template-driven; static dedicated menus stay as route templates only (sidebar hidden)
UPDATE sys_menu
SET visible = '1',
    remark = CONCAT(IFNULL(remark, ''), ' | M40-template-driven-nav')
WHERE menu_id IN (900100000126, 900100000150);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 900100000150
FROM sys_role r
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = 900100000150
WHERE r.role_key IN ('mark_user', 'common')
  AND rm.role_id IS NULL;
