<template>
  <div class="result-app">
    <header class="result-hero">
      <div class="result-hero-bg" aria-hidden="true"></div>
      <div class="result-hero-inner">
        <p class="result-hero-tip">针对全网所有平台查询到您的号码：</p>
        <p class="result-hero-phone">{{ displayPhone }}</p>
        <p class="result-hero-count">
          被 <span>{{ markedItems.length }}</span> 个平台标记
        </p>
      </div>
      <aside class="result-float-actions">
        <a class="result-float-btn" :href="callHref">
          <span class="result-float-icon result-float-icon--phone" aria-hidden="true">
            <img :src="servicePhoneIcon" alt="" width="22" height="22" />
          </span>
          <span class="result-float-label">服务电话</span>
        </a>
        <button type="button" class="result-float-btn" @click="openWechatModal">
          <span class="result-float-icon result-float-icon--wechat" aria-hidden="true">
            <img :src="wechatIcon" alt="" width="22" height="22" />
          </span>
          <span class="result-float-label">客服微信</span>
        </button>
      </aside>
    </header>

    <main class="result-main">
      <div v-if="loading" class="result-loading">
        <span class="result-loading-spinner" aria-hidden="true"></span>
        正在查询各平台标记，请稍候…
        <small class="result-loading-tip">通常需要 5～15 秒</small>
      </div>
      <div v-else-if="errorMessage" class="result-error">{{ errorMessage }}</div>
      <div v-else-if="markedItems.length" class="result-cards">
        <article v-for="(item, index) in markedItems" :key="`${item.platform}-${index}`" class="result-card">
          <div class="result-card-head">
            <img
              :class="['result-card-icon', { 'result-card-icon--xiaomi': isXiaomiPlatform(item.platform) }]"
              :src="getPlatformIcon(item.platform)"
              :alt="item.platform || '未知平台'"
              loading="lazy"
              decoding="async"
            />
            <h3 class="result-card-title">{{ buildPlatformTitle(item.platform) }}</h3>
          </div>
          <div class="result-card-row">
            <span class="result-card-label">标记名称：</span>
            <span class="result-card-value">{{ getDisplayStatus(item) }}</span>
          </div>
        </article>
        <section v-if="showInlineTeddyPanel" class="result-teddy-inline">
          <h3 class="result-teddy-inline-title">请完成短信验证以去除标记</h3>
          <p class="result-teddy-inline-subtitle">发送短信后自动开始校验，无需手动点击开始验证</p>
          <div class="result-teddy-entry-actions">
            <button type="button" class="result-teddy-btn result-teddy-btn--primary" :disabled="!canSendTeddyCode" @click="sendTeddyCode">
              {{ teddyCodeLoading ? '验证码准备中...' : '发送验证码' }}
            </button>
            <button type="button" class="result-teddy-btn result-teddy-btn--light" :disabled="teddyCodeLoading" @click="requestTeddyCode({ showSuccessMessage: true })">
              {{ teddyCodeLoading ? '获取中...' : '刷新验证码' }}
            </button>
          </div>
          <div class="result-teddy-tips">
            <p>• 请确保手机号为上方号码且可发送短信</p>
            <p>• 发送后会弹出安全验证窗口并自动检测结果</p>
          </div>
        </section>
      </div>
      <div v-else class="result-empty">该号码当前未发现被标记平台</div>
    </main>

    <footer class="result-footer">
      <button type="button" class="result-back-btn" @click="handleBack">返回重新查询</button>
      <p class="result-contact">
        联系我们:
        <a :href="callHref">{{ servicePhone }}</a>
      </p>
      <img class="result-qrcode" :src="wechatQrUrl" alt="客服微信二维码" loading="lazy" decoding="async" />
    </footer>

    <div v-if="teddyModalVisible" class="teddy-verify-modal" role="dialog" aria-modal="true">
      <div class="teddy-verify-mask" @click="closeTeddyVerifyModal"></div>
      <section class="teddy-verify-panel">
        <header class="teddy-verify-header">
          <h3 class="teddy-verify-title">请完成安全验证</h3>
          <button type="button" class="teddy-verify-close" @click="closeTeddyVerifyModal" aria-label="关闭">×</button>
        </header>
        <div class="teddy-verify-body">
          <div class="teddy-verify-card">
            <button type="button" class="teddy-verify-refresh" :disabled="teddyCodeLoading" @click="refreshTeddyCode" aria-label="刷新验证码">
              ↻
            </button>
            <div class="teddy-verify-field">
              <div class="teddy-verify-label">编辑短信</div>
              <div class="teddy-verify-code">{{ teddyVerifyCode || '--' }}</div>
            </div>
            <div class="teddy-verify-field teddy-verify-field--target">
              <div class="teddy-verify-label">发送至</div>
              <div class="teddy-verify-target">{{ teddyChannelCode || '--' }}</div>
            </div>
          </div>
          <p class="teddy-verify-status">
            <template v-if="teddyPolling">
              等待短信验证，剩余 <span>{{ teddyCountdown }}</span> s
            </template>
            <template v-else-if="teddyStatus === 'timeout'">等待短信验证超时，请点击右上角刷新后重试</template>
            <template v-else-if="teddyStatus === 'success'">验证成功，正在提交...</template>
            <template v-else-if="teddyStatus === 'error'">{{ teddyStatusMessage || '验证码校验失败，请刷新后重试' }}</template>
            <template v-else>等待短信验证，请发送短信后留在当前页面</template>
          </p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { loadMobileRuntimeConfig, useMobileRuntimeConfig } from '@/runtime/mobileRuntimeConfig'
