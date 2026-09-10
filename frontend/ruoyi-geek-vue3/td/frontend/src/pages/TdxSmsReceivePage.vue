<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import SectionHeader from '../components/SectionHeader.vue'
import StatePanel from '../components/StatePanel.vue'
import { getSettings, getTdxLine3Candidates, getTdxSettings, loginTdxSmsReceive, precheckTdxPhone, retryTdxTaskLine3, sendTdxSmsCode, sendTdxTaskSmsCode } from '../api/client'
import { useAsyncData } from '../composables/useAsyncData'
import {
  CLEARED_RECEIPT_TEXT,
  CLEARED_RECEIPT_TITLE,
  isProcessingReceipt,
  normalizeReceiptDisplayMessage,
  normalizeReceiptDisplayStatus,
} from '../utils/receipt'

const { data: systemSettings } = useAsyncData(getSettings, { base: [], switches: [] })
const { data: tdxSettings } = useAsyncData(getTdxSettings, {})
const route = useRoute()
const sourceTaskId = computed(() => String(route.query.taskId || route.query.id || '').trim())
const line3Candidates = ref([])
const selectedLine3TaskId = ref(sourceTaskId.value)

const form = reactive({
  line: String(route.query.line || 'line1'),
  phone: String(route.query.phone || ''),
  smsCode: String(route.query.smsCode || route.query.code || ''),
})

const actionState = reactive({
  checking: false,
  sendingCode: false,
  submitting: false,
  message: '',
  error: '',
})

const REPEAT_APPEAL_MESSAGE = '该号码30天内已申诉过，暂不可重复提交'

const successDialog = reactive({
  visible: false,
  phone: '',
  appealStatus: '已消除',
  resultMessage: '',
  appealDate: '',
  submitTime: '',
})

const repeatAppealDialog = reactive({
  visible: false,
  phone: '',
  message: REPEAT_APPEAL_MESSAGE,
})

const receiptIsProcessing = computed(() => isProcessingReceipt(successDialog.appealStatus, successDialog.resultMessage))
const receiptTagType = computed(() => (receiptIsProcessing.value ? 'warning' : 'success'))
const receiptAlertType = computed(() => (receiptIsProcessing.value ? 'warning' : 'success'))
const receiptIcon = computed(() => (receiptIsProcessing.value ? '…' : '✓'))
const receiptTitle = computed(() => (receiptIsProcessing.value ? '申诉已提交' : CLEARED_RECEIPT_TITLE))
const receiptAlertTitle = computed(() => (receiptIsProcessing.value ? '申诉已提交，正在处理' : CLEARED_RECEIPT_TEXT))
const receiptAlertDescription = computed(() => (
  receiptIsProcessing.value
    ? '系统已接收本次申诉，处理完成后结果会继续同步。'
    : ''
))

const tdxMarkEnabled = computed(() => (systemSettings.value?.switches || []).find((item) => item.key === 'tdxMarkEnabled')?.enabled !== false)
const line2UnderMaintenance = computed(() => Boolean(tdxSettings.value?.line2Maintenance))
const isLine3 = computed(() => String(form.line || '').trim().toLowerCase() === 'line3')
const LINE2_MAINTENANCE_MESSAGE = '线路2暂时维护，请选择线路1。'

watch(line2UnderMaintenance, (underMaintenance) => {
  if (underMaintenance && form.line === 'line2') form.line = 'line1'
}, { immediate: true })

function pad(value) {
  return String(value).padStart(2, '0')
}

function formatShanghaiDateTime(value = new Date()) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const formatter = new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
  const parts = Object.fromEntries(formatter.formatToParts(date).map((part) => [part.type, part.value]))
  return `${parts.year || date.getFullYear()}-${parts.month || pad(date.getMonth() + 1)}-${parts.day || pad(date.getDate())} ${parts.hour || pad(date.getHours())}:${parts.minute || pad(date.getMinutes())}:${parts.second || pad(date.getSeconds())}`
}

function normalizeDateTimeText(value, fallback = '') {
  const text = String(value || '').trim()
  if (!text) return String(fallback || '').trim()
  if (/^\d{4}-\d{2}-\d{2}( \d{2}:\d{2}:\d{2})?$/.test(text)) return text
  const formatted = formatShanghaiDateTime(text)
  return formatted || text
}

