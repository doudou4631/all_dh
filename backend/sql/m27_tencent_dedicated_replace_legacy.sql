SET NAMES utf8mb4;

-- M27: 腾讯用户端全面切换为专用页面，停用旧 index 批量流程
UPDATE `sys_menu`
SET `menu_name` = CONVERT(UNHEX('E885BEE8AEAF') USING utf8mb4),
    `component` = 'server/mark/user/tencent',
    `route_name` = 'MarkUserTencent',
    `path` = 'tencentMark',
    `query` = '',
    `is_cache` = '0',
    `status` = '0',
    `visible` = '0',
    `remark` = CONCAT(IFNULL(`remark`, ''), ' | M27-tencent-dedicated-replace-legacy')
WHERE `menu_id` = 900100000126;
