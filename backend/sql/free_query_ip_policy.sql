CREATE TABLE IF NOT EXISTS `free_query_ip_policy` (
  `id` bigint(20) NOT NULL COMMENT '配置ID',
  `daily_limit` int(11) NOT NULL DEFAULT 20 COMMENT '每个IP每日查询限额',
  `interval_ms` int(11) NOT NULL DEFAULT 500 COMMENT '同一IP查询间隔毫秒数，0表示不限制',
  `reset_time` time NOT NULL DEFAULT '00:00:00' COMMENT '每日配额重置时间',
  `exceed_message` varchar(500) NOT NULL COMMENT '超过每日限额提示文案',
  `whitelist_ips` text DEFAULT NULL COMMENT '白名单IP，一行一个',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_520_ci COMMENT = '免费查询IP访问策略';

INSERT INTO `free_query_ip_policy`
(`id`, `daily_limit`, `interval_ms`, `reset_time`, `exceed_message`, `whitelist_ips`, `create_by`, `create_time`)
SELECT 1, 20, 500, '00:00:00', '今日查询次数已用完，请明日再试。', '', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `free_query_ip_policy` WHERE `id` = 1);

UPDATE `sys_menu` SET `order_num` = 6 WHERE `menu_id` = 94849186247000164;
UPDATE `sys_menu` SET `order_num` = 7 WHERE `menu_id` = 94849186247000165;

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000177, 'IP访问策略', 94849186247000161, 5, 'ipPolicy',
       'server/freeQuerySettings/ipPolicy/index', NULL, '', 1, 0, 'C', '0', '0',
       'freeQuery:ipPolicy:list', 'lock', 'admin', NOW(), '', NULL, '免费查询IP限额与访问频率策略'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000177);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000178, 'IP访问策略修改', 94849186247000177, 1, '#', '', NULL, '',
       1, 0, 'F', '0', '0', 'freeQuery:ipPolicy:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000178);
