<template>
  <div :class="['app-container', 'tencent-mark-page', { 'td-second-mark-page': isTdSecondPlatform }]">
    <el-card shadow="never" class="page-card" :body-style="{ padding: '0' }">
      <div class="platform-main">
        <div v-if="activeTab === 'submit'" class="platform-main-remain">
          <div class="submit-right-remain">
            当前剩余：<span>{{ remainCount }}</span> 次
          </div>
        </div>
        <el-tabs v-model="activeTab" class="sub-tabs">
          <el-tab-pane :label="`${platformName} - 提交号码`" name="submit">
            <div class="submit-pane">
              <div class="tencent-content-shell">
                <div class="tencent-submit-section">
                  <div class="tencent-submit-head">
                    <div class="tencent-submit-title">{{ submitTitle }}</div>
                    <p class="tencent-submit-tip">请先获取短信验证码，再在此提交处理</p>

                    <div class="tencent-guide-box">
                      <div class="tencent-guide-box__title">操作说明</div>
                      <ol class="tencent-guide-box__list">
                        <li v-if="isTdSecondPlatform">请先获取短信验证码！</li>
                        <li v-else>
                          在
                          <el-link
                            type="primary"
                            href="https://yun.m.qq.com/person_apply.html"
                            target="_blank"
                          >腾讯官方页面</el-link>
                          下发验证码
                        </li>
                        <li>填写手机号与验证码，点击「提交处理」</li>
                      </ol>
                    </div>
                  </div>

                  <div class="tencent-form-panel">
                    <div v-if="isTdSecondPlatform && selectedLineUnderMaintenance" class="submit-warning-bar submit-warning-bar--maintenance">
                      {{ selectedLineMaintenanceMessage }}
                    </div>
                    <div v-else-if="!platformEnabled" class="submit-warning-bar">
                      {{ platformClosedMessage }}
                    </div>
                    <div v-else-if="!isLine3 && remainCount < 1" class="submit-warning-bar">
                      当前{{ platformName }}平台剩余次数不足，无法提交。
                    </div>

                    <el-form
                      label-position="top"
                      label-width="auto"
                      size="default"
                      :class="['tencent-submit-form', { 'tencent-submit-form--stacked': !isTdSecondPlatform }]"
                    >
                      <el-form-item v-if="isTdSecondPlatform" label="通道线路">
                        <el-radio-group v-model="form.line" :disabled="smsSending || submitting" @change="handleLineChange">
                          <el-radio-button label="line1" :disabled="line1UnderMaintenance">
                            线路一
                            <span v-if="line1UnderMaintenance" class="tdx-line-maintenance-label">暂时维护</span>
                          </el-radio-button>
                          <el-radio-button label="line2" :disabled="line2UnderMaintenance">
                            线路二备用
                            <span v-if="line2UnderMaintenance" class="tdx-line-maintenance-label">暂时维护</span>
                          </el-radio-button>
                          <el-radio-button label="line3" :disabled="line3UnderMaintenance">
                            线路三
                            <span v-if="line3UnderMaintenance" class="tdx-line-maintenance-label">暂时维护</span>
                            <span class="tdx-line-reuse-label">失败复用</span>
                          </el-radio-button>
                        </el-radio-group>
                      </el-form-item>
                      <el-form-item label="手机号" class="tdx-phone-item">
                        <el-input
                          v-model="form.phone"
                          maxlength="11"
                          clearable
                          :disabled="smsSending || submitting"
                          placeholder="请输入11位手机号"
                          :style="!isTdSecondPlatform ? { width: 'min(100%, 380px)' } : undefined"
                          @input="handlePhoneInput"
                        />
                      </el-form-item>
                      <el-form-item label="验证码" class="tdx-sms-item">
                        <el-input
                          v-model="form.smsCode"
                          maxlength="6"
                          clearable
                          placeholder="请输入6位验证码"
                          :style="!isTdSecondPlatform ? { width: 'min(100%, 380px)' } : undefined"
                          @input="handleSmsInput"
                        />
                      </el-form-item>
                    </el-form>

                    <div :class="['tencent-submit-buttons', { 'tencent-submit-buttons--stacked': !isTdSecondPlatform }]">
                      <el-button
                        v-if="isTdSecondPlatform"
                        type="primary"
                        plain
                        class="tencent-action-btn"
                        :loading="smsSending"
                        :disabled="!platformEnabled || selectedLineUnderMaintenance || (!isLine3 && remainCount < 1) || submitting || smsCountdown > 0"
                        @click="handleFetchSms"
                      >
                        {{ smsCountdown > 0 ? `${smsCountdown}秒后重试` : '获取短信' }}
                      </el-button>
                      <el-button
                        type="primary"
                        class="tencent-action-btn"
                        :loading="submitting"
                        :disabled="!platformEnabled || selectedLineUnderMaintenance || (!isLine3 && remainCount < 1)"
                        v-hasPermi="['server:markUser:order:add']"
                        @click="handleSubmit"
                      >
                        提交处理
                      </el-button>
                      <el-button
                        class="tencent-action-btn"
                        :disabled="submitting || (!form.phone && !form.smsCode)"
                        @click="handleResetForm"
                      >
                        重置
                      </el-button>
                    </div>
                  </div>
                </div>

                <div v-if="!isTdSecondPlatform" class="tencent-result-block">
                  <div class="tencent-result-head">提交结果</div>
                  <el-table :data="resultRows" border stripe class="tencent-result-table">
                    <el-table-column prop="label" label="项目" width="108" align="center" />
                    <el-table-column prop="value" label="信息备注" min-width="200" show-overflow-tooltip />
                    <template #empty>
                      <span class="tencent-result-empty">暂无提交结果</span>
                    </template>
                  </el-table>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="`${platformName} - 任务记录`" name="record">
            <div class="record-pane">
              <el-form
                :model="queryParams"
                :inline="true"
                size="small"
                label-width="68px"
                class="record-form"
                @submit.prevent
              >
                <el-form-item label="综合搜索">
                  <el-input
                    v-model="queryParams.keyword"
                    clearable
                    placeholder="订单号/手机号/用户名"
                    style="width: 220px"
                    @keyup.enter="handleQuery"
                    @clear="handleQuery"
                  />
                </el-form-item>
                <el-form-item label="处理状态">
                  <el-select
                    v-model="queryParams.orderStatus"
                    clearable
                    placeholder="状态"
                    style="width: 120px"
                    @change="handleQuery"
                    @clear="handleQuery"
                  >
                    <el-option label="待处理" value="0" />
                    <el-option label="处理中" value="3" />
                    <el-option label="处理完成" value="1" />
                    <el-option label="处理失败" value="2" />
                  </el-select>
                </el-form-item>
                <el-form-item label="提交时间">
                  <el-date-picker
                    v-model="recordDateRange"
                    type="daterange"
                    range-separator="-"
                    start-placeholder="开始"
                    end-placeholder="结束"
                    format="YYYY/MM/DD"
                    value-format="YYYY-MM-DD"
                    style="width: 240px"
                    @change="handleRecordDateRangeChange"
                  />
                </el-form-item>
                <el-form-item class="record-form__actions">
                  <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
                  <el-button icon="Refresh" @click="resetQuery">重置</el-button>
                  <el-button icon="Download" @click="exportRecordRows">导出</el-button>
                </el-form-item>
              </el-form>

              <RecordBatchActions
                :total="total"
                :selected-rows="recordSelectedRows"
                :allow-remark="!isTdSecondPlatform"
              />

              <el-table
                v-loading="loading"
                :data="orderList"
                border
                stripe
                :row-key="recordRowKey"
                class="record-table"
                @selection-change="handleRecordSelectionChange"
              >
                <el-table-column type="selection" width="48" align="center" />
                <el-table-column label="用户名" prop="userName" min-width="110" show-overflow-tooltip />
                <el-table-column label="号码（点击复制）" min-width="150" show-overflow-tooltip>
                  <template #default="scope">
                    <el-button link type="primary" @click="copyText(scope.row.phonePreview || '')">
                      {{ scope.row.phonePreview || '-' }}
                    </el-button>
                  </template>
                </el-table-column>
                <el-table-column label="平台" min-width="110" show-overflow-tooltip>
                  <template #default="scope">
                    {{ platformName || scope.row.platformName || '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="处理状态" width="92" align="center">
                  <template #default="scope">
                    <el-tag :type="recordStatusType(scope.row)" size="small">
                      {{ recordStatusLabel(scope.row) }}
                    </el-tag>
                  </template>
                </el-table-column>
                 <el-table-column v-if="!isTdSecondPlatform" label="备注" min-width="160" align="center">
                  <template #default="scope"><OrderRemarkEditor :row="scope.row" /></template>
                </el-table-column>
                <el-table-column label="提交时间" min-width="160" align="center">
                  <template #default="scope">
                    {{ formatDateTime(scope.row.createTime) }}
                  </template>
                </el-table-column>
                 <el-table-column label="处理完成" min-width="160" align="center">
                   <template #default="scope">{{ formatDateTime(scope.row.itemProcessedTime || scope.row.completedTime) }}</template>
                 </el-table-column>
                  <el-table-column v-if="isTdSecondPlatform" label="结果" width="80" align="center">
                   <template #default="scope">
                     <el-button link type="primary" @click="handleViewRecordResult(scope.row)">查看</el-button>
                   </template>
                  </el-table-column>
                  <el-table-column label="订单号" prop="orderNo" min-width="180" show-overflow-tooltip />
               </el-table>

              <pagination
                v-show="total > 0"
                :total="total"
                v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize"
                @pagination="getList"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </div>
</template>

<script setup name="MarkUserTencent">
import {
  listMarkUserPlatformPrice,
  listMarkUserOrder,
  submitMarkUserTencent,
  sendMarkUserTdxSecondCode,
  submitMarkUserTdxSecond,
  getMarkUserTencentSubmitResult,
  getMarkUserOrderDetail
} from '@/api/server/markUser'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  TENCENT_STYLE_PLATFORM_CODES,
  TENCENT_PLATFORM_CODE,
  resolveTencentStylePlatformName
} from '@/utils/markTencentPlatform'
import { resolveMarkPlatformCodeFromRoute } from '@/utils/markPlatformRoute'
import OrderRemarkEditor from './components/OrderRemarkEditor.vue'
import RecordBatchActions from './components/RecordBatchActions.vue'

const { proxy } = getCurrentInstance()
const route = useRoute()
const SMS_APPLY_URL = 'https://yun.m.qq.com/person_apply.html'

const activePlatformCode = computed(() => {
  return resolveMarkPlatformCodeFromRoute(route, TENCENT_STYLE_PLATFORM_CODES, TENCENT_PLATFORM_CODE)
})

const routePlatformName = computed(() => {
  return resolveTencentStylePlatformName(activePlatformCode.value)
})

const isTdSecondPlatform = computed(() => activePlatformCode.value === 'td_second')
function readMaintenanceFlag(key, fallback) {
  const configured = String(import.meta.env[key] ?? '').trim().toLowerCase()
  return configured ? ['1', 'true', 'yes', 'on'].includes(configured) : fallback
}
const line1UnderMaintenance = computed(() => isTdSecondPlatform.value && readMaintenanceFlag('VITE_TDX_LINE1_MAINTENANCE', true))
const line2UnderMaintenance = computed(() => {
  if (!isTdSecondPlatform.value) return false
  return readMaintenanceFlag('VITE_TDX_LINE2_MAINTENANCE', false)
})
const line3UnderMaintenance = computed(() => isTdSecondPlatform.value && readMaintenanceFlag('VITE_TDX_LINE3_MAINTENANCE', true))
const selectedLineUnderMaintenance = computed(() => ({
  line1: line1UnderMaintenance.value,
  line2: line2UnderMaintenance.value,
  line3: line3UnderMaintenance.value,
}[form.line] === true))
const selectedLineMaintenanceMessage = computed(() => ({
  line1: '线路1暂时维护，请选择线路2。',
  line2: '线路2暂时维护，请稍后重试。',
  line3: '线路3暂时维护，请选择线路2。',
}[form.line] || '当前线路暂时维护，请选择线路2。'))
const submitTitle = computed(() => `${platformName.value}平台清除`)

const activeTab = ref('submit')
const submitting = ref(false)
const smsSending = ref(false)
const smsCountdown = ref(0)
const remainCount = ref(0)
const platformName = ref(routePlatformName.value)
const platformEnabled = ref(true)
const submitResult = ref(null)

const form = reactive({
  phone: '',
  smsCode: '',
  line: 'line2',
  externalTaskId: '',
  externalRequestId: '',
  externalRequestPhone: ''
})
const isLine3 = computed(() => isTdSecondPlatform.value && form.line === 'line3')

const loading = ref(false)
const total = ref(0)
const orderList = ref([])
const recordSelectedRows = ref([])
const recordDateRange = ref([])
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: null,
  requestNo: null,
  keyword: null,
  phone: null,
  platformCode: activePlatformCode.value,
  orderStatus: null,
  params: {}
})

