import { randomUUID } from 'node:crypto'
import { createHash } from 'node:crypto'
import { ProxyAgent, fetch as undiciFetch } from 'undici'
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { getTdxConfig } from '../config/tdxConfig.js'
import { listConfigEntries } from '../data/businessEntityStore.js'
import { buildTdxExecutionPlan } from './tdxExecutionPlan.js'
import {
  clearSavedQmfSession,
  clearSavedDengfengSession,
  clearCurrentOrderSession,
  getNextCoolingOrderSession,
  getSavedDengfengSession,
  getSavedQmfSession,
  getSavedTdxLoginToken,
  getTdxRuntimeState,
  listReusableOrderSessions,
  listSavedTdxLoginTokens,
  markOrderSessionCooldown,
  markOrderUsed,
  rememberLastPayload,
  removeOrderSession,
  saveQmfSession,
  saveDengfengSession,
  saveOrderSession,
  saveTdxLoginToken,
  normalizeTdxLine,
} from './tdxRuntimeStore.js'
import {
  hasExplicitFailurePayload,
  isUsableTdxLoginToken,
  isUsableWxCode,
  sanitizeStoredWxCode,
} from './tdxSessionValue.js'

const MINI_APP_REFERER = 'https://servicewechat.com/wxf123c0cb425589de/7/page-frame.html'
const MINI_APP_USER_AGENT =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 MicroMessenger/7.0.20.1781(0x6700143B) NetType/WIFI MiniProgramEnv/Windows WindowsWechat/WMPF WindowsWechat(0x63090a13) UnifiedPCWindowsWechat(0xf254173b) XWEB/19027'
const DEFAULT_PROXY_SOURCE_URL = process.env.TDX_PROXY_SOURCE_URL || 'YOUR_TDX_PROXY_SOURCE_URL';
const TDX_PROXY_CACHE_MS = 55 * 1000
const TDX_HTTP_TIMEOUT_MS = 45000
const FALLBACK_ONE_PIXEL_JPEG = Buffer.from(
  '/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAkGBxAQEBAQEA8QDw8QDw8PEA8QDw8QDw8QFREWFhURFRUYHSggGBolGxUVITEhJSkrLi4uFx8zODMsNygtLisBCgoKDg0OGxAQGy0lICUtLS8tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLf/AABEIAAEAAQMBIgACEQEDEQH/xAAXAAADAQAAAAAAAAAAAAAAAAAAAQID/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAwDAQACEAMQAAAB6AD/xAAXEAEBAQEAAAAAAAAAAAAAAAABEQAh/9oACAEBAAEFAkLZr//EABQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQMBAT8BP//EABQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQIBAT8BP//EABgQAQEBAQEAAAAAAAAAAAAAAAERACEx/9oACAEBAAY/Ah2Kqf/EABoQAQADAQEBAAAAAAAAAAAAAAEAERAhMUH/2gAIAQEAAT8hQ0JKhA3lM4Kq1lJ7j//aAAwDAQACAAMAAAAQ8//EABQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQMBAT8QP//EABQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQIBAT8QP//EABoQAQEAAgMAAAAAAAAAAAAAAAERACExQWH/2gAIAQEAAT8QYShd3xW1uB0xkYYa9xJf/9k=',
  'base64',
)
const EVIDENCE_IMAGE_PATH = resolve(process.cwd(), 'data', 'tdx', 'kb.jpg')
const WECHAT_OPENID_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
const PRECHECK_DAILY_LIMIT_HINTS = [
  '今日申诉已达上',
  '今天申诉已达上',
  '每日同一用户申诉不得超过5次',
  '每天同一用户申诉不得超过5次',
  '申诉不得超过5次',
]
const ORDER_SESSION_ROTATE_HINTS = [
  'orderid',
  'order id',
  '订单',
  '二维码',
  'wxcode',
  'openid',
  '微信',
  'wechat',
]
const ORDER_SESSION_INVALID_HINTS = [
  'expired',
  'invalid',
  'not found',
  'closed',
  'cancelled',
  'over limit',
  'used up',
  'retry scan',
  '过期',
  '失效',
  '不存在',
  '无效',
  '关闭',
  '取消',
  '作废',
  '用完',
  '上限',
  '次数',
  '复扫',
  '重扫',
  '三次',
  '3次',
]
const QMF_TOKEN_HINTS = [
  'token',
  'usertoken',
  '登录',
  '登陆',
  'login',
  'auth',
  '会话',
  'session',
]
const QMF_TOKEN_INVALID_HINTS = [
  'expired',
  'invalid',
  'not found',
  'unauthorized',
  'forbidden',
  're-login',
  'login again',
  'please login',
  '过期',
  '失效',
  '不存在',
  '无效',
  '未登录',
  '重新登录',
  '重新登陆',
  '请先登录',
  '请重新登录',
]
const TDX_HARD_NON_RETRYABLE_HINTS = [
  '余额不足',
  'balance insufficient',
  'insufficient balance',
  '账号异常',
  '账户异常',
  '账号停用',
  '账户停用',
  '账号禁用',
  '账户禁用',
  '账号冻结',
  '账户冻结',
  '账号封禁',
  '账户封禁',
  '无权使用',
  '权限不足',
]
const TDX_SESSION_COOLDOWN_HINTS = [
  '1分钟内限制复扫一次',
  '限制复扫一次',
  '请稍后复扫',
  '稍后复扫',
  '频繁复扫',
  'rescan once per minute',
  'retry scan later',
  'rescan later',
  'scan again later',
  'scan too frequently',
  'cooldown',
]
const QMF_ORDER_COOLDOWN_HINTS = [
  '1分钟内限制复扫一次',
  '限制复扫一次',
  '请稍后复扫',
  '稍后复扫',
  '频繁复扫',
  'rescan once per minute',
  'retry scan later',
  'rescan later',
  'scan again later',
  'scan too frequently',
  'cooldown',
]
const QMF_ORDER_INVALID_HINTS = [
  '该号已暂停复扫',
  '暂停复扫',
  '授权失败',
  'expired',
  'invalid',
  'not found',
  'closed',
  'cancelled',
  'over limit',
  'used up',
  '过期',
  '失效',
  '不存在',
  '无效',
  '关闭',
  '取消',
  '作废',
  '用完',
  '上限',
  '次数',
  '三次',
  '3次',
]
const QMF_ORDER_COOLDOWN_MS = 75 * 1000

let qmfTokenRefreshPromise = null
let dengfengTokenRefreshPromise = null

function loadEvidenceImage() {
  try {
    if (existsSync(EVIDENCE_IMAGE_PATH)) {
      return readFileSync(EVIDENCE_IMAGE_PATH)
    }
  } catch {}
  return FALLBACK_ONE_PIXEL_JPEG
}

function buildSyntheticOpenId(seed) {
  const digest = createHash('sha256').update(`tdx-openid:${String(seed || '')}`).digest()
  let value = 'o'
  for (let index = 0; value.length < 28; index += 1) {
    value += WECHAT_OPENID_ALPHABET[digest[index % digest.length] % WECHAT_OPENID_ALPHABET.length]
  }
  return value
}

const EVIDENCE_IMAGE_JPEG = loadEvidenceImage()
const DEFAULT_QUESTION_ANSWERS = [
  { questionCode: '1-1', selectedOptionCode: '1-1-A', answerText: '本人使用手机号接听来电。' },
  { questionCode: '1-2', selectedOptionCode: '1-2-A', answerText: '近期频繁收到标记来电，影响正常通信。' },
  { questionCode: '1-3', selectedOptionCode: '1-3-A', answerText: '近5天内持续出现。' },
  { questionCode: '1-4', selectedOptionCode: '1-4-A', answerText: '每天约1次以上。' },
  { questionCode: '1-5', selectedOptionCode: '1-5-A', answerText: '该号码为本人实名号码，未从事营销行为。' },
  { questionCode: '1-6', selectedOptionCode: '1-6-A', answerText: '?' },
  { questionCode: '1-16', selectedOptionCode: '1-16-B', answerText: '仅供个人正常通信使用。' },
  { questionCode: '1-15', selectedOptionCode: '1-15-A', answerText: '请协助核实并尽快清理标记。' },
]

function asFormData(entries) {
  const form = new FormData()
  for (const [key, value] of Object.entries(entries)) {
    if (value !== undefined && value !== null && value !== '') {
      form.append(key, String(value))
    }
  }
  return form
}

function buildMiniAppHeaders(token, contentType = 'application/json') {
  const headers = {
    Accept: '*/*',
    'User-Agent': MINI_APP_USER_AGENT,
    xweb_xhr: '1',
    'Sec-Fetch-Site': 'cross-site',
    'Sec-Fetch-Mode': 'cors',
    'Sec-Fetch-Dest': 'empty',
    Referer: MINI_APP_REFERER,
    'Accept-Encoding': 'gzip, deflate, br',
    'Accept-Language': 'zh-CN,zh;q=0.9',
  }
  if (contentType) headers['Content-Type'] = contentType
  if (token) headers.Authorization = `Bearer ${token}`
  return headers
}


let cachedTdxProxy = { address: '', expiresAt: 0 }

function isDisabledEnv(value = '') {
  return /^(0|false|off|no|disabled)$/i.test(String(value || '').trim())
}

function shouldUseTdxProxy(url = '') {
  if (isDisabledEnv(process.env.TDX_PROXY_ENABLED)) return false
  const text = String(url || '')
  return /(^https?:\/\/[^/]*teddymobile\.cn\/)|(^https?:\/\/customer-apply-diversion\.teddymobile\.cn\/)/i.test(text)
}

function parseProxyAddress(text = '') {
  const match = String(text || '').match(/(\d{1,3}(?:\.\d{1,3}){3}:\d{2,5})/)
  return match ? match[1] : ''
}

function normalizeProxyUrl(proxy = '') {
  const value = String(proxy || '').trim()
  if (!value) return ''
  return /^https?:\/\//i.test(value) ? value : `http://${value}`
}

