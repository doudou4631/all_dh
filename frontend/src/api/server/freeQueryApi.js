import request from '@/utils/request'

export function getFreeQueryQuota() {
  return request({
    url: '/server/freeQuery/quota',
    method: 'get'
  })
}

export function queryFreeSingle(data) {
  return request({
    url: '/server/freeQuery/single',
    method: 'post',
    data
  })
}

export function listFreeQueryLogs(params) {
  return request({
    url: '/server/freeQuery/logs',
    method: 'get',
    params
  })
}

export function getFreeQueryLogsDashboard(params) {
  return request({
    url: '/server/freeQuery/logs/dashboard',
    method: 'get',
    params
  })
}

