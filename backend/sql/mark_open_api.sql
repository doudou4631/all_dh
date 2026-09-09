-- 标记业务自动化 API Key。可重复执行。
CREATE TABLE IF NOT EXISTS mark_open_api_credential (
  id bigint NOT NULL AUTO_INCREMENT,
  name varchar(64) NOT NULL,
  api_key varchar(80) NOT NULL,
  secret_hash varchar(100) NOT NULL,
  user_id bigint NOT NULL,
  status char(1) NOT NULL DEFAULT '0',
  scopes varchar(255) NOT NULL,
  expire_time datetime DEFAULT NULL,
  last_used_time datetime DEFAULT NULL,
  create_by varchar(64) DEFAULT '', create_time datetime NOT NULL,
  update_by varchar(64) DEFAULT '', update_time datetime DEFAULT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_mark_open_api_key (api_key), KEY idx_mark_open_api_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标记自动化API凭据';
CREATE TABLE IF NOT EXISTS mark_open_api_credential_platform (
  credential_id bigint NOT NULL, platform_code varchar(64) NOT NULL,
  PRIMARY KEY (credential_id,platform_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标记自动化API平台授权';
CREATE TABLE IF NOT EXISTS mark_open_api_access_log (
  id bigint NOT NULL AUTO_INCREMENT, credential_id bigint NOT NULL, api_key varchar(80) NOT NULL,
  request_path varchar(255) NOT NULL, http_method varchar(12) NOT NULL, platform_code varchar(64) DEFAULT NULL,
  success tinyint NOT NULL, message varchar(255) DEFAULT NULL, elapsed_ms bigint NOT NULL DEFAULT 0, create_time datetime NOT NULL,
  PRIMARY KEY(id), KEY idx_mark_open_api_log_time(create_time), KEY idx_mark_open_api_log_key(credential_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标记自动化API调用日志';

INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 104500000000000300,'自动化API管理',900100000001,7,'openApi','server/mark/openApi/index','', 'MarkOpenApi',1,0,'C','0','0','server:markOpenApi:list','api','admin',NOW(),'标记自动化API Key管理'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=104500000000000300);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT 104500000000000320+n,label,104500000000000300,n,'#','','','',1,0,'F','0','0',perm,'#','admin',NOW() FROM (
 SELECT 1 n,'API新增' label,'server:markOpenApi:add' perm UNION ALL SELECT 2,'API编辑','server:markOpenApi:edit' UNION ALL SELECT 3,'Secret轮换','server:markOpenApi:rotate'
) x WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=104500000000000320+x.n);
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.menu_id IN (104500000000000300,104500000000000321,104500000000000322,104500000000000323) WHERE r.role_key='admin';
