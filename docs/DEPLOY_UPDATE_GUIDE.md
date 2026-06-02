# 前后端发布更新手册（当前线上环境）
适用场景：你后续修改了 `frontend` 或 `backend` 代码，需要把新版本发布到当前生产服务器。

## 1. 当前线上环境基线（以此为准）
- 服务器登录用户：`ubuntu`（`sudo`）
- SSH 免密登录：已配置（推荐固定私钥：`~/.ssh/id_rsa_43_142_125_17`）
- Nginx 站点配置：`/etc/nginx/sites-available/biaoji.aleo1314.vip.conf`
- 前端目录：`/www/wwwroot/frontend`
- 手机端页面目录（线上）：`/www/wwwroot/frontend/mobile-h5`
- 手机端访问路径：`https://biaoji.aleo1314.vip/mobile-h5/`
- 手机端源码目录（仓库）：`frontend/public/mobile-h5`
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
手机端产物目录：`frontend/dist/mobile-h5`
说明：旧目录 `frontend/dist/free-query-ui` 已迁移为 `mobile-h5`

### 2.2 后端打包
```bash
mvn -f backend/pom.xml clean package -DskipTests
```
产物路径：`backend/geek-admin/target/geek-admin.jar`

## 3. 上传发布包到服务器
建议统一放到服务器 `/tmp/deploy`。
### 3.1 常规上传（前后端或前端整包发布）

```bash
KEY_OPTS="-i ~/.ssh/id_rsa_43_142_125_17"
ssh $KEY_OPTS ubuntu@43.142.125.17 "mkdir -p /tmp/deploy && rm -rf /tmp/deploy/frontend-dist /tmp/deploy/geek-admin.jar /tmp/deploy/migration.sql"

scp $KEY_OPTS -r ./frontend/dist ubuntu@43.142.125.17:/tmp/deploy/frontend-dist
scp $KEY_OPTS ./backend/geek-admin/target/geek-admin.jar ubuntu@43.142.125.17:/tmp/deploy/geek-admin.jar

# 如果本次有数据库变更，再上传 SQL（文件名示例）
scp $KEY_OPTS ./backend/sql/m4_mark_migration.sql ubuntu@43.142.125.17:/tmp/deploy/migration.sql

# 上传后做结构校验（必须通过）：防止出现 /tmp/deploy/frontend-dist/dist 嵌套
ssh $KEY_OPTS ubuntu@43.142.125.17 '
  test -f /tmp/deploy/frontend-dist/index.html &&
  test -d /tmp/deploy/frontend-dist/assets &&
  test ! -d /tmp/deploy/frontend-dist/dist &&
  echo "frontend-dist 结构OK"
'
```

### 3.2 仅手机端快速上传（可选）
仅当本次变更只涉及 `frontend/public/mobile-h5/**` 时使用：
```bash
KEY_OPTS="-i ~/.ssh/id_rsa_43_142_125_17"
ssh $KEY_OPTS ubuntu@43.142.125.17 "mkdir -p /tmp/deploy && rm -rf /tmp/deploy/mobile-h5"
scp $KEY_OPTS -r ./frontend/public/mobile-h5 ubuntu@43.142.125.17:/tmp/deploy/mobile-h5

# 上传后做结构校验（必须通过）：防止出现 /tmp/deploy/mobile-h5/mobile-h5 嵌套
ssh $KEY_OPTS ubuntu@43.142.125.17 '
  test -f /tmp/deploy/mobile-h5/index.html &&
  test ! -d /tmp/deploy/mobile-h5/mobile-h5 &&
  echo "mobile-h5 结构OK"
'
```
如果改动包含 `frontend/src/**` 或其他会影响 `dist` 构建产物的文件，必须走 3.1 常规上传。

