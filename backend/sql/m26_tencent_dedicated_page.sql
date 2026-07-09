SET NAMES utf8mb4;

-- M26：腾讯用户端独立页面（方案 B）
UPDATE `sys_menu`
SET `component` = 'server/mark/user/tencent',
    `route_name` = 'MarkUserTencent',
    `is_cache` = '0',
    `remark` = CONCAT(IFNULL(`remark`, ''), ' | M26-腾讯独立页面')
WHERE `menu_id` = 900100000126;
