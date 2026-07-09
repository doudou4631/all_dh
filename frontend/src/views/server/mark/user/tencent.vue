<template>
  <div class="app-container tencent-mark-page">
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
                    <div class="tencent-submit-title">{{ platformName }}平台清除(普通&高频)</div>
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
                    <div v-if="remainCount < 1" class="submit-warning-bar">
                      当前{{ platformName }}平台剩余次数不足，无法提交。
                    </div>

                    <el-form
                      label-width="72px"
                      size="default"
                      class="tencent-submit-form"
                      @keyup.enter="handleSubmit"
                    >
                      <el-form-item label="手机号">
                        <el-input
                          v-model="form.phone"
                          maxlength="11"
                          clearable
                          placeholder="请输入11位手机号"
                          @input="handlePhoneInput"
                        />
                      </el-form-item>
                      <el-form-item label="验证码">
                        <el-input
                          v-model="form.smsCode"
                          maxlength="6"
                          clearable
                          placeholder="请输入6位验证码"
                          @input="handleSmsInput"
                        />
                      </el-form-item>
                    </el-form>

                    <div class="tencent-submit-buttons">
                      <el-button
                        v-if="isTdSecondPlatform"
                        type="primary"
                        plain
                        class="tencent-action-btn"
                        @click="handleFetchSms"
                      >
                        获取短信
                      </el-button>
                      <el-button
                        type="primary"
                        class="tencent-action-btn"
                        :loading="submitting"
                        :disabled="remainCount < 1"
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

                <div class="tencent-result-block">
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
                  />
                </el-form-item>
                <el-form-item label="处理状态">
                  <el-select
                    v-model="queryParams.orderStatus"
                    clearable
                    placeholder="状态"
                    style="width: 120px"
                  >
                    <el-option label="待处理" value="0" />
                    <el-option label="处理中" value="1" />
                    <el-option label="已完成" value="2" />
                    <el-option label="已取消" value="3" />
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

<script setup name="MarkUserTencent">
import {
  listMarkUserPlatformPrice,
  listMarkUserOrder,
  submitMarkUserTencent,
  getMarkUserTencentSubmitResult,
  getMarkUserOrderDetail
} from '@/api/server/markUser'
import { useRoute } from 'vue-router'
import {
  TENCENT_PLATFORM_CODE,
  resolveTencentStylePlatformName
} from '@/utils/markTencentPlatform'

const { proxy } = getCurrentInstance()
const route = useRoute()
const SMS_APPLY_URL = 'https://yun.m.qq.com/person_apply.html'

const activePlatformCode = computed(() => {
  return String(route.query?.platformCode || TENCENT_PLATFORM_CODE).trim().toLowerCase() || TENCENT_PLATFORM_CODE
})

const routePlatformName = computed(() => {
  return String(route.query?.platformName || '').trim() || resolveTencentStylePlatformName(activePlatformCode.value)
})

const isTdSecondPlatform = computed(() => activePlatformCode.value === 'td_second')

const activeTab = ref('submit')
const submitting = ref(false)
const remainCount = ref(0)
const platformName = ref(routePlatformName.value)
const submitResult = ref(null)

const form = reactive({
  phone: '',
  smsCode: ''
})

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
  const phoneCount = Number(submitResult.value.phoneCount || 0)
  return [
    { label: '提交订单号', value: submitResult.value.orderNo || '-' },
    {
      label: '提交数量',
      value: phoneCount > 0 ? `正常提交了${phoneCount}个手机号码` : '-'
    }
  ]
})

function normalizePhone(value) {
  return String(value || '').replace(/[^\d]/g, '')
}

function handlePhoneInput(value) {
  form.phone = normalizePhone(value).slice(0, 11)
}

function handleSmsInput(value) {
  form.smsCode = normalizePhone(value).slice(0, 6)
}

function resetSubmitState() {
  submitResult.value = null
}

function handleResetForm() {
  form.phone = ''
  form.smsCode = ''
  resetSubmitState()
}