import {
  fetchSingleQuery,
  getTeddyVerifyCode,
  getPlatformIcon,
  isRegisteredUser,
  isTeddyGetCodeSuccess,
  isMarked,
  submitTeddyVerification,
  verifyTeddyCode
} from '@/services/freeQueryBridge'
import { recordQueryForCurrentUser } from '@/services/queryStats'
import { addSingleRecord } from '@/services/queryRecords'
import { notify, openWechatModal as openBottomNavWechatModal } from '@/services/bottomNavUi'

const { state, resolveHref } = useMobileRuntimeConfig()
const loading = ref(true)
const errorMessage = ref('')
const markedItems = ref([])
const queryPhone = ref('')
const teddyChannelCode = ref('')
const teddyVerifyCode = ref('')
const teddyCodeLoading = ref(false)
const teddyPolling = ref(false)
const teddyVerifying = ref(false)
const teddyModalVisible = ref(false)
const teddyCountdown = ref(60)
const teddyStatus = ref('idle')
const teddyStatusMessage = ref('')
let teddyPollTimer = null
let teddyCountdownTimer = null

const config = computed(() => state.config)
const servicePhone = computed(() => String(config.value.servicePhone || '--'))
const callHref = computed(() => (servicePhone.value && servicePhone.value !== '--' ? `tel:${servicePhone.value}` : 'tel:'))
const wechatQrUrl = computed(() => String(config.value.wechatQrUrl || '/mobile-h5/assets/icons/customer-wechat.png'))
const displayPhone = computed(() => queryPhone.value || '--')
const hasTeddyMarked = computed(() =>
  markedItems.value.some((item) => String(item?.platform || '').includes('泰迪') && isMarked(item))
)
const canUseTeddySms = computed(() => isRegisteredUser())
const showInlineTeddyPanel = computed(() => canUseTeddySms.value && hasTeddyMarked.value && !!queryPhone.value)
const canSendTeddyCode = computed(
  () => !!teddyChannelCode.value && !!teddyVerifyCode.value && !teddyCodeLoading.value
)
const teddyTimeoutSeconds = computed(() => {
  const raw = Number(config.value.teddyVerifyTimeoutSeconds || config.value.teddyTimeoutSeconds || 60)
  if (!Number.isFinite(raw) || raw <= 0) return 60
  return Math.min(Math.floor(raw), 180)
})
const teddyPollIntervalMs = computed(() => {
  const raw = Number(config.value.teddyVerifyPollIntervalMs || 3000)
  if (!Number.isFinite(raw) || raw < 1000) return 3000
  return Math.min(Math.floor(raw), 10000)
})
const servicePhoneIcon = '/mobile-h5/assets/icons/service-phone.png?v=2'
const wechatIcon = '/mobile-h5/assets/icons/wechat.png?v=2'

