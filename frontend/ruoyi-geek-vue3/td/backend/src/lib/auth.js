import { createHmac, randomBytes, timingSafeEqual } from 'node:crypto'
import { validateLiveCredentials } from '../data/liveUserStore.js'

const configuredSecret = process.env.TD_SESSION_SECRET || process.env.RIGHTCONTROL_SESSION_SECRET
const secret = configuredSecret || randomBytes(32).toString('base64url')
const revokedSessionIds = new Set()
const DEFAULT_SESSION_TTL_SECONDS = 12 * 60 * 60

if (!configuredSecret) {
  console.warn('[auth] TD_SESSION_SECRET is not configured; sessions will be invalidated when the service restarts.')
}

function base64urlEncode(value) {
  return Buffer.from(String(value), 'utf8').toString('base64url')
}

function base64urlDecode(value = '') {
  return Buffer.from(String(value), 'base64url').toString('utf8')
}

function sign(value = '') {
  return createHmac('sha256', secret).update(String(value)).digest('base64url')
}

function signaturesMatch(actual = '', expected = '') {
  const actualBuffer = Buffer.from(String(actual))
  const expectedBuffer = Buffer.from(String(expected))
  return actualBuffer.length === expectedBuffer.length && timingSafeEqual(actualBuffer, expectedBuffer)
}

function sessionTtlSeconds(value = process.env.TD_SESSION_TTL_SECONDS) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return DEFAULT_SESSION_TTL_SECONDS
  return Math.max(300, Math.min(30 * 24 * 60 * 60, Math.round(parsed)))
}

function decodeSession(token, { allowRevoked = false } = {}) {
  const parts = String(token || '').split('.')
  if (parts.length !== 3 || !['td1', 'rc2'].includes(parts[0])) return null
  const [prefix, encoded, signature] = parts
  if (!signaturesMatch(signature, sign(encoded))) return null
  try {
    const parsed = JSON.parse(base64urlDecode(encoded))
    if (!parsed?.user || !parsed?.sessionId || !parsed?.expiresAt) return null
    if (!allowRevoked && revokedSessionIds.has(parsed.sessionId)) return null
    if (Date.parse(parsed.expiresAt) <= Date.now()) {
      revokedSessionIds.delete(parsed.sessionId)
      return null
    }
    return {
      token: `${prefix}.${encoded}.${signature}`,
      user: parsed.user,
      sessionId: parsed.sessionId,
      issuedAt: parsed.issuedAt || '',
      expiresAt: parsed.expiresAt,
    }
  } catch {
    return null
  }
}

const defaultModules = [
  { module: '泰迪熊标记', read: '允许', create: '允许', edit: '允许', delete: '禁止' },
]

export async function validateCredentials({ username, password }) {
  const liveUser = await validateLiveCredentials(String(username || '').trim(), String(password || ''))
  return liveUser || null
}

export async function enrichUserProfile(user) {
  return {
    ...user,
    access: {
      roleId: user.roleId,
      roleName: user.role,
      modules: defaultModules,
      menuUrls: ['/orders/tdx-sms-receive'],
    },
  }
}

export function createSession(user, options = {}) {
  const now = options.now instanceof Date ? options.now : new Date()
  const ttlSeconds = sessionTtlSeconds(options.ttlSeconds)
  const payload = {
    user,
    sessionId: randomBytes(18).toString('base64url'),
    issuedAt: now.toISOString(),
    expiresAt: new Date(now.getTime() + ttlSeconds * 1000).toISOString(),
  }
  const encoded = base64urlEncode(JSON.stringify(payload))
  const token = `td1.${encoded}.${sign(encoded)}`
  return { token, ...payload }
}

export function getSession(token) {
  return decodeSession(token)
}

export function removeSession(token) {
  const session = decodeSession(token, { allowRevoked: true })
  if (!session) return false
  revokedSessionIds.add(session.sessionId)
  return true
}

export function parseBearerToken(value = '') {
  if (!value.startsWith('Bearer ')) return ''
  return value.slice(7).trim()
}
