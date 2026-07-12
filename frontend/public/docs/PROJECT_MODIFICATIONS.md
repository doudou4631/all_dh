# 项目修改汇总（本地）

> 生成时间：2026-07-08  
> 项目路径：`c:\Users\Administrator\Desktop\1500`  
> 说明：本文档汇总当前会话及近期开发中对标记业务（用户端 / 代理端 / 管理端）的主要改动、平台选项、SQL 脚本与部署注意事项。

---

## 1. 核心设计原则

### 1.1 平台识别规则

| 字段 | 作用 | 是否可随意修改 |
|------|------|----------------|
| `platformCode` | 后端主标识：路由、提交、预检、扣费、配额 | 不建议改（涉及多表同步） |
| `platformName` | 前端展示名称（侧边栏、页面标题） | 可改 |
| `unitPrice` | 每号码扣积分/次数 | 可改 |

**用户端 / 代理端导航**由用户绑定的 `mark_platform_template.template_info` 驱动，不再固定显示全部静态菜单。

### 1.2 页面类型

| 类型 | 页面 | 流程 |
|------|------|------|
| 专用提交页 | `tencent.vue` / `xiaomi.vue` / `baidu.vue` / `qihu360.vue` | 号码提取 → 批量提交 → 结果（订单号 + 数量） |
| 通用提交页 | `index.vue` | 预检 → 勾选 → 提交消除 |
| 代理处理页 | `agent/process/platform.vue` + `ProcessWorkbench.vue` | 明细列表、批量处理、状态回填 |

---

## 2. 平台编码与页面对照

### 2.1 专用页平台（Dedicated）

| platformCode | 平台备注 | 前端页面 | 路由示例 |
|--------------|----------|----------|----------|
| `tencent_mark` / `tengxun` / `tencent` / `tx` / `txwz` | 腾讯 / 腾讯速解 | `tencent.vue` | `/mark/tencentMark` |
| `xiaomi` | 小米手机 | `xiaomi.vue` | `/mark/xiaomiMark` |
| `baidu` | 百度 | `baidu.vue` | `/mark/baiduMark` |
| `sanliuling` / `360` | 360 | `qihu360.vue` | `/mark/sanliulingMark` |
| `qihu_first` | 360首次 / 360覆盖 | `qihu360.vue` | `/mark/qihuFirstMark` |
| `qihu_second` | 360二次 | `qihu360.vue` | `/mark/qihuSecondMark` |

识别工具：

- `frontend/src/utils/markTencentPlatform.js`
- `frontend/src/utils/markXiaomiPlatform.js`
- `frontend/src/utils/markBaiduPlatform.js`
- `frontend/src/utils/markQihu360Platform.js`

### 2.2 通用页平台（Precheck + Clear）

| platformCode | 平台备注 | 前端页面 | 特殊逻辑 |
|--------------|----------|----------|----------|
| `td_gaopin` | 泰迪熊高频 | `index.vue` | 泰迪高频预检规则 + 自动检测 |
| `td_second` | 泰迪熊二次 | `index.vue` | 通用预检 |
| `taidixiong` | 泰迪熊 | `index.vue` | 映射到泰迪熊 API，但无 td_gaopin 专用 UI |
| `mobile_gaopin` / `yidonggaopin` | 移动高频 | `index.vue` | 移动高频预检规则 |
| `dianhuabang` | 电话邦 | `index.vue` | 通用 |
| `sghmt` | 搜狗号码通 | `index.vue` | 通用 |
| `ltgj` | 联通管家 | `index.vue` | 通用 |

> **注意**：`td_gaopin` 与 `taidixiong` 不是同一编码；模版中应优先使用 `td_gaopin`。

### 2.3 错误编码示例（需修正）

| 错误编码 | 应使用 |
|----------|--------|
| `Sougou` / `sougou` | `sghmt` |
| `LiantongGuanjia` / `liantongguanjia` | `ltgj` |

修复脚本：`backend/sql/m46_fix_english_all_platform_codes.sql`

---

## 3. 模版（全平台 / 英文版全平台）

### 3.1 默认全平台模版（M13，9 平台）

`taidixiong`, `tengxun`, `sanliuling`, `baidu`, `sghmt`, `yidonggaopin`, `xiaomi`, `ltgj`, `dianhuabang`

脚本：`backend/sql/m13_mark_default_template_platform_alignment.sql`

### 3.2 全平台模版（id=3，6 平台）

`td_gaopin`, `qihu_first`, `mobile_gaopin`, `tencent_mark`, `xiaomi`, `baidu`

### 3.3 英文版全平台（id=4，10 平台）

含 `Sougou`、`LiantongGuanjia` 等非标准编码，需执行 M46 修正。

### 3.4 管理端模版编辑（template.vue）新增项

文件：`frontend/src/views/server/mark/admin/template.vue`

