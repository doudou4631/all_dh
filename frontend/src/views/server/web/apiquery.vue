<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getFreeQueryQuota, queryFreeSingle } from '@/api/server/freeQueryApi'

const platformOrder = [
  '泰迪熊',
  '腾讯',
  '360手机卫士',
  '百度',
  '搜狗',
  '移动高频',
  '联通管家',
  '电话邦',
  '小米手机'
]

const platformKeyToName = {
  teddy: '泰迪熊',
  taidixiong: '泰迪熊',
  tengxun: '腾讯',
  tencent: '腾讯',
  '360': '360手机卫士',
  sanliuling: '360手机卫士',
  baidu: '百度',
  sogou: '搜狗',
  sghmt: '搜狗',
  mobile_hf: '移动高频',
  yidonggaopin: '移动高频',
  unicom: '联通管家',
  ltgj: '联通管家',
  dianhuabang: '电话邦',
  xiaomi: '小米手机'
}

function maybeAutoQueryFromRoute() {
  if (!props.showPublicResult) {
    return
  }
  const routePhone = queryPhoneFromRoute.value
  if (!routePhone || routePhone === autoQueryPhone.value) {
    return
  }
  autoQueryPhone.value = routePhone
  phone.value = routePhone
  doQuery()
}

const platformNameToKeys = Object.entries(platformKeyToName).reduce((acc, [key, name]) => {
  if (!acc[name]) {
    acc[name] = []
  }
  acc[name].push(key)
  return acc
}, {})

const canceledMessageKeywords = ['泰迪未来标记已取消', '同步时间']
const publicPlatforms = [
  { name: '360', icon: '/free-query-icons/360.png' },
  { name: '百度', icon: '/free-query-icons/baidu.ico' },
  { name: '泰迪熊', icon: '/free-query-icons/teddy.png' },
  { name: '联通', icon: '/free-query-icons/unicom.svg' },
  { name: '腾讯', icon: '/free-query-icons/tencent.png' },
  { name: '移动高频', icon: '/free-query-icons/mobile.png' },
  { name: '电话邦', icon: '/free-query-icons/dianhuabang.ico' },
  { name: '搜狗', icon: '/free-query-icons/sogou.ico' },
  { name: '小米手机', icon: '/free-query-icons/xiaomi.jpeg' }
]
const platformIconMap = {
  '泰迪熊': '/free-query-icons/teddy.png',
  '腾讯': '/free-query-icons/tencent.png',
  '360手机卫士': '/free-query-icons/360.png',
  '百度': '/free-query-icons/baidu.ico',
  '搜狗': '/free-query-icons/sogou.ico',
  '移动高频': '/free-query-icons/mobile.png',
  '联通管家': '/free-query-icons/unicom.svg',
  '电话邦': '/free-query-icons/dianhuabang.ico',
  '小米手机': '/free-query-icons/xiaomi.jpeg'
}

const appealNotes = [
  '泰迪熊：普通标记申诉后6小时生效（30天内二次处理会驳回）',
  '提示10个工作日要暂停拨号',
  '腾讯管家：审核1-3个工作日（期间暂停外呼）',
  '360卫士：三个月内只能处理一次'
]

const phone = ref('')
const querying = ref(false)
const messageText = ref('')
const messageType = ref('')
const rows = ref([])
const props = defineProps({
  showPublicResult: {
    type: Boolean,
    default: false
  },
  publicMarkedOnly: {
    type: Boolean,
    default: false
  }
})
const route = useRoute()
const router = useRouter()
const isPublicShell = computed(() => /^\/free[-_]query(?:2|[-_]marked)?(\/|$)/i.test(route.path || ''))
const markedQueryPagePath = '/free-query-marked'
const quota = ref({
  ip: '-',
  limit: 20,
  used: 0,
  remaining: 20,
  resetAt: '-',
  /** 配置了字典 daily_all_limit 时由后端返回：平台当日总次数上限（全站共用） */
  allLimit: undefined,
  allUsed: undefined,
  allRemaining: undefined
})
/** 免费查询不发起外呼的平台展示名（与后端 UserPlatformUrlConfig.platformName 一致，由 /quota 与查询接口返回） */
const disabledPlatforms = ref([])
/** 最近一次成功查询返回的 results 原数组；loadQuota 刷新额度时据此重绘，避免把结果行刷成 “-” */
const lastQueryResults = ref([])
/** 是否已完成过至少一次成功查询；用于「未开放」平台在查询后展示「无标记」 */
const hadSuccessfulFreeQuery = ref(false)