function normalizeDateText(value, fallback = '') {
  const normalized = normalizeDateTimeText(value, fallback)
  return normalized ? normalized.slice(0, 10) : ''
}

function normalizePublicAppealMessage(value = '', fallback = '号码申诉已提交，请等待3-5个工作日的审核时间') {
  const text = String(value || '').trim()
  if (!text) return fallback
  if (/(?:授权会话|wxCode|wxcode|openId|openid|token|session|上游|QMF|orderId|upstream|progress\s*record|final\s*progress|cleanup\s*task|submitted\s+successfully|waiting\s+for.*processing|returned\s+yet|retumed\s+yet)/i.test(text)) {
    return fallback
  }
  return text
}

function isRepeatAppealMessage(value = '') {
  const text = String(value || '').trim()
  if (!text) return false
  return /(?:30\s*天|三十天|30日|近30天|一个月内|月内).*(?:申诉|提交)|(?:申诉|提交).*(?:30\s*天|三十天|30日|近30天|一个月内|月内)|(?:已申诉过|已提交过).*(?:30\s*天|三十天|30日|近30天|一个月内|月内)/i.test(text)
}

function normalizePublicInlineMessage(value = '', fallback = '') {
  const text = String(value || '').trim()
  const fallbackText = String(fallback || '').trim()
  if (!text) return fallbackText
  if (isRepeatAppealMessage(text)) return REPEAT_APPEAL_MESSAGE
  if (/(?:授权会话|wxCode|wxcode|openId|openid|token|session|上游|QMF|orderId|upstream|progress\s*record|final\s*progress|cleanup\s*task|save-login-token|login-sms|submitted\s+successfully|waiting\s+for.*processing|returned\s+yet|retumed\s+yet)/i.test(text)) {
    return fallbackText || '系统正在处理，请稍后查看结果。'
  }
  if (/会话已保存|继续申诉处理|Session ready/i.test(text)) {
    return fallbackText || '申诉请求已提交，请等待处理结果。'
  }
  return text
}

function resetMessages() {
  actionState.message = ''
  actionState.error = ''
}

function hideSuccessDialog() {
  successDialog.visible = false
}

function hideRepeatAppealDialog() {
  repeatAppealDialog.visible = false
}

function openRepeatAppealDialog(phone = '', message = REPEAT_APPEAL_MESSAGE) {
  repeatAppealDialog.phone = String(phone || form.phone || '').trim()
  repeatAppealDialog.message = normalizePublicInlineMessage(message, REPEAT_APPEAL_MESSAGE) || REPEAT_APPEAL_MESSAGE
  repeatAppealDialog.visible = true
}

function resetFeedback() {
  resetMessages()
  hideSuccessDialog()
  hideRepeatAppealDialog()
}

function validatePhone(phone) {
  return /^1\d{10}$/.test(String(phone || '').trim())
}

function ensureSelectedLineAvailable() {
  if (isLine3.value) return true
  if (!(line2UnderMaintenance.value && form.line === 'line2')) return true
  form.line = 'line1'
  actionState.error = LINE2_MAINTENANCE_MESSAGE
  return false
}

async function loadLine3Candidates({ silent = false } = {}) {
  if (!validatePhone(form.phone)) {
    line3Candidates.value = []
    selectedLine3TaskId.value = sourceTaskId.value || ''
    return []
  }
  try {
    const result = await getTdxLine3Candidates(form.phone)
    const candidates = Array.isArray(result?.candidates) ? result.candidates : []
    line3Candidates.value = candidates
    if (candidates.length === 1) selectedLine3TaskId.value = candidates[0].taskId
    else if (!candidates.some((item) => item.taskId === selectedLine3TaskId.value)) selectedLine3TaskId.value = ''
    return candidates
  } catch (err) {
    line3Candidates.value = Array.isArray(err?.data?.candidates) ? err.data.candidates : []
    if (!silent) actionState.error = normalizePublicInlineMessage(err instanceof Error ? err.message : '', '没有找到可复用的线路1失败订单')
    return line3Candidates.value
  }
}

watch(() => [form.line, form.phone], async ([line]) => {
  if (line === 'line3' && validatePhone(form.phone)) await loadLine3Candidates({ silent: true })
}, { flush: 'post' })

