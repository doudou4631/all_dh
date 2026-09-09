import request from '@/utils/request'
export const listAgents = params => request({ url:'/server/markAgentManagement/list',method:'get',params })
export const getAgentCapabilities = () => request({ url:'/server/markAgentManagement/capabilities',method:'get' })
export const createAgent = data => request({ url:'/server/markAgentManagement',method:'post',data })
export const changeAgentStatus = data => request({ url:'/server/markAgentManagement/status',method:'put',data })
export const resetAgentPassword = data => request({ url:'/server/markAgentManagement/resetPassword',method:'put',data })
export const getAgentPlatformOptions = id => request({ url:`/server/markAgentManagement/platform/options/${id}`,method:'get' })
export const adminRechargeAgent = data => request({ url:'/server/markAgentManagement/admin/recharge',method:'post',data })
export const transferAgentPoints = data => request({ url:'/server/markAgentManagement/agent/transfer',method:'post',data })
