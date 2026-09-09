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

// 用户管理中充值号码查询积分
export function rechargeQueryPoints(data) {
  return request({
    url: '/server/pointRecord/queryRecharge',
    method: 'post',
    data
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
