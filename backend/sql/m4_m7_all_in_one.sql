-- AUTO-GENERATED: merged migration script
-- order: m4_mark_migration.sql -> m5_free_query_menu.sql -> m6_free_query_dict.sql -> m7_mobile_menu_group.sql
-- NOTE: keep source scripts in sync when changing migration logic.

-- >>> BEGIN m4_mark_migration.sql
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M4 功能迁移：业务表
-- =========================================================

CREATE TABLE IF NOT EXISTS `mark_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `request_no` varchar(64) DEFAULT NULL COMMENT '请求幂等号',
  `user_id` bigint(20) NOT NULL COMMENT '下单用户ID',
  `assigned_agent_id` bigint(20) DEFAULT NULL COMMENT '分配代理ID',
  `platform_code` varchar(64) NOT NULL COMMENT '平台编码',
  `platform_name` varchar(128) DEFAULT NULL COMMENT '平台名称',
  `total_count` int(11) NOT NULL DEFAULT '0' COMMENT '总明细数',
  `success_count` int(11) NOT NULL DEFAULT '0' COMMENT '成功数',
  `failed_count` int(11) NOT NULL DEFAULT '0' COMMENT '失败数',
  `total_amount` bigint(20) NOT NULL DEFAULT '0' COMMENT '扣费总额',
  `refund_amount` bigint(20) NOT NULL DEFAULT '0' COMMENT '退款总额',
  `order_status` char(1) NOT NULL DEFAULT '0' COMMENT '订单状态（0待处理 1处理中 2已完成 3已取消）',
  `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mark_order_order_no` (`order_no`),
  UNIQUE KEY `uk_mark_order_user_request` (`user_id`,`request_no`),
  KEY `idx_mark_order_user_id` (`user_id`),
  KEY `idx_mark_order_assigned_agent_id` (`assigned_agent_id`),
  KEY `idx_mark_order_order_status` (`order_status`),
  KEY `idx_mark_order_platform_code` (`platform_code`),
  KEY `idx_mark_order_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='迁移订单表';