function applySuccessDialog(detail = {}) {
  const submitTime = normalizeDateTimeText(detail.submitTime, formatShanghaiDateTime())
  const detailStatus = String(detail.appealStatus || '').trim()
  const detailMessage = String(detail.resultMessage || '').trim()
  successDialog.phone = String(detail.phone || form.phone || '').trim()
  successDialog.appealStatus = normalizeReceiptDisplayStatus(detailStatus, detailMessage, '处理中')
  successDialog.resultMessage = normalizeReceiptDisplayMessage(
    successDialog.appealStatus,
    detailMessage,
    successDialog.appealStatus === '处理中'
      ? '系统已接收本次申诉，处理完成后结果会继续同步。'
      : CLEARED_RECEIPT_TEXT,
  )
  successDialog.appealDate = normalizeDateText(detail.appealDate, submitTime)
  successDialog.submitTime = submitTime
  successDialog.visible = true
}

function resolveAppealReceiptStatus(result = {}) {
  const status = String(
    result?.autoCleanupStatus
      || result?.status
      || result?.autoCleanupExecution?.result?.status
      || result?.autoCleanupRawStatus
      || '',
  ).toLowerCase()
  if (status === 'processing' || status === 'pending' || status.includes('retry')) return '处理中'
  return '已消除'
}

function buildFallbackSuccessDialog(result = {}) {
  const submitTime = formatShanghaiDateTime()
  const appealStatus = resolveAppealReceiptStatus(result)
  const publicMessage = normalizePublicAppealMessage(
    result?.autoCleanupMessage || result?.autoCleanupExecution?.result?.message,
    appealStatus === '处理中'
      ? '系统已接收本次申诉，处理完成后结果会继续同步。'
      : CLEARED_RECEIPT_TEXT,
  )
  return {
    phone: form.phone,
    appealStatus,
    resultMessage: normalizeReceiptDisplayMessage(appealStatus, publicMessage, publicMessage),
    appealDate: submitTime.slice(0, 10),
    submitTime,
  }
}

async function handleSendCode() {
  resetFeedback()
  if (!ensureSelectedLineAvailable()) return
  if (!validatePhone(form.phone)) {
    actionState.error = '请输入正确的手机号后再发送验证码'
    return
  }

  if (isLine3.value) {
    const candidates = await loadLine3Candidates()
    if (!selectedLine3TaskId.value) {
      actionState.error = candidates.length > 1 ? '请选择要复用的本地订单批次' : '当前账号下没有可复用的线路1失败订单'
      return
    }
    actionState.sendingCode = true
    try {
      await sendTdxSmsCode({ phone: form.phone, line: 'line3', sourceTaskId: selectedLine3TaskId.value })
      actionState.message = '验证码已发送，请收到后在线路3中提交同一个新验证码。'
    } catch (err) {
      actionState.error = normalizePublicInlineMessage(err instanceof Error ? err.message : '', '发送验证码失败，请稍后重试')
    } finally {
      actionState.sendingCode = false
    }
    return
  }

  actionState.checking = true
  try {
    const check = await precheckTdxPhone({ phone: form.phone, line: form.line })
    if (!check.allowed) {
      const blockedMessage = normalizePublicInlineMessage(check.message || check.status, '号码暂不符合申诉条件')
      if (isRepeatAppealMessage(blockedMessage) || isRepeatAppealMessage(check.status)) {
        openRepeatAppealDialog(form.phone, blockedMessage)
        return
      }
      actionState.error = blockedMessage || '号码预检查未通过'
      return
    }
    actionState.message = ''
  } catch (err) {
    const message = err instanceof Error ? err.message : '号码预检查失败'
    if (isRepeatAppealMessage(message)) {
      openRepeatAppealDialog(form.phone, message)
      return
    }
    actionState.error = normalizePublicInlineMessage(message, '号码预检查失败，请稍后重试')
    return
  } finally {
    actionState.checking = false
  }

  actionState.sendingCode = true
  actionState.message = ''
  try {
    await sendTdxSmsCode({ phone: form.phone, line: form.line })
    actionState.message = '验证码已发送，请注意查收短信。'
  } catch (err) {
    const message = err instanceof Error ? err.message : '发送验证码失败'
    actionState.error = normalizePublicInlineMessage(message, '发送验证码失败，请稍后重试')
  } finally {
    actionState.sendingCode = false
  }
}