const resultRows = computed(() => {
  if (!submitResult.value) return []
  if (isTdSecondPlatform.value) {
    return [
      { label: '提交订单号', value: submitResult.value.orderNo || '-' },
      { label: '手机号', value: submitResult.value.phone || '-' },
      { label: 'TDX订单ID', value: submitResult.value.tdxId || '-' },
      { label: '申诉流水号', value: submitResult.value.orderpicinumber || '-' },
      { label: '处理结果', value: submitResult.value.message || '提交成功' }
    ]
  }
  const phoneCount = Number(submitResult.value.phoneCount || 0)
  return [
    { label: '提交订单号', value: submitResult.value.orderNo || '-' },
    {
      label: '提交数量',
      value: phoneCount > 0 ? `正常提交了${phoneCount}个手机号码` : '-'
    }
  ]
})
const platformClosedMessage = computed(() => `${platformName.value}平台未开启，请联系管理员`)

function normalizePhone(value) {
  return String(value || '').replace(/[^\d]/g, '')
}

function createExternalRequestId() {
  const suffix = typeof globalThis.crypto?.randomUUID === 'function'
    ? globalThis.crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`
  return `web-tdx-${suffix}`
}

function clearExternalLine2Request() {
  form.externalTaskId = ''
  form.externalRequestId = ''
  form.externalRequestPhone = ''
}

function handlePhoneInput(value) {
  const nextPhone = normalizePhone(value).slice(0, 11)
  if (isTdSecondPlatform.value && form.externalRequestPhone && nextPhone !== form.externalRequestPhone) {
    clearExternalLine2Request()
    form.smsCode = ''
  }
  form.phone = nextPhone
}

function handleSmsInput(value) {
  form.smsCode = normalizePhone(value).slice(0, 6)
}

function handleLineChange(value) {
  if (value !== 'line2') {
    clearExternalLine2Request()
  }
}

function isTransportRetryableError(error) {
  const code = String(error?.code || '').toUpperCase()
  const message = String(error?.message || '').toLowerCase()
  return code === 'ERR_NETWORK'
    || code === 'ECONNABORTED'
    || message === 'network error'
    || message.includes('后端接口连接异常')
    || message.includes('系统接口请求超时')
}

function resetSubmitState() {
  submitResult.value = null
}

function handleResetForm() {
  form.phone = ''
  form.smsCode = ''
  form.line = 'line2'
  clearExternalLine2Request()
  resetSubmitState()
}

function ensureSelectedLineAvailable() {
  if (!isTdSecondPlatform.value || !selectedLineUnderMaintenance.value) return true
  proxy.$modal.msgWarning(selectedLineMaintenanceMessage.value)
  return false
}

let smsCountdownTimer = null

function stopSmsCountdown() {
  if (smsCountdownTimer) {
    window.clearInterval(smsCountdownTimer)
    smsCountdownTimer = null
  }
}

function startSmsCountdown(seconds = 60) {
  stopSmsCountdown()
  const deadline = Date.now() + seconds * 1000
  smsCountdown.value = seconds
  smsCountdownTimer = window.setInterval(() => {
    smsCountdown.value = Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
    if (smsCountdown.value === 0) {
      stopSmsCountdown()
    }
  }, 250)
}

async function handleFetchSms() {
  if (!isTdSecondPlatform.value) {
    window.open(SMS_APPLY_URL, '_blank', 'noopener,noreferrer')
    return
  }
  if (!ensureSelectedLineAvailable()) return
  const phone = normalizePhone(form.phone)
  if (!/^\d{11}$/.test(phone)) {
    proxy.$modal.msgWarning('请输入11位手机号')
    return
  }
  if (!platformEnabled.value) {
    proxy.$modal.msgError(platformClosedMessage.value)
    return
  }
  if (!isLine3.value && remainCount.value < 1) {
    proxy.$modal.msgError(`当前${platformName.value}平台剩余次数不足`)
    return
  }
  if (form.line === 'line2') {
    if (form.externalTaskId) {
      clearExternalLine2Request()
    }
    if (!form.externalRequestId) {
      form.externalRequestId = createExternalRequestId()
      form.externalRequestPhone = phone
    }
  }
  const requestPhone = phone
  const requestId = form.externalRequestId
  smsSending.value = true
  try {
    const res = await sendMarkUserTdxSecondCode({
      phone,
      line: form.line,
      externalRequestId: form.externalRequestId || undefined
    })
    const data = res?.data || {}
    if (form.phone !== requestPhone || form.externalRequestId !== requestId) {
      return
    }
    if (form.line === 'line2') {
      form.externalTaskId = String(data.taskId || '').trim()
      form.externalRequestId = String(data.externalRequestId || form.externalRequestId).trim()
      form.externalRequestPhone = phone
    }
    startSmsCountdown()
    proxy.$modal.alertSuccess(data.message || res?.msg || '短信验证码已发送，请查收')
  } catch (error) {
    if (!isTransportRetryableError(error)) {
      clearExternalLine2Request()
    }
    proxy.$modal.msgError(resolveTdxSecondError(error))
  } finally {
    smsSending.value = false
  }
}

async function resolveSubmitOrderNo(payload) {
  let orderNo = String(payload?.orderNo || '').trim()
  const orderId = payload?.orderId
  if (!orderNo && orderId) {
    try {
      const detailRes = await getMarkUserOrderDetail(orderId)
      orderNo = String(detailRes?.data?.order?.orderNo || '').trim()
    } catch (error) {
      // ignore
    }
  }
  return orderNo || '-'
}

async function applySubmitResultFromPayload(payload) {
  if (!payload?.itemId) return null
  submitResult.value = {
    orderNo: await resolveSubmitOrderNo(payload),
    phoneCount: 1,
    itemId: payload.itemId
  }
  return submitResult.value
}

function buildPollResultText(result) {
  const status = String(result?.processStatus || '')
  if (status === '1') {
    return '提交成功'
  }
  if (status === '2') {
    return '提交失败，验证码错误或者失效'
  }
  return '提交成功，处理中'
}

let pollAborted = false

async function pollTencentSubmitResult(itemId) {
  const maxAttempts = 40
  if (!itemId) {
    return
  }
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    if (pollAborted) {
      return
    }
    if (attempt > 0) {
      await new Promise((resolve) => setTimeout(resolve, 30000))
    }
    try {
      const res = await getMarkUserTencentSubmitResult(itemId)
      const result = res?.data || null
      const status = String(result?.processStatus || '')
      if (status === '1' || status === '2') {
        if (submitResult.value && submitResult.value.orderNo === '-' && result?.orderId) {
          const orderNo = await resolveSubmitOrderNo(result)
          if (orderNo !== '-') {
            submitResult.value = {
              ...submitResult.value,
              orderNo
            }
          }
        }
        if (status === '1') {
          proxy.$modal.msgSuccess(buildPollResultText(result))
        }
        await loadPlatformInfo()
        if (activeTab.value === 'record') {
          await getList()
        }
        return
      }
    } catch {
      // ignore and retry
    }
  }
  proxy.$modal.msgWarning('处理中，请稍后在提交记录中查看结果')
}

async function loadPlatformInfo() {
  platformName.value = routePlatformName.value
  try {
    const res = await listMarkUserPlatformPrice()
    const list = Array.isArray(res?.data) ? res.data : []
    const item = list.find((row) => String(row.platformCode || '').toLowerCase() === activePlatformCode.value)
    const remain = Number(item?.remainCount ?? 0)
    platformEnabled.value = item ? String(item.status ?? '0') !== '1' : false
    remainCount.value = Number.isFinite(remain) ? Math.max(0, remain) : 0
  } catch {
    platformEnabled.value = false
    remainCount.value = 0
  }
}

async function handleSubmit() {
  if (!ensureSelectedLineAvailable()) return
  const phone = normalizePhone(form.phone)
  if (!/^\d{11}$/.test(phone)) {
    proxy.$modal.msgWarning('请输入11位手机号')
    return
  }
  if (!platformEnabled.value) {
    proxy.$modal.msgError(platformClosedMessage.value)
    return
  }
  if (!isLine3.value && remainCount.value < 1) {
    proxy.$modal.msgError(`当前${platformName.value}平台剩余次数不足`)
    return
  }
  if (!/^\d{6}$/.test(form.smsCode)) {
    proxy.$modal.msgWarning('请输入6位验证码后再提交')
    return
  }

  pollAborted = false
  submitting.value = true
  try {
    if (isTdSecondPlatform.value) {
      const res = await submitMarkUserTdxSecond({
        phone,
        smsCode: form.smsCode,
        line: form.line,
        externalTaskId: form.externalTaskId || undefined,
        rotate: false
      })
      const result = res?.data || null
      if (result?.orderId || result?.itemId || result?.taskId) {
        submitResult.value = {
          ...result,
          phone,
          processedTime: new Date(),
          message: result?.message || res?.msg || '提交成功'
        }
        if (String(result?.processStatus || '') === '3') {
          submitResult.value.message = '申诉处理中，稍后再后台查看结果'
        }
        form.phone = ''
        form.smsCode = ''
        clearExternalLine2Request()
        await loadPlatformInfo()
        if (activeTab.value === 'record') {
          await getList()
        }
        await ElMessageBox.alert(buildTdxSecondResultHtml(submitResult.value), '提交结果', {
          width: 'min(580px, calc(100vw - 32px))',
          customClass: 'tdx-result-message-box',
          dangerouslyUseHTMLString: true,
          confirmButtonText: '确定'
        })
      }
      return
    }
    const res = await submitMarkUserTencent({
      platformCode: activePlatformCode.value,
      phone,
      smsCode: form.smsCode
    })
    const result = res?.data || null
    if (result?.itemId) {
      await applySubmitResultFromPayload(result)
      form.phone = ''
      form.smsCode = ''
      proxy.$modal.msgSuccess('提交成功，处理中')
      await pollTencentSubmitResult(result.itemId)
    }
  } catch (error) {
    if (isTdSecondPlatform.value) {
      proxy.$modal.msgError(resolveTdxSecondError(error))
    }
  } finally {
    submitting.value = false
  }
}

function resolveTdxSecondError(error) {
  const backendMessage = String(
    error?.response?.data?.msg
      || error?.response?.data?.message
      || error?.message
      || ''
  ).trim()
  if (
    !backendMessage
    || /^系统接口\d+异常$/.test(backendMessage)
    || /^Request failed with status code \d+$/i.test(backendMessage)
  ) {
    return 'Taidixiong二次处理后台未配置或暂不可用'
  }
  return backendMessage
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function buildTdxSecondResultHtml(result) {
  const processStatus = String(result?.processStatus || '')
  const isCompleted = processStatus === '1' || String(result?.upstreamStatus || '').toLowerCase() === 'completed'
  const isFailed = processStatus === '2' || String(result?.upstreamStatus || '').toLowerCase() === 'failed'
  const status = isCompleted
    ? '标记已取消'
    : isFailed
      ? '处理失败'
      : '处理中'
  const rows = [
    ['处理结果', isCompleted
      ? '号码标记已清除成功，请耐心等待3-5个小时的同步时间，感谢您的支持！'
      : (result?.message || '提交成功')],
    ['申诉号码', result?.phone || '-'],
    ['处理状态', status],
    ['处理时间', formatDateTime(result?.processedTime)],
    ['订单编号', result?.orderNo || '-']
  ]
  const renderValue = (label, value) => {
    if (label === '处理结果' && isCompleted) {
      return '<span style="white-space:nowrap;">号码标记已清除成功，请耐心等待3-5个小时的同步时间，</span><br><span class="tdx-result-thanks">感谢您的支持！</span>'
    }
    const normalizedValue = String(value ?? '').replace(
      '号码申诉已提交，请等待3-5个工作日的审核时间',
      '号码申诉已提交，请等待1-2个小时更新结果'
    )
    return escapeHtml(normalizedValue)
  }
  return `<div style="padding:68px 8px 0;line-height:1.9;text-align:center;">
    <div class="tdx-result-icon-wrap">
      <span style="display:inline-flex;align-items:center;justify-content:center;width:54px;height:54px;border-radius:50%;background:${isFailed ? '#f56c6c' : '#16b82e'};color:#fff;font-size:${isFailed ? '34px' : '38px'};font-weight:700;line-height:1;">${isFailed ? '&#10005;' : '&#10003;'}</span>
    </div>
    <div style="width:min(520px,100%);margin:0 auto;text-align:left;">
    ${rows
    .map(([label, value]) => `<div style="display:grid;grid-template-columns:80px minmax(0,1fr);gap:8px;margin:4px 0;align-items:start;"><strong style="color:#303133;white-space:nowrap;text-align:right;">${escapeHtml(label)}：</strong><span style="color:#606266;word-break:break-word;">${renderValue(label, value)}</span></div>`)
    .join('')}</div></div>`
}

async function handleViewRecordResult(row) {
  const orderId = row?.id
  if (!orderId) {
    proxy.$modal.msgWarning('订单不存在')
    return
  }
  let item = null
  try {
    const res = await getMarkUserOrderDetail(orderId)
    const items = Array.isArray(res?.data?.items) ? res.data.items : []
    item = items.find((entry) => String(entry?.id) === String(row?.itemId)) || items[0] || null
  } catch (error) {
    proxy.$modal.msgError('申诉结果读取失败，请稍后重试')
    return
  }
  if (!item) {
    proxy.$modal.msgWarning('暂未找到该号码的申诉结果')
    return
  }
  await ElMessageBox.alert(buildTdxSecondResultHtml({
    phone: item.phone || row.phonePreview,
    processStatus: item.processStatus,
    message: item.processResult || item.processNote || '暂无处理结果',
    processedTime: item.processedTime || row.completedTime || row.createTime,
    orderNo: row.orderNo
  }), '申诉结果', {
    width: 'min(580px, calc(100vw - 32px))',
    customClass: 'tdx-result-message-box',
    dangerouslyUseHTMLString: true,
    confirmButtonText: '确定'
  })
}

async function copyText(text) {
  const value = String(text || '').trim()
  if (!value) {
    proxy.$modal.msgWarning('没有可复制内容')
    return
  }
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(value)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = value
      textarea.style.position = 'fixed'
      textarea.style.left = '-9999px'
      document.body.appendChild(textarea)
      textarea.focus()
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    proxy.$modal.msgSuccess('复制成功')
  } catch (error) {
    proxy.$modal.msgError('复制失败，请检查浏览器权限')
  }
}

function buildCsvCell(value) {
  const text = String(value ?? '')
  return `"${text.replace(/"/g, '""')}"`
}

