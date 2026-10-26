<template>
  <div class="app-container mark-user-wuma-page">
    <el-card shadow="never" class="wuma-page-card" :body-style="{ padding: '0' }">
      <div class="platform-main">
        <div v-if="activeTab === 'submit'" class="platform-main-remain">
          <div class="submit-right-remain">当前剩余：<span>{{ remainCount }}</span> 次</div>
        </div>
        <el-tabs v-model="activeTab" class="sub-tabs">
          <el-tab-pane :label="submitTabLabel" name="submit">
            <div class="submit-pane">
              <div class="wuma-content-shell">
                <div class="wuma-submit-panel">
                  <div class="wuma-submit-title">{{ platformName }}标记提交</div>
                  <el-input v-model="phonesText" type="textarea" :rows="9" resize="vertical" class="wuma-submit-textarea" :disabled="submitting" placeholder="可粘贴混合文本，点击「号码提取」整理号码；支持手机号或座机号" />
                  <div class="wuma-submit-stats">
                    有效号码：<strong>{{ phoneStats.phones.length }}</strong> 个
                    <span v-if="phoneStats.duplicateCount">· 已去重 {{ phoneStats.duplicateCount }} 个</span>
                    <span v-if="phoneStats.invalidCount">· 未识别 {{ phoneStats.invalidCount }} 项</span>
                    <span v-if="phoneStats.phones.length" class="wuma-submit-stats__deduct">· 预计扣除 {{ expectedAmount }} 次</span>
                  </div>
                  <div class="wuma-submit-actions">
                    <div v-if="!platformEnabled" class="submit-warning-bar">{{ platformLoading ? '正在加载平台信息…' : `${platformName}平台未开启，请联系管理员。` }}</div>
                    <div v-else-if="!hasSubmitQuota" class="submit-warning-bar">当前平台剩余次数不足，无法提交。</div>
                    <div v-else-if="insufficientRemain && phoneStats.phones.length" class="submit-warning-bar">当前平台剩余次数不足，本次需 {{ expectedAmount }} 次，剩余 {{ remainCount }} 次。</div>
                    <div class="wuma-submit-buttons">
                      <el-button type="warning" class="wuma-action-btn wuma-action-btn--extract" :disabled="!String(phonesText || '').trim() || submitting" @click="extractPhones">号码提取</el-button>
                      <el-button type="primary" class="wuma-action-btn" :loading="submitting" :disabled="!canSubmit" v-hasPermi="['server:markUser:order:add']" @click="prepareSubmit">批量提交</el-button>
                      <el-button class="wuma-action-btn" :disabled="submitting || !String(phonesText || '').trim()" @click="resetSubmission">重置</el-button>
                    </div>
                  </div>
                </div>
                <div class="wuma-result-block">
                  <div class="wuma-result-head">提交结果</div>
                  <el-table :data="submitResultRows" border stripe class="wuma-result-table">
                    <el-table-column prop="label" label="项目" width="108" align="center" />
                    <el-table-column prop="value" label="信息备注" min-width="200" show-overflow-tooltip />
                    <template #empty><span class="wuma-result-empty">暂无提交结果</span></template>
                  </el-table>
                </div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane :label="recordTabLabel" name="record">
            <div class="record-pane">
              <el-form :model="query" :inline="true" size="small" label-width="68px" class="record-form" @submit.prevent>
                <el-form-item label="综合搜索"><el-input v-model="query.keyword" clearable placeholder="订单号/手机号" style="width: 220px" @keyup.enter="searchRecords" @clear="searchRecords" /></el-form-item>
                <el-form-item label="处理状态"><el-select v-model="query.orderStatus" clearable placeholder="状态" style="width: 120px" @change="searchRecords" @clear="searchRecords"><el-option label="待处理" value="0" /><el-option label="处理完成" value="1" /><el-option label="处理失败" value="2" /></el-select></el-form-item>
                <el-form-item label="提交时间"><el-date-picker v-model="dateRange" type="daterange" range-separator="-" start-placeholder="开始" end-placeholder="结束" format="YYYY/MM/DD" value-format="YYYY-MM-DD" style="width: 240px" @change="searchRecords" /></el-form-item>
                <el-form-item class="record-form__actions"><el-button type="primary" icon="Search" @click="searchRecords">搜索</el-button><el-button icon="Refresh" @click="resetRecords">重置</el-button><el-button icon="Download" @click="exportRecords">导出</el-button></el-form-item>
              </el-form>
              <RecordBatchActions :total="total" :selected-rows="selectedRows" />
              <el-table v-loading="recordsLoading" :data="records" row-key="itemId" border stripe class="record-table" @selection-change="selectedRows = $event">
                <el-table-column type="selection" width="48" align="center" />
                <el-table-column label="号码（点击复制）" min-width="150" show-overflow-tooltip><template #default="{ row }"><el-button link type="primary" @click="copyPhone(row.phonePreview)">{{ row.phonePreview || '-' }}</el-button></template></el-table-column>
                <el-table-column label="平台" prop="platformName" min-width="130" show-overflow-tooltip />
                <el-table-column label="处理状态" width="100" align="center"><template #default="{ row }"><el-tag :type="markItemProcessStatusTagType(row.itemProcessStatus)" size="small">{{ markItemProcessStatusLabel(row.itemProcessStatus) }}</el-tag></template></el-table-column>
                <el-table-column label="备注" min-width="160" align="center"><template #default="{ row }"><OrderRemarkEditor :row="row" /></template></el-table-column>
                <el-table-column label="提交时间" min-width="160" align="center"><template #default="{ row }">{{ formatTime(row.createTime) }}</template></el-table-column>
                <el-table-column label="处理完成" min-width="160" align="center"><template #default="{ row }">{{ formatTime(row.itemProcessedTime) }}</template></el-table-column>
                <el-table-column label="订单号" prop="orderNo" min-width="180" show-overflow-tooltip />
                <el-table-column label="处理结果" width="100" align="center" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="showResult(row)">查看</el-button></template></el-table-column>
              </el-table>
              <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadRecords" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
    <SubmitConfirmDialog v-model="confirmOpen" :platform-name="pendingSubmission?.platformName || ''" :phones="pendingSubmission?.phones || []" :loading="submitting" @confirm="submitOrder" @cancel="confirmOpen = false" />
    <el-dialog v-model="resultOpen" title="处理结果" width="min(560px, 92vw)" destroy-on-close>
      <div v-loading="resultLoading">
        <el-descriptions v-if="resultItem" :column="1" border>
          <el-descriptions-item label="号码">{{ resultItem.phone }}</el-descriptions-item>
          <el-descriptions-item label="平台">{{ platformName }}</el-descriptions-item>
          <el-descriptions-item label="处理状态">{{ markItemProcessStatusLabel(resultItem.processStatus) }}</el-descriptions-item>
          <el-descriptions-item label="处理结果">{{ resultText(resultItem) }}</el-descriptions-item>
          <el-descriptions-item label="处理说明">{{ resultItem.processNote || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ formatTime(resultItem.processedTime) }}</el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无处理结果" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup name="MarkUserWuma">