async function handleSubmitAppeal() {
  resetFeedback()
  if (!ensureSelectedLineAvailable()) return
  if (!validatePhone(form.phone)) {
    actionState.error = '请输入正确的手机号'
    return
  }
  if (!form.smsCode || String(form.smsCode).trim().length < 4) {
    actionState.error = '请输入短信验证码'
    return
  }
  if (isLine3.value) {
    const candidates = await loadLine3Candidates()
    if (!selectedLine3TaskId.value) {
      actionState.error = candidates.length > 1 ? '请选择要复用的本地订单批次' : '当前账号下没有可复用的线路1失败订单'
      return
    }
  }

  actionState.submitting = true
  try {
    const result = isLine3.value
      ? await retryTdxTaskLine3({ id: selectedLine3TaskId.value, smsCode: form.smsCode })
      : await loginTdxSmsReceive({
          phone: form.phone,
          smsCode: form.smsCode,
          line: form.line,
          taskId: sourceTaskId.value,
          rotate: false,
        })

    if (result?.successDialog || result?.autoCleanupCompleted || result?.autoCleanupStarted) {
      applySuccessDialog(result?.successDialog || buildFallbackSuccessDialog(result))
      actionState.message = ''
      return
    }

    actionState.message = isLine3.value
      ? (result?.task?.currentStep || '线路3已复用原订单和原微信提交')
      : '申诉请求已提交，请等待处理结果。'
  } catch (err) {
    const message = err instanceof Error ? err.message : '提交失败，请稍后重试。'
    if (isRepeatAppealMessage(message)) {
      openRepeatAppealDialog(form.phone, message)
      return
    }
    actionState.error = normalizePublicInlineMessage(message, '提交失败，请稍后重试。')
  } finally {
    actionState.submitting = false
  }
}

function clearForm() {
  form.phone = ''
  form.smsCode = ''
  line3Candidates.value = []
  selectedLine3TaskId.value = ''
  resetFeedback()
}
</script>

