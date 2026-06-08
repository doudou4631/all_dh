const ACCOUNT_KEY = 'profile_user_account'
const TODAY_QUERY_KEY = 'profile_today_query'

function pad(num) {
  return num < 10 ? `0${num}` : String(num)
}

function getTodayDateStr() {
  const date = new Date()
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export { getTodayDateStr }

export function getAccount() {
  try {
    return localStorage.getItem(ACCOUNT_KEY) || ''
  } catch (error) {
    return ''
  }
}

function readMap() {
  try {
    const raw = localStorage.getItem(TODAY_QUERY_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch (error) {
    return {}
  }
}

function writeMap(map) {
  try {
    localStorage.setItem(TODAY_QUERY_KEY, JSON.stringify(map))
  } catch (error) {}
}

export function getTodayQueryCount(account) {
  const targetAccount = account || getAccount()
  if (!targetAccount) return 0
  const today = getTodayDateStr()
  const map = readMap()
  const item = map[targetAccount]
  if (!item || item.date !== today) return 0
  return Number(item.count) || 0
}

export function recordQuery(account, count) {
  const targetAccount = account || getAccount()
  if (!targetAccount) return 0

  let delta = Number(count)
  if (Number.isNaN(delta) || delta <= 0) delta = 1

  const today = getTodayDateStr()
  const map = readMap()
  let item = map[targetAccount]
  if (!item || item.date !== today) {
    item = { date: today, count: 0 }
  }
  item.count = (Number(item.count) || 0) + delta
  map[targetAccount] = item
  writeMap(map)
  return item.count
}

export function recordQueryForCurrentUser(count) {
  return recordQuery(getAccount(), count)
}
