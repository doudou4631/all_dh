-- 客户 API 管理。先执行 mark_open_api.sql；本脚本可重复执行。
-- CUSTOMER 凭据用于客户检测、提交和状态查询；PROCESSING 保持现有代理任务读取和结果回填用途。

ALTER TABLE mark_open_api_credential
  ADD COLUMN IF NOT EXISTS credential_type varchar(24) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING/CUSTOMER' AFTER user_id,
  ADD COLUMN IF NOT EXISTS assigned_agent_id bigint DEFAULT NULL COMMENT '客户API责任代理ID' AFTER credential_type,
  ADD KEY IF NOT EXISTS idx_mark_open_api_type (credential_type),
  ADD KEY IF NOT EXISTS idx_mark_open_api_agent (assigned_agent_id);

UPDATE mark_open_api_credential
SET credential_type = 'PROCESSING'
WHERE credential_type IS NULL OR credential_type = '';

INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 104500000000000340,'客户API管理',900100000001,8,'customerApi','server/mark/customerApi/index','', 'MarkCustomerApi',1,0,'C','0','0','server:markCustomerApi:list','api','admin',NOW(),'客户提交标记订单API管理'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=104500000000000340);

INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT 104500000000000360+n,label,104500000000000340,n,'#','','','',1,0,'F','0','0',perm,'#','admin',NOW() FROM (
  SELECT 1 n,'客户API新增' label,'server:markCustomerApi:add' perm
  UNION ALL SELECT 2,'客户API编辑','server:markCustomerApi:edit'
  UNION ALL SELECT 3,'客户Secret轮换','server:markCustomerApi:rotate'
  UNION ALL SELECT 4,'客户API删除','server:markCustomerApi:remove'
) x WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=104500000000000360+x.n);

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id IN (104500000000000340,104500000000000361,104500000000000362,104500000000000363,104500000000000364)
WHERE r.role_key='admin';
