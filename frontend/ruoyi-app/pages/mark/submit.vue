<template>
  <view class="submit-page">
    <uni-card :is-shadow="false" class="platform-card">
      <view class="platform-name">{{ platformName }}</view>
      <view class="platform-meta">
        <text>{{ modeText }}</text>
        <text>剩余 {{ remainCount }} 次</text>
        <text>单价 {{ unitPrice }} 次</text>
      </view>
      <view v-if="!platformEnabled" class="warn-box">当前平台未开启，请联系管理员</view>
      <view v-else-if="remainCount < unitPrice" class="warn-box">当前平台剩余次数不足，无法提交</view>
    </uni-card>

    <uni-card :is-shadow="false" class="form-card">
      <template v-if="isSmsMode">
        <uni-forms label-position="top">
          <uni-forms-item label="手机号">
            <uni-easyinput v-model="singlePhone" type="number" maxlength="11" placeholder="请输入11位手机号" @input="handlePhoneInput" />
          </uni-forms-item>
          <uni-forms-item label="短信验证码">
            <uni-easyinput v-model="smsCode" type="number" maxlength="6" placeholder="请输入6位验证码" @input="handleSmsInput" />
          </uni-forms-item>
        </uni-forms>
        <view class="btn-row">
          <button v-if="isTdxSecond" class="light-btn" :disabled="smsSending || !canSendSms" @click="sendSms">
            {{ smsSending ? '发送中...' : '获取短信' }}
          </button>
          <button v-else class="light-btn" @click="openTencentPage">打开腾讯取码</button>
          <button class="primary-btn" :disabled="submitting || !canSubmitSms" @click="submitSmsOrder">
            {{ submitting ? '提交中...' : '提交处理' }}
          </button>
        </view>
      </template>

      <template v-else>
        <view class="textarea-label">号码内容</view>
        <textarea v-model="phonesText" class="phones-textarea" placeholder="可粘贴混合文本，点击号码提取整理；每行一个号码" />
        <view class="stats-row">
          <text>有效 {{ phoneStats.validCount }}</text>
          <text>重复 {{ phoneStats.duplicateCount }}</text>
          <text>无效 {{ phoneStats.invalidCount }}</text>
          <text>预计扣 {{ expectedDeduct }} 次</text>
        </view>
        <view v-if="phoneStats.validCount && expectedDeduct > remainCount" class="warn-box">
          剩余次数不足，本次需 {{ expectedDeduct }} 次
        </view>
        <view class="btn-row">
          <button class="light-btn" :disabled="!phonesText.trim() || submitting || precheckLoading" @click="extractPhones">号码提取</button>
          <button class="primary-btn" :disabled="!canSubmitPhones" @click="submitPhones">{{ submitButtonText }}</button>
        </view>
      </template>
    </uni-card>

    <uni-card v-if="precheckItems.length" :is-shadow="false" class="result-card">
      <view class="card-title">查询结果</view>
      <view class="result-item" v-for="item in precheckItems" :key="item.phone">
        <checkbox :checked="selectedPhoneMap[item.phone]" :disabled="!isPrecheckSubmittable(item)" @click="togglePrecheckItem(item)" />
        <view class="result-main">
          <view class="result-phone">{{ item.phone }}</view>
          <view class="result-text">{{ precheckResultText(item) }}</view>
        </view>
        <uni-tag :text="item.querySuccess ? '成功' : '失败'" :type="item.querySuccess ? 'success' : 'error'" size="small" />
      </view>
      <button class="primary-btn full" :disabled="clearSubmitting || !selectedPhones.length" @click="submitSelectedPrecheck">
        {{ clearSubmitting ? '提交中...' : `提交消除 ${selectedPhones.length} 个` }}
      </button>
    </uni-card>

    <uni-card v-if="submitResult" :is-shadow="false" class="result-card">
      <view class="card-title">提交结果</view>
      <view class="info-row"><text>订单号</text><text>{{ submitResult.orderNo || '-' }}</text></view>
      <view v-if="submitResult.phone" class="info-row"><text>手机号</text><text>{{ submitResult.phone }}</text></view>
      <view class="info-row"><text>提交数量</text><text>{{ submitResult.phoneCount || submitResult.totalCount || 1 }}</text></view>
      <view v-if="submitResult.message" class="info-row"><text>处理结果</text><text>{{ submitResult.message }}</text></view>
      <button class="light-btn full" @click="goOrders">查看记录</button>
    </uni-card>
  </view>
