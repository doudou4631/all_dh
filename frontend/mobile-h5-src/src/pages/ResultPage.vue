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
          <div class="result-teddy-card">
            <label class="result-teddy-field">
              <span class="result-teddy-field-label">手机号</span>
              <input class="result-teddy-field-input" :value="queryPhone" readonly />
            </label>
            <label class="result-teddy-field">
              <span class="result-teddy-field-label">发送号码</span>
              <input class="result-teddy-field-input" :value="teddyChannelCode" readonly />
            </label>
            <label class="result-teddy-field">
              <span class="result-teddy-field-label">编辑短信</span>
              <input class="result-teddy-field-input" :value="teddyVerifyCode" readonly />
            </label>
          </div>
          <div class="result-teddy-actions">
            <button type="button" class="result-teddy-btn result-teddy-btn--primary" :disabled="!canSendTeddyCode" @click="sendTeddyCode">
              发送验证码
            </button>
            <button
              type="button"
              class="result-teddy-btn result-teddy-btn--warning"
              :disabled="!canSendTeddyCode || teddyPolling"
              @click="startTeddyPolling"
            >
              {{ teddyPolling ? '验证中...' : '我已发送开始验证' }}
            </button>
            <button type="button" class="result-teddy-btn result-teddy-btn--light" :disabled="teddyCodeLoading" @click="requestTeddyCode">
              {{ teddyCodeLoading ? '获取中...' : '重新获取验证码' }}
            </button>
          </div>
          <div class="result-teddy-tips">
            <p>• 请确保手机号为上方号码且可发送短信</p>
            <p>• 发送完短信请点击我已发送开始验证</p>
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
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { loadMobileRuntimeConfig, useMobileRuntimeConfig } from '@/runtime/mobileRuntimeConfig'
import {
  fetchSingleQuery,
  getTeddyVerifyCode,
  getPlatformIcon,
  isTeddyGetCodeSuccess,
  isMarked,
  isRegisteredUser,
  isTeddyNormalMarkItem,
  submitTeddyVerification,
  verifyTeddyCode
} from '@/services/freeQueryBridge'
import { recordQueryForCurrentUser } from '@/services/queryStats'
import { addSingleRecord } from '@/services/queryRecords'
import { confirmDialog, notify, openWechatModal as openBottomNavWechatModal } from '@/services/bottomNavUi'

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
let teddyTimer = null

const config = computed(() => state.config)
const servicePhone = computed(() => String(config.value.servicePhone || '--'))
const callHref = computed(() => (servicePhone.value && servicePhone.value !== '--' ? `tel:${servicePhone.value}` : 'tel:'))
const wechatQrUrl = computed(() => String(config.value.wechatQrUrl || '/mobile-h5/assets/icons/customer-wechat.png'))
const displayPhone = computed(() => queryPhone.value || '--')
const canUseTeddySms = computed(() => isRegisteredUser())
const hasTeddyNormalMark = computed(() => markedItems.value.some((item) => isTeddyNormalMarkItem(item)))
const showInlineTeddyPanel = computed(() => canUseTeddySms.value && hasTeddyNormalMark.value && !!queryPhone.value)
const canSendTeddyCode = computed(
  () => !!teddyChannelCode.value && !!teddyVerifyCode.value && !teddyCodeLoading.value
)
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
  if (teddyTimer) {
    clearInterval(teddyTimer)
    teddyTimer = null
  }
}

function resetTeddyCodeData() {
  teddyChannelCode.value = ''
  teddyVerifyCode.value = ''
}

async function requestTeddyCode() {
  if (!showInlineTeddyPanel.value) return
  if (!queryPhone.value) {
    notify('未接收到手机号参数')
    return
  }
  teddyCodeLoading.value = true
  resetTeddyCodeData()
  stopTeddyPolling()
  try {
    const result = await getTeddyVerifyCode(queryPhone.value, config.value.teddyProtocolBase)
    if (Number(result.code) === 401) {
      notify('当前账号暂无短信处理权限，请联系客服')
      return
    }
    if (!isTeddyGetCodeSuccess(result)) {
      notify(result.msg || '短信验证码获取失败，请稍后重试')
      return
    }
    teddyChannelCode.value = result.channelCode
    teddyVerifyCode.value = result.verifyCode
  } catch (error) {
    notify(error?.message || '短信验证码获取失败，请重试')
  } finally {
    teddyCodeLoading.value = false
  }
}

function sendTeddyCode() {
  if (!canSendTeddyCode.value) {
    notify('验证码内容不完整，请重新获取')
    return
  }
  const userAgent = navigator.userAgent || ''
  const body = encodeURIComponent(teddyVerifyCode.value)
  const isIOS = /iPhone|iPad|iPod|iOS/i.test(userAgent)
  const smsHref = isIOS ? `sms:${teddyChannelCode.value}&body=${body}` : `sms:${teddyChannelCode.value}?body=${body}`
  window.location.href = smsHref
}

async function submitTeddyInlineVerify(captcha) {
  try {
    const result = await submitTeddyVerification(queryPhone.value, captcha)
    const code = Number(result?.code)
    if (code === 0) {
      const message = String(result?.msg || '已提交，请稍后查看处理结果')
      const handleDone = () => {
        runQuery()
      }
      confirmDialog(`申诉结果：${message}`, handleDone, handleDone)
      return
    }
    notify(result?.msg || '验证失败，请重试')
    requestTeddyCode()
  } catch (error) {
    notify(error?.message || '验证失败，请重试')
  }
}

async function pollTeddyVerify() {
  if (!queryPhone.value || !teddyVerifyCode.value || teddyVerifying.value) return
  teddyVerifying.value = true
  try {
    const result = await verifyTeddyCode(queryPhone.value, teddyVerifyCode.value, config.value.teddyProtocolBase)
    if (result.done || result.checkResult === 0) {
      stopTeddyPolling()
      submitTeddyInlineVerify(result.captcha)
    }
  } catch (error) {
    stopTeddyPolling()
    notify(error?.message || '验证码校验失败，请重试')
  } finally {
    teddyVerifying.value = false
  }
}

function startTeddyPolling() {
  if (!teddyVerifyCode.value) {
    notify('请先发送短信验证码')
    return
  }
  if (teddyPolling.value) return
  teddyPolling.value = true
  notify('验证码校验中，请稍候')
  pollTeddyVerify()
  teddyTimer = setInterval(() => {
    pollTeddyVerify()
  }, 3000)
}

async function syncInlineTeddyPanel() {
  stopTeddyPolling()
  resetTeddyCodeData()
  if (!showInlineTeddyPanel.value) return
  await requestTeddyCode()
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