const messageClass = computed(() => (messageType.value ? `msg ${messageType.value}` : 'msg'))
const publicResultRows = computed(() =>
  props.publicMarkedOnly
    ? rows.value.filter((row) => row?.parsed?.status === '有标记')
    : rows.value
)
const publicHeroTitle = computed(() => (props.publicMarkedOnly ? '号码标记查询结果' : '号码标记清除'))
const publicHeroSubtitle = computed(() =>
  props.publicMarkedOnly ? '仅展示被标记平台结果' : '快速查询并去除各平台号码标记'
)
const autoQueryPhone = ref('')
const queryPhoneFromRoute = computed(() => (typeof route.query.phone === 'string' ? route.query.phone.trim() : ''))
const currentQueryPhone = computed(() => phone.value.trim() || queryPhoneFromRoute.value)
const markedPlatformCount = computed(() => publicResultRows.value.length)
const supportPhone = '18537174371'
const supportWechatQr = '/wechat/customer-wechat-main.png'
const FREE_QUERY_DEVICE_ID_STORAGE_KEY = 'free_query_device_id'
const deviceId = ref('')
const isMobileDevice = ref(false)
const mobileUserAgentRegex = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Windows Phone|Mobi|Mobile/i

function generateDeviceId() {
  const byCrypto = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function' ? crypto.randomUUID() : ''
  if (byCrypto) {
    return `fq_${byCrypto.replace(/-/g, '')}`
  }
  return `fq_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 12)}`
}

function getOrCreateDeviceId() {
  if (typeof window === 'undefined') {
    return ''
  }
  try {
    const exists = window.localStorage.getItem(FREE_QUERY_DEVICE_ID_STORAGE_KEY)
    if (exists && exists.trim()) {
      return exists.trim()
    }
    const created = generateDeviceId()
    window.localStorage.setItem(FREE_QUERY_DEVICE_ID_STORAGE_KEY, created)
    return created
  } catch (error) {
    return generateDeviceId()
  }
}

function detectMobileDevice() {
  if (typeof window === 'undefined') {
    return
  }
  const ua = typeof navigator !== 'undefined' ? navigator.userAgent || '' : ''
  const touchPoints = typeof navigator !== 'undefined' ? navigator.maxTouchPoints || 0 : 0
  const byUserAgent = mobileUserAgentRegex.test(ua)
  const byViewport = window.matchMedia('(max-width: 767px)').matches
  const byTouchAndViewport = touchPoints > 0 && window.matchMedia('(max-width: 1024px)').matches
  isMobileDevice.value = byUserAgent || byViewport || byTouchAndViewport
}

function handleViewportChange() {
  detectMobileDevice()
}

function canAutoJumpToMarkedPage() {
  const path = route.path || ''
  return isPublicShell.value && !props.showPublicResult && /^\/free[-_]query2?(\/|$)/i.test(path)
}

function norm(value) {
  return typeof value === 'string' ? value.replace(/\s+/g, '').trim() : ''
}

function isCanceledMessage(value) {
  const normalized = norm(value)
  return canceledMessageKeywords.every((token) => normalized.includes(token))
}

function parseOne(item) {
  if (!item || typeof item !== 'object') {
    return { status: '-', message: '-' }
  }
  if (typeof item.error === 'string' && item.error.trim()) {
    return { status: '查询失败', message: item.error.trim() }
  }
  const payload = item.data && typeof item.data === 'object' ? item.data : null
  const platformResults = payload?.platformResults
  const rawResult = Array.isArray(platformResults) && platformResults.length > 0 ? platformResults[0]?.status : ''

  if (rawResult) {
    const statusText = String(rawResult)
    if (statusText.startsWith('yes-')) {
      const msg = statusText.slice(4).trim()
      if (isCanceledMessage(msg)) return { status: '无标记', message: '无标记' }
      return { status: '有标记', message: msg || '普通标记' }
    }
    if (statusText.startsWith('no-')) return { status: '无标记', message: '无标记' }
    if (statusText === 'yes') return { status: '有标记', message: '普通标记' }
    if (statusText === 'no' || /no/i.test(statusText)) return { status: '无标记', message: '无标记' }
  }

  return { status: '查询失败', message: item.error || '查询失败' }
}

function buildPlatformMap(list) {
  const map = {}
  if (!Array.isArray(list)) {
    return map
  }
  list.forEach((item) => {
    const displayName = item.platformName || platformKeyToName[item.platform] || platformKeyToName[item.name]
    if (displayName) {
      map[displayName] = item
      return
    }
    const innerStatus = item?.data?.platformResults?.[0]?.platform
    const name = platformKeyToName[innerStatus] || innerStatus
    if (name) {
      map[name] = item
    }
  })
  return map
}

function rank(status) {
  if (status === '有标记') return 0
  if (status === '无标记') return 1
  if (status === '查询失败') return 2
  if (status === '未开放') return 4
  return 3
}

