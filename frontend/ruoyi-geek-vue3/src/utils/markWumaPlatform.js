export const WUMA_PLATFORM_NAMES = Object.freeze({
  wuma_taidixiong: 'Taidixiong无码',
  wuma_tengxun: 'Tengxun无码',
  wuma_360: '360无码'
})

export const WUMA_PLATFORM_CODES = Object.freeze(Object.keys(WUMA_PLATFORM_NAMES))

export function isWumaPlatformCode(code) {
  return WUMA_PLATFORM_CODES.includes(String(code || '').trim().toLowerCase())
}
