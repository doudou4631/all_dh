import request from '@/utils/request'

// 查询聚合配置列表
export function listAggConfig(query) {
  return request({
    url: '/server/aggConfig/list',
    method: 'get',
    params: query
  })
}

// 查询聚合配置详细
export function getAggConfig(id) {
  return request({
    url: '/server/aggConfig/' + id,
    method: 'get'
  })
}

// 新增聚合配置
export function addAggConfig(data) {
  return request({
    url: '/server/aggConfig',
    method: 'post',
    data: data
  })
}

// 修改聚合配置
export function updateAggConfig(data) {
  return request({
    url: '/server/aggConfig',
    method: 'put',
    data: data
  })
}

// 删除聚合配置
export function delAggConfig(id) {
  return request({
    url: '/server/aggConfig/' + id,
    method: 'delete'
  })
}
