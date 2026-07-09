SET NAMES utf8mb4;

-- M28: 移除用户端旧 index 腾讯入口，仅保留专用页菜单 900100000126
UPDATE `sys_menu`
SET `menu_name` = CONVERT(UNHEX('E885BEE8AEAF') USING utf8mb4),
    `component` = 'server/mark/user/tencent',
    `route_name` = 'MarkUserTencent',
    `path` = 'tencentMark',
    `query` = '',
    `is_cache` = '0',
    `status` = '0',
    `visible` = '0',
    `remark` = CONCAT(IFNULL(`remark`, ''), ' | M28-remove-legacy-tencent-nav')
WHERE `menu_id` = 900100000126;

-- 若存在误插入的 index 腾讯菜单，则停用
UPDATE `sys_menu`
SET `status` = '1',
    `visible` = '1',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = CONCAT(IFNULL(`remark`, ''), ' | M28-disabled-legacy-index-tencent')
WHERE `parent_id` = 900100000001
  AND `menu_id` <> 900100000126
  AND `component` = 'server/mark/user/index'
  AND (
    `query` LIKE '%tencent_mark%'
    OR `query` LIKE '%tengxun%'
    OR `path` IN ('tencentMark', 'tengxunMark')
  );
