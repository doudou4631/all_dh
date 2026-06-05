SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M12：免费查询用户账号复用修复
-- 1) 释放历史软删除账号（del_flag='2'）占用的唯一账号名
-- 2) 统一菜单名称，避免与系统用户管理混淆
-- =========================================================

UPDATE free_query_user
SET account = concat(left(account, 37), '__del__', id)
WHERE del_flag = '2'
  AND account NOT REGEXP '__del__[0-9]+$';

UPDATE sys_menu
SET menu_name = '手机端用户管理',
    remark = 'M12-手机端用户管理菜单',
    update_by = 'admin',
    update_time = NOW()
WHERE path = 'freeUser'
  AND perms = 'server:freeQueryUser:list';

SET FOREIGN_KEY_CHECKS = 1;
