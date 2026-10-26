import request from '@/utils/request'

export const listMarkCustomerApiKeys = (params) => request({ url: '/server/markCustomerApi/list', method: 'get', params })
export const listMarkCustomerApiAgents = () => request({ url: '/server/markCustomerApi/agent/options', method: 'get' })
export const listMarkCustomerApiPlatforms = () => request({ url: '/server/markCustomerApi/platform/options', method: 'get' })
export const createMarkCustomerApiKey = (data) => request({ url: '/server/markCustomerApi', method: 'post', data })
export const updateMarkCustomerApiKey = (data) => request({ url: '/server/markCustomerApi', method: 'put', data })
export const deleteMarkCustomerApiKey = (id) => request({ url: '/server/markCustomerApi/' + id, method: 'delete' })
export const updateMarkCustomerApiKeyStatus = (data) => request({ url: '/server/markCustomerApi/status', method: 'put', data })
export const rotateMarkCustomerApiSecret = (id) => request({ url: '/server/markCustomerApi/' + id + '/rotate', method: 'post' })
