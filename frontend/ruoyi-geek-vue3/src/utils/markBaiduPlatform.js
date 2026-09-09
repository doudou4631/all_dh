export const BAIDU_PLATFORM_CODES = ['baidu']

export const BAIDU_DEDICATED_ROUTE = '/mark/baiduMark'

export const BAIDU_DEDICATED_ROUTE_NAME = 'MarkUserBaidu'

export const BAIDU_PLATFORM_CODE = 'baidu'

export function isDedicatedBaiduPlatform(item) {
  if (!item) return false
  const code = String(item.platformCode || item.code || '').toLowerCase()
  const name = String(item.platformName || item.name || '')
  return BAIDU_PLATFORM_CODES.includes(code) || name.includes('\u767e\u5ea6')
}

export function isDedicatedBaiduPlatformCode(code) {
  return BAIDU_PLATFORM_CODES.includes(String(code || '').toLowerCase())
}
