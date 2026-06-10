const DEFAULT_API_BASE = '/prod-api/'
const DEFAULT_TEDDY_PROTOCOL_BASE = 'https://www.teddymobile.cn'
const ICON_BASE = '/mobile-h5/assets/icons/'
const FREE_QUERY_DEVICE_ID_STORAGE_KEY = 'free_query_device_id'
const PROFILE_TOKEN_KEY = 'profile_user_token'
const PROFILE_SESSION_KEY = 'biaoji_user_session'
const TEDDY_CLIENT_FLAG = 2

const PLATFORM_ICONS = {
  泰迪熊: ICON_BASE + 'teddy.png',
  腾讯: ICON_BASE + 'tencent.png',
  '360': ICON_BASE + '360.png',
  '360手机卫士': ICON_BASE + '360.png',
  百度: ICON_BASE + 'baidu.ico',
  搜狗: ICON_BASE + 'sogou.ico',
  移动高频: ICON_BASE + 'mobile.png',
  联通: ICON_BASE + 'unicom.svg',
  联通管家: ICON_BASE + 'unicom.svg',
  电话邦: ICON_BASE + 'dianhuabang.ico',
  小米手机: '/mobile-h5/assets/icons/xiaomi.png?v=2'
}

function trim(value) {
  return String(value || '').trim()
}

function ensureTrailingSlash(url) {
  const text = trim(url)
  if (!text) return ''
  return text.endsWith('/') ? text : `${text}/`
}

function resolveTeddyProtocolBase(base) {
  const text = trim(base)
  return text || DEFAULT_TEDDY_PROTOCOL_BASE
}

function createRandomDeviceId() {
  try {
    if (typeof crypto !== 'undefined' && crypto && typeof crypto.randomUUID === 'function') {
      return `fq_${crypto.randomUUID().replace(/-/g, '')}`
    }
  } catch (error) {}
  return `fq_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 12)}`
}

function getOrCreateDeviceId() {
  if (typeof window === 'undefined') {
    return createRandomDeviceId()
  }
  try {
    const exists = trim(window.localStorage.getItem(FREE_QUERY_DEVICE_ID_STORAGE_KEY))
    if (exists) return exists
    const created = createRandomDeviceId()
    window.localStorage.setItem(FREE_QUERY_DEVICE_ID_STORAGE_KEY, created)
    return created
  } catch (error) {
    return createRandomDeviceId()
  }
}

function getPhoneLikeAccount(account) {
  const text = trim(account)
  return /^\d{11}$/.test(text) ? text : ''
}

async function requestJson(url, method = 'GET', data = null, headers = {}) {
  const response = await fetch(url, {
    method,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      ...headers
    },
    body: method === 'GET' ? null : JSON.stringify(data || {})
  })

  let payload = {}
  try {
    payload = await response.json()
  } catch (error) {
    payload = {}
  }

  if (!response.ok) {
    throw new Error(payload?.msg || '网络错误，请重试')
  }
  return payload
}

function normalizeBatchPhones(phones) {
  if (!Array.isArray(phones)) return []
  const seen = {}
  const list = []
  phones.forEach((item) => {
    const phone = String(item || '').replace(/\D/g, '').trim()
    if (!/^\d{7,15}$/.test(phone)) return
    if (seen[phone]) return
    seen[phone] = true
    list.push(phone)
  })
  return list.slice(0, 20)
}

export function getProfileToken() {
  if (typeof window === 'undefined') return ''
  try {
    const profileToken = trim(window.localStorage.getItem(PROFILE_TOKEN_KEY))
    if (profileToken) return profileToken
    const rawSession = window.localStorage.getItem(PROFILE_SESSION_KEY)
    if (!rawSession) return ''
    const parsedSession = JSON.parse(rawSession)
    return trim(parsedSession?.token)
  } catch (error) {
    return ''
  }
}

export function isRegisteredUser() {
  return !!getProfileToken()
}
function ensureRegisteredUserForTeddySms() {
  if (!isRegisteredUser()) {
    throw new Error('请先登录注册用户后再使用短信处理')
  }
}

function isTeddyPlatformName(name) {
  return trim(name).indexOf('泰迪') >= 0
}