CREATE TABLE IF NOT EXISTS `mark_order_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `phone` varchar(32) NOT NULL COMMENT '号码',
  `unit_price` bigint(20) NOT NULL DEFAULT '0' COMMENT '单价',
  `item_amount` bigint(20) NOT NULL DEFAULT '0' COMMENT '明细金额',
  `process_status` char(1) NOT NULL DEFAULT '0' COMMENT '处理状态（0待处理 1成功 2失败）',
  `process_result` varchar(255) DEFAULT NULL COMMENT '处理结果',
  `process_note` varchar(500) DEFAULT NULL COMMENT '处理备注',
  `processed_by` varchar(64) DEFAULT NULL COMMENT '处理人',
  `processed_time` datetime DEFAULT NULL COMMENT '处理时间',
  `refunded` char(1) NOT NULL DEFAULT '0' COMMENT '是否已退款（0否 1是）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_mark_order_item_order_id` (`order_id`),
  KEY `idx_mark_order_item_phone` (`phone`),
  KEY `idx_mark_order_item_process_status` (`process_status`),
  KEY `idx_mark_order_item_order_status` (`order_id`,`process_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='迁移订单明细表';

CREATE TABLE IF NOT EXISTS `mark_wallet_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `order_id` bigint(20) DEFAULT NULL COMMENT '订单ID',
  `order_item_id` bigint(20) DEFAULT NULL COMMENT '订单明细ID',
  `biz_type` varchar(32) NOT NULL COMMENT '流水业务类型（DEDUCT/REFUND/ADJUST）',
  `change_amount` bigint(20) NOT NULL COMMENT '变动金额（正负）',
  `balance_before` bigint(20) NOT NULL COMMENT '变动前余额',
  `balance_after` bigint(20) NOT NULL COMMENT '变动后余额',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_mark_wallet_log_user_id` (`user_id`),
  KEY `idx_mark_wallet_log_order_id` (`order_id`),
  KEY `idx_mark_wallet_log_order_item_id` (`order_item_id`),
  KEY `idx_mark_wallet_log_biz_type` (`biz_type`),
  KEY `idx_mark_wallet_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='迁移钱包流水表';

CREATE TABLE IF NOT EXISTS `mark_user_platform_price` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `platform_code` varchar(64) NOT NULL COMMENT '平台编码',
  `platform_name` varchar(128) DEFAULT NULL COMMENT '平台名称',
  `unit_price` bigint(20) NOT NULL DEFAULT '1' COMMENT '单价（每号码积分）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mark_user_platform_price` (`user_id`,`platform_code`),
  KEY `idx_mark_user_platform_price_platform_code` (`platform_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户平台单价表';

CREATE TABLE IF NOT EXISTS `mark_govern_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_key` varchar(100) NOT NULL COMMENT '规则键',
  `rule_value` text COMMENT '规则值',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mark_govern_rule_key` (`rule_key`),
  KEY `idx_mark_govern_rule_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='治理规则表';

CREATE TABLE IF NOT EXISTS `mark_arbitration_case` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` bigint(20) DEFAULT NULL COMMENT '订单ID',
  `order_item_id` bigint(20) DEFAULT NULL COMMENT '订单明细ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `agent_id` bigint(20) DEFAULT NULL COMMENT '代理ID',
  `case_status` char(1) NOT NULL DEFAULT '0' COMMENT '仲裁状态（0待处理 1已裁决 2已驳回）',
  `issue_desc` varchar(1000) DEFAULT NULL COMMENT '问题描述',
  `evidence_text` text COMMENT '证据内容',
  `decision_text` text COMMENT '裁决内容',
  `decided_by` varchar(64) DEFAULT NULL COMMENT '裁决人',
  `decided_time` datetime DEFAULT NULL COMMENT '裁决时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_mark_arbitration_case_order_id` (`order_id`),
  KEY `idx_mark_arbitration_case_order_item_id` (`order_item_id`),
  KEY `idx_mark_arbitration_case_user_id` (`user_id`),
  KEY `idx_mark_arbitration_case_agent_id` (`agent_id`),
  KEY `idx_mark_arbitration_case_case_status` (`case_status`),
  KEY `idx_mark_arbitration_case_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仲裁工单表';

-- =========================================================
-- M4 功能迁移：默认治理规则
-- =========================================================

INSERT INTO `mark_govern_rule` (`rule_name`, `rule_key`, `rule_value`, `status`, `create_by`, `create_time`, `remark`)
SELECT '失败自动退款开关', 'AUTO_REFUND_ON_FAIL', 'true', '0', 'admin', NOW(), '失败自动退款总开关'
WHERE NOT EXISTS (
  SELECT 1 FROM `mark_govern_rule` WHERE `rule_key` = 'AUTO_REFUND_ON_FAIL'
);

INSERT INTO `mark_govern_rule` (`rule_name`, `rule_key`, `rule_value`, `status`, `create_by`, `create_time`, `remark`)
SELECT '代理处理超时分钟数', 'AGENT_PROCESS_TIMEOUT_MINUTES', '120', '0', 'admin', NOW(), '用于治理与审计提醒'
WHERE NOT EXISTS (
  SELECT 1 FROM `mark_govern_rule` WHERE `rule_key` = 'AGENT_PROCESS_TIMEOUT_MINUTES'
);

-- =========================================================
-- 标记业务管理功能迁移：角色
-- =========================================================

INSERT INTO `sys_role` (
  `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`,
  `status`, `del_flag`, `create_by`, `create_time`, `remark`
)
SELECT '标记用户（下单）', 'user', 30, '2', 1, 1, '0', '0', 'admin', NOW(), 'M4迁移-用户角色'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_role` WHERE `role_key` = 'user'
);

INSERT INTO `sys_role` (
  `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`,
  `status`, `del_flag`, `create_by`, `create_time`, `remark`
)
SELECT '标记代理（处理）', 'agent', 31, '1', 1, 1, '0', '0', 'admin', NOW(), 'M4迁移-代理角色'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_role` WHERE `role_key` = 'agent'
);
UPDATE `sys_role` SET `data_scope` = '1' WHERE `role_key` = 'agent';
UPDATE `sys_role`
SET `role_name` = '标记用户（下单）',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `role_key` = 'user';
UPDATE `sys_role`
SET `role_name` = '标记代理（处理）',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `role_key` = 'agent';

-- =========================================================
-- 标记业务管理功能迁移：菜单与权限
-- =========================================================

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000108, '代理账户', 900100000104, 1, 'agentAccount', 'system/user/index', '', '',
       1, 0, 'C', '0', '0', 'system:user:list', 'peoples', 'admin', NOW(), '迁移-代理账户菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000108);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000109, '代理账户', 900100000001, 1, 'agentAccount', 'system/user/index', '', '',
       1, 0, 'C', '0', '0', 'system:user:list', 'peoples', 'admin', NOW(), '迁移-代理直连账户菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000109);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000001, '标记业务管理', 0, 8, 'mark', '', '', '',
       1, 0, 'M', '0', '0', '', 'guide', 'admin', NOW(), '标记业务管理目录'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000001);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000101, '用户订单', 900100000001, 3, 'userOrder', 'server/mark/user/index', '', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), '迁移-用户订单菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000101);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000102, '用户钱包', 900100000001, 4, 'userWallet', 'server/mark/user/wallet', '', '',
       1, 0, 'C', '0', '0', 'server:markUser:wallet:list', 'money', 'admin', NOW(), '迁移-用户钱包菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000102);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000103, '代理处理', 900100000001, 2, 'agentOrder', 'server/mark/agent/index', '', '',
       1, 0, 'C', '0', '0', 'server:markAgent:order:list', 'peoples', 'admin', NOW(), '迁移-代理处理菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000103);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000104, '管理端', 900100000001, 1, 'markAdmin', '', '', '',
       1, 0, 'M', '0', '0', '', 'system', 'admin', NOW(), '迁移-管理端目录'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000104);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000105, '治理规则', 900100000104, 2, 'rule', 'server/mark/admin/rule', '', '',
       1, 0, 'C', '0', '0', 'server:markAdmin:rule:list', 'edit', 'admin', NOW(), '迁移-治理规则菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000105);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000106, '仲裁工单', 900100000104, 3, 'case', 'server/mark/admin/case', '', '',
       1, 0, 'C', '0', '0', 'server:markAdmin:case:list', 'message', 'admin', NOW(), '迁移-仲裁工单菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000106);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000107, '审计看板', 900100000104, 4, 'audit', 'server/mark/admin/audit', '', '',
       1, 0, 'C', '0', '0', 'server:markAdmin:audit:order:list', 'monitor', 'admin', NOW(), '迁移-审计看板菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000107);

UPDATE `sys_menu` SET `order_num` = 1 WHERE `menu_id` = 900100000108;
UPDATE `sys_menu`
SET `menu_name` = '标记业务管理',
    `parent_id` = 0,
    `order_num` = 8,
    `path` = 'mark',
    `component` = '',
    `menu_type` = 'M',
    `visible` = '0',
    `status` = '0',
    `perms` = '',
    `icon` = 'guide',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = '标记业务管理目录'
WHERE `menu_id` = 900100000001;
UPDATE `sys_menu`
SET `menu_name` = '代理账户',
    `parent_id` = 900100000001,
    `path` = 'agentAccount',
    `component` = 'system/user/index',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'system:user:list',
    `icon` = 'peoples',
    `order_num` = 1
WHERE `menu_id` = 900100000109;
UPDATE `sys_menu` SET `order_num` = 2 WHERE `menu_id` = 900100000105;
UPDATE `sys_menu` SET `order_num` = 3 WHERE `menu_id` = 900100000106;
UPDATE `sys_menu` SET `order_num` = 4 WHERE `menu_id` = 900100000107;
UPDATE `sys_menu` SET `order_num` = 1 WHERE `menu_id` = 900100000104;
UPDATE `sys_menu` SET `order_num` = 2 WHERE `menu_id` = 900100000103;
UPDATE `sys_menu` SET `order_num` = 3 WHERE `menu_id` = 900100000101;
UPDATE `sys_menu` SET `order_num` = 4 WHERE `menu_id` = 900100000102;

-- 用户端功能权限
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001101, '用户订单查询', 900100000101, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markUser:order:query', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001101);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001102, '用户订单新增', 900100000101, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markUser:order:add', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001102);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001103, '用户订单预查询', 900100000101, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markUser:order:precheck', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001103);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001104, '用户订单提交消除', 900100000101, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markUser:order:clear', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001104);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001201, '钱包流水查询', 900100000102, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markUser:wallet:log:list', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001201);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001202, '平台单价查询', 900100000102, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markUser:price:list', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001202);

-- 代理端功能权限
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001301, '代理订单查询', 900100000103, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAgent:order:query', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001301);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001302, '代理明细回填', 900100000103, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAgent:item:feedback', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001302);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001303, '代理整单完成', 900100000103, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAgent:order:complete', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001303);

-- 管理端功能权限
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001401, '治理规则查询', 900100000105, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAdmin:rule:query', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001401);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001402, '治理规则新增', 900100000105, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAdmin:rule:add', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001402);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001403, '治理规则修改', 900100000105, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAdmin:rule:edit', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001403);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001404, '治理规则删除', 900100000105, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAdmin:rule:remove', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001404);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001501, '仲裁工单查询', 900100000106, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAdmin:case:query', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001501);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001502, '仲裁工单新增', 900100000106, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAdmin:case:add', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001502);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001503, '仲裁工单修改', 900100000106, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAdmin:case:edit', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001503);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001601, '订单审计查询', 900100000107, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAdmin:audit:order:list', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001601);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 900100001602, '流水审计查询', 900100000107, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'server:markAdmin:audit:wallet:list', '#', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100001602);

-- =========================================================
-- M4 功能迁移：角色与菜单绑定（幂等）
-- =========================================================
-- admin 角色：管理端 + 代理端（不包含用户端）
DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON rm.role_id = r.role_id
WHERE r.role_key = 'admin'
  AND rm.menu_id IN (
    900100000101, 900100000102,
    900100001101, 900100001102, 900100001103, 900100001104, 900100001201, 900100001202
  );
-- admin 角色：全量权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN (
  SELECT 900100000001 AS menu_id UNION ALL
  SELECT 900100000103 UNION ALL
  SELECT 900100000104 UNION ALL SELECT 900100000105 UNION ALL SELECT 900100000106 UNION ALL SELECT 900100000107 UNION ALL SELECT 900100000108 UNION ALL
  SELECT 900100001301 UNION ALL SELECT 900100001302 UNION ALL SELECT 900100001303 UNION ALL
  SELECT 900100001401 UNION ALL SELECT 900100001402 UNION ALL SELECT 900100001403 UNION ALL SELECT 900100001404 UNION ALL
  SELECT 900100001501 UNION ALL SELECT 900100001502 UNION ALL SELECT 900100001503 UNION ALL
  SELECT 900100001601 UNION ALL SELECT 900100001602
) m
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key = 'admin' AND rm.role_id IS NULL;

-- common + user 角色：用户端权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN (
  SELECT 900100000001 AS menu_id UNION ALL
  SELECT 900100000101 UNION ALL SELECT 900100000102 UNION ALL
  SELECT 900100001101 UNION ALL SELECT 900100001102 UNION ALL SELECT 900100001103 UNION ALL SELECT 900100001104 UNION ALL
  SELECT 900100001201 UNION ALL SELECT 900100001202
) m
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key IN ('common', 'user') AND rm.role_id IS NULL;

-- agent 角色：代理端权限
DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON rm.role_id = r.role_id
WHERE r.role_key = 'agent'
  AND rm.menu_id IN (900100000104, 900100000108, 1002);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN (
  SELECT 900100000001 AS menu_id UNION ALL
  SELECT 900100000109 UNION ALL
  SELECT 900100000103 UNION ALL
  SELECT 900100001301 UNION ALL SELECT 900100001302 UNION ALL SELECT 900100001303 UNION ALL
  SELECT 1000 UNION ALL SELECT 1001 UNION ALL
  SELECT 1003 UNION ALL
  SELECT 92281941572000191
) m
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key = 'agent' AND rm.role_id IS NULL;

-- =========================================================
-- M4 扩展（合并原 M9）：管理端核心功能平铺到“标记业务管理”二级菜单
-- 目标：
-- 1) 代理账户 / 治理规则 / 仲裁工单 / 审计看板 直接挂到 标记业务管理(900100000001) 下
-- 2) admin 不显示：管理端目录、旧代理账户、代理处理、用户订单、用户钱包
-- =========================================================
SET @mark_root_menu_id := 900100000001;

-- 兜底：若菜单不存在则补齐
INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000108, '代理账户', @mark_root_menu_id, 1, 'agentAccount', 'system/user/index', '', '',
       1, 0, 'C', '0', '0', 'system:user:list', 'peoples', 'admin', NOW(), 'M4-代理账户二级菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000108);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000105, '治理规则', @mark_root_menu_id, 2, 'rule', 'server/mark/admin/rule', '', '',
       1, 0, 'C', '0', '0', 'server:markAdmin:rule:list', 'edit', 'admin', NOW(), 'M4-治理规则二级菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000105);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000106, '仲裁工单', @mark_root_menu_id, 3, 'case', 'server/mark/admin/case', '', '',
       1, 0, 'C', '0', '0', 'server:markAdmin:case:list', 'message', 'admin', NOW(), 'M4-仲裁工单二级菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000106);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000107, '审计看板', @mark_root_menu_id, 4, 'audit', 'server/mark/admin/audit', '', '',
       1, 0, 'C', '0', '0', 'server:markAdmin:audit:order:list', 'monitor', 'admin', NOW(), 'M4-审计看板二级菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000107);

-- 统一菜单挂载关系（幂等）
UPDATE `sys_menu`
SET `menu_name` = '代理账户',
    `parent_id` = @mark_root_menu_id,
    `order_num` = 1,
    `path` = 'agentAccount',
    `component` = 'system/user/index',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'system:user:list',
    `icon` = 'peoples',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M4-代理账户二级菜单'
WHERE `menu_id` = 900100000108;

UPDATE `sys_menu`
SET `menu_name` = '治理规则',
    `parent_id` = @mark_root_menu_id,
    `order_num` = 2,
    `path` = 'rule',
    `component` = 'server/mark/admin/rule',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:markAdmin:rule:list',
    `icon` = 'edit',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M4-治理规则二级菜单'
WHERE `menu_id` = 900100000105;

UPDATE `sys_menu`
SET `menu_name` = '仲裁工单',
    `parent_id` = @mark_root_menu_id,
    `order_num` = 3,
    `path` = 'case',
    `component` = 'server/mark/admin/case',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:markAdmin:case:list',
    `icon` = 'message',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M4-仲裁工单二级菜单'
WHERE `menu_id` = 900100000106;

UPDATE `sys_menu`
SET `menu_name` = '审计看板',
    `parent_id` = @mark_root_menu_id,
    `order_num` = 4,
    `path` = 'audit',
    `component` = 'server/mark/admin/audit',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:markAdmin:audit:order:list',
    `icon` = 'monitor',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M4-审计看板二级菜单'
WHERE `menu_id` = 900100000107;

-- 标记业务管理保留 8 个二级菜单可见：
-- 管理端：代理账户(900100000108) / 治理规则 / 仲裁工单 / 审计看板
-- 代理端：代理处理(900100000103) / 代理账户(900100000109)
-- 用户端：用户订单(900100000101) / 用户钱包(900100000102)
UPDATE `sys_menu`
SET `visible` = '1',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `parent_id` = @mark_root_menu_id
  AND `menu_id` NOT IN (900100000108, 900100000105, 900100000106, 900100000107, 900100000103, 900100000109, 900100000101, 900100000102);

UPDATE `sys_menu`
SET `visible` = '0',
    `status` = '0',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` IN (900100000108, 900100000105, 900100000106, 900100000107);
UPDATE `sys_menu`
SET `visible` = '0',
    `status` = '0',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` IN (900100000103, 900100000109);
UPDATE `sys_menu`
SET `visible` = '0',
    `status` = '0',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` IN (900100000101, 900100000102);

-- admin 角色补齐新二级菜单与功能点授权
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN (
  SELECT 900100000108 AS menu_id UNION ALL
  SELECT 900100000105 UNION ALL
  SELECT 900100000106 UNION ALL
  SELECT 900100000107 UNION ALL
  SELECT 900100001401 UNION ALL
  SELECT 900100001402 UNION ALL
  SELECT 900100001403 UNION ALL
  SELECT 900100001404 UNION ALL
  SELECT 900100001501 UNION ALL
  SELECT 900100001502 UNION ALL
  SELECT 900100001503 UNION ALL
  SELECT 900100001601 UNION ALL
  SELECT 900100001602
) m
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key = 'admin'
  AND rm.role_id IS NULL;

-- admin 角色移除旧入口（与当前菜单策略保持一致）
DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON rm.role_id = r.role_id
WHERE r.role_key = 'admin'
  AND rm.menu_id IN (
    900100000104, -- 管理端目录
    900100000109, -- 旧代理账户（直连）
    900100000103, -- 代理处理
    900100000101, -- 用户订单
    900100000102, -- 用户钱包
    900100001301,
    900100001302,
    900100001303,
    900100001101,
    900100001102,
    900100001103,
    900100001104,
    900100001201,
    900100001202
  );

-- =========================================================
-- M4 增补：用户端平台菜单入口（高频拦截/泰迪高频/泰迪二次/360首次/360二次/电话邦/腾讯）
-- 目标：
-- 1) 7 个平台入口直接显示在“标记业务管理”二级菜单（用户订单/用户钱包同级位置）
-- 2) 隐藏“用户订单”菜单
-- 3) user/common 角色可见平台入口，admin/agent 不展示平台入口
-- =========================================================

