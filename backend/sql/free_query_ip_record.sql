ALTER TABLE `free_query_record`
  ADD COLUMN IF NOT EXISTS `query_phone` varchar(32) DEFAULT NULL COMMENT '查询号码' AFTER `user_agent`,
  ADD COLUMN IF NOT EXISTS `result_details` text DEFAULT NULL COMMENT '命中平台与标记类型JSON' AFTER `query_phone`;

SET @phone_index_exists = (
  SELECT count(*) FROM information_schema.statistics
  WHERE table_schema = database() AND table_name = 'free_query_record'
    AND index_name = 'idx_free_query_record_phone'
);
SET @phone_index_sql = IF(@phone_index_exists = 0,
  'CREATE INDEX idx_free_query_record_phone ON free_query_record (query_phone, create_time)',
  'SELECT 1');
PREPARE phone_index_stmt FROM @phone_index_sql;
EXECUTE phone_index_stmt;
DEALLOCATE PREPARE phone_index_stmt;

SET @ip_index_exists = (
  SELECT count(*) FROM information_schema.statistics
  WHERE table_schema = database() AND table_name = 'free_query_record'
    AND index_name = 'idx_free_query_record_ip'
);
SET @ip_index_sql = IF(@ip_index_exists = 0,
  'CREATE INDEX idx_free_query_record_ip ON free_query_record (ip_addr, create_time)',
  'SELECT 1');
PREPARE ip_index_stmt FROM @ip_index_sql;
EXECUTE ip_index_stmt;
DEALLOCATE PREPARE ip_index_stmt;

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
