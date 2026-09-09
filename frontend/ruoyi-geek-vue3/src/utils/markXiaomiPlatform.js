export const XIAOMI_PLATFORM_CODES = ['xiaomi']

export const XIAOMI_STYLE_PLATFORM_CODES = [
  'xiaomi',
  'sogou',
  'dianhuabang',
  'yidonggaopin',
  'ltgj'
]

const XIAOMI_STYLE_ROUTE_PATHS = {
  xiaomi: 'xiaomiMark',
  sogou: 'sogou',
  dianhuabang: 'dianhuabang',
  yidonggaopin: 'yidonggaopin',
  ltgj: 'ltgj'
}

const XIAOMI_STYLE_PLATFORM_NAMES = {
  xiaomi: '小米手机',
  sogou: '搜狗',
  dianhuabang: '电话邦',
  yidonggaopin: '移动高频',
  ltgj: '联通管家',
}

export const XIAOMI_DEDICATED_ROUTE = '/mark/xiaomiMark'

export const XIAOMI_DEDICATED_ROUTE_NAME = 'MarkUserXiaomi'

export const XIAOMI_PLATFORM_CODE = 'xiaomi'

export function normalizeXiaomiStylePlatformCode(code) {
  return String(code || '').trim().toLowerCase()
}

export function isXiaomiStylePlatformCode(code) {
  return XIAOMI_STYLE_PLATFORM_CODES.includes(normalizeXiaomiStylePlatformCode(code))
}

export function resolveXiaomiStyleRoutePath(code) {
  const normalized = normalizeXiaomiStylePlatformCode(code)
  return XIAOMI_STYLE_ROUTE_PATHS[normalized] || `markUser-${normalized || XIAOMI_PLATFORM_CODE}`
}

export function resolveXiaomiStylePlatformName(code) {
  const normalized = normalizeXiaomiStylePlatformCode(code)
  return XIAOMI_STYLE_PLATFORM_NAMES[normalized] || normalized || XIAOMI_STYLE_PLATFORM_NAMES.xiaomi
}

export function isDedicatedXiaomiPlatform(item) {
  if (!item) return false
  const code = String(item.platformCode || item.code || '').toLowerCase()
  const name = String(item.platformName || item.name || '')
  return isXiaomiStylePlatformCode(code) || name.includes('\u5c0f\u7c73')
}

export function isDedicatedXiaomiPlatformCode(code) {
  // Only Xiaomi has a dedicated page. Other Xiaomi-style platform codes use
  // the shared user platform page and must not redirect to the old route.
  return normalizeXiaomiStylePlatformCode(code) === XIAOMI_PLATFORM_CODE
}