-- 兜底插入 7 个平台菜单
INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000120, '高频拦截', @mark_root_menu_id, 3, 'mobileGaopin', 'server/mark/user/index', '{"platformCode":"mobile_gaopin"}', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), 'M4-用户端平台菜单-高频拦截'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000120);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000121, '泰迪高频', @mark_root_menu_id, 4, 'tdGaopin', 'server/mark/user/index', '{"platformCode":"td_gaopin"}', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), 'M4-用户端平台菜单-泰迪高频'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000121);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000122, '泰迪二次', @mark_root_menu_id, 5, 'tdSecond', 'server/mark/user/index', '{"platformCode":"td_second"}', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), 'M4-用户端平台菜单-泰迪二次'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000122);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000123, '360首次', @mark_root_menu_id, 6, 'qihuFirst', 'server/mark/user/index', '{"platformCode":"qihu_first"}', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), 'M4-用户端平台菜单-360首次'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000123);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000124, '360二次', @mark_root_menu_id, 7, 'qihuSecond', 'server/mark/user/index', '{"platformCode":"qihu_second"}', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), 'M4-用户端平台菜单-360二次'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000124);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000125, '电话邦', @mark_root_menu_id, 8, 'dianhuabang', 'server/mark/user/index', '{"platformCode":"dianhuabang"}', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), 'M4-用户端平台菜单-电话邦'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000125);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000126, '腾讯', @mark_root_menu_id, 9, 'tencentMark', 'server/mark/user/index', '{"platformCode":"tencent_mark"}', '',
       1, 0, 'C', '0', '0', 'server:markUser:order:list', 'list', 'admin', NOW(), 'M4-用户端平台菜单-腾讯'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000126);