async function fetchWithOptionalProxy(url, { method = 'GET', headers = {}, body = null, proxy = '', timeoutMs = TDX_HTTP_TIMEOUT_MS } = {}) {
  const proxyUrl = normalizeProxyUrl(proxy)
  const dispatcher = proxyUrl ? new ProxyAgent(proxyUrl) : null
  try {
    const options = {
      method: String(method || 'GET').toUpperCase(),
      headers,
      signal: AbortSignal.timeout(timeoutMs),
    }
    if (body !== null && body !== undefined) options.body = body
    if (dispatcher) options.dispatcher = dispatcher

    const response = await undiciFetch(url, options)
    const buffer = Buffer.from(await response.arrayBuffer())
    const contentType = response.headers.get('content-type') || ''
    return {
      status: response.status,
      contentType,
      buffer,
      text: decodeBufferWithFallback(buffer, parseResponseCharset(contentType)),
    }
  } finally {
    if (dispatcher) await dispatcher.close().catch(() => {})
  }
}

function parseResponseCharset(contentType = '') {
  const match = String(contentType || '').match(/charset\s*=\s*["']?([^;"'\s]+)/i)
  return match ? String(match[1] || '').trim().toLowerCase() : ''
}

function normalizeDecoderLabel(charset = '') {
  const value = String(charset || '').trim().toLowerCase()
  if (!value) return ''
  if (value === 'utf8') return 'utf-8'
  if (value === 'gb2312' || value === 'gb_2312-80') return 'gb18030'
  return value
}

function decodeBufferByCharset(buffer, charset = '') {
  if (!buffer || !buffer.length) return ''
  const label = normalizeDecoderLabel(charset) || 'utf-8'
  try {
    return new TextDecoder(label).decode(buffer)
  } catch {
    return null
  }
}

function getDecodeCharsetCandidates(preferredCharset = '') {
  const candidates = [preferredCharset, 'utf-8', 'gb18030', 'gbk']
  return [...new Set(candidates.map((item) => normalizeDecoderLabel(item)).filter(Boolean))]
}

function looksLikeMojibake(text = '') {
  const value = String(text || '')
  if (!value) return false
  return (
    value.includes('\uFFFD') ||
    /(?:鍚|璇|鐢|鏆|鏃|鏉|寮|淇|鎻|浜|褰|鍙|閿|璁|澶|宸|鐧|绯|绔|绶|鎴|鏍|閫|娉|勬|锛|锟)/.test(value)
  )
}

function decodeBufferWithFallback(buffer, preferredCharset = '') {
  const candidates = getDecodeCharsetCandidates(preferredCharset)
  let fallback = ''
  for (const charset of candidates) {
    const text = decodeBufferByCharset(buffer, charset)
    if (text === null) continue
    if (!fallback) fallback = text
    if (!looksLikeMojibake(text)) return text
  }
  return fallback
}

function parseJsonFromBuffer(buffer, { status = 0, contentType = '' } = {}) {
  const preferredCharset = parseResponseCharset(contentType)
  const candidates = getDecodeCharsetCandidates(preferredCharset)
  let fallbackText = ''

  for (const charset of candidates) {
    const text = decodeBufferByCharset(buffer, charset)
    if (text === null) continue
    if (!fallbackText) fallbackText = text
    try {
      const payload = text ? JSON.parse(text) : null
      if (!looksLikeMojibake(JSON.stringify(payload ?? ''))) {
        if (payload && typeof payload === 'object') payload._httpStatus = status
        return payload
      }
      if (!fallbackText || looksLikeMojibake(fallbackText)) fallbackText = text
    } catch {
      if (!fallbackText || looksLikeMojibake(fallbackText)) fallbackText = text
    }
  }

  let payload = null
  try {
    payload = fallbackText ? JSON.parse(fallbackText) : null
  } catch {
    payload = { rawText: fallbackText }
  }
  if (payload && typeof payload === 'object') {
    payload._httpStatus = status
  }
  return payload
}

async function resolveConfigValue(keys = [], fallback = '') {
  const envValue = keys
    .map((key) => process.env[key.toUpperCase().replace(/[^A-Z0-9]+/g, '_')])
    .find((value) => String(value || '').trim())
  if (envValue) return String(envValue).trim()

  try {
    const entries = await listConfigEntries()
    for (const key of keys) {
      const match = entries.find((item) => String(item.codekey || '').trim() === key && Number(item.qiyong ?? 1) !== 0)
      if (match && String(match.codevalue || '').trim()) return String(match.codevalue).trim()
    }
  } catch {
    // ignore and fallback
  }
  return fallback
}

async function getTdxProxyAddress({ force = false } = {}) {
  const direct = await resolveConfigValue(['tdx_proxy_url'], '')
  if (direct && /^\d{1,3}(?:\.\d{1,3}){3}:\d{2,5}$/.test(direct)) return direct

  const now = Date.now()
  if (!force && cachedTdxProxy.address && cachedTdxProxy.expiresAt > now) return cachedTdxProxy.address

  let sourceUrl = await resolveConfigValue(
    ['tdx_proxy_source_url', 'xiaomi_proxy_source_url', 'proxy_source_url'],
    DEFAULT_PROXY_SOURCE_URL,
  )
  if (!sourceUrl) throw new Error('TDX proxy source URL is not configured')
  if (/api3\.ydaili\.cn/i.test(sourceUrl) && /[?&]number=\d+/i.test(sourceUrl)) {
    sourceUrl = sourceUrl.replace(/([?&]number=)\d+/i, (match, prefix) => `${prefix}1`)
  }

  const { text } = await fetchWithOptionalProxy(sourceUrl, { timeoutMs: 25000 })
  const proxy = parseProxyAddress(text)
  if (!proxy) {
    throw new Error(`TDX proxy source did not return IP:PORT: ${text.trim().slice(0, 200)}`)
  }
  cachedTdxProxy = { address: proxy, expiresAt: now + TDX_PROXY_CACHE_MS }
  return proxy
}

async function nodeHttpRequest({ url, method = 'POST', headers = {}, body = null, proxy = '', timeoutMs = TDX_HTTP_TIMEOUT_MS }) {
  const { status, contentType, buffer, text: responseBody } = await fetchWithOptionalProxy(url, {
    method,
    headers,
    body,
    proxy,
    timeoutMs,
  })
  const payload = parseJsonFromBuffer(buffer, { status, contentType })
  if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'rawText') && !payload.rawText) {
    payload.rawText = responseBody
  }
  if (proxy && (!status || status < 200 || status >= 300)) {
    throw new Error(`TDX proxy returned HTTP ${status || 'unknown'}: ${responseBody.slice(0, 200)}`)
  }
  if (proxy && payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'rawText')) {
    throw new Error(`TDX proxy returned non-JSON response: ${String(payload.rawText || '').slice(0, 200)}`)
  }
  return payload
}

async function postViaTdxProxyIfNeeded(url, request) {
  if (!shouldUseTdxProxy(url)) return null
  let lastError = null
  for (let attempt = 0; attempt < 3; attempt += 1) {
    const proxy = await getTdxProxyAddress({ force: attempt > 0 })
    try {
      return await nodeHttpRequest({ ...request, url, proxy })
    } catch (error) {
      const endpoint = (() => {
        try {
          const parsed = new URL(url)
          return `${parsed.hostname}${parsed.pathname}`
        } catch {
          return String(url || 'TDX')
        }
      })()
      // TDX 要求走代理；只要没有拿到正确响应，都按当前代理不可用处理，换下一个代理。
      lastError = new Error(`[${endpoint}] ${error?.message || String(error)}`)
      cachedTdxProxy = { address: '', expiresAt: 0 }
    }
  }
  throw lastError || new Error('TDX proxy request failed')
}

async function parseResponse(response) {
  const buffer = Buffer.from(await response.arrayBuffer())
  return parseJsonFromBuffer(buffer, {
    status: response.status,
    contentType: response.headers.get('content-type') || '',
  })
}

async function postJson(url, payload, options = {}) {
  const headers = options.headers || { 'Content-Type': 'application/json' }
  const body = JSON.stringify(payload)
  const proxied = await postViaTdxProxyIfNeeded(url, { method: 'POST', headers, body })
  if (proxied) return proxied

  const response = await fetch(url, {
    method: 'POST',
    headers,
    body,
  })
  return parseResponse(response)
}

function parseCheckAppletPhonePrecheck(payload) {
  const body = payload?.body && typeof payload.body === 'object' ? payload.body : payload
  const isSuccess = body?.isSuccess
  const message = String(body?.message || payload?.message || payload?.msg || '').trim()

  if (isSameDayRepeatAppealAllowedMessage(message)) {
    return {
      ok: true,
      allowed: true,
      status: '可继续处理',
      message: message || '号码当天已提交过申诉，可继续处理。',
      raw: payload,
    }
  }

  if (isSuccess === true) {
    return {
      ok: true,
      allowed: true,
      status: '已标记，可申诉',
      message: message || '号码已标记，可继续处理。',
      raw: payload,
    }
  }

  if (isSuccess === false) {
    return {
      ok: true,
      allowed: false,
      status: '未标记，不可申诉',
      message: message || '号码未标记，不可继续处理。',
      raw: payload,
    }
  }

  return {
    ok: false,
    allowed: false,
    status: '检查失败',
    message: message || '号码预检查失败，请稍后重试。',
    raw: payload,
  }
}

function isPrecheckDailyLimitMessage(message = '') {
  const text = String(message || '').trim()
  if (!text) return false
  return PRECHECK_DAILY_LIMIT_HINTS.some((item) => text.includes(item))
}

function isSameDayRepeatAppealAllowedMessage(message = '') {
  const text = String(message || '').trim()
  if (!text) return false
  return /当天已提交过申诉|今天已提交过申诉|今日已提交过申诉|不可重复提交/.test(text)
}

async function postForm(url, payload) {
  const response = await fetch(url, {
    method: 'POST',
    body: asFormData(payload),
  })
  return parseResponse(response)
}

async function postUrlEncoded(url, payload) {
  const body = new URLSearchParams()
  for (const [key, value] of Object.entries(payload)) {
    if (value !== undefined && value !== null && value !== '') {
      body.set(key, String(value))
    }
  }
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString(),
  })
  return parseResponse(response)
}

async function postFormWithHeaders(url, payload, headers = {}) {
  const response = await fetch(url, {
    method: 'POST',
    headers,
    body: asFormData(payload),
  })
  return parseResponse(response)
}

