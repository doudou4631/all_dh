-- 管理后台保留认证；这里只同步 www 的菜单名称和层级，不接入 www 的接口功能。
UPDATE sys_menu
SET menu_name = '标记业务管理', path = 'business', component = '', menu_type = 'M', perms = ''
WHERE menu_id = 95000000000000001;

UPDATE sys_menu
SET menu_name = '标记处理', path = 'process', component = 'business/process/index', menu_type = 'C',
    route_name = 'BusinessProcess', perms = 'business:process:list'
WHERE menu_id = 95000000000000002;

UPDATE sys_menu
SET menu_name = '用户端', path = 'user', component = '', menu_type = 'M',
    route_name = '', perms = ''
WHERE menu_id = 95000000000000003;

DELETE FROM sys_menu
WHERE parent_id = 95000000000000003;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
    (95000000000000101, 'TdGaopin高频', 95000000000000003, 1, 'tdgaopin', 'business/user/index', '', 'BusinessUserTdGaopin', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), 'www 菜单'),
    (95000000000000102, 'Tengxun腾讯', 95000000000000003, 2, 'tengxun', 'business/user/index', '', 'BusinessUserTengxun', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), 'www 菜单'),
    (95000000000000103, 'Taidixiong二次', 95000000000000003, 3, 'taidixiong', 'business/user/index', '', 'BusinessUserTaidixiong', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), 'www 菜单'),
    (95000000000000104, '360Fugai覆盖', 95000000000000003, 4, '360fugai', 'business/user/index', '', 'BusinessUser360Fugai', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), 'www 菜单'),
    (95000000000000105, 'Xiaomi小米', 95000000000000003, 5, 'xiaomi', 'business/user/index', '', 'BusinessUserXiaomi', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), 'www 菜单'),
    (95000000000000106, 'Baidu百度', 95000000000000003, 6, 'baidu', 'business/user/index', '', 'BusinessUserBaidu', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), 'www 菜单'),
    (95000000000000107, 'YidongGaopin移动', 95000000000000003, 7, 'yidonggaopin', 'business/user/index', '', 'BusinessUserYidongGaopin', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), 'www 菜单'),
    (95000000000000108, 'Sougou', 95000000000000003, 8, 'sougou', 'business/user/index', '', 'BusinessUserSougou', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), 'www 菜单'),
    (95000000000000109, 'Dianhuabang', 95000000000000003, 9, 'dianhuabang', 'business/user/index', '', 'BusinessUserDianhuabang', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), 'www 菜单'),
    (95000000000000110, 'LiantongGuanjia', 95000000000000003, 10, 'liantongguanjia', 'business/user/index', '', 'BusinessUserLiantongGuanjia', 1, 0, 'C', '0', '0', 'business:user:list', 'number', 'admin', NOW(), 'www 菜单');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE menu_id = 95000000000000001 OR menu_id = 95000000000000002
   OR menu_id = 95000000000000003 OR parent_id = 95000000000000003;

-- 标记处理同步代理端平台目录。
UPDATE sys_menu
SET menu_type = 'M', component = '', route_name = '', perms = ''
WHERE menu_id = 95000000000000002;

DELETE FROM sys_menu WHERE parent_id = 95000000000000002;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 95000000000000200 + ROW_NUMBER() OVER (ORDER BY menu_id), menu_name, 95000000000000002,
       order_num, path, 'business/process/index', '', REPLACE(route_name, 'BusinessUser', 'BusinessProcess'),
       is_frame, is_cache, menu_type, visible, status, 'business:process:list', icon,
       'admin', NOW(), 'agent menu sync'
FROM sys_menu
WHERE parent_id = 95000000000000003
ORDER BY order_num;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE parent_id = 95000000000000002;

-- 使用 UTF-8 十六进制字节写入，避免 Windows 终端执行脚本时中文被转成问号。
UPDATE sys_menu SET menu_name = CONVERT(0xE6A087E8AEB0E4B89AE58AA1E7AEA1E79086 USING utf8mb4) WHERE menu_id = 95000000000000001;
UPDATE sys_menu SET menu_name = CONVERT(0xE6A087E8AEB0E5A484E79086 USING utf8mb4) WHERE menu_id = 95000000000000002;
UPDATE sys_menu SET menu_name = CONVERT(0xE794A8E688B7E7ABAF USING utf8mb4) WHERE menu_id = 95000000000000003;
UPDATE sys_menu SET menu_name = CONVERT(0x546447616F70696EE9AB98E9A291 USING utf8mb4) WHERE menu_id = 95000000000000101;
UPDATE sys_menu SET menu_name = CONVERT(0x54656E6778756EE885BEE8AEAF USING utf8mb4) WHERE menu_id = 95000000000000102;
UPDATE sys_menu SET menu_name = CONVERT(0x546169646978696F6E67E4BA8CE6ACA1 USING utf8mb4) WHERE menu_id = 95000000000000103;
UPDATE sys_menu SET menu_name = CONVERT(0x3336304675676169E8A686E79B96 USING utf8mb4) WHERE menu_id = 95000000000000104;
UPDATE sys_menu SET menu_name = CONVERT(0x5869616F6D69E5B08FE7B1B3 USING utf8mb4) WHERE menu_id = 95000000000000105;
UPDATE sys_menu SET menu_name = CONVERT(0x4261696475E799BEE5BAA6 USING utf8mb4) WHERE menu_id = 95000000000000106;
UPDATE sys_menu SET menu_name = CONVERT(0x5969646F6E6747616F70696EE7A7BBE58AA8 USING utf8mb4) WHERE menu_id = 95000000000000107;

UPDATE sys_menu AS process_menu
JOIN sys_menu AS user_menu ON user_menu.parent_id = 95000000000000003 AND user_menu.path = process_menu.path
SET process_menu.menu_name = user_menu.menu_name
WHERE process_menu.parent_id = 95000000000000002;
