# 数据库备份到当前项目操作指南
## 1. 适用场景
在以下场景执行前，先做数据库备份并保存到当前项目目录：
- 要发布后端代码（尤其是 `backend/**` 中涉及 Mapper、Service、SQL 的改动）
- 要执行任何数据库脚本（如 `backend/sql/*.sql`）
- 要在服务器上手工改表结构、改数据、改权限菜单
## 1.1 长期执行策略（本项目默认）
- 本文档默认每次都执行 `5.3`（整库 + 关键表 + 元信息），作为固定标准流程。
- `5.1/5.2/5.4` 仅作为特殊场景备选，不作为日常发版前备份流程。
- 未完成“服务器导出 + 下载到项目 + 本地完整性校验”前，不执行发版或改库操作。
- 每次备份完成后，在第 10 节追加一条记录，保证可追溯。

## 2. 本项目优先备份哪个库
- 主库：`verifynum`

建议原则：
- 不确定影响范围时，**直接备份整库 `verifynum`**
- 如果是权限/菜单改动，至少额外备份：`sys_role_menu`、`sys_menu`、`sys_role`
- 如果是字典/配置改动，额外备份：`sys_dict_type`、`sys_dict_data`、`sys_config`
- 如果是业务功能改动，额外备份对应业务表（如 `mark_*`、`user_*`、`batch_task_record` 等）

## 3. 项目内备份目录规范
在项目根目录下统一存放：

`db-backups/<时间戳>/`

示例：
- `db-backups/20260601_123000/verifynum_full.sql.gz`
- `db-backups/20260601_123000/key_tables.sql.gz`
- `db-backups/20260601_123000/backup-meta.txt`
## 4. 当前项目固定服务器信息
- SSH 用户：`ubuntu`
- 服务器地址：`43.142.125.17`
- SSH 私钥：`~/.ssh/id_rsa_43_142_125_17`
- 数据库名：`verifynum`
- 数据库用户：`verifyNum`
- 数据库端口：`3306`
- 线上后端配置文件：`/www/wwwroot/backend/application-data.yml`

## 5. 服务器上导出备份（SSH 登录后执行）
先登录服务器：

```bash path=null start=null
ssh -i ~/.ssh/id_rsa_43_142_125_17 ubuntu@43.142.125.17
```
重要（避免踩坑）：
- 当备份目录在 `/www/backup/**` 这类 root 权限目录时，不要写 `sudo mysqldump ... > 文件`。
- 原因：`>` 重定向由当前 shell 执行，不会自动继承 `sudo`，容易出现 `Permission denied`。
- 推荐写法：`sudo mysqldump ... | sudo tee 文件 > /dev/null`，让写文件动作在 sudo 权限下执行，并避免变量作用域问题。
### 5.1 备份整个业务库（推荐，最常用）
```bash path=null start=null
TS=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/www/backup/deploy/$TS
sudo mkdir -p "$BACKUP_DIR"
sudo mysqldump \
  --single-transaction --routines --triggers --events --set-gtid-purged=OFF \
  verifynum | sudo tee "$BACKUP_DIR/verifynum_full.sql" > /dev/null

sudo gzip -f "$BACKUP_DIR/verifynum_full.sql"
sudo ls -lh "$BACKUP_DIR"
```

