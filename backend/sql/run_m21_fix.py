import subprocess

sql = (
    "UPDATE sys_menu SET menu_name=CONVERT(UNHEX('E5A484E79086E8AEA2E58D95') USING utf8mb4) "
    "WHERE menu_id=900100000103; "
    "UPDATE sys_menu SET menu_name=CONVERT(UNHEX('E5A484E79086E8AEA2E58D9528E4B88BE7BAA729') USING utf8mb4) "
    "WHERE menu_id=900100000119; "
    "SELECT menu_id, menu_name, HEX(menu_name) FROM sys_menu "
    "WHERE menu_id IN (900100000103,900100000119);"
)

subprocess.run(
    [r"C:\xampp\mysql\bin\mysql.exe", "-u", "root", "verifynum", "-e", sql],
    check=True,
)
