SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M11：标记模板权限收敛与继承修复
-- 1) 模板管理能力收敛到 mark_admin
-- 2) 回收 admin/agent/mark_agent 的模板菜单与功能点
-- 3) 回填处理账号 rel_mark_template（优先 owner 默认模板）
-- 4) 同步下游账号模板为处理账号绑定模板
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
WHERE r.role_key = 'mark_admin'
  AND rm.role_id IS NULL;

DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON rm.role_id = r.role_id
WHERE r.role_key IN ('admin', 'agent', 'mark_agent')
  AND rm.menu_id IN (900100000127, 900100001701, 900100001702, 900100001703, 900100001704);

UPDATE `sys_user` u
JOIN (
  SELECT ur.user_id
  FROM `sys_user_role` ur
  JOIN `sys_role` r ON r.role_id = ur.role_id
  WHERE r.role_key IN ('agent', 'mark_agent')
  GROUP BY ur.user_id
) p ON p.user_id = u.user_id
JOIN (
  SELECT owner_user_id, MIN(id) AS template_id
  FROM `mark_platform_template`
  WHERE owner_user_id IS NOT NULL
    AND status = '0'
    AND is_default = '1'
  GROUP BY owner_user_id
) d ON d.owner_user_id = u.user_id
SET u.rel_mark_template = d.template_id
WHERE u.del_flag = '0'
  AND (u.rel_mark_template IS NULL OR u.rel_mark_template = 0);

UPDATE `sys_user` u
JOIN (
  SELECT ur.user_id
  FROM `sys_user_role` ur
  JOIN `sys_role` r ON r.role_id = ur.role_id
  WHERE r.role_key IN ('agent', 'mark_agent')
  GROUP BY ur.user_id
) p ON p.user_id = u.user_id
JOIN (
  SELECT owner_user_id, MIN(id) AS template_id
  FROM `mark_platform_template`
  WHERE owner_user_id IS NOT NULL
    AND status = '0'
  GROUP BY owner_user_id
  HAVING COUNT(*) = 1
) s ON s.owner_user_id = u.user_id
SET u.rel_mark_template = s.template_id
WHERE u.del_flag = '0'
  AND (u.rel_mark_template IS NULL OR u.rel_mark_template = 0);

UPDATE `sys_user` u
JOIN (
  SELECT p.user_id AS processor_user_id, MIN(d.rel_mark_template) AS template_id
  FROM `sys_user` p
  JOIN `sys_user_role` pur ON pur.user_id = p.user_id
  JOIN `sys_role` pr ON pr.role_id = pur.role_id AND pr.role_key IN ('agent', 'mark_agent')
  JOIN `sys_user` d
    ON CONVERT(d.create_by USING utf8mb4) COLLATE utf8mb4_general_ci
     = CONVERT(p.user_name USING utf8mb4) COLLATE utf8mb4_general_ci
  JOIN `sys_user_role` dur ON dur.user_id = d.user_id
  JOIN `sys_role` dr ON dr.role_id = dur.role_id AND dr.role_key IN ('user', 'mark_user')
  WHERE p.del_flag = '0'
    AND d.del_flag = '0'
    AND d.rel_mark_template IS NOT NULL
  GROUP BY p.user_id
  HAVING COUNT(DISTINCT d.rel_mark_template) = 1
) c ON c.processor_user_id = u.user_id
JOIN `mark_platform_template` t ON t.id = c.template_id AND t.status = '0'
SET u.rel_mark_template = c.template_id
WHERE u.del_flag = '0'
  AND (u.rel_mark_template IS NULL OR u.rel_mark_template = 0);

UPDATE `sys_user` d
JOIN (
  SELECT u.user_id, u.user_name, u.rel_mark_template
  FROM `sys_user` u
  JOIN `sys_user_role` ur ON ur.user_id = u.user_id
  JOIN `sys_role` r ON r.role_id = ur.role_id
  WHERE r.role_key IN ('agent', 'mark_agent')
    AND u.del_flag = '0'
    AND u.rel_mark_template IS NOT NULL
  GROUP BY u.user_id, u.user_name, u.rel_mark_template
) p
  ON CONVERT(d.create_by USING utf8mb4) COLLATE utf8mb4_general_ci
   = CONVERT(p.user_name USING utf8mb4) COLLATE utf8mb4_general_ci
JOIN `sys_user_role` dur ON dur.user_id = d.user_id
JOIN `sys_role` dr ON dr.role_id = dur.role_id AND dr.role_key IN ('user', 'mark_user')
SET d.rel_mark_template = p.rel_mark_template
WHERE d.del_flag = '0'
  AND (d.rel_mark_template IS NULL OR d.rel_mark_template <> p.rel_mark_template);

SET FOREIGN_KEY_CHECKS = 1;
