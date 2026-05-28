import request from '@/utils/request'

// 单次查询api
export function singleApi(data) {
  return request({
    url: '/server/apiServer/single',
    method: 'post',
    data: data
  })
}


// 批量查询api
export function batchApi(data) {
  return request({
    url: '/server/apiServer/batch',
    method: 'post',
    data: data
  })
}