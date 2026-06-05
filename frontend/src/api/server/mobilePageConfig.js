import request from '@/utils/request'

export function listMobilePageConfig(query) {
  return request({
    url: '/server/mobilePageConfig/list',
    method: 'get',
    params: query
  })
}

export function getMobilePageConfig(id) {
  return request({
    url: '/server/mobilePageConfig/' + id,
    method: 'get'
  })
}

export function addMobilePageConfig(data) {
  return request({
    url: '/server/mobilePageConfig',
    method: 'post',
    data
  })
}

export function updateMobilePageConfig(data) {
  return request({
    url: '/server/mobilePageConfig',
    method: 'put',
    data
  })
}

export function delMobilePageConfig(id) {
  return request({
    url: '/server/mobilePageConfig/' + id,
    method: 'delete'
  })
}
