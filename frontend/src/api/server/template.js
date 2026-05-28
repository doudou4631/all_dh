import request from '@/utils/request'

// 查询查询模板定义列表
export function listTemplate(query) {
  return request({
    url: '/server/template/list',
    method: 'get',
    params: query
  })
}

// 查询查询模板定义详细
export function getTemplate(id) {
  return request({
    url: '/server/template/' + id,
    method: 'get'
  })
}

// 新增查询模板定义
export function addTemplate(data) {
  return request({
    url: '/server/template',
    method: 'post',
    data: data
  })
}

// 修改查询模板定义
export function updateTemplate(data) {
  return request({
    url: '/server/template',
    method: 'put',
    data: data
  })
}

// 删除查询模板定义
export function delTemplate(id) {
  return request({
    url: '/server/template/' + id,
    method: 'delete'
  })
}
