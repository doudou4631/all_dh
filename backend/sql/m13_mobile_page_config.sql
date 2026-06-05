SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M13：手机端多页面配置（第二方案：固定入口 + page 参数）
-- 1) 新增 mobile_page_config
-- 2) 手机端菜单新增“手机页配置”及按钮权限
-- 3) 初始化 mobile-h5 / mobile-h1 页面配置
-- =========================================================

CREATE TABLE IF NOT EXISTS `mobile_page_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `page_code` varchar(32) NOT NULL COMMENT '页面编码（唯一）',
  `page_name` varchar(64) NOT NULL COMMENT '页面名称',
  `service_phone` varchar(20) NOT NULL DEFAULT '' COMMENT '客服电话',
  `wechat_qr_url` varchar(255) NOT NULL DEFAULT '' COMMENT '客服二维码地址',
  `nav_home_url` varchar(255) DEFAULT '/' COMMENT '底部-首页链接',
  `nav_query_url` varchar(255) DEFAULT '/?tab=query' COMMENT '底部-免费查询链接',
  `nav_batch_url` varchar(255) DEFAULT '/batch/' COMMENT '底部-批量查询链接',
  `nav_profile_url` varchar(255) DEFAULT '/profile/' COMMENT '底部-个人中心链接',
  `result_back_url` varchar(255) DEFAULT '/' COMMENT '结果页返回按钮链接',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '排序',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mobile_page_config_code` (`page_code`),
  KEY `idx_mobile_page_config_status` (`status`),
  KEY `idx_mobile_page_config_sort` (`sort`),
  KEY `idx_mobile_page_config_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手机端页面配置表';

-- 初始化：mobile-h5
INSERT INTO `mobile_page_config` (
  `page_code`, `page_name`, `service_phone`, `wechat_qr_url`,
  `nav_home_url`, `nav_query_url`, `nav_batch_url`, `nav_profile_url`, `result_back_url`,
  `status`, `sort`, `del_flag`, `create_by`, `create_time`, `remark`
)
SELECT 'mobile-h5', '手机页H5', '13027616171', '/mobile-h5/assets/icons/customer-wechat.png',
       '/', '/?tab=query', '/batch/', '/profile/', '/',
       '0', 10, '0', 'admin', NOW(), 'M13-初始化mobile-h5页面配置'
WHERE NOT EXISTS (
  SELECT 1 FROM `mobile_page_config` WHERE `page_code` = 'mobile-h5'
);

UPDATE `mobile_page_config`
SET `page_name` = '手机页H5',
    `service_phone` = '13027616171',
    `wechat_qr_url` = '/mobile-h5/assets/icons/customer-wechat.png',
    `nav_home_url` = '/',
    `nav_query_url` = '/?tab=query',
    `nav_batch_url` = '/batch/',
    `nav_profile_url` = '/profile/',
    `result_back_url` = '/',
    `status` = '0',
    `sort` = 10,
    `del_flag` = '0',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M13-初始化mobile-h5页面配置'
WHERE `page_code` = 'mobile-h5';

-- 初始化：mobile-h1（二维码路径迁移到公共路径）
INSERT INTO `mobile_page_config` (
  `page_code`, `page_name`, `service_phone`, `wechat_qr_url`,
  `nav_home_url`, `nav_query_url`, `nav_batch_url`, `nav_profile_url`, `result_back_url`,
  `status`, `sort`, `del_flag`, `create_by`, `create_time`, `remark`
)
SELECT 'mobile-h1', '手机页H1', '13027616171', '/mobile-h5/assets/icons/customer-wechat-h1.png',
       '/', '/?tab=query', '/batch/', '/profile/', '/',
       '0', 20, '0', 'admin', NOW(), 'M13-初始化mobile-h1页面配置'
WHERE NOT EXISTS (
  SELECT 1 FROM `mobile_page_config` WHERE `page_code` = 'mobile-h1'
);

UPDATE `mobile_page_config`
SET `page_name` = '手机页H1',
    `service_phone` = '13027616171',
    `wechat_qr_url` = '/mobile-h5/assets/icons/customer-wechat-h1.png',
    `nav_home_url` = '/',
    `nav_query_url` = '/?tab=query',
    `nav_batch_url` = '/batch/',
    `nav_profile_url` = '/profile/',
    `result_back_url` = '/',
    `status` = '0',
    `sort` = 20,
    `del_flag` = '0',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M13-初始化mobile-h1页面配置'
WHERE `page_code` = 'mobile-h1';

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
SELECT @mobile_menu_default_id, '手机端管理', 0, 9, 'mobile', '', '', 'mobileClient',
       1, 0, 'M', '0', '0', NULL, 'component', 'admin', NOW(), 'M13-手机端目录兜底'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` WHERE `parent_id` = 0 AND `path` = 'mobile'
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
SELECT 900100000205, '手机页配置', @mobile_menu_id, 4, 'mobilePageConfig', 'server/web/mobilePageConfig/index', '', 'mobilePageConfig',
       1, 0, 'C', '0', '0', 'server:mobilePageConfig:list', 'tool', 'admin', NOW(), 'M13-手机页配置菜单'
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_menu`
  WHERE `menu_id` = 900100000205
     OR (`parent_id` = @mobile_menu_id AND `path` = 'mobilePageConfig')
);

UPDATE `sys_menu`
SET `menu_name` = '手机页配置',
    `parent_id` = @mobile_menu_id,
    `order_num` = 4,
    `path` = 'mobilePageConfig',
    `component` = 'server/web/mobilePageConfig/index',
    `route_name` = 'mobilePageConfig',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:mobilePageConfig:list',
    `icon` = 'tool',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M13-手机页配置菜单'
WHERE `menu_id` = 900100000205
   OR (`parent_id` = @mobile_menu_id AND `path` = 'mobilePageConfig');

SET @mobile_page_config_menu_id := (
  SELECT `menu_id`
  FROM `sys_menu`
  WHERE `parent_id` = @mobile_menu_id
    AND `path` = 'mobilePageConfig'
  ORDER BY `menu_id`
  LIMIT 1
);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 900100001801, '手机页配置查询', @mobile_page_config_menu_id, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:mobilePageConfig:query', '#', 'admin', NOW(), 'M13-手机页配置查询按钮'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001801 OR (`parent_id` = @mobile_page_config_menu_id AND `perms` = 'server:mobilePageConfig:query'));

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 900100001802, '手机页配置新增', @mobile_page_config_menu_id, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:mobilePageConfig:add', '#', 'admin', NOW(), 'M13-手机页配置新增按钮'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001802 OR (`parent_id` = @mobile_page_config_menu_id AND `perms` = 'server:mobilePageConfig:add'));

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 900100001803, '手机页配置修改', @mobile_page_config_menu_id, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:mobilePageConfig:edit', '#', 'admin', NOW(), 'M13-手机页配置修改按钮'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001803 OR (`parent_id` = @mobile_page_config_menu_id AND `perms` = 'server:mobilePageConfig:edit'));

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 900100001804, '手机页配置删除', @mobile_page_config_menu_id, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:mobilePageConfig:remove', '#', 'admin', NOW(), 'M13-手机页配置删除按钮'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001804 OR (`parent_id` = @mobile_page_config_menu_id AND `perms` = 'server:mobilePageConfig:remove'));

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN `sys_menu` m
  ON m.menu_id = @mobile_page_config_menu_id
  OR (m.parent_id = @mobile_page_config_menu_id AND m.perms IN (
      'server:mobilePageConfig:query',
      'server:mobilePageConfig:add',
      'server:mobilePageConfig:edit',
      'server:mobilePageConfig:remove'
  ))
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key = 'admin'
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;
