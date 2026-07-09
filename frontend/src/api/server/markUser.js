import request from '@/utils/request'

// 用户提交记录
export function listMarkUserSubmission(query) {
  return request({
    url: '/server/markUser/submission/list',
    method: 'get',
    params: query
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

// 用户提交订单
export function createMarkUserOrder(data) {
  return request({
    url: '/server/markUser/order',
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

// 用户提交前预查询
export function precheckMarkUserOrder(data) {
  return request({
    url: '/server/markUser/order/precheck',
    method: 'post',
    data
  })
}

// 腾讯号码实时状态查询
export function queryMarkUserTencentStatus(data) {
  return request({
    url: '/server/markUser/tencent/status/query',
    method: 'post',
    data
  })
}

// 腾讯手机号验证码提交
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

// 我的消息列表
export function listMarkUserNotice(query) {
  return request({
    url: '/server/markUser/notice/list',
    method: 'get',
    params: query
  })
}

// 未读消息数量
export function getMarkUserNoticeUnreadCount() {
  return request({
    url: '/server/markUser/notice/unread/count',
    method: 'get'
  })
}

// 消息详情
export function getMarkUserNoticeDetail(noticeId) {
  return request({
    url: '/server/markUser/notice/' + noticeId,
    method: 'get'
  })
}

// 标记已读
export function readMarkUserNotice(noticeId) {
  return request({
    url: '/server/markUser/notice/' + noticeId + '/read',
    method: 'post'
  })
}

// 全部已读
export function readAllMarkUserNotice() {
  return request({
    url: '/server/markUser/notice/readAll',
    method: 'post'
  })
}
