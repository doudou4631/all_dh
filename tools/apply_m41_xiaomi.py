# -*- coding: utf-8 -*-
import pymysql

conn = pymysql.connect(
    host='127.0.0.1', user='verifyNum', password='pL6NspjfTHLazatP',
    database='verifynum', charset='utf8mb4',
)
cur = conn.cursor()

# xiaomi menu
cur.execute(
    """
    INSERT INTO sys_menu (
      menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
    )
    SELECT
      900100000150,
      CONVERT(UNHEX('E5B08FE7B1B3E6898BE69CBA') USING utf8mb4),
      900100000001,
      10,
      'xiaomiMark',
      'server/mark/user/xiaomi',
      '',
      'MarkUserXiaomi',
      1, 0, 'C', '1', '0',
      'server:markUser:order:clear',
      'phone',
      'admin',
      NOW(),
      'M41-xiaomi-all-platform-template'
    WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 900100000150)
    """
)

cur.execute(
    """
    UPDATE sys_menu
    SET visible = '1',
        component = 'server/mark/user/xiaomi',
        route_name = 'MarkUserXiaomi',
        status = '0',
        menu_name = CONVERT(UNHEX('E5B08FE7B1B3E6898BE69CBA') USING utf8mb4)
    WHERE menu_id = 900100000150
    """
)

for role_key in ('mark_user', 'common', 'admin', 'user'):
    cur.execute(
        """
        INSERT INTO sys_role_menu (role_id, menu_id)
        SELECT r.role_id, 900100000150
        FROM sys_role r
        LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = 900100000150
        WHERE r.role_key = %s AND rm.role_id IS NULL
        """,
        (role_key,),
    )

# normalize Xiaomi -> xiaomi in template 3
cur.execute('SELECT template_info FROM mark_platform_template WHERE id=3')
info = cur.fetchone()[0]
if info and '"Xiaomi"' in info:
    info = info.replace('"platformCode":"Xiaomi"', '"platformCode":"xiaomi"')
    cur.execute('UPDATE mark_platform_template SET template_info=%s WHERE id=3', (info,))

conn.commit()
cur.execute("SELECT menu_id, menu_name, path, visible FROM sys_menu WHERE path='xiaomiMark'")
print('xiaomi:', cur.fetchone())
conn.close()
print('ok')