-- 统一修正平台菜单配置（幂等）
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
    `remark` = 'M4-用户端平台菜单-高频拦截'
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
    `remark` = 'M4-用户端平台菜单-泰迪高频'
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
    `remark` = 'M4-用户端平台菜单-泰迪二次'
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
    `remark` = 'M4-用户端平台菜单-360首次'
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
    `remark` = 'M4-用户端平台菜单-360二次'
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
    `remark` = 'M4-用户端平台菜单-电话邦'
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
    `remark` = 'M4-用户端平台菜单-腾讯'
WHERE `menu_id` = 900100000126;

-- “用户订单”菜单改为隐藏；用户钱包保留可见
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

-- common/user 角色补齐 7 个平台入口，并移除“用户订单”菜单入口
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
  SELECT 900100000126
) m
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key IN ('common', 'user')
  AND rm.role_id IS NULL;

DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON rm.role_id = r.role_id
WHERE r.role_key IN ('common', 'user')
  AND rm.menu_id IN (900100000101);

-- admin/agent 不展示用户端 7 个平台入口
DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON rm.role_id = r.role_id
WHERE r.role_key IN ('admin', 'agent')
  AND rm.menu_id IN (900100000120, 900100000121, 900100000122, 900100000123, 900100000124, 900100000125, 900100000126);

