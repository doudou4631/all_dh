<template>
  <main class="tdx-page">
    <header class="tdx-header">
      <div class="tdx-platform-info">
        <img class="tdx-platform-icon" :src="teddyHomeIcon" alt="泰迪熊" />
        <h1 class="tdx-platform-name">泰迪熊安全验证</h1>
      </div>
      <p class="tdx-platform-desc">请完成短信验证以去除标记</p>
    </header>

    <section class="tdx-card">
      <label class="tdx-field">
        <span class="tdx-field-label">手机号</span>
        <input class="tdx-field-input" :value="phone" readonly />
      </label>
      <p class="tdx-card-tip">发送短信后将自动检测验证结果，无需手动开始验证</p>
    </section>

    <section class="tdx-actions">
      <button type="button" class="tdx-btn tdx-btn--primary" :disabled="!canSendCode" @click="sendCode">
        {{ codeLoading ? '验证码准备中...' : '发送验证码' }}
      </button>
      <button type="button" class="tdx-btn tdx-btn--light" :disabled="codeLoading" @click="requestCode({ showSuccessMessage: true })">
        {{ codeLoading ? '获取中...' : '刷新验证码' }}
      </button>
    </section>

    <section class="tdx-tips">
      <p>• 请确保手机号为上方号码且可发送短信</p>
      <p>• 发送后会弹出安全验证窗口并自动检测结果</p>
    </section>

    <div v-if="verifyModalVisible" class="tdx-verify-modal" role="dialog" aria-modal="true">
      <div class="tdx-verify-mask" @click="closeVerifyModal"></div>
      <section class="tdx-verify-panel">
        <header class="tdx-verify-header">
          <h3 class="tdx-verify-title">请完成安全验证</h3>
          <button type="button" class="tdx-verify-close" @click="closeVerifyModal" aria-label="关闭">×</button>
        </header>
        <div class="tdx-verify-body">
          <div class="tdx-verify-card">
            <button type="button" class="tdx-verify-refresh" :disabled="codeLoading" @click="refreshCode" aria-label="刷新验证码">
              ↻
            </button>
            <div class="tdx-verify-field">
              <div class="tdx-verify-label">编辑短信</div>
              <div class="tdx-verify-code">{{ verifyCode || '--' }}</div>
            </div>
            <div class="tdx-verify-field tdx-verify-field--target">
              <div class="tdx-verify-label">发送至</div>
              <div class="tdx-verify-target">{{ channelCode || '--' }}</div>
            </div>
          </div>
          <p class="tdx-verify-status">
            <template v-if="polling">
              等待短信验证，剩余 <span>{{ countdown }}</span> s
            </template>
            <template v-else-if="verifyStatus === 'timeout'">等待短信验证超时，请点击右上角刷新后重试</template>
            <template v-else-if="verifyStatus === 'success'">验证成功，正在提交...</template>
            <template v-else-if="verifyStatus === 'error'">{{ verifyStatusMessage || '验证码校验失败，请刷新后重试' }}</template>
            <template v-else>等待短信验证，请发送短信后留在当前页面</template>
          </p>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useMobileRuntimeConfig } from '@/runtime/mobileRuntimeConfig'
import {
  getTeddyVerifyCode,
  isRegisteredUser,
  isTeddyGetCodeSuccess,
  submitTeddyVerification,
  verifyTeddyCode
} from '@/services/freeQueryBridge'
import { notify } from '@/services/bottomNavUi'

const { state, resolveHref, loadMobileRuntimeConfig } = useMobileRuntimeConfig()
const teddyHomeIcon = '/mobile-h5/assets/icons/teddy-home.png'

const phone = ref('')
const channelCode = ref('')
const verifyCode = ref('')
const codeLoading = ref(false)
const polling = ref(false)
const verifying = ref(false)
const verifyModalVisible = ref(false)
const countdown = ref(60)
const verifyStatus = ref('idle')
const verifyStatusMessage = ref('')
let pollTimer = null
let countdownTimer = null

const canSendCode = computed(() => !!channelCode.value && !!verifyCode.value && !codeLoading.value)
const canUseTeddySms = computed(() => isRegisteredUser())
const timeoutSeconds = computed(() => {
  const raw = Number(state.config.teddyVerifyTimeoutSeconds || state.config.teddyTimeoutSeconds || 60)
  if (!Number.isFinite(raw) || raw <= 0) return 60
  return Math.min(Math.floor(raw), 180)
})
const pollIntervalMs = computed(() => {
  const raw = Number(state.config.teddyVerifyPollIntervalMs || 3000)
  if (!Number.isFinite(raw) || raw < 1000) return 3000
  return Math.min(Math.floor(raw), 10000)
})

function getPhoneFromUrl() {
  const params = new URLSearchParams(window.location.search || '')
  return String(params.get('phone') || '').trim()
}

