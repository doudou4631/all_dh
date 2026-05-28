# 前后端发布更新手册（当前线上环境）
适用场景：你后续修改了 `frontend` 或 `backend` 代码，需要把新版本发布到当前生产服务器。

## 1. 当前线上环境基线（以此为准）
- 服务器登录用户：`ubuntu`（`sudo`）
- Nginx 站点配置：`/etc/nginx/sites-available/biaoji.aleo1314.vip.conf`
- 前端目录：`/www/wwwroot/frontend`
- 后端 Jar：`/www/wwwroot/backend/geek-admin.jar`
- 后端服务：`geek-admin.service`（systemd 托管）
- 后端端口：`8080`
- 对外域名：`https://biaoji.aleo1314.vip`
- API 转发：`/prod-api/ -> http://127.0.0.1:8080/`
- 数据库配置来源：`/www/wwwroot/backend/application-data.yml`
- 当前数据库连接（以线上配置为准）：`jdbc:mysql://43.142.125.17:3306/verifynum`
- 重要：本地启动 `backend` 也会使用 `application-data.yml`，若未切换到本地库，会直接读写线上库

注意：本文档已经按当前非宝塔环境重写，旧的 root/宝塔/JDK 固定路径流程全部废弃。

## 2. 本地打包
在项目根目录执行。

### 2.1 前端打包
```bash
npm --prefix frontend ci
npm --prefix frontend run build:prod
```
产物目录：`frontend/dist`

### 2.2 后端打包
```bash
mvn -f backend/pom.xml clean package -DskipTests
```
产物路径：`backend/geek-admin/target/geek-admin.jar`

## 3. 上传发布包到服务器
建议统一放到服务器 `/tmp/deploy`。

```bash
ssh ubuntu@43.142.125.17 "mkdir -p /tmp/deploy"

scp -r ./frontend/dist ubuntu@43.142.125.17:/tmp/deploy/frontend-dist
scp ./backend/geek-admin/target/geek-admin.jar ubuntu@43.142.125.17:/tmp/deploy/geek-admin.jar

# 如果本次有数据库变更，再上传 SQL（文件名示例）
scp ./backend/sql/m4_mark_migration.sql ubuntu@43.142.125.17:/tmp/deploy/migration.sql
```

## 4. 服务器发布步骤（SSH 后执行）
```bash
ssh ubuntu@43.142.125.17
```

### 4.1 定义变量
```bash
TS=$(date +%F_%H%M%S)
BACKUP_DIR=/www/backup/deploy/$TS
FRONT_DIR=/www/wwwroot/frontend
JAR_PATH=/www/wwwroot/backend/geek-admin.jar
TMP_DIR=/tmp/deploy
```

### 4.2 备份当前线上版本
```bash
sudo mkdir -p "$BACKUP_DIR"
sudo cp -a "$JAR_PATH" "$BACKUP_DIR/geek-admin.jar"
sudo cp -a "$FRONT_DIR" "$BACKUP_DIR/frontend"
```

### 4.3 （可选）执行数据库迁移
只有本次发布包含 SQL 变更时才执行。

先确认当前线上数据库连接信息（以线上配置为准）：
```bash
sudo grep -nE 'url:|username:|password:' /www/wwwroot/backend/application-data.yml
```

执行 SQL（推荐使用环境变量，不要把密码写死在命令里）：
```bash
DB_HOST=43.142.125.17
DB_PORT=3306
DB_NAME=verifynum
DB_USER=verifyNum

read -s -p "DB password: " DB_PASS
echo
# 先备份关键权限表，防止误操作导致菜单权限丢失
MYSQL_PWD="$DB_PASS" mysqldump -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" --no-create-info --skip-triggers "$DB_NAME" sys_role_menu > "$BACKUP_DIR/sys_role_menu.sql"
MYSQL_PWD="$DB_PASS" mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" "$DB_NAME" < "$TMP_DIR/migration.sql"
unset DB_PASS MYSQL_PWD
```

### 4.4 发布前端
```bash
sudo rm -rf "$FRONT_DIR"/*
sudo cp -a "$TMP_DIR/frontend-dist/." "$FRONT_DIR"/

# 修正权限，避免静态资源 MIME/403/500 异常
sudo find "$FRONT_DIR" -type d -exec chmod 755 {} +
sudo find "$FRONT_DIR" -type f -exec chmod 644 {} +
sudo chown -R www-data:www-data "$FRONT_DIR"
```

