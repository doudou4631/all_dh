-- 已部署客户 API 管理环境的删除权限增量脚本，可重复执行。
-- 仅新增权限和管理员角色绑定，不删除任何业务数据。
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT 104500000000000364,'客户API删除',104500000000000340,4,'#','','','',1,0,'F','0','0','server:markCustomerApi:remove','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=104500000000000364);

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id=104500000000000364 AND m.perms='server:markCustomerApi:remove'
WHERE r.role_key='admin';