### 5.2 备份整个 MySQL 实例（所有库，可选）
```bash path=null start=null
TS=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/www/backup/deploy/$TS
sudo mkdir -p "$BACKUP_DIR"
sudo mysqldump \
  --single-transaction --routines --triggers --events --set-gtid-purged=OFF \
  --all-databases | sudo tee "$BACKUP_DIR/mysql_all_databases.sql" > /dev/null

sudo gzip -f "$BACKUP_DIR/mysql_all_databases.sql"
sudo ls -lh "$BACKUP_DIR"
```
### 5.3 默认标准流程（始终执行：整库 + 关键表 + 元信息）
```bash path=null start=null
TS=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/www/backup/deploy/$TS
sudo mkdir -p "$BACKUP_DIR"

# 1) 整库备份（兜底保障）
sudo mysqldump \
  --single-transaction --routines --triggers --events --set-gtid-purged=OFF \
  verifynum | sudo tee "$BACKUP_DIR/verifynum_full.sql" > /dev/null
sudo gzip -f "$BACKUP_DIR/verifynum_full.sql"

# 2) 菜单/权限/配置关键表备份（快速定点核验）
sudo mysqldump \
  --single-transaction --set-gtid-purged=OFF \
  verifynum sys_role_menu sys_menu sys_role sys_dict_type sys_dict_data sys_config \
  | sudo tee "$BACKUP_DIR/key_tables.sql" > /dev/null
sudo gzip -f "$BACKUP_DIR/key_tables.sql"

# 3) 记录备份元信息
OPERATOR=$(whoami)
printf 'time=%s\ndb=verifynum\noperator=%s\nnote=before menu/db change\n' "$TS" "$OPERATOR" | sudo tee "$BACKUP_DIR/backup-meta.txt" > /dev/null

sudo ls -lh "$BACKUP_DIR"
echo "BACKUP_TS=$TS"
echo "BACKUP_DIR=$BACKUP_DIR"
```

### 5.4 使用业务库账号导出（备选方案）
```bash path=null start=null
TS=$(date +%Y%m%d_%H%M%S)
DB_HOST=43.142.125.17
DB_PORT=3306
DB_NAME=verifynum
DB_USER=verifyNum
BACKUP_DIR=/tmp/db-backup/$TS
mkdir -p "$BACKUP_DIR"

read -s -p "DB password: " DB_PASS
echo
MYSQL_PWD="$DB_PASS" mysqldump \
  -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" \
  --single-transaction --routines --triggers --events --set-gtid-purged=OFF \
  "$DB_NAME" > "$BACKUP_DIR/verifynum_full.sql"

MYSQL_PWD="$DB_PASS" mysqldump \
  -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" \
  --single-transaction --set-gtid-purged=OFF \
  "$DB_NAME" sys_role_menu sys_menu sys_role sys_dict_type sys_dict_data sys_config \
  > "$BACKUP_DIR/key_tables.sql"
gzip -f "$BACKUP_DIR/verifynum_full.sql"
gzip -f "$BACKUP_DIR/key_tables.sql"

cat > "$BACKUP_DIR/backup-meta.txt" <<EOF
time=$TS
db=$DB_NAME
host=$DB_HOST
port=$DB_PORT
operator=$(whoami)
note=before deploy/code change
EOF

unset DB_PASS MYSQL_PWD
ls -lh "$BACKUP_DIR"
```

## 6. 下载到当前项目目录（本地执行）
在项目根目录执行（Windows PowerShell 示例）：

```powershell path=null start=null
$ProjectRoot = "C:\Users\doupc01\Desktop\all"
$ServerTs = "20260601_123000"   # 改成服务器上实际时间戳
$SshKey = "$HOME\.ssh\id_rsa_43_142_125_17"
$SshUser = "ubuntu"
$ServerHost = "43.142.125.17"
$LocalDir = Join-Path $ProjectRoot "db-backups\$ServerTs"
New-Item -ItemType Directory -Path $LocalDir -Force | Out-Null
# 默认执行 5.3（标准流程：整库 + 关键表 + 元信息）
scp -i $SshKey ${SshUser}@${ServerHost}:/www/backup/deploy/$ServerTs/* $LocalDir

# 若执行的是 5.1（仅整库）
# scp -i $SshKey ${SshUser}@${ServerHost}:/www/backup/deploy/$ServerTs/verifynum_full.sql.gz $LocalDir

# 若执行的是 5.2（整实例）
# scp -i $SshKey ${SshUser}@${ServerHost}:/www/backup/deploy/$ServerTs/mysql_all_databases.sql.gz $LocalDir
# 若执行的是 5.4（业务库账号导出）
# scp -i $SshKey ${SshUser}@${ServerHost}:/tmp/db-backup/$ServerTs/*.sql.gz $LocalDir
# scp -i $SshKey ${SshUser}@${ServerHost}:/tmp/db-backup/$ServerTs/backup-meta.txt $LocalDir

Get-ChildItem $LocalDir | Select-Object Name,Length,LastWriteTime
```
下载成功后，按第 9 节执行服务器清理，避免临时备份长期堆积。