</template>

<script setup>
  import { computed, ref } from 'vue'
  import { onLoad, onUnload } from '@dcloudio/uni-app'
  import {
    createMarkUserClearOrder,
    getMarkUserTencentSubmitResult,
    listMarkUserPlatformPrice,
    precheckMarkUserOrder,
    sendMarkUserTdxSecondCode,
    submitMarkUserTdxSecond,
    submitMarkUserTencent
  } from '@/api/markUser'
  import {
    formatRemain,
    formatUnitPrice,
    isPlatformEnabled,
    isPrecheckSubmittable,
    normalizeCode,
    normalizePhone,
    parsePhones,
    platformMode,
    platformModeText,
    precheckResultText,
    TDX_SECOND_CODE
  } from '@/utils/markUser'

  const platformCode = ref('')
  const routePlatformName = ref('')
  const platform = ref(null)
  const singlePhone = ref('')
  const smsCode = ref('')
  const phonesText = ref('')
  const smsSending = ref(false)
  const submitting = ref(false)
  const precheckLoading = ref(false)
  const clearSubmitting = ref(false)
  const precheckItems = ref([])
  const selectedPhones = ref([])
  const precheckPayload = ref(null)
  const submitResult = ref(null)
  let pollStopped = false

  const platformName = computed(() => platform.value?.platformName || routePlatformName.value || platformCode.value || '标记平台')
  const mode = computed(() => platformMode(platform.value || { platformCode: platformCode.value, platformName: platformName.value }))
  const modeText = computed(() => platformModeText(platform.value || { platformCode: platformCode.value, platformName: platformName.value }))
  const isSmsMode = computed(() => mode.value === 'sms')
  const isTdxSecond = computed(() => normalizeCode(platformCode.value) === TDX_SECOND_CODE)
  const unitPrice = computed(() => formatUnitPrice(platform.value?.unitPrice))
  const remainCount = computed(() => formatRemain(platform.value?.remainCount))
  const platformEnabled = computed(() => isPlatformEnabled(platform.value || {}))
  const phoneStats = computed(() => parsePhones(phonesText.value))
  const expectedDeduct = computed(() => phoneStats.value.validCount * unitPrice.value)
  const canSubmitPhones = computed(() => !submitting.value && !precheckLoading.value && platformEnabled.value && phoneStats.value.validCount > 0 && expectedDeduct.value <= remainCount.value && remainCount.value >= unitPrice.value)
  const canSendSms = computed(() => /^\d{11}$/.test(singlePhone.value) && platformEnabled.value && remainCount.value >= unitPrice.value)
  const canSubmitSms = computed(() => canSendSms.value && /^\d{6}$/.test(smsCode.value))
  const submitButtonText = computed(() => submitting.value || precheckLoading.value ? '处理中...' : (mode.value === 'precheck' ? '一键批量查询' : '批量提交'))
  const selectedPhoneMap = computed(() => selectedPhones.value.reduce((map, phone) => ({ ...map, [phone]: true }), {}))

  function toast(title) {
    uni.showToast({ title, icon: 'none' })
  }

  function loadPlatform() {
    listMarkUserPlatformPrice().then(res => {
      const list = Array.isArray(res.data) ? res.data : []
      platform.value = list.find(item => normalizeCode(item.platformCode) === normalizeCode(platformCode.value))
        || list.find(item => String(item.platformName || '') === platformName.value)
        || null
    }).catch(() => {})
  }

  function handlePhoneInput(value) {
    singlePhone.value = normalizePhone(value).slice(0, 11)
  }

  function handleSmsInput(value) {
    smsCode.value = normalizePhone(value).slice(0, 6)
  }

  function extractPhones() {
    const phones = phoneStats.value.phones
    if (!phones.length) {
      toast('未提取到有效号码')
      return
    }
    phonesText.value = phones.join('\n')
    toast(`已提取 ${phones.length} 个号码`)
  }

  function submitPhones() {
    const phones = phoneStats.value.phones
    if (!phones.length) {
      toast('请输入有效号码')
      return
    }
    submitResult.value = null
    if (mode.value === 'precheck') {
      executePrecheck(phones)
    } else {
      createDirectOrder(phones)
    }
  }

  function createDirectOrder(phones) {
    submitting.value = true
    createMarkUserClearOrder({
      platformCode: platformCode.value,
      platformName: platformName.value,
      phones,
      requestNo: '',
      remark: ''
    }).then(res => {
      const order = res.data?.order || res.data || {}
      submitResult.value = {
        orderNo: order.orderNo || '-',
        phoneCount: Number(order.totalCount || phones.length || 0)
      }
      phonesText.value = ''
      toast(`正常提交了 ${submitResult.value.phoneCount} 个手机号码`)
      loadPlatform()
    }).finally(() => {
      submitting.value = false
    })
  }

  function executePrecheck(phones) {
    precheckLoading.value = true
    precheckItems.value = []
    selectedPhones.value = []
    const payload = { platformCode: platformCode.value, platformName: platformName.value, phones, requestNo: '', remark: '' }
    precheckMarkUserOrder(payload).then(res => {
      const items = Array.isArray(res.data?.items) ? res.data.items : []
      precheckItems.value = items
      selectedPhones.value = items.filter(isPrecheckSubmittable).map(item => item.phone)
      precheckPayload.value = payload
      toast(`查询完成，可提交 ${selectedPhones.value.length} 个`)
    }).finally(() => {
      precheckLoading.value = false
    })
  }

  function togglePrecheckItem(item) {
    if (!isPrecheckSubmittable(item)) return
    const index = selectedPhones.value.indexOf(item.phone)
    if (index >= 0) selectedPhones.value.splice(index, 1)
    else selectedPhones.value.push(item.phone)
  }

  function submitSelectedPrecheck() {
    if (!precheckPayload.value || !selectedPhones.value.length) return
    clearSubmitting.value = true
    createMarkUserClearOrder({ ...precheckPayload.value, phones: [...selectedPhones.value], requestNo: '' }).then(res => {
      const order = res.data?.order || res.data || {}
      submitResult.value = { orderNo: order.orderNo || '-', phoneCount: Number(order.totalCount || selectedPhones.value.length || 0) }
      precheckItems.value = []
      selectedPhones.value = []
      phonesText.value = ''
      toast(`提交成功 ${submitResult.value.phoneCount} 个`)
      loadPlatform()
    }).finally(() => {
      clearSubmitting.value = false
    })
  }

  function openTencentPage() {
    // #ifdef H5
    window.open('https://yun.m.qq.com/person_apply.html', '_blank')
    // #endif
    // #ifndef H5
    uni.setClipboardData({ data: 'https://yun.m.qq.com/person_apply.html' })
    // #endif
  }

  function sendSms() {
    if (!canSendSms.value) {
      toast('请输入11位手机号，并确认额度充足')
      return
    }
    smsSending.value = true
    sendMarkUserTdxSecondCode({ phone: singlePhone.value, line: 'line1' }).then(res => {
      toast(res.data?.message || res.msg || '短信验证码已发送')
    }).finally(() => {
      smsSending.value = false
    })
  }

  function submitSmsOrder() {
    if (!canSubmitSms.value) {
      toast('请输入11位手机号和6位验证码')
      return
    }
    submitting.value = true
    pollStopped = false
    const request = isTdxSecond.value
      ? submitMarkUserTdxSecond({ platformCode: platformCode.value, phone: singlePhone.value, smsCode: smsCode.value, line: 'line1', rotate: false })
      : submitMarkUserTencent({ platformCode: platformCode.value, phone: singlePhone.value, smsCode: smsCode.value })
    request.then(res => {
      const data = res.data || {}
      submitResult.value = { ...data, phone: singlePhone.value, phoneCount: 1, message: data.message || res.msg || '提交成功' }
      toast(isTdxSecond.value ? submitResult.value.message : '提交成功，处理中')
      if (!isTdxSecond.value && data.itemId) pollTencentResult(data.itemId)
      singlePhone.value = ''
      smsCode.value = ''
      loadPlatform()
    }).finally(() => {
      submitting.value = false
    })
  }

  async function pollTencentResult(itemId) {
    for (let i = 0; i < 10; i++) {
      if (pollStopped) return
      if (i > 0) await new Promise(resolve => setTimeout(resolve, 5000))
      try {
        const res = await getMarkUserTencentSubmitResult(itemId)
        const data = res.data || {}
        if (data.orderNo && submitResult.value) submitResult.value.orderNo = data.orderNo
        if (String(data.processStatus || '') === '1') {
          toast('提交成功')
          return
        }
        if (String(data.processStatus || '') === '2') {
          toast('提交失败，验证码错误或者失效')
          return
        }
      } catch (e) {}
    }
  }

  function goOrders() {
    uni.switchTab({ url: '/pages/mark/orders' })
  }

  onLoad(options => {
    platformCode.value = decodeURIComponent(options.platformCode || '')
    routePlatformName.value = decodeURIComponent(options.platformName || '')
    loadPlatform()
  })

  onUnload(() => {
    pollStopped = true
  })
