import request from '@/utils/request'

// 查询用户积分关联列表
export function listPoint(query) {
  return request({
    url: '/server/point/list',
    method: 'get',
    params: query
  })
}

// 查询用户积分关联详细
export function getPoint(id) {
  return request({
    url: '/server/point/' + id,
    method: 'get'
  })
}

// 新增用户积分关联
export function addPoint(data) {
  return request({
    url: '/server/point',
    method: 'post',
    data: data
  })
}

// 修改用户积分关联
export function updatePoint(data) {
  return request({
    url: '/server/point',
    method: 'put',
    data: data
  })
}

// 删除用户积分关联
export function delPoint(id) {
  return request({
    url: '/server/point/' + id,
    method: 'delete'
  })
}
