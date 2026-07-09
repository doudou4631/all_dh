export const AUTO_PROCESSING_PLATFORMS = ['tencent_mark', 'tengxun', 'tencent', 'tx', 'txwz']

export const XIAOMI_PLATFORM_CODE = 'xiaomi'

export const MARK_ITEM_PROCESS_STATUS_OPTIONS = [
  { label: '待处理', value: '0' },
  { label: '处理中', value: '3' },
  { label: '处理完成', value: '1' },
  { label: '处理失败', value: '2' }
]

export const MARK_ITEM_FEEDBACK_OPTIONS = [
  { label: '处理完成', value: '1' },
  { label: '处理失败', value: '2' }
]

export function isAutoProcessingPlatform(row) {
  const code = String(row?.platformCode || '').trim().toLowerCase()
  return AUTO_PROCESSING_PLATFORMS.includes(code)
}

export function isXiaomiPlatform(row) {
  const code = String(row?.platformCode || '').trim().toLowerCase()
  return code === XIAOMI_PLATFORM_CODE
}

export function markItemProcessStatusLabel(status, row) {
  const code = String(status ?? '')
  if (code === '3') return '处理中'
  if (code === '0' && row && isAutoProcessingPlatform(row)) return '处理中'
  const map = { '0': '待处理', '1': '处理完成', '2': '处理失败', '3': '处理中' }
  return map[code] || '-'
}

export function markItemProcessStatusTagType(status, row) {
  const code = String(status ?? '')
  if (code === '1') return 'success'
  if (code === '2') return 'danger'
  if (code === '3') return 'warning'
  if (code === '0' && row && isAutoProcessingPlatform(row)) return 'warning'
  return 'info'
}

export function buildMarkItemProcessStatusQuery(rawQuery = {}) {
  const params = {
    pageNum: rawQuery.pageNum,
    pageSize: rawQuery.pageSize,
    phone: rawQuery.phone,
    platformCode: rawQuery.platformCode,
    platformCodes: rawQuery.platformCodes,
    processStatus: null,
    params: { ...(rawQuery.params || {}) }
  }
  const statusFilter = String(rawQuery.processStatus ?? '').trim()
  if (statusFilter === 'processing' || statusFilter === '3') {
    params.params.processingOnly = '1'
  } else if (statusFilter === 'pending' || statusFilter === '0') {
    params.params.pendingOnly = '1'
  } else if (statusFilter === '1' || statusFilter === '2') {
    params.processStatus = statusFilter
  }
  return params
}

export function canBatchProcessXiaomi(row) {
  return isXiaomiPlatform(row) && String(row?.processStatus || '0') === '0'
}

export function canBatchDetectXiaomi(row) {
  return isXiaomiPlatform(row) && String(row?.processStatus || '0') === '3'
}

export function canBatchSuccessXiaomi(row) {
  const status = String(row?.processStatus || '0')
  return isXiaomiPlatform(row) && (status === '0' || status === '2' || status === '3')
}

export function canSelectXiaomiBatchRow(row) {
  return canBatchProcessXiaomi(row) || canBatchDetectXiaomi(row) || canBatchSuccessXiaomi(row)
}

/** @deprecated use canBatchProcessXiaomi */
export function canBatchMarkXiaomiSubmitted(row) {
  return canBatchProcessXiaomi(row)
}
