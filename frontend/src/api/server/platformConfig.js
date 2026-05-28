import request from '@/utils/request'

// 查询查询平台url配置列表
export function listPlatformConfig(query) {
  return request({
    url: '/server/platformConfig/list',
    method: 'get',
    params: query
  })
}

// 查询查询平台url配置用户列表
export function listPlatformConfigUser(query) {
  return request({
    url: '/server/platformConfig/userList',
    method: 'get',
    params: query
  })
}

// 查询查询平台url配置详细
export function getPlatformConfig(id) {
  return request({
    url: '/server/platformConfig/' + id,
    method: 'get'
  })
}

// 新增查询平台url配置
export function addPlatformConfig(data) {
  return request({
    url: '/server/platformConfig',
    method: 'post',
    data: data
  })
}

// 修改查询平台url配置
export function updatePlatformConfig(data) {
  return request({
    url: '/server/platformConfig',
    method: 'put',
    data: data
  })
}

// 删除查询平台url配置
export function delPlatformConfig(id) {
  return request({
    url: '/server/platformConfig/' + id,
    method: 'delete'
  })
}
