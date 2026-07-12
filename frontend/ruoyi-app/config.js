// 应用全局配置
export default {
  // H5 部署在同域名时走 nginx /prod-api 代理；App 打包时可改成测试服完整地址：
  // baseUrl: 'http://212.64.16.212/prod-api',
  baseUrl: '/prod-api',
  // 应用信息
  appInfo: {
    // 应用名称
    name: "标记用户端",
    // 应用版本
    version: "1.2.0",
    // 应用logo
    logo: "/static/logo.png",
    // 官方网站
    site_url: "http://ruoyi.vip",
    // 政策协议
    agreements: [{
        title: "隐私政策",
        url: "https://ruoyi.vip/protocol.html"
      },
      {
        title: "用户服务协议",
        url: "https://ruoyi.vip/protocol.html"
      }
    ]
  }
}
