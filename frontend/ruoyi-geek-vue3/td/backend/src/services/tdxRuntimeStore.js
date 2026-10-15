import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { query } from '../data/liveDb.js'
import { getTdxConfig } from '../config/tdxConfig.js'
import { sanitizeStoredLoginToken, sanitizeStoredWxCode } from './tdxSessionValue.js'

const runtimePath = resolve(process.cwd(), 'data', 'tdx-runtime.json')

const fallbackState = {
  currentOrderId: '',
  currentWxCode: '',
  currentLine: 'line1',
  orderSessions: {},
  retiredSessions: {},
  usage: {},
  phoneClaims: {},
  legacyUsage: {},
  qmfSession: null,
  loginTokens: {},
  lastPayload: null,
}

let tokenTableReadyPromise = null
let legacyTokenMigrationPromise = null

function formatMysqlDate(value = new Date()) {
  const date = value instanceof Date ? value : new Date(value)
  const safe = Number.isNaN(date.getTime()) ? new Date() : date
  const pad = (item) => String(item).padStart(2, '0')
  return `${safe.getFullYear()}-${pad(safe.getMonth() + 1)}-${pad(safe.getDate())} ${pad(safe.getHours())}:${pad(safe.getMinutes())}:${pad(safe.getSeconds())}`
}

function normalizeTokenRow(row = {}) {
  if (!row) return null
  const savedAt = row.SavedAt || row.savedAt || row.UpdatedAt || row.updatedAt || null
  const updatedAt = row.UpdatedAt || row.updatedAt || savedAt || null
  return {
    token: String(row.Token || row.token || ''),
    orderId: String(row.OrderId || row.orderId || ''),
    wxCode: String(row.WxCode || row.wxCode || ''),
    savedAt: savedAt instanceof Date ? savedAt.toISOString() : String(savedAt || ''),
    updatedAt: updatedAt instanceof Date ? updatedAt.toISOString() : String(updatedAt || ''),
  }
}

function normalizeOrderSessionStatus(value = '') {
  const status = String(value || 'active').trim().toLowerCase()
  return ['active', 'cooldown', 'invalid'].includes(status) ? status : 'active'
}

export function normalizeTdxLine(value = '') {
  const line = String(value || '').trim().toLowerCase()
  if (line === 'line2') return 'line2'
  if (line === 'line3') return 'line3'
  return 'line1'
}

function normalizeClaimedPhone(value = '') {
  return String(value || '').trim().replace(/\s+/g, '')
}

function normalizePhoneClaims(input = {}) {
  const claims = {}
  for (const [key, values] of Object.entries(input || {})) {
    const orderId = String(key || '').trim()
    if (!orderId) continue
    const uniquePhones = [...new Set((Array.isArray(values) ? values : [])
      .map((value) => normalizeClaimedPhone(value))
      .filter(Boolean))]
    if (uniquePhones.length) claims[orderId] = uniquePhones
  }
  return claims
}

function normalizeUsageCount(value) {
  const count = Number(value || 0)
  return Number.isFinite(count) && count > 0 ? Math.floor(count) : 0
}

function normalizeLegacyUsage(input = {}, usage = {}) {
  const legacyUsage = {}
  const orderIds = new Set([
    ...Object.keys(usage || {}),
    ...Object.keys(input || {}),
  ])
  for (const orderId of orderIds) {
    const normalizedOrderId = String(orderId || '').trim()
    if (!normalizedOrderId) continue
    const count = Object.prototype.hasOwnProperty.call(input || {}, normalizedOrderId)
      ? normalizeUsageCount(input[normalizedOrderId])
      : normalizeUsageCount(usage?.[normalizedOrderId])
    if (count) legacyUsage[normalizedOrderId] = count
  }
  return legacyUsage
}

function normalizeOrderSessions(input = {}) {
  const sessions = {}
  for (const [key, item] of Object.entries(input || {})) {
    const orderId = String(item?.orderId || key || '').trim()
    const wxCode = String(item?.wxCode || '').trim()
    if (!orderId) continue
    const now = new Date().toISOString()
    const savedAt = String(item?.savedAt || item?.createdAt || item?.updatedAt || now).trim()
    const updatedAt = String(item?.updatedAt || savedAt || now).trim()
    sessions[orderId] = {
      orderId,
      wxCode,
      line: normalizeTdxLine(item?.line),
      savedAt,
      updatedAt,
      status: normalizeOrderSessionStatus(item?.status),
      cooldownUntil: String(item?.cooldownUntil || '').trim(),
      lastFailure: String(item?.lastFailure || '').trim(),
      lastCheckedAt: String(item?.lastCheckedAt || '').trim(),
    }
  }
  return sessions
}