function parseStatus(item) {
  if (item && typeof item.error === 'string' && item.error.trim()) {
    return { status: '查询失败', process: 0 }
  }
  const firstPlatform = item?.data?.platformResults?.[0]
  if (!firstPlatform) {
    return { status: '查询失败', process: 0 }
  }

  const platformName = trim(item?.platformName || item?.platform)
  const status = trim(firstPlatform.status)
  const teddy = isTeddyPlatformName(platformName)
  const hasToken = !!getProfileToken()

  if (status.startsWith('yes-')) {
    const statusText = trim(status.slice(4))
    if (statusText.includes('泰迪未来标记已取消') || statusText.includes('同步时间')) {
      return { status: '无标记', process: 0 }
    }
    if (platformName === '移动高频' && (!statusText || statusText === '普通标记')) {
      return { status: '高频拦截', process: 1 }
    }
    if (teddy && hasToken) {
      const normalTeddyStatus =
        !statusText ||
        statusText === '普通标记' ||
        statusText === '有标记' ||
        statusText.startsWith('普通标记')
      if (normalTeddyStatus) {
        return { status: statusText.startsWith('普通标记') ? statusText : '普通标记', process: 1 }
      }
      return { status: statusText, process: 0 }
    }
    return { status: statusText || '有标记', process: 1 }
  }

  if (status === 'yes') {
    if (platformName === '移动高频') {
      return { status: '高频拦截', process: 1 }
    }
    if (teddy && hasToken) {
      return { status: '普通标记', process: 1 }
    }
    return { status: '有标记', process: 1 }
  }

  if (status === 'no' || status.startsWith('no')) {
    return { status: '无标记', process: 0 }
  }
  return { status: '查询失败', process: 0 }
}

function transformSearchResponse(response) {
  if (response?.code === 42901 || response?.code === 42902 || response?.code === 42903) {
    return { code: 1, msg: response?.msg || '查询失败' }
  }
  if (response?.code !== 200 && response?.code !== 0) {
    return { code: 1, msg: response?.msg || '查询失败' }
  }

  const rawResults = Array.isArray(response?.data?.results) ? response.data.results : []
  const results = rawResults.map((item) => {
    const parsed = parseStatus(item)
    return {
      platform: item?.platformName || '',
      status: parsed.status,
      process: parsed.process
    }
  })

  return {
    code: 0,
    data: {
      results,
      expire_time: null
    }
  }
}

export function isMarked(item) {
  if (!item) return false
  if (item.process === 1) return true
  const status = trim(item.status)
  if (!status) return false
  if (status === '无标记' || status === '未标记' || status === '查询失败' || status === '未开放' || status === '-') {
    return false
  }
  return status !== '无标记'
}

export function isTeddyNormalMarkItem(item) {
  if (!item || typeof item !== 'object') return false
  const name = trim(item.platform)
  if (!name || !name.includes('泰迪')) return false
  const status = trim(item.status)
  return status === '普通标记' || status.startsWith('普通标记')
}

export function getPlatformIcon(name) {
  const platform = trim(name)
  if (PLATFORM_ICONS[platform]) return PLATFORM_ICONS[platform]
  if (platform.includes('360')) return PLATFORM_ICONS['360']
  if (platform.includes('泰迪')) return PLATFORM_ICONS['泰迪熊']
  if (platform.includes('腾讯')) return PLATFORM_ICONS['腾讯']
  if (platform.includes('百度')) return PLATFORM_ICONS['百度']
  if (platform.includes('搜狗')) return PLATFORM_ICONS['搜狗']
  if (platform.includes('移动')) return PLATFORM_ICONS['移动高频']
  if (platform.includes('联通')) return PLATFORM_ICONS['联通管家']
  if (platform.includes('电话邦')) return PLATFORM_ICONS['电话邦']
  if (platform.includes('小米')) return PLATFORM_ICONS['小米手机']
  return ICON_BASE + '360.png'
}

export async function fetchSingleQuery(phone, apiBase) {
  const targetPhone = trim(phone)
  if (!targetPhone) {
    throw new Error('未提供手机号码')
  }

  const headers = {}
  const token = getProfileToken()
  if (token) {
    headers['X-Free-Token'] = token
  }

  const payload = await requestJson(
    `${ensureTrailingSlash(apiBase || DEFAULT_API_BASE)}server/freeQuery/single`,
    'POST',
    {
      phone: targetPhone,
      deviceId: getOrCreateDeviceId()
    },
    headers
  )

  const transformed = transformSearchResponse(payload)
  if (transformed.code !== 0) {
    throw new Error(transformed.msg || '查询失败')
  }
  return transformed.data
}

export async function requestRemoteLogin(account, password, apiBase) {
  const loginAccount = trim(account)
  const loginPassword = String(password || '')
  if (!loginAccount || !loginPassword) {
    throw new Error('请输入账号和密码')
  }

  const payload = {
    account: loginAccount,
    password: loginPassword
  }
  const phoneAccount = getPhoneLikeAccount(loginAccount)
  if (phoneAccount) {
    payload.phone = phoneAccount
  }

  const response = await requestJson(
    `${ensureTrailingSlash(apiBase || DEFAULT_API_BASE)}server/freeQuery/login`,
    'POST',
    payload
  )
  const code = Number(response?.code)
  if (code !== 0 && code !== 200) {
    throw new Error(response?.msg || '登录失败')
  }
  return response?.data || {}
}

