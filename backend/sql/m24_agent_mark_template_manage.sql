SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M24：开放代理端标记模板创建与编辑
-- 1) 为 agent / mark_agent 绑定标记模板菜单与功能点
-- 2) 代理仅可管理本人 owner_user_id 下的模板（服务层已做范围限制）
-- =========================================================

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN (
  SELECT 900100000127 AS menu_id UNION ALL
  SELECT 900100001701 UNION ALL
  SELECT 900100001702 UNION ALL
  SELECT 900100001703 UNION ALL
  SELECT 900100001704
) m
LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key IN ('agent', 'mark_agent')
  AND rm.role_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;
