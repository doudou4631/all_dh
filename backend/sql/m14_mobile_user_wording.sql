SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M14：手机端用户文案统一
-- 1) 将“免费用户管理/免费查询用户”菜单与按钮文案统一为“手机端用户”
-- 2) 不修改权限标识与路由，仅调整展示名称
-- =========================================================

UPDATE `sys_menu`
SET `menu_name` = '手机端用户管理',
    `remark` = 'M14-手机端用户管理菜单',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE (`menu_type` = 'C' AND `perms` = 'server:freeQueryUser:list')
   OR (`path` = 'freeUser' AND `menu_type` = 'C');

UPDATE `sys_menu`
SET `menu_name` = '手机端用户查询',
    `remark` = 'M14-手机端用户查询按钮',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_type` = 'F' AND `perms` = 'server:freeQueryUser:query';

UPDATE `sys_menu`
SET `menu_name` = '手机端用户新增',
    `remark` = 'M14-手机端用户新增按钮',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_type` = 'F' AND `perms` = 'server:freeQueryUser:add';

UPDATE `sys_menu`
SET `menu_name` = '手机端用户修改',
    `remark` = 'M14-手机端用户修改按钮',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_type` = 'F' AND `perms` = 'server:freeQueryUser:edit';

UPDATE `sys_menu`
SET `menu_name` = '手机端用户删除',
    `remark` = 'M14-手机端用户删除按钮',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_type` = 'F' AND `perms` = 'server:freeQueryUser:remove';

UPDATE `sys_menu`
SET `menu_name` = '手机端用户积分调整',
    `remark` = 'M14-手机端用户积分调整按钮',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_type` = 'F' AND `perms` = 'server:freeQueryUser:adjust';

UPDATE `sys_menu`
SET `menu_name` = '手机端用户重置密码',
    `remark` = 'M14-手机端用户重置密码按钮',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_type` = 'F' AND `perms` = 'server:freeQueryUser:resetPwd';

SET FOREIGN_KEY_CHECKS = 1;
