-- 管理员全部用户资金明细菜单，可重复执行。
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 900100000116, '资金明细（总）', 900100000108, 3, 'fundDetailsTotal',
  'server/mark/fundTotal/index', '', 'MarkFundDetailsTotal',
  1, 0, 'C', '0', '0', 'server:markFund:total:list', 'money', 'admin', NOW(), '管理员查看全部用户标记业务资金流水'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000116);

UPDATE sys_menu SET menu_name='资金明细（总）', parent_id=900100000108, order_num=3,
  path='fundDetailsTotal', component='server/mark/fundTotal/index', route_name='MarkFundDetailsTotal',
  menu_type='C', visible='0', status='0', perms='server:markFund:total:list', icon='money',
  remark='管理员查看全部用户标记业务资金流水'
WHERE menu_id=900100000116;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 900100000116 FROM sys_role WHERE role_key='admin';
