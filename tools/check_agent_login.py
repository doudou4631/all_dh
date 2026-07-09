# -*- coding: utf-8 -*-
import pymysql

conn = pymysql.connect(
    host='127.0.0.1', user='verifyNum', password='pL6NspjfTHLazatP',
    database='verifynum', charset='utf8mb4',
)
cur = conn.cursor()

names = ['111222', '112233', '123456789', 'markagent', 'markuser', 'admin']
print('=== CHECK USERS ===')
for name in names:
    cur.execute(
        """
        SELECT u.user_id, u.user_name, u.status, u.del_flag, GROUP_CONCAT(r.role_key)
        FROM sys_user u
        LEFT JOIN sys_user_role ur ON ur.user_id = u.user_id
        LEFT JOIN sys_role r ON r.role_id = ur.role_id
        WHERE u.user_name = %s
        GROUP BY u.user_id, u.user_name, u.status, u.del_flag
        """,
        (name,),
    )
    row = cur.fetchone()
    print(name, '=>', row if row else 'NOT FOUND')

print('\n=== ALL USERS WITH ROLES ===')
cur.execute(
    """
    SELECT u.user_name, GROUP_CONCAT(r.role_key)
    FROM sys_user u
    LEFT JOIN sys_user_role ur ON ur.user_id = u.user_id
    LEFT JOIN sys_role r ON r.role_id = ur.role_id
    WHERE u.del_flag = 0
    GROUP BY u.user_name
    ORDER BY u.user_name
    """
)
for row in cur.fetchall():
    print(row)

print('\n=== AGENT LOGIN ATTEMPTS ===')
cur.execute(
    """
    SELECT user_name, status, msg, login_time
    FROM sys_logininfor
    WHERE user_name IN ('111222', '112233', 'markagent', '123456789')
    ORDER BY info_id DESC
    LIMIT 15
    """
)
for row in cur.fetchall():
    print(row)

conn.close()
