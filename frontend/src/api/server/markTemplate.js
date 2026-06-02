import request from '@/utils/request'

export function listMarkTemplate(query) {
  return request({
    url: '/server/markTemplate/list',
    method: 'get',
    params: query
  })
}

export function getMarkTemplate(id) {
  return request({
    url: '/server/markTemplate/' + id,
    method: 'get'
  })
}

export function addMarkTemplate(data) {
  return request({
    url: '/server/markTemplate',
    method: 'post',
    data
  })
}

export function updateMarkTemplate(data) {
  return request({
    url: '/server/markTemplate',
    method: 'put',
    data
  })
}

export function delMarkTemplate(ids) {
  return request({
    url: '/server/markTemplate/' + ids,
    method: 'delete'
  })
}

export function listMarkTemplateOptions() {
  return request({
    url: '/server/markTemplate/options',
    method: 'get'
  })
}

export function listMarkPlatformOptions() {
  return request({
    url: '/server/markTemplate/platformOptions',
    method: 'get'
  })
}
