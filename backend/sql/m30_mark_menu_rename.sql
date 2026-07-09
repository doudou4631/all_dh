-- M30: rename mark root menu and keep remark in sync
UPDATE `sys_menu`
SET `menu_name` = '标记业务管理',
    `remark` = '标记业务管理目录',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 900100000001;
