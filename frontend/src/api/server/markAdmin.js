import request from '@/utils/request'

// 治理规则
export function listMarkGovernRule(query) {
  return request({
    url: '/server/markAdmin/rule/list',
    method: 'get',
    params: query
  })
}

export function getMarkGovernRule(id) {
  return request({
    url: '/server/markAdmin/rule/' + id,
    method: 'get'
  })
}

export function addMarkGovernRule(data) {
  return request({
    url: '/server/markAdmin/rule',
    method: 'post',
    data
  })
}

export function updateMarkGovernRule(data) {
  return request({
    url: '/server/markAdmin/rule',
    method: 'put',
    data
  })
}

export function delMarkGovernRule(ids) {
  return request({
    url: '/server/markAdmin/rule/' + ids,
    method: 'delete'
  })
}

// 仲裁工单
export function listMarkArbitrationCase(query) {
  return request({
    url: '/server/markAdmin/case/list',
    method: 'get',
    params: query
  })
}

export function getMarkArbitrationCase(id) {
  return request({
    url: '/server/markAdmin/case/' + id,
    method: 'get'
  })
}

export function addMarkArbitrationCase(data) {
  return request({
    url: '/server/markAdmin/case',
    method: 'post',
    data
  })
}

export function updateMarkArbitrationCase(data) {
  return request({
    url: '/server/markAdmin/case',
    method: 'put',
    data
  })
}

// 审计
export function listMarkAuditOrder(query) {
  return request({
    url: '/server/markAdmin/audit/order/list',
    method: 'get',
    params: query
  })
}

export function listMarkAuditWallet(query) {
  return request({
    url: '/server/markAdmin/audit/wallet/list',
    method: 'get',
    params: query
  })
}
