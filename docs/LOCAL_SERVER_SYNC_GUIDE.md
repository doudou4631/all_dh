# 本地与服务器改动同步指南
## 1. 目标
避免“本地一套、服务器一套”导致代码漂移，统一做到：
- 本地代码可追溯（Git 有记录）
- 服务器改动可回收（能同步回本地）
- 发布可回滚（配置、SQL、产物都有备份）

## 2. 核心原则
- 本地仓库是唯一真源（Source of Truth）。
- 服务器只作为运行环境，不作为长期开发环境。
- 任何服务器临时修改，必须在当天回灌到本地并提交 Git。

## 3. 场景 A：本地改了代码，如何同步到服务器
按 `docs/DEPLOY_UPDATE_GUIDE.md` 执行标准发布：
1. 本地改代码并测试。
2. 本地构建前后端产物。
3. 上传到服务器 `/tmp/deploy`。
4. 服务器先备份，再替换发布。
5. 发布后验收（主页、API、mobile-h5、服务状态）。

结论：这是常规路径，方向为 **本地 -> 服务器**。

## 4. 场景 B：服务器上改了内容，如何同步回本地
适用于线上应急修复（配置、SQL、Nginx、脚本等）。

### 4.1 先从服务器拉快照到本地
```powershell
$TS = Get-Date -Format "yyyyMMdd_HHmmss"
$Key = "$HOME\.ssh\id_rsa_43_142_125_17"
$User = "ubuntu"
$Host = "43.142.125.17"
$LocalRoot = "C:\Users\doupc01\Desktop\all\server-sync\$TS"
New-Item -ItemType Directory -Path $LocalRoot -Force | Out-Null

# 拉配置（示例）
scp -i $Key ${User}@${Host}:/www/wwwroot/backend/application-data.yml "$LocalRoot\application-data.yml"
scp -i $Key ${User}@${Host}:/etc/nginx/sites-available/biaoji.aleo1314.vip.conf "$LocalRoot\biaoji.aleo1314.vip.conf"

# 若有数据库变更，导出并拉回（示例）
ssh -i $Key ${User}@${Host} "TS=$TS; sudo mkdir -p /tmp/db-sync/\$TS; sudo mysqldump --single-transaction --set-gtid-purged=OFF verifynum sys_menu sys_role_menu > /tmp/db-sync/\$TS/menu_role.sql"
scp -i $Key ${User}@${Host}:/tmp/db-sync/$TS/menu_role.sql "$LocalRoot\menu_role.sql"
```

### 4.2 将快照改动“映射回仓库文件”
- 配置类：更新仓库对应模板/配置文件（例如部署文档、配置样例）。
- SQL 类：在 `backend/sql/` 新增迁移脚本，不要只保留导出 dump。
- 运维类：更新 `docs/` 发布手册或新增 SOP。

### 4.3 本地验证并提交 Git
```bash
git --no-pager status --short
git add .
git commit -m "sync: recover server hotfix changes to local source"
```

结论：应急路径结束后，方向必须闭环为 **服务器 -> 本地 -> Git**。

## 5. 强限制（非常重要）
- 服务器上的 `jar`、`dist` 属于构建产物，不能可靠反推源码改动。
- 所以业务代码变更必须回到本地源码修改，再重新构建发布。
- 线上临时修复只建议改：配置、脚本、SQL（且要立刻回灌）。

## 6. 推荐日常工作流
1. 日常开发只在本地完成。
2. 任何发布前先做数据库备份（见 `docs/DB_BACKUP_TO_PROJECT_GUIDE.md`）。
3. 发生线上热修时，修完 30 分钟内执行“4.1 拉快照 + 4.2 映射回仓库”。
4. 当天完成 Git 提交，避免次日遗忘。

## 7. 最小检查清单
- [ ] 本地分支代码是最新且已提交
- [ ] 服务器改动已拉回 `server-sync/<TS>/`
- [ ] 仓库中已有对应源码/SQL/文档变更（不只是快照）
- [ ] 本地已验证并提交 Git
