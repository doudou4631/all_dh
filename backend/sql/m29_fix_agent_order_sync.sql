-- Fix agent order sync: assign downstream orders to owning agent and auto-pass pending audit.
SET NAMES utf8mb4;

UPDATE mark_order mo
INNER JOIN sys_user su ON su.user_id = mo.user_id
INNER JOIN sys_user agent ON agent.user_name = su.create_by AND agent.del_flag = '0'
INNER JOIN sys_user_role aur ON aur.user_id = agent.user_id
INNER JOIN sys_role ar ON ar.role_id = aur.role_id AND ar.role_key IN ('agent', 'mark_agent')
SET mo.assigned_agent_id = agent.user_id,
    mo.update_time = NOW()
WHERE mo.assigned_agent_id IS NULL;

UPDATE mark_order mo
INNER JOIN sys_user su ON su.user_id = mo.user_id
INNER JOIN mark_platform_template mpt ON mpt.id = su.rel_mark_template AND mpt.status = '0'
INNER JOIN sys_user agent ON agent.user_id = mpt.owner_user_id AND agent.del_flag = '0'
INNER JOIN sys_user_role aur ON aur.user_id = agent.user_id
INNER JOIN sys_role ar ON ar.role_id = aur.role_id AND ar.role_key IN ('agent', 'mark_agent')
SET mo.assigned_agent_id = agent.user_id,
    mo.update_time = NOW()
WHERE mo.assigned_agent_id IS NULL;

UPDATE mark_order mo
INNER JOIN sys_user su ON su.user_id = mo.user_id
SET mo.assigned_agent_id = (
        SELECT agent.user_id
        FROM sys_user agent
        INNER JOIN sys_user_role aur ON aur.user_id = agent.user_id
        INNER JOIN sys_role ar ON ar.role_id = aur.role_id AND ar.role_key IN ('agent', 'mark_agent')
        WHERE agent.del_flag = '0'
          AND agent.rel_mark_template = su.rel_mark_template
        ORDER BY agent.user_id ASC
        LIMIT 1
    ),
    mo.update_time = NOW()
WHERE mo.assigned_agent_id IS NULL
  AND su.rel_mark_template IS NOT NULL
  AND su.rel_mark_template > 0;

UPDATE mark_order mo
INNER JOIN sys_user su ON su.user_id = mo.user_id
SET mo.audit_status = '1',
    mo.order_status = CASE WHEN mo.order_status = '0' THEN '1' ELSE mo.order_status END,
    mo.audit_opinion = CONVERT(UNHEX('E794A8E688B7E68F90E4BAA4E887AAE58AA8E5AEA1E6A0B8') USING utf8mb4),
    mo.audit_time = IFNULL(mo.audit_time, NOW()),
    mo.audit_by = IFNULL(mo.audit_by, IFNULL(su.create_by, 'system')),
    mo.update_time = NOW()
WHERE mo.audit_status = '0';
