SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M5：管理端补充“免费查询 / 日志管理”菜单（幂等）
-- 归属目录：号码查询（menu_id = 92281941572000140）
-- =========================================================

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000201, '免费查询', 92281941572000140, 12, 'apiquery', 'server/web/apiquery', '', 'freeApiQuery',
       1, 0, 'C', '0', '0', 'server:freeQuery:page', 'search', 'admin', NOW(), 'M5-免费查询管理端菜单'
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_menu`
  WHERE `menu_id` = 900100000201
     OR (`parent_id` = 92281941572000140 AND `path` = 'apiquery')
);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000202, '日志管理', 92281941572000140, 13, 'apilog', 'server/web/apilog', '', 'freeApiLog',
       1, 0, 'C', '0', '0', 'server:freeQuery:log:list', 'form', 'admin', NOW(), 'M5-免费查询日志菜单'
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_menu`
  WHERE `menu_id` = 900100000202
     OR (`parent_id` = 92281941572000140 AND `path` = 'apilog')
);

-- 若菜单已存在（历史手工配置等），统一矫正为目标配置
UPDATE `sys_menu`
SET `menu_name` = '免费查询',
    `order_num` = 12,
    `component` = 'server/web/apiquery',
    `route_name` = 'freeApiQuery',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:freeQuery:page',
    `icon` = 'search',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M5-免费查询管理端菜单'
WHERE `parent_id` = 92281941572000140
  AND `path` = 'apiquery';

UPDATE `sys_menu`
SET `menu_name` = '日志管理',
    `order_num` = 13,
    `component` = 'server/web/apilog',
    `route_name` = 'freeApiLog',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:freeQuery:log:list',
    `icon` = 'form',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M5-免费查询日志菜单'
WHERE `parent_id` = 92281941572000140
  AND `path` = 'apilog';

-- admin 角色绑定（按路径取 menu_id，适配历史手工创建或不同ID）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN `sys_menu` m
  ON m.parent_id = 92281941572000140
 AND m.path IN ('apiquery', 'apilog')
LEFT JOIN `sys_role_menu` rm
  ON rm.role_id = r.role_id
 AND rm.menu_id = m.menu_id
WHERE r.role_key = 'admin'
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;
