const ACCOUNT_KEY = 'profile_user_account'
const RECORDS_MAP_KEY = 'profile_query_records_map'
const LEGACY_RECORDS_KEY = 'profile_query_records'
const MAX_RECORDS_PER_ACCOUNT = 120

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
    return (localStorage.getItem(ACCOUNT_KEY) || '').trim()
  } catch (error) {
    return ''
  }
}

function pad(num) {
  return num < 10 ? `0${num}` : String(num)
}

function nowText() {
  const date = new Date()
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(
    date.getMinutes()
  )}`
}

function normalizeType(type) {
  return String(type || '').trim() === '批量查询' ? '批量查询' : '单号查询'
}

function normalizeMarked(marked) {
  const value = Number(marked)
  if (Number.isNaN(value) || value < 0) return 0
  return Math.round(value)
}

function normalizePhone(phone) {
  const raw = String(phone == null ? '' : phone).trim()
  if (!raw) return ''
  const digits = raw.replace(/\D/g, '')
  if (/^\d{7,15}$/.test(digits)) return digits
  return raw
}

function normalizeMarkedPlatforms(value) {
  let list = []
  if (Array.isArray(value)) {
    list = value
  } else if (typeof value === 'string') {
    list = value.split(/[、,，;；|·]+/)
  } else if (value != null) {
    list = [value]
  }
  const seen = {}
  return list
    .map((item) => String(item == null ? '' : item).trim())
    .filter((name) => {
      if (!name) return false
      if (seen[name]) return false
      seen[name] = true
      return true
    })
}

function normalizeRecord(record) {
  if (!record) return null
  const phone = normalizePhone(record.phone)
  if (!phone) return null

  const markedPlatforms = normalizeMarkedPlatforms(
    record.markedPlatforms != null ? record.markedPlatforms : record.platforms
  )
  let marked = normalizeMarked(record.marked)
  if (!marked && markedPlatforms.length) {
    marked = markedPlatforms.length
  }
  return {
    phone,
    type: normalizeType(record.type),
    marked,
    markedPlatforms,
    time: String(record.time || '').trim() || nowText()
  }
}

function readMap() {
  const map = readJson(RECORDS_MAP_KEY, {})
  return map && typeof map === 'object' ? map : {}
}

function writeMap(map) {
  writeJson(RECORDS_MAP_KEY, map)
}

function migrateLegacyRecords(account) {
  if (!account) return
  const legacy = readJson(LEGACY_RECORDS_KEY, [])
  if (!Array.isArray(legacy) || !legacy.length) return

  const map = readMap()
  if (Array.isArray(map[account]) && map[account].length) return

  const normalized = legacy.map(normalizeRecord).filter(Boolean)
  if (!normalized.length) return

  map[account] = normalized.slice(0, MAX_RECORDS_PER_ACCOUNT)
  writeMap(map)
  try {
    localStorage.removeItem(LEGACY_RECORDS_KEY)
  } catch (error) {}
}

export function getRecords(account) {
  const targetAccount = String(account || getAccount() || '').trim()
  if (!targetAccount) return []
  migrateLegacyRecords(targetAccount)
  const map = readMap()
  const list = Array.isArray(map[targetAccount]) ? map[targetAccount] : []
  return list.map(normalizeRecord).filter(Boolean)
}

export function addRecords(records, account) {
  const targetAccount = String(account || getAccount() || '').trim()
  if (!targetAccount) return []
  if (!Array.isArray(records) || !records.length) return getRecords(targetAccount)

  const items = records.map(normalizeRecord).filter(Boolean)
  if (!items.length) return getRecords(targetAccount)

  const map = readMap()
  const oldList = Array.isArray(map[targetAccount]) ? map[targetAccount] : []
  map[targetAccount] = items.concat(oldList).slice(0, MAX_RECORDS_PER_ACCOUNT)
  writeMap(map)
  return map[targetAccount]
}

export function addRecord(record, account) {
  return addRecords([record], account)
}

export function addSingleRecord(phone, marked, markedPlatforms, account) {
  let targetAccount = account
  let platforms = markedPlatforms
  if (arguments.length === 3 && !Array.isArray(markedPlatforms)) {
    targetAccount = markedPlatforms
    platforms = []
  }
  return addRecord(
    {
      phone,
      type: '单号查询',
      marked,
      markedPlatforms: platforms,
      time: nowText()
    },
    targetAccount
  )
}

export function addBatchRecords(results, account) {
  if (!Array.isArray(results) || !results.length) {
    return getRecords(account)
  }
  const time = nowText()
  const records = results.map((row) => {
    let marked = 0
    let markedPlatforms = []
    if (Array.isArray(row?.markedItems)) {
      marked = row.markedItems.length
      markedPlatforms = row.markedItems.map((item) => item?.platform)
    } else if (row && typeof row === 'object') {
      marked = normalizeMarked(row.marked)
      markedPlatforms = normalizeMarkedPlatforms(row.markedPlatforms != null ? row.markedPlatforms : row.platforms)
    }
    return {
      phone: row?.phone,
      type: '批量查询',
      marked,
      markedPlatforms,
      time
    }
  })
  return addRecords(records, account)
}
