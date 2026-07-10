<template>
  <div class="app-container mark-user-qihu360-page">
    <el-card shadow="never" class="qihu360-page-card" :body-style="{ padding: '0' }">
      <div class="platform-main">
        <div v-if="activeTab === 'submit'" class="platform-main-remain">
          <div class="submit-right-remain">
            当前剩余：<span>{{ remainCount }}</span> 次
          </div>
        </div>
        <el-tabs v-model="activeTab" class="sub-tabs">
          <el-tab-pane :label="submitTabLabel" name="submit">
            <div class="submit-pane">
              <div class="qihu360-content-shell">
                <div class="qihu360-submit-panel">
                  <div class="qihu360-submit-section">
                    <div class="qihu360-submit-title">360标记提交</div>
                    <el-input
                      v-model="phonesText"
                      type="textarea"
                      :rows="9"
                      resize="vertical"
                      class="qihu360-submit-textarea"
                      placeholder="可粘贴混合文本，点击「号码提取」整理号码；每行1个号码，7-15位数字"
                    />
                    <div class="qihu360-submit-stats">
                      有效号码：<strong>{{ validPhones.length }}</strong> 个
                      <span v-if="validPhones.length > 0" class="qihu360-submit-stats__deduct">
                        · 预计扣除 {{ expectedDeductAmount }} 次
                      </span>
                    </div>
                    <div class="qihu360-submit-actions">
                      <div v-if="!platformEnabled" class="submit-warning-bar">
                        {{ platformClosedMessage }}
                      </div>
                      <div v-else-if="!hasSubmitQuota" class="submit-warning-bar">
                        当前平台剩余次数不足，无法提交。
                      </div>
                      <div v-else-if="insufficientRemain && validPhones.length" class="submit-warning-bar">
                        当前平台剩余次数不足，本次需 {{ expectedDeductAmount }} 次，剩余 {{ remainCount }} 次。
                      </div>
                      <div class="qihu360-submit-buttons">
                        <el-button
                          type="warning"
                          class="qihu360-action-btn qihu360-action-btn--extract"
                          :disabled="!String(phonesText || '').trim() || submitting"
                          @click="handleExtractPhones"
                        >
                          号码提取
                        </el-button>
                        <el-button
                          type="primary"
                          class="qihu360-action-btn"
                          :loading="submitting"
                          :disabled="!validPhones.length || !platformEnabled || !hasSubmitQuota || insufficientRemain"
                          v-hasPermi="['server:markUser:order:clear']"
                          @click="handleSubmit"
                        >
                          批量提交
                        </el-button>
                        <el-button
                          class="qihu360-action-btn"
                          :disabled="submitting || !String(phonesText || '').trim()"
                          @click="handleReset"
                        >
                          重置
                        </el-button>
                        <el-select
                          v-if="showCoverageRemarkSelect"
                          v-model="coverageRemark"
                          class="qihu360-remark-select"
                          placeholder="请选择备注"
                          clearable
                          :disabled="submitting"
                        >
                          <el-option
                            v-for="item in coverageRemarkOptions"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                          />
                        </el-select>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="qihu360-result-block">
                  <div class="qihu360-result-head">提交结果</div>
                  <el-table :data="resultRows" border stripe class="qihu360-result-table">
                    <el-table-column prop="label" label="项目" width="108" align="center" />
                    <el-table-column prop="value" label="信息备注" min-width="200" show-overflow-tooltip />
                    <template #empty>
                      <span class="qihu360-result-empty">暂无提交结果</span>
                    </template>
                  </el-table>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="recordTabLabel" name="record">
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
                  <el-select v-model="queryParams.orderStatus" clearable placeholder="状态" style="width: 120px" @change="handleQuery" @clear="handleQuery">
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
                <el-table-column label="平台" prop="platformName" min-width="110" show-overflow-tooltip />
                <el-table-column label="处理状态" width="92" align="center">
                  <template #default="scope">
                    <el-tag :type="recordStatusType(scope.row)" size="small">
                      {{ recordStatusLabel(scope.row) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="订单号" prop="orderNo" min-width="180" show-overflow-tooltip />
                <el-table-column label="提交时间" min-width="160" align="center">
                  <template #default="scope">
                    {{ formatDateTime(scope.row.createTime) }}
                  </template>
                </el-table-column>
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

<script setup name="MarkUserQihu360">
import { computed, ref, reactive, watch, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useRoute } from 'vue-router'
import { createMarkUserClearOrder, listMarkUserPlatformPrice, listMarkUserOrder } from '@/api/server/markUser'
import { extractPhoneNumbersPreservingOrder, formatExtractedPhones } from '@/utils/markPhoneExtract'
import {
  is360PlatformNameMatch,
  normalize360PlatformCode,
  resolve360PlatformCodeForPage,
  resolve360PlatformNameFallback
} from '@/utils/markQihu360Platform'

const { proxy } = getCurrentInstance()
const route = useRoute()

function resolveRoutePlatformCode() {
  const queryCode = String(route.query?.platformCode || '').trim().toLowerCase()
  if (queryCode) return resolve360PlatformCodeForPage(queryCode)
  const path = String(route.path || '').toLowerCase()
  if (path.includes('qihufirst')) return 'qihu_first'
  if (path.includes('qihusecond')) return 'qihu_second'
  if (path.includes('sanliuling')) return 'sanliuling'
  return 'sanliuling'
}

const activePlatformCode = ref(resolveRoutePlatformCode())
const submitTabLabel = computed(() => `${platformName.value} - 提交号码`)
const recordTabLabel = computed(() => `${platformName.value} - 任务记录`)

const activeTab = ref('submit')
const phonesText = ref('')
const submitting = ref(false)
const remainCount = ref(0)
const unitPrice = ref(1)
const platformName = ref(resolve360PlatformNameFallback(activePlatformCode.value))
const platformEnabled = ref(true)
const submitResult = ref(null)
const coverageRemark = ref('快递送餐')
const coverageRemarkOptions = [
  { label: '快递送餐', value: '快递送餐' },
  { label: '房产中介', value: '房产中介' },
  { label: '保险理财', value: '保险理财' }
]
const showCoverageRemarkSelect = computed(() => {
  const code = String(activePlatformCode.value || '').trim().toLowerCase()
  const name = String(platformName.value || '').trim().toLowerCase()
  return code === 'sanliuling' || name.includes('fugai') || name.includes('覆盖')
})

const loading = ref(false)
const total = ref(0)
const orderList = ref([])
const recordSelectedRows = ref([])
const recordDateRange = ref([])
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: null,
  phone: null,
  platformCode: activePlatformCode.value,
  orderStatus: null,
  params: {}
})

