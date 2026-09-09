-- 业务管理 -> 处理菜单
-- 可重复执行：仅在菜单不存在时插入。
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 95000000000000001, '业务管理', 0, 4, 'business', '', '', '',
       1, 0, 'M', '0', '0', '', 'management',
       'admin', NOW(), '', NULL, '业务管理目录'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000001
);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 95000000000000002, '处理', 95000000000000001, 1, 'process', 'business/process/index', '', 'BusinessProcess',
       1, 0, 'C', '0', '0', 'business:process:list', 'list',
       'admin', NOW(), '', NULL, '业务处理菜单'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000002
);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 95000000000000003, '用户端', 95000000000000001, 2, 'user', 'business/user/index', '', 'BusinessUser',
       1, 0, 'C', '0', '0', 'business:user:list', 'user',
       'admin', NOW(), '', NULL, '业务管理用户端菜单'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000003
);

-- 用户端改为目录，子菜单对应 www 前端中的用户页面。
UPDATE sys_menu
SET menu_type = 'M', component = '', perms = '', icon = 'user'
WHERE menu_id = 95000000000000003;

DELETE FROM sys_menu
WHERE menu_id IN (95000000000000016, 95000000000000017);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 95000000000000011, 'TdGaopin高频', 95000000000000003, 1, 'tdgaopin', 'business/user/index', 'platform=tdgaopin', 'BusinessUserTdGaopin',
       1, 0, 'C', '0', '0', 'business:user:list', 'number',
       'admin', NOW(), '', NULL, '用户端平台菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000011);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 95000000000000012, 'Tengxun腾讯', 95000000000000003, 2, 'tengxun', 'business/user/index', 'platform=tengxun', 'BusinessUserTengxun',
       1, 0, 'C', '0', '0', 'business:user:list', 'number',
       'admin', NOW(), '', NULL, '用户端平台菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000012);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 95000000000000013, 'Taidixiong二次', 95000000000000003, 3, 'taidixiong', 'business/user/index', 'platform=taidixiong', 'BusinessUserTaidixiong',
       1, 0, 'C', '0', '0', 'business:user:list', 'number',
       'admin', NOW(), '', NULL, '用户端平台菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000013);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 95000000000000014, '360Fugai覆盖', 95000000000000003, 4, '360fugai', 'business/user/index', 'platform=360fugai', 'BusinessUser360Fugai',
       1, 0, 'C', '0', '0', 'business:user:list', 'number',
       'admin', NOW(), '', NULL, '用户端平台菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000014);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 95000000000000015, 'Xiaomi小米', 95000000000000003, 5, 'xiaomi', 'business/user/index', 'platform=xiaomi', 'BusinessUserXiaomi',
       1, 0, 'C', '0', '0', 'business:user:list', 'number',
       'admin', NOW(), '', NULL, '用户端平台菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000015);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 95000000000000016, 'Baidu百度', 95000000000000003, 6, 'baidu', 'business/user/index', 'platform=baidu', 'BusinessUserBaidu', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), '', NULL, '用户端平台菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000016);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 95000000000000017, 'YidongGaopin移动', 95000000000000003, 7, 'yidonggaopin', 'business/user/index', 'platform=yidonggaopin', 'BusinessUserYidongGaopin', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), '', NULL, '用户端平台菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000017);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 95000000000000018, 'Sougou', 95000000000000003, 8, 'sougou', 'business/user/index', 'platform=sougou', 'BusinessUserSougou', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), '', NULL, '用户端平台菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000018);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 95000000000000019, 'Dianhuabang', 95000000000000003, 9, 'dianhuabang', 'business/user/index', 'platform=dianhuabang', 'BusinessUserDianhuabang', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), '', NULL, '用户端平台菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000019);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 95000000000000020, 'LiantongGuanjia', 95000000000000003, 10, 'liantongguanjia', 'business/user/index', 'platform=liantongguanjia', 'BusinessUserLiantongGuanjia', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), '', NULL, '用户端平台菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000020);

UPDATE sys_menu SET menu_name = 'TdGaopin高频', order_num = 1, path = 'tdgaopin', component = 'business/user/index', query = 'platform=tdgaopin', route_name = 'BusinessUserTdGaopin', perms = 'business:user:list' WHERE menu_id = 95000000000000011;
UPDATE sys_menu SET menu_name = 'Tengxun腾讯', order_num = 2, path = 'tengxun', component = 'business/user/index', query = 'platform=tengxun', route_name = 'BusinessUserTengxun', perms = 'business:user:list' WHERE menu_id = 95000000000000012;
UPDATE sys_menu SET menu_name = 'Taidixiong二次', order_num = 3, path = 'taidixiong', component = 'business/user/index', query = 'platform=taidixiong', route_name = 'BusinessUserTaidixiong', perms = 'business:user:list' WHERE menu_id = 95000000000000013;
UPDATE sys_menu SET menu_name = '360Fugai覆盖', order_num = 4, path = '360fugai', component = 'business/user/index', query = 'platform=360fugai', route_name = 'BusinessUser360Fugai', perms = 'business:user:list' WHERE menu_id = 95000000000000014;
UPDATE sys_menu SET menu_name = 'Xiaomi小米', order_num = 5, path = 'xiaomi', component = 'business/user/index', query = 'platform=xiaomi', route_name = 'BusinessUserXiaomi', perms = 'business:user:list' WHERE menu_id = 95000000000000015;
UPDATE sys_menu SET menu_name = 'Baidu百度', order_num = 6, path = 'baidu', component = 'business/user/index', query = 'platform=baidu', route_name = 'BusinessUserBaidu', perms = 'business:user:list' WHERE menu_id = 95000000000000016;
UPDATE sys_menu SET menu_name = 'YidongGaopin移动', order_num = 7, path = 'yidonggaopin', component = 'business/user/index', query = 'platform=yidonggaopin', route_name = 'BusinessUserYidongGaopin', perms = 'business:user:list' WHERE menu_id = 95000000000000017;

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 95000000000000016, '钱包', 95000000000000003, 6, 'wallet', 'server/mark/user/wallet', '', 'MarkUserWallet',
       1, 0, 'C', '0', '0', 'server:markUser:wallet', 'money',
       'admin', NOW(), '', NULL, 'www 用户端钱包'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000016);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 95000000000000017, '消息通知', 95000000000000003, 7, 'notice', 'server/mark/user/notice/index', '', 'MarkUserNotice',
       1, 0, 'C', '0', '0', 'server:markUser:notice', 'message',
       'admin', NOW(), '', NULL, 'www 用户端消息通知'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 95000000000000017);

-- 最终结构：平台列表由用户端页面内部展示，不作为后台菜单层级。
UPDATE sys_menu
SET menu_name = '标记业务管理', path = 'business', component = '', menu_type = 'M', perms = ''
WHERE menu_id = 95000000000000001;

UPDATE sys_menu
SET menu_name = '标记处理', path = 'process', component = 'business/process/index', menu_type = 'C',
    route_name = 'BusinessProcess', perms = 'business:process:list'
WHERE menu_id = 95000000000000002;

UPDATE sys_menu
SET menu_name = '用户端', path = 'user', component = 'business/user/index', menu_type = 'C',
    route_name = 'BusinessUser', perms = 'business:user:list'
WHERE menu_id = 95000000000000003;

DELETE FROM sys_menu
WHERE parent_id = 95000000000000003;