function applyDisabledFromPayload(payload) {
  if (!payload || typeof payload !== 'object') {
    return
  }
  const raw = payload.disabledPlatforms
  if (Array.isArray(raw)) {
    disabledPlatforms.value = raw.map((x) => String(x).trim()).filter(Boolean)
    return
  }
  if (typeof raw === 'string' && raw.trim()) {
    disabledPlatforms.value = raw.split(',').map((s) => s.trim()).filter(Boolean)
  }
}

function renderRows(platformMap) {
  const disabled = new Set(disabledPlatforms.value)
  const nextRows = platformOrder.map((name) => {
    if (disabled.has(name)) {
      const msg = hadSuccessfulFreeQuery.value ? '无标记' : '-'
      return {
        name,
        skipped: true,
        parsed: { status: '未开放', message: msg }
      }
    }
    let item = platformMap[name]
    if (!item && platformNameToKeys[name]) {
      for (const key of platformNameToKeys[name]) {
        if (platformMap[key]) {
          item = platformMap[key]
          break
        }
      }
    }
    return { name, parsed: parseOne(item) }
  })
  nextRows.sort((a, b) => {
    const ra = rank(a.parsed.status)
    const rb = rank(b.parsed.status)
    if (ra !== rb) return ra - rb
    return platformOrder.indexOf(a.name) - platformOrder.indexOf(b.name)
  })
  rows.value = nextRows
}

function setMsg(text, type = '') {
  messageText.value = text
  messageType.value = type
}

function clearInput() {
  phone.value = ''
  setMsg('', '')
  lastQueryResults.value = []
  hadSuccessfulFreeQuery.value = false
  renderRows(buildPlatformMap([]))
}

async function loadQuota() {
  const res = await getFreeQueryQuota()
  const payload = res?.data ?? res
  if (payload && typeof payload === 'object') {
    quota.value = {
      ...quota.value,
      ...payload
    }
    applyDisabledFromPayload(payload)
    renderRows(buildPlatformMap(lastQueryResults.value))
  }
}

async function doQuery() {
  if (!phone.value.trim()) {
    setMsg('请输入号码', 'err')
    return
  }
  const currentDeviceId = deviceId.value || getOrCreateDeviceId()
  deviceId.value = currentDeviceId
  querying.value = true
  setMsg('查询中...', '')
  try {
    const res = await queryFreeSingle({ phone: phone.value.trim(), deviceId: currentDeviceId })
    if (res?.quota) {
      quota.value = { ...quota.value, ...res.quota }
      applyDisabledFromPayload(res.quota)
    }
    if (res?.code === 42901) {
      window.alert(res?.msg || '当前IP今日免费查询次数已达上限，请添加客服微信查询。')
      setMsg(res?.msg || '查询失败', 'err')
      lastQueryResults.value = []
      hadSuccessfulFreeQuery.value = false
      renderRows(buildPlatformMap([]))
      return
    }
    if (res?.code === 42902) {
      window.alert(res?.msg || '平台当日免费额度已用完，请联系客服')
      setMsg(res?.msg || '查询失败', 'err')
      lastQueryResults.value = []
      hadSuccessfulFreeQuery.value = false
      renderRows(buildPlatformMap([]))
      await loadQuota()
      return
    }
    const okCode = Number(res?.code)
    if (okCode !== 200 && okCode !== 0) {
      setMsg(res?.msg || '查询失败', 'err')
      lastQueryResults.value = []
      hadSuccessfulFreeQuery.value = false
      renderRows(buildPlatformMap([]))
      return
    }
    const resultList = Array.isArray(res?.data?.results) ? res.data.results : []
    lastQueryResults.value = resultList
    hadSuccessfulFreeQuery.value = true
    if (res?.data && typeof res.data === 'object') {
      applyDisabledFromPayload(res.data)
    }
    renderRows(buildPlatformMap(resultList))
    if (canAutoJumpToMarkedPage()) {
      await router.push({
        path: markedQueryPagePath,
        query: { phone: phone.value.trim() }
      }).catch(() => {})
      return
    }
    setMsg('查询成功', 'ok')
    await loadQuota()
  } catch (error) {
    setMsg(`请求失败：${error.message || error}`, 'err')
  } finally {
    querying.value = false
  }
}

function goLogManage() {
  const currentPath = route.path || ''
  const target = /apiquery\/?$/i.test(currentPath)
    ? currentPath.replace(/apiquery\/?$/i, 'apilog')
    : '/server/web/apilog'
  router.push(target).catch(() => {})
}

function backToPublicQueryPage() {
  router.push({ path: '/free-query' }).catch(() => {})
}