## 4. 服务器发布步骤（SSH 后执行）
```bash
ssh -i ~/.ssh/id_rsa_43_142_125_17 ubuntu@43.142.125.17
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

### 4.4 常规发布前端（整包）
```bash
# 发布前防呆校验（必须通过）：防止把错误目录结构发布到线上
sudo test -f "$TMP_DIR/frontend-dist/index.html"
sudo test -d "$TMP_DIR/frontend-dist/assets"
sudo test ! -d "$TMP_DIR/frontend-dist/dist"
sudo rm -rf "$FRONT_DIR"/*
sudo cp -a "$TMP_DIR/frontend-dist/." "$FRONT_DIR"/

# 修正权限，避免静态资源 MIME/403/500 异常
sudo find "$FRONT_DIR" -type d -exec chmod 755 {} +
sudo find "$FRONT_DIR" -type f -exec chmod 644 {} +
sudo chown -R www-data:www-data "$FRONT_DIR"
```
### 4.5 （可选）仅手机端快速发布（mobile-h5）
仅当本次变更只涉及 `frontend/public/mobile-h5/**` 时使用：
```bash
if [ -d "$FRONT_DIR/mobile-h5" ]; then
  sudo cp -a "$FRONT_DIR/mobile-h5" "$BACKUP_DIR/mobile-h5"
fi
sudo rm -rf "$FRONT_DIR/mobile-h5"
sudo cp -a "$TMP_DIR/mobile-h5" "$FRONT_DIR/mobile-h5"

sudo find "$FRONT_DIR/mobile-h5" -type d -exec chmod 755 {} +
sudo find "$FRONT_DIR/mobile-h5" -type f -exec chmod 644 {} +
sudo chown -R www-data:www-data "$FRONT_DIR/mobile-h5"
```

### 4.6 发布后端并重启服务
```bash
sudo cp -f "$TMP_DIR/geek-admin.jar" "$JAR_PATH"
sudo systemctl restart geek-admin
sudo systemctl status geek-admin --no-pager
```
### 4.7 校验 Nginx 配置并重载
```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 5. 发布后验收
### 5.1 服务器本机验收（Host 头）
```bash
curl -I -m 8 -H "Host: biaoji.aleo1314.vip" http://127.0.0.1/
ASSET_JS=$(grep -oE '/assets/index-[^"]+\.js' /www/wwwroot/frontend/index.html | sed -n '1p' | sed 's#^/assets/##')
test -n "$ASSET_JS"
curl -I -m 8 -H "Host: biaoji.aleo1314.vip" "http://127.0.0.1/assets/$ASSET_JS"

curl -I -m 8 -H "Host: biaoji.aleo1314.vip" http://127.0.0.1/prod-api/
curl -I -m 8 -H "Host: biaoji.aleo1314.vip" http://127.0.0.1/mobile-h5/
curl -I -m 8 -H "Host: biaoji.aleo1314.vip" http://127.0.0.1/mobile-h5/assets/result-page.js
```

### 5.2 外网验收
```bash
curl -I -m 10 https://biaoji.aleo1314.vip/
curl -I -m 10 https://biaoji.aleo1314.vip/prod-api/
curl -I -m 10 https://biaoji.aleo1314.vip/mobile-h5/
curl -I -m 10 https://biaoji.aleo1314.vip/mobile-h5/assets/result-page.js

# 可选：外网首页实时提取正在引用的入口 JS 并校验（排查 CDN/缓存时很有用）
ASSET_JS=$(curl -fsSL https://biaoji.aleo1314.vip/ | grep -oE '/assets/index-[^"]+\.js' | sed -n '1p')
test -n "$ASSET_JS"
curl -I -m 10 "https://biaoji.aleo1314.vip$ASSET_JS"
```

### 5.3 日志与端口检查
```bash
ss -tlnp | grep -E ':80|:443|:8080'
sudo journalctl -u geek-admin -n 120 --no-pager
sudo tail -n 120 /var/log/nginx/error.log
```
### 5.4 菜单与权限验收（涉及菜单/权限 SQL 时必做）
```bash
# 一级菜单名称是否正确（当前应为“标记业务管理”）
sudo mysql -N -D verifynum -e "SELECT menu_id,menu_name FROM sys_menu WHERE menu_id=900100000001;"

# admin 角色菜单数量是否异常偏少（若只剩十几条通常说明被误删）
sudo mysql -N -D verifynum -e "SELECT COUNT(*) AS admin_menu_count FROM sys_role_menu WHERE role_id=1;"
```

验收标准：
- `/` 返回 `200`
- `/assets/index-*.js` 返回 `200` 且 `Content-Type` 为 `application/javascript`
- `/prod-api/` 返回 `200`
- `/mobile-h5/` 返回 `200`
- `/mobile-h5/assets/result-page.js` 返回 `200` 且 `Content-Type` 为 `application/javascript`
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

## 7. 日常发布流程
### 7.1 前后端都改了（常规）
1. 本地打包前端和后端。
2. 上传前先清空 `/tmp/deploy` 旧包，再上传 `frontend-dist` 和 `geek-admin.jar`（见 3.1）。
3. 上传后立即做结构校验：必须有 `frontend-dist/index.html`、`frontend-dist/assets`，且不能有 `frontend-dist/dist`。
4. 服务器先备份，再执行 4.4 的发布前防呆校验，通过后再替换前端。
5. 替换后端并重启 `geek-admin`；如有 SQL 变更，执行迁移脚本。
6. 按第 5 节做本机 + 外网验收（含按 `index.html` 提取真实入口 JS 校验）。

### 7.2 仅手机端改了（快速）
1. 确认变更仅在 `frontend/public/mobile-h5/**`。
2. 上传 `mobile-h5` 到 `/tmp/deploy/mobile-h5`（见 3.2）。
3. 服务器备份并替换 `/www/wwwroot/frontend/mobile-h5`（见 4.5）。
4. 验收 `/mobile-h5/` 与 `/mobile-h5/assets/result-page.js`。

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

### 8.5 手机端页面 404 或未生效
优先检查：
- `/www/wwwroot/frontend/mobile-h5` 是否存在且权限正确（目录 `755`、文件 `644`）。
- 本次是否误用“快速发布”：若改动包含 `frontend/src/**`，必须走 3.1 常规整包发布。
- `https://biaoji.aleo1314.vip/mobile-h5/assets/result-page.js` 是否返回 `200` 且 `Content-Type` 为 `application/javascript`。

### 8.6 前端已发布但页面仍是旧版（`frontend-dist/dist` 嵌套）
典型现象：
- 发布流程看起来成功，但页面仍是旧内容。
- `/tmp/deploy/frontend-dist` 下存在异常嵌套（如 `dist/` 子目录），导致复制到了错误层级文件。

快速修复：
```bash
KEY_OPTS="-i ~/.ssh/id_rsa_43_142_125_17"
ssh $KEY_OPTS ubuntu@43.142.125.17 "rm -rf /tmp/deploy/frontend-dist && mkdir -p /tmp/deploy"
scp $KEY_OPTS -r ./frontend/dist ubuntu@43.142.125.17:/tmp/deploy/frontend-dist
ssh $KEY_OPTS ubuntu@43.142.125.17 '
  test -f /tmp/deploy/frontend-dist/index.html &&
  test -d /tmp/deploy/frontend-dist/assets &&
  test ! -d /tmp/deploy/frontend-dist/dist &&
  echo "frontend-dist 结构OK"
'
```
然后重新执行 4.4 前端发布步骤与第 5 节验收步骤。

### 8.7 `UNPROTECTED PRIVATE KEY FILE`（私钥权限过宽）
典型报错：
- `WARNING: UNPROTECTED PRIVATE KEY FILE!`
- `Permissions for '...id_rsa_43_142_125_17' are too open`

处理方式（在本地执行）：
```bash
# Linux/macOS
chmod 700 ~/.ssh
chmod 600 ~/.ssh/id_rsa_43_142_125_17
```

如果是 Windows + OpenSSH，需去掉该私钥文件对其他用户/组的读权限，仅保留当前登录用户可读写。
