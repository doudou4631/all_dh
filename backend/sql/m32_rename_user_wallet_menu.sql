-- M32: rename user wallet sidebar menu to consumption details
UPDATE `sys_menu`
SET `menu_name` = '消费明细',
    `remark` = '消费明细菜单',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 900100000102;