function sanitizeAuditMessage(value = '') {
  return String(value || '')
    .replace(/(token|authorization|wxcode|openid|access[_-]?token|password|secret)\s*[:=]\s*[^\s,;]+/gi, '$1=[redacted]')
    .replace(/\b[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b/g, '[redacted-jwt]')
    .replace(/\b(?=[A-Za-z0-9_-]{12,128}\b)(?=[A-Za-z0-9_-]*[A-Za-z])(?=[A-Za-z0-9_-]*\d)[A-Za-z0-9_-]+\b/g, '[redacted-wxcode]')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 1000)
}

function normalizeRetiredSessions(input = {}) {
  const sessions = {}
  const entries = Array.isArray(input)
    ? input.map((item) => [item?.orderId, item])
    : Object.entries(input || {})
  for (const [key, item] of entries) {
    const orderId = String(item?.orderId || key || '').trim()
    if (!orderId) continue
    const phoneCount = Number(item?.phoneCount || 0)
    sessions[orderId] = {
      orderId,
      line: normalizeTdxLine(item?.line),
      usedCount: normalizeUsageCount(item?.usedCount),
      phoneCount: Number.isFinite(phoneCount) && phoneCount > 0 ? Math.floor(phoneCount) : 0,
      savedAt: String(item?.savedAt || '').trim(),
      retiredAt: String(item?.retiredAt || '').trim(),
      retiredReason: String(item?.retiredReason || 'removed').trim().slice(0, 120),
      retiredMessage: sanitizeAuditMessage(item?.retiredMessage),
      source: String(item?.source || '').trim().slice(0, 120),
      status: normalizeOrderSessionStatus(item?.status),
      lastFailure: sanitizeAuditMessage(item?.lastFailure),
    }
  }
  return sessions
}

function normalizeAuthSession(input = null) {
  const token = String(input?.token || '').trim()
  if (!token) return null
  const savedAt = String(input?.savedAt || '').trim()
  const updatedAt = String(input?.updatedAt || savedAt || '').trim()
  const source = String(input?.source || '').trim()
  return {
    token,
    savedAt,
    updatedAt,
    source,
  }
}

function normalizeRuntimeState(next = {}) {
  const usage = next.usage || {}
  const orderSessions = normalizeOrderSessions(next.orderSessions)
  const retiredSessions = normalizeRetiredSessions(next.retiredSessions)
  const phoneClaims = normalizePhoneClaims(next.phoneClaims)
  const legacyUsage = normalizeLegacyUsage(next.legacyUsage, usage)
  const currentOrderId = String(next.currentOrderId || '').trim()
  const currentWxCode = String(next.currentWxCode || '').trim()
  const currentLine = normalizeTdxLine(next.currentLine || next.orderSessions?.[currentOrderId]?.line)
  const qmfSession = normalizeAuthSession(next.qmfSession)
  if (currentOrderId && !orderSessions[currentOrderId]) {
    const now = new Date().toISOString()
    orderSessions[currentOrderId] = {
      orderId: currentOrderId,
      wxCode: currentWxCode,
      line: currentLine,
      savedAt: now,
      updatedAt: now,
      status: 'active',
      cooldownUntil: '',
      lastFailure: '',
      lastCheckedAt: now,
    }
  } else if (currentOrderId && currentWxCode && orderSessions[currentOrderId] && !orderSessions[currentOrderId].wxCode) {
    orderSessions[currentOrderId] = {
      ...orderSessions[currentOrderId],
      wxCode: currentWxCode,
      line: normalizeTdxLine(orderSessions[currentOrderId].line || currentLine),
    }
  }
  return {
    currentOrderId,
    currentWxCode,
    currentLine,
    orderSessions,
    retiredSessions,
    usage,
    phoneClaims,
    legacyUsage,
    qmfSession,
    loginTokens: next.loginTokens || {},
    lastPayload: next.lastPayload ?? null,
  }
}

function orderSessionSortValue(item = {}) {
  const timestamp = Date.parse(item.updatedAt || item.savedAt || '')
  return Number.isFinite(timestamp) ? timestamp : 0
}

function getOrderSessionUsedCount(state = {}, orderId = '') {
  const normalizedOrderId = String(orderId || '').trim()
  if (!normalizedOrderId) return 0
  const claimedPhones = state.phoneClaims?.[normalizedOrderId] || []
  // Phone claims are the authoritative count for the current model. Legacy
  // usage is only a fallback for sessions created before phoneClaims existed;
  // adding both values double-counts a session after migration.
  if (claimedPhones.length) return claimedPhones.length
  const legacyCount = normalizeUsageCount(
    Object.prototype.hasOwnProperty.call(state.legacyUsage || {}, normalizedOrderId)
      ? state.legacyUsage[normalizedOrderId]
      : state.usage?.[normalizedOrderId],
  )
  return legacyCount
}

