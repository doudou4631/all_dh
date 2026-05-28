import request from '@/utils/request'

// 查询批量任务记录列表
export function listBatchTaskRecord(query) {
  return request({
    url: '/server/batchTask/list',
    method: 'get',
    params: query
  })
}

// 获取当前用户的运行中任务
export function getRunningTasks() {
  return request({
    url: '/server/batchTask/running',
    method: 'get'
  })
}

// 获取当前用户最近的任务记录
export function getRecentTasks(days) {
  return request({
    url: '/server/batchTask/recent/' + days,
    method: 'get'
  })
}

// 导出批量任务记录
export function exportBatchTaskRecord(query) {
  return request({
    url: '/server/batchTask/export',
    method: 'post',
    data: query
  })
}

// 获取批量任务记录详细信息
export function getBatchTaskRecord(id) {
  return request({
    url: '/server/batchTask/' + id,
    method: 'get'
  })
}

// 根据任务ID获取任务记录
export function getBatchTaskRecordByTaskId(taskId) {
  return request({
    url: '/server/batchTask/task/' + taskId,
    method: 'get'
  })
}

/**
 * 删除批量任务记录
 * 后端示例：remove(@PathVariable Long[] ids)
 * 请求：DELETE /server/batchTask/1,2,3（路径中为逗号分隔的 Long）
 * @param {number[] | string | number} ids 主键 id；多个时传数组，由本函数拼接为 path 段
 */
export function delBatchTaskRecord(ids) {
  const pathSegment = Array.isArray(ids) ? ids.join(',') : String(ids)
  return request({
    url: '/server/batchTask/' + pathSegment,
    method: 'delete'
  })
}

// 清理过期任务记录
export function cleanupBatchTaskRecord(days) {
  return request({
    url: '/server/batchTask/cleanup/' + days,
    method: 'delete'
  })
}

// 获取任务统计信息
export function getTaskStatistics() {
  return request({
    url: '/server/batchTask/statistics',
    method: 'get'
  })
}