function stopPolling() {
  polling.value = false
  verifying.value = false
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

function setVerifyErrorStatus(message) {
  verifyStatus.value = 'error'
  verifyStatusMessage.value = String(message || '验证码校验失败，请刷新后重试')
}

async function requestCode(options = {}) {
  const showSuccessMessage = !!options.showSuccessMessage
  if (!canUseTeddySms.value) {
    notify('请先登录注册用户后再使用短信处理')
    return false
  }
  if (!phone.value) {
    notify('未接收到手机号参数')
    return false
  }

  codeLoading.value = true
  channelCode.value = ''
  verifyCode.value = ''
  stopPolling()
  verifyStatus.value = 'idle'
  verifyStatusMessage.value = ''
  countdown.value = timeoutSeconds.value
  try {
    const result = await getTeddyVerifyCode(phone.value, state.config.teddyProtocolBase)
    if (Number(result.code) === 401) {
      const message = '当前账号暂无短信处理权限，请联系客服'
      setVerifyErrorStatus(message)
      notify(message)
      return false
    }
    if (!isTeddyGetCodeSuccess(result)) {
      const message = result.msg || '短信验证码获取失败，请稍后重试'
      setVerifyErrorStatus(message)
      notify(message)
      return false
    }
    channelCode.value = result.channelCode
    verifyCode.value = result.verifyCode
    if (showSuccessMessage) {
      notify('验证码已刷新，请重新发送短信')
    }
    return true
  } catch (error) {
    const message = error?.message || '短信验证码获取失败，请重试'
    setVerifyErrorStatus(message)
    notify(message)
    return false
  } finally {
    codeLoading.value = false
  }
}
function openSmsApp() {
  const userAgent = navigator.userAgent || ''
  const body = encodeURIComponent(verifyCode.value)
  const isIOS = /iPhone|iPad|iPod|iOS/i.test(userAgent)
  const smsHref = isIOS ? `sms:${channelCode.value}&body=${body}` : `sms:${channelCode.value}?body=${body}`
  window.location.href = smsHref
}

async function sendCode() {
  if (!channelCode.value || !verifyCode.value) {
    const ok = await requestCode({ showSuccessMessage: true })
    if (!ok || !channelCode.value || !verifyCode.value) {
      notify('验证码内容不完整，请重新获取')
      return
    }
  }
  verifyModalVisible.value = true
  startPolling()
  openSmsApp()
}

function closeVerifyModal() {
  verifyModalVisible.value = false
  stopPolling()
}

async function refreshCode() {
  if (codeLoading.value) return
  const ok = await requestCode()
  if (!ok) return
  verifyModalVisible.value = true
  startPolling()
  openSmsApp()
}

function openVerifyModalWithPolling() {
  verifyModalVisible.value = true
  startPolling()
}

async function submitVerify(captcha) {
  try {
    const result = await submitTeddyVerification(phone.value, captcha)
    const code = Number(result?.code)
    if (code === 0) {
      verifyModalVisible.value = false
      const message = String(result?.msg || '已提交，请稍后查看处理结果')
      notify(`申诉结果：${message}`)
      return
    }
    notify(result?.msg || '验证失败，请重试')
    requestCode()
  } catch (error) {
    notify(error?.message || '验证失败，请重试')
  }
}

async function pollVerify() {
  if (!polling.value || !phone.value || !verifyCode.value || verifying.value) return
  verifying.value = true
  try {
    const result = await verifyTeddyCode(phone.value, verifyCode.value, state.config.teddyProtocolBase)
    if (result.done || result.checkResult === 0) {
      stopPolling()
      verifyStatus.value = 'success'
      verifyStatusMessage.value = ''
      submitVerify(result.captcha)
    }
  } catch (error) {
    stopPolling()
    setVerifyErrorStatus(error?.message || '验证码校验失败，请重试')
  } finally {
    verifying.value = false
  }
}

function startPolling() {
  if (!verifyCode.value) {
    notify('请先发送短信验证码')
    return
  }
  stopPolling()
  polling.value = true
  verifyStatus.value = 'waiting'
  verifyStatusMessage.value = ''
  countdown.value = timeoutSeconds.value
  pollVerify()
  pollTimer = setInterval(() => {
    pollVerify()
  }, pollIntervalMs.value)
  countdownTimer = setInterval(() => {
    if (!polling.value) return
    countdown.value = Math.max(0, countdown.value - 1)
    if (countdown.value <= 0) {
      stopPolling()
      verifyStatus.value = 'timeout'
      verifyStatusMessage.value = ''
    }
  }, 1000)
}

function goBack() {
  stopPolling()
  verifyModalVisible.value = false
  if (window.history.length > 1) {
    window.history.back()
    return
  }
  if (phone.value) {
    window.location.assign(resolveHref(`/result/?phone=${encodeURIComponent(phone.value)}`))
    return
  }
  window.location.assign(resolveHref('/'))
}

onMounted(() => {
  phone.value = getPhoneFromUrl()
  if (!canUseTeddySms.value) {
    notify('请先登录注册用户后再使用短信处理')
    if (phone.value) {
      window.location.assign(resolveHref(`/result/?phone=${encodeURIComponent(phone.value)}`))
    } else {
      window.location.assign(resolveHref('/'))
    }
    return
  }
  loadMobileRuntimeConfig().finally(async () => {
    const ok = await requestCode()
    verifyModalVisible.value = true
    if (ok) {
      startPolling()
    }
  })
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.tdx-page {
  min-height: 100vh;
  background: #f9fdff;
  box-sizing: border-box;
  padding: 15px;
}

.tdx-header {
  text-align: center;
  padding: 20px 0 30px;
}

.tdx-platform-info {
  align-items: center;
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}

.tdx-platform-icon {
  border-radius: 8px;
  height: 40px;
  margin-right: 10px;
  width: 40px;
}

.tdx-platform-name {
  color: #333;
  font-size: 18px;
  font-weight: 700;
  margin: 0;
}

.tdx-platform-desc {
  color: #666;
  font-size: 14px;
  line-height: 1.2;
  margin: 0;
}

.tdx-card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  margin-bottom: 15px;
  padding: 15px;
}

.tdx-field {
  align-items: center;
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.tdx-field:last-child {
  margin-bottom: 0;
}

.tdx-field-label {
  color: #333;
  flex: 0 0 76px;
  font-size: 14px;
}

.tdx-field-input {
  background: #f6f8fb;
  border: 1px solid #e5e8ef;
  border-radius: 8px;
  box-sizing: border-box;
  color: #333;
  flex: 1;
  font-size: 14px;
  height: 40px;
  min-width: 0;
  padding: 0 12px;
}
.tdx-card-tip {
  margin: 2px 0 0;
  color: #8691a6;
  font-size: 13px;
  line-height: 1.6;
}

.tdx-actions {
  margin-bottom: 20px;
  padding: 0 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tdx-btn {
  border: none;
  border-radius: 22px;
  cursor: pointer;
  display: block;
  font-size: 15px;
  font-weight: 600;
  height: 44px;
  width: 100%;
}

.tdx-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.tdx-btn--primary {
  background: #3b71fe;
  color: #fff;
}

.tdx-btn--light {
  background: #f3f6fb;
  color: #3b71fe;
}

.tdx-tips {
  color: #999;
  font-size: 12px;
  line-height: 1.8;
  padding: 0 10px;
}

.tdx-tips p {
  margin: 0 0 4px;
}

.tdx-tips p:last-child {
  margin-bottom: 0;
}

.tdx-verify-modal {
  position: fixed;
  inset: 0;
  z-index: 12000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 8px;
}

.tdx-verify-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
}

.tdx-verify-panel {
  position: relative;
  width: min(100%, 380px);
  border-radius: 6px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.26);
}

.tdx-verify-header {
  height: 56px;
  border-bottom: 1px solid #e9e9e9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px 0 20px;
}

.tdx-verify-body {
  padding: 16px;
}

.tdx-verify-close {
  border: none;
  background: transparent;
  color: #a8adb6;
  font-size: 26px;
  line-height: 1;
  padding: 0;
}

.tdx-verify-title {
  margin: 0;
  font-size: 17px;
  font-weight: 500;
  color: #2f2f2f;
  text-align: left;
}

.tdx-verify-card {
  position: relative;
  background: #b9cce5;
  border-radius: 4px;
  padding: 16px 14px 14px;
}

.tdx-verify-refresh {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: #f9fbff;
  color: #2f86f4;
  font-size: 18px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.18);
}

.tdx-verify-refresh:disabled {
  opacity: 0.6;
}

.tdx-verify-field {
  margin-bottom: 10px;
}

.tdx-verify-field:last-child {
  margin-bottom: 0;
}

.tdx-verify-label {
  color: #5f7694;
  font-size: 14px;
}

.tdx-verify-code {
  margin-top: 8px;
  color: #111;
  font-size: 17px;
  font-weight: 500;
  letter-spacing: 0.22em;
  line-height: 1.15;
  word-break: break-all;
}

.tdx-verify-field--target {
  margin-top: 10px;
}

.tdx-verify-target {
  margin-top: 6px;
  color: #2a3b51;
  font-size: 14px;
  font-weight: 500;
  word-break: break-all;
  line-height: 1.2;
}

.tdx-verify-status {
  margin: 14px 0 0;
  background: #f3f3f3;
  border-radius: 4px;
  text-align: center;
  color: #8a8f99;
  font-size: 15px;
  line-height: 1.5;
  padding: 12px 10px;
}

.tdx-verify-status span {
  color: #2f86f4;
  font-weight: 500;
}
</style>