<template>
  <AppShell active="tdx" title="tdx二次申诉" subtitle="">
    <StatePanel
      v-if="!tdxMarkEnabled"
      variant="error"
      title="模块已关闭"
      description="当前系统设置里已关闭泰迪熊标记模块，请先开启后再使用。"
    />

    <section class="layout-main-wide">
      <section class="card panel old-form-panel appeal-panel">
        <SectionHeader title="tdx二次申诉" subtitle="" />

        <el-form class="appeal-form" label-position="top">
          <el-row :gutter="18" class="appeal-form-stack">
            <el-col :span="24">
              <el-form-item label="线路">
                <div class="tdx-line-choice" role="radiogroup" aria-label="线路选择">
                  <button
                    type="button"
                    class="tdx-line-option"
                    :class="{ 'is-active': form.line === 'line1' }"
                    :aria-checked="form.line === 'line1'"
                    role="radio"
                    @click="form.line = 'line1'"
                  >
                    <span class="tdx-line-option__radio" aria-hidden="true"></span>
                    <span class="tdx-line-option__label">线路1</span>
                    <span class="tdx-line-option__badge">推荐</span>
                  </button>
                  <button
                    type="button"
                    class="tdx-line-option"
                    :class="{ 'is-active': form.line === 'line2', 'is-disabled': line2UnderMaintenance }"
                    :aria-checked="form.line === 'line2'"
                    :aria-disabled="line2UnderMaintenance"
                    :disabled="line2UnderMaintenance"
                    role="radio"
                    @click="form.line = 'line2'"
                  >
                    <span class="tdx-line-option__radio" aria-hidden="true"></span>
                    <span class="tdx-line-option__label">线路2</span>
                    <span v-if="line2UnderMaintenance" class="tdx-line-option__badge tdx-line-option__badge--maintenance">暂时维护</span>
                  </button>
                  <button
                    type="button"
                    class="tdx-line-option"
                    :class="{ 'is-active': form.line === 'line3' }"
                    :aria-checked="form.line === 'line3'"
                    role="radio"
                    @click="form.line = 'line3'"
                  >
                    <span class="tdx-line-option__radio" aria-hidden="true"></span>
                    <span class="tdx-line-option__label">线路3</span>
                    <span class="tdx-line-option__badge">失败复用</span>
                  </button>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="手机号">
                <el-input
                  v-model="form.phone"
                  size="large"
                  maxlength="11"
                  clearable
                  placeholder="请输入 11 位手机号"
                />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="短信验证码">
                <el-input
                  v-model="form.smsCode"
                  size="large"
                  clearable
                  placeholder="请输入短信验证码"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div v-if="isLine3 && line3Candidates.length > 1" class="line3-candidate-picker">
          <el-form-item label="可复用失败订单批次">
            <el-select v-model="selectedLine3TaskId" size="large" placeholder="请选择本地订单批次" style="width: 100%">
              <el-option
                v-for="candidate in line3Candidates"
                :key="candidate.taskId"
                :label="`${candidate.localOrderNumber || candidate.taskId} / ${candidate.failedAt || candidate.createdAt || ''} / ${candidate.status || ''}`"
                :value="candidate.taskId"
              />
            </el-select>
          </el-form-item>
        </div>

        <div class="appeal-actions">
          <el-button
            plain
            size="large"
            @click="handleSendCode"
            :loading="actionState.checking || actionState.sendingCode"
            :disabled="actionState.submitting || !tdxMarkEnabled"
          >
            {{ actionState.checking ? '检查中...' : (actionState.sendingCode ? '发送中...' : '获取短信验证码') }}
          </el-button>
          <el-button
            type="primary"
            size="large"
            @click="handleSubmitAppeal"
            :loading="actionState.submitting"
            :disabled="actionState.checking || actionState.sendingCode || !tdxMarkEnabled"
          >
            {{ actionState.submitting ? '提交中...' : '提交申诉' }}
          </el-button>
          <el-button size="large" @click="clearForm">清空</el-button>
        </div>

        <div class="appeal-feedback">
          <el-alert
            v-if="actionState.message"
            type="success"
            show-icon
            :closable="false"
            title="操作成功"
            :description="actionState.message"
          />
          <el-alert
            v-if="actionState.error"
            type="error"
            show-icon
            :closable="false"
            title="操作失败"
            :description="actionState.error"
          />
        </div>
      </section>
    </section>

    <el-dialog
      v-model="successDialog.visible"
      width="680px"
      align-center
      destroy-on-close
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      class="appeal-success-dialog"
    >
      <template #header>
        <div class="appeal-success-header">
          <div class="appeal-success-check" :class="{ 'is-processing': receiptIsProcessing }">{{ receiptIcon }}</div>
          <div>
            <p class="appeal-success-kicker" :class="{ 'is-processing': receiptIsProcessing }">处理回执</p>
            <h3>{{ receiptTitle }}</h3>
          </div>
        </div>
      </template>

      <div class="appeal-success-body">
        <el-alert
          :type="receiptAlertType"
          :closable="false"
          show-icon
          :title="receiptAlertTitle"
          :description="receiptAlertDescription"
        />

        <el-descriptions :column="1" border size="large" class="appeal-success-descriptions">
          <el-descriptions-item label="手机号">
            <span class="appeal-success-value">{{ successDialog.phone }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="申诉状态">
            <el-tag :type="receiptTagType" effect="dark" round>{{ successDialog.appealStatus }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="处理结果">
            <div class="appeal-success-result">{{ successDialog.resultMessage }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="申诉时间">
            <span class="appeal-success-value">{{ successDialog.appealDate }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            <span class="appeal-success-value">{{ successDialog.submitTime }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <div class="appeal-success-footer">
          <el-button type="primary" size="large" @click="hideSuccessDialog">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="repeatAppealDialog.visible"
      width="560px"
      align-center
      destroy-on-close
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      class="repeat-appeal-dialog"
    >
      <template #header>
        <div class="repeat-appeal-header">
          <div class="repeat-appeal-badge">!</div>
          <div>
            <p class="repeat-appeal-kicker">申诉限制</p>
            <h3>暂不可重复申诉</h3>
          </div>
        </div>
      </template>

      <div class="repeat-appeal-body">
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="该号码近期已提交过申诉"
          description="当前处于 30 天限制期内，请稍后再试。"
        />

        <el-descriptions :column="1" border size="large" class="repeat-appeal-descriptions">
          <el-descriptions-item label="手机号">
            <span class="repeat-appeal-value">{{ repeatAppealDialog.phone }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="提示">
            <div class="repeat-appeal-message">{{ repeatAppealDialog.message }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <div class="repeat-appeal-footer">
          <el-button type="primary" size="large" @click="hideRepeatAppealDialog">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </AppShell>
</template>

<style scoped>
.appeal-panel {
  overflow: hidden;
}

.appeal-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #0f172a;
}

.appeal-form-stack {
  max-width: 560px;
}

.tdx-line-choice {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.tdx-line-option {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 128px;
  padding: 10px 14px;
  border: 1px solid #d7e0eb;
  border-radius: 8px;
  background: #ffffff;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tdx-line-option:hover {
  border-color: #93c5fd;
  box-shadow: 0 8px 20px rgba(148, 163, 184, 0.14);
}

.tdx-line-option.is-disabled,
.tdx-line-option:disabled {
  border-color: #e2e8f0;
  background: #f8fafc;
  color: #94a3b8;
  cursor: not-allowed;
  box-shadow: none;
}

.tdx-line-option.is-disabled:hover,
.tdx-line-option:disabled:hover {
  border-color: #e2e8f0;
  box-shadow: none;
}

.tdx-line-option:focus-visible {
  outline: 3px solid rgba(59, 130, 246, 0.22);
  outline-offset: 2px;
}

.tdx-line-option.is-active {
  border-color: #409eff;
  background: #f5f9ff;
  color: #2563eb;
}

.tdx-line-option__radio {
  position: relative;
  width: 14px;
  height: 14px;
  border: 1.5px solid #94a3b8;
  border-radius: 50%;
  flex: 0 0 auto;
}

.tdx-line-option.is-active .tdx-line-option__radio {
  border-color: #409eff;
}

.tdx-line-option.is-active .tdx-line-option__radio::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #409eff;
  transform: translate(-50%, -50%);
}

.tdx-line-option__label {
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
}

.tdx-line-option__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 6px;
  border-radius: 999px;
  background: #e8fff2;
  color: #16a34a;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}

.tdx-line-option__badge--maintenance {
  background: #fff7ed;
  color: #c2410c;
}

.appeal-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 4px;
}

.line3-candidate-picker {
  max-width: 560px;
  margin-top: 4px;
}

.appeal-feedback {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.appeal-success-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.appeal-success-header h3 {
  margin: 4px 0 0;
  font-size: 24px;
  color: #0f172a;
  line-height: 1.45;
  white-space: normal;
  word-break: break-word;
}

.appeal-success-kicker {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: #10b981;
  text-transform: uppercase;
}

.appeal-success-kicker.is-processing {
  color: #f59e0b;
}

.appeal-success-check {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  box-shadow: 0 18px 36px rgba(34, 197, 94, 0.22);
}

.appeal-success-check.is-processing {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  box-shadow: 0 18px 36px rgba(245, 158, 11, 0.22);
}

.appeal-success-body {
  display: grid;
  gap: 18px;
}

.appeal-success-descriptions :deep(.el-descriptions__label) {
  width: 118px;
  font-weight: 700;
  color: #334155;
  background: #f8fafc;
}

.appeal-success-descriptions :deep(.el-descriptions__content) {
  padding: 16px 18px;
}

.appeal-success-value {
  font-weight: 600;
  color: #0f172a;
}

.appeal-success-result {
  line-height: 1.75;
  color: #0f172a;
}

.appeal-success-footer {
  display: flex;
  justify-content: flex-end;
}

.repeat-appeal-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.repeat-appeal-badge {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  box-shadow: 0 18px 36px rgba(245, 158, 11, 0.22);
}

.repeat-appeal-kicker {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: #f59e0b;
  text-transform: uppercase;
}

.repeat-appeal-header h3 {
  margin: 4px 0 0;
  font-size: 24px;
  color: #0f172a;
}

.repeat-appeal-body {
  display: grid;
  gap: 18px;
}

.repeat-appeal-descriptions :deep(.el-descriptions__label) {
  width: 118px;
  font-weight: 700;
  color: #334155;
  background: #f8fafc;
}

.repeat-appeal-descriptions :deep(.el-descriptions__content) {
  padding: 16px 18px;
}

.repeat-appeal-value {
  font-weight: 600;
  color: #0f172a;
}

.repeat-appeal-message {
  line-height: 1.75;
  color: #0f172a;
}

.repeat-appeal-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
