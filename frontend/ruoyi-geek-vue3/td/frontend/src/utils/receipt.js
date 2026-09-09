export const CLEARED_RECEIPT_TEXT = '标记已消除'
export const CLEARED_RECEIPT_TITLE = '泰迪熊标记已成功取消，3-12小时后手机端数据同步更新'

function joinReceiptText(status = '', message = '') {
  return `${status || ''} ${message || ''}`.replace(/\s+/g, ' ').trim()
}

export function isRefundedReviewReceipt(status = '', message = '') {
  const text = joinReceiptText(status, message)
  return /本次退款|已退款|退款/.test(text) && (/审核中|正在审核|标记未消除|未消除/.test(text) || /本次退款/.test(text))
}

export function isNoActionReceipt(status = '', message = '') {
  const text = joinReceiptText(status, message)
  if (!text || isRefundedReviewReceipt(status, message)) return false
  return /无需清除|无需处理|无标记|无需申诉/.test(text)
}

export function isClearedReceipt(status = '', message = '') {
  const text = joinReceiptText(status, message)
  if (!text || isRefundedReviewReceipt(status, message)) return false
  if (/标记未消除|未消除/.test(text)) return false
  return /标记已取消|标记已成功取消|标记已消除|泰迪未来标记已取消|泰迪熊标记已成功取消|已消除|清除成功|消除成功/.test(text)
    || ((/已处理|成功|完成/.test(String(status || '')) || /已处理|成功|完成/.test(String(message || '')))
      && !/处理中|待处理|审核中|审核|失败|异常|错误|过期|退款|不可用/.test(text))
}

export function isFailedReceipt(status = '', message = '') {
  const text = joinReceiptText(status, message)
  if (!text || isRefundedReviewReceipt(status, message)) return false
  if (isClearedReceipt(status, message) || isNoActionReceipt(status, message)) return false
  return /失败|异常|错误|过期|驳回|拒绝|失效|不可用/.test(text)
}

export function isProcessingReceipt(status = '', message = '') {
  const text = joinReceiptText(status, message)
  if (!text) return false
  if (isRefundedReviewReceipt(status, message)) return false
  if (isClearedReceipt(status, message) || isNoActionReceipt(status, message) || isFailedReceipt(status, message)) return false
  return /处理中|已提交|等待|待处理|待清理|申诉/.test(String(status || ''))
    || /处理中|已提交|后台处理|等待|审核|同步/.test(String(message || ''))
}

export function isPositiveReceipt(status = '', message = '') {
  return isClearedReceipt(status, message) || isNoActionReceipt(status, message)
}

export function normalizeReceiptDisplayStatus(status = '', message = '', fallback = '处理回执') {
  const explicit = String(status || '').trim()
  if (isRefundedReviewReceipt(explicit, message)) return '审核中'
  if (isClearedReceipt(explicit, message)) return '已消除'
  if (isNoActionReceipt(explicit, message)) return '无需清除'
  if (isFailedReceipt(explicit, message)) return explicit || '处理失败'
  if (isProcessingReceipt(explicit, message)) return explicit || '处理中'
  if (explicit) return explicit
  return fallback
}

export function normalizeReceiptDisplayMessage(status = '', message = '', fallback = '暂无回执内容') {
  const text = String(message || '').trim()
  if (isRefundedReviewReceipt(status, text)) return '标记未消除，本次退款'
  if (isClearedReceipt(status, text)) return CLEARED_RECEIPT_TEXT
  if (isNoActionReceipt(status, text)) return '无需清除'
  if (text) return text
  if (isFailedReceipt(status, text)) return '处理失败'
  if (isProcessingReceipt(status, text)) return '系统已接收本次申诉，处理完成后结果会继续同步。'
  return fallback
}
