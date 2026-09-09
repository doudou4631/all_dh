# 自动化 API 接口说明

## 1. 基本信息

- 正式环境地址：`https://biaoji.aleo1314.vip`
- API 基础路径：`https://biaoji.aleo1314.vip/prod-api/open/v1/mark`
- 数据格式：`application/json; charset=UTF-8`
- 鉴权方式：API Key + API Secret
- IP 白名单：不校验
- 登录要求：不需要调用 `/login`，不会产生在线用户会话

所有请求必须使用 HTTPS。不要把 API Secret 放在 URL、日志、前端网页或公开代码仓库中。

## 2. 创建 API 凭据

管理员进入“标记业务管理 -> 自动化 API 管理”，新增 API Key：

1. 填写名称，例如“高频自动处理”。
2. 填写绑定用户名，例如 `110125`。
3. 选择允许处理的平台编码。
4. 选择接口权限。
5. 可设置到期时间；不设置则永久有效，直到管理员停用。

创建成功后会返回 `API Key` 和 `API Secret`。Secret 只显示一次，关闭窗口后无法再次查看；丢失时需要执行“轮换 Secret”，旧 Secret 会立即失效。

## 3. 请求鉴权

每次请求都要携带以下请求头：

```http
X-API-Key: ak_xxxxxxxxxxxxxxxxxxxxxxxx
X-API-Secret: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

权限范围：

| 权限 | 说明 | 对应接口 |
| --- | --- | --- |
| `MARK_READ` | 读取授权平台的处理项 | `GET /items` |
| `MARK_FEEDBACK` | 回填授权平台的处理结果 | `POST /items/{itemId}/feedback` |

API Key 还会同时校验：启用状态、到期时间、绑定用户状态和平台授权。绑定用户本身必须有标记业务处理权限。

## 4. 查询处理项

### 请求

```http
GET /prod-api/open/v1/mark/items
```

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `platformCode` | string | 是 | 平台编码，必须在该 API Key 的授权平台中 |
| `pageNum` | integer | 否 | 页码，建议从 `1` 开始 |
| `pageSize` | integer | 否 | 每页数量，建议不超过 `100` |
| `phone` | string | 否 | 按号码模糊筛选 |
| `processStatus` | string | 否 | 状态：`0` 待处理、`1` 成功、`2` 失败、`3` 处理中 |
| `pendingOnly` | string | 否 | 传 `1` 时只查询该平台当前可处理的待处理项 |
| `processingOnly` | string | 否 | 传 `1` 时只查询处理中项目 |

自动化程序通常使用：

```bash
curl --request GET \
  'https://biaoji.aleo1314.vip/prod-api/open/v1/mark/items?platformCode=td_gaopin&pendingOnly=1&pageNum=1&pageSize=100' \
  --header 'X-API-Key: YOUR_API_KEY' \
  --header 'X-API-Secret: YOUR_API_SECRET'
```

### 成功响应

```json
{
  "total": 1,
  "rows": [
    {
      "id": 123456,
      "orderId": 10001,
      "orderNo": "MO202608020001",
      "platformCode": "td_gaopin",
      "platformName": "TdGaopin高频",
      "phone": "13800138000",
      "processStatus": "0",
      "processResult": null,
      "processNote": null,
      "processedBy": null,
      "processedTime": null,
      "userName": "110125",
      "orderRemark": null,
      "createTime": "2026-08-02 10:00:00",
      "updateTime": "2026-08-02 10:00:00"
    }
  ],
  "code": 200,
  "msg": "查询成功"
}
```

`rows[].id` 是回填接口使用的 `itemId`，不是订单 ID。

## 5. 回填处理结果

### 请求

```http
POST /prod-api/open/v1/mark/items/{itemId}/feedback
Content-Type: application/json
```

路径参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `itemId` | integer | 是 | 查询接口返回的明细 `id` |

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `processStatus` | string | 是 | 仅支持 `1` 成功或 `2` 失败 |
| `processResult` | string | 否 | 平台返回的检测或处理结果 |
| `processNote` | string | 否 | 补充说明、失败原因等 |

成功示例：

```bash
curl --request POST \
  'https://biaoji.aleo1314.vip/prod-api/open/v1/mark/items/123456/feedback' \
  --header 'Content-Type: application/json' \
  --header 'X-API-Key: YOUR_API_KEY' \
  --header 'X-API-Secret: YOUR_API_SECRET' \
  --data '{
    "processStatus": "1",
    "processResult": "已完成平台处理",
    "processNote": "自动化程序回填"
  }'
