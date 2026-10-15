# 泰迪熊线路 2（备用）外部 API 使用说明

更新时间：2026-10-06

本文面向需要接入泰迪熊线路 2 二次申诉的外部平台。当前正式环境将线路 2 作为备用线路，协议中的 `line2` 名称保持不变。外部平台负责收集手机号和短信验证码，本服务负责预检查、线路 2 上游调用、订单归属、扣费和最终处理。

## 1. 接入信息

生产环境 API 根地址：

```text
http://43.143.254.68:4175/backend/api/open-api/v1
```

生产环境建议通过 HTTPS 域名访问。当前服务不使用 IP 白名单，使用服务 Token 鉴权。

请求头：

```http
X-TDX-Integration-Token: <TDX_INTEGRATION_TOKEN>
Content-Type: application/json
```

也支持：

```http
Authorization: Bearer <TDX_INTEGRATION_TOKEN>
```

真实 Token 不写入代码仓库或文档，应只保存在外部平台后端的密钥配置中。当前订单归属账号为 `15617687579`，用户 ID 为 `42`，外部平台不需要传入这个 ID。

## 2. 调用流程

```text
用户输入手机号
    -> 创建申诉并发送验证码
用户输入短信验证码
    -> 提交验证码
    -> 轮询任务状态
    -> completed 或 failed
```

服务端固定使用线路 2。外部平台不需要也不应该传入 `line`、`appId`、`grade`、`orderId`、`wxCode`、线路 2 账号或密码。

## 3. 创建申诉并发送验证码

接口：

```http
POST /tdx-line2/appeals
```

完整地址：

```text
http://43.143.254.68:4175/backend/api/open-api/v1/tdx-line2/appeals
```

请求体：

```json
{
  "phone": "13800138000",
  "externalRequestId": "partner-order-10001"
}
```

`externalRequestId` 是外部平台自己的订单号或请求号，最长 128 个字符。它是本次创建请求的幂等键：同一次请求因为网络超时而重试时必须继续使用原值；只有重新发起新的申诉尝试、重新获取验证码或更换手机号时，才生成新的值。也可以不传请求体字段，改用请求头 `Idempotency-Key`。

同一个 `externalRequestId` 如果已经绑定了其它手机号，服务会返回 `409`，并提示该请求号已被其它手机号使用。外部平台应在创建请求前生成并持久化请求号，不要在每次网络重试时重新生成。

cURL 示例：

```bash
curl -X POST \
  'http://43.143.254.68:4175/backend/api/open-api/v1/tdx-line2/appeals' \
  -H 'X-TDX-Integration-Token: <TDX_INTEGRATION_TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{
    "phone": "13800138000",
    "externalRequestId": "partner-order-10001"
  }'
```

成功响应：

```json
{
  "success": true,
  "message": "验证码已发送",
  "data": {
    "taskId": "tdx-ext-xxxxxxxx",
    "externalRequestId": "partner-order-10001",
    "phone": "13800138000",
    "status": "awaiting_sms",
    "message": "验证码已发送，等待用户输入短信验证码",
    "createdAt": "2026-10-06T15:30:00.000Z",
    "updatedAt": "2026-10-06T15:30:00.000Z"
  }
}
```

外部平台保存 `taskId`，后续提交验证码和查询状态都使用它。

## 4. 提交短信验证码

接口：

```http
POST /tdx-line2/appeals/{taskId}/submit
```

请求体：

```json
{
  "smsCode": "123456"
}
```

cURL 示例：

```bash
curl -X POST \
  'http://43.143.254.68:4175/backend/api/open-api/v1/tdx-line2/appeals/tdx-ext-xxxxxxxx/submit' \
  -H 'X-TDX-Integration-Token: <TDX_INTEGRATION_TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"smsCode":"123456"}'
```

验证码正确时，服务端会继续执行线路 2 登录和后续清理任务。响应中的 `status` 通常为 `processing`：

```json
{
  "success": true,
  "message": "线路2申诉已提交，正在处理。",
  "data": {
    "taskId": "tdx-ext-xxxxxxxx",
    "externalRequestId": "partner-order-10001",
    "phone": "13800138000",
    "status": "processing",
    "message": "线路2申诉已提交，正在处理。",
    "createdAt": "2026-10-06T15:30:00.000Z",
    "updatedAt": "2026-10-06T15:31:10.000Z"
  }
}
```

## 5. 查询任务状态

接口：

```http
GET /tdx-line2/appeals/{taskId}
```

