# 号码标记管理系统（all）
## 项目简介
本仓库包含号码标记管理系统的前后端代码、部署文档与数据库脚本。
当前采用前后端分离架构：
- 后端：Spring Boot 3 多模块工程（Java 21）
- 前端：Vue 3 + Vite

## 目录结构
```text path=null start=null
all/
├─ .git/                        # Git 元数据
├─ backend/                     # 后端工程（Maven 多模块）
│  ├─ geek-admin/               # 启动模块（生成 geek-admin.jar）
│  ├─ geek-server/              # 业务模块
│  ├─ geek-system/              # 系统模块
│  ├─ geek-framework/           # 框架模块
│  ├─ geek-common/              # 公共模块
│  ├─ geek-modules/             # 扩展模块聚合
│  │  ├─ geek-module-generator/
│  │  ├─ geek-module-online/
│  │  └─ geek-module-quartz/
│  ├─ geek-plugins/             # 预留插件目录（当前仅 .gitkeep）
│  ├─ geek-scenes/              # 预留场景目录（当前仅 .gitkeep）
│  ├─ sql/                      # 数据库脚本
│  ├─ bin/                      # 后端批处理脚本
│  ├─ doc/                      # 后端文档与流程图
│  └─ .idea/                    # IntelliJ 本地配置
├─ frontend/                    # 前端工程（Vue3 + Vite）
│  ├─ src/
│  │  ├─ api/                   # 接口请求
│  │  ├─ components/            # 通用组件
│  │  ├─ views/                 # 页面视图
│  │  ├─ router/                # 路由
│  │  ├─ store/                 # 状态管理
│  │  ├─ utils/                 # 工具函数
│  │  ├─ assets/                # 静态资源
│  │  ├─ layout/                # 布局
│  │  ├─ plugins/               # 前端插件
│  │  ├─ types/                 # 类型定义
│  │  ├─ directive/             # 自定义指令
│  │  ├─ modules/               # 业务模块
│  │  ├─ hook/                  # 组合式 hooks
│  │  ├─ entity/                # 实体相关定义
│  │  └─ annotation/            # 注解与扩展
│  ├─ public/
│  │  ├─ mobile-h5/             # 手机端静态页面
│  │  ├─ assets/
│  │  ├─ free-query-icons/
│  │  ├─ draco/
│  │  ├─ glb/
│  │  └─ wechat/
│  ├─ vite/
│  │  └─ plugins/               # Vite 插件配置拆分
│  ├─ scripts/                  # 前端脚本目录（当前可为空）
│  ├─ lib/                      # 前端库文件（如 vform）
│  ├─ html/                     # 独立 HTML 页面
│  ├─ doc/                      # 前端文档与示意图
│  ├─ bin/                      # 前端批处理脚本
│  ├─ dist/                     # 前端构建产物
│  ├─ node_modules/             # 前端依赖目录
│  └─ .vscode/                  # VS Code 本地配置
├─ docs/                        # 部署与维护文档
├─ db-backups/                  # 数据库备份归档（按时间戳分目录）
├─ server-backups/              # 整机备份归档（按时间戳分目录）
└─ README.md                    # 当前说明文档
```

## 环境要求
- JDK：21
- Maven：3.9+
- Node.js：`^20.19.0 || >=22.12.0`
- npm：与 Node 配套版本

## 本地开发
### 1) 前端启动
```bash path=null start=null
npm --prefix frontend ci
npm --prefix frontend run dev
```

### 2) 后端打包与启动
```bash path=null start=null
mvn -f backend/pom.xml clean package -DskipTests
java -jar backend/geek-admin/target/geek-admin.jar
```

可选（开发期直接运行）：
```bash path=null start=null
mvn -f backend/geek-admin/pom.xml spring-boot:run
```

## 数据库脚本说明
`backend/sql` 当前保留两个 SQL 文件：
- `m4_m7_all_in_one.sql`：迁移合并脚本（推荐执行这个）
- `verifynum.sql`：整库快照脚本（体积较大）

执行迁移示例：
```bash path=null start=null
mysql -h{{DB_HOST}} -P{{DB_PORT}} -u{{DB_USER}} -p{{DB_NAME}} < backend/sql/m4_m7_all_in_one.sql
```

## 发布与备份文档
- 发布流程：`docs/DEPLOY_UPDATE_GUIDE.md`
- 数据库备份：`docs/DB_BACKUP_TO_PROJECT_GUIDE.md`

## 重要注意事项
- 变更 SQL 或发布前，先做数据库备份。
- 启动后端前，先检查 `backend/geek-admin/src/main/resources/application-data.yml` 的数据源配置，确认不是误连非目标库。