function downloadCsv(filename, rows) {
  const csvText = `\ufeff${rows.map((row) => row.map((cell) => buildCsvCell(cell)).join(',')).join('\n')}`
  const blob = new Blob([csvText], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

function isAutoProcessingRecord(row) {
  const code = String(row?.platformCode || '').trim().toLowerCase()
  return ['tengxun', 'tencent', 'tx', 'txwz'].includes(code)
}

function isManualPendingStatus3Record(row) {
  const code = String(row?.platformCode || '').trim().toLowerCase()
  return ['td_gaopin'].includes(code)
}

function recordStatusLabel(row) {
  const auditStatus = String(row?.auditStatus ?? '1')
  if (auditStatus === '0') return '待审核'
  if (auditStatus === '2') return '已拒绝'
  if (auditStatus === '3') return '已打回'
  const itemStatus = String(row?.itemProcessStatus ?? '')
  if (itemStatus === '3') return '处理中'
  if (itemStatus === '0') return isAutoProcessingRecord(row) ? '处理中' : '待处理'
  if (itemStatus === '1') return '处理完成'
  if (itemStatus === '2') return '处理失败'
  const status = String(row?.orderStatus ?? '')
  const successCount = Number(row?.successCount ?? 0)
  const failedCount = Number(row?.failedCount ?? 0)
  if (status === '0') return '待处理'
  if (status === '1') return '处理中'
  if (status === '2') return failedCount > 0 && successCount <= 0 ? '处理失败' : (failedCount > 0 ? '处理失败' : '处理完成')
  if (status === '3') return '处理失败'
  return '待处理'
}

function recordStatusType(row) {
  const auditStatus = String(row?.auditStatus ?? '1')
  if (auditStatus === '2') return 'danger'
  if (auditStatus === '3') return 'warning'
  if (auditStatus === '0') return 'info'
  const label = recordStatusLabel(row)
  if (label === '处理完成') return 'success'
  if (label === '处理失败') return 'danger'
  if (label === '处理中') return 'warning'
  return 'info'
}

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function recordRowKey(row) {
  return `${row?.itemId ?? row?.id ?? ''}-${row?.phonePreview ?? ''}`
}

function normalizeRecordKeyword() {
  const keyword = String(queryParams.keyword || '').trim()
  queryParams.keyword = keyword || null
  if (keyword && /^\d{7,15}$/.test(keyword)) {
    queryParams.phone = keyword
    return
  }
  queryParams.phone = null
}

function getList() {
  queryParams.platformCode = activePlatformCode.value
  normalizeRecordKeyword()
  loading.value = true
  return listMarkUserOrder(queryParams).then((res) => {
    orderList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.orderNo = null
  queryParams.requestNo = null
  queryParams.keyword = null
  queryParams.phone = null
  queryParams.orderStatus = null
  queryParams.params = {}
  recordDateRange.value = []
  recordSelectedRows.value = []
  queryParams.platformCode = activePlatformCode.value
  handleQuery()
}

function handleRecordDateRangeChange(value) {
  queryParams.pageNum = 1
  if (!Array.isArray(value) || value.length !== 2 || !value[0] || !value[1]) {
    recordDateRange.value = []
    queryParams.params = {}
    getList()
    return
  }
  queryParams.params = {
    beginTime: value[0],
    endTime: value[1]
  }
  getList()
}

function handleRecordSelectionChange(rows) {
  recordSelectedRows.value = Array.isArray(rows) ? rows : []
}

function exportRecordRows() {
  const rows = recordSelectedRows.value.length > 0 ? recordSelectedRows.value : orderList.value
  if (!rows.length) {
    proxy.$modal.msgWarning('暂无可导出记录')
    return
  }
  const includeRemark = !isTdSecondPlatform.value
  const header = ['用户名', '手机号', '平台', '处理状态', ...(includeRemark ? ['备注'] : []), '提交时间', '订单号']
  const body = rows.map((item) => [
    item.userName || '',
    item.phonePreview || '',
    platformName.value || item.platformName || '',
    recordStatusLabel(item),
    ...(includeRemark ? [item.userRemark || ''] : []),
    formatDateTime(item.createTime),
    item.orderNo || ''
  ])
  downloadCsv(`${activePlatformCode.value || 'tencent'}-record-${Date.now()}.csv`, [header, ...body])
}

let recordPollTimer = null
function startRecordPolling() {
  // User task records should not auto-poll; refresh only on search/reset/page change.
  stopRecordPolling()
}

function stopRecordPolling() {
  if (recordPollTimer) {
    window.clearInterval(recordPollTimer)
    recordPollTimer = null
  }
}

function syncRecordTabData() {
  if (activeTab.value === 'record') {
    getList()
  } else {
    stopRecordPolling()
  }
}

watch(activeTab, () => {
  syncRecordTabData()
})

watch([line1UnderMaintenance, line2UnderMaintenance, line3UnderMaintenance], () => {
  if (isTdSecondPlatform.value && selectedLineUnderMaintenance.value) form.line = 'line2'
}, { immediate: true })

watch([activePlatformCode, routePlatformName], async ([newCode], [oldCode]) => {
  platformName.value = routePlatformName.value
  queryParams.platformCode = newCode
  submitResult.value = null
  if (newCode !== oldCode) {
    queryParams.pageNum = 1
    await loadPlatformInfo()
    if (activeTab.value === 'record') {
      await getList()
    }
  }
})

onMounted(() => {
  loadPlatformInfo()
  syncRecordTabData()
})

onBeforeUnmount(() => {
  pollAborted = true
  stopSmsCountdown()
  stopRecordPolling()
})
</script>

<style scoped lang="scss">
.tencent-mark-page {
  padding: 0 !important;
  background: #f5f7fa;
}

.page-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background: #fff;
}

:global(.tdx-result-message-box) {
  position: relative;
  --el-messagebox-width: min(580px, calc(100vw - 32px));
  width: min(580px, calc(100vw - 32px)) !important;
  max-width: min(580px, calc(100vw - 32px)) !important;
  height: min(400px, calc(100vh - 32px));
}

:global(.tdx-result-message-box .el-message-box__content) {
  padding-right: 24px;
  padding-left: 24px;
}

:global(.tdx-result-message-box .tdx-result-icon-wrap) {
  position: absolute;
  top: 64px;
  left: 50%;
  width: 54px;
  height: 54px;
  margin: 0;
  transform: translateX(-50%);
}

.platform-main {
  position: relative;
}

.platform-main-remain {
  position: absolute;
  top: 0;
  right: 16px;
  z-index: 2;
  display: flex;
  align-items: center;
  height: 40px;
}

.sub-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 16px;
  background: #fff;
}

.sub-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--el-border-color-light);
}

