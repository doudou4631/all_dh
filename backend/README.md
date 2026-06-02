# 号码标记管理系统后端（backend）
## 1. 项目说明
后端是基于 Spring Boot 3 的多模块工程，提供管理端/业务端 API、权限体系、数据访问与系统能力。

## 2. 技术栈
- Java 21
- Spring Boot 3.5.x
- Maven 多模块
- MyBatis-Flex + Druid + MySQL
- Knife4j / SpringDoc（接口文档能力）

## 3. 目录结构（核心）
```text
backend/
├─ geek-admin/                  # 启动入口模块（打包产物 geek-admin.jar）
├─ geek-server/                 # 业务模块
├─ geek-system/                 # 系统模块
├─ geek-framework/              # 框架模块
├─ geek-common/                 # 公共模块
├─ geek-modules/                # 扩展模块
│  ├─ geek-module-generator/
│  ├─ geek-module-online/
│  └─ geek-module-quartz/
├─ geek-plugins/                # 插件目录（按需启用）
├─ geek-scenes/                 # 场景扩展目录（按需启用）
└─ sql/                         # 数据库脚本
```

## 4. 环境要求
- JDK 21
- Maven 3.9+
- MySQL 8+

## 5. 本地开发与运行
在仓库根目录执行：

### 5.1 打包
```bash
mvn -f backend/pom.xml clean package -DskipTests
```

### 5.2 运行 Jar
```bash
java -jar backend/geek-admin/target/geek-admin.jar
```

### 5.3 开发模式启动（可选）
```bash
mvn -f backend/geek-admin/pom.xml spring-boot:run
```

默认服务端口与配置可见 `geek-admin/src/main/resources/application.yml`（当前为 `8080`）。
默认激活 profile：`data,auth,plugins,model,apidoc`，默认 `liquibase` 为关闭状态。

## 6. 配置说明
- 主配置：`geek-admin/src/main/resources/application.yml`
- 数据源配置：`geek-admin/src/main/resources/application-data.yml`

重要：启动前先确认数据源指向目标数据库，避免误连线上库。

## 7. SQL 与迁移说明
当前推荐迁移脚本：
- `sql/m4_m7_all_in_one.sql`（合并迁移脚本，优先执行）

整库快照：
- `sql/verifynum.sql`

执行示例：
```bash
mysql -h{{DB_HOST}} -P{{DB_PORT}} -u{{DB_USER}} -p{{DB_NAME}} < backend/sql/m4_m7_all_in_one.sql
```

## 8. 相关文档
- 发布流程：`../docs/DEPLOY_UPDATE_GUIDE.md`
- 数据库备份：`../docs/DB_BACKUP_TO_PROJECT_GUIDE.md`
- 角色矩阵：`../docs/MARK_ROLE_MATRIX.md`