import { computed, getCurrentInstance, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute } from 'vue-router'
import { createMarkUserOrder, getMarkUserOrderDetail, listMarkUserOrder, listMarkUserPlatformPrice } from '@/api/server/markUser'
import { extractPhoneNumbersPreservingOrder, formatExtractedPhones } from '@/utils/markPhoneExtract'
import { resolveMarkPlatformCodeFromRoute } from '@/utils/markPlatformRoute'
import { WUMA_PLATFORM_CODES, WUMA_PLATFORM_NAMES } from '@/utils/markWumaPlatform'
import { markItemProcessStatusLabel, markItemProcessStatusTagType } from '@/utils/markProcessStatus'
import SubmitConfirmDialog from './components/SubmitConfirmDialog.vue'
import OrderRemarkEditor from './components/OrderRemarkEditor.vue'
import RecordBatchActions from './components/RecordBatchActions.vue'

const { proxy } = getCurrentInstance()
const route = useRoute()
const platformCode = computed(() => resolveMarkPlatformCodeFromRoute(route, WUMA_PLATFORM_CODES))
const platformName = computed(() => WUMA_PLATFORM_NAMES[platformCode.value] || '无码处理')
const submitTabLabel = computed(() => `${platformName.value} - 提交号码`)
const recordTabLabel = computed(() => `${platformName.value} - 任务记录`)
const platformInfo = ref(null)
const platformLoading = ref(false)
const platformEnabled = computed(() => !!platformInfo.value && String(platformInfo.value.status ?? '0') !== '1')
const remainCount = computed(() => Math.max(0, Number(platformInfo.value?.remainCount) || 0))
const unitPrice = computed(() => Math.max(1, Number(platformInfo.value?.unitPrice) || 1))
const activeTab = ref('submit')
const phonesText = ref('')
const remark = ref('')
const submitting = ref(false)
const submitResult = ref(null)
const confirmOpen = ref(false)
const pendingSubmission = ref(null)
let submissionRequestNo = ''
const phoneStats = computed(() => {
  const segments = phonesText.value.split(/[\n,，;；]+/).map((text) => text.trim()).filter(Boolean)
  const extracted = segments.map((segment) => extractPhoneNumbersPreservingOrder(segment, true))
  const allPhones = extracted.flat()
  const phones = [...new Set(allPhones)]
  return { phones, duplicateCount: allPhones.length - phones.length, invalidCount: extracted.filter((phones) => !phones.length).length }
})
const expectedAmount = computed(() => phoneStats.value.phones.length * unitPrice.value)
const hasSubmitQuota = computed(() => remainCount.value >= unitPrice.value)
const insufficientRemain = computed(() => expectedAmount.value > remainCount.value)
const canSubmit = computed(() => platformEnabled.value && !platformLoading.value && !!phoneStats.value.phones.length && !insufficientRemain.value)
const submitResultRows = computed(() => {
  if (!submitResult.value) return []
  return [
    { label: '提交订单号', value: submitResult.value.orderNo || '-' },
    { label: '提交数量', value: `正常提交了${submitResult.value.phoneCount}个号码` },
    { label: '扣除次数', value: `${submitResult.value.totalAmount} 次` }
  ]
})
const records = ref([])
const selectedRows = ref([])
const total = ref(0)
const recordsLoading = ref(false)
const dateRange = ref([])
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', orderStatus: '' })
let recordsRequest = 0
const resultOpen = ref(false)
const resultLoading = ref(false)
const resultItem = ref(null)
let resultRequest = 0