// Audit responses may be generated after a task has been persisted. Read the
// current or retired session count directly so older tasks can still expose
// the usage that is already tracked by the runtime store.
export function getTdxOrderSessionUsedCount(orderId = '') {
  const state = readState()
  const normalizedOrderId = String(orderId || '').trim()
  if (!normalizedOrderId) return null

  if (state.orderSessions?.[normalizedOrderId]) {
    return getOrderSessionUsedCount(state, normalizedOrderId)
  }
  if (state.retiredSessions?.[normalizedOrderId]) {
    return normalizeUsageCount(state.retiredSessions[normalizedOrderId].usedCount)
  }

  const hasUsage = Object.prototype.hasOwnProperty.call(state.usage || {}, normalizedOrderId)
    || Object.prototype.hasOwnProperty.call(state.legacyUsage || {}, normalizedOrderId)
    || Object.prototype.hasOwnProperty.call(state.phoneClaims || {}, normalizedOrderId)
  return hasUsage ? getOrderSessionUsedCount(state, normalizedOrderId) : null
}

function getOrderSessionPhoneUseCount(state = {}, orderId = '', phone = '') {
  const normalizedOrderId = String(orderId || '').trim()
  const normalizedPhone = normalizeClaimedPhone(phone)
  if (!normalizedOrderId || !normalizedPhone) return 0
  const index = (state.phoneClaims?.[normalizedOrderId] || []).indexOf(normalizedPhone)
  return index >= 0 ? index + 1 : 0
}

function isOrderSessionCooling(item = {}, nowMs = Date.now()) {
  if (!item) return false
  const cooldownUntilMs = Date.parse(item.cooldownUntil || '')
  return item.status === 'cooldown' && Number.isFinite(cooldownUntilMs) && cooldownUntilMs > nowMs
}

function publicOrderSessionView(state = {}, item = {}) {
  const usedCount = getOrderSessionUsedCount(state, item.orderId)
  return {
    orderId: item.orderId,
    wxCode: item.wxCode,
    line: normalizeTdxLine(item.line),
    usedCount,
    savedAt: item.savedAt || '',
    updatedAt: item.updatedAt || '',
    status: item.status || 'active',
    cooldownUntil: item.cooldownUntil || '',
    lastFailure: item.lastFailure || '',
    lastCheckedAt: item.lastCheckedAt || '',
  }
}

async function ensureTdxLoginTokenTable() {
  if (!tokenTableReadyPromise) {
    tokenTableReadyPromise = query(`
      CREATE TABLE IF NOT EXISTS t_tdx_login_token (
        Id BIGINT NOT NULL AUTO_INCREMENT,
        Phone VARCHAR(32) NOT NULL,
        Token TEXT NOT NULL,
        OrderId VARCHAR(128) NOT NULL DEFAULT '',
        WxCode VARCHAR(255) NOT NULL DEFAULT '',
        SavedAt DATETIME NOT NULL,
        UpdatedAt DATETIME NOT NULL,
        PRIMARY KEY (Id),
        UNIQUE KEY uk_tdx_login_token_phone (Phone),
        KEY idx_tdx_login_token_updated_at (UpdatedAt)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    `)
  }
  await tokenTableReadyPromise
}

async function upsertTdxLoginToken({ phone, token, orderId = '', wxCode = '', savedAt = new Date() } = {}) {
  const normalizedPhone = String(phone || '').trim()
  const normalizedToken = String(token || '').trim()
  if (!normalizedPhone || !normalizedToken) return null
  const savedAtText = formatMysqlDate(savedAt)
  const updatedAtText = formatMysqlDate()
  await query(
    `INSERT INTO t_tdx_login_token (Phone, Token, OrderId, WxCode, SavedAt, UpdatedAt)
     VALUES (?, ?, ?, ?, ?, ?)
     ON DUPLICATE KEY UPDATE
       Token = VALUES(Token),
       OrderId = VALUES(OrderId),
       WxCode = VALUES(WxCode),
       SavedAt = VALUES(SavedAt),
       UpdatedAt = VALUES(UpdatedAt)`,
    [normalizedPhone, normalizedToken, String(orderId || ''), String(wxCode || ''), savedAtText, updatedAtText],
  )
  return {
    token: normalizedToken,
    orderId: String(orderId || ''),
    wxCode: String(wxCode || ''),
    savedAt: new Date(savedAtText).toISOString(),
  }
}

