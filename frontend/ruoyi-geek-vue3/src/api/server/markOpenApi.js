import request from '@/utils/request'

export const listMarkOpenApiKeys = params => request({ url: '/server/markOpenApi/list', method: 'get', params })
export const createMarkOpenApiKey = data => request({ url: '/server/markOpenApi', method: 'post', data })
export const updateMarkOpenApiKeyStatus = data => request({ url: '/server/markOpenApi/status', method: 'put', data })
export const rotateMarkOpenApiSecret = id => request({ url: `/server/markOpenApi/${id}/rotate`, method: 'post' })