SET FOREIGN_KEY_CHECKS = 1;

-- <<< END m4_mark_migration.sql

-- >>> BEGIN m5_free_query_menu.sql
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M5：管理端补充“免费查询 / 日志管理”菜单（幂等）
-- 归属目录：号码查询（menu_id = 92281941572000140）
-- =========================================================

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000201, '免费查询', 92281941572000140, 12, 'apiquery', 'server/web/apiquery', '', 'freeApiQuery',
       1, 0, 'C', '0', '0', 'server:freeQuery:page', 'search', 'admin', NOW(), 'M5-免费查询管理端菜单'
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_menu`
  WHERE `menu_id` = 900100000201
     OR (`parent_id` = 92281941572000140 AND `path` = 'apiquery')
);

INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000202, '日志管理', 92281941572000140, 13, 'apilog', 'server/web/apilog', '', 'freeApiLog',
       1, 0, 'C', '0', '0', 'server:freeQuery:log:list', 'form', 'admin', NOW(), 'M5-免费查询日志菜单'
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_menu`
  WHERE `menu_id` = 900100000202
     OR (`parent_id` = 92281941572000140 AND `path` = 'apilog')
);

-- 若菜单已存在（历史手工配置等），统一矫正为目标配置
UPDATE `sys_menu`
SET `menu_name` = '免费查询',
    `order_num` = 12,
    `component` = 'server/web/apiquery',
    `route_name` = 'freeApiQuery',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:freeQuery:page',
    `icon` = 'search',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M5-免费查询管理端菜单'