async function migrateLegacyLoginTokens() {
  await ensureTdxLoginTokenTable()
  const state = readState()
  const entries = Object.entries(state.loginTokens || {})
  for (const [phone, item] of entries) {
    await upsertTdxLoginToken({
      phone,
      token: item?.token,
      orderId: item?.orderId,
      wxCode: item?.wxCode,
      savedAt: item?.savedAt || new Date(),
    })
  }
}

async function ensureTokenStorageReady() {
  await ensureTdxLoginTokenTable()
  if (!legacyTokenMigrationPromise) {
    legacyTokenMigrationPromise = migrateLegacyLoginTokens()
  }
  await legacyTokenMigrationPromise
}

function readState() {
  try {
    const raw = readFileSync(runtimePath, 'utf-8')
    const parsed = JSON.parse(raw.replace(/^\uFEFF/, ''))
    return normalizeRuntimeState({
      ...fallbackState,
      ...parsed,
      usage: parsed?.usage || {},
      retiredSessions: parsed?.retiredSessions || {},
      qmfSession: parsed?.qmfSession || null,
      loginTokens: parsed?.loginTokens || {},
      orderSessions: parsed?.orderSessions || {},
    })
  } catch {
    return normalizeRuntimeState({ ...fallbackState })
  }
}

function writeState(next) {
  const normalized = normalizeRuntimeState(next)
  writeFileSync(runtimePath, JSON.stringify(normalized, null, 2), 'utf-8')
  return normalized
}

export async function getTdxRuntimeState() {
  const state = readState()
  let savedLoginTokenPhones = []
  let tokenStorage = 'mysql'
  try {
    await ensureTokenStorageReady()
    const rows = await query('SELECT Phone FROM t_tdx_login_token ORDER BY UpdatedAt DESC')
    savedLoginTokenPhones = rows.map((row) => String(row.Phone || '')).filter(Boolean)
  } catch (err) {
    tokenStorage = 'mysql-unavailable'
    savedLoginTokenPhones = Object.keys(state.loginTokens || {})
  }
  const orderSessions = Object.values(state.orderSessions || {})
    .map((item) => publicOrderSessionView(state, item))
    .sort((a, b) => orderSessionSortValue(b) - orderSessionSortValue(a))
  const retiredSessions = Object.values(state.retiredSessions || {})
    .sort((a, b) => Date.parse(b.retiredAt || '') - Date.parse(a.retiredAt || ''))
  const usage = Object.fromEntries(
    [...new Set([
      ...Object.keys(state.usage || {}),
      ...Object.keys(state.phoneClaims || {}),
      ...Object.keys(state.legacyUsage || {}),
    ])].map((orderId) => [orderId, getOrderSessionUsedCount(state, orderId)]),
  )
  return {
    currentOrderId: state.currentOrderId,
    currentWxCode: state.currentWxCode,
    currentLine: normalizeTdxLine(state.currentLine),
    orderSessionCount: orderSessions.length,
    orderSessions,
    retiredSessionCount: retiredSessions.length,
    retiredSessions,
    usage,
    qmfSession: state.qmfSession?.token
      ? {
        hasToken: true,
        savedAt: state.qmfSession.savedAt || '',
        updatedAt: state.qmfSession.updatedAt || '',
        source: state.qmfSession.source || '',
      }
      : {
        hasToken: false,
        savedAt: '',
        updatedAt: '',
        source: '',
      },
    tokenStorage,
    savedLoginTokenCount: savedLoginTokenPhones.length,
    savedLoginTokenPhones,
    lastPayload: state.lastPayload,
  }
}

export function getCurrentOrderSession(line = '') {
  const state = readState()
  const normalizedLine = line ? normalizeTdxLine(line) : ''
  let current = null
  if (state.currentOrderId) {
    const currentItem = state.orderSessions?.[state.currentOrderId] || {
      orderId: state.currentOrderId,
      wxCode: state.currentWxCode,
      line: state.currentLine,
    }
    if (!normalizedLine || normalizeTdxLine(currentItem.line) === normalizedLine) {
      current = currentItem
    }
  }
  if (!current && normalizedLine) {
    current = Object.values(state.orderSessions || {})
      .filter((item) => item?.orderId && normalizeTdxLine(item.line) === normalizedLine)
      .sort((a, b) => orderSessionSortValue(b) - orderSessionSortValue(a))[0] || null
  }
  if (!current) return null
  return publicOrderSessionView(state, current)
}

