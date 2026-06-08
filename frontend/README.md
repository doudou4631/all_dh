# 号码标记管理系统前端（frontend）
## 1. 项目说明
前端基于 Vue 3 + Vite，承载管理端页面、业务页面，以及手机端源码工程（`mobile-h5-src`）与发布静态目录（`public/mobile-h5`）。

## 2. 技术栈
- Vue 3
- Vite 7
- Vue Router
- Pinia
- Element Plus
- Axios

## 3. 环境要求
- Node.js：`^20.19.0 || >=22.12.0`
- npm（建议与 Node LTS 配套版本）

## 4. 安装与启动
在仓库根目录执行：

### 4.1 安装依赖
```bash
npm --prefix frontend ci
```

### 4.2 启动开发环境
```bash
npm --prefix frontend run dev
```

默认开发端口见 `vite.config.js`（当前配置为 `80`）。

### 4.3 启动手机端源码开发环境
```bash
npm --prefix frontend run dev:mobile-h5-src
```
默认端口见 `vite.mobile-h5.config.js`（当前配置为 `5175`）。

## 5. 构建与预览
### 5.1 生产构建
```bash
npm --prefix frontend run build:prod
```

### 5.2 预发布构建
```bash
npm --prefix frontend run build:stage
```

### 5.3 本地预览构建结果
```bash
npm --prefix frontend run preview
```

构建产物默认输出到 `frontend/dist`。

### 5.4 手机端源码构建与切换
```bash
npm --prefix frontend run build:mobile-h5-src
npm --prefix frontend run check:mobile-h5-shims
npm --prefix frontend run cutover:mobile-h5-src
```
说明：
- `build:mobile-h5-src` 产物目录为 `frontend/mobile-h5-src/dist/mobile-h5`。
- `cutover:mobile-h5-src` 会将构建产物同步到 `frontend/public/mobile-h5`。
### 5.5 手机端当前关键行为（2026-06）
- 页面标题：`标记查询`（`mobile-h5-src/index.html`）。
- 查询结果页在“泰迪熊普通标记 + 已登录用户”场景下，直接显示内嵌短信处理区域，不再通过“短信处理”按钮跳转到独立页面。
- `captcha/tdx` 路径继续保留兼容入口能力，但默认流程在结果页完成。
- 平台图标中腾讯使用 `public/assets/icons/tencent.png` 资源。

## 6. 目录结构（核心）
```text
frontend/
├─ src/
│  ├─ annotation/               # 注解与元信息定义
│  ├─ api/                      # 接口封装
│  ├─ assets/                   # 资源文件
│  ├─ components/               # 公共组件
│  ├─ directive/                # 自定义指令
│  ├─ entity/                   # 实体/类型对象
│  ├─ views/                    # 页面
│  ├─ router/                   # 路由
│  ├─ store/                    # 状态管理
│  ├─ types/                    # TS 类型定义
│  ├─ utils/                    # 工具函数
│  ├─ layout/                   # 布局
│  ├─ plugins/                  # 插件注册
│  ├─ hook/                     # 组合式 hook
│  ├─ modules/                  # 功能模块
│  └─ main.ts                   # 入口
├─ public/
│  ├─ mobile-h5/                # 移动端静态资源
│  └─ assets/
├─ mobile-h5-src/               # 手机端 Vue 源码工程（Vite root）
├─ vite.config.js               # Vite 配置
├─ vite.mobile-h5.config.js     # 手机端 Vite 配置
└─ package.json
```

## 7. 与后端联调说明
当前开发代理配置在 `vite.config.js`：
- `/dev-api` → `http://localhost:8080`
- `/v3` → `http://localhost:8080`

建议后端本地服务先启动，再启动前端开发服务。

## 8. 常用配置文件
- `vite.config.js`：开发端口、代理、别名
- `vite.mobile-h5.config.js`：手机端源码构建与预览配置
- `package.json`：脚本与依赖
- `.env*`：环境变量（如项目中已配置）

## 9. 相关文档
- 部署流程：`../docs/DEPLOY_UPDATE_GUIDE.md`
- 数据库备份：`../docs/DB_BACKUP_TO_PROJECT_GUIDE.md`
- 角色矩阵（主文档）：`../backend/doc/权限控制.md`

## 10. 代理账户页角色约束（2026-06 更新）
`src/views/system/user/index.vue` 在代理账户场景新增了角色与模板收敛逻辑，避免前端继续暴露越权选项：

- 代理身份兼容判断：`agent` 与 `mark_agent` 都视为代理操作者。
- 代理账户页用户加载兼容角色键：`user`、`mark_user`、`agent`、`mark_agent`。
- 新增用户默认角色只会从 `user/mark_user` 中选择，不再使用 `common` 作为默认候选。
- 角色下拉会根据当前编辑目标收敛：
  - 下游账号：仅展示 `user/mark_user`
  - 代理本人账号：仅展示 `agent/mark_agent`
- 代理账户页新增/修改时要求标记模板（`relMarkTemplate`）有效。

注意：前端限制仅用于减少误操作，最终权限边界仍以后端接口校验为准。
