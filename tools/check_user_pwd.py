# -*- coding: utf-8 -*-
import pymysql

conn = pymysql.connect(
    host='127.0.0.1', user='verifyNum', password='pL6NspjfTHLazatP',
    database='verifynum', charset='utf8mb4',
)
cur = conn.cursor()

print('=== USER PASSWORD INFO ===')
cur.execute(
    """
    SELECT user_name, password, CHAR_LENGTH(password) AS pwd_len,
           create_by, create_time, update_by, update_time, remark
    FROM sys_user
    WHERE user_name IN ('123456789', 'markuser', 'markagent', '111222')
    """
)
for row in cur.fetchall():
    print(row)

print('\n=== OPER LOG FOR 123456789 ===')
cur.execute(
    """
    SELECT oper_time, title, oper_name, oper_param, json_result
    FROM sys_oper_log
    WHERE oper_param LIKE %s
    ORDER BY oper_id DESC
    LIMIT 8
    """,
    ('%123456789%',),
)
for row in cur.fetchall():
    print(row)

print('\n=== INIT PASSWORD CONFIG ===')
cur.execute(
    "SELECT config_key, config_value FROM sys_config WHERE config_key LIKE '%initPassword%'"
)
for row in cur.fetchall():
    print(row)

conn.close()