export function listReusableOrderSessions(maxReuse = 3, line = '') {
  const state = readState()
  const limit = Math.max(1, Number(maxReuse) || 3)
  const nowMs = Date.now()
  const normalizedLine = line ? normalizeTdxLine(line) : ''
  const allSessions = Object.values(state.orderSessions || {})
    .filter((item) => item?.orderId && normalizeOrderSessionStatus(item?.status) !== 'invalid')
    .filter((item) => !normalizedLine || normalizeTdxLine(item.line) === normalizedLine)
    .sort((a, b) => orderSessionSortValue(b) - orderSessionSortValue(a))

  const currentId = String(state.currentOrderId || '').trim()
  const current = currentId
    ? allSessions.find((item) => item.orderId === currentId && (!normalizedLine || normalizeTdxLine(item.line) === normalizedLine))
    : null
  const candidates = [
    ...(current ? [current] : []),
    ...allSessions.filter((item) => item.orderId !== currentId),
  ]

  return candidates
    .map((item) => publicOrderSessionView(state, item))
    .filter((item) => item.usedCount < limit && !isOrderSessionCooling(item, nowMs))
}

export function listOrderSessionsClaimingPhone(phone = '', line = '') {
  const state = readState()
  const normalizedPhone = normalizeClaimedPhone(phone)
  if (!normalizedPhone) return []
  const normalizedLine = line ? normalizeTdxLine(line) : ''
  const nowMs = Date.now()
  return Object.values(state.orderSessions || {})
    .filter((item) => item?.orderId && normalizeOrderSessionStatus(item?.status) !== 'invalid')
    .filter((item) => !normalizedLine || normalizeTdxLine(item.line) === normalizedLine)
    .filter((item) => !isOrderSessionCooling(item, nowMs))
    .filter((item) => (state.phoneClaims?.[item.orderId] || []).includes(normalizedPhone))
    .sort((a, b) => orderSessionSortValue(b) - orderSessionSortValue(a))
    .map((item) => publicOrderSessionView(state, item))
}

export function reserveOrderSessionPhone({ orderId = '', phone = '', maxReuse = 3 } = {}) {
  const state = readState()
  const normalizedOrderId = String(orderId || '').trim()
  const normalizedPhone = normalizeClaimedPhone(phone)
  const limit = Math.max(1, Number(maxReuse) || 3)
  if (!normalizedOrderId || !normalizedPhone || !state.orderSessions?.[normalizedOrderId]) {
    return { ok: false, usedCount: 0, reason: 'invalid-session-or-phone' }
  }

  const claimedPhones = state.phoneClaims?.[normalizedOrderId] || []
  const usedCount = getOrderSessionUsedCount(state, normalizedOrderId)
  if (claimedPhones.includes(normalizedPhone)) {
    return {
      ok: true,
      reused: true,
      usedCount: getOrderSessionPhoneUseCount(state, normalizedOrderId, normalizedPhone),
    }
  }
  if (usedCount >= limit) {
    return { ok: false, usedCount, reason: 'phone-limit-reached' }
  }

  const phoneClaims = {
    ...(state.phoneClaims || {}),
    [normalizedOrderId]: [...claimedPhones, normalizedPhone],
  }
  const nextState = writeState({ ...state, phoneClaims })
  return {
    ok: true,
    reused: false,
    usedCount: getOrderSessionPhoneUseCount(nextState, normalizedOrderId, normalizedPhone),
  }
}

export function releaseOrderSessionPhone({ orderId = '', phone = '' } = {}) {
  const state = readState()
  const normalizedOrderId = String(orderId || '').trim()
  const normalizedPhone = normalizeClaimedPhone(phone)
  if (!normalizedOrderId || !normalizedPhone) {
    return { ok: false, removed: false, usedCount: 0, reason: 'invalid-session-or-phone' }
  }

  const claimedPhones = state.phoneClaims?.[normalizedOrderId] || []
  if (!claimedPhones.includes(normalizedPhone)) {
    return {
      ok: true,
      removed: false,
      usedCount: getOrderSessionUsedCount(state, normalizedOrderId),
    }
  }

  const remainingPhones = claimedPhones.filter((item) => item !== normalizedPhone)
  const phoneClaims = { ...(state.phoneClaims || {}) }
  if (remainingPhones.length) {
    phoneClaims[normalizedOrderId] = remainingPhones
  } else {
    delete phoneClaims[normalizedOrderId]
  }
  const nextState = writeState({ ...state, phoneClaims })
  return {
    ok: true,
    removed: true,
    usedCount: getOrderSessionUsedCount(nextState, normalizedOrderId),
  }
}

