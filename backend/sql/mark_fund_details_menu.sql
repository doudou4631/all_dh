-- 标记业务资金明细菜单。可重复执行。
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 900100000108, '资金管理', 900100000001, 5, 'fundManagement',
  '', '', 'MarkFundManagement',
  1, 0, 'M', '0', '0', '', 'money', 'admin', NOW(), '标记业务资金管理目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=900100000108);

UPDATE sys_menu SET menu_name='资金管理', parent_id=900100000001, order_num=5,
  path='fundManagement', component='', route_name='MarkFundManagement',
  menu_type='M', visible='0', status='0', perms='', icon='money'
WHERE menu_id=900100000108;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 900100000106, '资金明细', 900100000108, 1, 'fundDetails',
  'server/mark/fund/index', '', 'MarkFundDetails',
  1, 0, 'C', '0', '0', 'server:markUser:wallet:log:list', 'money', 'admin', NOW(), '当前用户标记业务资金明细'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=900100000106);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 900100000107, '资金明细(下级)', 900100000108, 2, 'fundDetailsSubordinate',
  'server/mark/fundSubordinate/index', '', 'MarkFundDetailsSubordinate',
  1, 0, 'C', '0', '0', 'server:markAgent:wallet:list', 'money', 'admin', NOW(), '代理下级用户标记业务资金明细'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=900100000107);

UPDATE sys_menu SET menu_name='资金明细', parent_id=900100000108, order_num=1,
  path='fundDetails', component='server/mark/fund/index', route_name='MarkFundDetails',
  menu_type='C', visible='0', status='0', perms='server:markUser:wallet:log:list', icon='money'
WHERE menu_id=900100000106;

UPDATE sys_menu SET menu_name='资金明细(下级)', parent_id=900100000108, order_num=2,
  path='fundDetailsSubordinate', component='server/mark/fundSubordinate/index', route_name='MarkFundDetailsSubordinate',
  menu_type='C', visible='0', status='0', perms='server:markAgent:wallet:list', icon='money'
WHERE menu_id=900100000107;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 900100000106 FROM sys_role WHERE role_key IN ('admin','user','mark_user');
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 900100000107 FROM sys_role WHERE role_key IN ('admin','agent','mark_agent');
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 900100000108 FROM sys_role WHERE role_key IN ('admin','user','mark_user','agent','mark_agent');
