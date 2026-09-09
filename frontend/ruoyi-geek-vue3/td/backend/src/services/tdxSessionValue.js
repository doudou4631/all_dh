const WXCODE_FORMAT_RE = /^[A-Za-z0-9_-]{12,128}$/
const JWT_FORMAT_RE = /^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/
const FAILURE_TEXT_RE = /fail|error|invalid|expired|forbidden|paused|suspend|unauthori[sz]ed|denied|blocked|授权失败|授权异常|未授权|无效|错误|失败|过期|禁止|封禁|停用|暂停|复扫|风控|拦截|限制|异常/i
const DISALLOWED_PUNCTUATION_RE = /[:：,，。；;（）()【】\[\]{}<>《》"'`]/u
const CJK_RE = /[\u3400-\u9fff\uf900-\ufaff]/u

function normalizeText(value = '') {
  return String(value ?? '').trim()
}

export function looksLikeFailureText(value = '') {
  const text = normalizeText(value)
  return Boolean(text) && FAILURE_TEXT_RE.test(text)
}

export function isUsableWxCode(value = '') {
  const text = normalizeText(value)
  if (!text) return false
  if (text.length < 12 || text.length > 128) return false
  if (/\s/u.test(text)) return false
  if (CJK_RE.test(text)) return false
  if (DISALLOWED_PUNCTUATION_RE.test(text)) return false
  if (!WXCODE_FORMAT_RE.test(text)) return false
  if (looksLikeFailureText(text)) return false
  return true
}

export function sanitizeStoredWxCode(value = '') {
  const text = normalizeText(value)
  return isUsableWxCode(text) ? text : ''
}

export function isLikelyJwtToken(value = '') {
  const text = normalizeText(value)
  if (!text) return false
  const parts = text.split('.')
  return parts.length === 3 && parts.every(Boolean) && JWT_FORMAT_RE.test(text)
}

function parseJwtPayload(value = '') {
  const text = normalizeText(value)
  if (!isLikelyJwtToken(text)) return null
  try {
    const payloadSegment = text.split('.')[1]
    const base64 = payloadSegment.replace(/-/g, '+').replace(/_/g, '/')
    const padded = `${base64}${'='.repeat((4 - (base64.length % 4 || 4)) % 4)}`
    return JSON.parse(Buffer.from(padded, 'base64').toString('utf-8'))
  } catch {
    return null
  }
}

export function getJwtExpiryMs(value = '') {
  const payload = parseJwtPayload(value)
  if (!payload || typeof payload !== 'object') return 0

  const expireTime = Number(payload.expireTime)
  if (Number.isFinite(expireTime) && expireTime > 0) {
    return expireTime > 1e12 ? expireTime : expireTime * 1000
  }

  const exp = Number(payload.exp)
  if (Number.isFinite(exp) && exp > 0) {
    return exp * 1000
  }

  return 0
}

export function isUsableTdxLoginToken(value = '', options = {}) {
  const text = normalizeText(value)
  const mode = options.mode || 'live'
  if (!text) return false
  if (/\s/u.test(text)) return false
  if (looksLikeFailureText(text)) return false
  if (mode === 'mock') return true
  if (text.toLowerCase().startsWith('tdx_')) return false
  if (!isLikelyJwtToken(text)) return false
  const expiryMs = getJwtExpiryMs(text)
  const minRemainingMs = Number.isFinite(Number(options.minRemainingMs)) ? Number(options.minRemainingMs) : 15000
  if (expiryMs && expiryMs <= Date.now() + minRemainingMs) {
    return false
  }
  return true
}

export function sanitizeStoredLoginToken(value = '', options = {}) {
  const text = normalizeText(value)
  return isUsableTdxLoginToken(text, options) ? text : ''
}

export function hasExplicitFailurePayload(payload = {}) {
  const text = [
    payload?.message,
    payload?.msg,
    payload?.error,
    payload?.data?.wxCode,
  ]
    .filter(Boolean)
    .join(' ')
  return looksLikeFailureText(text)
}
