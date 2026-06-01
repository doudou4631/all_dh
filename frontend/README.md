# 号码标记管理系统前端（frontend）
## 1. 项目说明
前端基于 Vue 3 + Vite，承载管理端页面、业务页面与移动端静态资源（`public/mobile-h5`）。

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
├─ vite.config.js               # Vite 配置
└─ package.json
```

## 7. 与后端联调说明
当前开发代理配置在 `vite.config.js`：
- `/dev-api` → `http://localhost:8080`
- `/v3` → `http://localhost:8080`

建议后端本地服务先启动，再启动前端开发服务。

## 8. 常用配置文件
- `vite.config.js`：开发端口、代理、别名
- `package.json`：脚本与依赖
- `.env*`：环境变量（如项目中已配置）

## 9. 相关文档
- 部署流程：`../docs/DEPLOY_UPDATE_GUIDE.md`
- 数据库备份：`../docs/DB_BACKUP_TO_PROJECT_GUIDE.md`
