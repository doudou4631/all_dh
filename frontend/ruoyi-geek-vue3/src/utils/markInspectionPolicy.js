import { WUMA_PLATFORM_CODES } from './markWumaPlatform.js'

export const INSPECTION_SCOPE_OPTIONS = [
  { value: 'PENDING', label: '仅待处理' },
  { value: 'PROCESSING', label: '仅处理中' },
  { value: 'BOTH', label: '待处理及处理中' }
]

export function inspectionDefaults(platformCode) {
  if (WUMA_PLATFORM_CODES.includes(platformCode)) return { orderScope: 'BOTH', autoTakeover: true }
  return { orderScope: platformCode === 'xiaomi' ? 'BOTH' : 'PENDING', autoTakeover: false }
}

export function inspectionScopeLabel(row) {
  const scope = row.orderScope || inspectionDefaults(row.platformCode).orderScope
  return INSPECTION_SCOPE_OPTIONS.find(option => option.value === scope)?.label || '-'
}

export function inspectionForm(row = {}) {
  const defaults = inspectionDefaults(row.platformCode)
  return {
    id: null, policyName: '', platformCode: '', platformName: '', intervalValue: 30,
    intervalUnit: 'MINUTE', batchLimit: 100, autoAction: 'SUCCESS', status: '1', ...row,
    orderScope: row.orderScope || defaults.orderScope,
    autoTakeover: row.autoTakeover == null ? defaults.autoTakeover : [true, 1, '1'].includes(row.autoTakeover)
  }
}

export function inspectionRunSummary(log = {}) {
  return `巡检完成，扫描 ${log.scannedCount || 0} 条，实际检测 ${log.checkedCount || 0} 条，已处理 ${log.successCount || 0} 条，仍有标记 ${log.markedCount || 0} 条，检测失败 ${log.failedCount || 0} 条，跳过 ${log.skippedCount || 0} 条`
}
