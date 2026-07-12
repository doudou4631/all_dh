# 测试服务器与正式上线流程说明

## 1. 服务器定位

当前项目采用“先测试服务器验证，再正式服务器上线”的发布流程。

### 1.1 测试服务器

- 用途：日常代码修改、功能联调、数据库恢复验证、上线前预发布测试
- 服务器地址：`212.64.16.212`
- SSH 用户：`ubuntu`
- 本地 SSH 别名：`server212-16`
- 登录方式：

```bash
ssh server212-16
```

测试服务器用于提前部署当前开发版本。以后我们写代码、改前端、改后端、改数据库脚本，都优先部署到这台服务器验证。

### 1.2 正式服务器

- 用途：正式线上生产环境
- 服务器地址：`43.142.125.17`
- SSH 用户：`ubuntu`
- 说明：只有测试服务器验证通过后，才允许发布到正式服务器。

## 2. 当前测试服务器部署状态

测试服务器 `212.64.16.212` 已部署当前项目。

### 2.1 部署路径

- 后端目录：`/www/wwwroot/backend`
- 后端 Jar：`/www/wwwroot/backend/geek-admin.jar`
- 后端配置：`/www/wwwroot/backend/application-data.yml`
- 前端目录：`/www/wwwroot/frontend`
- 文件存储目录：`/data/files/master`
- 数据库名：`verifynum`
- 后端服务：`geek-admin`
- Nginx 入口端口：`80`
- 后端端口：`8080`

### 2.2 当前访问地址

```text
http://212.64.16.212/
```

## 3. 标准发布流程

以后发布必须按以下顺序执行：

1. 本地修改代码。
2. 本地打包前端和后端。
3. 先发布到测试服务器 `212.64.16.212`。
4. 在测试服务器完整验证功能。
5. 验证通过后，按数据库备份文档先备份正式服务器数据库。
6. 再发布到正式服务器 `43.142.125.17`。

## 4. 测试服务器验证重点

每次部署到测试服务器后，至少检查：

- 首页是否正常打开
- 管理端是否能登录
- 后端服务是否正常
- 菜单权限是否正常
- 本次修改的业务流程是否正常
- 涉及数据库变更时，确认表结构和关键数据是否符合预期

常用检查命令：

```bash
ssh server212-16

systemctl is-active mysql nginx geek-admin
ss -tlnp | grep -E ':80|:8080'
curl -I http://127.0.0.1/
curl -I http://127.0.0.1/prod-api/
curl -I http://127.0.0.1/mobile-h5/
```

查看后端日志：

```bash
sudo journalctl -u geek-admin -n 120 --no-pager
```

## 5. 正式上线前要求

正式服务器上线前必须满足：

- 测试服务器已部署成功
- 测试服务器核心功能验证通过
- 若涉及数据库变更，测试库已验证通过
- 已按 `docs/DB_BACKUP_TO_PROJECT_GUIDE.md` 完成正式服务器数据库备份
- 明确本次要发布的前端、后端、SQL 变更范围

## 6. 当前测试环境初始化来源

测试服务器数据库当前由正式服务器备份恢复而来：

- 备份时间戳：`20260712_121417`
- 本地备份目录：`db-backups/20260712_121417`
- 测试服务器备份留存目录：`/tmp/db-backup/20260712_121417`

该环境作为后续测试、预发布、联调使用。正式上线仍以正式服务器当前数据为准，不能直接用测试服务器数据覆盖正式服务器。

## 7. 从测试服务器验证通过到正式服务器上线

### 7.1 核心原则

- 测试服务器验证通过后，正式上线只发布“代码产物”和“必要 SQL 变更”。
- 不要直接把测试服务器数据库覆盖到正式服务器。
- 不要把测试服务器的 `application-data.yml` 覆盖正式服务器配置，正式服务器继续使用自己的线上配置。
- 正式上线前必须先按 `docs/DB_BACKUP_TO_PROJECT_GUIDE.md` 对正式服务器执行数据库备份。