WHERE `parent_id` = 92281941572000140
  AND `path` = 'apiquery';

UPDATE `sys_menu`
SET `menu_name` = '日志管理',
    `order_num` = 13,
    `component` = 'server/web/apilog',
    `route_name` = 'freeApiLog',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'server:freeQuery:log:list',
    `icon` = 'form',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M5-免费查询日志菜单'
WHERE `parent_id` = 92281941572000140
  AND `path` = 'apilog';

-- admin 角色绑定（按路径取 menu_id，适配历史手工创建或不同ID）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN `sys_menu` m
  ON m.parent_id = 92281941572000140
 AND m.path IN ('apiquery', 'apilog')
LEFT JOIN `sys_role_menu` rm
  ON rm.role_id = r.role_id
 AND rm.menu_id = m.menu_id
WHERE r.role_key = 'admin'
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- <<< END m5_free_query_menu.sql

-- >>> BEGIN m6_free_query_dict.sql
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M6：补齐免费查询字典配置（幂等）
-- dict_type: free_query_config
-- =========================================================

INSERT INTO `sys_dict_type` (
  `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`
)
SELECT '免费查询配置', 'free_query_config', '0', 'admin', NOW(), '免费查询每日额度与平台限制配置'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'free_query_config'
);

-- 若类型已存在，确保可用
UPDATE `sys_dict_type`
SET `dict_name` = '免费查询配置',
    `status` = '0',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = '免费查询每日额度与平台限制配置'