function handleFetchSms() {
  window.open(SMS_APPLY_URL, '_blank', 'noopener,noreferrer')
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
  return '提交成功，后台处理中'
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
  proxy.$modal.msgWarning('后台处理中，请稍后在提交记录中查看结果')
}

async function loadPlatformInfo() {
  platformName.value = routePlatformName.value
  try {
    const res = await listMarkUserPlatformPrice()
    const list = Array.isArray(res?.data) ? res.data : []
    const item = list.find((row) => String(row.platformCode || '').toLowerCase() === activePlatformCode.value)
      || list.find((row) => String(row.platformName || '') === platformName.value)
    const remain = Number(item?.remainCount ?? 0)
    if (item?.platformName) {
      platformName.value = item.platformName
    }
    remainCount.value = Number.isFinite(remain) ? Math.max(0, remain) : 0
  } catch {
    remainCount.value = 0
  }
}

async function handleSubmit() {
  const phone = normalizePhone(form.phone)
  if (!/^\d{11}$/.test(phone)) {
    proxy.$modal.msgWarning('请输入11位手机号')
    return
  }
  if (remainCount.value < 1) {
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
      proxy.$modal.msgSuccess('提交成功，后台处理中')
      await pollTencentSubmitResult(result.itemId)
    }
  } catch {
    // submit failure: no floating toast
  } finally {
    submitting.value = false
  }
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

function recordStatusLabel(row) {
  const auditStatus = String(row?.auditStatus ?? '1')
  if (auditStatus === '0') return '待审核'
  if (auditStatus === '2') return '已拒绝'
  if (auditStatus === '3') return '已打回'
  const itemStatus = String(row?.itemProcessStatus ?? '')
  if (itemStatus === '0') return '待处理'
  if (itemStatus === '1') return '成功'
  if (itemStatus === '2') return '失败'
  const status = String(row?.orderStatus ?? '')
  const successCount = Number(row?.successCount ?? 0)
  const failedCount = Number(row?.failedCount ?? 0)
  if (status === '0' || status === '1') return '待处理'
  if (status === '2') return failedCount > 0 && successCount <= 0 ? '失败' : (failedCount > 0 ? '部分失败' : '成功')
  if (status === '3') return '失败'
  return '待处理'
}

function recordStatusType(row) {
  const auditStatus = String(row?.auditStatus ?? '1')
  if (auditStatus === '2') return 'danger'
  if (auditStatus === '3') return 'warning'
  if (auditStatus === '0') return 'info'
  const itemStatus = String(row?.itemProcessStatus ?? '')
  if (itemStatus === '1') return 'success'
  if (itemStatus === '2') return 'danger'
  if (itemStatus === '0') return 'warning'
  const label = recordStatusLabel(row)
  if (label === '成功') return 'success'
  if (label === '失败' || label === '部分失败') return 'danger'
  return 'warning'
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
  if (!Array.isArray(value) || value.length !== 2 || !value[0] || !value[1]) {
    recordDateRange.value = []
    queryParams.params = {}
    return
  }
  queryParams.params = {
    beginTime: value[0],
    endTime: value[1]
  }
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
  const header = ['用户名', '手机号', '平台', '处理状态', '订单号', '提交时间']
  const body = rows.map((item) => [
    item.userName || '',
    item.phonePreview || '',
    item.platformName || '',
    recordStatusLabel(item),
    item.orderNo || '',
    formatDateTime(item.createTime)
  ])
  downloadCsv(`${activePlatformCode.value || 'tencent'}-record-${Date.now()}.csv`, [header, ...body])
}

let recordPollTimer = null
function startRecordPolling() {
  stopRecordPolling()
  recordPollTimer = window.setInterval(() => {
    if (activeTab.value !== 'record') return
    getList()
  }, 5000)
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
    startRecordPolling()
  } else {
    stopRecordPolling()
  }
}

watch(activeTab, () => {
  syncRecordTabData()
})

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

.tencent-submit-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-left: 72px;
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

@media (max-width: 768px) {
  .tencent-content-shell {
    width: 100%;
    max-width: none;
    min-width: 0;
  }

  .tencent-submit-buttons {
    padding-left: 0;
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
