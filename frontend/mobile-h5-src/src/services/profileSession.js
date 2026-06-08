const STORAGE_KEY = 'profile_user_account'
const PASSWORD_KEY = 'profile_password_map'
const POINTS_KEY = 'profile_user_points'
const TOKEN_KEY = 'profile_user_token'
const RECHARGE_RECORDS_KEY = 'profile_recharge_records'
const DEFAULT_POINTS = 100

const DEMO_ACCOUNTS = {
  admin: '123456',
  test: 'test123'
}

function trim(value) {
  return String(value || '').trim()
}

function readJson(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch (error) {
    return fallback
  }
}

function writeJson(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch (error) {}
}

export function getAccount() {
  try {
    return localStorage.getItem(STORAGE_KEY) || ''
  } catch (error) {
    return ''
  }
}

export function setAccount(account) {
  try {
    const value = trim(account)
    if (value) localStorage.setItem(STORAGE_KEY, value)
    else localStorage.removeItem(STORAGE_KEY)
  } catch (error) {}
}

export function getToken() {
  try {
    return trim(localStorage.getItem(TOKEN_KEY))
  } catch (error) {
    return ''
  }
}

export function setToken(token) {
  try {
    const value = trim(token)
    if (value) localStorage.setItem(TOKEN_KEY, value)
    else localStorage.removeItem(TOKEN_KEY)
  } catch (error) {}
}

export function isLoggedIn() {
  return !!(trim(getAccount()) && trim(getToken()))
}

export function clearLogin() {
  setAccount('')
  setToken('')
}

export function getPasswordMap() {
  return readJson(PASSWORD_KEY, {})
}

export function getAccountPassword(account) {
  const target = trim(account)
  if (!target) return ''
  const map = getPasswordMap()
  if (map[target]) return map[target]
  if (Object.prototype.hasOwnProperty.call(DEMO_ACCOUNTS, target)) {
    return DEMO_ACCOUNTS[target]
  }
  return ''
}

export function setAccountPassword(account, password) {
  const target = trim(account)
  if (!target) return
  const map = getPasswordMap()
  map[target] = String(password || '')
  writeJson(PASSWORD_KEY, map)
}

export function maskAccount(account) {
  const value = trim(account)
  if (!value) return value
  if (value.length <= 2) return value
  if (value.length <= 4) return value.slice(0, 1) + '***'
  return value.slice(0, 2) + '***' + value.slice(-2)
}

export function getPoints(account) {
  const targetAccount = trim(account || getAccount())
  if (!targetAccount || !trim(getToken())) return 0
  const map = readJson(POINTS_KEY, {})
  if (Object.prototype.hasOwnProperty.call(map, targetAccount)) {
    return Number(map[targetAccount]) || 0
  }
  return DEFAULT_POINTS
}

export function setPoints(value, account) {
  const targetAccount = trim(account || getAccount())
  if (!targetAccount) return
  const points = Number(value)
  if (Number.isNaN(points)) return
  const map = readJson(POINTS_KEY, {})
  map[targetAccount] = Math.max(0, Math.round(points))
  writeJson(POINTS_KEY, map)
}

export function getRechargeRecords() {
  const list = readJson(RECHARGE_RECORDS_KEY, null)
  if (Array.isArray(list) && list.length) return list
  return [
    { title: '批量查询套餐', amount: '¥79.00', remark: '100 次查询', time: '2026-05-28 16:30' },
    { title: '体验包', amount: '¥19.90', remark: '20 次查询', time: '2026-05-20 09:12' }
  ]
}