export function getNextCoolingOrderSession(maxReuse = 3, line = '') {
  const state = readState()
  const limit = Math.max(1, Number(maxReuse) || 3)
  const nowMs = Date.now()
  const normalizedLine = line ? normalizeTdxLine(line) : ''

  return Object.values(state.orderSessions || {})
    .filter((item) => item?.orderId && normalizeOrderSessionStatus(item?.status) !== 'invalid')
    .filter((item) => !normalizedLine || normalizeTdxLine(item.line) === normalizedLine)
    .map((item) => publicOrderSessionView(state, item))
    .filter((item) => item.usedCount < limit && isOrderSessionCooling(item, nowMs))
    .map((item) => {
      const untilMs = Date.parse(item.cooldownUntil || '')
      return {
        ...item,
        cooldownRemainingMs: Number.isFinite(untilMs) ? Math.max(0, untilMs - nowMs) : 0,
      }
    })
    .sort((a, b) => a.cooldownRemainingMs - b.cooldownRemainingMs)[0] || null
}

export function getReusableOrderSession(maxReuse = 3, line = '') {
  return listReusableOrderSessions(maxReuse, line)[0] || null
}

export function saveOrderSession(session) {
  const state = readState()
  const orderId = String(session?.orderId || '').trim()
  if (!orderId) return state
  const existing = state.orderSessions?.[orderId] || {}
  const wxCode = String(session?.wxCode || existing.wxCode || '').trim()
  const line = normalizeTdxLine(session?.line || existing.line || state.currentLine)
  const status = normalizeOrderSessionStatus(session?.status)
  const keepCooldown = status === 'cooldown'
  const now = new Date().toISOString()
  const usage = { ...state.usage }
  if (!Object.prototype.hasOwnProperty.call(usage, orderId)) {
    usage[orderId] = 0
  }
  const orderSessions = {
    ...(state.orderSessions || {}),
    [orderId]: {
      orderId,
      wxCode,
      line,
      savedAt: existing.savedAt || now,
      updatedAt: now,
      status,
      cooldownUntil: keepCooldown ? String(session?.cooldownUntil || existing.cooldownUntil || '').trim() : '',
      lastFailure: String(session?.lastFailure || (keepCooldown ? existing.lastFailure : '') || '').trim().slice(0, 1000),
      lastCheckedAt: now,
    },
  }
  return writeState({
    ...state,
    currentOrderId: orderId,
    currentLine: line,
    currentWxCode: wxCode || (state.currentOrderId === orderId ? state.currentWxCode : ''),
    orderSessions,
    usage,
  })
}

export function removeOrderSession(orderId, options = {}) {
  const state = readState()
  const normalizedOrderId = String(orderId || '').trim()
  if (!normalizedOrderId) return state
  const orderSessions = { ...(state.orderSessions || {}) }
  const usage = { ...state.usage }
  const phoneClaims = { ...(state.phoneClaims || {}) }
  const legacyUsage = { ...(state.legacyUsage || {}) }
  const item = orderSessions[normalizedOrderId] || null
  const claimedPhones = phoneClaims[normalizedOrderId] || []
  const usedCount = getOrderSessionUsedCount(state, normalizedOrderId)
  const nowText = new Date().toISOString()
  const retiredSessions = { ...(state.retiredSessions || {}) }
  if (item || claimedPhones.length || Object.prototype.hasOwnProperty.call(usage, normalizedOrderId)) {
    const retiredReason = String(options?.reason || 'removed').trim().slice(0, 120)
    const retiredMessage = sanitizeAuditMessage(options?.message || item?.lastFailure)
    const source = String(options?.source || '').trim().slice(0, 120)
    retiredSessions[normalizedOrderId] = {
      orderId: normalizedOrderId,
      line: normalizeTdxLine(item?.line || state.currentLine),
      usedCount,
      phoneCount: claimedPhones.length,
      savedAt: String(item?.savedAt || '').trim(),
      retiredAt: nowText,
      retiredReason,
      retiredMessage,
      source,
      status: normalizeOrderSessionStatus(item?.status),
      lastFailure: sanitizeAuditMessage(item?.lastFailure),
    }
    console.warn(
      `[tdx-session-retired] orderId=${normalizedOrderId} line=${normalizeTdxLine(item?.line || state.currentLine)} used=${usedCount} phoneCount=${claimedPhones.length} reason=${retiredReason} source=${source || 'unknown'} message=${retiredMessage || '-'}`,
    )
  }
  delete orderSessions[normalizedOrderId]
  delete usage[normalizedOrderId]
  delete phoneClaims[normalizedOrderId]
  delete legacyUsage[normalizedOrderId]
  return writeState({
    ...state,
    currentOrderId: state.currentOrderId === normalizedOrderId ? '' : state.currentOrderId,
    currentLine: state.currentOrderId === normalizedOrderId ? 'line1' : state.currentLine,
    currentWxCode: state.currentOrderId === normalizedOrderId ? '' : state.currentWxCode,
    orderSessions,
    retiredSessions,
    usage,
    phoneClaims,
    legacyUsage,
  })
}