### 7.2 推荐上线方式：本地同一份构建产物发布到正式服务器

推荐流程是：本地打包一次，先部署测试服务器验证；验证通过后，把同一份本地构建产物发布到正式服务器。

本地项目根目录执行：

```powershell
$ProjectRoot = "C:\Users\doupc01\Desktop\all"
$SshKey = "$HOME\.ssh\id_rsa_43_142_125_17"
$SshUser = "ubuntu"
$ProdHost = "43.142.125.17"

# 1) 清理正式服务器临时发布目录
ssh -i $SshKey ${SshUser}@${ProdHost} "rm -rf /tmp/deploy && mkdir -p /tmp/deploy"

# 2) 上传前端和后端产物
scp -i $SshKey -r "$ProjectRoot\frontend\dist" ${SshUser}@${ProdHost}:/tmp/deploy/frontend-dist
scp -i $SshKey "$ProjectRoot\backend\geek-admin\target\geek-admin.jar" ${SshUser}@${ProdHost}:/tmp/deploy/geek-admin.jar

# 3) 上传后做结构校验
ssh -i $SshKey ${SshUser}@${ProdHost} "test -f /tmp/deploy/frontend-dist/index.html && test -d /tmp/deploy/frontend-dist/assets && test ! -d /tmp/deploy/frontend-dist/dist && test -f /tmp/deploy/geek-admin.jar && echo deploy-package-ok"
```

### 7.3 正式服务器发布命令

登录正式服务器后执行：

```bash
ssh -i ~/.ssh/id_rsa_43_142_125_17 ubuntu@43.142.125.17
```

正式服务器执行：

```bash
set -e

TS=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/www/backup/deploy/$TS
FRONT_DIR=/www/wwwroot/frontend
BACKEND_DIR=/www/wwwroot/backend
JAR_PATH=$BACKEND_DIR/geek-admin.jar
TMP_DIR=/tmp/deploy

# 1) 发布前备份当前正式服前后端
sudo mkdir -p "$BACKUP_DIR"
if [ -f "$JAR_PATH" ]; then
  sudo cp -a "$JAR_PATH" "$BACKUP_DIR/geek-admin.jar"
fi
if [ -d "$FRONT_DIR" ]; then
  sudo cp -a "$FRONT_DIR" "$BACKUP_DIR/frontend"
fi

# 2) 校验发布包结构
test -f "$TMP_DIR/frontend-dist/index.html"
test -d "$TMP_DIR/frontend-dist/assets"
test ! -d "$TMP_DIR/frontend-dist/dist"
test -f "$TMP_DIR/geek-admin.jar"

# 3) 发布前端
sudo mkdir -p "$FRONT_DIR"
sudo rm -rf "$FRONT_DIR"/*
sudo cp -a "$TMP_DIR/frontend-dist/." "$FRONT_DIR"/
sudo find "$FRONT_DIR" -type d -exec chmod 755 {} +
sudo find "$FRONT_DIR" -type f -exec chmod 644 {} +
sudo chown -R www-data:www-data "$FRONT_DIR"

# 4) 发布后端
sudo mkdir -p "$BACKEND_DIR"
sudo cp -f "$TMP_DIR/geek-admin.jar" "$JAR_PATH"
sudo chown -R ubuntu:ubuntu "$BACKEND_DIR"

# 5) 重启服务
sudo systemctl restart geek-admin
sudo nginx -t
sudo systemctl reload nginx

echo "PROD_DEPLOY_TS=$TS"
echo "PROD_BACKUP_DIR=$BACKUP_DIR"
```

### 7.4 如果本次包含数据库变更

如果本次改动包含 SQL 脚本，正式上线时必须：

1. 先按 `docs/DB_BACKUP_TO_PROJECT_GUIDE.md` 对正式服务器执行 `5.3` 全量备份。
2. 确认备份已下载到本地 `db-backups/<TS>/` 并通过 `gzip -t` 校验。
3. 再把经过测试服务器验证的 SQL 脚本上传到正式服务器。
4. 在正式服务器执行 SQL。
5. 执行后立刻验证关键表、菜单权限和本次业务功能。