## 7. 发版前必须备份清单（强制）
每次发布前，以下文件必须全部存在于项目中：
- `db-backups/<TS>/verifynum_full.sql.gz`
- `db-backups/<TS>/key_tables.sql.gz`
- `db-backups/<TS>/backup-meta.txt`

## 8. 注意事项
- 不要把数据库密码写死在命令里，使用交互输入或环境变量。
- 若导出到 `/www/backup/**`，优先使用 `sudo mysqldump ... | sudo tee 文件 > /dev/null`，避免重定向权限和变量作用域问题。
- 备份文件通常包含敏感数据，默认建议仅本地留存，不直接提交到远程仓库。
- 备份完成后再执行迁移或发版，避免“改完才发现没备份”。
- 下载到本地后，建议执行 `gzip -t` 校验压缩文件完整性，再继续后续操作。
- 每次备份后及时在第 10 节追加记录（时间戳、文件大小、操作人、备注）。

## 9. 下载完成后的服务器清理（新增）
原则：
- `/tmp/db-backup/<TS>/` 属于临时目录，下载成功后应立即删除。
- `/www/backup/deploy/<TS>/` 建议按发布周期保留（如 7~14 天），不要无限堆积。

### 9.1 清理临时目录（推荐立刻执行）
```bash path=null start=null
TS=20260601_123000   # 改成本次备份时间戳
rm -rf "/tmp/db-backup/$TS"
```

### 9.2 清理服务器长期备份（按需）
若你只保留本地副本，也可删除本次服务器备份：
```bash path=null start=null
TS=20260601_123000
sudo rm -rf "/www/backup/deploy/$TS"
```

### 9.3 按保留期自动清理旧备份（推荐）
示例：只保留最近 14 天目录。
```bash path=null start=null
sudo find /www/backup/deploy -mindepth 1 -maxdepth 1 -type d -mtime +14 -exec rm -rf {} +
```

### 9.4 清理前确认（必须）
执行删除前，先确认本地项目目录备份完整：
- `db-backups/<TS>/verifynum_full.sql.gz` 存在且文件大小正常
- 若有关键表备份，`db-backups/<TS>/key_tables.sql(.gz)` 也已下载完成
## 10. 全量备份执行记录（按时间倒序）
### 10.1 20260609_032942
- 备份策略：按 5.3 执行（整库 + 关键表 + 元信息）
- 服务器目录：`/www/backup/deploy/20260609_032942`
- 本地目录：`db-backups/20260609_032942`
- 本地落盘文件：
  - `verifynum_full.sql.gz`（856521 bytes）
  - `key_tables.sql.gz`（11525 bytes）
  - `backup-meta.txt`（81 bytes）
- 完整性校验：`gzip -t` 已通过（`verifynum_full.sql.gz`、`key_tables.sql.gz`）
### 10.2 20260609_030137
- 备份策略：按 5.3 执行（整库 + 关键表 + 元信息）
- 服务器目录：`/www/backup/deploy/20260609_030137`
- 本地目录：`db-backups/20260609_030137`
- 本地落盘文件：
  - `verifynum_full.sql.gz`（856520 bytes）
  - `key_tables.sql.gz`（11524 bytes）
  - `backup-meta.txt`（81 bytes）
- 备注：`/www/backup/deploy/20260609_030110` 为一次失败尝试产生的空目录，可按第 9 节清理。
## 11. 新服务器重建可用性结论（20260609 实测）
- 结论：当前备份可直接用于新服务器恢复数据库数据（已做导入实测）。
- 实测方式：在原服务器创建临时库 `verifynum_restore_check_20260609_030920`，导入 `verifynum_full.sql.gz` 后对比关键指标。
- 实测结果（源库 `verifynum` vs 临时恢复库）：
  - 表总数：`54 vs 54`
  - `sys_menu`：`188 vs 188`
  - `sys_role`：`4 vs 4`
  - `sys_role_menu`：`218 vs 218`
  - `sys_config`：`13 vs 13`
  - routines / triggers / events：`0 / 0 / 0`（两边一致）
- 兼容性前提：
  - 目标 MySQL 版本与字符集需兼容（源端为 MySQL `8.0.45`）。
  - 目标端执行恢复的账号需具备建库、建表、写入权限。