WHERE `dict_type` = 'free_query_config';

INSERT INTO `sys_dict_data` (
  `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
  `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`
)
SELECT 1, 'daily_limit', '20', 'free_query_config',
       '', 'default', 'N', '0', 'admin', NOW(), '单IP每日免费查询次数上限'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data`
  WHERE `dict_type` = 'free_query_config' AND `dict_label` = 'daily_limit'
);

INSERT INTO `sys_dict_data` (
  `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
  `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`
)
SELECT 2, 'over_limit_msg', '当前IP今日免费查询次数已达上限，请添加客服微信查询。', 'free_query_config',
       '', 'default', 'N', '0', 'admin', NOW(), '单IP额度用尽提示文案'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data`
  WHERE `dict_type` = 'free_query_config' AND `dict_label` = 'over_limit_msg'
);

INSERT INTO `sys_dict_data` (
  `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
  `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`
)
SELECT 3, 'daily_all_limit', '2000', 'free_query_config',
       '', 'default', 'N', '0', 'admin', NOW(), '平台每日总免费查询次数上限'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data`
  WHERE `dict_type` = 'free_query_config' AND `dict_label` = 'daily_all_limit'
);

INSERT INTO `sys_dict_data` (
  `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
  `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`
)
SELECT 4, 'daily_device_limit', '20', 'free_query_config',
       '', 'default', 'N', '0', 'admin', NOW(), '单设备每日免费查询次数上限'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data`
  WHERE `dict_type` = 'free_query_config' AND `dict_label` = 'daily_device_limit'
);

