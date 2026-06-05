import request from '@/utils/request'

export function listFreeQueryUser(query) {
  return request({
    url: '/server/freeQueryUser/list',
    method: 'get',
    params: query
  })
}

export function getFreeQueryUser(id) {
  return request({
    url: '/server/freeQueryUser/' + id,
    method: 'get'
  })
}

export function addFreeQueryUser(data) {
  return request({
    url: '/server/freeQueryUser',
    method: 'post',
    data
  })
}

export function updateFreeQueryUser(data) {
  return request({
    url: '/server/freeQueryUser',
    method: 'put',
    data
  })
}

export function delFreeQueryUser(id) {
  return request({
    url: '/server/freeQueryUser/' + id,
    method: 'delete'
  })
}

export function adjustFreeQueryUserPoints(data) {
  return request({
    url: '/server/freeQueryUser/adjustPoints',
    method: 'post',
    data
  })
}

export function resetFreeQueryUserPwd(data) {
  return request({
    url: '/server/freeQueryUser/resetPwd',
    method: 'put',
    data
  })
}