```

失败示例：

```json
{
  "processStatus": "2",
  "processResult": "平台处理失败",
  "processNote": "第三方平台返回失败"
}
```

### 成功响应

```json
{
  "code": 200,
  "msg": "回填成功",
  "data": {
    "order": {},
    "items": []
  }
}
```

`data` 返回更新后的订单及全部明细。自动化程序应以 `code == 200` 作为业务成功依据。

### 业务影响

- 回填失败时，系统会按现有订单规则自动退款，避免重复退款。
- 已失败且已退款的项目改回成功时，系统会重新扣费。
- 腾讯类订单由后台自动处理，不支持此接口手工回填。
- API Key 只能回填其授权平台且绑定用户有权访问的订单明细。

## 6. 错误响应

鉴权失败使用 HTTP `401`：

```json
{
  "code": 401,
  "msg": "缺少 API Key 或 Secret"
}
```

常见错误：

| HTTP/业务状态 | 原因 | 处理方式 |
| --- | --- | --- |
| `401` | 缺少 Key/Secret | 检查两个请求头 |
| `401` | API Key 无效或已停用 | 在管理页面检查状态 |
| `401` | API Secret 无效 | 使用创建或轮换后最新的 Secret |
| `401` | API Key 已过期 | 创建新的 API Key |
| 业务错误 | 没有接口权限 | 增加 `MARK_READ` 或 `MARK_FEEDBACK` 权限 |
| 业务错误 | 无权处理该平台 | 给 Key 增加正确的平台编码 |
| 业务错误 | 订单明细不存在 | 检查 `itemId` 是否来自查询结果 |
| 业务错误 | 处理状态不支持 | 只传字符串 `"1"` 或 `"2"` |
| 业务错误 | 订单未审核或账号无权访问 | 检查订单与绑定用户权限 |

调用方应同时检查 HTTP 状态码和响应 JSON 中的 `code`，不要只根据 HTTP `200` 判断业务成功。

## 7. Python 调用示例

```python
import requests

BASE_URL = "https://biaoji.aleo1314.vip/prod-api/open/v1/mark"
HEADERS = {
    "X-API-Key": "YOUR_API_KEY",
    "X-API-Secret": "YOUR_API_SECRET",
}

response = requests.get(
    f"{BASE_URL}/items",
    headers=HEADERS,
    params={
        "platformCode": "td_gaopin",
        "pendingOnly": "1",
        "pageNum": 1,
        "pageSize": 100,
    },
    timeout=30,
)
response.raise_for_status()
result = response.json()

for item in result.get("rows", []):
    feedback = requests.post(
        f"{BASE_URL}/items/{item['id']}/feedback",
        headers=HEADERS,
        json={
            "processStatus": "1",
            "processResult": "自动处理成功",
            "processNote": "API 自动回填",
        },
        timeout=30,
    )
    feedback.raise_for_status()
    body = feedback.json()
    if body.get("code") != 200:
        raise RuntimeError(body.get("msg", "回填失败"))
```

## 8. 调用建议

1. 每个平台单独创建 API Key，便于停用、轮换和审计。
2. 每次只授权所需的平台和权限，遵循最小权限原则。
3. 查询时使用 `pendingOnly=1`，处理成功后再回填。
4. 保存已处理 `itemId`，避免程序重启后重复提交第三方平台。
5. 对网络超时进行有限次数重试；回填超时后先重新查询状态，再决定是否重试。
6. 遇到 `401` 不要无限重试，应停止任务并检查凭据。
7. Secret 轮换时先更新调用方配置，再停用旧调用进程。

## 9. 审计与会话

每次 API 调用会记录 API Key、接口路径、请求方法、平台、成功状态、耗时和调用时间。API 调用不创建系统登录 Token，因此不会出现在“在线用户”列表中。

文档版本：2026-08-02
