SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M7：新增独立一级“手机端”菜单，并将“免费查询/日志管理”迁移到其下（幂等）
-- 一级顺序：默认放在“代理管理”后一个顺位
-- =========================================================
SET @root_parent_id := 0;
SET @agent_order := (
  SELECT `order_num`
  FROM `sys_menu`
  WHERE `menu_name` = '代理管理'
    AND `parent_id` = 0
  ORDER BY `menu_id`
  LIMIT 1
);
SET @mobile_root_order := IFNULL(@agent_order + 1, 9);
SET @mobile_menu_default_id := 900100000203;

-- 1) 新增“手机端”目录（若不存在）
INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT @mobile_menu_default_id, '手机端', @root_parent_id, @mobile_root_order, 'mobile', '', '', 'mobileClient',
       1, 0, 'M', '0', '0', NULL, 'component', 'admin', NOW(), 'M7-手机端目录'
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_menu`
  WHERE `menu_id` = @mobile_menu_default_id
     OR (`parent_id` = @root_parent_id AND `path` = 'mobile')
);

-- 2) 统一“手机端”目录配置
UPDATE `sys_menu`
SET `menu_name` = '手机端',
    `parent_id` = @root_parent_id,
    `order_num` = @mobile_root_order,
    `component` = '',
    `route_name` = 'mobileClient',
    `menu_type` = 'M',
    `visible` = '0',
    `status` = '0',
    `perms` = NULL,
    `icon` = 'component',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M7-手机端目录'
WHERE `menu_id` = @mobile_menu_default_id
   OR (`menu_name` = '手机端' AND `path` = 'mobile');

SET @mobile_menu_id := (
  SELECT `menu_id`
  FROM `sys_menu`
  WHERE `menu_name` = '手机端'
    AND `path` = 'mobile'
  ORDER BY `menu_id`
  LIMIT 1
);

-- 3) 若历史环境没有这两个菜单，先补齐
INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000201, '免费查询', @mobile_menu_id, 1, 'apiquery', 'server/web/apiquery', '', 'freeApiQuery',
       1, 0, 'C', '0', '0', 'server:freeQuery:page', 'search', 'admin', NOW(), 'M7-免费查询管理端菜单'
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_menu`
  WHERE `menu_id` = 900100000201
     OR (`component` = 'server/web/apiquery')
);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000202, '日志管理', @mobile_menu_id, 2, 'apilog', 'server/web/apilog', '', 'freeApiLog',
       1, 0, 'C', '0', '0', 'server:freeQuery:log:list', 'form', 'admin', NOW(), 'M7-免费查询日志菜单'
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_menu`
  WHERE `menu_id` = 900100000202
     OR (`component` = 'server/web/apilog')
);

-- 4) 统一把两个菜单迁移到“手机端”下
UPDATE `sys_menu`
SET `menu_name` = '免费查询',
    `parent_id` = @mobile_menu_id,
    `order_num` = 1,
    `component` = 'server/web/apiquery',
    `route_name` = 'freeApiQuery',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:freeQuery:page',
    `icon` = 'search',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M7-免费查询管理端菜单'
WHERE `menu_id` = 900100000201
   OR (`path` = 'apiquery' AND `component` = 'server/web/apiquery');

UPDATE `sys_menu`
SET `menu_name` = '日志管理',
    `parent_id` = @mobile_menu_id,
    `order_num` = 2,
    `component` = 'server/web/apilog',
    `route_name` = 'freeApiLog',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:freeQuery:log:list',
    `icon` = 'form',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M7-免费查询日志菜单'
WHERE `menu_id` = 900100000202
   OR (`path` = 'apilog' AND `component` = 'server/web/apilog');

-- 5) admin 角色补齐目录与子菜单授权
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, @mobile_menu_id
FROM `sys_role` r
LEFT JOIN `sys_role_menu` rm
  ON rm.role_id = r.role_id
 AND rm.menu_id = @mobile_menu_id
WHERE r.role_key = 'admin'
  AND rm.role_id IS NULL;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN `sys_menu` m
  ON m.parent_id = @mobile_menu_id
 AND m.path IN ('apiquery', 'apilog')
LEFT JOIN `sys_role_menu` rm
  ON rm.role_id = r.role_id
 AND rm.menu_id = m.menu_id
WHERE r.role_key = 'admin'
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;
