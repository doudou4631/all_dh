# -*- coding: utf-8 -*-
import pymysql

SQL_PATH = r"c:\Users\Administrator\Desktop\1500\backend\sql\m46_fix_english_all_platform_codes.sql"


def main():
    with open(SQL_PATH, "r", encoding="utf-8") as f:
        sql_text = f.read()

    # Split by semicolon, keep statements that look like SQL
    statements = []
    buf = []
    for line in sql_text.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("--"):
            continue
        buf.append(line)
        if stripped.endswith(";"):
            statements.append("\n".join(buf))
            buf = []
    if buf:
        statements.append("\n".join(buf))

    conn = pymysql.connect(
        host="127.0.0.1",
        user="verifyNum",
        password="pL6NspjfTHLazatP",
        database="verifynum",
        charset="utf8mb4",
        autocommit=False,
    )
    cur = conn.cursor()
    try:
        for stmt in statements:
            s = stmt.strip()
            if not s or s.upper().startswith("SET NAMES"):
                if s.upper().startswith("SET NAMES"):
                    cur.execute(s)
                continue
            cur.execute(s)
            print("OK rows=%s | %s" % (cur.rowcount, s.splitlines()[0][:80]))
        conn.commit()
        print("\nCOMMITTED")

        cur.execute("SELECT id, template_name, template_info FROM mark_platform_template WHERE id=4")
        row = cur.fetchone()
        print("\nTemplate 4 after fix:")
        print("  name=%s" % row[1])
        print("  info=%s" % row[2])

        cur.execute(
            "SELECT platform_code, platform_name, remain_count "
            "FROM mark_user_platform_quota "
            "WHERE user_id=102864332034000145 ORDER BY platform_code"
        )
        print("\nUser 111222 quota after fix:")
        for q in cur.fetchall():
            print("  %s | %s | remain=%s" % q)
    except Exception as e:
        conn.rollback()
        print("FAILED:", e)
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
