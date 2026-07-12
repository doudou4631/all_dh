import request from '@/utils/request'

// 我的平台单价/剩余次数
export function listMarkUserPlatformPrice() {
  return request({
    url: '/server/markUser/price/list',
    method: 'get'
  })
}

// 提交前预查询
export function precheckMarkUserOrder(data) {
  return request({
    url: '/server/markUser/order/precheck',
    method: 'post',
    data
  })
}

// 用户提交消除订单
export function createMarkUserClearOrder(data) {
  return request({
    url: '/server/markUser/order/clear',
    method: 'post',
    data
  })
}

// 用户订单列表
export function listMarkUserOrder(query) {
  return request({
    url: '/server/markUser/order/list',
    method: 'get',
    params: query
  })
}

// 用户订单详情
export function getMarkUserOrderDetail(orderId) {
  return request({
    url: '/server/markUser/order/' + orderId,
    method: 'get'
  })
}

// 腾讯验证码提交
export function submitMarkUserTencent(data) {
  return request({
    url: '/server/markUser/tencent/submit',
    method: 'post',
    data
  })
}

// 腾讯提交结果查询
export function getMarkUserTencentSubmitResult(itemId) {
  return request({
    url: '/server/markUser/tencent/submit/result/' + itemId,
    method: 'get'
  })
}

// Taidixiong 二次发送短信验证码
export function sendMarkUserTdxSecondCode(data) {
  return request({
    url: '/server/markUser/tdxSecond/sendCode',
    method: 'post',
    data
  })
}

// Taidixiong 二次提交申诉
export function submitMarkUserTdxSecond(data) {
  return request({
    url: '/server/markUser/tdxSecond/submit',
    method: 'post',
    data
  })
}