示例：

```powershell
$SshKey = "$HOME\.ssh\id_rsa_43_142_125_17"
$ProdHost = "43.142.125.17"
scp -i $SshKey ".\backend\sql\本次变更.sql" ubuntu@${ProdHost}:/tmp/deploy/migration.sql
```

正式服务器执行：

```bash
DB_NAME=verifynum
DB_USER=verifyNum
DB_HOST=127.0.0.1
DB_PORT=3306

read -s -p "DB password: " DB_PASS
echo
MYSQL_PWD="$DB_PASS" mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" "$DB_NAME" < /tmp/deploy/migration.sql
unset DB_PASS MYSQL_PWD
```

### 7.5 正式上线后验证

正式服务器发布后至少验证：

```bash
systemctl is-active mysql nginx geek-admin
ss -tlnp | grep -E ':80|:443|:8080'

curl -I http://127.0.0.1/
curl -I http://127.0.0.1/prod-api/
curl -I http://127.0.0.1/mobile-h5/

sudo journalctl -u geek-admin -n 120 --no-pager
```

如果域名仍指向正式服务器，也要验证：

```bash
curl -I https://biaoji.aleo1314.vip/
curl -I https://biaoji.aleo1314.vip/prod-api/
curl -I https://biaoji.aleo1314.vip/mobile-h5/
```

### 7.6 回滚方式

如果正式服务器上线后发现问题，可以用第 `7.3` 节生成的 `PROD_BACKUP_DIR` 回滚前后端：

```bash
BACKUP_DIR=/www/backup/deploy/替换成实际时间戳
FRONT_DIR=/www/wwwroot/frontend
JAR_PATH=/www/wwwroot/backend/geek-admin.jar

sudo systemctl stop geek-admin

if [ -f "$BACKUP_DIR/geek-admin.jar" ]; then
  sudo cp -f "$BACKUP_DIR/geek-admin.jar" "$JAR_PATH"
fi

if [ -d "$BACKUP_DIR/frontend" ]; then
  sudo rm -rf "$FRONT_DIR"/*
  sudo cp -a "$BACKUP_DIR/frontend/." "$FRONT_DIR"/
  sudo chown -R www-data:www-data "$FRONT_DIR"
fi

sudo systemctl start geek-admin
sudo nginx -t
sudo systemctl reload nginx
```

数据库回滚不能直接套用前后端回滚命令，必须根据正式上线前的数据库备份和本次 SQL 变更范围单独处理。

## 8. 手机端方案三：根路径自动进入若依移动端

当前采用“方案三”：

- PC 访问 `http://212.64.16.212/`：继续显示现有管理端/前端。
- 手机访问 `http://212.64.16.212/`：由主前端入口自动跳转到 `http://212.64.16.212/mark-app/`。
- 若需要在手机上临时查看 PC 端，可访问 `http://212.64.16.212/?pc=1` 或 `http://212.64.16.212/?desktop=1`。

移动端代码目录：

```text
frontend/ruoyi-app
```

H5 发布路径：

```text
frontend/public/mark-app
/www/wwwroot/frontend/mark-app
```

标准构建顺序：

```bash
cd frontend/ruoyi-app
npm install
npm run build:h5

cd ../
npm run sync:ruoyi-app-h5
npm run build:prod
```

测试服务器部署后至少验证：

```bash
curl -I http://127.0.0.1/
curl -I http://127.0.0.1/mark-app/
curl -I http://127.0.0.1/prod-api/
```

手机端跳转是前端 JS 执行的，不是 Nginx 302；`curl -I` 不会看到 `Location`。可先确认入口脚本已发布：

```bash
curl -s http://127.0.0.1/ | grep -E "mark-app|方案三"
```

最终以手机浏览器或浏览器 DevTools 手机模式访问 `http://212.64.16.212/`，确认自动进入 `/mark-app/`。