- 重要：`mysqldump verifynum` 备份通常不包含“建库/建用户授权”步骤，新服务器需先完成库和用户权限初始化。
## 12. 新服务器迁移时，除数据库外必须准备的资产
### 12.1 P0（必须）
- 后端程序与配置：`/www/wwwroot/backend`（含 `geek-admin.jar`、`application-data.yml`）
- 前端静态资源：`/www/wwwroot/frontend`
- 本地文件存储：`/data/files/master`
- systemd 服务：`/etc/systemd/system/geek-admin.service`
- Nginx 站点配置：
  - `/etc/nginx/sites-available/biaoji.aleo1314.vip.conf`
  - `/etc/nginx/sites-enabled/biaoji.aleo1314.vip.conf`（软链）
- HTTPS 证书与续期配置：
  - `/etc/letsencrypt/live/biaoji.aleo1314.vip`
  - `/etc/letsencrypt/archive/biaoji.aleo1314.vip`
  - `/etc/letsencrypt/renewal/biaoji.aleo1314.vip.conf`
### 12.2 P1（建议）
- root 定时任务（按需迁移）：`crontab -l -u root` 中现有任务
- 证书自动续期与 Nginx reload 流程联调
### 12.3 新服务器数据库恢复参考流程
在新服务器执行（先创建库与账号，再导入备份）：
```bash path=null start=null
DB_NAME=verifynum
DB_USER=verifyNum
DB_PASS='{{DB_PASSWORD}}'
BACKUP_GZ=/path/to/verifynum_full.sql.gz

sudo mysql <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'%';
FLUSH PRIVILEGES;
SQL

gunzip -c "$BACKUP_GZ" | mysql "$DB_NAME"
mysql -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}';"
```
## 13. 长期执行模板（每次都走 5.3）
### 13.1 服务器端导出（SSH 后执行）
```bash path=null start=null
TS=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/www/backup/deploy/$TS
sudo mkdir -p "$BACKUP_DIR"

sudo mysqldump \
  --single-transaction --routines --triggers --events --set-gtid-purged=OFF \
  verifynum | sudo tee "$BACKUP_DIR/verifynum_full.sql" > /dev/null
sudo gzip -f "$BACKUP_DIR/verifynum_full.sql"

sudo mysqldump \
  --single-transaction --set-gtid-purged=OFF \
  verifynum sys_role_menu sys_menu sys_role sys_dict_type sys_dict_data sys_config \
  | sudo tee "$BACKUP_DIR/key_tables.sql" > /dev/null
sudo gzip -f "$BACKUP_DIR/key_tables.sql"

OPERATOR=$(whoami)
printf 'time=%s\ndb=verifynum\noperator=%s\nnote=before deploy/full backup\n' "$TS" "$OPERATOR" | sudo tee "$BACKUP_DIR/backup-meta.txt" > /dev/null

sudo ls -lh "$BACKUP_DIR"
echo "BACKUP_TS=$TS"
```
### 13.2 本地下载与完整性校验（PowerShell）
```powershell path=null start=null
$ProjectRoot = "C:\Users\doupc01\Desktop\all"
$ServerTs = "20260601_123000"   # 改成服务器输出的 BACKUP_TS
$SshKey = "$HOME\.ssh\id_rsa_43_142_125_17"
$SshUser = "ubuntu"
$ServerHost = "43.142.125.17"
$LocalDir = Join-Path $ProjectRoot "db-backups\$ServerTs"
New-Item -ItemType Directory -Path $LocalDir -Force | Out-Null

scp -i $SshKey ${SshUser}@${ServerHost}:/www/backup/deploy/$ServerTs/* $LocalDir
Get-ChildItem $LocalDir | Select-Object Name,Length,LastWriteTime

gzip -t (Join-Path $LocalDir "verifynum_full.sql.gz")
gzip -t (Join-Path $LocalDir "key_tables.sql.gz")
```
### 13.3 判定完成标准
- `db-backups/<TS>/` 下同时存在 `verifynum_full.sql.gz`、`key_tables.sql.gz`、`backup-meta.txt`
- `gzip -t` 校验通过（退出码为 0）
- 第 10 节已追加本次备份记录

