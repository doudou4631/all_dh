import request from '@/utils/request'

export function listFreeQueryIpRecords(query) {
  return request({
    url: '/server/freeQuerySettings/ipRecord/list',
    method: 'get',
    params: query
  })
}

export function getFreeQueryIpRecordDetails(id) {
  return request({
    url: `/server/freeQuerySettings/ipRecord/${id}`,
    method: 'get'
  })
}
