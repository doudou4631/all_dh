import request from '@/utils/request'

export function getFreeQueryPageConfig(pageCode = '1') {
  return request({
    url: '/server/freeQuerySettings/page/config',
    method: 'get',
    params: { pageCode }
  })
}

export function listFreeQueryPageConfigs() {
  return request({
    url: '/server/freeQuerySettings/page/configs',
    method: 'get'
  })
}

export function updateFreeQueryPageConfig(data) {
  return request({
    url: '/server/freeQuerySettings/page/config',
    method: 'put',
    data
  })
}
