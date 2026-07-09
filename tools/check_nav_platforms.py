# -*- coding: utf-8 -*-
import pymysql

conn = pymysql.connect(
    host='127.0.0.1', user='verifyNum', password='pL6NspjfTHLazatP',
    database='verifynum', charset='utf8mb4',
)
cur = conn.cursor()

cur.execute('SELECT id, template_name, template_info FROM mark_platform_template WHERE id=3')
row = cur.fetchone()
print('template:', row[0], row[1])
print('info:', row[2])

print('\nmenus under mark root:')
cur.execute(
    'SELECT menu_id, menu_name, path, component, visible, order_num FROM sys_menu WHERE parent_id=900100000001 ORDER BY order_num'
)
for r in cur.fetchall():
    print(r)

print('\ndedicated menus:')
cur.execute(
    "SELECT menu_id, menu_name, path, component, visible FROM sys_menu WHERE path IN ('tencentMark','xiaomiMark')"
)
for r in cur.fetchall():
    print(r)

conn.close()