export function markOrderSessionCooldown(orderId, { message = '', cooldownMs = 75000 } = {}) {
  const state = readState()
  const normalizedOrderId = String(orderId || '').trim()
  if (!normalizedOrderId) return state
  const now = new Date()
  const nowText = now.toISOString()
  const safeCooldownMs = Math.max(60000, Number(cooldownMs || 75000))
  const item = state.orderSessions?.[normalizedOrderId] || {
    orderId: normalizedOrderId,
    wxCode: state.currentOrderId === normalizedOrderId ? state.currentWxCode || '' : '',
    line: state.currentOrderId === normalizedOrderId ? state.currentLine : 'line1',
    savedAt: nowText,
    updatedAt: nowText,
    status: 'active',
    cooldownUntil: '',
    lastFailure: '',
    lastCheckedAt: nowText,
  }
  const usage = { ...state.usage }
  if (!Object.prototype.hasOwnProperty.call(usage, normalizedOrderId)) {
    usage[normalizedOrderId] = 0
  }
  const orderSessions = {
    ...(state.orderSessions || {}),
    [normalizedOrderId]: {
      ...item,
      status: 'cooldown',
      cooldownUntil: new Date(now.getTime() + safeCooldownMs).toISOString(),
      lastFailure: String(message || '').trim().slice(0, 1000),
      lastCheckedAt: nowText,
      updatedAt: nowText,
    },
  }
  return writeState({
    ...state,
    currentOrderId: normalizedOrderId,
    currentLine: normalizeTdxLine(item.line),
    currentWxCode: item.wxCode || (state.currentOrderId === normalizedOrderId ? state.currentWxCode : ''),
    orderSessions,
    usage,
  })
}

export function retireOrderSession(orderId, options = {}) {
  return removeOrderSession(orderId, options)
}

export function getSavedQmfSession() {
  const state = readState()
  return state.qmfSession?.token ? state.qmfSession : null
}

export function saveQmfSession(session = {}) {
  const state = readState()
  const token = String(session?.token || '').trim()
  if (!token) return state.qmfSession || null
  const now = new Date().toISOString()
  const current = state.qmfSession || null
  const nextSession = {
    token,
    savedAt: String(session?.savedAt || current?.savedAt || now).trim(),
    updatedAt: now,
    source: String(session?.source || current?.source || '').trim(),
  }
  writeState({
    ...state,
    qmfSession: nextSession,
  })
  return nextSession
}

export function clearSavedQmfSession() {
  const state = readState()
  return writeState({
    ...state,
    qmfSession: null,
  })
}

export async function saveTdxLoginToken({ phone, token, orderId = '', wxCode = '' } = {}) {
  const normalizedPhone = String(phone || '').trim()
  const normalizedToken = String(token || '').trim()
  if (!normalizedPhone || !normalizedToken) return null
  if (normalizedToken.toLowerCase().startsWith('tdx_')) {
    return null
  }
  const config = await getTdxConfig()
  const sanitizedToken = sanitizeStoredLoginToken(normalizedToken, { mode: config.mode })
  if (!sanitizedToken) {
    return null
  }
  const sanitizedWxCode = sanitizeStoredWxCode(wxCode)
  await ensureTokenStorageReady()
  return upsertTdxLoginToken({
    phone: normalizedPhone,
    token: sanitizedToken,
    orderId,
    wxCode: sanitizedWxCode,
  })
}

export async function getSavedTdxLoginToken(phone = '') {
  const normalizedPhone = String(phone || '').trim()
  if (!normalizedPhone) return null
  await ensureTokenStorageReady()
  const rows = await query(
    'SELECT Token, OrderId, WxCode, SavedAt, UpdatedAt FROM t_tdx_login_token WHERE Phone = ? LIMIT 1',
    [normalizedPhone],
  )
  const rowToken = normalizeTokenRow(rows[0])
  if (rowToken?.token) return rowToken

  // Compatibility fallback for tokens saved by older builds before MySQL storage.
  const state = readState()
  return state.loginTokens?.[normalizedPhone] || null
}

