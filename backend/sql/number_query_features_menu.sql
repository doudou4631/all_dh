SET @number_query_menu_id := 92281941572000140;
SET @features_menu_id := 104500000000000201;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  @features_menu_id, '功能', @number_query_menu_id, 2, 'features', 'server/user/features', NULL, '',
  1, 0, 'C', '0', '0', NULL, 'setting',
  'admin', NOW(), 'admin', NOW(), '单次查询功能设置'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = @features_menu_id);

UPDATE sys_menu
SET menu_name = '功能', parent_id = @number_query_menu_id, order_num = 2,
    path = 'features', component = 'server/user/features', menu_type = 'C',
    visible = '0', status = '0', perms = NULL, icon = 'setting',
    update_by = 'admin', update_time = NOW(), remark = '单次查询功能设置'
WHERE menu_id = @features_menu_id;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role_id, @features_menu_id
FROM sys_role r
WHERE r.role_key = 'mark_user'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.role_id = r.role_id AND rm.menu_id = @features_menu_id
  );
