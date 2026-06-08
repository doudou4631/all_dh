SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

SET @mark_root_menu_id := 900100000001;

-- 兜底插入 7 个平台菜单
INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000120, '高频拦截', @mark_root_menu_id, 3, 'mobileGaopin', 'server/mark/user/index', '{"platformCode":"mobile_gaopin"}', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), 'M8-用户端平台菜单-高频拦截'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000120);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000121, '泰迪高频', @mark_root_menu_id, 4, 'tdGaopin', 'server/mark/user/index', '{"platformCode":"td_gaopin"}', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), 'M8-用户端平台菜单-泰迪高频'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000121);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000122, '泰迪二次', @mark_root_menu_id, 5, 'tdSecond', 'server/mark/user/index', '{"platformCode":"td_second"}', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), 'M8-用户端平台菜单-泰迪二次'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000122);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000123, '360首次', @mark_root_menu_id, 6, 'qihuFirst', 'server/mark/user/index', '{"platformCode":"qihu_first"}', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), 'M8-用户端平台菜单-360首次'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000123);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000124, '360二次', @mark_root_menu_id, 7, 'qihuSecond', 'server/mark/user/index', '{"platformCode":"qihu_second"}', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), 'M8-用户端平台菜单-360二次'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000124);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000125, '电话邦', @mark_root_menu_id, 8, 'dianhuabang', 'server/mark/user/index', '{"platformCode":"dianhuabang"}', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), 'M8-用户端平台菜单-电话邦'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000125);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000126, '腾讯', @mark_root_menu_id, 9, 'tencentMark', 'server/mark/user/index', '{"platformCode":"tencent_mark"}', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), 'M8-用户端平台菜单-腾讯'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000126);

-- 幂等矫正
UPDATE `sys_menu`
SET `menu_name` = '高频拦截',
    `parent_id` = @mark_root_menu_id,
    `order_num` = 3,
    `path` = 'mobileGaopin',
    `component` = 'server/mark/user/index',
    `query` = '{"platformCode":"mobile_gaopin"}',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:markUser:order:list',
    `icon` = 'list',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M8-用户端平台菜单-高频拦截'
WHERE `menu_id` = 900100000120;

UPDATE `sys_menu`
SET `menu_name` = '泰迪高频',
    `parent_id` = @mark_root_menu_id,
    `order_num` = 4,
    `path` = 'tdGaopin',
    `component` = 'server/mark/user/index',
    `query` = '{"platformCode":"td_gaopin"}',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:markUser:order:list',
    `icon` = 'list',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M8-用户端平台菜单-泰迪高频'
WHERE `menu_id` = 900100000121;

UPDATE `sys_menu`
SET `menu_name` = '泰迪二次',
    `parent_id` = @mark_root_menu_id,
    `order_num` = 5,
    `path` = 'tdSecond',
    `component` = 'server/mark/user/index',
    `query` = '{"platformCode":"td_second"}',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:markUser:order:list',
    `icon` = 'list',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M8-用户端平台菜单-泰迪二次'
WHERE `menu_id` = 900100000122;

UPDATE `sys_menu`
SET `menu_name` = '360首次',
    `parent_id` = @mark_root_menu_id,
    `order_num` = 6,
    `path` = 'qihuFirst',
    `component` = 'server/mark/user/index',
    `query` = '{"platformCode":"qihu_first"}',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:markUser:order:list',
    `icon` = 'list',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M8-用户端平台菜单-360首次'
WHERE `menu_id` = 900100000123;

UPDATE `sys_menu`
SET `menu_name` = '360二次',
    `parent_id` = @mark_root_menu_id,
    `order_num` = 7,
    `path` = 'qihuSecond',
    `component` = 'server/mark/user/index',
    `query` = '{"platformCode":"qihu_second"}',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:markUser:order:list',
    `icon` = 'list',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M8-用户端平台菜单-360二次'
WHERE `menu_id` = 900100000124;

UPDATE `sys_menu`
SET `menu_name` = '电话邦',
    `parent_id` = @mark_root_menu_id,
    `order_num` = 8,
    `path` = 'dianhuabang',
    `component` = 'server/mark/user/index',
    `query` = '{"platformCode":"dianhuabang"}',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:markUser:order:list',
    `icon` = 'list',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M8-用户端平台菜单-电话邦'
WHERE `menu_id` = 900100000125;

UPDATE `sys_menu`
SET `menu_name` = '腾讯',
    `parent_id` = @mark_root_menu_id,
    `order_num` = 9,
    `path` = 'tencentMark',
    `component` = 'server/mark/user/index',
    `query` = '{"platformCode":"tencent_mark"}',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:markUser:order:list',
    `icon` = 'list',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M8-用户端平台菜单-腾讯'
WHERE `menu_id` = 900100000126;

-- 用户订单隐藏，用户钱包保留
UPDATE `sys_menu`
SET `visible` = '1',
    `status` = '0',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 900100000101;

UPDATE `sys_menu`
SET `visible` = '0',
    `status` = '0',
    `order_num` = 10,
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 900100000102;
-- 用户端按钮权限（预查询/直接提交/提交消除）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001103, '用户订单预查询', 900100000101, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markUser:order:precheck', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001103);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001104, '用户订单提交消除', 900100000101, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markUser:order:clear', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001104);

UPDATE `sys_menu`
SET `menu_name` = '用户订单预查询',
    `parent_id` = 900100000101,
    `order_num` = 3,
    `menu_type` = 'F',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:markUser:order:precheck',
    `icon` = '#',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 900100001103;

UPDATE `sys_menu`
SET `menu_name` = '用户订单提交消除',
    `parent_id` = 900100000101,
    `order_num` = 4,
    `menu_type` = 'F',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:markUser:order:clear',
    `icon` = '#',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 900100001104;

-- common/user 角色保留平台入口，移除用户订单入口
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN (
  SELECT 900100000120 AS menu_id UNION ALL
  SELECT 900100000121 UNION ALL
  SELECT 900100000122 UNION ALL
  SELECT 900100000123 UNION ALL
  SELECT 900100000124 UNION ALL
  SELECT 900100000125 UNION ALL
  SELECT 900100000126 UNION ALL
  SELECT 900100001101 UNION ALL
  SELECT 900100001102 UNION ALL
  SELECT 900100001103 UNION ALL
  SELECT 900100001104
) m
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key IN ('common', 'user')
  AND rm.role_id IS NULL;

DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON rm.role_id = r.role_id
WHERE r.role_key IN ('common', 'user')
  AND rm.menu_id IN (900100000101);

-- admin/agent 不展示用户端平台入口
DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON rm.role_id = r.role_id
WHERE r.role_key IN ('admin', 'agent')
  AND rm.menu_id IN (900100000120, 900100000121, 900100000122, 900100000123, 900100000124, 900100000125, 900100000126);

SET FOREIGN_KEY_CHECKS = 1;
