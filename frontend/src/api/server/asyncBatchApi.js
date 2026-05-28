import request from '@/utils/request'

// 异步批量查询 - 提交任务
export function submitBatchQuery(data) {
  return request({
    url: '/server/apiServer/asyncBatch',
    method: 'post',
    data: data,
    timeout: 120000 // 批量查询任务提交使用2分钟超时
  })
}

/** 优化版：后端并行 + 平台限流 + 进度落库节流，轮询/取消接口与旧版相同 */
export function submitBatchQueryOptimized(data) {
  return request({
    url: '/server/apiServer/asyncBatchOpt',
    method: 'post',
    data: data,
    timeout: 120000
  })
}

// 查询批量任务状态（options.silent：静默失败，不弹全局错误）
export function getBatchTaskStatus(taskId, options = {}) {
  return request({
    url: '/server/apiServer/taskStatus/' + taskId,
    method: 'get',
    silent: Boolean(options.silent)
  })
}

// 获取批量任务结果
export function getBatchTaskResults(taskId, options = {}) {
  return request({
    url: '/server/apiServer/taskResults/' + taskId,
    method: 'get',
    silent: Boolean(options.silent)
  })
}

// 取消批量任务
export function cancelBatchTask(taskId) {
  return request({
    url: '/server/apiServer/cancelTask/' + taskId,
    method: 'delete'
  })
}
