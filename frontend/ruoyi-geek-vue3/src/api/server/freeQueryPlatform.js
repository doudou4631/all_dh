import request from '@/utils/request'

export function listFreeQueryPlatforms() {
  return request({
    url: '/server/freeQuerySettings/platform/list',
    method: 'get'
  })
}

export function updateFreeQueryPlatformDisplay(data) {
  return request({
    url: '/server/freeQuerySettings/platform/display',
    method: 'put',
    data
  })
}

export function changeFreeQueryPlatformStatus(id, status) {
  return request({
    url: '/server/freeQuerySettings/platform/status',
    method: 'put',
    data: { id, status }
  })
}

export function sortFreeQueryPlatforms(ids) {
  return request({
    url: '/server/freeQuerySettings/platform/sort',
    method: 'put',
    data: { ids }
  })
}