.sub-tabs :deep(.el-tabs__item) {
  height: 42px;
  line-height: 42px;
  padding: 0 18px;
  font-size: 14px;
}

.sub-tabs :deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
  font-weight: 600;
}

.sub-tabs :deep(.el-tabs__content) {
  padding: 0;
}

.platform-main:has(.platform-main-remain) .sub-tabs :deep(.el-tabs__header) {
  padding-right: 160px;
}

.submit-right-remain {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
  background: #ecf5ff;
  color: #606266;
  font-size: 13px;
  line-height: 1;
}

.submit-right-remain span {
  color: #409eff;
  font-weight: 600;
}

.submit-pane,
.record-pane {
  padding: 14px 16px 18px;
  background: #fff;
}

.tencent-content-shell {
  width: 66%;
  max-width: 660px;
  min-width: 420px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.tencent-submit-section {
  width: 100%;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
}

.tencent-submit-head {
  margin-bottom: 12px;
}

.tencent-form-panel {
  margin-top: 2px;
  padding: 12px 14px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #fafbfc;
  box-sizing: border-box;
}

.tencent-submit-title {
  margin-bottom: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
  text-align: center;
}

.tencent-submit-tip {
  margin: 0 0 10px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  text-align: center;
}

.tencent-guide-box {
  margin-bottom: 10px;
  padding: 8px 10px;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  background: #ecf5ff;
  text-align: center;
}

.tencent-guide-box__title {
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  text-align: center;
}

.tencent-guide-box__list {
  margin: 0;
  padding-left: 0;
  list-style-position: inside;
  color: #606266;
  font-size: 12px;
  line-height: 1.7;
  text-align: center;
}

.tencent-guide-box__list li {
  margin-bottom: 2px;
}

.tencent-submit-form {
  width: 100%;
  margin-bottom: 10px;

  :deep(.el-form-item) {
    width: 100%;
    margin-right: 0;
    margin-bottom: 10px;
  }

  :deep(.el-form-item:last-child) {
    margin-bottom: 0;
  }

  :deep(.el-form-item__content) {
    flex: 1;
  }

  :deep(.el-input) {
    width: 100%;
  }

  :deep(.el-form-item__label) {
    font-size: 13px;
    color: #606266;
    padding-right: 10px;
  }

  :deep(.el-input__wrapper) {
    font-size: 13px;
  }
}

:global(.tdx-result-message-box .tdx-result-thanks) {
  display: block;
  padding-left: 7em;
  box-sizing: border-box;
  white-space: nowrap;
  text-align: left;
}

.tencent-submit-form--stacked {
  width: min(100%, 380px);
  margin-right: auto;
  margin-left: auto;

  :deep(.el-form-item__label) {
    height: auto;
    padding: 0 0 6px;
    color: #303133;
    font-weight: 600;
    line-height: 20px;
  }

}

.submit-warning-bar {
  width: 100%;
  margin-bottom: 10px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 13px;
  line-height: 1.5;
}

.submit-warning-bar--maintenance {
  border: 1px solid #f3d19e;
  background: #fdf6ec;
  color: #e6a23c;
}

.tdx-line-maintenance-label {
  margin-left: 6px;
  color: #e6a23c;
  font-size: 12px;
}

.tdx-line-reuse-label {
  margin-left: 6px;
  color: #67c23a;
  font-size: 12px;
}


.tencent-submit-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-left: 0;
}