export async function fetchQueryRecords(token, apiBase) {
  const authToken = trim(token || getProfileToken())
  if (!authToken) {
    throw new Error('请先登录后查看查询记录')
  }

  const response = await requestJson(
    `${ensureTrailingSlash(apiBase || DEFAULT_API_BASE)}server/freeQuery/records`,
    'GET',
    null,
    {
      'X-Free-Token': authToken
    }
  )

  const code = Number(response?.code)
  if (code !== 0 && code !== 200) {
    throw new Error(response?.msg || '获取查询记录失败')
  }
  const data = response?.data
  return Array.isArray(data) ? data : []
}


function buildTeddyProtocolPayload(phone, extra) {
  const payload = {
    phone: trim(phone),
    clientFlag: TEDDY_CLIENT_FLAG,
    complainantPhone: ''
  }
  if (extra && typeof extra === 'object') {
    Object.keys(extra).forEach((key) => {
      payload[key] = extra[key]
    })
  }
  return payload
}
export async function fetchBatchQuery(phones, token, apiBase) {
  const authToken = trim(token || getProfileToken())
  if (!authToken) {
    throw new Error('请先登录后再批量查询')
  }
  const normalizedPhones = normalizeBatchPhones(phones)
  if (!normalizedPhones.length) {
    throw new Error('请输入至少一个有效号码')
  }

  const response = await requestJson(
    `${ensureTrailingSlash(apiBase || DEFAULT_API_BASE)}server/apiServer/asyncBatchOpt`,
    'POST',
    {
      phones: normalizedPhones,
      token: authToken,
      deviceId: getOrCreateDeviceId()
    },
    {
      'X-Free-Token': authToken
    }
  )

  const code = Number(response?.code)
  if (code !== 0 && code !== 200) {
    throw new Error(response?.msg || '批量查询失败')
  }
  return response?.data || {}
}
export function isTeddyGetCodeSuccess(result) {
  const code = trim(result?.code)
  const channelCode = trim(result?.channelCode)
  const verifyCode = trim(result?.verifyCode)
  return code === '0' && !!channelCode && !!verifyCode
}

export async function getTeddyVerifyCode(phone, teddyProtocolBase) {
  ensureRegisteredUserForTeddySms()
  const targetPhone = trim(phone)
  if (!targetPhone) {
    throw new Error('未提供手机号')
  }

  const response = await requestJson(
    `${resolveTeddyProtocolBase(teddyProtocolBase)}/api/phone/getVerifyCode`,
    'POST',
    buildTeddyProtocolPayload(targetPhone)
  )

  return {
    code: trim(response?.code),
    channelCode: trim(response?.data?.channelCode),
    verifyCode: trim(response?.data?.verifyCode),
    msg: trim(response?.msg)
  }
}

export async function verifyTeddyCode(phone, verifyCode, teddyProtocolBase) {
  ensureRegisteredUserForTeddySms()
  const targetPhone = trim(phone)
  const targetCode = trim(verifyCode)
  if (!targetPhone || !targetCode) {
    throw new Error('验证码内容不完整')
  }

  const response = await requestJson(
    `${resolveTeddyProtocolBase(teddyProtocolBase)}/api/phone/queryVerifyCodeResult`,
    'POST',
    buildTeddyProtocolPayload(targetPhone, {
      verifyCode: targetCode
    })
  )
  const data = response?.data
  const checkResultRaw =
    data?.checkResult ??
    data?.check_result ??
    data?.verifyResult ??
    data?.verify_result ??
    data?.status
  const checkResultText = trim(checkResultRaw)
  const checkResultNum = checkResultText === '' ? Number.NaN : Number(checkResultText)
  const doneByCheckResult = Number.isFinite(checkResultNum) && checkResultNum === 0
  const doneByBooleanFlag = data?.verified === true || data?.success === true
  const doneByStringStatus =
    checkResultText.toLowerCase() === 'success' ||
    checkResultText.toLowerCase() === 'done'
  const done = doneByCheckResult || doneByBooleanFlag || doneByStringStatus
  const normalizedCheckResult = Number.isFinite(checkResultNum) ? checkResultNum : (done ? 0 : 1)
  return {
    done,
    checkResult: normalizedCheckResult,
    captcha: done ? 'protocol-ok' : '',
    msg: trim(response?.msg) || (done ? '校验成功' : '校验中'),
    raw: data || null
  }
}

export async function submitTeddyVerification() {
  ensureRegisteredUserForTeddySms()
  return {
    code: 0,
    msg: '已按协议提交，请稍后查看处理结果'
  }
}
