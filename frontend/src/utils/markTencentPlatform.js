import { isDedicatedXiaomiPlatform } from '@/utils/markXiaomiPlatform'
import { isDedicatedBaiduPlatform } from '@/utils/markBaiduPlatform'
import { isDedicated360Platform } from '@/utils/markQihu360Platform'

export const TENCENT_PLATFORM_CODES = ['tencent_mark', 'tencent', 'tengxun', 'tx', 'txwz']

export const TENCENT_STYLE_PLATFORM_CODES = [
  ...TENCENT_PLATFORM_CODES,
  'td_second'
]

const TENCENT_STYLE_ROUTE_PATHS = {
  tencent_mark: 'tencentMark',
  tencent: 'tencentMark',
  tengxun: 'tencentMark',
  tx: 'tencentMark',
  txwz: 'tencentMark',
  td_second: 'tdSecondMark'
}

const TENCENT_STYLE_PLATFORM_NAMES = {
  tencent_mark: '腾讯速解',
  tencent: '腾讯',
  tengxun: '腾讯',
  tx: '腾讯',
  txwz: '腾讯',
  td_second: 'Taidixiong2'
}

export const TENCENT_DEDICATED_ROUTE = '/mark/tencentMark'

export const TENCENT_DEDICATED_ROUTE_NAME = 'MarkUserTencent'

export const TENCENT_PLATFORM_CODE = 'tengxun'

export function normalizeTencentStylePlatformCode(code) {
  return String(code || '').trim().toLowerCase()
}

export function isTencentStylePlatformCode(code) {
  return TENCENT_STYLE_PLATFORM_CODES.includes(normalizeTencentStylePlatformCode(code))
}

export function resolveTencentStyleRoutePath(code) {
  const normalized = normalizeTencentStylePlatformCode(code)
  return TENCENT_STYLE_ROUTE_PATHS[normalized] || `markUser-${normalized || TENCENT_PLATFORM_CODE}`
}

export function resolveTencentStylePlatformName(code) {
  const normalized = normalizeTencentStylePlatformCode(code)
  return TENCENT_STYLE_PLATFORM_NAMES[normalized] || normalized || TENCENT_STYLE_PLATFORM_NAMES.tengxun
}

export function isDedicatedTencentPlatform(item) {
  if (!item) {
    return false
  }
  const code = String(item.platformCode || item.code || '').toLowerCase()
  const name = String(item.platformName || item.name || '')
  return isTencentStylePlatformCode(code) || name.includes('\u817e\u8baf')
}

export function isDedicatedTencentPlatformCode(code) {
  return isTencentStylePlatformCode(code)
}

export function filterLegacyMarkPlatforms(list) {
  if (!Array.isArray(list)) {
    return []
  }
  return list.filter((item) => !isDedicatedTencentPlatform(item) && !isDedicatedXiaomiPlatform(item) && !isDedicatedBaiduPlatform(item) && !isDedicated360Platform(item))
}
