import request from '@/utils/request'

// 代理待审核列表
export function listMarkAgentAuditPending(query) {
  return request({
    url: '/server/markAgent/audit/pending',
    method: 'get',
    params: query
  })
}

// 代理审核历史
export function listMarkAgentAuditHistory(query) {
  return request({
    url: '/server/markAgent/audit/history',
    method: 'get',
    params: query
  })
}

// 审核通过
export function passMarkAgentAudit(orderId, data) {
  return request({
    url: '/server/markAgent/audit/' + orderId + '/pass',
    method: 'post',
    data
  })
}

// 审核拒绝
export function rejectMarkAgentAudit(orderId, data) {
  return request({
    url: '/server/markAgent/audit/' + orderId + '/reject',
    method: 'post',
    data
  })
}

// 审核打回
export function returnMarkAgentAudit(orderId, data) {
  return request({
    url: '/server/markAgent/audit/' + orderId + '/return',
    method: 'post',
    data
  })
}

// 审核统计
export function getMarkAgentAuditStats() {
  return request({
    url: '/server/markAgent/audit/stats',
    method: 'get'
  })
}

// 代理处理明细列表
export function listMarkAgentOrderItem(query) {
  return request({
    url: '/server/markAgent/item/list',
    method: 'get',
    params: query
  })
}

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
// 代理查询下线账号流水
export function listMarkAgentWalletLog(query) {
  return request({
    url: '/server/markAgent/wallet/log/list',
    method: 'get',
    params: query
  })
}

// 代理下线账户概览
export function listMarkAgentDownstreamSummary(query) {
  return request({
    url: '/server/markAgent/downstream/summary',
    method: 'get',
    params: query
  })
}

// 代理模板平台列表（导航栏）
export function listMarkAgentPlatformOptions() {
  return request({
    url: '/server/markAgent/platform/list',
    method: 'get'
  })
}

// 当前代理账户概览
export function getMarkAgentMeSummary() {
  return request({
    url: '/server/markAgent/me/summary',
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

// 泰迪高频待处理订单自动检测
export function autoDetectTdGaopinItems() {
  return request({
    url: '/server/markAgent/item/autoDetectTdGaopin',
    method: 'post'
  })
}

// 小米待处理订单自动检测
export function autoDetectXiaomiItems() {
  return request({
    url: '/server/markAgent/item/autoDetectXiaomi',
    method: 'post'
  })
}

// 小米批量处理（开启自动检测）
export function batchProcessXiaomi(data) {
  return request({
    url: '/server/markAgent/item/batchProcessXiaomi',
    method: 'post',
    data
  })
}

// 小米批量手动检测
export function batchDetectXiaomi(data) {
  return request({
    url: '/server/markAgent/item/batchDetectXiaomi',
    method: 'post',
    data
  })
}

// 代理批量标记成功
export function batchMarkSuccess(data) {
  return request({
    url: '/server/markAgent/item/batchMarkSuccess',
    method: 'post',
    data
  })
}

// 代理批量标记失败
export function batchMarkFailed(data) {
  return request({
    url: '/server/markAgent/item/batchMarkFailed',
    method: 'post',
    data
  })
}

// 代理整单处理（可传 processStatus=1成功/2失败）
export function completeMarkOrder(orderId, data) {
  return request({
    url: '/server/markAgent/order/' + orderId + '/complete',
    method: 'post',
    data
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

// 代理开启/关闭下线平台
export function setMarkAgentPlatformStatus(data) {
  return request({
    url: '/server/markAgent/quota/status',
    method: 'post',
    data
  })
}