lastQueryResults.value = []
renderRows(buildPlatformMap([]))
loadQuota()
onMounted(() => {
  maybeAutoQueryFromRoute()
  deviceId.value = getOrCreateDeviceId()
  detectMobileDevice()
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', handleViewportChange)
    window.addEventListener('orientationchange', handleViewportChange)
  }
})
onBeforeUnmount(() => {
  if (typeof window === 'undefined') {
    return
  }
  window.removeEventListener('resize', handleViewportChange)
  window.removeEventListener('orientationchange', handleViewportChange)
})
watch(() => route.query.phone, () => {
  maybeAutoQueryFromRoute()
})
</script>

<template>
  <div v-if="isPublicShell && props.publicMarkedOnly" :class="['public-root', { 'mobile-device': isMobileDevice }]">
    <div v-if="isMobileDevice" class="mobile-marked-shell">
      <section class="mobile-card">
        <h1 class="mobile-page-title">号码标记查询结果</h1>
        <div class="mobile-page-subtitle">号码：{{ currentQueryPhone || '-' }}</div>
        <div class="mobile-marked-count">
          被 <span class="highlight">{{ markedPlatformCount }}</span> 个平台标记
        </div>
      </section>

      <section class="mobile-card">
        <h2 class="mobile-section-title">标记详情</h2>
        <div v-if="hadSuccessfulFreeQuery && publicResultRows.length > 0" class="mobile-marked-list">
          <article v-for="row in publicResultRows" :key="row.name" class="mobile-marked-item">
            <div class="mobile-marked-item-head">
              <img
                v-if="platformIconMap[row.name]"
                class="mobile-marked-item-icon"
                :src="platformIconMap[row.name]"
                :alt="row.name"
              />
              <span class="mobile-marked-item-name">{{ row.name }}</span>
            </div>
            <div class="mobile-marked-item-desc">
              标记名称：<span>{{ row.parsed.message || '-' }}</span>
            </div>
          </article>
        </div>
        <div v-else class="mobile-empty">
          {{ querying ? '正在查询，请稍候...' : '该号码当前未发现被标记平台' }}
        </div>
      </section>

      <section class="mobile-card mobile-support-card">
        <h2 class="mobile-section-title">客服支持</h2>
        <div class="mobile-support-text">
          客服电话：<a class="mobile-support-link" :href="`tel:${supportPhone}`">{{ supportPhone }}</a>
        </div>
        <img class="mobile-support-qrcode" :src="supportWechatQr" alt="客服微信二维码" />
      </section>
    </div>

    <div v-else class="public-shell">
      <header class="ui-card public-nav">
        <div class="nav-brand">
          <span class="brand-dot" />
          <span class="brand-text">号码标记服务</span>
        </div>
        <nav class="nav-links">
          <a class="nav-link" :href="`tel:${supportPhone}`">客服热线</a>
          <button class="nav-link nav-link-btn" type="button" @click="backToPublicQueryPage">重新查询</button>
        </nav>
      </header>

      <div class="public-layout">
        <main class="public-main">
          <section class="ui-card marked-hero-card">
            <div class="marked-top-label">全网平台查询到号码：</div>
            <div class="marked-top-phone">{{ currentQueryPhone || '-' }}</div>
            <div class="marked-top-count">
              被 <span class="highlight">{{ markedPlatformCount }}</span> 个平台标记
            </div>
          </section>

          <section class="ui-card">
            <div class="module-head">
              <h2 class="module-title">标记详情</h2>
            </div>
            <div v-if="hadSuccessfulFreeQuery && publicResultRows.length > 0" class="marked-list">
              <article v-for="row in publicResultRows" :key="row.name" class="marked-item">
                <div class="marked-item-head">
                  <img v-if="platformIconMap[row.name]" class="marked-item-icon" :src="platformIconMap[row.name]" :alt="row.name" />
                  <span class="marked-item-title">{{ row.name }}</span>
                </div>
                <div class="marked-item-desc">
                  <span class="label">标记名称：</span>
                  <span class="value">{{ row.parsed.message || '-' }}</span>
                </div>
              </article>
            </div>
            <div v-else class="marked-empty">
              {{ querying ? '正在查询，请稍候...' : '该号码当前未发现被标记平台' }}
            </div>
          </section>
        </main>

        <aside class="public-side">
          <section class="ui-card side-card">
            <h3 class="side-title">下一步建议</h3>
            <ul class="side-list">
              <li>优先处理展示为“有标记”的平台。</li>
              <li>申诉审核期间建议减少外呼频次。</li>
              <li>如遇驳回可联系人工客服协助处理。</li>
            </ul>
          </section>

          <section class="ui-card side-card">
            <h3 class="side-title">联系客服</h3>
            <div class="marked-contact">
              联系电话：<a class="marked-contact-link" :href="`tel:${supportPhone}`">{{ supportPhone }}</a>
            </div>
            <img class="marked-qrcode" :src="supportWechatQr" alt="客服微信二维码" />
            <el-button class="primary-btn block" @click="backToPublicQueryPage">返回重新查询</el-button>
          </section>
        </aside>
      </div>
    </div>

    <nav v-if="isMobileDevice" class="mobile-bottom-nav">
      <div class="mobile-bottom-nav-inner">
        <a class="mobile-bottom-link" :href="`tel:${supportPhone}`">联系客服</a>
        <button class="mobile-bottom-link mobile-bottom-primary" type="button" @click="backToPublicQueryPage">重新查询</button>
      </div>
    </nav>
  </div>

  <div v-else-if="isPublicShell" :class="['public-root', { 'mobile-device': isMobileDevice }]">
    <div v-if="isMobileDevice" class="mobile-query-shell">
      <section class="mobile-card">
        <h1 class="mobile-page-title">{{ publicHeroTitle }}</h1>
        <div class="mobile-page-subtitle">{{ publicHeroSubtitle }}</div>
        <div class="mobile-search-bar">
          <el-input
            v-model.trim="phone"
            class="mobile-search-input"
            placeholder="请输入手机号码"
            clearable
            @keyup.enter="doQuery"
          />
        </div>
        <div v-if="messageText" class="mobile-page-message" :class="{ ok: messageType === 'ok', err: messageType === 'err' }">
          {{ messageText }}
        </div>
      </section>

      <section class="mobile-card">
        <h2 class="mobile-section-title">支持去除以下平台标记</h2>
        <div class="mobile-platform-grid">
          <article v-for="item in publicPlatforms" :key="item.name" class="mobile-platform-item">
            <div class="mobile-platform-icon-wrap">
              <img class="mobile-platform-icon" :src="item.icon" :alt="item.name" />
            </div>
            <div class="mobile-platform-label">{{ item.name }}</div>
          </article>
        </div>
      </section>

      <section class="mobile-card" id="appeal-note-mobile">
        <h2 class="mobile-section-title">申诉说明</h2>
        <ul class="mobile-appeal-list">
          <li v-for="tip in appealNotes" :key="tip" class="mobile-appeal-item">{{ tip }}</li>
        </ul>
      </section>

      <section class="mobile-card mobile-support-card">
        <h2 class="mobile-section-title">客服支持</h2>
        <div class="mobile-support-text">
          客服电话：<a class="mobile-support-link" :href="`tel:${supportPhone}`">{{ supportPhone }}</a>
        </div>
        <img class="mobile-support-qrcode" :src="supportWechatQr" alt="客服微信二维码" />
      </section>
    </div>

    <div v-else class="public-shell">
      <header class="ui-card public-nav">
        <div class="nav-brand">
          <span class="brand-dot" />
          <span class="brand-text">号码标记服务</span>
        </div>
        <nav class="nav-links">
          <a class="nav-link" :href="`tel:${supportPhone}`">联系客服</a>
          <a class="nav-link" href="#appeal-note">申诉说明</a>
        </nav>
      </header>

      <div class="public-layout">
        <main class="public-main">
          <section class="ui-card public-hero-card">
            <h1 class="hero-title">{{ publicHeroTitle }}</h1>
            <div class="hero-subtitle">{{ publicHeroSubtitle }}</div>
            <div class="hero-search">
              <el-input
                v-model.trim="phone"
                class="hero-input"
                placeholder="请输入手机号码"
                clearable
                @keyup.enter="doQuery"
              />
              <el-button class="hero-button" :loading="querying" @click="doQuery">
                {{ querying ? '查询中' : '查询' }}
              </el-button>
            </div>
            <div v-if="messageText" class="hero-message" :class="{ ok: messageType === 'ok', err: messageType === 'err' }">
              {{ messageText }}
            </div>
          </section>

          <section class="ui-card">
            <div class="module-head">
              <h2 class="module-title">支持去除以下平台标记</h2>
            </div>
            <div class="platform-grid">
              <article v-for="item in publicPlatforms" :key="item.name" class="platform-item">
                <div class="platform-icon-wrap">
                  <img class="platform-icon" :src="item.icon" :alt="item.name" />
                </div>
                <div class="platform-label">{{ item.name }}</div>
              </article>
            </div>
          </section>

          <section id="appeal-note" class="ui-card">
            <div class="module-head">
              <h2 class="module-title">申诉说明</h2>
            </div>
            <ul class="appeal-list">
              <li v-for="tip in appealNotes" :key="tip" class="appeal-item">{{ tip }}</li>
            </ul>
          </section>
        </main>

        <aside class="public-side">
          <section class="ui-card side-card">
            <h3 class="side-title">服务支持</h3>
            <div class="marked-contact">
              联系电话：<a class="marked-contact-link" :href="`tel:${supportPhone}`">{{ supportPhone }}</a>
            </div>
            <img class="marked-qrcode" :src="supportWechatQr" alt="客服微信二维码" />
          </section>

          <section class="ui-card side-card">
            <h3 class="side-title">办理流程</h3>
            <ul class="side-list">
              <li>输入号码并查询标记状态。</li>
              <li>根据平台规则提交对应申诉。</li>
              <li>审核通过后重新查询确认结果。</li>
            </ul>
          </section>
        </aside>
      </div>
    </div>

    <nav v-if="isMobileDevice" class="mobile-bottom-nav">
      <div class="mobile-bottom-nav-inner">
        <a class="mobile-bottom-link" :href="`tel:${supportPhone}`">联系客服</a>
        <button class="mobile-bottom-link mobile-bottom-primary" type="button" @click="doQuery">
          {{ querying ? '查询中' : '立即查询' }}
        </button>
      </div>
    </nav>
  </div>

  <div v-else class="app-container free-query-wrap">
    <el-card shadow="never" class="query-card">
      <div class="title">号码标记免费查询平台</div>
      <div class="quota-row">
        <el-tag type="warning">今日免费额度：{{ quota.remaining }}/{{ quota.limit }}</el-tag>
      </div>
      <div class="query-row">
        <el-input v-model.trim="phone" placeholder="输入手机号或固话号码" clearable @keyup.enter="doQuery" />
        <el-button class="query-btn-start" :loading="querying" type="primary" @click="doQuery">
          {{ querying ? '查询中...' : '开始查询' }}
        </el-button>
        <el-button class="query-btn-clear" :disabled="querying" @click="clearInput">清空</el-button>
        <el-button type="primary" plain @click="goLogManage">日志管理</el-button>
      </div>
      <div :class="messageClass">{{ messageText }}</div>
      <div class="result-list">
        <div v-for="row in rows" :key="row.name" class="result-card">
          <div class="result-card-top">
            <div class="platform-name" :title="row.name">{{ row.name }}</div>
            <div
              :class="{
                'result-text': true,
                marked: row.parsed.status === '有标记',
                muted: row.skipped || row.parsed.message === '-',
                'platform-free-disabled': row.skipped
              }"
            >
              {{ row.parsed.message || '-' }}
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.public-root {
  --ui-primary: #3b82f6;
  --ui-primary-hover: #2563eb;
  --ui-bg: #f3f6fb;
  --ui-card: #ffffff;
  --ui-radius: 16px;
  --ui-radius-sm: 12px;
  --ui-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  --ui-border: #e2e8f0;
  --ui-text: #0f172a;
  --ui-muted: #475569;
  min-height: 100vh;
  background: var(--ui-bg);
  padding: 20px;
  box-sizing: border-box;
  color: var(--ui-text);
  line-height: 1.5;
  font-family: Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.public-shell {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  gap: 20px;
}

.ui-card {
  background: var(--ui-card);
  border-radius: var(--ui-radius);
  box-shadow: var(--ui-shadow);
  padding: 20px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.ui-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
}

.public-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
}