| 功能 | 说明 |
|------|------|
| **绑定编码** | 只读展示 `platformCode`，保存后不在此修改 |
| **平台备注** | 只读，显示标准平台名称，辅助识别编码 |
| **平台展示名称** | 可编辑，仅影响前端显示 |
| **积分/号码** | 每号码扣费 |
| **排序** | 模版内平台顺序 |
| **新增平台** | 自定义编码（需使用标准 code） |

---

## 4. 前端主要改动文件

### 4.1 用户端

| 文件 | 改动摘要 |
|------|----------|
| `frontend/src/store/modules/permission.ts` | 模版驱动路由注入；专用页/通用页分流；404 与 legacy 重定向修复 |
| `frontend/src/views/server/mark/user/index.vue` | 通用预检提交；泰迪高频 UI；专用平台自动跳转 |
| `frontend/src/views/server/mark/user/tencent.vue` | 腾讯专用页；提交结果仅订单号+数量；失败不弹 toast |
| `frontend/src/views/server/mark/user/xiaomi.vue` | 小米专用页（66% 宽，max 660px） |
| `frontend/src/views/server/mark/user/baidu.vue` | 百度专用页（小米风格） |
| `frontend/src/views/server/mark/user/qihu360.vue` | 360 专用页（小米风格） |
| `frontend/src/views/server/mark/user/orderDetail.vue` | 全平台订单详情 |
| `frontend/src/utils/markProcessStatus.js` | 处理状态文案/按钮 |
| `frontend/src/api/server/markUser.js` | 用户端 API |

### 4.2 代理端

| 文件 | 改动摘要 |
|------|----------|
| `frontend/src/views/server/mark/agent/components/ProcessWorkbench.vue` | 代理处理工作台；小米/泰迪高频批量操作 |
| `frontend/src/views/server/mark/agent/process/platform.vue` | 按平台处理页 |
| `frontend/src/views/server/mark/agent/audit/index.vue` | 订单审核 |
| `frontend/src/api/server/markAgent.js` | 代理 API（含 item/list、platform/list） |

### 4.3 管理端

| 文件 | 改动摘要 |
|------|----------|
| `frontend/src/views/server/mark/admin/template.vue` | 绑定编码 + 平台备注 + 展示名称 |

### 4.4 历史页面生成脚本说明

原 `tools/` 目录中的页面生成、修复、检查脚本均为一次性辅助脚本，已在 2026-07-12 清理删除；后续以前端源码和 Git 历史为准。

---

## 5. 后端主要改动

### 5.1 核心服务

| 文件 | 改动摘要 |
|------|----------|
| `MarkOrderServiceImpl.java` | 腾讯提交按 reCode 判定；小米 status=3 自动检测；td_gaopin 预检/自动处理；平台编码映射 |
| `MarkAgentController.java` | 代理 item/list、platform/list、小米/泰迪批量接口 |
| `MarkUserController.java` | 用户提交/预检/腾讯 API |
| `MarkPlatformTemplateServiceImpl.java` | 模版平台选项、菜单平台映射 |

### 5.2 数据库连接稳定性（2026-07-08）

文件：`backend/geek-admin/src/main/resources/application-data.yml`

| 配置项 | 修改 |
|--------|------|
| JDBC URL | `useSSL=false`、`allowPublicKeyRetrieval=true`、`autoReconnect=true` |
| Druid | `testOnBorrow=true`、`keepAlive=true` |

> 解决代理端/用户端间歇性 `Communications link failure` 导致接口 500。

---

## 6. SQL 迁移脚本清单（M17–M46）

按编号排列，部署时按依赖顺序执行（已执行的可跳过）：

| 编号 | 文件 | 用途 |
|------|------|------|
| M13 | `m13_mark_default_template_platform_alignment.sql` | 默认 9 平台模版对齐 |
| M17 | `m17_mark_order_audit.sql` / `m17_mark_order_audit_menu_fix.sql` | 订单审核流程 |
| M18 | `m18_mark_user_notice.sql` | 用户消息 |
| M19–M25 | `m19`~`m25` | 代理门户、模版管理、权限 |
| M26–M28 | `m26`~`m28` | 腾讯专用页菜单 |
| M29 | `m29_fix_agent_order_sync.sql` | 代理订单同步 |
| M30–M35 | `m30`~`m35` | 菜单重命名、订单详情、权限 |
| M36–M37 | `m36`~`m37` | 代理处理详情/总览菜单 |
| M38 | `m38_xiaomi_dedicated_page.sql` | 小米专用页菜单 |
| M39 | `m39_xiaomi_hide_user_nav.sql` | （已废弃，被 M40 替代） |
| M40 | `m40_template_driven_user_nav.sql` | 模版驱动用户导航 |
| M41 | `m41_xiaomi_all_platform_template.sql` | 全平台模版补回小米 |
| M42 | `m42_xiaomi_process_status_3.sql` | 小米 status=3 自动检测 |
| M43 | `m43_platform_code_lowercase.sql` | 平台编码小写归一 |
| M44 | `m44_baidu_dedicated_page.sql` | 百度专用页菜单 |
| M45 | `m45_qihu360_dedicated_page.sql` | 360 专用页菜单 |
| M46 | `m46_fix_english_all_platform_codes.sql` | 英文版全平台编码修正 |

