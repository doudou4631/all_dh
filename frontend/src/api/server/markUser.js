import request from '@/utils/request'

// 用户订单列表
export function listMarkUserOrder(query) {
  return request({
    url: '/server/markUser/order/list',
    method: 'get',
    params: query
  })
}

// 用户提交订单
export function createMarkUserOrder(data) {
  return request({
    url: '/server/markUser/order',
    method: 'post',
    data
  })
}

// 用户订单详情
export function getMarkUserOrderDetail(orderId) {
  return request({
    url: '/server/markUser/order/' + orderId,
    method: 'get'
  })
}

// 钱包汇总
export function getMarkUserWalletSummary() {
  return request({
    url: '/server/markUser/wallet/summary',
    method: 'get'
  })
}

// 钱包流水列表
export function listMarkUserWalletLog(query) {
  return request({
    url: '/server/markUser/wallet/log/list',
    method: 'get',
    params: query
  })
}

// 我的平台单价
export function listMarkUserPlatformPrice() {
  return request({
    url: '/server/markUser/price/list',
    method: 'get'
  })
}
