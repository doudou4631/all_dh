# 查询业务链路总览（单次 / 批量 / 免费查询）
本文用于后续改造前快速对齐“当前实际链路”。  
范围覆盖：后台管理端、公开免费查询页、mobile-h5。

## 1. 统一网关转发规则
- Nginx 统一入口：`/prod-api/` -> `http://127.0.0.1:8080/`
- 由于 `proxy_pass` 使用了尾斜杠，转发时会去掉 `/prod-api/` 前缀。
- 等价关系：
  - 前端请求：`/prod-api/server/freeQuery/single`
  - 后端实际：`/server/freeQuery/single`

## 2. 按业务口径的查询链路

### A. 普通单次查询（后台登录用户）
- 业务定位：管理端“单次查询”
- 前端页面：`frontend/src/views/server/user/single.vue`
- 前端 API：`frontend/src/api/server/apiServer.js` -> `singleApi()`
- 请求接口：`POST /prod-api/server/apiServer/single`
- 后端入口：`ApiController.single`（`/server/apiServer/single`）
- 流程标签：主流程（后台）

### B. 普通批量查询（后台登录用户）
- 业务定位：管理端“批量任务查询”
- 前端页面：`frontend/src/views/server/user/batch.vue`
- 前端 API：`frontend/src/api/server/asyncBatchApi.js`
- 主要接口：
  - `POST /prod-api/server/apiServer/asyncBatch`
  - `POST /prod-api/server/apiServer/asyncBatchOpt`（无 `X-Free-Token`）
  - `GET /prod-api/server/apiServer/taskStatus/{taskId}`
  - `GET /prod-api/server/apiServer/taskResults/{taskId}`
- 后端入口：`BatchApiController`（异步批量任务链路）
- 流程标签：主流程（后台）

### C. 免费单次查询（公开页 + mobile-h5）
- 业务定位：免费查询（单号）
- 前端入口（公开页）：
  - `/free-query2`
  - `/free-query-marked`
- 前端入口（mobile-h5）：
  - `/mobile-h5/result?phone=...`
- 前端 API：
  - `GET /prod-api/server/freeQuery/quota`
  - `POST /prod-api/server/freeQuery/single`
- 后端入口：`FreeQueryController.quota/single`
- 流程标签：主流程（免费）

### D. 免费批量查询（mobile-h5 登录用户）
- 业务定位：免费账号批量查询
- 前端入口：`/mobile-h5/batch`
- 前端 API：`POST /prod-api/server/apiServer/asyncBatchOpt`（携带 `X-Free-Token`）
- 后端关键分流：
  - `BatchApiController.submitBatchQueryOptimizedForFree`（`headers = "X-Free-Token"`）
  - 内部转到：`FreeQueryController.batch`
- 说明：同一路径 `/server/apiServer/asyncBatchOpt`，是否带 `X-Free-Token` 决定是“免费批量”还是“普通批量”。
- 流程标签：主流程（免费）

### E. 免费查询记录（mobile-h5 登录用户）
- 业务定位：免费用户查询历史
- 前端入口：`/mobile-h5/profile/query-records.html`
- 前端 API：`GET /prod-api/server/freeQuery/records`（需 `X-Free-Token`）
- 后端入口：`FreeQueryController.records`
- 流程标签：主流程（免费）

### F. 短信验证兼容链路（TDX）
- 业务定位：泰迪相关短信验证兼容流程
- 前端入口：`/mobile-h5/captcha/tdx`（含多个 alias）
- 主要调用：外部域名
  - `POST https://www.teddymobile.cn/api/phone/getVerifyCode`
  - `POST https://www.teddymobile.cn/api/phone/queryVerifyCodeResult`
- 说明：该链路不走本站 `/prod-api/` 反代。
- 流程标签：兼容流程（保留）

### G. 标记下单角色：一键批量查询链路（本次补齐）
- 业务定位：`标记下单` 角色在“提交号码”页执行预查询并提交消除。
- 前端页面：`frontend/src/views/server/mark/user/index.vue`
- 前端请求封装：`frontend/src/api/server/markUser.js`
- 请求模型：`MarkOrderCreateRequest`（`platformCode`、`platformName`、`phones`、`requestNo`、`remark`）。

1) 一键批量查询（预查询，不下单）
- 按钮：`一键批量查询`（`submitBatchOrder`）
- 接口：`POST /prod-api/server/markUser/order/precheck`
- 后端入口：`MarkUserController.precheckOrder` -> `MarkOrderServiceImpl.precheckOrder`
- 实际执行：
  - 逐号码调用 `freeQueryService.singleQuery`
  - 传入当前平台 `platformCode/platformName`
  - 在 `FreeQueryServiceImpl.singleQuery` 内按平台做单平台选择（非全平台轮询）
- 返回结果：`markedPhones`、`unmarkedPhones`、`failedPhones`、`items`（用于右侧查询结果表格）。

2) 提交消除（当前实现为“创建待处理订单”）
- 按钮：`提交消除`（`submitSelectedMarkedPhones`）
- 前端筛选：仅提交“勾选且已标记”的号码。
- 接口：`POST /prod-api/server/markUser/order/clear`
- 后端入口：`MarkUserController.createClearOrder` -> `markOrderService.createOrder(request)`
- 当前行为（关键）：
  - 创建 `mark_order` + `mark_order_item`
  - 按平台次数扣减配额并写钱包流水
  - `mark_order_item.process_status` 初始为 `0`（待处理）
  - 不在该步骤直接调用外部“消除执行”接口

3) 后续处理（代理链路）
- 代理逐条回填：`POST /server/markAgent/item/{itemId}/feedback`
- 代理整单处理：`POST /server/markAgent/order/{orderId}/complete`
- 失败退款：在代理回填失败路径触发退款逻辑（`feedbackOrderItem` 内部处理）。

## 3. 主流程与兼容/历史入口

### 3.1 当前主流程（建议改造优先关注）
- 后台：普通单次查询（A）
- 后台：普通批量查询（B）
- 免费：单次查询（C）
- 免费：批量查询（D）
- 免费：查询记录（E）

### 3.2 兼容/历史保留（改造需评估是否下线）
- `captcha/tdx` 兼容短信验证流程（F）
- `/free_query2`、`/free_query_marked`（下划线别名）
- `/mobile-h1`、`/mobile-h1/` 已在 Nginx 直接 `410`

## 4. 后续改造时的风险点清单
- `/server/apiServer/asyncBatchOpt` 存在“同路径双语义”（是否带 `X-Free-Token`）：
  - 改动接口网关、鉴权中间件、前端请求封装时，容易误伤免费批量链路。
- `queryType` 与 `sourceType` 是两套维度：
  - `queryType`（如 1/2）偏“记录类型维度”
  - `sourceType`（`FREE_SINGLE/FREE_BATCH`）偏“免费查询来源维度”
  - 报表/筛选改造时要避免混用。
- free 查询依赖请求头 `X-Free-Token`：
  - 前端统一 request 拦截器或后端鉴权变更时，需回归 `single/batch/records` 三条免费接口。
- TDX 兼容链路依赖外部域名：
  - 若要收敛链路，需要先明确替代方案，再下线兼容入口。

