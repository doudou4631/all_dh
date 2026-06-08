import { reactive, readonly } from 'vue'

const DEFAULT_PAGE_CODE = 'mobile-h5'
const PAGE_CODE_PATTERN = /^[a-z0-9-]{2,32}$/
const DEFAULTS = Object.freeze({
  requestedPage: DEFAULT_PAGE_CODE,
  pageCode: DEFAULT_PAGE_CODE,
  pageName: '手机页H5',
  servicePhone: '--',
  wechatQrUrl: '/mobile-h5/assets/icons/customer-wechat.png',
  navHomeUrl: '/',
  navQueryUrl: '/?tab=query',
  navBatchUrl: '/batch/',
  navProfileUrl: '/profile/',
  resultBackUrl: '/',
  apiBase: '/prod-api/',
  teddyProtocolBase: 'https://www.teddymobile.cn',
  entryUrl: '/mobile-h5/?page=mobile-h5'
})

const state = reactive({
  requestedPage: getRequestedPageFromUrl(),
  ready: false,
  loading: false,
  config: { ...DEFAULTS }
})

function trim(value) {
  return String(value || '').trim()
}

function normalizePageCode(value) {
  const code = trim(value).toLowerCase()
  if (!code) return ''
  if (!PAGE_CODE_PATTERN.test(code)) return ''
  return code
}

function ensureTrailingSlash(url) {
  const text = trim(url)
  if (!text) return ''
  return text.endsWith('/') ? text : `${text}/`
}

function getRequestedPageFromUrl() {
  try {
    const params = new URLSearchParams(window.location.search || '')
    return normalizePageCode(params.get('page')) || DEFAULT_PAGE_CODE
  } catch (error) {
    return DEFAULT_PAGE_CODE
  }
}

export function getAppBase() {
  const path = window.location.pathname || '/'
  if (path === '/mobile-h5' || path.indexOf('/mobile-h5/') === 0) {
    return '/mobile-h5'
  }
  if (path === '/mobile-h1' || path.indexOf('/mobile-h1/') === 0) {
    return '/mobile-h1'
  }
  return ''
}

function getRuntimeOverrides() {
  if (typeof window === 'undefined') return {}
  const source = window.BiaojiRuntimeOverrides || window.__BIAOJI_RUNTIME_OVERRIDES__
  if (!source || typeof source !== 'object') return {}
  return source
}

function resolveApiBase() {
  const overrides = getRuntimeOverrides()
  const byOverride = ensureTrailingSlash(overrides.apiBase || overrides.API_BASE || '')
  if (byOverride) return byOverride
  return ensureTrailingSlash(DEFAULTS.apiBase)
}

function mergeConfig(remoteData = {}) {
  const pageCode = normalizePageCode(remoteData.pageCode) || DEFAULT_PAGE_CODE
  const requestedPage = normalizePageCode(remoteData.requestedPage) || state.requestedPage || DEFAULT_PAGE_CODE
  return {
    requestedPage,
    pageCode,
    pageName: trim(remoteData.pageName) || state.config.pageName || DEFAULTS.pageName,
    servicePhone: trim(remoteData.servicePhone) || state.config.servicePhone || DEFAULTS.servicePhone,
    wechatQrUrl: trim(remoteData.wechatQrUrl) || state.config.wechatQrUrl || DEFAULTS.wechatQrUrl,
    navHomeUrl: trim(remoteData.navHomeUrl) || state.config.navHomeUrl || DEFAULTS.navHomeUrl,
    navQueryUrl: trim(remoteData.navQueryUrl) || state.config.navQueryUrl || DEFAULTS.navQueryUrl,
    navBatchUrl: trim(remoteData.navBatchUrl) || state.config.navBatchUrl || DEFAULTS.navBatchUrl,
    navProfileUrl: trim(remoteData.navProfileUrl) || state.config.navProfileUrl || DEFAULTS.navProfileUrl,
    resultBackUrl: trim(remoteData.resultBackUrl) || state.config.resultBackUrl || DEFAULTS.resultBackUrl,
    apiBase: ensureTrailingSlash(remoteData.apiBase) || state.config.apiBase || ensureTrailingSlash(DEFAULTS.apiBase),
    teddyProtocolBase: trim(remoteData.teddyProtocolBase) || state.config.teddyProtocolBase || DEFAULTS.teddyProtocolBase,
    entryUrl: trim(remoteData.entryUrl) || `/mobile-h5/?page=${encodeURIComponent(pageCode)}`
  }
}

