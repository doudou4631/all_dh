# 标记下单刷新 404 修复发布说明（2026-06-05）
## 1. 目标
修复“标记代理角色新增/删除模板后，标记下单角色刷新页面进入 404”的线上问题，并保证后续平台自定义变更（新增、删除、改名、改编码）时，用户不会因为旧路由失效而直接落到 404 页面。

## 2. 问题定义与根因结论
### 2.1 现象
- 标记代理角色对模板进行增删改后，标记下单角色停留在旧平台 URL（例如历史平台编码路径）并刷新。
- 刷新后命中前端 404 页面。

### 2.2 根因
- 这不是 Nginx `try_files` 或服务不可用导致的“站点级 404”，而是前端动态路由与平台集合变更之间的“路由失配”导致的“应用级 404”。
- 当平台列表变化后，旧 URL 中的 `platformCode` 不再存在；前端重写后的可用子路由不包含旧路径，最终被前端兜底 404 路由接管。

## 3. 本次修复范围
### 3.1 前端修复（阶段 2，已完成）
- 文件：`frontend/src/store/modules/permission.ts`
  - 增加稳定查询构造逻辑，重写标记下单子路由时构建旧路径兼容映射（隐藏 fallback route）。
  - 对已失效旧平台路径做兜底映射，避免刷新直接进入 404。
  - 在缺失关键权限时跳过不安全重写，减少异常路由注入风险。
- 文件：`frontend/src/views/server/mark/user/index.vue`
  - 增加路由自愈：若当前 URL 的 `platformCode` 已不在可用平台列表，自动 `replace` 到第一个可用平台。
  - 增加用户提示：当前平台已变更，系统已自动切换。

### 3.2 后端加固（阶段 3，已完成）
- 文件：`backend/geek-server/src/main/java/com/geek/server/service/impl/MarkOrderServiceImpl.java`
  - 可用平台码改为并集策略：菜单平台码 ∪ 模板平台码 ∪ 用户显式平台价码。
  - 当模板为空或解析失败时，不直接返回空集合，回退到菜单平台集合，避免前端列表突然归零。
  - 增加关键日志字段（如 user/template/available candidates）便于后续追踪。

## 4. 计划执行状态
- 计划 ID：`a67ec7e2-3df9-4409-b261-6b06062ffcd0`
- 阶段 2（前端根因修复）：已完成
- 阶段 3（后端加固）：已完成
- 发布：已执行（按当前会话确认）
- 当前唯一剩余项：发布后验收（服务状态、接口连通、静态资源、日志）

## 5. 验收标准
需要同时满足以下条件：
- `/` 返回 `200`
- `/prod-api/` 返回 `200`
- 首页实际引用的 `/assets/index-*.js` 返回 `200` 且 `Content-Type` 为 `application/javascript`
- `geek-admin` 状态为 `active (running)`
- `nginx` 状态为 `active (running)`
- `journalctl -u geek-admin` 与 Nginx error log 中无本次发布引入的关键报错
- 业务验收：模板增删改后，下单角色刷新旧平台 URL 不再 404；无效平台自动跳转到可用平台

## 6. 建议验收命令（服务器执行）
```bash
sudo systemctl is-active geek-admin
sudo systemctl is-active nginx
ss -tlnp | grep -E ':80|:443|:8080'

curl -I -m 8 -H "Host: biaoji.aleo1314.vip" http://127.0.0.1/
ASSET_JS=$(grep -oE '/assets/index-[^"]+\.js' /www/wwwroot/frontend/index.html | sed -n '1p' | sed 's#^/assets/##')
printf "ASSET_JS=%s\n" "$ASSET_JS"
test -n "$ASSET_JS" && curl -I -m 8 -H "Host: biaoji.aleo1314.vip" "http://127.0.0.1/assets/$ASSET_JS"
curl -I -m 8 -H "Host: biaoji.aleo1314.vip" http://127.0.0.1/prod-api/

curl -I -m 10 https://biaoji.aleo1314.vip/
curl -I -m 10 https://biaoji.aleo1314.vip/prod-api/

sudo journalctl -u geek-admin -n 120 --no-pager
sudo tail -n 120 /var/log/nginx/error.log
```

## 7. 业务回归建议（手工）
### 7.1 必测路径
1. 标记代理账号：新增一个平台模板（或修改模板平台集合）。
2. 标记下单账号：保持在旧平台 URL，直接刷新。
3. 标记代理账号：删除/改编码一个平台。
4. 标记下单账号：再次刷新原 URL。

### 7.2 预期
- 页面不出现 404。
- 若 URL 对应平台已失效，自动切到首个可用平台并有提示。
- Tabs、`getRouters`、`server/markUser/price/list` 平台集合一致。
- 下单流程、订单列表、订单详情、钱包扣退无回归异常。

## 8. 风险与观察点
- 如果模板 JSON 被写入非法格式，后端应记录解析告警并回退菜单集合；需重点关注相关日志是否持续告警。
- 平台编码频繁调整时，应重点观察前端是否总能稳定重定向到可用平台，而非保留无效 query。

## 9. 回滚策略（摘要）
若验收失败或出现严重回归：
1. 回滚前端包到最近备份版本。
2. 回滚后端 `geek-admin.jar` 到最近备份版本并重启。
3. 重跑第 5 节验收命令确认恢复。

## 10. 当前结论
代码修复与发布环节已完成，当前仅差“发布后验收结果”落地确认。  
完成第 6 节与第 7 节后，可正式关闭本次故障处理。
