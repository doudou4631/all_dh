SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- M18: mark user notice (Phase 1.5)

CREATE TABLE IF NOT EXISTS `mark_user_notice` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `user_id` bigint(20) NOT NULL COMMENT 'user id',
  `notice_type` varchar(32) NOT NULL DEFAULT 'ORDER_AUDIT' COMMENT 'notice type',
  `title` varchar(128) NOT NULL COMMENT 'title',
  `content` varchar(500) DEFAULT NULL COMMENT 'content',
  `biz_type` varchar(32) DEFAULT NULL COMMENT 'biz type',
  `biz_id` bigint(20) DEFAULT NULL COMMENT 'biz id',
  `read_flag` char(1) NOT NULL DEFAULT '0' COMMENT '0 unread 1 read',
  `create_by` varchar(64) DEFAULT '' COMMENT 'creator',
  `create_time` datetime DEFAULT NULL COMMENT 'create time',
  `update_by` varchar(64) DEFAULT '' COMMENT 'updater',
  `update_time` datetime DEFAULT NULL COMMENT 'update time',
  `remark` varchar(500) DEFAULT NULL COMMENT 'remark',
  PRIMARY KEY (`id`),
  KEY `idx_mark_user_notice_user_read` (`user_id`,`read_flag`),
  KEY `idx_mark_user_notice_user_time` (`user_id`,`create_time`),
  KEY `idx_mark_user_notice_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mark user notice';

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000112, CONVERT(UNHEX('E68891E79A84E6B688E681AF') USING utf8mb4), 900100000001, 5, 'userNotice', 'server/mark/user/notice', '', '',
       1, 0, 'C', '0', '0', 'server:markUser:notice:list', 'message', 'admin', NOW(), 'M18-user-notice-menu'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000112);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100001310, CONVERT(UNHEX('E6B688E681AFE58897E8A1A8') USING utf8mb4), 900100000112, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markUser:notice:list', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001310);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100001311, CONVERT(UNHEX('E6B688E681AFE5B7B2E8AFBB') USING utf8mb4), 900100000112, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markUser:notice:read', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001311);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100001312, CONVERT(UNHEX('E5AEA1E6A0B8E7BB9FE8AEA1') USING utf8mb4), 900100000110, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAgent:audit:stats', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001312);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN (
  SELECT 900100000112 AS menu_id UNION ALL
  SELECT 900100001310 UNION ALL
  SELECT 900100001311
) m
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key IN ('admin', 'common', 'user', 'mark_user')
  AND rm.role_id IS NULL;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, 900100001312
FROM `sys_role` r
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = 900100001312
WHERE r.role_key IN ('admin', 'agent', 'mark_agent')
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;