const validPhones = computed(() => {
  const seen = new Set()
  const result = []
  String(phonesText.value || '')
    .split(/\r?\n/)
    .forEach((line) => {
      const digits = String(line || '').replace(/\D/g, '')
      if (digits.length < 7 || digits.length > 15) return
      if (seen.has(digits)) return
      seen.add(digits)
      result.push(digits)
    })
  return result
})

const expectedDeductAmount = computed(() => validPhones.value.length * unitPrice.value)
const insufficientRemain = computed(() => expectedDeductAmount.value > remainCount.value)
const hasSubmitQuota = computed(() => remainCount.value >= unitPrice.value)
const platformClosedMessage = computed(() => `${platformName.value}平台未开启，请联系管理员`)

const resultRows = computed(() => {
  if (!submitResult.value) return []
  const phoneCount = Number(submitResult.value.phoneCount || 0)
  return [
    { label: '提交订单号', value: submitResult.value.orderNo || '-' },
    {
      label: '提交数量',
      value: phoneCount > 0 ? `正常提交了${phoneCount}个手机号码` : '-'
    }
  ]
})

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

async function loadPlatformInfo() {
  try {
    const resp = await listMarkUserPlatformPrice()
    const list = Array.isArray(resp?.data) ? resp.data : []
    const matched = list.find((item) => {
      const rawCode = String(item.platformCode || '').trim().toLowerCase()
      const code = rawCode === '360' ? 'sanliuling' : rawCode
      if (code === activePlatformCode.value) return true
      if (activePlatformCode.value !== 'sanliuling') return false
      const name = String(item.platformName || item.name || '').trim().toLowerCase()
      return is360PlatformNameMatch(item) && (rawCode === '360' || name.includes('fugai') || name.includes('覆盖'))
    })
    if (matched) {
      platformName.value = matched.platformName || platformName.value
      platformEnabled.value = String(matched.status ?? '0') !== '1'
      remainCount.value = Number(matched.remainCount ?? 0)
      const price = Number(matched.unitPrice ?? 1)
      unitPrice.value = Number.isFinite(price) && price > 0 ? price : 1
    } else {
      platformName.value = resolve360PlatformNameFallback(activePlatformCode.value)
      platformEnabled.value = false
      remainCount.value = 0
      unitPrice.value = 1
    }
  } catch (error) {
    platformEnabled.value = false
    remainCount.value = 0
    unitPrice.value = 1
  }
}

