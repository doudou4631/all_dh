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
-- 代理管理功能迁移：角色
-- =========================================================

INSERT INTO `sys_role` (
  `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`,
  `status`, `del_flag`, `create_by`, `create_time`, `remark`
)
SELECT '迁移用户', 'user', 30, '2', 1, 1, '0', '0', 'admin', NOW(), 'M4迁移-用户角色'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_role` WHERE `role_key` = 'user'
);

INSERT INTO `sys_role` (
  `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`,
  `status`, `del_flag`, `create_by`, `create_time`, `remark`
)
SELECT '迁移代理', 'agent', 31, '1', 1, 1, '0', '0', 'admin', NOW(), 'M4迁移-代理角色'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_role` WHERE `role_key` = 'agent'
);
UPDATE `sys_role` SET `data_scope` = '1' WHERE `role_key` = 'agent';

-- =========================================================
-- 代理管理功能迁移：菜单与权限
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
SELECT 900100000001, '代理管理', 0, 8, 'mark', '', '', '',
       1, 0, 'M', '0', '0', '', 'guide', 'admin', NOW(), '代理管理目录'
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
    900100001101, 900100001102, 900100001201, 900100001202
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
  SELECT 900100001101 UNION ALL SELECT 900100001102 UNION ALL
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

SET FOREIGN_KEY_CHECKS = 1;
