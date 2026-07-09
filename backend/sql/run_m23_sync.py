# -*- coding: utf-8 -*-
import pymysql

DB = dict(
    host="127.0.0.1",
    user="verifyNum",
    password="pL6NspjfTHLazatP",
    database="verifynum",
    charset="utf8mb4",
)

AGENT_ID = 100002
AGENT_USERNAME = "markagent"


def main():
    conn = pymysql.connect(**DB)
    cur = conn.cursor()

    cur.execute(
        """
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
        WHERE mo.audit_status = '0'
        """
    )
    conn.commit()
    print("auto-pass updated:", cur.rowcount)

    cur.execute(
        """
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
          AND (mo.assigned_agent_id IS NULL OR mo.assigned_agent_id = 1)
        """
    )
    conn.commit()
    print("reassign agent updated:", cur.rowcount)

    cur.execute(
        """
        SELECT COUNT(*)
        FROM mark_order_item moi
        INNER JOIN mark_order mo ON mo.id = moi.order_id
        LEFT JOIN sys_user su ON su.user_id = mo.user_id
        WHERE mo.audit_status = '1'
          AND mo.platform_code = 'td_gaopin'
          AND (
                mo.assigned_agent_id = %s
                OR (mo.assigned_agent_id IS NULL AND su.create_by = %s)
                OR EXISTS (
                    SELECT 1
                    FROM sys_user agent_u
                    INNER JOIN sys_user_role aur ON aur.user_id = agent_u.user_id
                    INNER JOIN sys_role ar ON ar.role_id = aur.role_id AND ar.role_key IN ('agent', 'mark_agent')
                    WHERE agent_u.user_id = %s
                      AND agent_u.del_flag = '0'
                      AND agent_u.rel_mark_template IS NOT NULL
                      AND agent_u.rel_mark_template > 0
                      AND su.rel_mark_template = agent_u.rel_mark_template
                )
          )
        """,
        (AGENT_ID, AGENT_USERNAME, AGENT_ID),
    )
    print("markagent visible td_gaopin items:", cur.fetchone()[0])

    cur.execute(
        """
        SELECT mo.order_no, mo.audit_status, mo.assigned_agent_id, moi.phone
        FROM mark_order_item moi
        INNER JOIN mark_order mo ON mo.id = moi.order_id
        WHERE mo.platform_code = 'td_gaopin'
        ORDER BY moi.id DESC
        LIMIT 5
        """
    )
    for row in cur.fetchall():
        print(row)

    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
