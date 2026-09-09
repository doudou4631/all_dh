ALTER TABLE `user_platform_url_config`
  ADD COLUMN IF NOT EXISTS `free_query_status` char(1) NOT NULL DEFAULT '0' COMMENT '免费查询状态（0启用 1停用）' AFTER `status`,
  ADD COLUMN IF NOT EXISTS `free_query_sort` int(11) DEFAULT NULL COMMENT '免费查询优先级' AFTER `free_query_status`,
  ADD COLUMN IF NOT EXISTS `free_query_icon` varchar(255) DEFAULT NULL COMMENT '免费查询平台图标' AFTER `free_query_sort`,
  ADD COLUMN IF NOT EXISTS `free_query_description` varchar(500) DEFAULT NULL COMMENT '免费查询数据来源说明' AFTER `free_query_icon`;

UPDATE `user_platform_url_config`
SET `free_query_sort` = coalesce(`free_query_sort`, `sort`),
    `free_query_icon` = coalesce(`free_query_icon`, CASE `platform_name`
      WHEN '泰迪熊' THEN 'tdx.png' WHEN '360' THEN 'sll.png' WHEN '腾讯' THEN 'tx.png'
      WHEN '百度' THEN 'bd.png' WHEN '联通管家' THEN 'lt.png' WHEN '联通' THEN 'lt.png'
      WHEN '移动高频' THEN 'ydhigh.png' WHEN '电话邦' THEN 'dhb.png' WHEN '搜狗' THEN 'sghmt.png'
      WHEN '小米手机' THEN 'xm.png' WHEN '小米' THEN 'xm.png' ELSE '114.png' END),
    `free_query_description` = coalesce(`free_query_description`, concat(`platform_name`, '号码标记数据源'));

UPDATE `sys_menu` SET `order_num` = 3 WHERE `menu_id` = 94849186247000163;
UPDATE `sys_menu` SET `order_num` = 4 WHERE `menu_id` = 94849186247000164;
UPDATE `sys_menu` SET `order_num` = 5 WHERE `menu_id` = 94849186247000165;

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000171, '免费平台管理', 94849186247000161, 2, 'platform',
       'server/freeQuerySettings/platform/index', NULL, '', 1, 0, 'C', '0', '0',
       'freeQuery:platform:list', 'list', 'admin', NOW(), '', NULL, '免费查询平台展示与优先级管理'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000171);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000172, '免费平台修改', 94849186247000171, 1, '#', '', NULL, '',
       1, 0, 'F', '0', '0', 'freeQuery:platform:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000172);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000173, '免费平台启停', 94849186247000171, 2, '#', '', NULL, '',
       1, 0, 'F', '0', '0', 'freeQuery:platform:status', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000173);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000174, '免费平台排序', 94849186247000171, 3, '#', '', NULL, '',
       1, 0, 'F', '0', '0', 'freeQuery:platform:sort', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000174);
