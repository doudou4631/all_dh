import request from '@/utils/request'

// 代理订单列表
export function listMarkAgentOrder(query) {
  return request({
    url: '/server/markAgent/order/list',
    method: 'get',
    params: query
  })
}

// 代理订单详情
export function getMarkAgentOrderDetail(orderId) {
  return request({
    url: '/server/markAgent/order/' + orderId,
    method: 'get'
  })
}

// 代理回填处理结果
export function feedbackMarkOrderItem(itemId, data) {
  return request({
    url: '/server/markAgent/item/' + itemId + '/feedback',
    method: 'post',
    data
  })
}

// 代理完成整单
export function completeMarkOrder(orderId) {
  return request({
    url: '/server/markAgent/order/' + orderId + '/complete',
    method: 'post'
  })
}

// 代理查询下线平台次数选项
export function listMarkAgentQuotaPlatformOptions(userId) {
  return request({
    url: '/server/markAgent/quota/platformOptions/' + userId,
    method: 'get'
  })
}

// 代理调整下线平台次数
export function adjustMarkAgentQuota(data) {
  return request({
    url: '/server/markAgent/quota/adjust',
    method: 'post',
    data
  })
}
