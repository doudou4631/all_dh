# 腾讯复查/提交流程网络代理接入说明（预留）
适用场景：后期需要给腾讯链路加网络代理，避免复查与提交长期使用同一出口 IP。

## 1. 生效范围
本说明覆盖以下两个接口对应的腾讯外呼：
- 复查接口：`POST /server/markUser/tencent/status/query`
- 提交接口：`POST /server/markUser/tencent/submit`

当前两条链路都走同一个后端调用入口：
- `backend/geek-server/src/main/java/com/geek/server/service/impl/MarkOrderServiceImpl.java`
  - `queryTencentStatus(...)`
  - `submitTencent(...)`
  - `callTencentJsonp(...)`

只要在 `callTencentJsonp(...)` 增加“可选代理”能力，复查和提交会同时生效。

## 2. 推荐方案（最小改造、只影响腾讯链路）
推荐只改腾讯这条链路，不改全局 `HttpUtils` 行为，避免影响其它平台查询。

实施思路：
1. 新增腾讯代理配置（开关 + 地址 + 认证）。
2. `callTencentJsonp(...)` 判断开关：
   - 未开启：继续走当前直连逻辑。
   - 已开启：走带代理的 HTTP GET。
3. 代理请求失败时，返回失败并打日志；是否降级直连可按策略选择（默认建议不降级，避免 IP 泄露回直连）。

## 3. 建议新增配置项
配置文件建议放在：
- `backend/geek-admin/src/main/resources/application-data.yml`

建议结构（示例）：
- `tencent.proxy.enabled`: 是否启用代理
- `tencent.proxy.host`: 代理主机
- `tencent.proxy.port`: 代理端口
- `tencent.proxy.scheme`: `http` / `https`
- `tencent.proxy.username`: 代理账号（可空）
- `tencent.proxy.password`: 代理密码（可空）
- `tencent.proxy.connect-timeout-ms`: 连接超时
- `tencent.proxy.response-timeout-ms`: 响应超时

建议默认：
- `enabled=false`
- 其余为空或沿用当前超时

## 4. 建议代码落点
## 4.1 新增配置类
建议新增：
- `backend/geek-server/src/main/java/com/geek/server/config/TencentProxyProperties.java`

用于接收 `tencent.proxy.*` 配置，字段包含：
- `enabled`
- `scheme`
- `host`
- `port`
- `username`
- `password`
- `connectTimeoutMs`
- `responseTimeoutMs`

## 4.2 改造腾讯请求方法
修改：
- `backend/geek-server/src/main/java/com/geek/server/service/impl/MarkOrderServiceImpl.java`

目标：
- 在 `callTencentJsonp(...)` 内按配置分流：
  - 直连：保留 `HttpUtils.get(url, headers)`。
  - 代理：新增 `doTencentGetViaProxy(...)`（建议私有方法）。

## 4.3 代理请求实现建议
可用 Apache HttpClient（项目已有依赖）实现带代理请求：
- 设置代理主机/端口
- 带请求头（`Referer`、`User-Agent` 等）
- 需要账号密码时配置代理认证
- 读取响应后继续复用 `parseTencentJsonp(...)`

说明：
- 先支持 HTTP/HTTPS 代理即可满足大多数场景。
- 如果后期需要 SOCKS5，再单独扩展（不建议第一版就做复杂化）。

## 5. 日志与安全建议
- 日志打印代理配置时不要输出明文密码。
- 失败日志建议包含：
  - 接口路径（如 `/core/sjg/phone_complain_status`）
  - 是否走代理
  - 代理主机与端口（可脱敏）
  - 异常类型与简要信息
- 配置文件中的账号密码建议通过环境注入，不建议明文硬编码到仓库。

## 6. 验证步骤
## 6.1 本地构建验证
- 后端编译：
  - `mvn -f backend/pom.xml -pl geek-server -am -DskipTests compile`

## 6.2 功能验证
1. `enabled=false` 时，复查与提交行为与当前一致。
2. `enabled=true` 且代理可用时：
   - 复查接口返回正常
   - 提交接口返回正常
3. `enabled=true` 但代理不可用时：
   - 接口应返回明确失败信息（按既定错误处理逻辑）
   - 日志可定位到代理连接问题

## 6.3 线上验证
- 发布后优先验证：
  - `/server/markUser/tencent/status/query`
  - `/server/markUser/tencent/submit`
- 观察服务日志是否出现代理连接异常、超时、认证失败等告警。

## 7. 回滚方案
紧急回滚不需要改代码，直接配置回滚：
1. 将 `tencent.proxy.enabled` 改为 `false`
2. 重启后端服务

如果是代码层问题，再回滚到上一版本 Jar。

## 8. 后续增强（可选）
- 代理池轮换（多代理随机/轮询）
- 按失败次数切换下一代理
- 代理健康检查与熔断
- 提交和复查使用不同代理组（按业务策略隔离）

---
结论：这项改造属于低到中等复杂度，第一版按“单代理可开关”实现最稳，且已能同时覆盖复查与提交两条链路。
