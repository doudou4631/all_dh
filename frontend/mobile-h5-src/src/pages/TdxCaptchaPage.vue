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
      <label class="tdx-field">
        <span class="tdx-field-label">发送号码</span>
        <input class="tdx-field-input" :value="channelCode" readonly />
      </label>
      <label class="tdx-field">
        <span class="tdx-field-label">编辑短信</span>
        <input class="tdx-field-input" :value="verifyCode" readonly />
      </label>
    </section>

    <section class="tdx-actions">
      <button type="button" class="tdx-btn tdx-btn--primary" :disabled="!canSendCode" @click="sendCode">
        发送验证码
      </button>
      <button type="button" class="tdx-btn tdx-btn--warning" :disabled="!canSendCode || polling" @click="startPolling">
        {{ polling ? '验证中...' : '我已发送开始验证' }}
      </button>
      <button type="button" class="tdx-btn tdx-btn--light" :disabled="codeLoading" @click="requestCode">
        {{ codeLoading ? '获取中...' : '重新获取验证码' }}
      </button>
    </section>

    <section class="tdx-tips">
      <p>• 请确保手机号为上方号码且可发送短信</p>
      <p>• 发送完短信请回到此页面点击我已发送开始验证</p>
    </section>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useMobileRuntimeConfig } from '@/runtime/mobileRuntimeConfig'
import {
  getTeddyVerifyCode,
  isTeddyGetCodeSuccess,
  submitTeddyVerification,
  verifyTeddyCode
} from '@/services/freeQueryBridge'
import { confirmDialog, notify } from '@/services/bottomNavUi'

const { state, resolveHref, loadMobileRuntimeConfig } = useMobileRuntimeConfig()
const teddyHomeIcon = '/mobile-h5/assets/icons/teddy-home.png'

const phone = ref('')
const channelCode = ref('')
const verifyCode = ref('')
const codeLoading = ref(false)
const polling = ref(false)
const verifying = ref(false)
let timer = null

const canSendCode = computed(() => !!channelCode.value && !!verifyCode.value && !codeLoading.value)

function getPhoneFromUrl() {
  const params = new URLSearchParams(window.location.search || '')
  return String(params.get('phone') || '').trim()
}

function stopPolling() {
  polling.value = false
  verifying.value = false
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

async function requestCode() {
  if (!phone.value) {
    notify('未接收到手机号参数')
    return
  }

  codeLoading.value = true
  channelCode.value = ''
  verifyCode.value = ''
  stopPolling()
  try {
    const result = await getTeddyVerifyCode(phone.value, state.config.teddyProtocolBase)
    if (Number(result.code) === 401) {
      notify('当前账号暂无短信处理权限，请联系客服')
      return
    }
    if (!isTeddyGetCodeSuccess(result)) {
      notify(result.msg || '短信验证码获取失败，请稍后重试')
      return
    }
    channelCode.value = result.channelCode
    verifyCode.value = result.verifyCode
  } catch (error) {
    notify(error?.message || '短信验证码获取失败，请重试')
  } finally {
    codeLoading.value = false
  }
}

function sendCode() {
  if (!channelCode.value || !verifyCode.value) {
    notify('验证码内容不完整，请重新获取')
    return
  }
  const userAgent = navigator.userAgent || ''
  const body = encodeURIComponent(verifyCode.value)
  const isIOS = /iPhone|iPad|iPod|iOS/i.test(userAgent)
  const smsHref = isIOS ? `sms:${channelCode.value}&body=${body}` : `sms:${channelCode.value}?body=${body}`
  window.location.href = smsHref
}

async function submitVerify(captcha) {
  try {
    const result = await submitTeddyVerification(phone.value, captcha)
    const code = Number(result?.code)
    if (code === 0) {
      const message = String(result?.msg || '已提交，请稍后查看处理结果')
      confirmDialog(`申诉结果：${message}`, goBack, goBack)
      return
    }
    notify(result?.msg || '验证失败，请重试')
    requestCode()
  } catch (error) {
    notify(error?.message || '验证失败，请重试')
  }
}

async function pollVerify() {
  if (!phone.value || !verifyCode.value || verifying.value) return
  verifying.value = true
  try {
    const result = await verifyTeddyCode(phone.value, verifyCode.value, state.config.teddyProtocolBase)
    if (result.done || result.checkResult === 0) {
      stopPolling()
      submitVerify(result.captcha)
    }
  } catch (error) {
    stopPolling()
    notify(error?.message || '验证码校验失败，请重试')
  } finally {
    verifying.value = false
  }
}

function startPolling() {
  if (!verifyCode.value) {
    notify('请先发送短信验证码')
    return
  }
  if (polling.value) return
  polling.value = true
  notify('验证码校验中，请稍候')
  pollVerify()
  timer = setInterval(() => {
    pollVerify()
  }, 3000)
}

function goBack() {
  stopPolling()
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
  loadMobileRuntimeConfig().finally(() => {
    requestCode()
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

.tdx-actions {
  margin-bottom: 20px;
  padding: 0 10px;
}

.tdx-btn {
  border: none;
  border-radius: 22px;
  cursor: pointer;
  display: block;
  font-size: 15px;
  font-weight: 600;
  height: 44px;
  margin-bottom: 10px;
  width: 100%;
}

.tdx-btn:last-child {
  margin-bottom: 0;
}

.tdx-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.tdx-btn--primary {
  background: #3b71fe;
  color: #fff;
}

.tdx-btn--warning {
  background: #ffb22b;
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
</style>
