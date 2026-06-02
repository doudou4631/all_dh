# 去除标记 - 前端静态资源

从 http://hmbj.vhapi.com/ 下载的生产环境构建产物（uni-app + Vue 3 + Vite）。

## 目录结构

- `index.html` - 入口页
- `assets/` - JS/CSS 分块与样式

## 本地预览

- **仅本机**：双击 `启动.bat`，访问 http://127.0.0.1:8080
- **局域网**（手机/其他电脑同一 WiFi）：双击 `启动-局域网.bat`，用手机浏览器访问脚本里显示的 `http://192.168.x.x:8080`

若其他设备打不开，在 Windows「防火墙」中允许入站端口 **8080**。

## 布局

- `assets/fixed-layout.css` 与 [xbh5.open10086.com](https://xbh5.open10086.com) 一致：`max-width: 30rem` 居中，电脑约 480px 宽，手机铺满屏宽。

## 说明

- 这是**已编译打包**的前端，不是 Vue/uni-app 源码。
- 接口基地址在打包代码中为：`http://haomaapi.vhapi.com/`
- 页面依赖微信 JSSDK（`jweixin-1.6.0.js`），部分能力需在微信环境使用。
- 验证码等页面会加载第三方脚本（腾讯验证码、百度等外链）。
