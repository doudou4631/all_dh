SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M9：标记模板能力
-- 1) 新增模板表 mark_platform_template
-- 2) sys_user 新增 rel_mark_template
-- 3) 标记业务管理新增“标记模板”菜单与权限（admin）
-- =========================================================

CREATE TABLE IF NOT EXISTS `mark_platform_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_info` text COMMENT '模板信息（平台编码数组JSON）',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mark_platform_template_name` (`template_name`),
  KEY `idx_mark_platform_template_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标记平台模板表';

SET @has_rel_mark_template := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'rel_mark_template'
);
SET @alter_sql := IF(
  @has_rel_mark_template = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `rel_mark_template` bigint(20) NULL COMMENT ''标记模板ID'' AFTER `rel_template`',
  'SELECT 1'
);
PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @mark_root_menu_id := 900100000001;

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000127, '标记模板', @mark_root_menu_id, 11, 'markTemplate', 'server/mark/admin/template', '', '',
       1, 0, 'C', '0', '0', 'server:markTemplate:list', 'edit', 'admin', NOW(), 'M9-标记模板菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000127);

UPDATE `sys_menu`
SET `menu_name` = '标记模板',
    `parent_id` = @mark_root_menu_id,
    `order_num` = 11,
    `path` = 'markTemplate',
    `component` = 'server/mark/admin/template',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:markTemplate:list',
    `icon` = 'edit',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M9-标记模板菜单'
WHERE `menu_id` = 900100000127;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001701, '标记模板查询', 900100000127, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markTemplate:query', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001701);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001702, '标记模板新增', 900100000127, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markTemplate:add', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001702);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001703, '标记模板修改', 900100000127, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markTemplate:edit', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001703);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001704, '标记模板删除', 900100000127, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markTemplate:remove', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001704);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN (
  SELECT 900100000127 AS menu_id UNION ALL
  SELECT 900100001701 UNION ALL
  SELECT 900100001702 UNION ALL
  SELECT 900100001703 UNION ALL
  SELECT 900100001704
) m
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key = 'admin'
  AND rm.role_id IS NULL;

DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON rm.role_id = r.role_id
WHERE r.role_key IN ('common', 'user', 'agent')
  AND rm.menu_id IN (900100000127, 900100001701, 900100001702, 900100001703, 900100001704);

SET FOREIGN_KEY_CHECKS = 1;