async function loadPlatform() {
  const code = platformCode.value
  platformLoading.value = true
  try {
    const response = await listMarkUserPlatformPrice()
    if (code === platformCode.value) {
      platformInfo.value = (response.data || []).find((item) => String(item.platformCode || '').trim().toLowerCase() === code) || null
    }
  } catch {
    if (code === platformCode.value) platformInfo.value = null
  } finally {
    if (code === platformCode.value) platformLoading.value = false
  }
}

function extractPhones() {
  const phones = phoneStats.value.phones
  if (!phones.length) {
    proxy.$modal.msgWarning('未提取到有效号码（手机号或座机号）')
    return
  }
  phonesText.value = formatExtractedPhones(phones)
  proxy.$modal.msgSuccess(`已提取 ${phones.length} 个号码，请确认后点击「批量提交」`)
}

function resetSubmission() {
  phonesText.value = ''
  remark.value = ''
  submitResult.value = null
  pendingSubmission.value = null
  submissionRequestNo = ''
}

watch([phonesText, remark, platformCode], () => { submissionRequestNo = '' })

function prepareSubmit() {
  if (submitting.value) return
  if (!phoneStats.value.phones.length) return proxy.$modal.msgWarning('请输入有效号码（手机号或座机号）')
  if (!platformEnabled.value) return proxy.$modal.msgWarning(`${platformName.value}平台未开启，请联系管理员`)
  if (insufficientRemain.value) return proxy.$modal.msgWarning(`当前平台剩余次数不足，本次需 ${expectedAmount.value} 次，剩余 ${remainCount.value} 次`)
  if (!submissionRequestNo) submissionRequestNo = `wuma-${globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`}`
  pendingSubmission.value = {
    platformCode: platformCode.value, platformName: platformName.value,
    phones: [...phoneStats.value.phones], remark: remark.value.trim(), requestNo: submissionRequestNo
  }
  confirmOpen.value = true
}

async function submitOrder() {
  if (submitting.value || !pendingSubmission.value) return
  submitting.value = true
  const payload = pendingSubmission.value
  try {
    const response = await createMarkUserOrder(payload)
    const order = response.data?.order || response.data || {}
    submitResult.value = {
      orderNo: order.orderNo || '-',
      phoneCount: Number(order.totalCount || payload.phones.length),
      totalAmount: Number(order.totalAmount ?? payload.phones.length * unitPrice.value)
    }
    phonesText.value = ''
    remark.value = ''
    confirmOpen.value = false
    pendingSubmission.value = null
    submissionRequestNo = ''
    await loadPlatform()
    proxy.$modal.msgSuccess(`正常提交了${submitResult.value.phoneCount}个号码`)
  } catch {
    // Retain the payload and request number so an uncertain network result can be retried.
  } finally {
    submitting.value = false
  }
}

async function loadRecords() {
  const code = platformCode.value
  if (!code) return
  const request = ++recordsRequest
  recordsLoading.value = true
  selectedRows.value = []
  try {
    const response = await listMarkUserOrder({
      ...query, keyword: query.keyword.trim() || null, platformCode: code,
      params: dateRange.value?.length === 2 ? { beginTime: dateRange.value[0], endTime: dateRange.value[1] } : {}
    })
    if (request === recordsRequest && code === platformCode.value) {
      records.value = response.rows || []
      total.value = response.total || 0
    }
  } finally {
    if (request === recordsRequest) recordsLoading.value = false
  }
}

function searchRecords() { query.pageNum = 1; loadRecords().catch(() => {}) }
function resetRecords() {
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', orderStatus: '' })
  dateRange.value = []
  records.value = []
  selectedRows.value = []
  total.value = 0
  if (activeTab.value === 'record') searchRecords()
}

function formatTime(value) { return value ? proxy.parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') || value : '-' }
function resultText(item) {
  if (item.processResult === 'success') return '处理成功'
  if (item.processResult === 'failed') return '处理失败'
  return item.processResult || '-'
}

async function showResult(row) {
  const code = platformCode.value
  const request = ++resultRequest
  resultOpen.value = true
  resultItem.value = null
  resultLoading.value = true
  try {
    const response = await getMarkUserOrderDetail(row.id)
    if (request === resultRequest && code === platformCode.value) resultItem.value = (response.data?.items || []).find((item) => String(item.id) === String(row.itemId)) || null
  } catch {
    // The request interceptor displays the error; keep the dialog available for retry.
  } finally {
    if (request === resultRequest) resultLoading.value = false
  }
}

async function copyPhone(phone) {
  try { await navigator.clipboard.writeText(phone); proxy.$modal.msgSuccess('号码已复制') }
  catch { proxy.$modal.msgWarning('复制失败，请手动复制号码') }
}

function exportRecords() {
  const rows = selectedRows.value.length ? selectedRows.value : records.value
  if (!rows.length) return proxy.$modal.msgWarning('暂无可导出记录')
  const table = [
    ['号码', '平台', '处理状态', '备注', '提交时间', '处理时间', '订单号'],
    ...rows.map((row) => [row.phonePreview, row.platformName, markItemProcessStatusLabel(row.itemProcessStatus), row.userRemark, formatTime(row.createTime), formatTime(row.itemProcessedTime), row.orderNo])
  ]
  const content = '\ufeff' + table.map((row) => row.map((cell) => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(',')).join('\n')
  const url = URL.createObjectURL(new Blob([content], { type: 'text/csv;charset=utf-8;' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `${platformName.value}_任务记录_${Date.now()}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

