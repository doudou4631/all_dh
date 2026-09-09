-- 免费查询定时任务配置表
CREATE TABLE IF NOT EXISTS `free_query_schedule_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称',
  `task_type` varchar(20) NOT NULL COMMENT '任务类型（QUERY_WINDOW、DATA_IMPORT、DATA_CLEANUP）',
  `start_time` time NULL DEFAULT NULL COMMENT '查询开放开始时间',
  `end_time` time NULL DEFAULT NULL COMMENT '查询开放停止时间',
  `cron_expression` varchar(100) NULL DEFAULT NULL COMMENT 'Cron表达式',
  `data_source` varchar(500) NULL DEFAULT NULL COMMENT '导入数据源地址',
  `retention_days` int(11) NULL DEFAULT NULL COMMENT '清理任务数据保留天数',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_free_query_schedule_task_name` (`task_name`) USING BTREE,
  KEY `idx_free_query_schedule_type_status` (`task_type`, `status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_520_ci COMMENT = '免费查询定时任务配置表';

-- 免费查询设置一级菜单
-- 可在已有 verifynum 数据库中单独执行；重复执行不会重复插入。
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000161, '免费查询设置', 0, 8, 'freeQuerySettings', NULL, NULL, '',
       1, 0, 'M', '0', '0', NULL, 'component', 'admin', NOW(), '', NULL,
       '免费查询页面设置目录'
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `menu_id` = 94849186247000161
       OR (`parent_id` = 0 AND `path` = 'freeQuerySettings')
);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000162, '定时任务', 94849186247000161, 1, 'schedule',
       'server/freeQuerySettings/schedule/index', NULL, '', 1, 0, 'C', '0', '0',
       'freeQuery:schedule:list', 'job', 'admin', NOW(), '', NULL, '免费查询定时任务设置'
WHERE EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000161)
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000162);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000163, '数据统计', 94849186247000161, 3, 'statistics',
       'server/freeQuerySettings/statistics/index', NULL, '', 1, 0, 'C', '0', '0',
       'freeQuery:statistics:list', 'chart', 'admin', NOW(), '', NULL, '免费查询数据统计'
WHERE EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000161)
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000163);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000164, '消息通知', 94849186247000161, 5, 'notification',
       'server/freeQuerySettings/notification/index', NULL, '', 1, 0, 'C', '0', '0',
       'freeQuery:notification:list', 'message', 'admin', NOW(), '', NULL, '免费查询消息通知设置'
WHERE EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000161)
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000164);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000165, 'API接口', 94849186247000161, 6, 'api',
       'server/freeQuerySettings/api/index', NULL, '', 1, 0, 'C', '0', '0',
       'freeQuery:api:list', 'api_manager', 'admin', NOW(), '', NULL, '免费查询开放API设置'
WHERE EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000161)
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000165);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000166, '定时任务查询', 94849186247000162, 1, '#', '', NULL, '',
       1, 0, 'F', '0', '0', 'freeQuery:schedule:query', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000166);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000167, '定时任务新增', 94849186247000162, 2, '#', '', NULL, '',
       1, 0, 'F', '0', '0', 'freeQuery:schedule:add', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000167);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000168, '定时任务修改', 94849186247000162, 3, '#', '', NULL, '',
       1, 0, 'F', '0', '0', 'freeQuery:schedule:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000168);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000169, '定时任务删除', 94849186247000162, 4, '#', '', NULL, '',
       1, 0, 'F', '0', '0', 'freeQuery:schedule:remove', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000169);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000170, '定时任务启停', 94849186247000162, 5, '#', '', NULL, '',
       1, 0, 'F', '0', '0', 'freeQuery:schedule:changeStatus', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000170);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000171, '免费平台管理', 94849186247000161, 2, 'platform',
       'server/freeQuerySettings/platform/index', NULL, '', 1, 0, 'C', '0', '0',
       'freeQuery:platform:list', 'list', 'admin', NOW(), '', NULL, '免费查询平台展示与优先级管理'
WHERE EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000161)
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000171);

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

UPDATE `sys_menu` SET `order_num` = 2 WHERE `menu_id` = 94849186247000171;
UPDATE `sys_menu` SET `order_num` = 3 WHERE `menu_id` = 94849186247000163;
UPDATE `sys_menu` SET `order_num` = 5 WHERE `menu_id` = 94849186247000164;
UPDATE `sys_menu` SET `order_num` = 6 WHERE `menu_id` = 94849186247000165;

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000175, 'IP查询记录', 94849186247000161, 4, 'ipRecord',
       'server/freeQuerySettings/ipRecord/index', NULL, '', 1, 0, 'C', '0', '0',
       'freeQuery:ipRecord:list', 'log', 'admin', NOW(), '', NULL, '免费查询IP审计记录'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000175);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000176, 'IP查询记录详情', 94849186247000175, 1, '#', '', NULL, '',
       1, 0, 'F', '0', '0', 'freeQuery:ipRecord:query', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000176);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000177, 'IP访问策略', 94849186247000161, 5, 'ipPolicy',
       'server/freeQuerySettings/ipPolicy/index', NULL, '', 1, 0, 'C', '0', '0',
       'freeQuery:ipPolicy:list', 'lock', 'admin', NOW(), '', NULL, '免费查询IP访问策略设置'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000177);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`,
 `create_time`, `update_by`, `update_time`, `remark`)
SELECT 94849186247000178, 'IP访问策略修改', 94849186247000177, 1, '#', '', NULL, '',
       1, 0, 'F', '0', '0', 'freeQuery:ipPolicy:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 94849186247000178);

UPDATE `sys_menu` SET `order_num` = 5 WHERE `menu_id` = 94849186247000177;
UPDATE `sys_menu` SET `order_num` = 6 WHERE `menu_id` = 94849186247000164;
UPDATE `sys_menu` SET `order_num` = 7 WHERE `menu_id` = 94849186247000165;
