import request from '@/utils/request'

// 查询积分流水记录列表
export function listPointRecord(query) {
  return request({
    url: '/server/pointRecord/list',
    method: 'get',
    params: query
  })
}

// 查询积分流水记录详细
export function getPointRecord(id) {
  return request({
    url: '/server/pointRecord/' + id,
    method: 'get'
  })
}

// 新增积分流水记录
export function addPointRecord(data) {
  return request({
    url: '/server/pointRecord',
    method: 'post',
    data: data
  })
}

// 调整用户积分并记录流水
export function adjustPointRecord(data) {
  return request({
    url: '/server/pointRecord/adjust',
    method: 'post',
    data: data
  })
}

// 修改积分流水记录
export function updatePointRecord(data) {
  return request({
    url: '/server/pointRecord',
    method: 'put',
    data: data
  })
}

// 删除积分流水记录
export function delPointRecord(id) {
  return request({
    url: '/server/pointRecord/' + id,
    method: 'delete'
  })
}
