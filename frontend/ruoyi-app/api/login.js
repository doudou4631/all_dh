import request from '@/utils/request'

// 登录方法：当前后端使用 /captcha/get + CaptchaVO，登录时提交 captcha 对象
export function login(data) {
  return request({
    'url': '/login',
    headers: {
      isToken: false
    },
    'method': 'post',
    'data': data
  })
}

// 注册方法
export function register(data) {
  return request({
    url: '/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 获取用户详细信息
export function getInfo() {
  return request({
    'url': '/getInfo',
    'method': 'get'
  })
}

// 退出方法
export function logout() {
  return request({
    'url': '/logout',
    'method': 'post'
  })
}

// 获取验证码开关
export function getConfigKey(configKey) {
  return request({
    url: '/system/config/configKey/' + configKey,
    headers: {
      isToken: false
    },
    method: 'get'
  })
}

// 获取验证码
export function getCaptcha(data) {
  return request({
    url: '/captcha/get',
    headers: {
      isToken: false
    },
    method: 'post',
    data,
    timeout: 20000
  })
}