</script>

<style lang="scss" scoped>
  page { background: #f5f6f7; }
  .submit-page { padding: 20rpx 20rpx 40rpx; }
  .platform-name { font-size: 34rpx; color: #1f2937; font-weight: 700; }
  .platform-meta { display: flex; flex-wrap: wrap; gap: 20rpx; margin-top: 12rpx; color: #667085; font-size: 24rpx; }
  .warn-box { margin-top: 18rpx; padding: 18rpx; border-radius: 12rpx; color: #d92d20; background: #fff1f0; font-size: 25rpx; }
  .textarea-label { margin-bottom: 12rpx; color: #344054; font-size: 27rpx; }
  .phones-textarea { width: 100%; height: 300rpx; padding: 20rpx; border-radius: 14rpx; background: #f7f8fa; box-sizing: border-box; font-size: 27rpx; }
  .stats-row { display: flex; flex-wrap: wrap; gap: 18rpx; margin-top: 16rpx; color: #667085; font-size: 24rpx; }
  .btn-row { display: grid; grid-template-columns: 1fr 1fr; gap: 18rpx; margin-top: 24rpx; }
  .primary-btn, .light-btn { height: 78rpx; line-height: 78rpx; border-radius: 39rpx; font-size: 28rpx; }
  .primary-btn { color: #fff; background: #1f6bff; }
  .light-btn { color: #1f6bff; background: #eef5ff; }
  .primary-btn[disabled], .light-btn[disabled] { opacity: .55; }
  .full { margin-top: 24rpx; width: 100%; }
  .card-title { margin-bottom: 18rpx; font-size: 31rpx; color: #1f2937; font-weight: 700; }
  .result-item { display: flex; align-items: center; gap: 14rpx; padding: 18rpx 0; border-bottom: 1rpx solid #eef0f4; }
  .result-main { flex: 1; min-width: 0; }
  .result-phone { color: #1f2937; font-size: 28rpx; font-weight: 700; }
  .result-text { margin-top: 6rpx; color: #667085; font-size: 24rpx; }
  .info-row { display: flex; justify-content: space-between; gap: 20rpx; padding: 18rpx 0; border-bottom: 1rpx solid #eef0f4; color: #344054; font-size: 26rpx; }
</style>
