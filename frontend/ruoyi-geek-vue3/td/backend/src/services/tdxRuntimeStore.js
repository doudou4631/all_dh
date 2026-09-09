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
  usage: {},
  qmfSession: null,
  dengfengSession: null,
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
  return String(value || '').trim().toLowerCase() === 'line2' ? 'line2' : 'line1'
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
  const currentOrderId = String(next.currentOrderId || '').trim()
  const currentWxCode = String(next.currentWxCode || '').trim()
  const currentLine = normalizeTdxLine(next.currentLine || next.orderSessions?.[currentOrderId]?.line)
  const qmfSession = normalizeAuthSession(next.qmfSession)
  const dengfengSession = normalizeAuthSession(next.dengfengSession)
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
    usage,
    qmfSession,
    dengfengSession,
    loginTokens: next.loginTokens || {},
    lastPayload: next.lastPayload ?? null,
  }
}

function orderSessionSortValue(item = {}) {
  const timestamp = Date.parse(item.updatedAt || item.savedAt || '')
  return Number.isFinite(timestamp) ? timestamp : 0
}

function isOrderSessionCooling(item = {}, nowMs = Date.now()) {
  if (!item) return false
  const cooldownUntilMs = Date.parse(item.cooldownUntil || '')
  return item.status === 'cooldown' && Number.isFinite(cooldownUntilMs) && cooldownUntilMs > nowMs
}

function publicOrderSessionView(state = {}, item = {}) {
  const usedCount = Number(state.usage?.[item.orderId] || 0)
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
  return {
    currentOrderId: state.currentOrderId,
    currentWxCode: state.currentWxCode,
    currentLine: normalizeTdxLine(state.currentLine),
    orderSessionCount: orderSessions.length,
    orderSessions,
    usage: state.usage,
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
    dengfengSession: state.dengfengSession?.token
      ? {
        hasToken: true,
        savedAt: state.dengfengSession.savedAt || '',
        updatedAt: state.dengfengSession.updatedAt || '',
        source: state.dengfengSession.source || '',
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

export function removeOrderSession(orderId) {
  const state = readState()
  const normalizedOrderId = String(orderId || '').trim()
  if (!normalizedOrderId) return state
  const orderSessions = { ...(state.orderSessions || {}) }
  const usage = { ...state.usage }
  delete orderSessions[normalizedOrderId]
  delete usage[normalizedOrderId]
  return writeState({
    ...state,
    currentOrderId: state.currentOrderId === normalizedOrderId ? '' : state.currentOrderId,
    currentLine: state.currentOrderId === normalizedOrderId ? 'line1' : state.currentLine,
    currentWxCode: state.currentOrderId === normalizedOrderId ? '' : state.currentWxCode,
    orderSessions,
    usage,
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

export function getSavedDengfengSession() {
  const state = readState()
  return state.dengfengSession?.token ? state.dengfengSession : null
}

export function saveDengfengSession(session = {}) {
  const state = readState()
  const token = String(session?.token || '').trim()
  if (!token) return state.dengfengSession || null
  const now = new Date().toISOString()
  const current = state.dengfengSession || null
  const nextSession = {
    token,
    savedAt: String(session?.savedAt || current?.savedAt || now).trim(),
    updatedAt: now,
    source: String(session?.source || current?.source || '').trim(),
  }
  writeState({
    ...state,
    dengfengSession: nextSession,
  })
  return nextSession
}

export function clearSavedDengfengSession() {
  const state = readState()
  return writeState({
    ...state,
    dengfengSession: null,
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
  writeState({ ...state, usage })
  return usage[normalizedOrderId]
}

export function clearCurrentOrderSession(line = '') {
  const state = readState()
  const normalizedLine = line ? normalizeTdxLine(line) : ''
  if (normalizedLine && state.currentOrderId) {
    const current = state.orderSessions?.[state.currentOrderId] || null
    if (current && normalizeTdxLine(current.line) === normalizedLine) {
      return removeOrderSession(state.currentOrderId)
    }
    const latest = Object.values(state.orderSessions || {})
      .filter((item) => item?.orderId && normalizeTdxLine(item.line) === normalizedLine)
      .sort((a, b) => orderSessionSortValue(b) - orderSessionSortValue(a))[0] || null
    if (latest?.orderId) {
      return removeOrderSession(latest.orderId)
    }
  }
  if (!state.currentOrderId) {
    return writeState({
      ...state,
      currentLine: normalizeTdxLine(state.currentLine),
      currentWxCode: '',
    })
  }
  return removeOrderSession(state.currentOrderId)
}

export function rememberLastPayload(payload) {
  const state = readState()
  return writeState({
    ...state,
    lastPayload: payload,
  })
}