INSERT INTO `sys_dict_data` (
  `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
  `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`
)
SELECT 5, 'device_over_limit_msg', '当前设备今日免费查询次数已达上限，请明日再试。', 'free_query_config',
       '', 'default', 'N', '0', 'admin', NOW(), '单设备额度用尽提示文案'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data`
  WHERE `dict_type` = 'free_query_config' AND `dict_label` = 'device_over_limit_msg'
);

INSERT INTO `sys_dict_data` (
  `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
  `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`
)
SELECT 6, 'require_device_id', '0', 'free_query_config',
       '', 'default', 'N', '0', 'admin', NOW(), '是否强制要求携带设备标识（1是，0否）'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data`
  WHERE `dict_type` = 'free_query_config' AND `dict_label` = 'require_device_id'
);

INSERT INTO `sys_dict_data` (
  `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
  `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`
)
SELECT 7, 'require_device_id_msg', '当前设备标识缺失，请刷新页面后重试。', 'free_query_config',
       '', 'default', 'N', '0', 'admin', NOW(), '强制设备标识时的提示文案'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data`
  WHERE `dict_type` = 'free_query_config' AND `dict_label` = 'require_device_id_msg'
);

INSERT INTO `sys_dict_data` (
  `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
  `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`
)
SELECT 8, 'disabled_platforms', '联通管家', 'free_query_config',
       '', 'default', 'N', '0', 'admin', NOW(), '免费查询禁用平台名称，英文逗号分隔'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data`
  WHERE `dict_type` = 'free_query_config' AND `dict_label` = 'disabled_platforms'
);

-- 历史默认值兼容迁移：仅将旧默认“泰迪熊,联通管家”调整为“联通管家”，不覆盖自定义配置
UPDATE `sys_dict_data`
SET `dict_value` = '联通管家',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `dict_type` = 'free_query_config'
  AND `dict_label` = 'disabled_platforms'
  AND `dict_value` = '泰迪熊,联通管家';

SET FOREIGN_KEY_CHECKS = 1;

-- <<< END m6_free_query_dict.sql

-- >>> BEGIN m7_mobile_menu_group.sql
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M7：新增独立一级“手机端”菜单，并将“免费查询/日志管理”迁移到其下（幂等）
-- 一级顺序：默认放在“标记业务管理”后一个顺位
-- =========================================================
SET @root_parent_id := 0;
SET @agent_order := (
  SELECT `order_num`
  FROM `sys_menu`
  WHERE `menu_id` = 900100000001
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

-- <<< END m7_mobile_menu_group.sql

