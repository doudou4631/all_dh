# 泰迪高频（td_gaopin）业务逻辑说明

> 生成时间：2026-07-08  
> 项目路径：`c:\Users\Administrator\Desktop\1500`  
> 平台编码：`td_gaopin`  
> 展示名称：泰迪高频 / 泰迪熊高频

---

## 1. 概述

**泰迪高频**是标记业务中的一个平台类型，业务目标是：用户提交当前为「泰迪熊高频」状态的号码，系统扣次建单后，由后台定时自动检测；当号码标记降为「有标记」（普通标记）时，自动判定处理成功。

与其他平台对比：

| 对比项 | 泰迪高频 `td_gaopin` | 腾讯专页 | 小米专页 |
|--------|---------------------|----------|----------|
| 用户页面 | 通用页 `index.vue` | 专页 `tencent.vue` | 专页 `xiaomi.vue` |
| 提交前 | 必须预查 + 仅高频可提交 | 实时查 + 提交 | 实时查 + 提交 |
| 审核 | 用户提交后自动审核通过 | 自动审核 + 自动提交腾讯 | 自动审核 |
| 代理处理 | **后台自动检测**，代理不可手动回填待处理项 | 后台自动提交腾讯 API | 代理批量处理 + 自动检测 |
| 成功标准 | 标记变为「有标记」 | 腾讯 `reCode == 0` | 检测无标记 |

---

## 2. 平台标识与路由

### 2.1 核心字段

| 字段 | 值 | 说明 |
|------|-----|------|
| `platformCode` | `td_gaopin` | 路由、下单、扣次、检测的唯一标识 |
| `platformName` | 泰迪高频 / 泰迪熊高频 | 仅展示，可改 |
| API 映射 | 泰迪熊 | 预查/检测时走泰迪熊相关平台 API |

### 2.2 代码中的名称映射

后端 `MarkOrderServiceImpl` 将以下名称统一映射到 **泰迪熊** API：

- 编码：`taidixiong`、`td_gaopin`、`td_second`
- 名称：泰迪高频、泰迪二次、泰迪熊平台

### 2.3 用户端入口

- 菜单：标记业务 → 泰迪高频（由用户绑定模板动态显示）
- 组件：`frontend/src/views/server/mark/user/index.vue`
- 路由 query：`{"platformCode":"td_gaopin"}`

### 2.4 代理端入口

- 处理工作台 Tab：`供应(TDX泰迪频)`，包含 `mobile_gaopin,td_gaopin,td_second`
- 组件：`frontend/src/views/server/mark/agent/components/ProcessWorkbench.vue`
- 独立菜单（若已执行 M22）：`agentProcessTdGaopin`

---

## 3. 用户端完整流程

```
粘贴/输入号码
    ↓
【一键批量查询】→ POST /server/markUser/order/precheck
    ↓
解析泰迪熊 API 返回，判断是否「泰迪熊高频」
    ↓
自动勾选可提交行（不可提交行无法勾选）
    ↓
【提交消除】→ POST /server/markUser/order/clear
    ↓
后端二次校验 → 扣平台次数 → 建单 → 自动审核
    ↓
明细 processStatus = 0（待处理）
    ↓
后台每 30 秒自动检测
    ↓
标记变为「有标记」→ 自动成功 processStatus = 1
```

---

## 4. 预查询（precheck）逻辑

### 4.1 前端触发

文件：`frontend/src/views/server/mark/user/index.vue`

1. 用户点击 **「一键批量查询」** → `submitBatchOrder()`
2. 调用 `precheckMarkUserOrder(payload)`
3. 接口：`POST /server/markUser/order/precheck`

请求体示例：

```json
{
  "platformCode": "td_gaopin",
  "platformName": "泰迪高频",
  "phones": ["13800138000", "13900139000"],
  "requestNo": "",
  "remark": ""
}
```

### 4.2 后端预查

