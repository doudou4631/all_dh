-- 标记业务代理管理菜单。可重复执行。
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS agent_level tinyint DEFAULT NULL COMMENT '代理等级0管理员1一级2下级代理，普通账户为空' AFTER rel_mark_template;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS parent_user_id bigint DEFAULT NULL COMMENT '直属上级用户ID' AFTER agent_level;
CREATE INDEX IF NOT EXISTS idx_sys_user_agent_parent ON sys_user(agent_level,parent_user_id);

CREATE TABLE IF NOT EXISTS mark_agent_point_transaction (
  id bigint NOT NULL AUTO_INCREMENT,
  transaction_no varchar(64) NOT NULL,
  request_no varchar(64) NOT NULL,
  transaction_type varchar(32) NOT NULL,
  platform_code varchar(64) NOT NULL,
  platform_name varchar(128) DEFAULT NULL,
  amount bigint NOT NULL,
  from_user_id bigint DEFAULT NULL,
  to_user_id bigint NOT NULL,
  operator_user_id bigint NOT NULL,
  reason varchar(255) DEFAULT NULL,
  status varchar(16) NOT NULL,
  create_time datetime NOT NULL,
  PRIMARY KEY(id), UNIQUE KEY uk_mark_agent_point_request(request_no),
  UNIQUE KEY uk_mark_agent_point_transaction(transaction_no),
  KEY idx_mark_agent_point_users(from_user_id,to_user_id), KEY idx_mark_agent_point_time(create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理平台次数充值交易';

UPDATE sys_user u JOIN sys_user_role ur ON ur.user_id=u.user_id JOIN sys_role r ON r.role_id=ur.role_id
SET u.agent_level=0,u.parent_user_id=NULL WHERE r.role_key='admin';
UPDATE sys_user u
JOIN sys_user_role ur ON ur.user_id=u.user_id
JOIN sys_role r ON r.role_id=ur.role_id
SET u.agent_level=1
WHERE r.role_key='mark_agent' AND u.agent_level IS NULL AND u.parent_user_id IS NULL;

UPDATE sys_user u
JOIN sys_user_role ur ON ur.user_id=u.user_id
JOIN sys_role r ON r.role_id=ur.role_id
JOIN sys_user p ON p.user_id=u.parent_user_id AND p.agent_level=1
SET u.agent_level=2
WHERE r.role_key='mark_agent' AND u.agent_level IS NULL;

-- 新模式只保留两级代理。原三级代理迁移为直属二级代理创建的普通账户。
INSERT IGNORE INTO sys_user_role(user_id,role_id)
SELECT u.user_id,user_role.role_id
FROM sys_user u
JOIN sys_role user_role ON user_role.role_key='mark_user'
WHERE u.agent_level=3 AND u.parent_user_id IS NOT NULL;

DELETE ur FROM sys_user_role ur
JOIN sys_user u ON u.user_id=ur.user_id AND u.agent_level=3 AND u.parent_user_id IS NOT NULL
JOIN sys_role agent_role ON agent_role.role_id=ur.role_id AND agent_role.role_key='mark_agent';

UPDATE sys_user SET agent_level=NULL WHERE agent_level=3 AND parent_user_id IS NOT NULL;

UPDATE sys_user u
JOIN sys_user_role ur ON ur.user_id=u.user_id
JOIN sys_role r ON r.role_id=ur.role_id AND r.role_key='mark_agent'
SET u.parent_user_id=NULL
WHERE u.agent_level=1;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 900100000109, '代理管理', 900100000001, 6, 'agentManagement', '', '', 'MarkAgentManagement',
  1, 0, 'M', '0', '0', '', 'peoples', 'admin', NOW(), '标记业务代理分级管理目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=900100000109);

UPDATE sys_menu SET menu_name='代理管理', parent_id=900100000001, order_num=6,
  path='agentManagement', component='', query='', route_name='MarkAgentManagement',
  menu_type='M', visible='0', status='0', perms='', icon='peoples'
WHERE menu_id=900100000109;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT p.menu_id, p.menu_name, 900100000109, p.order_num, p.path, 'server/mark/agentManagement/index', p.query, p.route_name,
  1, 0, 'C', '0', '0', 'server:markAgentManagement:list', 'user', 'admin', NOW(), '标记业务代理等级入口'
FROM (
  SELECT 900100000110 menu_id, '一级代理' menu_name, 1 order_num, 'level1' path,
    '{"roleKey":"mark_agent","agentLevel":"1"}' query, 'MarkAgentLevel1' route_name
  UNION ALL SELECT 900100000111, '下级代理', 2, 'level2',
    '{"roleKey":"mark_agent","agentLevel":"2"}', 'MarkAgentLevel2'
  UNION ALL SELECT 900100000112, '普通账户', 3, 'level3',
    '{"roleKey":"mark_user","agentLevel":"3"}', 'MarkAgentLevel3'
) p
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=p.menu_id);

UPDATE sys_menu SET menu_name='一级代理', parent_id=900100000109, order_num=1, path='level1',
  component='server/mark/agentManagement/index', query='{"agentLevel":"1"}',
  route_name='MarkAgentLevel1', menu_type='C', visible='0', status='0', perms='server:markAgentManagement:list', icon='user'
WHERE menu_id=900100000110;
UPDATE sys_menu SET menu_name='下级代理', parent_id=900100000109, order_num=2, path='level2',
  component='server/mark/agentManagement/index', query='{"agentLevel":"2"}',
  route_name='MarkAgentLevel2', menu_type='C', visible='0', status='0', perms='server:markAgentManagement:list', icon='user'
WHERE menu_id=900100000111;
UPDATE sys_menu SET menu_name='普通账户', parent_id=900100000109, order_num=3, path='level3',
  component='server/mark/agentManagement/index', query='{"roleKey":"mark_user","agentLevel":"3"}',
  route_name='MarkAgentLevel3', menu_type='C', visible='0', status='0', perms='server:markAgentManagement:list', icon='user'
WHERE menu_id=900100000112;

UPDATE sys_menu SET menu_name='次数充值' WHERE menu_id=900100001018;

-- 页面可见时必须同时具备列表加载和详情查询权限。
UPDATE sys_menu
SET perms='server:markAgent:order:list,server:markAgent:order:query'
WHERE menu_id IN (
  900100000302,900100000303,900100000304,900100000305,
  900100000306,900100000307,900100000308,900100000309,
  900100000310,900100000311,900100000312,900100000314,
  900100001008
);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r JOIN sys_menu m ON m.menu_id BETWEEN 900100000109 AND 900100000112
WHERE r.role_key='admin';

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.menu_id BETWEEN 900100000109 AND 900100000112
WHERE r.role_key='mark_agent';

INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT 900100001014+p.n,p.label,900100000109,p.n,'#','','','',1,0,'F','0','0',p.perm,'#','admin',NOW()
FROM (
 SELECT 1 n,'代理新增' label,'server:markAgentManagement:add' perm
 UNION ALL SELECT 2,'代理状态','server:markAgentManagement:edit'
 UNION ALL SELECT 3,'初始化密码','server:markAgentManagement:resetPwd'
 UNION ALL SELECT 4,'次数充值','server:markAgentManagement:allocate'
) p WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=900100001014+p.n);

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.menu_id BETWEEN 900100001015 AND 900100001018
WHERE r.role_key IN('admin','mark_agent');

-- 二级代理角色同时拥有普通用户业务和下级管理能力。
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id BETWEEN 900100001001 AND 900100001007
WHERE r.role_key='mark_user' AND r.del_flag='0';

-- 普通用户角色不能继承代理管理菜单和按钮权限。
DELETE rm FROM sys_role_menu rm
JOIN sys_role r ON r.role_id=rm.role_id AND r.role_key='mark_user'
WHERE rm.menu_id BETWEEN 900100000109 AND 900100000112
   OR rm.menu_id BETWEEN 900100001015 AND 900100001018
   OR rm.menu_id=900100000107;

-- 旧版本创建的二级代理可能只绑定了普通用户角色，补齐代理角色。
INSERT IGNORE INTO sys_user_role(user_id,role_id)
SELECT u.user_id,r.role_id
FROM sys_user u
JOIN sys_role r ON r.role_key='mark_agent' AND r.status='0' AND r.del_flag='0'
WHERE u.agent_level IN (1,2) AND u.del_flag=0;
