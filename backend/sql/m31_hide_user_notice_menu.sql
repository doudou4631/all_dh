-- M31: hide "My Messages" from sidebar navigation (keep navbar bell + permissions)
UPDATE `sys_menu`
SET `visible` = '1',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 900100000112;