async function getJsonWithHeaders(url, headers = {}) {
  const response = await fetch(url, {
    method: 'GET',
    headers,
  })
  return parseResponse(response)
}

async function postMiniAppJson(url, payload, token) {
  const headers = buildMiniAppHeaders(token, 'application/json')
  const body = JSON.stringify(payload)
  const proxied = await postViaTdxProxyIfNeeded(url, { method: 'POST', headers, body })
  if (proxied) return proxied

  const response = await fetch(url, {
    method: 'POST',
    headers,
    body,
  })
  return parseResponse(response)
}

async function postTeddyPhoneCheck(phone) {
  const url = 'https://www.teddymobile.cn/api/phone/check'
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/json, text/plain, */*',
    'User-Agent': MINI_APP_USER_AGENT,
    Origin: 'https://www.teddymobile.cn',
    Referer: 'https://www.teddymobile.cn/numberComplain',
  }
  const body = JSON.stringify({ phone })
  const proxied = await postViaTdxProxyIfNeeded(url, { method: 'POST', headers, body })
  if (proxied) return proxied

  const response = await fetch(url, {
    method: 'POST',
    headers,
    body,
  })
  return parseResponse(response)
}

function parseTeddyPhoneCheckPrecheck(payload) {
  const code = payload?.code
  const message = String(payload?.message || payload?.msg || '').trim()

  if (isSameDayRepeatAppealAllowedMessage(message)) {
    return {
      ok: true,
      allowed: true,
      status: '可继续处理',
      message: message || '号码当天已提交过申诉，可继续处理。',
      raw: payload,
    }
  }

  if (code === 200 && (!message || message === 'success')) {
    return {
      ok: true,
      allowed: true,
      status: '已标记，可申诉',
      message: '号码已标记，可继续处理。',
      raw: payload,
    }
  }

  return {
    ok: true,
    allowed: false,
    status: '不可申诉',
    message: message || '号码暂不符合申诉条件。',
    raw: payload,
  }
}

async function resolvePrecheckTokenCandidates(phone, config) {
  const candidates = []
  const seen = new Set()

  const addCandidate = (item, source = 'rotation') => {
    const token = isUsableTdxLoginToken(item?.token, { mode: config.mode }) ? String(item?.token || '').trim() : ''
    const authPhone = String(item?.phone || phone || '').trim()
    if (!token || !authPhone) return
    const uniqueKey = `${authPhone}\u0000${token}`
    if (seen.has(uniqueKey)) return
    seen.add(uniqueKey)
    candidates.push({
      token,
      source,
      phone: authPhone,
      orderId: String(item?.orderId || '').trim(),
      wxCode: String(item?.wxCode || '').trim(),
      savedAt: String(item?.savedAt || '').trim(),
      updatedAt: String(item?.updatedAt || '').trim(),
    })
  }

  const savedLogin = await getSavedTdxLoginToken(phone)
  if (savedLogin) {
    addCandidate(
      {
        phone,
        ...savedLogin,
      },
      'phone',
    )
  }

  let recentLogins = []
  try {
    recentLogins = await listSavedTdxLoginTokens(20)
  } catch {
    recentLogins = []
  }
  for (const item of recentLogins) {
    const source = candidates.length ? 'rotation' : 'latest'
    addCandidate(item, source)
  }

  if (candidates.length) {
    return candidates
  }

  throw new Error('当前暂无可用授权会话，请先完成一笔短信登录后再试。')
}

function createMiniAppEvidenceForm(sessionId) {
  const form = new FormData()
  form.append('sessionId', String(sessionId))
  form.append('selectedOptionCode', '1-10-A')
  form.append('questionCode', '1-10')
  form.append('files', new Blob([EVIDENCE_IMAGE_JPEG], { type: 'image/jpeg' }), 'kb.jpg')
  return form
}

async function uploadMiniAppEvidence(url, sessionId, token) {
  const headers = buildMiniAppHeaders(token, null)
  const proxied = await postViaTdxProxyIfNeeded(url, {
    method: 'POST',
    headers,
    body: createMiniAppEvidenceForm(sessionId),
  })
  if (proxied) return proxied

  const response = await fetch(url, {
    method: 'POST',
    headers,
    body: createMiniAppEvidenceForm(sessionId),
  })
  return parseResponse(response)
}

function createMockOrderSession(existingOrderId = '', line = 'line1') {
  const suffix = randomUUID().replace(/-/g, '').slice(0, 18)
  const normalizedLine = normalizeTdxLine(line)
  const prefix = normalizedLine === 'line2' ? 'DF' : 'QMF'
  const orderId = existingOrderId || `${prefix}-${suffix}`
  const wxCode = `wx_${normalizedLine}_${suffix}`
  return {
    line: normalizedLine,
    orderId,
    wxCode,
    raw: {
      flag: true,
      code: 20000,
      message: 'mock auth success',
      data: {
        orderId,
        wxCode,
        line: normalizedLine,
      },
    },
  }
}

function normalizeSessionId(value) {
  if (typeof value === 'number') return value
  if (typeof value === 'string' && /^\d+$/.test(value)) return Number(value)
  return value
}

function sameDate(a, b) {
  if (!a || !b) return false
  return String(a).slice(0, 10) === String(b).slice(0, 10)
}

function looksExplicitFailure(payload) {
  return hasExplicitFailurePayload(payload)
}

function isStepSuccess(payload) {
  if (!payload) return false
  if (payload.flag === false) return false
  if (payload.success === false) return false
  if (payload.isSuccess === false) return false
  if (looksExplicitFailure(payload)) return false
  if (payload.code === 200 || payload.code === '200' || payload.code === 20000 || payload.code === '20000') return true
  if (payload.status === 200 || payload.status === 'ok') return true
  if (payload.success === true || payload.flag === true || payload.isSuccess === true) return true
  if (payload._httpStatus && payload._httpStatus >= 200 && payload._httpStatus < 300 && payload.code === undefined) return true
  return false
}

function buildStepState(task, config, statuses, notes = {}) {
  const plan = buildTdxExecutionPlan(task, config)
  return plan.map((step) => ({
    ...step,
    status: statuses[step.key] || 'pending',
    note: notes[step.key] || '',
    statusLabel: {
      completed: 'Completed',
      active: 'Running',
      ready: 'Ready',
      blocked: 'Blocked',
      pending: 'Pending',
    }[statuses[step.key] || 'pending'],
  }))
}

function extractUsableQmfWxCode(value = '') {
  const raw = String(value || '').trim()
  if (!raw) return ''
  const direct = sanitizeStoredWxCode(raw)
  if (direct) return direct
  const matched = raw.match(/(?:微信|wxcode|openId|openid)[:：=\s]+([A-Za-z0-9_-]{12,255})/i)
  if (matched?.[1]) {
    return sanitizeStoredWxCode(matched[1])
  }
  return ''
}

function validateLiveSessionResponse(payload) {
  const orderId = extractQmfOrderIdFromPayload(payload)
  const rawWxCode = payload?.data?.wxCode || ''
  const wxCode = extractUsableQmfWxCode(rawWxCode)
  const message = String(payload?.message || payload?.msg || '').trim()
  return {
    orderId,
    wxCode,
    rawWxCode,
    message,
    invalid: !orderId || !wxCode || looksExplicitFailure({ message, data: { wxCode: rawWxCode } }),
  }
}

function isQmfWxCodeSuccessPayload(payload) {
  const code = String(payload?.code ?? '').trim()
  return payload?.flag !== false && code === '20000'
}

function collectQmfPayloadText(payload) {
  return [
    payload?.message,
    payload?.msg,
    payload?.error,
    payload?.description,
    payload?.callback,
    payload?.data?.message,
    payload?.data?.msg,
    payload?.data?.wxCode,
    payload?.data?.callback,
    payload?.description?.message,
    payload?.description?.callback,
    payload?.description?.data?.message,
    payload?.description?.data?.callback,
    payload?.description?.data?.wxCode,
  ]
    .map((item) => String(item || '').trim())
    .filter(Boolean)
    .join(' ')
}

export function isHardTdxSessionFailureMessage(message = '') {
  const text = String(message || '').trim()
  if (!text) return false
  const lowered = text.toLowerCase()
  return TDX_HARD_NON_RETRYABLE_HINTS.some((hint) => lowered.includes(String(hint).toLowerCase()))
}

export function isTdxSessionCooldownMessage(message = '') {
  const text = String(message || '').trim()
  if (!text) return false
  if (/该号已暂停复扫|暂停复扫/i.test(text)) {
    return false
  }
  const lowered = text.toLowerCase()
  if (TDX_SESSION_COOLDOWN_HINTS.some((hint) => lowered.includes(String(hint).toLowerCase()))) {
    return true
  }
  return /(?:1\s*分钟.*复扫|请稍后复扫|稍后复扫|限制复扫一次)/i.test(text)
}

function isQmfOrderCooldownMessage(message = '') {
  const text = String(message || '').trim()
  if (!text) return false
  const lowered = text.toLowerCase()
  if (QMF_ORDER_COOLDOWN_HINTS.some((hint) => lowered.includes(String(hint).toLowerCase()))) {
    return true
  }
  return /(?:1\s*分钟.*复扫|请稍后复扫|稍后复扫|限制复扫一次)/i.test(text)
}

function isQmfOrderInvalidMessage(message = '') {
  const text = String(message || '').trim()
  if (!text) return false
  if (/该号已暂停复扫|授权失败/i.test(text)) {
    return true
  }
  if (isQmfOrderCooldownMessage(text)) {
    return false
  }
  const lowered = text.toLowerCase()
  return QMF_ORDER_INVALID_HINTS.some((hint) => lowered.includes(String(hint).toLowerCase()))
}

function resolveQmfWxCodeFailureMessage(payload) {
  const message = [
    payload?.message,
    payload?.msg,
    payload?.error,
    payload?.description,
    payload?.callback,
    payload?.data?.message,
    payload?.data?.msg,
    payload?.data?.wxCode,
    payload?.data?.callback,
    payload?.description?.message,
    payload?.description?.callback,
    payload?.description?.data?.message,
    payload?.description?.data?.callback,
    payload?.description?.data?.wxCode,
  ]
    .map((item) => String(item || '').trim())
    .find(Boolean)
  return message || 'Unable to acquire wxCode'
}

function createQmfWxCodeError(message, status = 'unknown', orderId = '', payload = null) {
  const error = new Error(String(message || 'Unable to acquire wxCode').trim() || 'Unable to acquire wxCode')
  error.qmfOrderStatus = String(status || 'unknown').trim() || 'unknown'
  error.qmfOrderId = String(orderId || '').trim()
  error.qmfPayload = payload || null
  const payloadCooldownMs = Number(payload?.cooldownMs || payload?.data?.cooldownMs || payload?.description?.data?.cooldownMs || 0)
  if (Number.isFinite(payloadCooldownMs) && payloadCooldownMs > 0) {
    error.cooldownMs = payloadCooldownMs
  } else if (error.qmfOrderStatus === 'cooldown') {
    error.cooldownMs = QMF_ORDER_COOLDOWN_MS
  }
  return error
}

function extractQmfOrderIdFromPayload(payload = null) {
  return [
    payload?.data?.orderId,
    payload?.data?.orderID,
    payload?.orderId,
    payload?.orderID,
    payload?.description?.data?.orderId,
    payload?.description?.data?.orderID,
  ]
    .map((item) => String(item || '').trim())
    .find(Boolean) || ''
}

function extractQmfOrderIdFromError(error = null) {
  return String(error?.qmfOrderId || extractQmfOrderIdFromPayload(error?.qmfPayload) || '').trim()
}

function classifyQmfWxCodeFailure(payload, orderId = '') {
  const message = resolveQmfWxCodeFailureMessage(payload)
  if (isQmfOrderCooldownMessage(message)) {
    return {
      status: 'cooldown',
      orderId: String(orderId || '').trim(),
      message,
      cooldownMs: QMF_ORDER_COOLDOWN_MS,
      payload,
    }
  }
  if (isQmfOrderInvalidMessage(message)) {
    return {
      status: 'invalid',
      orderId: String(orderId || '').trim(),
      message,
      cooldownMs: 0,
      payload,
    }
  }
  return {
    status: 'unknown',
    orderId: String(orderId || '').trim(),
    message,
    cooldownMs: 0,
    payload,
  }
}

function shouldRefreshQmfToken(payload) {
  const text = collectQmfPayloadText(payload)
  if (!text) return false
  if (/余额不足|balance insufficient|insufficient balance/i.test(text)) return false
  if (/超时|timeout|timed out|temporar|网络|异常|busy|繁忙|rate limit/i.test(text)) return false
  if (/未登录|重新登录|重新登陆|请先登录|请重新登录|login again|re-login|token expired|token invalid/i.test(text)) return true

  const lowered = text.toLowerCase()
  const hasTokenHint = QMF_TOKEN_HINTS.some((hint) => lowered.includes(hint))
  const hasInvalidHint = QMF_TOKEN_INVALID_HINTS.some((hint) => lowered.includes(hint))
  return hasTokenHint && hasInvalidHint
}

export function validateLiveTdxConfig(config, { requireAppealChannel = true } = {}) {
  if (config.mode !== 'live') return []
  const problems = []
  const sharedRequired = [
    ['originalIp', 'originalIp is required in live mode'],
    ['sendSmsUrl', 'sendSmsUrl is required in live mode'],
    ['loginSmsUrl', 'loginSmsUrl is required in live mode'],
    ['answersStartUrl', 'answersStartUrl is required in live mode'],
    ['checkAppletPhoneUrl', 'checkAppletPhoneUrl is required in live mode'],
    ['answersSubmitUrl', 'answersSubmitUrl is required in live mode'],
    ['queryProgressUrl', 'queryProgressUrl is required in live mode'],
  ]
  for (const [key, message] of sharedRequired) {
    if (!config?.[key]) problems.push(message)
  }

  if (!requireAppealChannel) return problems

  const line1Problems = []
  if (!config.qmfUsername) line1Problems.push('线路1(QMF) username is required in live mode')
  if (!config.qmfPassword) line1Problems.push('线路1(QMF) password is required in live mode')
  if (!config.qmfAppId) line1Problems.push('线路1(QMF) appId is required in live mode')
  if (!config.qmfLoginUrl) line1Problems.push('线路1(QMF) login url is required in live mode')
  if (!config.qmfCodeUrl) line1Problems.push('线路1(QMF) code url is required in live mode')

  const line2Problems = []
  if (!config.dfUsername) line2Problems.push('线路2(登峰) username is required in live mode')
  if (!config.dfPassword) line2Problems.push('线路2(登峰) password is required in live mode')
  if (!config.dfLoginUrl) line2Problems.push('线路2(登峰) login url is required in live mode')
  if (!config.dfCreateOrderUrl) line2Problems.push('线路2(登峰) create-order url is required in live mode')
  if (!config.dfTwoAuthUrl) line2Problems.push('线路2(登峰) twoauth url is required in live mode')
  if (!config.dfOrderViewUrl) line2Problems.push('线路2(登峰) order-view url is required in live mode')
  if (!config.dfProjectId) line2Problems.push('线路2(登峰) projectId is required in live mode')

  if (line1Problems.length && line2Problems.length) {
    problems.push(...line1Problems, ...line2Problems)
  }
  return problems
}

async function ensureTdxConfig(options = {}) {
  const config = await getTdxConfig()
  if (config.mode === 'live') {
    const problems = validateLiveTdxConfig(config, options)
    if (problems.length) {
      throw new Error(`TDX live mode is not ready: ${problems.join('; ')}`)
    }
  }
  return config
}

export async function sendTdxSmsCode(phone, line = 'line1') {
  const config = await ensureTdxConfig({ requireAppealChannel: false })
  const normalizedLine = normalizeTdxLine(line)
  if (config.mode === 'mock') {
    return {
      ok: false,
      mode: 'mock',
      line: normalizedLine,
      payload: { code: 503, message: '当前为模拟模式，未发送真实短信，请切换到实盘模式。' },
    }
  }

  const payload = await postJson(config.sendSmsUrl, { phone, bizType: 'LOGIN' }, { headers: buildMiniAppHeaders('', 'application/json') })
  rememberLastPayload({ type: 'sendSms', payload })
  return {
    ok: payload?.code === 200 || /success/i.test(payload?.message || ''),
    mode: 'live',
    line: normalizedLine,
    payload,
  }
}

async function fetchQmfToken(config) {
  return fetchQmfTokenWithCache(config)
}

async function fetchQmfTokenWithCache(config, { forceRefresh = false } = {}) {
  if (config.mode === 'mock') return 'mock-user-token'

  if (!forceRefresh) {
    const saved = getSavedQmfSession()
    const savedToken = String(saved?.token || '').trim()
    if (savedToken) {
      return savedToken
    }
    if (qmfTokenRefreshPromise) {
      return qmfTokenRefreshPromise
    }
  }

  if (qmfTokenRefreshPromise) {
    return qmfTokenRefreshPromise
  }

  qmfTokenRefreshPromise = (async () => {
    const response = await postUrlEncoded(config.qmfLoginUrl, {
      username: config.qmfUsername,
      password: config.qmfPassword,
    })
    rememberLastPayload({ type: 'qmfLogin', payload: response })
    const token = String(response?.data?.usertoken || response?.data?.token || response?.token || '').trim()
    if (token) {
      saveQmfSession({ token, source: 'qmfLogin' })
      return token
    }
    if (forceRefresh) {
      clearSavedQmfSession()
    }
    return ''
  })().finally(() => {
    qmfTokenRefreshPromise = null
  })

  return qmfTokenRefreshPromise
}

function extractDengfengToken(response = null) {
  return [
    response?.data?.token,
    response?.data?.access_token,
    response?.data?.authorization,
    response?.token,
    response?.access_token,
    response?.authorization,
  ]
    .map((item) => String(item || '').trim())
    .find(Boolean) || ''
}

function collectDengfengPayloadText(payload = null) {
  return [
    payload?.message,
    payload?.msg,
    payload?.error,
    payload?.description,
    payload?.data?.message,
    payload?.data?.msg,
    payload?.data?.error,
    payload?.data?.remark,
    payload?.remark,
  ]
    .map((item) => String(item || '').trim())
    .filter(Boolean)
    .join(' ')
}

function shouldRefreshDengfengToken(payload = null) {
  const text = collectDengfengPayloadText(payload)
  if (!text) return false
  return /token|授权|auth|登录|登陆|失效|过期|未登录|请先登录|重新登录|unauthorized|forbidden|expired|invalid/i.test(text)
}

async function fetchDengfengToken(config) {
  return fetchDengfengTokenWithCache(config)
}

async function fetchDengfengTokenWithCache(config, { forceRefresh = false } = {}) {
  if (config.mode === 'mock') return 'mock-dengfeng-token'

  if (!forceRefresh) {
    const saved = getSavedDengfengSession()
    const savedToken = String(saved?.token || '').trim()
    if (savedToken) {
      return savedToken
    }
    if (dengfengTokenRefreshPromise) {
      return dengfengTokenRefreshPromise
    }
  }

  if (dengfengTokenRefreshPromise) {
    return dengfengTokenRefreshPromise
  }

  dengfengTokenRefreshPromise = (async () => {
    const response = await postUrlEncoded(config.dfLoginUrl, {
      member_user: config.dfUsername,
      member_pwd: config.dfPassword,
    })
    rememberLastPayload({ type: 'dfLogin', payload: response })
    const token = extractDengfengToken(response)
    if (token) {
      saveDengfengSession({ token, source: 'dfLogin' })
      return token
    }
    if (forceRefresh) {
      clearSavedDengfengSession()
    }
    return ''
  })().finally(() => {
    dengfengTokenRefreshPromise = null
  })

  return dengfengTokenRefreshPromise
}

function extractDengfengOrderId(response = null) {
  return [
    response?.data?.o_id,
    response?.data?.order_id,
    response?.data?.orderId,
    response?.o_id,
    response?.order_id,
    response?.orderId,
  ]
    .map((item) => String(item || '').trim())
    .find(Boolean) || ''
}

function extractDengfengWxCode(response = null) {
  return [
    response?.data?.wxid,
    response?.data?.wxCode,
    response?.data?.code,
    response?.data?.openid,
    response?.wxid,
    response?.wxCode,
    response?.code,
    response?.openid,
  ]
    .map((item) => sanitizeStoredWxCode(item))
    .find(Boolean) || ''
}

async function requestQmfWxCode(config, token, orderId = '') {
  const response = await postForm(config.qmfCodeUrl, {
    token,
    appId: config.qmfAppId,
    qrCode: config.qmfQrCode,
    orderId,
  })
  rememberLastPayload({ type: 'qmfCode', payload: response })
  return response
}

async function fetchWxCodeInternal(config, orderId = '', { line = 'line1', allowTokenRefresh = true } = {}) {
  const normalizedLine = normalizeTdxLine(line)
  if (config.mode === 'mock') {
    return createMockOrderSession(orderId, normalizedLine)
  }

  if (normalizedLine === 'line2') {
    const token = await fetchDengfengToken(config)
    if (!token) {
      throw new Error('Unable to acquire Dengfeng token')
    }

    const normalizedOrderId = String(orderId || '').trim()
    let createOrderResponse = null
    if (!normalizedOrderId) {
      createOrderResponse = await postForm(config.dfCreateOrderUrl, {
        projectid: config.dfProjectId,
        type: config.dfOrderType || '1',
      })
      rememberLastPayload({ type: 'dfCreateOrder', payload: createOrderResponse })
    }
    const resolvedOrderId = String(normalizedOrderId || extractDengfengOrderId(createOrderResponse)).trim()
    if (!resolvedOrderId) {
      throw new Error(collectDengfengPayloadText(createOrderResponse) || 'Unable to create Dengfeng order')
    }

    let authToken = token
    let twoAuthResponse = await postFormWithHeaders(
      config.dfTwoAuthUrl,
      { o_id: resolvedOrderId },
      { Authorization: authToken },
    )
    rememberLastPayload({ type: 'dfTwoAuth', payload: twoAuthResponse })
    if (shouldRefreshDengfengToken(twoAuthResponse) && allowTokenRefresh) {
      clearSavedDengfengSession()
      authToken = await fetchDengfengTokenWithCache(config, { forceRefresh: true })
      if (!authToken) {
        throw new Error('Unable to refresh Dengfeng token')
      }
      twoAuthResponse = await postFormWithHeaders(
        config.dfTwoAuthUrl,
        { o_id: resolvedOrderId },
        { Authorization: authToken },
      )
      rememberLastPayload({ type: 'dfTwoAuth', payload: twoAuthResponse })
    }

    const viewUrl = `${config.dfOrderViewUrl}?o_id=${encodeURIComponent(resolvedOrderId)}`
    let orderViewResponse = await getJsonWithHeaders(viewUrl, { Authorization: authToken })
    rememberLastPayload({ type: 'dfOrderView', payload: orderViewResponse })
    if (shouldRefreshDengfengToken(orderViewResponse) && allowTokenRefresh) {
      clearSavedDengfengSession()
      authToken = await fetchDengfengTokenWithCache(config, { forceRefresh: true })
      if (!authToken) {
        throw new Error('Unable to refresh Dengfeng token')
      }
      orderViewResponse = await getJsonWithHeaders(viewUrl, { Authorization: authToken })
      rememberLastPayload({ type: 'dfOrderView', payload: orderViewResponse })
    }

    const wxCode = extractDengfengWxCode(orderViewResponse)
    if (!wxCode) {
      throw new Error(collectDengfengPayloadText(orderViewResponse) || 'Unable to extract Dengfeng wxCode')
    }

    return {
      line: normalizedLine,
      orderId: resolvedOrderId,
      wxCode,
      raw: {
        createOrder: createOrderResponse,
        twoAuth: twoAuthResponse,
        orderView: orderViewResponse,
      },
    }
  }

  const normalizedOrderId = String(orderId || '').trim()
  const token = await fetchQmfToken(config)
  if (!token) {
    throw createQmfWxCodeError('Unable to acquire QMF token', 'unknown', normalizedOrderId)
  }

  const response = await requestQmfWxCode(config, token, normalizedOrderId)
  if (shouldRefreshQmfToken(response) && allowTokenRefresh) {
    clearSavedQmfSession()
    const refreshedToken = await fetchQmfTokenWithCache(config, { forceRefresh: true })
    if (!refreshedToken) {
      throw createQmfWxCodeError('Unable to refresh QMF token', 'unknown', normalizedOrderId, response)
    }
    return fetchWxCodeInternal(config, normalizedOrderId, { line: normalizedLine, allowTokenRefresh: false })
  }

  if (!isQmfWxCodeSuccessPayload(response)) {
    const resolvedOrderId = normalizedOrderId || extractQmfOrderIdFromPayload(response)
    const failure = classifyQmfWxCodeFailure(response, resolvedOrderId)
    if (failure.status === 'cooldown' && resolvedOrderId) {
      markOrderSessionCooldown(resolvedOrderId, { message: failure.message, cooldownMs: failure.cooldownMs })
    } else if (failure.status === 'invalid' && resolvedOrderId) {
      removeOrderSession(resolvedOrderId)
    } else if (resolvedOrderId) {
      saveOrderSession({
        orderId: resolvedOrderId,
        wxCode: '',
        status: 'active',
        lastFailure: failure.message,
      })
    }
    const error = createQmfWxCodeError(failure.message, failure.status, resolvedOrderId, response)
    if (Number(failure.cooldownMs || 0) > 0) {
      error.cooldownMs = Number(failure.cooldownMs)
    }
    throw error
  }

  const resolved = validateLiveSessionResponse(response)
  if (resolved.invalid) {
    const resolvedOrderId = normalizedOrderId || resolved.orderId
    const failure = classifyQmfWxCodeFailure(response, resolvedOrderId)
    if (failure.status === 'cooldown' && resolvedOrderId) {
      markOrderSessionCooldown(resolvedOrderId, { message: failure.message, cooldownMs: failure.cooldownMs })
    } else if (failure.status === 'invalid' && resolvedOrderId) {
      removeOrderSession(resolvedOrderId)
    } else if (resolvedOrderId) {
      saveOrderSession({
        orderId: resolvedOrderId,
        wxCode: '',
        status: 'active',
        lastFailure: resolved.message || resolved.rawWxCode || failure.message,
      })
    }
    const error = createQmfWxCodeError(
      resolved.message || resolved.rawWxCode || failure.message,
      failure.status,
      resolvedOrderId,
      response,
    )
    if (Number(failure.cooldownMs || 0) > 0) {
      error.cooldownMs = Number(failure.cooldownMs)
    }
    throw error
  }

  return {
    line: normalizedLine,
    orderId: resolved.orderId,
    wxCode: resolved.wxCode,
    raw: response,
  }
}

export async function allocateTdxOrderSession({ forceRotate = false, allowNewOrder = true, line = 'line1' } = {}) {
  const config = await ensureTdxConfig()
  const normalizedLine = normalizeTdxLine(line)
  const maxOrderReuse = Math.max(1, Number(config.maxOrderReuse) || 3)
  const reusableSessions = forceRotate ? [] : listReusableOrderSessions(maxOrderReuse, normalizedLine)
  const failures = []
  let cooldownFailure = null

  for (const candidate of reusableSessions) {
    try {
      const next = await fetchWxCodeInternal(config, candidate.orderId, { line: normalizedLine, allowTokenRefresh: true })
      saveOrderSession({ ...next, line: normalizedLine })
      return {
        reused: next.orderId === candidate.orderId,
        orderId: next.orderId,
        wxCode: next.wxCode,
        line: normalizedLine,
        usedCount: next.orderId === candidate.orderId ? candidate.usedCount : 0,
        raw: next.raw,
        qmfNewOrderOpened: false,
      }
    } catch (err) {
      const actual = err instanceof Error ? err.message : String(err || 'unknown error')
      failures.push(`复用 orderId ${candidate.orderId} 失败：${actual}`)
      const cooldownMs = Number(err?.cooldownMs || 0)
      if (err?.qmfOrderStatus === 'cooldown' || isTdxSessionCooldownMessage(actual)) {
        cooldownFailure = {
          orderId: String(err?.qmfOrderId || candidate.orderId || '').trim(),
          message: actual,
          cooldownMs: Math.max(QMF_ORDER_COOLDOWN_MS, Number.isFinite(cooldownMs) ? cooldownMs : 0),
        }
      }
    }
  }

  if (!allowNewOrder) {
    const coolingSession = forceRotate ? null : getNextCoolingOrderSession(maxOrderReuse, normalizedLine)
    const coolingOrderId = String(coolingSession?.orderId || cooldownFailure?.orderId || '').trim()
    const cooldownRemainingMs = Math.max(
      QMF_ORDER_COOLDOWN_MS,
      Number(coolingSession?.cooldownRemainingMs || 0),
      Number(cooldownFailure?.cooldownMs || 0),
    )
    if (coolingSession || cooldownFailure) {
      const message = coolingOrderId
        ? `暂无可用 orderId：orderId ${coolingOrderId} 请稍后复扫，系统将在冷却后自动重试。`
        : '暂无可用 orderId：请稍后复扫，系统将在冷却后自动重试。'
      const error = createQmfWxCodeError(message, 'cooldown', coolingOrderId)
      error.cooldownMs = cooldownRemainingMs
      error.qmfNewOrderOpened = false
      throw error
    }
    const message = failures.length
      ? `暂无可用 orderId：${failures.join('；')}`
      : '暂无可用 orderId，且当前流程禁止新开 QMF 订单。'
    const error = createQmfWxCodeError(message, 'exhausted')
    error.qmfNewOrderOpened = false
    throw error
  }

  try {
    const next = await fetchWxCodeInternal(config, '', { line: normalizedLine, allowTokenRefresh: true })
    saveOrderSession({ ...next, line: normalizedLine })
    return {
      reused: false,
      orderId: next.orderId,
      wxCode: next.wxCode,
      line: normalizedLine,
      usedCount: 0,
      raw: next.raw,
      qmfNewOrderOpened: normalizedLine === 'line1',
    }
  } catch (err) {
    const actual = err instanceof Error ? err : createQmfWxCodeError(String(err || 'Unable to acquire wxCode'))
    if (failures.length) {
      actual.message = `${actual.message}；${failures.join('；')}`
    }
    actual.qmfNewOrderOpened = normalizedLine === 'line1' ? Boolean(extractQmfOrderIdFromError(actual)) : false
    throw actual
  }
}

export async function loginTdxWithSms({ phone, smsCode, openId }) {
  const config = await ensureTdxConfig()
  if (config.mode === 'mock') {
    if (process.env.RIGHTCONTROL_STRICT_LIVE === '1') {
      return {
        ok: false,
        token: '',
        message: 'TDX 严格实盘模式下禁止使用 mock 登录。',
        invalidToken: true,
        payload: { code: 503, message: 'TDX strict live mode forbids mock login' },
      }
    }
    const token = `tdx_${randomUUID()}`
    return {
      ok: true,
      token,
      payload: { code: 200, token, message: 'Mock login success' },
    }
  }

  let payload
  try {
    payload = await postJson(
      config.loginSmsUrl,
      { phone, smsCode, nickname: '', openId, originalIp: config.originalIp },
      { headers: buildMiniAppHeaders('', 'application/json') },
    )
  } catch (error) {
    console.error('[tdx][login-sms][error]', JSON.stringify({
      phone: String(phone || '').trim(),
      smsCode: String(smsCode || '').trim(),
      openId: String(openId || '').trim(),
      error: error instanceof Error ? error.message : String(error || ''),
    }))
    throw error
  }
  rememberLastPayload({ type: 'loginSms', payload })

  const rawToken = String(payload?.token || payload?.data?.token || '').trim()
  const message = payload?.message || payload?.msg || payload?.data?.message || ''
  const loginOkCode = payload?.code === 200 || payload?.code === '200' || payload?.success === true || payload?.isSuccess === true
  const usableToken = isUsableTdxLoginToken(rawToken, { mode: config.mode })
  if (!loginOkCode || !usableToken) {
    console.error('[tdx][login-sms][failed]', JSON.stringify({
      phone: String(phone || '').trim(),
      upstreamCode: payload?.code ?? null,
      upstreamMessage: String(message || '').trim(),
      loginOkCode,
      usableToken,
      invalidToken: Boolean(rawToken && !usableToken),
      tokenPresent: Boolean(rawToken),
      payload,
    }))
  }

  return {
    ok: loginOkCode && usableToken,
    token: usableToken ? rawToken : '',
    message: loginOkCode && rawToken && !usableToken
      ? '短信登录未返回有效会话 token。'
      : message,
    invalidToken: Boolean(rawToken && !usableToken),
    payload,
  }
}

export async function persistTdxSmsReceiveLoginToken({
  phone,
  token,
  orderId = '',
  wxCode = '',
  line = 'line1',
  login = null,
} = {}) {
  const normalizedPhone = String(phone || '').trim()
  const normalizedToken = String(token || '').trim()
  const normalizedOrderId = String(orderId || '').trim()
  const normalizedWxCode = sanitizeStoredWxCode(wxCode)
  const normalizedLine = normalizeTdxLine(line)
  const normalizedLogin =
    login && typeof login === 'object'
      ? {
          ...login,
          ok: login.ok !== false,
          token: normalizedToken || String(login.token || '').trim(),
        }
      : {
          ok: Boolean(normalizedToken),
          token: normalizedToken,
          payload: null,
        }

  const retryContext =
    normalizedPhone && normalizedToken && normalizedOrderId && normalizedWxCode
      ? {
          mode: 'save-login-token',
          phone: normalizedPhone,
          orderId: normalizedOrderId,
          wxCode: normalizedWxCode,
          loginToken: normalizedToken,
          line: normalizedLine,
        }
      : null

  const buildFailure = (message = '短信登录成功，但 token 未成功写入 MySQL。') => ({
    ok: false,
    orderId: normalizedOrderId,
    wxCode: normalizedWxCode,
    usedCount: 0,
    message,
    publicMessage: '网络波动，请刷新重试',
    failureStep: 'save-login-token',
    login: normalizedLogin,
    tokenSaved: false,
    retryContext,
  })

  if (!normalizedPhone || !normalizedToken || !normalizedOrderId || !normalizedWxCode) {
    return buildFailure('短信登录成功，但保存会话缺少必要参数。')
  }

  try {
    const savedToken = await saveTdxLoginToken({
      phone: normalizedPhone,
      token: normalizedToken,
      orderId: normalizedOrderId,
      wxCode: normalizedWxCode,
    })
    const verifiedToken = await getSavedTdxLoginToken(normalizedPhone)
    const tokenPersisted = Boolean(
      savedToken?.token
      && verifiedToken?.token === normalizedToken
      && String(verifiedToken?.orderId || '').trim() === normalizedOrderId
      && sanitizeStoredWxCode(verifiedToken?.wxCode || '') === normalizedWxCode
    )
    if (tokenPersisted) {
      return {
        ok: true,
        orderId: normalizedOrderId,
        wxCode: normalizedWxCode,
        usedCount: 0,
        login: normalizedLogin,
        tokenSaved: true,
      }
    }
  } catch (err) {
    const actual = err instanceof Error ? err.message : String(err || '短信登录 token 保存失败')
    return buildFailure(`短信登录成功，但 token 保存失败：${actual}`)
  }

  return buildFailure('短信登录成功，但 token 未成功写入 MySQL。')
}

export async function createTdxSmsReceiveSession({ phone, smsCode, rotate = false, allowNewOrder = true, line = 'line1' }) {
  const config = await ensureTdxConfig()
  const normalizedLine = normalizeTdxLine(line)
  // Live-mode retries are delegated to the background retry queue so we do not
  // hammer QMF and burn fresh orderIds inside a single user request.
  const maxWxCodeAttempts = 1
  const authFailures = []
  let session = null
  let login = null
  let qmfNewOrderOpened = false
  let authorizedUsedCount = 0
  let sessionCooldownMs = 0
  let saveLoginTokenRetryContext = null

  const describeLoginFailure = (loginResult) => (
    loginResult?.message
    || loginResult?.payload?.message
    || loginResult?.payload?.msg
    || '上游短信登录失败'
  )

  const isDirectLoginSmsFailure = (loginResult) => {
    const msg = String(describeLoginFailure(loginResult) || '').trim()
    return /该手机号通过(?:其他|其它)微信登录|(?:其他|其它)微信登录|无法使用当前微信登录|当前微信登录/.test(msg)
  }

  const publicLoginSmsFailureMessage = (loginResult) => {
    const msg = String(describeLoginFailure(loginResult) || '').trim()
    if (isDirectLoginSmsFailure(loginResult) && msg) {
      return '解除时间不够30天哦 ，解除失败'
    }
    return '登录失败，请核对手机号和验证码后重试。'
  }

  const isWechatAuthFailure = (loginResult) => {
    if (!loginResult || loginResult.ok) return false
    if (isDirectLoginSmsFailure(loginResult)) return false
    const msg = String(loginResult.message || loginResult.payload?.message || loginResult.payload?.msg || '').toLowerCase()
    return msg.includes('微信授权') || msg.includes('授权') || msg.includes('openid') || msg.includes('wxcode') || msg.includes('auth')
  }

  const shouldStopRetrying = (message = '') => (
    isHardTdxSessionFailureMessage(message)
    || isTdxSessionCooldownMessage(message)
  )

  for (let attempt = 0; attempt < maxWxCodeAttempts; attempt += 1) {
    try {
      const forceRotateCurrentAttempt = Boolean(rotate) && attempt === 0
      session = await allocateTdxOrderSession({
        forceRotate: forceRotateCurrentAttempt,
        allowNewOrder,
        line: normalizedLine,
      })
      qmfNewOrderOpened = qmfNewOrderOpened || Boolean(session?.qmfNewOrderOpened)
    } catch (err) {
      const actual = err instanceof Error ? err.message : String(err || 'wxCode 分配失败')
      qmfNewOrderOpened = qmfNewOrderOpened || Boolean(err?.qmfNewOrderOpened)
      const cooldownMs = Number(err?.cooldownMs || 0)
      if (Number.isFinite(cooldownMs) && cooldownMs > 0) {
        sessionCooldownMs = Math.max(sessionCooldownMs, cooldownMs)
      }
      authFailures.push(`第 ${attempt + 1} 次获取 wxCode 失败：${actual}`)
      login = {
        ok: false,
        token: '',
        message: actual,
        payload: null,
        cooldownMs: Number.isFinite(cooldownMs) && cooldownMs > 0 ? cooldownMs : 0,
      }
      if (shouldStopRetrying(actual)) {
        break
      }
      continue
    }

    if (config.mode !== 'mock' && !isUsableWxCode(session.wxCode)) {
      authFailures.push(`第 ${attempt + 1} 次获取到的 wxCode 不可用`)
      login = {
        ok: false,
        token: '',
        message: '上游授权会话不可用',
        payload: session.raw || null,
      }
      continue
    }

    try {
      login = await loginTdxWithSms({ phone, smsCode, openId: session.wxCode })
    } catch (err) {
      login = {
        ok: false,
        token: '',
        message: err instanceof Error ? err.message : String(err || '上游短信登录失败'),
        payload: null,
      }
    }
    if (!login.ok) {
      const failureMessage = describeLoginFailure(login)
      if (isWechatAuthFailure(login) && !shouldStopRetrying(failureMessage) && attempt < maxWxCodeAttempts - 1) {
        authFailures.push(`第 ${attempt + 1} 次 wxCode 授权失败：${failureMessage}`)
        continue
      }
      break
    }

    if (!isUsableTdxLoginToken(login.token, { mode: config.mode })) {
      authFailures.push(`第 ${attempt + 1} 次短信登录返回了不可用 token`)
      login = { ...login, invalidToken: true, token: '' }
      if (!shouldStopRetrying(login.message) && attempt < maxWxCodeAttempts - 1) {
        continue
      }
      break
    }

    const usedCount = markOrderUsed(session.orderId)
    authorizedUsedCount = usedCount
    session = { ...session, usedCount }
    const persistedLogin = await persistTdxSmsReceiveLoginToken({
      phone,
      token: login.token,
      orderId: session.orderId,
      wxCode: session.wxCode,
      line: normalizedLine,
      login,
    })
    login = persistedLogin.login || login
    if (persistedLogin.ok) {
      return {
        ...persistedLogin,
        line: normalizedLine,
        usedCount,
        qmfNewOrderOpened,
      }
    }
    saveLoginTokenRetryContext = persistedLogin.retryContext || saveLoginTokenRetryContext
    authFailures.push(`第 ${attempt + 1} 次会话保存失败：${persistedLogin.message || '短信登录成功，但 token 未成功写入 MySQL。'}`)
  }

  if (!session || (config.mode !== 'mock' && !isUsableWxCode(session.wxCode))) {
    return {
      ok: false,
      orderId: session?.orderId || '',
      wxCode: '',
      line: normalizedLine,
      usedCount: session?.usedCount || 0,
      message: authFailures.length
        ? `上游授权会话不可用：${authFailures.join('；')}`
        : '上游授权会话不可用。',
      publicMessage: '网络波动，请刷新重试',
      failureStep: 'prepare-session',
      login,
      tokenSaved: false,
      qmfNewOrderOpened,
      cooldownMs: sessionCooldownMs,
    }
  }

  if (!login?.ok) {
    const authRelated = isWechatAuthFailure(login) || Boolean(login?.invalidToken)
    const message = [
      describeLoginFailure(login),
      ...authFailures,
    ].filter(Boolean).join('；')
    return {
      ok: false,
      orderId: session.orderId,
      wxCode: session.wxCode,
      line: normalizedLine,
      usedCount: session.usedCount || authorizedUsedCount,
      message: message || '上游短信登录失败',
      publicMessage: authRelated
        ? '网络波动，请刷新重试'
        : publicLoginSmsFailureMessage(login),
      failureStep: 'login-sms',
      login,
      tokenSaved: false,
      qmfNewOrderOpened,
      cooldownMs: sessionCooldownMs,
    }
  }

  return {
    ok: false,
    orderId: session.orderId,
    wxCode: session.wxCode,
    line: normalizedLine,
    usedCount: session.usedCount || authorizedUsedCount,
    message: authFailures.length
      ? authFailures.join('；')
      : '短信登录成功，但会话保存失败，请稍后重试。',
    publicMessage: '网络波动，请刷新重试',
    failureStep: 'save-login-token',
    login,
    tokenSaved: false,
    retryContext: saveLoginTokenRetryContext,
    qmfNewOrderOpened,
    cooldownMs: sessionCooldownMs,
  }
}

export async function precheckTdxPhone(phone) {
  const config = await ensureTdxConfig({ requireAppealChannel: false })
  if (config.mode === 'mock') {
    const payload = { code: 200, message: 'mock 高频标记，可处理' }
    rememberLastPayload({ type: 'checkAppletPhonePrecheck', targetPhone: phone, payload })
    return {
      ok: true,
      allowed: true,
      status: '可处理',
      message: '模拟环境：号码可继续处理。',
      raw: payload,
    }
  }

  const teddyPayload = await postTeddyPhoneCheck(phone)
  const teddyResult = parseTeddyPhoneCheckPrecheck(teddyPayload)
  rememberLastPayload({
    type: 'teddyPhoneCheckPrecheck',
    targetPhone: phone,
    payload: teddyPayload,
  })
  if (!isPrecheckDailyLimitMessage(teddyResult.message)) {
    return teddyResult
  }

  let authCandidates = []
  try {
    authCandidates = await resolvePrecheckTokenCandidates(phone, config)
  } catch (err) {
    rememberLastPayload({
      type: 'checkAppletPhonePrecheck',
      targetPhone: phone,
      fallbackFrom: 'teddyDailyLimit',
      rotationSkipped: true,
      rotationSkipReason: err instanceof Error ? err.message : String(err || 'no-available-token'),
      payload: teddyPayload,
    })
    return teddyResult
  }

  const requestPayload = {
    phone,
    openId: buildSyntheticOpenId(phone),
    originalIp: config.originalIp,
  }
  const attempts = [
    {
      authSource: 'teddy',
      authPhone: '',
      allowed: Boolean(teddyResult.allowed),
      status: teddyResult.status,
      message: teddyResult.message,
      hitDailyLimit: true,
    },
  ]
  let lastAttempt = {
    auth: {
      source: 'teddy',
      phone: '',
      token: '',
    },
    payload: teddyPayload,
    result: teddyResult,
    hitDailyLimit: true,
  }

  for (let index = 0; index < authCandidates.length; index += 1) {
    const auth = authCandidates[index]
    const payload = await postMiniAppJson(config.checkAppletPhoneUrl, requestPayload, auth.token)
    const result = parseCheckAppletPhonePrecheck(payload)
    const hitDailyLimit = isPrecheckDailyLimitMessage(result.message)
    attempts.push({
      authSource: auth.source,
      authPhone: auth.phone,
      allowed: Boolean(result.allowed),
      status: result.status,
      message: result.message,
      hitDailyLimit,
    })
    lastAttempt = {
      auth,
      payload,
      result,
      hitDailyLimit,
    }

    if (hitDailyLimit && index < authCandidates.length - 1) {
      continue
    }

    if (auth.phone && auth.token && !hitDailyLimit) {
      try {
        await saveTdxLoginToken({
          phone: auth.phone,
          token: auth.token,
          orderId: auth.orderId,
          wxCode: auth.wxCode,
        })
      } catch {}
    }

    rememberLastPayload({
      type: 'checkAppletPhonePrecheck',
      targetPhone: phone,
      fallbackFrom: 'teddyDailyLimit',
      authSource: auth.source,
      authPhone: auth.phone,
      rotatedToken: attempts.length > 1,
      rotationExhausted: Boolean(hitDailyLimit && attempts.length >= authCandidates.length),
      attempts,
      payload,
    })
    return result
  }

  if (lastAttempt) {
    rememberLastPayload({
      type: 'checkAppletPhonePrecheck',
      targetPhone: phone,
      fallbackFrom: 'teddyDailyLimit',
      authSource: lastAttempt.auth.source,
      authPhone: lastAttempt.auth.phone,
      rotatedToken: attempts.length > 1,
      rotationExhausted: Boolean(lastAttempt.hitDailyLimit),
      attempts,
      payload: lastAttempt.payload,
    })
    return lastAttempt.result
  }

  throw new Error('号码预检查失败，请稍后重试。')
}

export async function getTdxRuntimeSnapshot() {
  return getTdxRuntimeState()
}

export async function rotateTdxOrderSession(line = 'line1') {
  clearCurrentOrderSession(line)
  return getTdxRuntimeState()
}

export async function runTdxHealthCheck() {
  const config = await getTdxConfig()
  const runtime = await getTdxRuntimeState()
  const validationErrors = validateLiveTdxConfig(config)

  if (config.mode === 'mock') {
    const payload = {
      ok: true,
      mode: 'mock',
      checks: [
        { key: 'shared-config', ok: true, message: 'Mock mode does not require upstream credentials.' },
        { key: 'line1-config', ok: true, message: '线路1 mock 配置已就绪。' },
        { key: 'line2-config', ok: true, message: '线路2 mock 配置已就绪。' },
        { key: 'runtime', ok: true, message: runtime.currentOrderId ? 'Runtime has an active order session.' : 'Runtime is empty and ready.' },
      ],
      runtime,
    }
    rememberLastPayload({ type: 'healthCheck', payload })
    return payload
  }

  if (validationErrors.length) {
    const payload = {
      ok: false,
      mode: 'live',
      checks: [{ key: 'shared-config', ok: false, message: validationErrors.join('; ') }],
      runtime,
    }
    rememberLastPayload({ type: 'healthCheck', payload })
    return payload
  }

  const line1Ready = Boolean(config.qmfUsername && config.qmfPassword && config.qmfAppId && config.qmfLoginUrl && config.qmfCodeUrl)
  const line2Ready = Boolean(config.dfUsername && config.dfPassword && config.dfLoginUrl && config.dfCreateOrderUrl && config.dfTwoAuthUrl && config.dfOrderViewUrl && config.dfProjectId)
  let qmfToken = ''
  let dengfengToken = ''
  if (line1Ready) {
    try {
      qmfToken = await fetchQmfToken(config)
    } catch {}
  }
  if (line2Ready) {
    try {
      dengfengToken = await fetchDengfengToken(config)
    } catch {}
  }
  const payload = {
    ok: (line1Ready && Boolean(qmfToken)) || (line2Ready && Boolean(dengfengToken)),
    mode: 'live',
    checks: [
      { key: 'shared-config', ok: true, message: '共享上游配置已就绪。' },
      { key: 'line1-config', ok: line1Ready, message: line1Ready ? (qmfToken ? '线路1(QMF) 凭据校验成功。' : '线路1(QMF) 无法获取 token。') : '线路1(QMF) 配置未完成。' },
      { key: 'line2-config', ok: line2Ready, message: line2Ready ? (dengfengToken ? '线路2(登峰) 凭据校验成功。' : '线路2(登峰) 无法获取 token。') : '线路2(登峰) 配置未完成。' },
      { key: 'runtime', ok: true, message: runtime.currentOrderId ? 'Runtime has an active order session.' : 'Runtime is empty and ready.' },
    ],
    runtime,
  }
  rememberLastPayload({ type: 'healthCheck', payload })
  return payload
}

function createLiveStepMap(task, config) {
  const plan = buildTdxExecutionPlan(task, config)
  const statuses = Object.fromEntries(plan.map((step, index) => [step.key, index === 0 ? 'ready' : 'pending']))
  const notes = {}
  return { statuses, notes }
}

function failExecution(task, config, state, failureKey, message, extra = {}) {
  state.statuses[failureKey] = 'blocked'
  state.notes[failureKey] = message
  return {
    ok: false,
    mode: config.mode,
    result: {
      status: 'failed',
      message,
      failureStep: failureKey,
      ...extra,
      steps: buildStepState(task, config, state.statuses, state.notes),
    },
  }
}

function pickProgressItem(items = [], matcher = {}) {
  if (!Array.isArray(items) || !items.length) return null
  const orderId = String(matcher.orderId || '').trim()
  const phone = String(matcher.phone || '').trim()
  if (orderId) {
    const matched = items.find((item) => {
      const values = [
        item?.orderId,
        item?.applyOrderId,
        item?.appealOrderId,
        item?.id,
        item?.applyId,
        item?.sessionId,
      ].map((value) => String(value || '').trim())
      return values.includes(orderId) || JSON.stringify(item || {}).includes(orderId)
    })
    if (matched) return matched
  }
  if (phone) {
    const matched = items.find((item) => JSON.stringify(item || {}).includes(phone))
    if (matched) return matched
  }
  return items[0]
}

const TDX_DIRECT_SUCCESS_MESSAGE = '泰迪未来标记已取消'
const TDX_REFUNDED_PENDING_MESSAGE = '该号码显示正在审核，已退款'

function collectProgressTexts(progress = {}) {
  return [
    progress?.statusDesc,
    progress?.auditDesc,
    progress?.auditRemark,
    progress?.auditOpinion,
    progress?.opinion,
    progress?.resultDesc,
    progress?.remark,
  ]
    .map((value) => String(value || '').trim())
    .filter(Boolean)
}

function resolveProgressResult(progressPayload, matcher = {}) {
  const items = progressPayload?.data?.data
  const first = pickProgressItem(items, matcher)
  if (!first) {
    return {
      finalStatus: 'processing',
      message: '号码申诉已提交，请等待1-2个小时更新结果',
      progress: null,
    }
  }

  const status = String(first.status || '')
  const statusDesc = String(first.statusDesc || '')
  const texts = collectProgressTexts(first)
  const directSuccessText = texts.find((text) => text.includes(TDX_DIRECT_SUCCESS_MESSAGE))
  const processedLike = status === '4' || /processed|已处理|处理完成|已完成/i.test(texts.join(' '))
  const progressMessage = texts.find(Boolean)

  if (directSuccessText) {
    return {
      finalStatus: 'completed',
      message: directSuccessText,
      progress: first,
    }
  }

  if (processedLike) {
    return {
      finalStatus: 'failed',
      message: TDX_REFUNDED_PENDING_MESSAGE,
      progress: first,
      refundSuggested: true,
      failureReason: progressMessage || statusDesc || '上游返回已处理状态，但未返回直接成功标记。',
    }
  }

  return {
    finalStatus: 'processing',
    message: statusDesc || '号码申诉已提交，请等待1-2个小时更新结果',
    progress: first,
  }
}

export async function queryTdxCleanupProgress({ phone = '', orderId = '', loginToken = '' } = {}) {
  const config = await ensureTdxConfig()
  if (config.mode === 'mock') {
    return {
      ok: true,
      mode: 'mock',
      result: {
        status: 'completed',
        message: 'Mock cleanup progress completed.',
        orderId,
        progress: null,
      },
    }
  }

  let token = loginToken || ''
  if (!isUsableTdxLoginToken(token, { mode: config.mode })) {
    const savedLogin = phone ? await getSavedTdxLoginToken(phone) : null
    token = savedLogin?.token || ''
  }
  if (!isUsableTdxLoginToken(token, { mode: config.mode })) {
    throw new Error('Saved login-sms token is invalid or expired; unable to query progress.')
  }

  const progressPayload = await postMiniAppJson(config.queryProgressUrl, {}, token)
  rememberLastPayload({ type: 'autoQueryProgress', payload: progressPayload })
  if (!isStepSuccess(progressPayload)) {
    return {
      ok: false,
      mode: config.mode,
      result: {
        status: 'failed',
        message: progressPayload?.message || 'Unable to query the upstream progress.',
        orderId,
        raw: progressPayload,
      },
    }
  }

  const progressResult = resolveProgressResult(progressPayload, { phone, orderId })
  return {
    ok: true,
    mode: config.mode,
    result: {
      status: progressResult.finalStatus,
      message: progressResult.message,
      orderId,
      progress: progressResult.progress,
      refundSuggested: Boolean(progressResult.refundSuggested),
      failureReason: progressResult.failureReason || '',
    },
  }
}

export async function executeTdxCleanupTask(task) {
  const config = await ensureTdxConfig()
  const skipCheckAppletPhone = Boolean(task?.skipCheckAppletPhone)
  const wxCode = config.mode === 'mock'
    ? String(task.wxCode || '').trim()
    : sanitizeStoredWxCode(task.wxCode)

  if (!skipCheckAppletPhone && (!task.orderId || !wxCode)) {
    throw new Error('Task is missing a valid orderId or wxCode; prepare the session first.')
  }
  task = { ...task, orderId: String(task?.orderId || '').trim(), wxCode }

  if (config.mode === 'mock') {
    const steps = buildTdxExecutionPlan(task, config, { phase: 'completed' })
    const payload = {
      ok: true,
      mode: 'mock',
      result: {
        status: 'completed',
        message: 'Mock cleanup execution completed.',
        orderId: task.orderId,
        wxCode: task.wxCode,
        steps,
      },
    }
    rememberLastPayload({ type: 'executeCleanup', payload })
    return payload
  }

  let loginToken = task.result?.login?.token || task.loginToken || ''
  if (!isUsableTdxLoginToken(loginToken, { mode: config.mode })) {
    const savedLogin = await getSavedTdxLoginToken(task.phone)
    loginToken = savedLogin?.token || ''
  }
  if (!isUsableTdxLoginToken(loginToken, { mode: config.mode })) {
    throw new Error('Saved login-sms token is invalid or expired; prepare the session again.')
  }

  const state = createLiveStepMap(task, config)

  const startPayload = await postMiniAppJson(config.answersStartUrl, { questionnaireId: 1 }, loginToken)
  rememberLastPayload({ type: 'answersStart', payload: startPayload })
  const sessionId = normalizeSessionId(startPayload?.data?.sessionId || startPayload?.sessionId)
  if (!isStepSuccess(startPayload) || !sessionId) {
    return failExecution(task, config, state, 'answers-start', startPayload?.message || 'Unable to create the questionnaire session.', { raw: startPayload })
  }
  state.statuses['answers-start'] = 'completed'
  state.notes['answers-start'] = `Questionnaire session created: ${sessionId}`

  if (skipCheckAppletPhone) {
    state.statuses['check-applet-phone'] = 'completed'
    state.notes['check-applet-phone'] = 'Skipped during admin resubmit because SMS precheck already passed.'
  } else {
    const checkPayload = await postMiniAppJson(
      config.checkAppletPhoneUrl,
      { phone: task.phone, openId: task.wxCode, originalIp: config.originalIp },
      loginToken,
    )
    rememberLastPayload({ type: 'checkAppletPhone', payload: checkPayload })
    if (!isStepSuccess(checkPayload)) {
      return failExecution(task, config, state, 'check-applet-phone', checkPayload?.message || 'Phone binding validation failed.', { raw: checkPayload })
    }
    state.statuses['check-applet-phone'] = 'completed'
    state.notes['check-applet-phone'] = 'Phone and openId validation passed.'
  }

  for (const answer of DEFAULT_QUESTION_ANSWERS.slice(0, 6)) {
    const answerPayload = await postMiniAppJson(
      config.answersSubmitUrl,
      { sessionId, selectedOptionCode: answer.selectedOptionCode, answerText: answer.answerText, questionCode: answer.questionCode },
      loginToken,
    )
    rememberLastPayload({ type: `submitAnswer:${answer.questionCode}`, payload: answerPayload })
    if (!isStepSuccess(answerPayload)) {
      return failExecution(task, config, state, 'submit-answers', `Questionnaire answer ${answer.questionCode} failed: ${answerPayload?.message || 'unknown error'}`, { raw: answerPayload })
    }
  }

  const evidencePayload = await uploadMiniAppEvidence(config.answersSubmitUrl, sessionId, loginToken)
  rememberLastPayload({ type: 'uploadEvidence', payload: evidencePayload })
  if (!isStepSuccess(evidencePayload)) {
    return failExecution(task, config, state, 'upload-evidence', evidencePayload?.message || 'Evidence upload failed.', { raw: evidencePayload })
  }

  for (const answer of DEFAULT_QUESTION_ANSWERS.slice(6)) {
    const answerPayload = await postMiniAppJson(
      config.answersSubmitUrl,
      { sessionId, selectedOptionCode: answer.selectedOptionCode, answerText: answer.answerText, questionCode: answer.questionCode },
      loginToken,
    )
    rememberLastPayload({ type: `submitAnswer:${answer.questionCode}`, payload: answerPayload })
    if (!isStepSuccess(answerPayload)) {
      return failExecution(task, config, state, 'submit-answers', `Questionnaire answer ${answer.questionCode} failed: ${answerPayload?.message || 'unknown error'}`, { raw: answerPayload })
    }
  }

  state.statuses['submit-answers'] = 'completed'
  state.notes['submit-answers'] = 'The fixed questionnaire answers were submitted successfully.'
  state.statuses['upload-evidence'] = 'completed'
  state.notes['upload-evidence'] = 'The placeholder evidence image was uploaded successfully.'
  state.statuses['query-progress'] = 'active'
  state.notes['query-progress'] = 'Polling the upstream progress endpoint.'

  const progressPayload = await postMiniAppJson(config.queryProgressUrl, {}, loginToken)
  rememberLastPayload({ type: 'queryProgress', payload: progressPayload })
  if (!isStepSuccess(progressPayload)) {
    return failExecution(task, config, state, 'query-progress', progressPayload?.message || 'Unable to query the upstream progress.', { raw: progressPayload })
  }

  const progressResult = resolveProgressResult(progressPayload)
  state.statuses['query-progress'] = progressResult.finalStatus === 'completed'
    ? 'completed'
    : progressResult.finalStatus === 'failed'
      ? 'failed'
      : 'active'
  state.notes['query-progress'] = progressResult.message

  const steps = buildStepState(task, config, state.statuses, state.notes)
  const payload = {
    ok: true,
    mode: 'live',
    result: {
      status: progressResult.finalStatus,
      message: progressResult.message,
      orderId: task.orderId,
      wxCode: task.wxCode,
      sessionId,
      progress: progressResult.progress,
      refundSuggested: Boolean(progressResult.refundSuggested),
      failureReason: progressResult.failureReason || '',
      steps,
    },
  }
  rememberLastPayload({ type: 'executeCleanup', payload })
  return payload
}