文件：`MarkOrderServiceImpl.precheckOrder()`

对每个号码：

1. 校验平台编码、用户是否开通、剩余次数
2. 调用 `executeMarkPrecheckSingleQuery()`
3. 通过 `selectMarkPrecheckPlatform()` 选择泰迪熊 API 配置
4. 调用 `OptimizedBatchApiExecutor` 执行单次查询
5. 对 `td_gaopin` 执行 `applyTdGaopinPrecheckRules(item)` 解析结果

### 4.3 可提交判定规则

前后端规则一致，原始返回文案满足 **任一** 即可提交：

| 规则 | 关键词 |
|------|--------|
| 规则 A | 包含 `高频标记至少需要10个工作日或找平台方帮忙处理` |
| 规则 B | 同时包含 `疑似诈骗` 和 `高频` |

前端常量（`index.vue`）：

```javascript
TEDDY_GAOPIN_HF_KEY = '高频标记至少需要10个工作日或找平台方帮忙处理'
TEDDY_GAOPIN_FRAUD_KEY = '疑似诈骗'
TEDDY_GAOPIN_HF_SHORT_KEY = '高频'
```

后端常量（`MarkOrderServiceImpl.java`）：

```java
TD_GAOPIN_HF_KEY = "高频标记至少需要10个工作日或找平台方帮忙处理";
TD_GAOPIN_FRAUD_KEY = "疑似诈骗";
```

### 4.4 预查结果展示规则

| API 原始结果 | 解析后 detail | 是否可提交 |
|-------------|---------------|-----------|
| 含高频完整文案 / 疑似诈骗+高频 | 泰迪熊高频 | ? 可提交 |
| 普通标记 | 有标记 | ? 不可提交 |
| 无 / no / 无标记 | 无 | ? 不可提交 |
| 查询失败 | FAIL + 错误信息 | ? 不可提交 |

预查完成后：

- 可提交行 **自动勾选**
- 提示文案示例：`仅 X 个为「泰迪熊高频」可提交扣次，已自动勾选`

---

## 5. 提交（createOrder）逻辑

### 5.1 前端触发

用户勾选可提交号码后，点击 **「提交消除」** → `submitSelectedMarkedPhones()`

- 接口：`POST /server/markUser/order/clear`（等同 `/order`）
- 函数：`createMarkUserClearOrder(payload)`

### 5.2 后端校验 `assertTdGaopinOrderPhonesValid`

提交前对每个号码 **再次预查**（防前端篡改）：

| 校验项 | 失败提示 |
|--------|----------|
| 本批次号码重复 | `以下号码重复提交：...` |
| 已有待处理同号码订单 | `以下号码已有待处理泰迪高频订单：...` |
| 不是泰迪熊高频结果 | `以下号码不是「泰迪熊高频」结果，无法提交：...` |
| 预查接口失败 | `号码(查询失败)` |

SQL 查重条件（`selectUserPendingTdGaopinPhones`）：

- `platform_code = 'td_gaopin'`
- `audit_status = '1'`
- `process_status = '0'`

### 5.3 提交成功后

`MarkOrderServiceImpl.createOrder()` 执行：

1. **扣平台次数**：`号码数 × unitPrice`
2. **写订单** `mark_order`：`orderStatus=0`，`auditStatus=0`（随后自动改 1）
3. **写明细** `mark_order_item`：每条号码 `processStatus=0`
4. **自动审核**：`autoPassOrderForAgentProcessing()` → `auditStatus=1`，意见「用户提交自动审核」
5. **分配代理**：写入 `assignedAgentId`
6. **写钱包流水**：类型 `DEDUCT`
7. **发用户通知**：`sendOrderSubmitNotice()`

---

## 6. 自动检测与处理完成

### 6.1 定时任务

文件：`MarkTdGaopinAutoProcessConfig.java`

```java
@Scheduled(fixedRate = 30000)  // 每 30 秒
public void autoDetectTdGaopinPendingItems()
```