### 4.5 发布后端并重启服务
```bash
sudo cp -f "$TMP_DIR/geek-admin.jar" "$JAR_PATH"
sudo systemctl restart geek-admin
sudo systemctl status geek-admin --no-pager
```

### 4.6 校验 Nginx 配置并重载
```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 5. 发布后验收
### 5.1 服务器本机验收（Host 头）
```bash
curl -I -m 8 -H "Host: biaoji.aleo1314.vip" http://127.0.0.1/

ASSET_JS=$(ls /www/wwwroot/frontend/assets/index-*.js | sed -n '1p' | xargs -n1 basename)
curl -I -m 8 -H "Host: biaoji.aleo1314.vip" "http://127.0.0.1/assets/$ASSET_JS"

curl -I -m 8 -H "Host: biaoji.aleo1314.vip" http://127.0.0.1/prod-api/
```

### 5.2 外网验收
```bash
curl -I -m 10 https://biaoji.aleo1314.vip/
curl -I -m 10 https://biaoji.aleo1314.vip/prod-api/
```

### 5.3 日志与端口检查
```bash
ss -tlnp | grep -E ':80|:443|:8080'
sudo journalctl -u geek-admin -n 120 --no-pager
sudo tail -n 120 /var/log/nginx/error.log
```
### 5.4 菜单与权限验收（涉及菜单/权限 SQL 时必做）
```bash
# 一级菜单名称是否正确（当前应为“代理管理”）
sudo mysql -N -D verifynum -e "SELECT menu_id,menu_name FROM sys_menu WHERE menu_id=900100000001;"

# admin 角色菜单数量是否异常偏少（若只剩十几条通常说明被误删）
sudo mysql -N -D verifynum -e "SELECT COUNT(*) AS admin_menu_count FROM sys_role_menu WHERE role_id=1;"
```

验收标准：
- `/` 返回 `200`
- `/assets/index-*.js` 返回 `200` 且 `Content-Type` 为 `application/javascript`
- `/prod-api/` 返回 `200`
- `geek-admin` 服务状态为 `active (running)`

## 6. 回滚流程（发布失败立即执行）
```bash
LAST_BACKUP=$(ls -dt /www/backup/deploy/* | sed -n '1p')

sudo cp -f "$LAST_BACKUP/geek-admin.jar" /www/wwwroot/backend/geek-admin.jar
sudo rm -rf /www/wwwroot/frontend/*
sudo cp -a "$LAST_BACKUP/frontend/." /www/wwwroot/frontend/

sudo find /www/wwwroot/frontend -type d -exec chmod 755 {} +
sudo find /www/wwwroot/frontend -type f -exec chmod 644 {} +
sudo chown -R www-data:www-data /www/wwwroot/frontend

sudo systemctl restart geek-admin
sudo nginx -t && sudo systemctl reload nginx
```

## 7. 日常最短发布流程（前后端都改了）
1. 本地打包前端和后端。
2. 上传 `frontend-dist` 和 `geek-admin.jar` 到 `/tmp/deploy`。
3. 服务器先备份，再替换前端，再替换后端并重启 `geek-admin`。
4. 如有 SQL 变更，执行迁移脚本。
5. 按第 5 节做本机 + 外网验收。

## 8. 常见问题快速判断
### 8.1 页面 500 或 JS MIME 错误
优先检查：
- `location /` 的 root 是否仍为 `/www/wwwroot/frontend`
- `frontend/assets` 权限是否目录 `755`、文件 `644`
- 是否误把旧 hash 的前端文件引用到了新页面

### 8.2 `/prod-api/` 不通
优先检查：
- `sudo systemctl status geek-admin --no-pager`
- `sudo journalctl -u geek-admin -n 200 --no-pager`
- `/www/wwwroot/backend/application-data.yml` 的数据库连接是否正确

### 8.3 Nginx 重载失败
```bash
sudo nginx -t
```
按报错行修复 `biaoji.aleo1314.vip.conf` 后再 reload。
### 8.4 admin 菜单突然变少
优先检查 `sys_role_menu` 是否被误删或覆盖：
```bash
sudo mysql -N -D verifynum -e "SELECT COUNT(*) AS admin_menu_count FROM sys_role_menu WHERE role_id=1;"
```
如果数量异常偏少，先恢复最近备份里的 `sys_role_menu.sql`，再重新执行正确的迁移脚本。