cURL 示例：

```bash
curl \
  'http://43.143.254.68:4175/backend/api/open-api/v1/tdx-line2/appeals/tdx-ext-xxxxxxxx' \
  -H 'X-TDX-Integration-Token: <TDX_INTEGRATION_TOKEN>'
```

建议每 3-5 秒查询一次，直到进入终态。终态包括：

| 状态 | 含义 | 外部平台处理建议 |
|---|---|---|
| `awaiting_sms` | 等待用户输入验证码 | 显示验证码输入框 |
| `processing` | 已提交，正在处理 | 继续轮询 |
| `completed` | 处理完成 | 显示成功并停止轮询 |
| `failed` | 本次处理失败 | 显示失败原因并停止轮询 |
| `retryable` | 暂时不可用，可重新发起 | 使用新的 `externalRequestId` 重新创建申诉 |

## 6. 错误处理

| HTTP 状态码 | 含义 | 处理建议 |
|---:|---|---|
| `400` | 手机号、验证码或请求参数格式错误 | 修正参数后重试 |
| `401` | Token 缺失或错误 | 检查外部平台后端密钥配置 |
| `404` | 任务不存在或不属于当前接入账号 | 检查 `taskId`，不要继续提交 |
| `409` | 重复申诉、任务正在处理或当前状态不可操作 | 先查询任务状态；可重试时使用新的请求号 |
| `502` | 线路 2 上游或内部处理失败 | 先查询任务状态，再决定是否新建任务 |
| `503` | 服务未配置或线路维护 | 稍后重试 |

如果创建申诉接口发生网络超时，先使用相同的 `externalRequestId` 重试创建请求；不要立刻生成新请求号，否则可能创建重复任务。服务返回 `taskId` 后，外部平台应持久化它。

如果提交验证码后客户端发生网络超时，不要立即重复提交验证码。先调用状态查询接口：任务为 `processing` 时继续轮询，只有任务明确为 `retryable` 或 `failed` 时才重新发起。

发码失败的原任务会保留原 `externalRequestId`。重新获取验证码时必须生成新的 `externalRequestId`，避免幂等请求继续返回旧失败任务。

## 7. 外部平台示例代码

```js
const apiBase = 'http://43.143.254.68:4175/backend/api/open-api/v1'
const token = process.env.TDX_INTEGRATION_TOKEN
const businessOrderId = 'order-10001'
const attemptNo = 1
const phone = '13800138000'

async function callApi(path, options = {}) {
  const response = await fetch(`${apiBase}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-TDX-Integration-Token': token,
      ...(options.headers || {}),
    },
  })
  const payload = await response.json()
  if (!response.ok || payload.success === false) {
    throw new Error(payload.message || `HTTP ${response.status}`)
  }
  return payload.data
}

const externalRequestId = `partner-${businessOrderId}-${attemptNo}`

const created = await callApi('/tdx-line2/appeals', {
  method: 'POST',
  body: JSON.stringify({
    phone,
    externalRequestId,
  }),
})

const submitted = await callApi(`/tdx-line2/appeals/${created.taskId}/submit`, {
  method: 'POST',
  body: JSON.stringify({ smsCode: '123456' }),
})

const current = await callApi(`/tdx-line2/appeals/${created.taskId}`)
console.log({ submitted, current })
```

## 8. 数据边界和安全要求

- Token 只能放在外部平台服务端，不能下发到浏览器、App 或前端 JavaScript。
- 不要把 Token、短信验证码、请求头或完整上游响应写入日志。
- 外部平台只保存 `taskId`、自己的请求号和公开状态。
- 不要依赖 `orderId`、`wxCode`、上游 Token 等内部字段。
- 当前服务不启用 IP 白名单，但生产环境应使用 HTTPS 和密钥管理系统。
- 当前服务配置一个固定的线路 2 集成账号，任务查询和提交会校验该集成账号的归属。
- `externalRequestId` 当前在任务存储中全局查重，不按多个外部平台分别命名空间；多个平台共用服务时必须保证请求号全局唯一。

## 9. 接口文档和代码位置

- 路由实现：`src/backend-js/src/routes/openApi.js`
- 任务存储：`src/backend-js/src/data/tdxTaskStore.js`
- 集成 Token 鉴权：`src/backend-js/src/lib/authGuard.js`
- 线路 2 业务流程：`src/backend-js/src/routes/tdx.js`
- 接口索引：`src/backend-js/docs/api-map.md`
