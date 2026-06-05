SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M10：免费查询独立用户体系
-- 1) 新增 free_query_user / free_query_point_record
-- 2) 手机端菜单新增“免费用户管理”与按钮权限
-- 3) 仅给 admin 角色授权
-- =========================================================

CREATE TABLE IF NOT EXISTS `free_query_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account` varchar(64) NOT NULL COMMENT '账号',
  `password` varchar(100) NOT NULL COMMENT '密码（BCrypt）',
  `nick_name` varchar(64) DEFAULT '' COMMENT '昵称',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `points` int(11) NOT NULL DEFAULT 0 COMMENT '积分余额',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `last_login_ip` varchar(128) DEFAULT NULL COMMENT '最后登录IP',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_free_query_user_account` (`account`),
  KEY `idx_free_query_user_phone` (`phone`),
  KEY `idx_free_query_user_status` (`status`),
  KEY `idx_free_query_user_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='免费查询独立用户表';

CREATE TABLE IF NOT EXISTS `free_query_point_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `free_user_id` bigint(20) NOT NULL COMMENT '免费查询用户ID',
  `point_amount` int(11) NOT NULL COMMENT '积分变动值',
  `point_type` char(1) NOT NULL COMMENT '积分类型（1增加 2扣减）',
  `business_type` varchar(32) DEFAULT NULL COMMENT '业务类型',
  `biz_no` varchar(64) DEFAULT NULL COMMENT '业务单号',
  `reason` varchar(255) DEFAULT NULL COMMENT '变动原因',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作人ID',
  `balance_after` int(11) DEFAULT NULL COMMENT '变动后余额',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_free_query_point_record_user` (`free_user_id`),
  KEY `idx_free_query_point_record_biz_no` (`biz_no`),
  KEY `idx_free_query_point_record_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='免费查询积分流水表';

SET @mobile_menu_id := (
  SELECT `menu_id`
  FROM `sys_menu`
  WHERE `parent_id` = 0
    AND `path` = 'mobile'
  ORDER BY `menu_id`
  LIMIT 1
);

SET @mobile_menu_default_id := IFNULL(@mobile_menu_id, 900100000203);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT @mobile_menu_default_id, '手机端', 0, 9, 'mobile', '', '', 'mobileClient',
       1, 0, 'M', '0', '0', NULL, 'component', 'admin', NOW(), 'M10-手机端目录兜底'
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_menu`
  WHERE `parent_id` = 0
    AND `path` = 'mobile'
);

SET @mobile_menu_id := (
  SELECT `menu_id`
  FROM `sys_menu`
  WHERE `parent_id` = 0
    AND `path` = 'mobile'
  ORDER BY `menu_id`
  LIMIT 1
);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000204, '免费用户管理', @mobile_menu_id, 3, 'freeUser', 'server/web/freeUser/index', '', 'freeQueryUser',
       1, 0, 'C', '0', '0', 'server:freeQueryUser:list', 'peoples', 'admin', NOW(), 'M10-免费用户管理菜单'
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_menu`
  WHERE `menu_id` = 900100000204
     OR (`parent_id` = @mobile_menu_id AND `path` = 'freeUser')
);

UPDATE `sys_menu`
SET `menu_name` = '免费用户管理',
    `parent_id` = @mobile_menu_id,
    `order_num` = 3,
    `path` = 'freeUser',
    `component` = 'server/web/freeUser/index',
    `route_name` = 'freeQueryUser',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:freeQueryUser:list',
    `icon` = 'peoples',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M10-免费用户管理菜单'
WHERE `menu_id` = 900100000204
   OR (`parent_id` = @mobile_menu_id AND `path` = 'freeUser');

SET @free_user_menu_id := (
  SELECT `menu_id`
  FROM `sys_menu`
  WHERE `parent_id` = @mobile_menu_id
    AND `path` = 'freeUser'
  ORDER BY `menu_id`
  LIMIT 1
);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 900100001721, '免费用户查询', @free_user_menu_id, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:freeQueryUser:query', '#', 'admin', NOW(), 'M10-免费用户查询按钮'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001721 OR (`parent_id` = @free_user_menu_id AND `perms` = 'server:freeQueryUser:query'));

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 900100001722, '免费用户新增', @free_user_menu_id, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:freeQueryUser:add', '#', 'admin', NOW(), 'M10-免费用户新增按钮'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001722 OR (`parent_id` = @free_user_menu_id AND `perms` = 'server:freeQueryUser:add'));

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 900100001723, '免费用户修改', @free_user_menu_id, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:freeQueryUser:edit', '#', 'admin', NOW(), 'M10-免费用户修改按钮'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001723 OR (`parent_id` = @free_user_menu_id AND `perms` = 'server:freeQueryUser:edit'));

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 900100001724, '免费用户删除', @free_user_menu_id, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:freeQueryUser:remove', '#', 'admin', NOW(), 'M10-免费用户删除按钮'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001724 OR (`parent_id` = @free_user_menu_id AND `perms` = 'server:freeQueryUser:remove'));

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 900100001725, '免费用户积分调整', @free_user_menu_id, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:freeQueryUser:adjust', '#', 'admin', NOW(), 'M10-免费用户积分调整按钮'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001725 OR (`parent_id` = @free_user_menu_id AND `perms` = 'server:freeQueryUser:adjust'));

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 900100001726, '免费用户重置密码', @free_user_menu_id, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:freeQueryUser:resetPwd', '#', 'admin', NOW(), 'M10-免费用户重置密码按钮'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001726 OR (`parent_id` = @free_user_menu_id AND `perms` = 'server:freeQueryUser:resetPwd'));

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN `sys_menu` m
  ON m.menu_id = @free_user_menu_id
  OR (m.parent_id = @free_user_menu_id AND m.perms IN (
      'server:freeQueryUser:query',
      'server:freeQueryUser:add',
      'server:freeQueryUser:edit',
      'server:freeQueryUser:remove',
      'server:freeQueryUser:adjust',
      'server:freeQueryUser:resetPwd'
  ))
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key = 'admin'
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;