.brand-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #60a5fa 0%, #2563eb 100%);
}

.brand-text {
  font-size: 15px;
  letter-spacing: 0.2px;
}

.nav-links {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.nav-link {
  color: var(--ui-primary);
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.2s ease, opacity 0.2s ease;
}

.nav-link:hover {
  color: var(--ui-primary-hover);
  opacity: 0.9;
}

.nav-link-btn {
  border: 0;
  background: transparent;
  cursor: pointer;
  padding: 0;
  font: inherit;
}

.public-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 20px;
}

.public-main,
.public-side {
  display: grid;
  gap: 20px;
  align-content: start;
}

.public-hero-card,
.marked-hero-card {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
}

.hero-title {
  margin: 0;
  font-size: clamp(28px, 3.8vw, 40px);
  font-weight: 800;
  letter-spacing: 0.2px;
}

.hero-subtitle {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.92);
  font-size: 14px;
}

.hero-search {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-input {
  flex: 1;
}

.hero-input :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: var(--ui-radius-sm);
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid transparent;
  box-shadow: none !important;
  padding: 0 14px;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.hero-input :deep(.el-input__wrapper.is-focus) {
  border-color: #93c5fd;
  background: #fff;
}

.hero-input :deep(.el-input__inner) {
  color: #0f172a;
  font-size: 15px;
}

.hero-button {
  min-width: 92px;
  height: 48px;
  border: 0;
  border-radius: var(--ui-radius-sm);
  background: #fff;
  color: var(--ui-primary);
  font-weight: 700;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.hero-button:hover,
.hero-button:focus {
  transform: translateY(-1px);
  box-shadow: 0 10px 16px rgba(0, 0, 0, 0.12);
  background: #eff6ff;
  color: var(--ui-primary-hover);
}

.hero-message {
  margin-top: 10px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.95);
}

.hero-message.ok {
  color: #d1fae5;
}

.hero-message.err {
  color: #fee2e2;
}

.module-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.module-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0.1px;
}