调用：`markOrderService.processTdGaopinPendingItemsAuto()`

- 每批最多处理 **50** 条（`TD_GAOPIN_AUTO_BATCH_LIMIT`）
- 操作人标识：`td-gaopin-auto`

### 6.2 待检测明细筛选

SQL：`MarkOrderItemMapper.selectPendingTdGaopinProcessItems`

条件：

- `moi.process_status = '0'`
- `mo.audit_status = '1'`
- `mo.platform_code = 'td_gaopin'`

### 6.3 单条自动处理 `processTdGaopinOrderItemAuto`

对每个待处理明细：

1. 再次调用泰迪熊 API 查询当前标记
2. **查询失败** → 保持 `processStatus=0`，仅更新时间
3. **结果不是「有标记」**（仍是泰迪熊高频等）→ 保持待处理
4. **结果 detail = 「有标记」** → 自动成功：
   - `processStatus = '1'`
   - `processResult = '有标记'`
   - `processNote = '自动检测：号码状态为「有标记」，处理完成'`
   - 刷新订单统计
   - 发送用户处理结果通知 `sendTdGaopinProcessNotice()`

### 6.4 成功判定代码

```java
private boolean isTdGaopinAutoCompleteDetail(String detail) {
    return "有标记".equals(StringUtils.trimToNull(detail));
}
```

**业务含义**：用户提交的是「泰迪熊高频」，处理成功的标志是标记 **降为普通「有标记」**。

---

## 7. 代理端行为

### 7.1 工作台展示

文件：`ProcessWorkbench.vue`

- 有待处理泰迪高频时，顶部提示：  
  `泰迪高频后台每30秒自动检测，本页面同步刷新状态`
- 页面每 **30 秒** 自动刷新列表（`setupTdGaopinAutoRefresh`）

### 7.2 代理不可手动操作待处理项

```javascript
// processStatus=0 的 td_gaopin 不允许手动回填
if (isTdGaopinPlatform(row) && processStatus === '0') return false
```

后端批量成功接口也会跳过：

```java
if (isTdGaopinPlatform(order.getPlatformCode()) && "0".equals(currentStatus)) {
    skippedCount++;
    continue;
}
```

### 7.3 手动触发检测 API

- 接口：`POST /server/markAgent/item/autoDetectTdGaopin`
- 控制器：`MarkAgentController.autoDetectTdGaopin()`
- 作用：立即执行一轮自动检测（不等定时任务）

---

## 8. API 清单

### 8.1 用户端

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/server/markUser/order/precheck` | 一键批量查询（预查） |
| POST | `/server/markUser/order/clear` | 提交消除订单 |
| POST | `/server/markUser/order` | 提交订单（同上逻辑） |
| GET | `/server/markUser/order/list` | 订单列表 |
| GET | `/server/markUser/order/{orderId}` | 订单详情 |

### 8.2 代理端

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/server/markAgent/item/list` | 处理明细列表 |
| POST | `/server/markAgent/item/autoDetectTdGaopin` | 手动触发泰迪高频自动检测 |
| POST | `/server/markAgent/item/{itemId}/feedback` | 回填结果（td_gaopin 待处理项会被跳过） |

---

## 9. 状态字段说明

### 9.1 订单 `mark_order`

| 字段 | 提交后 | 说明 |
|------|--------|------|
| `audit_status` | `1` | 自动审核通过 |
| `order_status` | `1` | 进入处理中 |
| `platform_code` | `td_gaopin` | 固定 |

### 9.2 明细 `mark_order_item`

| process_status | 含义 |
|----------------|------|
| `0` | 待处理（等待自动检测） |
| `1` | 处理成功（标记已降为「有标记」） |
| `2` | 处理失败 |
| `3` | 处理中（小米等平台用，td_gaopin 不走此状态） |

---

## 10. 相关源码文件

