-- 标记业务平台管理：全局来源/目标映射与菜单。可重复执行。
CREATE TABLE IF NOT EXISTS mark_platform_mapping (
  id bigint NOT NULL AUTO_INCREMENT,
  source_platform_name varchar(50) NOT NULL,
  source_platform_code varchar(64) NOT NULL,
  source_enabled char(1) NOT NULL DEFAULT '0',
  target_platform_name varchar(50) NOT NULL,
  target_platform_code varchar(64) NOT NULL,
  target_enabled char(1) NOT NULL DEFAULT '0',
  mapping_enabled char(1) NOT NULL DEFAULT '0',
  sort_num int NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mark_platform_mapping (source_platform_code, target_platform_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标记业务平台映射';

ALTER TABLE mark_platform_mapping ADD COLUMN IF NOT EXISTS sort_num int NOT NULL DEFAULT 0 COMMENT '排序';

INSERT IGNORE INTO mark_platform_mapping
  (source_platform_name, source_platform_code, target_platform_name, target_platform_code, sort_num)
VALUES
  ('泰迪熊','taidixiong','泰迪熊','taidixiong',1),
  ('泰迪熊','taidixiong','泰迪高频','td_gaopin',1),
  ('泰迪熊','taidixiong','泰迪二次','td_second',1),
  ('腾讯','tengxun','腾讯','tengxun',2),
  ('360','360fugai','360','360fugai',3),
  ('360','360fugai','360二次','qihu_second',3),
  ('百度','baidu','百度','baidu',4),
  ('搜狗','sogou','搜狗','sogou',5),
  ('移动高频','yidonggaopin','移动高频','yidonggaopin',6),
  ('电话邦','dianhuabang','电话邦','dianhuabang',7),
  ('联通管家','ltgj','联通管家','ltgj',8),
  ('小米手机','xiaomi','小米手机','xiaomi',9);

-- 同步已有记录的展示名称，兼容重复执行和历史名称修正。
UPDATE mark_platform_mapping SET source_platform_name='泰迪熊' WHERE source_platform_code='taidixiong';
UPDATE mark_platform_mapping SET source_platform_name='腾讯' WHERE source_platform_code='tengxun';
UPDATE mark_platform_mapping SET source_platform_name='360' WHERE source_platform_code='360fugai';
UPDATE mark_platform_mapping SET source_platform_name='百度' WHERE source_platform_code='baidu';
UPDATE mark_platform_mapping SET source_platform_name='搜狗' WHERE source_platform_code='sogou';
UPDATE mark_platform_mapping SET source_platform_name='移动高频' WHERE source_platform_code='yidonggaopin';
UPDATE mark_platform_mapping SET source_platform_name='电话邦' WHERE source_platform_code='dianhuabang';
UPDATE mark_platform_mapping SET source_platform_name='联通管家' WHERE source_platform_code='ltgj';
UPDATE mark_platform_mapping SET source_platform_name='小米手机' WHERE source_platform_code='xiaomi';
UPDATE mark_platform_mapping SET target_platform_name='泰迪熊' WHERE target_platform_code='taidixiong';
UPDATE mark_platform_mapping SET target_platform_name='泰迪高频' WHERE target_platform_code='td_gaopin';
UPDATE mark_platform_mapping SET target_platform_name='泰迪二次' WHERE target_platform_code='td_second';
UPDATE mark_platform_mapping SET target_platform_name='腾讯' WHERE target_platform_code='tengxun';
UPDATE mark_platform_mapping SET target_platform_name='360' WHERE target_platform_code='360fugai';
UPDATE mark_platform_mapping SET target_platform_name='360二次' WHERE target_platform_code='qihu_second';
UPDATE mark_platform_mapping SET target_platform_name='百度' WHERE target_platform_code='baidu';
UPDATE mark_platform_mapping SET target_platform_name='搜狗' WHERE target_platform_code='sogou';
UPDATE mark_platform_mapping SET target_platform_name='移动高频' WHERE target_platform_code='yidonggaopin';
UPDATE mark_platform_mapping SET target_platform_name='电话邦' WHERE target_platform_code='dianhuabang';
UPDATE mark_platform_mapping SET target_platform_name='联通管家' WHERE target_platform_code='ltgj';
UPDATE mark_platform_mapping SET target_platform_name='小米手机' WHERE target_platform_code='xiaomi';

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 900100000105, '平台管理', 900100000001, 4, 'platformManagement',
  'server/mark/platformManagement/index', '', 'MarkPlatformManagement',
  1, 0, 'C', '0', '0', 'server:markPlatform:list', 'switch', 'admin', NOW(), '标记平台映射及开关'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=900100000105);

UPDATE sys_menu SET menu_name='平台管理', parent_id=900100000001, order_num=4,
  path='platformManagement', component='server/mark/platformManagement/index',
  route_name='MarkPlatformManagement', menu_type='C', visible='0', status='0',
  perms='server:markPlatform:list', icon='switch'
WHERE menu_id=900100000105;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 900100001010+p.n, p.label, 900100000105, p.n, '#', '', '', '', 1, 0, 'F', '0', '0', p.perm, '#', 'admin', NOW()
FROM (
  SELECT 1 n, '平台新增' label, 'server:markPlatform:add' perm
  UNION ALL SELECT 2, '平台编辑', 'server:markPlatform:edit'
  UNION ALL SELECT 3, '平台删除', 'server:markPlatform:remove'
  UNION ALL SELECT 4, '平台开关', 'server:markPlatform:status'
) p
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=900100001010+p.n);
UPDATE sys_menu SET menu_name='平台新增' WHERE menu_id=900100001011;
UPDATE sys_menu SET menu_name='平台编辑' WHERE menu_id=900100001012;
UPDATE sys_menu SET menu_name='平台删除' WHERE menu_id=900100001013;
UPDATE sys_menu SET menu_name='平台开关' WHERE menu_id=900100001014;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 900100000105 FROM sys_role WHERE role_key='admin';
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r JOIN sys_menu m ON m.menu_id BETWEEN 900100001011 AND 900100001014
WHERE r.role_key='admin';