.tencent-submit-buttons--stacked {
  width: min(100%, 380px);
  margin-right: auto;
  margin-left: auto;
  padding-left: 0;
}

.tencent-action-btn {
  min-width: 88px;
  height: 32px;
  padding: 0 14px;
  margin: 0;
  font-size: 13px;
}

.tencent-result-block {
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-top: 12px;
  padding: 12px 14px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #fff;
  box-sizing: border-box;
  min-height: 188px;
}

.tencent-result-head {
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.tencent-result-table {
  width: 100%;
  flex: 1;

  :deep(.el-table__header th) {
    background: #eef3f8;
    color: #303133;
    font-weight: 600;
    font-size: 13px;
    padding: 6px 0;
  }

  :deep(.el-table__body td) {
    font-size: 13px;
    padding: 6px 0;
  }

  :deep(.el-table__empty-block) {
    min-height: 148px;
  }

  :deep(.el-table__empty-text) {
    line-height: 148px;
  }
}

.tencent-result-empty {
  color: #909399;
  font-size: 13px;
}

.tencent-result-time {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.record-form {
  margin-bottom: 12px;
  padding: 14px 16px 6px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: #fafafa;
}

.record-form :deep(.el-form-item) {
  margin-bottom: 10px;
  margin-right: 12px;
}

.record-form__actions :deep(.el-button + .el-button) {
  margin-left: 8px;
}

.record-table {
  width: 100%;
}

.record-pane :deep(.pagination-container) {
  margin-top: 12px;
  padding: 0;
}

.td-second-mark-page {
  .sub-tabs :deep(.el-tabs__header) {
    padding: 0 40px;
  }

  .submit-pane {
    padding: 20px 40px 28px;
  }

  .tencent-content-shell {
    width: 600px;
    max-width: 600px;
    min-width: 600px;
  }

  .tencent-form-panel {
    min-height: 380px;
  }

  .tencent-submit-form :deep(.tdx-phone-item) {
    margin-bottom: 16px;
  }

  .tencent-result-block {
    margin-top: 18px;
    min-height: 358px;
  }
}

@media (max-width: 768px) {
  :global(.tdx-result-message-box) {
    height: auto;
  }

  :global(.tdx-result-message-box .el-message-box__content) {
    padding-right: 16px;
    padding-left: 16px;
  }

  :global(.tdx-result-message-box .tdx-result-thanks) {
    padding-left: 0;
    text-align: center;
  }

  .tencent-content-shell {
    width: 100%;
    max-width: none;
    min-width: 0;
  }

  .tencent-submit-buttons {
    padding-left: 0;
  }

  .td-second-mark-page {
    .sub-tabs :deep(.el-tabs__header) {
      padding: 0 12px;
    }

    .submit-pane {
      padding: 14px 12px 18px;
    }

    .tencent-content-shell {
      width: 100%;
      max-width: none;
      min-width: 0;
    }

    .tencent-form-panel {
      min-height: 0;
    }

    .tencent-submit-buttons {
      padding-left: 0;
    }
  }

  .platform-main:has(.platform-main-remain) .sub-tabs :deep(.el-tabs__header) {
    padding-right: 16px;
  }

  .platform-main-remain {
    position: static;
    justify-content: flex-end;
    height: auto;
    padding: 8px 16px 0;
  }

  .submit-pane,
  .record-pane {
    padding: 12px;
  }
}
</style>