function getDisplayStatus(item) {
  const platform = String(item?.platform || '').trim()
  const status = String(item?.status || '').trim()
  if (platform === '移动高频' && (status === '有标记' || status === '普通标记')) {
    return '高频拦截'
  }
  if (!status || status === '有标记') {
    return '普通标记'
  }
  return status
}

function getQueryPhoneFromUrl() {
  const params = new URLSearchParams(window.location.search || '')
  return String(params.get('phone') || '').trim()
}

function isXiaomiPlatform(platformName) {
  return String(platformName || '').indexOf('小米') >= 0
}

function buildPlatformTitle(platformName) {
  const name = String(platformName || '').trim() || '未知平台'
  return name.indexOf('标记') >= 0 ? `[${name}]` : `[${name} 标记]`
}

function stopTeddyPolling() {
  teddyPolling.value = false
  teddyVerifying.value = false
  if (teddyPollTimer) {
    clearInterval(teddyPollTimer)
    teddyPollTimer = null
  }
  if (teddyCountdownTimer) {
    clearInterval(teddyCountdownTimer)
    teddyCountdownTimer = null
  }
}

function resetTeddyCodeData() {
  teddyChannelCode.value = ''
  teddyVerifyCode.value = ''
}
function setTeddyErrorStatus(message) {
  teddyStatus.value = 'error'
  teddyStatusMessage.value = String(message || '验证码校验失败，请刷新后重试')
}

async function requestTeddyCode(options = {}) {
  const showSuccessMessage = !!options.showSuccessMessage
  if (!showInlineTeddyPanel.value) return false
  if (!queryPhone.value) {
    notify('未接收到手机号参数')
    return false
  }
  teddyCodeLoading.value = true
  resetTeddyCodeData()
  stopTeddyPolling()
  teddyStatus.value = 'idle'
  teddyStatusMessage.value = ''
  teddyCountdown.value = teddyTimeoutSeconds.value
  try {
    const result = await getTeddyVerifyCode(queryPhone.value, config.value.teddyProtocolBase)
    if (Number(result.code) === 401) {
      const message = '当前账号暂无短信处理权限，请联系客服'
      setTeddyErrorStatus(message)
      notify(message)
      return false
    }
    if (!isTeddyGetCodeSuccess(result)) {
      const message = result.msg || '短信验证码获取失败，请稍后重试'
      setTeddyErrorStatus(message)
      notify(message)
      return false
    }
    teddyChannelCode.value = result.channelCode
    teddyVerifyCode.value = result.verifyCode
    if (showSuccessMessage) {
      notify('验证码已刷新，请重新发送短信')
    }
    return true
  } catch (error) {
    const message = error?.message || '短信验证码获取失败，请重试'
    setTeddyErrorStatus(message)
    notify(message)
    return false
  } finally {
    teddyCodeLoading.value = false
  }
}
function openTeddySmsApp() {
  const userAgent = navigator.userAgent || ''
  const body = encodeURIComponent(teddyVerifyCode.value)
  const isIOS = /iPhone|iPad|iPod|iOS/i.test(userAgent)
  const smsHref = isIOS ? `sms:${teddyChannelCode.value}&body=${body}` : `sms:${teddyChannelCode.value}?body=${body}`
  window.location.href = smsHref
}

async function sendTeddyCode() {
  if (!canSendTeddyCode.value) {
    const ok = await requestTeddyCode({ showSuccessMessage: true })
    if (!ok || !canSendTeddyCode.value) {
      notify('验证码内容不完整，请重新获取')
      return
    }
  }
  teddyModalVisible.value = true
  startTeddyPolling()
  openTeddySmsApp()
}

function closeTeddyVerifyModal() {
  teddyModalVisible.value = false
  stopTeddyPolling()
}

async function refreshTeddyCode() {
  if (teddyCodeLoading.value) return
  const ok = await requestTeddyCode()
  if (!ok) return
  teddyModalVisible.value = true
  startTeddyPolling()
  openTeddySmsApp()
}

function openTeddyVerifyModalWithPolling() {
  teddyModalVisible.value = true
  startTeddyPolling()
}