.platform-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.platform-item {
  border-radius: var(--ui-radius-sm);
  border: 1px solid var(--ui-border);
  padding: 14px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.platform-item:hover {
  transform: translateY(-2px);
  border-color: #bfdbfe;
  background: #f8fbff;
}

.platform-icon-wrap {
  width: 46px;
  height: 46px;
  border-radius: var(--ui-radius-sm);
  display: grid;
  place-items: center;
  background: #eff6ff;
}

.platform-icon {
  width: 30px;
  height: 30px;
  object-fit: contain;
}

.platform-label {
  font-size: 14px;
  color: #1e293b;
  font-weight: 600;
}

.appeal-list {
  margin: 0;
  padding-left: 20px;
  color: var(--ui-muted);
  display: grid;
  gap: 8px;
}

.appeal-item {
  font-size: 15px;
  line-height: 1.5;
}

.marked-top-label {
  font-size: 15px;
  opacity: 0.92;
}

.marked-top-phone {
  margin-top: 8px;
  font-size: clamp(24px, 4vw, 36px);
  line-height: 1.2;
  font-weight: 700;
  color: #fef08a;
}

.marked-top-count {
  margin-top: 10px;
  font-size: clamp(20px, 3vw, 30px);
}

.marked-top-count .highlight {
  color: #fde68a;
  font-weight: 800;
}

.marked-list {
  display: grid;
  gap: 12px;
}

.marked-item {
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-sm);
  background: #fff;
  padding: 14px;
  display: grid;
  gap: 10px;
}

