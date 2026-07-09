export const QIHU360_PLATFORM_CODES = ['sanliuling', '360', 'qihu_first', 'qihu_second']

const QIHU360_ROUTE_PATHS = {
  qihu_first: 'qihuFirstMark',
  qihu_second: 'qihuSecondMark',
  sanliuling: 'sanliulingMark',
  '360': 'sanliulingMark'
}

export function normalize360PlatformCode(code) {
  const normalized = String(code || '').trim().toLowerCase()
  if (!normalized) return ''
  if (normalized === '360') return 'sanliuling'
  if (QIHU360_PLATFORM_CODES.includes(normalized)) return normalized
  return normalized
}

export function is360PlatformCode(code) {
  const normalized = normalize360PlatformCode(code)
  return QIHU360_PLATFORM_CODES.includes(normalized)
}

export function resolve360PlatformNameFallback(platformCode) {
  const code = normalize360PlatformCode(platformCode)
  if (code === 'qihu_first') return '360\u9996\u6b21'
  if (code === 'qihu_second') return '360\u4e8c\u6b21'
  return '360'
}

export function is360PlatformNameMatch(item) {
  const name = String(item?.platformName || item?.name || '')
  return name.includes('360') || name.includes('\u5947\u864e')
}

export function isDedicated360Platform(item) {
  if (!item) return false
  const code = String(item.platformCode || item.code || '').trim().toLowerCase()
  if (code === '360' || QIHU360_PLATFORM_CODES.includes(code)) return true
  return is360PlatformNameMatch(item)
}

export function isDedicated360PlatformCode(code) {
  return is360PlatformCode(code)
}

export function resolve360DedicatedRoutePath(platformCode) {
  const code = normalize360PlatformCode(platformCode)
  return QIHU360_ROUTE_PATHS[code] || 'sanliulingMark'
}

export function resolve360DedicatedRoutePathByCode(platformCode) {
  return `/mark/${resolve360DedicatedRoutePath(platformCode)}`
}

export function resolve360DedicatedRouteName(platformCode) {
  const segment = normalize360PlatformCode(platformCode)
    .replace(/[^a-z0-9_-]+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^[-_]+|[-_]+$/g, '')
  return `MarkUser__${segment || 'sanliuling'}`
}

export function resolve360PlatformCodeForPage(code) {
  const normalized = normalize360PlatformCode(code)
  return is360PlatformCode(normalized) ? normalized : 'sanliuling'
}