async function submitTeddyInlineVerify(captcha) {
  try {
    const result = await submitTeddyVerification(queryPhone.value, captcha)
    const code = Number(result?.code)
    if (code === 0) {
      teddyModalVisible.value = false
      const message = String(result?.msg || '已提交，请稍后查看处理结果')
      notify(`申诉结果：${message}`)
      return
    }
    notify(result?.msg || '验证失败，请重试')
    requestTeddyCode()
  } catch (error) {
    notify(error?.message || '验证失败，请重试')
  }
}

async function pollTeddyVerify() {
  if (!teddyPolling.value || !queryPhone.value || !teddyVerifyCode.value || teddyVerifying.value) return
  teddyVerifying.value = true
  try {
    const result = await verifyTeddyCode(queryPhone.value, teddyVerifyCode.value, config.value.teddyProtocolBase)
    if (result.done || result.checkResult === 0) {
      stopTeddyPolling()
      teddyStatus.value = 'success'
      teddyStatusMessage.value = ''
      submitTeddyInlineVerify(result.captcha)
    }
  } catch (error) {
    stopTeddyPolling()
    setTeddyErrorStatus(error?.message || '验证码校验失败，请重试')
  } finally {
    teddyVerifying.value = false
  }
}

function startTeddyPolling() {
  if (!teddyVerifyCode.value) {
    notify('请先发送短信验证码')
    return
  }
  stopTeddyPolling()
  teddyPolling.value = true
  teddyStatus.value = 'waiting'
  teddyStatusMessage.value = ''
  teddyCountdown.value = teddyTimeoutSeconds.value
  pollTeddyVerify()
  teddyPollTimer = setInterval(() => {
    pollTeddyVerify()
  }, teddyPollIntervalMs.value)
  teddyCountdownTimer = setInterval(() => {
    if (!teddyPolling.value) return
    teddyCountdown.value = Math.max(0, teddyCountdown.value - 1)
    if (teddyCountdown.value <= 0) {
      stopTeddyPolling()
      teddyStatus.value = 'timeout'
      teddyStatusMessage.value = ''
    }
  }, 1000)
}

async function syncInlineTeddyPanel() {
  stopTeddyPolling()
  teddyModalVisible.value = false
  teddyStatus.value = 'idle'
  teddyStatusMessage.value = ''
  teddyCountdown.value = teddyTimeoutSeconds.value
  resetTeddyCodeData()
  if (!showInlineTeddyPanel.value) return
  const ok = await requestTeddyCode()
  teddyModalVisible.value = true
  if (ok) {
    startTeddyPolling()
  }
}

function openWechatModal() {
  openBottomNavWechatModal()
}

function handleBack() {
  const target = String(config.value.resultBackUrl || '/')
  window.location.assign(resolveHref(target))
}

async function runQuery() {
  queryPhone.value = getQueryPhoneFromUrl()
  if (!queryPhone.value) {
    loading.value = false
    errorMessage.value = '未提供手机号码'
    return
  }
  if (!/^\d{7,15}$/.test(queryPhone.value)) {
    loading.value = false
    errorMessage.value = '请输入正确的号码'
    return
  }

  loading.value = true
  errorMessage.value = ''
  markedItems.value = []
  stopTeddyPolling()
  resetTeddyCodeData()
  try {
    const data = await fetchSingleQuery(queryPhone.value, config.value.apiBase)
    recordQueryForCurrentUser(1)
    const all = Array.isArray(data?.results) ? data.results : []
    const marked = all.filter(isMarked)
    markedItems.value = marked
    const markedPlatforms = marked
      .map((item) => String(item?.platform || '').trim())
      .filter((name, index, arr) => name && arr.indexOf(name) === index)
    addSingleRecord(queryPhone.value, marked.length, markedPlatforms)
    await syncInlineTeddyPanel()
  } catch (error) {
    errorMessage.value = error?.message || '查询失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  document.body.classList.add('result-body')
  loadMobileRuntimeConfig().finally(() => {
    runQuery()
  })
})

onBeforeUnmount(() => {
  stopTeddyPolling()
  document.body.classList.remove('result-body')
})
</script>

<style src="@/styles/result-page.css"></style>