.marked-item-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.marked-item-icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.marked-item-title {
  color: #1d4ed8;
  font-size: 20px;
  font-weight: 700;
}

.marked-item-desc {
  font-size: 20px;
  line-height: 1.45;
}

.marked-item-desc .label {
  color: #334155;
}

.marked-item-desc .value {
  color: #dc2626;
  font-weight: 600;
}

.marked-empty {
  border-radius: var(--ui-radius-sm);
  border: 1px dashed var(--ui-border);
  background: #f8fafc;
  padding: 18px 14px;
  text-align: center;
  font-size: 15px;
  color: var(--ui-muted);
}

.side-card {
  display: grid;
  gap: 12px;
}

.side-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.side-list {
  margin: 0;
  padding-left: 20px;
  display: grid;
  gap: 8px;
  color: var(--ui-muted);
  font-size: 14px;
}

.marked-contact {
  font-size: 14px;
  color: var(--ui-muted);
}

.marked-contact-link {
  color: var(--ui-primary);
  font-weight: 700;
  text-decoration: none;
  transition: color 0.2s ease;
}

.marked-contact-link:hover {
  color: var(--ui-primary-hover);
}

.marked-qrcode {
  width: 136px;
  height: 136px;
  object-fit: contain;
  border-radius: var(--ui-radius-sm);
  border: 1px solid var(--ui-border);
  justify-self: center;
  background: #fff;
}

.primary-btn {
  height: 44px;
  border-radius: var(--ui-radius-sm);
  border: 0;
  background: var(--ui-primary);
  color: #fff;
  font-weight: 700;
  transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.primary-btn:hover,
.primary-btn:focus {
  transform: translateY(-1px);
  background: var(--ui-primary-hover);
  box-shadow: 0 8px 16px rgba(37, 99, 235, 0.3);
}

.primary-btn.block {
  width: 100%;
}

.public-root.mobile-device {
  padding: 0;
  background: #f3f4f6;
  overflow-x: hidden;
}

.mobile-query-shell,
.mobile-marked-shell {
  width: 100%;
  max-width: 375px;
  margin: 0 auto;
  padding: 16px 16px calc(88px + env(safe-area-inset-bottom));
  box-sizing: border-box;
  display: grid;
  gap: 14px;
}

.mobile-card {
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 16px;
  box-sizing: border-box;
}

.mobile-page-title {
  margin: 0;
  font-size: 22px;
  line-height: 1.35;
  font-weight: 800;
  color: #111827;
}

.mobile-page-subtitle {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.5;
  color: #4b5563;
}


.mobile-page-message {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.5;
}

.mobile-page-message.ok {
  color: #059669;
}

.mobile-page-message.err {
  color: #dc2626;
}

.mobile-marked-count {
  margin-top: 8px;
  font-size: 16px;
  color: #1f2937;
}

.mobile-marked-count .highlight {
  color: #2563eb;
  font-weight: 700;
}

.mobile-search-bar {
  margin-top: 12px;
}

.mobile-search-input {
  width: 100%;
}

.mobile-search-input :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 12px;
  border: 1px solid #d1d5db;
  background: #fff;
  padding: 0 12px;
  box-shadow: none !important;
}

