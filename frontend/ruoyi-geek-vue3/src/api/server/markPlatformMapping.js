import request from '@/utils/request'

export const listMarkPlatformMappings = params => request({ url: '/server/mark/platformMapping/list', method: 'get', params })
export const addMarkPlatformMapping = data => request({ url: '/server/mark/platformMapping', method: 'post', data })
export const updateMarkPlatformMapping = data => request({ url: '/server/mark/platformMapping', method: 'put', data })
export const deleteMarkPlatformMapping = id => request({ url: `/server/mark/platformMapping/${id}`, method: 'delete' })
export const changeSourceStatus = data => request({ url: '/server/mark/platformMapping/source-status', method: 'put', data })
export const changeTargetStatus = data => request({ url: '/server/mark/platformMapping/target-status', method: 'put', data })
export const changeMappingStatus = data => request({ url: '/server/mark/platformMapping/mapping-status', method: 'put', data })
