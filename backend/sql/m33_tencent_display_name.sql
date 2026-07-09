-- M33: unify Tencent platform display name to 腾讯速解 (menu + template JSON)
UPDATE `sys_menu`
SET `menu_name` = CONVERT(UNHEX('E885BEE8AEAFE9809FE8A7A3') USING utf8mb4),
    `remark` = '腾讯速解菜单',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 900100000126;

-- Align stored template platform names with sidebar label
UPDATE `mark_platform_template`
SET `template_info` = REPLACE(`template_info`, '"platformName":"腾讯"', '"platformName":"腾讯速解"'),
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `template_info` LIKE '%"platformName":"腾讯"%';
