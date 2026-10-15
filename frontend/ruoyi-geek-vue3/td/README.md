# TDX 二次申诉（方案 A）

从 RightControl 抽出的 **独立可迁移子模块**，仅包含「tdx二次申诉」业务：

- 号码预检 → 发送验证码 → 短信登录 → 自动申诉 → 扣费 → 自动对单/退款

## 目录结构

```
td/
├── backend/          # Fastify API（默认 :4320）
├── frontend/         # Vue3 页面（默认 :4321）
├── db/tdx_schema.sql # 最小表结构
├── proxy/qmf-proxy.js
└── scripts/          # 从主项目同步源码
```

## 快速启动

### 1. 数据库

```bash
mysql -u root -p YourDatabase < db/tdx_schema.sql
```

确保 `t_user` 中有可登录账号，且 `dealsmsgettdx`（单价，分）、`bal`（余额）已配置。

### 2. 后端

```bash
cd backend
copy .env.example .env
# 编辑 .env：数据库、QMF、TDX 通道
npm install
npm run dev
```

健康检查：`GET http://127.0.0.1:4320/health`

### 3. 前端

```bash
cd frontend
copy .env.example .env
npm install
npm run dev
```

浏览器打开：`http://127.0.0.1:4321` → 登录 → `/appeal`

## 核心 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/tdx/sms-receive/precheck` | 号码预检 |
| POST | `/api/tdx/sms-receive/send-code` | 发送验证码 |
| POST | `/api/tdx/sms-receive/login` | 提交申诉（主入口） |
| GET  | `/api/tdx/tasks/:id` | 任务详情（可选） |

登录会话默认有效 12 小时，可通过 `TD_SESSION_TTL_SECONDS` 调整。退出登录后，当前进程会立即吊销该会话。

## 迁入其他项目

**方式 1：独立微服务** — 保持 `td/backend` 单独运行，目标项目 HTTP 调用上述 API。

**方式 2：源码合并** — 将 `backend/src` 拷入目标 Node 项目，注册 `/api/tdx` 路由；前端拷 `TdxSmsReceivePage.vue` + `api/client.js`。

需对接的字段：

- `request.currentUser.liveUserId` / `username`
- `t_user.bal`、`dealsmsgettdx`
- `t_orderdeal`（`companycode=duanxingettaidixiong`）

## 环境变量（最少）

见 `backend/.env.example`。真实短信必须使用 `TDX_MODE=live`；模拟模式会明确返回未发送。当前项目本地只保留 QMF 线路 1，线路 2由外部 API负责。

## 与完整 TDX（方案 B）的区别

本包 **不包含**：泰迪熊语音标记、腾讯清理、批量任务台、Dashboard 统计等。仅需时可从主项目 `src/backend-js` 追加拷贝。

## 从主项目重新同步

```powershell
.\scripts\sync-from-parent.ps1
```