function applyConfig(remoteData) {
  state.config = mergeConfig(remoteData)
}

function bindLegacyRuntimeConfig() {
  if (typeof window === 'undefined') return false
  const runtime = window.MobileRuntimeConfig
  if (!runtime || typeof runtime.getConfig !== 'function') return false
  applyConfig(runtime.getConfig() || {})
  if (typeof runtime.ready === 'function') {
    runtime.ready((latestConfig) => {
      applyConfig(latestConfig || {})
    })
  }
  state.ready = true
  return true
}

async function requestPublicConfig(pageCode) {
  const apiBase = resolveApiBase()
  const query = pageCode ? `?page=${encodeURIComponent(pageCode)}` : ''
  const response = await fetch(`${apiBase}server/mobilePageConfig/public/current${query}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
  if (!response.ok) {
    throw new Error('request_failed')
  }
  const payload = await response.json()
  const code = Number(payload?.code)
  if (code !== 0 && code !== 200) {
    throw new Error(payload?.msg || 'request_failed')
  }
  return payload?.data || {}
}

export async function loadMobileRuntimeConfig() {
  if (state.ready || state.loading) return
  if (bindLegacyRuntimeConfig()) return

  state.loading = true
  try {
    const data = await requestPublicConfig(state.requestedPage)
    applyConfig(data)
  } catch (error) {
    applyConfig({})
  } finally {
    state.loading = false
    state.ready = true
  }
}

function splitUrl(url) {
  let source = String(url || '')
  let hash = ''
  let query = ''
  const hashIndex = source.indexOf('#')
  if (hashIndex >= 0) {
    hash = source.slice(hashIndex)
    source = source.slice(0, hashIndex)
  }
  const queryIndex = source.indexOf('?')
  if (queryIndex >= 0) {
    query = source.slice(queryIndex + 1)
    source = source.slice(0, queryIndex)
  }
  return { path: source || '', query, hash }
}

function isExternalHref(href) {
  const text = trim(href).toLowerCase()
  if (!text) return false
  return (
    text.startsWith('http://') ||
    text.startsWith('https://') ||
    text.startsWith('//') ||
    text.startsWith('mailto:') ||
    text.startsWith('tel:') ||
    text.startsWith('javascript:') ||
    text.startsWith('data:')
  )
}

export function resolveHref(href, options = {}) {
  const raw = trim(href)
  if (!raw) return raw
  if (raw.startsWith('#')) return raw
  if (isExternalHref(raw)) return raw

  const parts = splitUrl(raw)
  if (!parts.path || !parts.path.startsWith('/')) return raw

  const appBase = getAppBase()
  let finalPath = parts.path
  if (appBase) {
    if (parts.path === appBase || parts.path.indexOf(`${appBase}/`) === 0) {
      finalPath = parts.path
    } else {
      finalPath = parts.path === '/' ? `${appBase}/` : `${appBase}${parts.path}`
    }
  }

  const pageCode = normalizePageCode(options.pageCode || state.config.pageCode || DEFAULT_PAGE_CODE)
  const shouldAddPage = options.addPage !== false
  let query = parts.query
  if (shouldAddPage && pageCode) {
    const params = new URLSearchParams(query || '')
    if (!params.has('page')) {
      params.set('page', pageCode)
    }
    query = params.toString()
  }
  return `${finalPath}${query ? `?${query}` : ''}${parts.hash}`
}

export function toPath(url) {
  const text = trim(url)
  if (!text) return ''
  if (!isExternalHref(text)) return text
  try {
    const parsed = new URL(text, window.location.origin)
    return `${parsed.pathname}${parsed.search || ''}${parsed.hash || ''}`
  } catch (error) {
    return text
  }
}

export function useMobileRuntimeConfig() {
  return {
    state: readonly(state),
    loadMobileRuntimeConfig,
    resolveHref,
    toPath,
    getAppBase
  }
}