watch(platformCode, () => {
  recordsRequest++
  resultRequest++
  resultLoading.value = false
  recordsLoading.value = false
  platformInfo.value = null
  confirmOpen.value = false
  pendingSubmission.value = null
  resultOpen.value = false
  activeTab.value = 'submit'
  resetSubmission()
  resetRecords()
  if (platformCode.value) loadPlatform()
}, { immediate: true })
watch(activeTab, (tab) => { if (tab === 'record') { loadRecords().catch(() => {}); loadPlatform() } })
onBeforeRouteLeave(() => !submitting.value)
onBeforeRouteUpdate(() => !submitting.value)
</script>

<style scoped>
.mark-user-wuma-page { padding: 12px; }
.wuma-page-card { min-height: 100%; }
.platform-main { position: relative; min-height: 680px; }
.platform-main-remain { position: absolute; top: 15px; right: 18px; z-index: 3; color: #606266; font-size: 14px; }
.submit-right-remain span { color: #f56c6c; font-size: 18px; font-weight: 700; }
.sub-tabs :deep(.el-tabs__header) { margin: 0; padding: 0 18px; }
.sub-tabs :deep(.el-tabs__nav-wrap::after) { height: 1px; }
.sub-tabs :deep(.el-tabs__item) { height: 50px; line-height: 50px; font-weight: 600; }
.submit-pane, .record-pane { padding: 18px; }
.wuma-content-shell { width: min(684px, 100%); }
.wuma-submit-panel, .wuma-result-block { border: 1px solid #e4eaf2; border-radius: 6px; background: #fff; }
.wuma-submit-panel { padding: 16px; }
.wuma-submit-title, .wuma-result-head { margin-bottom: 12px; color: #1f2d3d; font-size: 15px; font-weight: 700; }
.wuma-submit-textarea :deep(.el-textarea__inner) { min-height: 194px !important; border-color: #dbe3ee; }
.wuma-submit-stats { min-height: 28px; padding: 10px 0 5px; color: #5c6b7a; font-size: 14px; }
.wuma-submit-stats strong { color: #409eff; font-size: 16px; }
.wuma-submit-stats span { color: #8a97a6; }
.wuma-submit-stats .wuma-submit-stats__deduct { color: #e6a23c; }
.wuma-submit-actions { margin-top: 4px; }
.submit-warning-bar { margin: 8px 0 12px; padding: 11px 14px; color: #f56c6c; background: #fef0f0; border-radius: 4px; font-size: 14px; }
.wuma-submit-buttons { display: flex; flex-wrap: wrap; gap: 10px; }
.wuma-submit-buttons .el-button { min-width: 90px; margin-left: 0; }
.wuma-action-btn--extract { color: #fff; background: #e6bd73; border-color: #e6bd73; }
.wuma-action-btn--extract:hover { background: #d9ae60; border-color: #d9ae60; }
.wuma-result-block { margin-top: 14px; padding: 16px; }
.wuma-result-table :deep(.el-table__header th) { background: #eef4f9; color: #1f2d3d; }
.wuma-result-empty { display: inline-block; padding: 48px 0; color: #909399; }
.record-form { margin-bottom: 4px; }
.record-form :deep(.el-form-item) { margin-right: 16px; margin-bottom: 12px; }
.record-form__actions :deep(.el-form-item__content) { gap: 8px; }
.record-form__actions .el-button { margin-left: 0; }
.record-table { width: 100%; }
@media (max-width: 768px) {
  .mark-user-wuma-page { padding: 8px; }
  .platform-main-remain { position: static; padding: 12px 16px 0; text-align: right; }
  .sub-tabs :deep(.el-tabs__header) { padding: 0 10px; }
  .submit-pane, .record-pane { padding: 12px; }
  .wuma-submit-panel, .wuma-result-block { padding: 12px; }
}
</style>