| 层级 | 文件 | 职责 |
|------|------|------|
| 用户端页面 | `frontend/src/views/server/mark/user/index.vue` | 预查、勾选、提交 UI |
| 用户端 API | `frontend/src/api/server/markUser.js` | precheck / clear 请求 |
| 代理端工作台 | `frontend/src/views/server/mark/agent/components/ProcessWorkbench.vue` | 列表展示、30 秒刷新 |
| 代理端 API | `frontend/src/api/server/markAgent.js` | 明细列表、自动检测 |
| 后端核心 | `backend/.../MarkOrderServiceImpl.java` | 预查、提交校验、自动检测 |
| 定时任务 | `backend/.../MarkTdGaopinAutoProcessConfig.java` | 30 秒调度 |
| 用户控制器 | `backend/.../MarkUserController.java` | 用户 API 入口 |
| 代理控制器 | `backend/.../MarkAgentController.java` | 代理 API 入口 |
| Mapper | `backend/.../mapper/server/MarkOrderItemMapper.xml` | 待处理/重复号码 SQL |
| 管理端模板 | `frontend/src/views/server/mark/admin/template.vue` | 平台备注 `td_gaopin → 泰迪熊高频` |
| 状态工具 | `frontend/src/utils/markProcessStatus.js` | 自动处理平台列表含 `td_gaopin` |

---

## 11. 菜单与 SQL

| 脚本/配置 | 说明 |
|-----------|------|
| `backend/sql/m4_m7_all_in_one.sql` | 用户端「泰迪高频」菜单（path: `tdGaopin`） |
| `backend/sql/m22_agent_process_platform_menus.sql` | 代理端泰迪高频处理菜单 |
| `backend/sql/m40_template_driven_user_nav.sql` | 模板驱动用户导航（平台由模板控制） |

用户能否看到泰迪高频菜单，取决于：

1. 管理员在 **标记模板** 中勾选 `td_gaopin`
2. 用户账号绑定该模板（`sys_user.rel_mark_template`）
3. 用户重新登录刷新路由

---

## 12. 与相近平台区别

| platformCode | 名称 | 可提交条件 | 处理方式 |
|--------------|------|-----------|----------|
| `td_gaopin` | 泰迪高频 | 必须是「泰迪熊高频」 | 后台自动检测，降为「有标记」即成功 |
| `td_second` | 泰迪二次 | 通用：有标记即可 | 代理手动处理 |
| `taidixiong` | 泰迪熊 | 通用：有标记即可 | 代理手动处理 |
| `mobile_gaopin` | 移动高频 | 必须是「有标记」 | 代理手动处理 |

> 注意：`td_gaopin` 与 `taidixiong` 不是同一编码；模板中应使用标准码 `td_gaopin`。

---

## 13. 常见问题排查

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| 预查后 0 个可提交 | 号码不是泰迪熊高频 | 确认 API 返回文案是否含高频关键词 |
| 提交报「不是泰迪熊高频」 | 提交时二次预查结果变化 | 重新预查后再提交 |
| 提交报「已有待处理订单」 | 同号码有 processStatus=0 的单 | 等自动检测完成或联系代理 |
| 提交后一直待处理 | 标记未降为「有标记」 | 正常，等待 30 秒轮询；可手动调 autoDetect API |
| 代理无法手动点成功 | 设计如此 | td_gaopin 待处理项禁止手动回填 |
| 菜单看不到泰迪高频 | 模板未含 td_gaopin | 管理端模板勾选 + 用户重新登录 |

---

## 14. 测试账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 用户端 | `markuser` | `admin123` |
| 代理端 | `markagent` | `admin123` |
| 管理端 | `admin` | `admin123` |

本地访问：`http://localhost/`（前端 80 端口，后端 8080）

---

*本文档根据当前代码库自动生成，如修改 `MarkOrderServiceImpl` 或 `index.vue` 中泰迪高频相关逻辑，请同步更新本文档。*
