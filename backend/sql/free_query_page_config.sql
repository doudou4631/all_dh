CREATE TABLE IF NOT EXISTS `free_query_page_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `page_code` varchar(32) NOT NULL COMMENT '页面编号',
  `phone` varchar(32) NOT NULL COMMENT '联系电话',
  `qr_code` varchar(500) NOT NULL COMMENT '二维码图片地址',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_free_query_page_code` (`page_code`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_520_ci COMMENT = '免费查询页面配置';

INSERT INTO `free_query_page_config`
(`page_code`, `phone`, `qr_code`, `create_by`, `create_time`)
SELECT '1', '13027616171', '/static/img/contact-qr.png', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `free_query_page_config` WHERE `page_code` = '1');

UPDATE `sys_menu` SET `order_num` = 4 WHERE `menu_id` = 94849186247000163;
UPDATE `sys_menu` SET `order_num` = 5 WHERE `menu_id` = 94849186247000175;
UPDATE `sys_menu` SET `order_num` = 6 WHERE `menu_id` = 94849186247000177;
UPDATE `sys_menu` SET `order_num` = 7 WHERE `menu_id` = 94849186247000164;
UPDATE `sys_menu` SET `order_num` = 8 WHERE `menu_id` = 94849186247000165;

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000179, '查询页面管理', 94849186247000161, 3, 'page',
       'server/freeQuerySettings/page/index', NULL, '', 1, 0, 'C', '0', '0',
       'freeQuery:page:list', 'link', 'admin', NOW(), '', NULL, '免费查询页面电话与二维码配置'
WHERE EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000161)
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000179);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000180, '查询页面修改', 94849186247000179, 1, '#', '', NULL, '',
       1, 0, 'F', '0', '0', 'freeQuery:page:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000179)
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000180);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT `role_id`, 94849186247000179 FROM `sys_role_menu` WHERE `menu_id` = 94849186247000161;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT `role_id`, 94849186247000180 FROM `sys_role_menu` WHERE `menu_id` = 94849186247000161;
