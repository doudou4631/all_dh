# 标记用户端 RuoYi-App

本目录基于若依移动端 `RuoYi-App` / `uni-app` 改造，用于标记用户端手机适配。

已接入：

- 若依移动端登录、token、request、store、modal、tab 体系
- 标记平台列表：`/server/markUser/price/list`
- 标记提交：
  - 普通平台直接提交：`/server/markUser/order/clear`
  - 预查询平台：`/server/markUser/order/precheck`
  - 腾讯/TDX 短信验证码：`/server/markUser/tencent/submit`、`/server/markUser/tdxSecond/*`
- 任务记录：`/server/markUser/order/list`
- 订单详情：`/server/markUser/order/{orderId}`

测试服 API 默认走：

```js
// config.js
baseUrl: '/prod-api'
```

如使用 App 真机调试，可改成：

```js
baseUrl: 'http://212.64.16.212/prod-api'
```

开发建议使用 HBuilderX 打开本目录运行到 H5/手机模拟器。

## H5 发布到 `/mark-app/`

方案三采用 PC/手机分流：

- PC 访问 `http://212.64.16.212/`：继续显示现有后台/前端
- 手机访问 `http://212.64.16.212/`：自动跳转到 `http://212.64.16.212/mark-app/`

H5 构建方式：

```bash
cd frontend/ruoyi-app
npm install
npm run build:h5
```

构建产物目录：

```text
frontend/ruoyi-app/dist/build/h5
```

复制到主前端 public：

```bash
cd frontend
npm run sync:ruoyi-app-h5
```

然后再执行主前端构建：

```bash
cd frontend
npm run build:prod
```

服务器最终访问路径为：

```text
/www/wwwroot/frontend/mark-app/
/www/wwwroot/frontend/mark-app/static/images/tabbar/
```

其中 `static/images/tabbar` 是底部 tabBar 图标资源，缺失时底部会显示破图。

若使用 HBuilderX 发布 H5，也可以发布后执行 `npm run sync:ruoyi-app-h5`，脚本会优先读取 CLI 产物，其次读取 HBuilderX 的 `unpackage/dist/build/h5`。

---

原 RuoYi-App 说明见官方文档：https://doc.ruoyi.vip/ruoyi-app/
