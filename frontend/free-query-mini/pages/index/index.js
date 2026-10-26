const app = getApp()

function randomHex(length) {
  let value = ''
  while (value.length < length) {
    value += Math.floor(Math.random() * 0x100000000).toString(16).padStart(8, '0')
  }
  return value.slice(0, length)
}

function getStableId(key, length) {
  const cached = wx.getStorageSync(key)
  if (cached && typeof cached === 'string') return cached
  const generated = randomHex(length)
  wx.setStorageSync(key, generated)
  return generated
}

function request(path, options = {}) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.apiBaseUrl}${path}`,
      timeout: options.timeout || 30000,
      method: options.method || 'GET',
      data: options.data,
      header: options.header || {},
      success: resolve,
      fail: reject,
    })
  })
}

function unwrap(response, fallbackMessage) {
  const body = response && response.data ? response.data : {}
  if (String(body.code) !== '0000') {
    throw new Error(body.msg || fallbackMessage)
  }
  return body.data || {}
}

function responseCookie(response) {
  const headers = (response && response.header) || {}
  const value = headers['Set-Cookie'] || headers['set-cookie'] || ''
  return String(value).split(';')[0]
}

const PLATFORMS = [
  ['tdx', '泰迪熊'],
  ['sll', '360'],
  ['tx', '腾讯'],
  ['bd', '百度'],
  ['lt', '联通'],
  ['ydhigh', '移动高频'],
  ['dhb', '电话邦'],
  ['sghmt', '搜狗'],
  ['xm', '小米'],
  ['114', '114'],
].map(([code, name]) => ({
  code,
  name,
  icon: `/images/platform/${code}.png`,
}))

Page({
  data: {
    phone: '',
    loading: false,
    queryProgress: 0,
    error: '',
    result: null,
    searchedPhone: '',
    platforms: PLATFORMS,
    contactPhone: '13027616171',
    showQrModal: false,
  },

  onLoad() {
    this.visitorId = getStableId('free_query_visitor_id', 32)
    this.fingerprint = getStableId('free_query_fingerprint', 64)
  },

  onUnload() {
    this.stopQueryProgress()
  },

  startQueryProgress() {
    this.stopQueryProgress()
    const progress = { startedAt: Date.now(), delayMs: null }
    this.queryProgressState = progress
    progress.policyReady = request('/h5/mark/policy', { timeout: 10000 })
      .then((response) => {
        const configured = Number(unwrap(response, '读取查询设置失败').result_delay_ms)
        progress.delayMs = Number.isFinite(configured) && configured >= 0 ? configured : 6000
      })
      .catch(() => { progress.delayMs = 6000 })
    progress.timer = setInterval(() => {
      if (this.queryProgressState !== progress || progress.delayMs === null) return
      const percentage = progress.delayMs === 0
        ? 99
        : Math.floor((Date.now() - progress.startedAt) * 100 / progress.delayMs)
      const queryProgress = Math.min(99, Math.max(this.data.queryProgress, percentage))
      if (queryProgress !== this.data.queryProgress) this.setData({ queryProgress })
      if (queryProgress >= 99) {
        clearInterval(progress.timer)
        progress.timer = null
      }
    }, 100)
    return progress
  },

  waitForQueryProgress(progress, duration) {
    return new Promise((resolve) => {
      progress.resolveWait = resolve
      progress.finishTimer = setTimeout(() => {
        progress.finishTimer = null
        progress.resolveWait = null
        resolve()
      }, duration)
    })
  },

  async finishQueryProgress(progress) {
    await progress.policyReady
    if (this.queryProgressState !== progress) return false
    const remaining = Math.max(0, progress.delayMs - (Date.now() - progress.startedAt))
    if (remaining > 0) await this.waitForQueryProgress(progress, remaining)
    if (this.queryProgressState !== progress) return false
    clearInterval(progress.timer)
    progress.timer = null
    this.setData({ queryProgress: 100 })
    await this.waitForQueryProgress(progress, 500)
    return this.queryProgressState === progress
  },

  stopQueryProgress() {
    const progress = this.queryProgressState
    this.queryProgressState = null
    if (!progress) return
    clearInterval(progress.timer)
    clearTimeout(progress.finishTimer)
    if (progress.resolveWait) progress.resolveWait()
  },

  preventTouchMove() {},

  onPhoneInput(event) {
    this.setData({
      phone: String(event.detail.value || '').replace(/\D/g, '').slice(0, 20),
      error: '',
    })
  },

  clearPhone() {
    this.setData({ phone: '', error: '', result: null, searchedPhone: '', showQrModal: false })
  },

  async submitQuery() {
    if (this.data.loading) return
    const phone = this.data.phone.trim()
    if (!/^\d{5,20}$/.test(phone)) {
      this.setData({ error: '请输入正确的手机或座机号码' })
      return
    }

    wx.hideKeyboard()
    this.setData({ loading: true, queryProgress: 0, error: '', result: null, searchedPhone: '' })
    const progress = this.startQueryProgress()
    try {
      const commonHeaders = { 'X-Visitor-Id': this.visitorId }
      const challengeResponse = await request('/h5/security/challenge', {
        method: 'POST',
        data: { fingerprint: this.fingerprint },
        header: commonHeaders,
      })
      if (this.queryProgressState !== progress) return
      this.sessionCookie = responseCookie(challengeResponse) || this.sessionCookie || ''
      const challenge = unwrap(challengeResponse, '安全验证失败，请稍后重试')
      const queryHeaders = Object.assign({}, commonHeaders, {
        'X-Device-Fingerprint': this.fingerprint,
        'X-Free-Query-Token': challenge.token,
      })
      if (this.sessionCookie) queryHeaders.Cookie = this.sessionCookie
      const queryResponse = await request('/h5/mark/query', {
        method: 'POST',
        data: { number: phone },
        header: queryHeaders,
        timeout: 90000,
      })
      if (this.queryProgressState !== progress) return
      const query = unwrap(queryResponse, '号码查询失败')
      const infoHeaders = Object.assign({}, commonHeaders)
      if (this.sessionCookie) infoHeaders.Cookie = this.sessionCookie
      const infoResponse = await request('/h5/mark/getInfo', {
        method: 'POST',
        data: { id: query.id },
        header: infoHeaders,
      })
      if (this.queryProgressState !== progress) return
      const result = unwrap(infoResponse, '查询结果已失效，请重试')
      if (!await this.finishQueryProgress(progress)) return
      this.setData({ result: this.normalizeResult(result), searchedPhone: phone })
    } catch (error) {
      if (this.queryProgressState === progress) {
        this.setData({ error: error.message || '网络异常，请稍后重试' })
      }
    } finally {
      if (this.queryProgressState === progress) {
        this.stopQueryProgress()
        this.setData({ loading: false })
      }
    }
  },

  normalizeResult(result) {
    const source = result && typeof result === 'object' ? result : {}
    const item = Array.isArray(source.item) ? source.item : []
    const normalizedItems = item.map((entry) => {
      const current = entry && typeof entry === 'object' ? entry : {}
      const code = String(current.platform || '114').replace(/[^a-zA-Z0-9_-]/g, '') || '114'
      return Object.assign({}, current, {
        iconPath: `/images/platform/${code}.png`,
      })
    })
    return Object.assign({}, source, {
      number: String(source.number || this.data.phone),
      is_mark: Boolean(source.is_mark),
      title: String(source.title || (normalizedItems.length ? `发现 ${normalizedItems.length} 个平台标记` : '')),
      item: normalizedItems,
    })
  },

  backToQuery() {
    this.setData({ result: null, searchedPhone: '', error: '', showQrModal: false })
  },

  openQrModal() {
    this.setData({ showQrModal: true })
  },

  closeQrModal() {
    this.setData({ showQrModal: false })
  },

  onShareAppMessage() {
    return { title: '手机号免费标记查询' }
  },
})
