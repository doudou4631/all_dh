import request from '@/utils/request'

export function getFreeQueryIpPolicy() {
  return request({
    url: '/server/freeQuerySettings/ipPolicy/config',
    method: 'get'
  })
}

export function updateFreeQueryIpPolicy(data) {
  return request({
    url: '/server/freeQuerySettings/ipPolicy/config',
    method: 'put',
    data
  })
}
