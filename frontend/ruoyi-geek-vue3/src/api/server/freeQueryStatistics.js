import request from '@/utils/request'

export function getFreeQueryStatistics() {
  return request({
    url: '/server/freeQuerySettings/statistics/summary',
    method: 'get'
  })
}
