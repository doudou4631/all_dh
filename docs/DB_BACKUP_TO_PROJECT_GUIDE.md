# 数据库备份到当前项目操作指南
## 1. 适用场景
在以下场景执行前，先做数据库备份并保存到当前项目目录：
- 要发布后端代码（尤其是 `backend/**` 中涉及 Mapper、Service、SQL 的改动）
- 要执行任何数据库脚本（如 `backend/sql/*.sql`）
- 要在服务器上手工改表结构、改数据、改权限菜单

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
- `db-backups/20260601_123000/verifynum_full.sql`
- `db-backups/20260601_123000/key_tables.sql`
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
### 5.1 备份整个业务库（推荐，最常用）
```bash path=null start=null
TS=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/www/backup/deploy/$TS
sudo mkdir -p "$BACKUP_DIR"

sudo mysqldump \
  --single-transaction --routines --triggers --events --set-gtid-purged=OFF \
  verifynum > "$BACKUP_DIR/verifynum_full.sql"

sudo gzip "$BACKUP_DIR/verifynum_full.sql"
sudo ls -lh "$BACKUP_DIR"
```

### 5.2 备份整个 MySQL 实例（所有库，可选）
```bash path=null start=null
TS=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/www/backup/deploy/$TS
sudo mkdir -p "$BACKUP_DIR"

sudo mysqldump \
  --single-transaction --routines --triggers --events --set-gtid-purged=OFF \
  --all-databases > "$BACKUP_DIR/mysql_all_databases.sql"

sudo gzip "$BACKUP_DIR/mysql_all_databases.sql"
sudo ls -lh "$BACKUP_DIR"
```

### 5.3 使用业务库账号导出（备选方案）
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

# 若执行的是 5.1（整库）
scp -i $SshKey ${SshUser}@${ServerHost}:/www/backup/deploy/$ServerTs/verifynum_full.sql.gz $LocalDir

# 若执行的是 5.2（整实例）
# scp -i $SshKey ${SshUser}@${ServerHost}:/www/backup/deploy/$ServerTs/mysql_all_databases.sql.gz $LocalDir

# 若执行的是 5.3（业务库账号导出）
# scp -i $SshKey ${SshUser}@${ServerHost}:/tmp/db-backup/$ServerTs/*.sql $LocalDir
# scp -i $SshKey ${SshUser}@${ServerHost}:/tmp/db-backup/$ServerTs/backup-meta.txt $LocalDir

Get-ChildItem $LocalDir | Select-Object Name,Length,LastWriteTime
```
下载成功后，按第 9 节执行服务器清理，避免临时备份长期堆积。

## 7. 发版前最小备份清单（建议）
每次发布至少保证以下文件存在于项目中：
- `db-backups/<TS>/verifynum_full.sql.gz`（走 5.1 时）
- `db-backups/<TS>/backup-meta.txt`（走 5.3 时）

若本次涉及菜单/权限或配置，再加：
- `db-backups/<TS>/key_tables.sql`

## 8. 注意事项
- 不要把数据库密码写死在命令里，使用交互输入或环境变量。
- 备份文件通常包含敏感数据，默认建议仅本地留存，不直接提交到远程仓库。
- 备份完成后再执行迁移或发版，避免“改完才发现没备份”。

## 9. 下载完成后的服务器清理（新增）
原则：
- `/tmp/db-backup/<TS>/` 属于临时目录，下载成功后应立即删除。
- `/www/backup/deploy/<TS>/` 建议按回滚窗口保留（如 7~14 天），不要无限堆积。

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
