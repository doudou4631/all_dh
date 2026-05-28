import request from '@/utils/request'

// 查询接口查询记录通用列表
export function listApiRecord(query) {
  return request({
    url: '/server/apiRecord/list',
    method: 'get',
    params: query
  })
}

/** 查询记录：按 phone + taskId 聚合，服务端分页（单条/批量等全部类型，可用 queryType 筛选） */
export function listSingleQueryBatch(query) {
  return request({
    url: '/server/apiRecord/singleBatch/list',
    method: 'get',
    params: query
  })
}

/** 某批次下各平台完整记录（单条/批量） */
export function getSingleQueryBatchDetail(phone, batchKey) {
  return request({
    url: '/server/apiRecord/singleBatch/detail',
    method: 'get',
    params: { phone, batchKey }
  })
}

// 查询接口查询记录通用详细
export function getApiRecord(id) {
  return request({
    url: '/server/apiRecord/' + id,
    method: 'get'
  })
}

// 新增接口查询记录通用
export function addApiRecord(data) {
  return request({
    url: '/server/apiRecord',
    method: 'post',
    data: data
  })
}

// 修改接口查询记录通用
export function updateApiRecord(data) {
  return request({
    url: '/server/apiRecord',
    method: 'put',
    data: data
  })
}

// 删除接口查询记录通用
export function delApiRecord(id) {
  return request({
    url: '/server/apiRecord/' + id,
    method: 'delete'
  })
}
