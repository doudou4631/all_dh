import request from '@/utils/request'

export const listInspectionPolicies = params => request({ url: '/server/mark/inspection/policy/list', method: 'get', params })
export const getInspectionPolicy = id => request({ url: `/server/mark/inspection/policy/${id}`, method: 'get' })
export const addInspectionPolicy = data => request({ url: '/server/mark/inspection/policy', method: 'post', data })
export const updateInspectionPolicy = data => request({ url: '/server/mark/inspection/policy', method: 'put', data })
export const deleteInspectionPolicy = id => request({ url: `/server/mark/inspection/policy/${id}`, method: 'delete' })
export const runInspectionPolicy = id => request({ url: `/server/mark/inspection/policy/${id}/run`, method: 'post' })
export const listInspectionPlatforms = () => request({ url: '/server/mark/inspection/platform/options', method: 'get' })
export const listInspectionLogs = params => request({ url: '/server/mark/inspection/log/list', method: 'get', params })
export const listInspectionLogDetails = (id, params) => request({ url: `/server/mark/inspection/log/${id}/detail/list`, method: 'get', params })