---

## 7. 菜单与路由

### 7.1 用户端静态菜单（模板，visible 多为隐藏）

- 通用：`server/mark/user/index`（tdGaopin、mobileGaopin 等）
- 专用：`tencentMark`、`xiaomiMark`、`baiduMark`、`qihuFirstMark` 等

实际显示由 **用户绑定模版** + `permission.ts` 动态注入决定。

### 7.2 代理端

- `agent/process/platform`：按平台处理
- `agent/audit`：订单审核
- `agent/process/downstream`：处理总览
- `agentOrder`（旧入口，visible=1 隐藏）

### 7.3 Legacy 重定向

- `markUser-:legacyCode` → 模版对应平台路由
- 专用平台访问 `index.vue` 时自动跳转到专用页

---

## 8. 腾讯平台特殊逻辑

| 项目 | 规则 |
|------|------|
| 提交成功判定 | 腾讯 API `reCode == 0` |
| 失败提示 | 不弹 floating toast（仅成功 toast） |
| 提交结果展示 | 仅「提交订单号」「提交数量」 |
| 页面宽度 | 66%，max 660px（与小米一致） |
| 旧批量接口 | `assertNotLegacyTencentBatchPlatform` 拦截 |

---

## 9. 小米 / 360 / 百度专用页

| 平台 | 页面 | 提交 API | 结果字段 |
|------|------|----------|----------|
| 小米 | `xiaomi.vue` | `createMarkUserClearOrder` | 订单号 + 数量 |
| 百度 | `baidu.vue` | `createMarkUserClearOrder` | 订单号 + 数量 |
| 360 | `qihu360.vue` | `createMarkUserClearOrder` | 订单号 + 数量 |

---

## 10. 已知问题与运维

### 10.1 部署后必做

1. 执行未应用的 SQL（尤其 M40、M41、M44、M45、M46）
2. 用户/代理 **重新登录** 刷新路由
3. 前端生产：`npm run build:prod`
4. 后端：`java -jar geek-admin.jar` 或 `backend/rebuild_and_restart.ps1`

### 10.2 本地启动

| 服务 | 地址 |
|------|------|
| 前端（Vite） | `http://localhost/`（port 80） |
| 后端 | `http://localhost:8080` |
| MySQL | `127.0.0.1:3306/verifynum` |

脚本：

- `backend/rebuild_and_restart.ps1` — 编译并重启后端

### 10.3 测试账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 标记用户 | `markuser` | `admin123` |
| 标记代理 | `markagent` | `admin123` |
| 管理员 | `admin` | `admin123` |

### 10.4 常见问题

| 现象 | 原因 | 处理 |
|------|------|------|
| 代理端/用户端随机 500 | MySQL 连接池断开 | 已修 application-data.yml，重启后端 |
| 平台菜单 404 | 路由未注入 / SQL 未执行 | 执行 M40/M44/M45，重新登录 |
| 英文版搜狗/联通不可用 | 编码错误 Sougou/LiantongGuanjia | 执行 M46 |
| 前端 API ECONNREFUSED | 后端未启动或访问错端口 | 用 80 端口前端，确认 8080 后端 |
| td_gaopin 被误识别为 360 | 已修复 markQihu360Platform.js | 仅 explicit 360 codes |

---

## 11. 本次会话最新改动（2026-07-08）

1. **标记模版管理**：新增「绑定编码」「平台备注」只读字段
2. **MySQL 连接池**：优化 Druid / JDBC 配置，修复代理端间歇报错
3. **英文版全平台审计**：发现 Sougou/LiantongGuanjia 编码问题，新增 M46
4. **平台编码说明文档**：明确 platformCode 不可随意修改

---

## 12. 相关文档

| 文件 | 内容 |
|------|------|
| `docs/QUERY_CHAIN_MAP.md` | 查询/提交链路 |
| `docs/DEPLOY_UPDATE_GUIDE.md` | 部署更新指南 |
| `docs/CAPTCHA_SWITCH_GUIDE.md` | 验证码开关 |
| `docs/TENCENT_NETWORK_PROXY_GUIDE.md` | 腾讯网络代理 |
| `TX.MD` | 腾讯相关说明 |

---

*本文档由开发过程整理生成，如有新增 SQL 或平台，请同步更新本节。*
