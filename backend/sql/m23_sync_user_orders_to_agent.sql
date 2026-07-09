-- Sync user-submitted orders to agent processing queue.
UPDATE mark_order mo
INNER JOIN sys_user su ON su.user_id = mo.user_id
SET mo.audit_status = '1',
    mo.order_status = '1',
    mo.audit_opinion = CONVERT(UNHEX('E794A8E688B7E68F90E4BAA4E887AAE58AA8E5AEA1E6A0B8') USING utf8mb4),
    mo.audit_time = NOW(),
    mo.audit_by = IFNULL(su.create_by, 'system'),
    mo.assigned_agent_id = (
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
WHERE mo.audit_status = '0';

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
WHERE mo.platform_code = 'td_gaopin'
  AND (mo.assigned_agent_id IS NULL OR mo.assigned_agent_id = 1);
