# -*- coding: utf-8 -*-
import pymysql

try:
    import bcrypt
except ImportError:
    import subprocess
    subprocess.check_call(['pip', 'install', 'bcrypt', '-q'])
    import bcrypt

plain = b'123456'
pwd_hash = bcrypt.hashpw(plain, bcrypt.gensalt(rounds=10)).decode()
print('Generated hash for 123456:', pwd_hash)

conn = pymysql.connect(
    host='127.0.0.1', user='verifyNum', password='pL6NspjfTHLazatP',
    database='verifynum', charset='utf8mb4',
)
cur = conn.cursor()
cur.execute(
    "UPDATE sys_user SET password=%s, update_time=NOW() WHERE user_name IN ('123456789', '111222')",
    (pwd_hash,),
)
conn.commit()
cur.execute(
    "SELECT user_name, CHAR_LENGTH(password), password FROM sys_user WHERE user_name='123456789'"
)
row = cur.fetchone()
print('After update:', row[0], row[1], row[2][:20] + '...')
print('Verify:', bcrypt.checkpw(plain, row[2].encode()))
conn.close()