async function handleSubmit() {
  const phones = validPhones.value
  if (!phones.length) {
    proxy.$modal.msgWarning('请输入有效号码（每行1个，7-15位数字）')
    return
  }
  if (!platformEnabled.value) {
    proxy.$modal.msgError(platformClosedMessage.value)
    return
  }
  if (!hasSubmitQuota.value) {
    proxy.$modal.msgError('当前平台剩余次数不足，无法提交')
    return
  }
  if (insufficientRemain.value) {
    proxy.$modal.msgError(`当前平台剩余次数不足，本次需 ${expectedDeductAmount.value} 次，剩余 ${remainCount.value} 次`)
    return
  }
  const remark = String(coverageRemark.value || '').trim()
  if (showCoverageRemarkSelect.value && !remark) {
    proxy.$modal.msgWarning('请选择备注类型')
    return
  }
  submitting.value = true
  try {
    const res = await createMarkUserClearOrder({
      platformCode: activePlatformCode.value,
      platformName: platformName.value,
      phones,
      requestNo: '',
      remark
    })
    const order = res?.data?.order || res?.data || {}
    const phoneCount = Number(order.totalCount || phones.length || 0)
    submitResult.value = {
      orderNo: order.orderNo || '-',
      phoneCount
    }
    phonesText.value = ''
    coverageRemark.value = '快递送餐'
    await loadPlatformInfo()
    proxy.$modal.msgSuccess(`正常提交了${phoneCount}个手机号码`)
    if (activeTab.value === 'record') {
      await getList()
    }
  } catch (error) {
    proxy.$modal.msgError(error?.message || '提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  phonesText.value = ''
  coverageRemark.value = '快递送餐'
  submitResult.value = null
}

function handleExtractPhones() {
  const raw = String(phonesText.value || '').trim()
  if (!raw) {
    proxy.$modal.msgWarning('请先粘贴或输入包含号码的文本')
    return
  }
  const phones = extractPhoneNumbersPreservingOrder(raw)
  if (!phones.length) {
    proxy.$modal.msgWarning('未提取到有效号码（7-15位数字）')
    return
  }
  phonesText.value = formatExtractedPhones(phones)
  proxy.$modal.msgSuccess(`已提取 ${phones.length} 个号码，请确认后点击「批量提交」`)
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

function isAutoProcessingRecord(row) {
  const code = String(row?.platformCode || '').trim().toLowerCase()
  return ['tencent_mark', 'tengxun', 'tencent', 'tx', 'txwz'].includes(code)
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

function exportRecordRows() {
  const rows = recordSelectedRows.value.length > 0 ? recordSelectedRows.value : orderList.value
  if (!rows.length) {
    proxy.$modal.msgWarning('暂无可导出记录')
    return
  }
  const header = ['用户名', '号码（点击复制）', '平台', '处理状态', '订单号', '提交时间']
  const body = rows.map((item) => [
    item.userName || '',
    item.phonePreview || '',
    item.platformName || '',
    recordStatusLabel(item),
    item.orderNo || '',
    formatDateTime(item.createTime)
  ])
  downloadCsv(`qihu360-record-${Date.now()}.csv`, [header, ...body])
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

watch(
  () => route.query?.platformCode,
  (code) => {
    const nextCode = resolve360PlatformCodeForPage(code || activePlatformCode.value)
    if (nextCode === activePlatformCode.value) return
    activePlatformCode.value = nextCode
    queryParams.platformCode = nextCode
    platformName.value = resolve360PlatformNameFallback(nextCode)
    loadPlatformInfo()
    syncRecordTabData()
  }
)

onMounted(() => {
  activePlatformCode.value = resolveRoutePlatformCode()
  queryParams.platformCode = activePlatformCode.value
  platformName.value = resolve360PlatformNameFallback(activePlatformCode.value)
  loadPlatformInfo()
  syncRecordTabData()
})

onBeforeUnmount(() => {
  stopRecordPolling()
})
</script>

<style scoped lang="scss">
.mark-user-qihu360-page {
  padding: 0 !important;
  background: #f5f7fa;
}

.qihu360-page-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background: #fff;
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

.qihu360-content-shell {
  width: 66%;
  max-width: 660px;
  min-width: 420px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.qihu360-submit-panel {
  width: 100%;
}

.qihu360-submit-section {
  width: 100%;
  padding: 12px 14px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #fafbfc;
  box-sizing: border-box;
}

.qihu360-submit-title {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
}

.qihu360-submit-textarea {
  display: block;
  width: 100%;

  :deep(.el-textarea__inner) {
    display: block;
    width: 100%;
    height: 188px;
    min-height: 188px;
    padding: 9px 11px;
    font-size: 13px;
    line-height: 1.55;
    background: #fff;
    box-sizing: border-box;
    resize: vertical;
  }
}

.qihu360-submit-stats {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.qihu360-submit-stats strong {
  color: var(--el-color-primary);
  font-weight: 600;
}

.qihu360-submit-stats__deduct {
  color: #606266;
}

.qihu360-submit-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}

.qihu360-submit-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 2px;
}

.qihu360-action-btn {
  min-width: 88px;
  height: 32px;
  padding: 0 14px;
  margin: 0;
  font-size: 13px;
}

.qihu360-remark-select {
  width: 128px;
}

.qihu360-action-btn--extract {
  --el-button-bg-color: #d48806;
  --el-button-border-color: #d48806;
  --el-button-hover-bg-color: #b8740a;
  --el-button-hover-border-color: #b8740a;
  --el-button-active-bg-color: #9c6509;
  --el-button-active-border-color: #9c6509;
  --el-button-disabled-bg-color: #e8c98a;
  --el-button-disabled-border-color: #e8c98a;
  color: #fff;
}

.submit-warning-bar {
  width: 100%;
  padding: 8px 12px;
  border-radius: 4px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 13px;
  line-height: 1.5;
}

.qihu360-result-block {
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

.qihu360-result-head {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.qihu360-result-table {
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

.qihu360-result-empty {
  color: #909399;
  font-size: 13px;
}

.record-form {
  margin-bottom: 12px;
}

.record-form__actions {
  margin-left: auto;
}

.record-table {
  width: 100%;
}

@media (max-width: 768px) {
  .qihu360-content-shell {
    width: 100%;
    max-width: none;
    min-width: 0;
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

  .qihu360-submit-section,
  .qihu360-result-block {
    padding-left: 12px;
    padding-right: 12px;
  }
}
</style>
