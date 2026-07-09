SET NAMES utf8mb4;

-- M34: user-side unified order detail page (all platforms)
INSERT INTO `sys_menu` (
  `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`
)
SELECT 900100000149,
       CONVERT(UNHEX('E8AEA2E58D95E8AFA6E68385') USING utf8mb4),
       900100000001,
       2,
       'userOrderDetail',
       'server/mark/user/orderDetail',
       '',
       '',
       1, 0, 'C', '0', '0',
       'server:markUser:order:list',
       'documentation',
       'admin',
       NOW(),
       '用户端订单详情菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 900100000149);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, 900100000149
FROM `sys_role` r
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = 900100000149
WHERE r.role_key IN ('admin', 'common', 'user', 'mark_user')
  AND rm.role_id IS NULL;
