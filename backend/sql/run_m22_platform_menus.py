# -*- coding: utf-8 -*-
import subprocess
from pathlib import Path

PARENT = 900100000118
COMPONENT = "server/mark/agent/process/platform"

PLATFORMS = [
    (900100000134, "agentProcessMobileGaopin", "mobile_gaopin", (0x79FB, 0x52A8, 0x9AD8, 0x9891)),
    (900100000135, "agentProcessYidonggaopin", "yidonggaopin", (0x79FB, 0x52A8, 0x9AD8, 0x9891)),
    (900100000136, "agentProcessTdGaopin", "td_gaopin", (0x6CF0, 0x8FEA, 0x9AD8, 0x9891)),
    (900100000137, "agentProcessTaidixiong", "taidixiong", (0x6CF0, 0x8FEA, 0x718A)),
    (900100000138, "agentProcessTdSecond", "td_second", (0x6CF0, 0x8FEA, 0x4E8C, 0x6B21)),
    (900100000139, "agentProcessQihuFirst", "qihu_first", (0x33, 0x36, 0x30, 0x9996, 0x6B21)),
    (900100000140, "agentProcessQihuSecond", "qihu_second", (0x33, 0x36, 0x30, 0x4E8C, 0x6B21)),
    (900100000141, "agentProcessSanliuling", "sanliuling", (0x33, 0x36, 0x30)),
    (900100000142, "agentProcessTencent", "tencent_mark", (0x817E, 0x8BAF)),
    (900100000143, "agentProcessTengxun", "tengxun", (0x817E, 0x8BAF)),
    (900100000144, "agentProcessDianhuabang", "dianhuabang", (0x7535, 0x8BDD, 0x90A6)),
    (900100000145, "agentProcessBaidu", "baidu", (0x767E, 0x5EA6)),
    (900100000146, "agentProcessSghmt", "sghmt", (0x641C, 0x72D7)),
    (900100000147, "agentProcessXiaomi", "xiaomi", (0x5C0F, 0x7C73, 0x624B, 0x673A)),
    (900100000148, "agentProcessLtgj", "ltgj", (0x8054, 0x901A, 0x7BA1, 0x5BB6)),
]

lines = [
    "SET NAMES utf8mb4;",
    "SET FOREIGN_KEY_CHECKS = 0;",
    "UPDATE sys_menu SET visible = '1', order_num = 99 WHERE menu_id = 900100000103;",
    "UPDATE sys_menu SET order_num = 90 WHERE menu_id = 900100000119;",
    "UPDATE sys_menu SET order_num = 91 WHERE menu_id = 900100000128;",
    "UPDATE sys_menu SET order_num = 92 WHERE menu_id = 900100000110;",
]

for idx, (menu_id, path, code, name_codes) in enumerate(PLATFORMS, start=1):
    name_hex = "".join(chr(c) for c in name_codes).encode("utf-8").hex().upper()
    query_json = '{"platformCode":"%s"}' % code
    lines.append(
        "INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, "
        "is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) "
        "SELECT %d, CONVERT(UNHEX('%s') USING utf8mb4), %d, %d, '%s', '%s', '%s', '', "
        "1, 0, 'C', '0', '0', 'server:markAgent:order:query', 'edit', 'admin', NOW(), 'M22-agent-process-platform' "
        "WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = %d);"
        % (menu_id, name_hex, PARENT, idx, path, COMPONENT, query_json, menu_id)
    )

union_parts = " UNION ALL ".join("SELECT %d AS menu_id" % item[0] for item in PLATFORMS)
lines.append(
    "INSERT INTO sys_role_menu (role_id, menu_id) "
    "SELECT r.role_id, m.menu_id FROM sys_role r "
    "JOIN (%s) m "
    "LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id "
    "WHERE r.role_key IN ('agent', 'mark_agent') AND rm.role_id IS NULL;" % union_parts
)
lines.append("SET FOREIGN_KEY_CHECKS = 1;")

sql_path = Path(__file__).with_name("m22_agent_process_platform_menus.sql")
content = "\n".join(lines)
sql_path.write_text(content, encoding="utf-8")
subprocess.run([r"C:\xampp\mysql\bin\mysql.exe", "-u", "root", "verifynum"], input=content, text=True, check=True)
print("done")
