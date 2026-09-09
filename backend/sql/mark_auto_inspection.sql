CREATE TABLE IF NOT EXISTS mark_inspection_policy (
  id bigint NOT NULL AUTO_INCREMENT,
  policy_name varchar(100) NOT NULL,
  platform_code varchar(64) NOT NULL,
  platform_name varchar(50) NOT NULL,
  interval_seconds int NOT NULL DEFAULT 30,
  interval_value int NOT NULL DEFAULT 30,
  interval_unit varchar(10) NOT NULL DEFAULT 'MINUTE',
  batch_limit int NOT NULL DEFAULT 100,
  auto_action varchar(10) NOT NULL DEFAULT 'SUCCESS',
  status char(1) NOT NULL DEFAULT '1',
  running char(1) NOT NULL DEFAULT '0',
  last_run_time datetime DEFAULT NULL,
  next_run_time datetime DEFAULT NULL,
  PRIMARY KEY (id), KEY idx_inspection_due(status,running,next_run_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动巡检策略';

ALTER TABLE mark_inspection_policy ADD COLUMN IF NOT EXISTS interval_value int NOT NULL DEFAULT 30 AFTER interval_seconds;
ALTER TABLE mark_inspection_policy ADD COLUMN IF NOT EXISTS interval_unit varchar(10) NOT NULL DEFAULT 'MINUTE' AFTER interval_value;
ALTER TABLE mark_inspection_policy ADD COLUMN IF NOT EXISTS auto_action varchar(10) NOT NULL DEFAULT 'SUCCESS' AFTER batch_limit;

CREATE TABLE IF NOT EXISTS mark_inspection_run_log (
  id bigint NOT NULL AUTO_INCREMENT,
  run_no varchar(64) NOT NULL,
  policy_id bigint NOT NULL,
  policy_name varchar(100) NOT NULL,
  platform_code varchar(64) NOT NULL,
  platform_name varchar(50) NOT NULL,
  trigger_type varchar(20) NOT NULL,
  scanned_count int NOT NULL DEFAULT 0,
  matched_count int NOT NULL DEFAULT 0,
  success_count int NOT NULL DEFAULT 0,
  failed_count int NOT NULL DEFAULT 0,
  skipped_count int NOT NULL DEFAULT 0,
  run_status varchar(20) NOT NULL,
  error_message varchar(500) DEFAULT NULL,
  start_time datetime NOT NULL,
  end_time datetime DEFAULT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_inspection_run_no(run_no), KEY idx_inspection_log(policy_id,start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动巡检执行日志';

CREATE TABLE IF NOT EXISTS mark_inspection_run_detail (
  id bigint NOT NULL AUTO_INCREMENT,
  run_log_id bigint NOT NULL,
  order_id bigint DEFAULT NULL,
  order_item_id bigint DEFAULT NULL,
  order_no varchar(64) DEFAULT NULL,
  phone varchar(32) NOT NULL,
  detection_result varchar(50) DEFAULT NULL,
  qualified char(1) NOT NULL DEFAULT '0',
  submission_status varchar(20) NOT NULL,
  submission_result varchar(100) DEFAULT NULL,
  process_note varchar(500) DEFAULT NULL,
  detected_time datetime NOT NULL,
  PRIMARY KEY (id), KEY idx_inspection_detail_run(run_log_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动巡检号码明细';

INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 900100000113,'自动巡检',900100000001,6,'autoInspection','', '', 'MarkAutoInspection',1,0,'M','0','0','','timer','admin',NOW(),'待处理订单自动巡检'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=900100000113);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT 900100000114,'巡检策略',900100000113,1,'policy','server/mark/inspection/policy', '', 'MarkInspectionPolicy',1,0,'C','0','0','server:markInspection:policy:list','setting','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=900100000114);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT 900100000115,'执行日志',900100000113,2,'log','server/mark/inspection/log', '', 'MarkInspectionLog',1,0,'C','0','0','server:markInspection:log:list','list','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=900100000115);

INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT 900100001020+n,label,900100000114,n,'#','','','',1,0,'F','0','0',perm,'#','admin',NOW() FROM (
 SELECT 1 n,'新增策略' label,'server:markInspection:policy:add' perm UNION ALL
 SELECT 2,'编辑策略','server:markInspection:policy:edit' UNION ALL
 SELECT 3,'删除策略','server:markInspection:policy:remove' UNION ALL
 SELECT 4,'执行巡检','server:markInspection:policy:run'
) p WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=900100001020+n);

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.menu_id IN (900100000113,900100000114,900100000115,900100001021,900100001022,900100001023,900100001024) WHERE r.role_key='admin';

-- Explicitly keep every new policy stopped until an administrator enables it.
UPDATE mark_inspection_policy SET status='1',running='0';
