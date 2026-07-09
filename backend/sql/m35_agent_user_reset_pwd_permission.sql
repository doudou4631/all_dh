SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- M35: grant reset password permission to mark agents
-- menu 1006 -> system:user:resetPwd

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, 1006
FROM `sys_role` r
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = 1006
WHERE r.role_key IN ('agent', 'mark_agent')
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;
