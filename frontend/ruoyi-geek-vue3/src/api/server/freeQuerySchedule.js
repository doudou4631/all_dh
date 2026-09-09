import request from '@/utils/request'

export function listFreeQuerySchedule(query) {
  return request({
    url: '/server/freeQuerySettings/schedule/list',
    method: 'get',
    params: query
  })
}

export function getFreeQuerySchedule(id) {
  return request({
    url: `/server/freeQuerySettings/schedule/${id}`,
    method: 'get'
  })
}

export function addFreeQuerySchedule(data) {
  return request({
    url: '/server/freeQuerySettings/schedule',
    method: 'post',
    data
  })
}

export function updateFreeQuerySchedule(data) {
  return request({
    url: '/server/freeQuerySettings/schedule',
    method: 'put',
    data
  })
}

export function changeFreeQueryScheduleStatus(id, status) {
  return request({
    url: '/server/freeQuerySettings/schedule/changeStatus',
    method: 'put',
    data: { id, status }
  })
}

export function delFreeQuerySchedule(ids) {
  return request({
    url: `/server/freeQuerySettings/schedule/${ids}`,
    method: 'delete'
  })
}
