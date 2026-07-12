export const TENCENT_CODES = ['tencent_mark', 'tencent', 'tengxun', 'tx', 'txwz', 'td_second']
export const TDX_SECOND_CODE = 'td_second'
export const DIRECT_CODES = [
  'xiaomi',
  'sghmt',
  'dianhuabang',
  'yidonggaopin',
  'ltgj',
  'baidu',
  'sanliuling',
  '360',
  'qihu_first',
  'qihu_second'
]

export function normalizeCode(value) {
  return String(value || '').trim().toLowerCase()
}

export function normalizePhone(value) {
  return String(value || '').replace(/[^\d]/g, '')
}

function collectPhonesFromSegment(segment) {
  const trimmed = String(segment || '').trim()
  if (!trimmed) return []

  const digitsOnly = trimmed.replace(/[^\d]/g, '')
  if (digitsOnly.length >= 7 && digitsOnly.length <= 15) {
    return [digitsOnly]
  }

  const candidates = []
  candidates.push(...(trimmed.match(/1[3-9]\d{9}/g) || []))
  candidates.push(...(trimmed.match(/0\d{2,3}[-\s.]?\d{7,8}/g) || []))
  candidates.push(...(trimmed.match(/[48]00[-\s.]?\d{7}/g) || []))
  candidates.push(...(trimmed.match(/\d{7,15}/g) || []))

  return candidates
    .map((item) => String(item || '').replace(/[^\d]/g, ''))
    .filter((phone) => phone.length >= 7 && phone.length <= 15)
}

export function parsePhones(text) {
  const segments = String(text || '')
    .split(/[\n,，;；\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
  const all = []
  let invalidCount = 0
  segments.forEach((segment) => {
    const phones = collectPhonesFromSegment(segment)
    if (!phones.length) invalidCount += 1
    all.push(...phones)
  })
  const phones = []
  const seen = new Set()
  all.forEach((phone) => {
    if (seen.has(phone)) return
    seen.add(phone)
    phones.push(phone)
  })
  return {
    inputCount: segments.length,
    validCount: phones.length,
    duplicateCount: Math.max(0, all.length - phones.length),
    invalidCount,
    phones
  }
}

export function platformMode(item) {
  const code = normalizeCode(item?.platformCode || item?.code || item)
  const name = String(item?.platformName || item?.name || '')
  if (TENCENT_CODES.includes(code) || name.includes('腾讯')) return 'sms'
  if (
    DIRECT_CODES.includes(code) ||
    name.includes('小米') ||
    name.includes('百度') ||
    name.includes('360') ||
    name.includes('奇虎') ||
    name.includes('搜狗') ||
    name.includes('电话邦') ||
    name.includes('移动高频') ||
    name.includes('联通')
  ) {
    return 'direct'
  }
  return 'precheck'
}

export function platformModeText(item) {
  const mode = platformMode(item)
  if (mode === 'sms') return normalizeCode(item?.platformCode || item) === TDX_SECOND_CODE ? '短信验证' : '验证码提交'
  if (mode === 'direct') return '直接提交'
  return '先查询后提交'
}

export function isPlatformEnabled(item) {
  return String(item?.status ?? '0') !== '1'
}

export function formatRemain(value) {
  const number = Number(value ?? 0)
  return Number.isFinite(number) ? Math.max(0, number) : 0
}

export function formatUnitPrice(value) {
  const number = Number(value ?? 1)
  return Number.isFinite(number) && number > 0 ? number : 1
}

export function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const p = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())} ${p(date.getHours())}:${p(date.getMinutes())}:${p(date.getSeconds())}`
}

export function recordStatusLabel(row) {
  const auditStatus = String(row?.auditStatus ?? '1')
  if (auditStatus === '0') return '待审核'
  if (auditStatus === '2') return '已拒绝'
  if (auditStatus === '3') return '已打回'
  const itemStatus = String(row?.itemProcessStatus ?? '')
  if (itemStatus === '3') return '处理中'
  if (itemStatus === '0') {
    const code = normalizeCode(row?.platformCode)
    return ['tencent_mark', 'tengxun', 'tencent', 'tx', 'txwz'].includes(code) ? '处理中' : '待处理'
  }
  if (itemStatus === '1') return '处理完成'
  if (itemStatus === '2') return '处理失败'
  const status = String(row?.orderStatus ?? '')
  const successCount = Number(row?.successCount ?? 0)
  const failedCount = Number(row?.failedCount ?? 0)
  if (status === '0') return '待处理'
  if (status === '1') return '处理中'
  if (status === '2') return failedCount > 0 && successCount <= 0 ? '处理失败' : failedCount > 0 ? '处理失败' : '处理完成'
  if (status === '3') return '处理失败'
  return '待处理'
}

export function recordStatusType(row) {
  const label = recordStatusLabel(row)
  if (label === '处理完成') return 'success'
  if (label === '处理失败' || label === '已拒绝') return 'error'
  if (label === '处理中' || label === '已打回') return 'warning'
  return 'default'
}

export function itemStatusLabel(value) {
  const status = String(value ?? '')
  if (status === '1') return '处理完成'
  if (status === '2') return '处理失败'
  if (status === '3') return '处理中'
  if (status === '0') return '待处理'
  return status || '-'
}

export function itemStatusType(value) {
  const status = String(value ?? '')
  if (status === '1') return 'success'
  if (status === '2') return 'error'
  if (status === '3' || status === '0') return 'warning'
  return 'default'
}

export function precheckResultText(row) {
  if (row?.querySuccess === false) return row?.errorMessage || row?.detail || '查询失败'
  if (row?.detail) {
    return String(row.detail).replace(/^[\w.-]+[：:]\s*/, '').replace(/^普通标记[-—]?/, '有标记')
  }
  if (row?.marked === true) return '已标记'
  if (row?.marked === false) return '未标记'
  return '查询成功'
}

export function isPrecheckSubmittable(row) {
  return row?.querySuccess === true && row?.marked !== false
}