.mobile-search-input :deep(.el-input__wrapper.is-focus) {
  border-color: #93c5fd;
}

.mobile-search-input :deep(.el-input__inner) {
  font-size: 14px;
  color: #111827;
}


.mobile-section-title {
  margin: 0 0 12px;
  font-size: 20px;
  line-height: 1.35;
  font-weight: 800;
  color: #111827;
}

.mobile-platform-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  column-gap: 8px;
  row-gap: 12px;
}

.mobile-platform-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  text-align: center;
  min-width: 0;
}

.mobile-platform-icon-wrap {
  width: 44px;
  height: 44px;
  min-width: 44px;
  min-height: 44px;
  border-radius: 10px;
  background: #eef2ff;
  display: grid;
  place-items: center;
}

.mobile-platform-icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.mobile-platform-label {
  font-size: 14px;
  line-height: 1.35;
  color: #4b5563;
}

.mobile-appeal-list {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 8px;
}

.mobile-appeal-item {
  font-size: 14px;
  line-height: 1.5;
  color: #374151;
}

.mobile-marked-list {
  display: grid;
  gap: 12px;
}

.mobile-marked-item {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px;
  display: grid;
  gap: 8px;
}

.mobile-marked-item-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mobile-marked-item-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.mobile-marked-item-name {
  font-size: 16px;
  color: #1f2937;
  font-weight: 700;
}

.mobile-marked-item-desc {
  font-size: 14px;
  line-height: 1.5;
  color: #4b5563;
}

.mobile-marked-item-desc span {
  color: #dc2626;
  font-weight: 600;
}

.mobile-empty {
  border-radius: 12px;
  border: 1px dashed #d1d5db;
  background: #f9fafb;
  padding: 14px;
  font-size: 14px;
  color: #6b7280;
  text-align: center;
}

.mobile-support-card {
  display: grid;
  gap: 10px;
}

.mobile-support-text {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.5;
}

.mobile-support-link {
  color: #2563eb;
  text-decoration: none;
  font-weight: 700;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
}

.mobile-support-qrcode {
  width: 120px;
  height: 120px;
  object-fit: contain;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
}

.mobile-bottom-nav {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 64px;
  padding: 8px 16px calc(8px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.98);
  border-top: 1px solid #e5e7eb;
  z-index: 99;
}

.mobile-bottom-nav-inner {
  max-width: 375px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-items: center;
}

.mobile-bottom-link {
  min-height: 44px;
  border-radius: 12px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.mobile-bottom-primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
  cursor: pointer;
}
.free-query-wrap {
  max-width: 1080px;
  margin: 0 auto;
}

.query-card {
  border-radius: 8px;
}

.title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 16px;
}

.quota-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.query-row {
  display: grid;
  grid-template-columns: 1fr auto auto auto;
  gap: 10px;
  margin-bottom: 10px;
}

.msg {
  min-height: 24px;
  line-height: 24px;
  color: var(--el-text-color-regular);
  margin-bottom: 10px;
}

.msg.ok {
  color: var(--el-color-success);
}

.msg.err {
  color: var(--el-color-danger);
}

.result-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.result-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 10px;
  background: #fff;
}

.result-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.platform-name {
  font-weight: 600;
}

.result-text {
  color: var(--el-text-color-primary);
}

.result-text.marked {
  color: var(--el-color-danger);
  font-weight: 600;
}

.result-text.muted {
  color: var(--el-text-color-secondary);
}

.result-text.platform-free-disabled {
  font-style: normal;
  color: var(--el-text-color-secondary);
}
@media (min-width: 768px) {
  .public-layout {
    grid-template-columns: minmax(0, 1fr) 280px;
  }
}

@media (min-width: 1200px) {
  .public-layout {
    grid-template-columns: minmax(0, 1fr) 320px;
  }

  .platform-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1199px) {
  .module-title {
    font-size: 22px;
  }

  .marked-item-title {
    font-size: 18px;
  }

  .marked-item-desc {
    font-size: 18px;
  }
}

@media (max-width: 767px) {
  .public-root {
    padding: 14px;
  }

  .ui-card {
    border-radius: var(--ui-radius-sm);
    padding: 16px;
  }

  .public-nav {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-title {
    font-size: 30px;
  }
  .hero-search {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-button {
    width: 100%;
  }

  .platform-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .module-title {
    font-size: 20px;
  }

  .marked-item-desc {
    font-size: 16px;
  }
  .query-row {
    grid-template-columns: 1fr;
  }
  .result-list {
    grid-template-columns: 1fr;
  }
}
</style>