export async function getLatestSavedTdxLoginToken() {
  await ensureTokenStorageReady()
  const config = await getTdxConfig()
  const rows = await query(
    'SELECT Phone, Token, OrderId, WxCode, SavedAt, UpdatedAt FROM t_tdx_login_token ORDER BY UpdatedAt DESC LIMIT 20',
  )
  for (const row of rows) {
    const tokenRow = normalizeTokenRow(row)
    const token = sanitizeStoredLoginToken(tokenRow?.token || '', { mode: config.mode })
    if (token) {
      return {
        phone: String(row.Phone || row.phone || ''),
        ...tokenRow,
        token,
      }
    }
  }

  const state = readState()
  const entries = Object.entries(state.loginTokens || {})
  for (const [phone, item] of entries) {
    const token = String(item?.token || '').trim()
    if (!token) continue
    return {
      phone: String(phone || '').trim(),
      token,
      orderId: String(item?.orderId || '').trim(),
      wxCode: String(item?.wxCode || '').trim(),
      savedAt: String(item?.savedAt || ''),
    }
  }
  return null
}

export async function listSavedTdxLoginTokens(limit = 20) {
  const safeLimit = Math.max(1, Math.min(100, Number(limit || 20)))
  await ensureTokenStorageReady()
  const config = await getTdxConfig()
  const items = []
  const seen = new Set()

  const addItem = (phone, tokenRow) => {
    const normalizedPhone = String(phone || '').trim()
    const token = sanitizeStoredLoginToken(tokenRow?.token || '', { mode: config.mode })
    if (!normalizedPhone || !token) return
    const uniqueKey = `${normalizedPhone}\u0000${token}`
    if (seen.has(uniqueKey)) return
    seen.add(uniqueKey)
    items.push({
      phone: normalizedPhone,
      ...tokenRow,
      token,
    })
  }

  const rows = await query(
    `SELECT Phone, Token, OrderId, WxCode, SavedAt, UpdatedAt
       FROM t_tdx_login_token
      ORDER BY UpdatedAt DESC
      LIMIT ${safeLimit}`,
  )
  for (const row of rows) {
    addItem(String(row.Phone || row.phone || ''), normalizeTokenRow(row))
    if (items.length >= safeLimit) {
      return items
    }
  }

  const state = readState()
  for (const [phone, item] of Object.entries(state.loginTokens || {})) {
    addItem(phone, normalizeTokenRow(item))
    if (items.length >= safeLimit) {
      break
    }
  }

  return items
}

export function markOrderUsed(orderId) {
  const state = readState()
  const normalizedOrderId = String(orderId || '').trim()
  if (!normalizedOrderId) return 0
  const usage = { ...state.usage, [normalizedOrderId]: (state.usage[normalizedOrderId] || 0) + 1 }
  const existingLegacyUsage = Object.prototype.hasOwnProperty.call(state.legacyUsage || {}, normalizedOrderId)
    ? state.legacyUsage[normalizedOrderId]
    : state.usage?.[normalizedOrderId]
  const legacyUsage = {
    ...(state.legacyUsage || {}),
    [normalizedOrderId]: normalizeUsageCount(existingLegacyUsage) + 1,
  }
  const nextState = writeState({ ...state, usage, legacyUsage })
  return getOrderSessionUsedCount(nextState, normalizedOrderId)
}

export function clearCurrentOrderSession(line = '', options = {}) {
  const state = readState()
  const retirementOptions = {
    reason: 'manual-rotate',
    source: 'runtime-rotate',
    ...options,
  }
  const normalizedLine = line ? normalizeTdxLine(line) : ''
  if (normalizedLine && state.currentOrderId) {
    const current = state.orderSessions?.[state.currentOrderId] || null
    if (current && normalizeTdxLine(current.line) === normalizedLine) {
      return removeOrderSession(state.currentOrderId, retirementOptions)
    }
    const latest = Object.values(state.orderSessions || {})
      .filter((item) => item?.orderId && normalizeTdxLine(item.line) === normalizedLine)
      .sort((a, b) => orderSessionSortValue(b) - orderSessionSortValue(a))[0] || null
    if (latest?.orderId) {
      return removeOrderSession(latest.orderId, retirementOptions)
    }
  }
  if (!state.currentOrderId) {
    return writeState({
      ...state,
      currentLine: normalizeTdxLine(state.currentLine),
      currentWxCode: '',
    })
  }
  return removeOrderSession(state.currentOrderId, retirementOptions)
}

export function rememberLastPayload(payload) {
  const state = readState()
  return writeState({
    ...state,
    lastPayload: payload,
  })
}

