# 标记业务角色矩阵（阶段 C+D）
## 1. 适用范围
本文档描述当前已落地的“标记业务权限收敛”规则（阶段 C + D），用于开发、测试、发布验收统一口径。

## 2. 当前角色键与含义
- `admin`：平台超级管理员（全量权限，不做标记链路限制）。
- `agent` / `mark_agent`：标记代理（兼容并存角色键）。
- `user` / `mark_user`：标记下游用户（兼容并存角色键）。
- `common`：通用角色，不属于标记链路可分配目标。

说明：`mark_admin` 为后续迁移目标角色，本阶段未启用专门分配规则。

## 3. 权限边界原则
- 后端接口是最终权限边界；前端收敛仅用于降低误操作。
- 代理链路角色分配必须经过后端白名单校验，不能依赖前端选项限制。

## 4. 数据范围（代理视角）
- 非超管且命中 `agent/mark_agent` 时，用户查询仅允许看到：
  - 本人账号；
  - 由本人创建链路中的账号。

## 5. 角色分配矩阵（当前生效）
### 5.1 `admin`
- 可按现有平台权限模型管理角色与用户，不受本矩阵限制。

### 5.2 `agent` / `mark_agent`
- 编辑本人账号：仅允许分配 `agent`、`mark_agent`。
- 编辑下游账号：仅允许分配 `user`、`mark_user`。
- 禁止分配：`admin`、`common` 及其他非标记下游角色。

### 5.3 `user` / `mark_user` / `common`
- 不应承担标记链路中的角色分配管理职责（除非被额外授予系统管理权限）。

## 6. 关键接口约束
- `GET /system/user/`、`GET /system/user/{userId}`
  - 返回 `roles` 会根据操作者与目标用户自动收敛：
    - 代理编辑本人：仅 `agent/mark_agent`
    - 代理编辑下游：仅 `user/mark_user`
- `POST /system/user`、`PUT /system/user`、`PUT /system/user/authRole`
  - 统一执行可分配角色白名单校验；
  - 代理越权分配会被后端直接拒绝。
- `GET /system/user/authRole/{userId}`
  - 角色回显同样执行可见集合收敛，不向代理暴露越权角色。

## 7. 前端代理账户页行为（配合后端）
- 代理身份兼容：`agent` 与 `mark_agent` 视为同类操作者。
- 代理账户页查询兼容：`user`、`mark_user`、`agent`、`mark_agent`。
- 新增下游默认角色仅从 `user/mark_user` 选择，不再回退 `common`。
- 角色下拉按目标类型收敛：
  - 下游账号：`user/mark_user`
  - 代理本人：`agent/mark_agent`
- 代理账户页新增/修改时要求有效 `relMarkTemplate`。

## 8. 验收检查点
- 代理通过新增/编辑/授权角色接口无法分配 `common`、`admin`。
- 代理调用角色回显接口时，看不到越权角色。
- 代理用户列表仅包含本人与本人创建链路账号。
- 前端代理账户页不再出现 `common` 默认回退。

## 9. 参考实现文件
- `backend/geek-admin/src/main/java/com/geek/web/controller/system/SysUserController.java`
- `backend/geek-system/src/main/java/com/geek/system/service/impl/SysUserServiceImpl.java`
- `frontend/src/views/system/user/index.vue`
