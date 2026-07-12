<template>
  <div class="mark-agent-process-page">
    <el-tabs
      v-if="showSupplyTabs && supplyTabs.length > 1"
      v-model="activeSupplyKey"
      class="supply-tabs"
      @tab-change="handleSupplyChange"
    >
      <el-tab-pane
        v-for="item in supplyTabs"
        :key="item.key"
        :label="item.label"
        :name="item.key"
      />
    </el-tabs>

    <el-card shadow="never" class="search-card">
      <div class="search-panel">
        <div class="search-header">
          <span v-if="pageTitle" class="toolbar-title">{{ pageTitle }}</span>
          <span v-if="total > 0" class="toolbar-count">共 {{ total }} 条记录</span>
        </div>

        <el-alert
          v-if="autoDetectBanner"
          class="auto-detect-alert"
          :title="autoDetectBanner"
          type="info"
          :closable="false"
          show-icon
        />

        <el-form
          :model="queryParams"
          :inline="true"
          size="small"
          label-width="68px"
          class="search-form"
          @submit.prevent
        >
          <el-form-item label="号码">
            <el-input
              v-model="queryParams.phone"
              placeholder="请输入号码"
              clearable
              class="field-phone"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="处理状态">
            <el-select v-model="queryParams.processStatus" clearable placeholder="全部" class="field-status">
              <el-option v-for="item in processStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="截止日期">
            <el-date-picker
              v-model="deadlineDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择日期"
              clearable
              class="field-date"
            />
          </el-form-item>
          <el-form-item class="search-actions">
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="RefreshLeft" @click="resetQuery">重置</el-button>
            <el-button icon="RefreshRight" @click="getList">刷新</el-button>
          </el-form-item>
        </el-form>

        <div class="batch-action-bar">
          <span v-if="isXiaomiWorkbench" class="batch-action-bar__hint">
            已选择 <strong>{{ selectedRows.length }}</strong> 条号码
          </span>
          <el-button
            v-if="isXiaomiWorkbench"
            type="warning"
            plain
            :disabled="!selectedProcessRows.length || batchSubmitting"
            :loading="batchSubmitting"
            v-hasPermi="['server:markAgent:item:feedback']"
            @click="handleBatchMarkSubmitted"
          >
            批量处理
          </el-button>
          <el-button
            v-if="isXiaomiWorkbench"
            type="success"
            plain
            :disabled="!selectedSuccessRows.length || batchSuccessSubmitting"
            :loading="batchSuccessSubmitting"
            v-hasPermi="['server:markAgent:item:feedback']"
            @click="handleBatchSuccess"
          >
            批量成功
          </el-button>
          <el-button
            icon="Download"
            :loading="exportLoading"
            v-hasPermi="['server:markAgent:order:list']"
            @click="handleExport"
          >
            导出
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="itemList"
        border
        stripe
        size="small"
        class="process-table"
        style="width: 100%;"
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          v-if="isXiaomiWorkbench"
          type="selection"
          width="46"
          align="center"
          :selectable="rowSelectable"
        />
        <el-table-column label="提交用户" prop="userName" width="96" align="center" show-overflow-tooltip />
        <el-table-column label="手机/固话" prop="phone" min-width="118" align="center" show-overflow-tooltip />
        <el-table-column label="验证码" min-width="88" align="center" show-overflow-tooltip>
          <template #default="scope">{{ verifyCodeText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="100" align="center" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.processStatus === '0' && isTdGaopinPlatform(scope.row)" class="auto-detect-tip">自动检测</span>
            <span v-else>{{ submitRemarkText(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="处理状态" width="96" align="center">
          <template #default="scope">
            <el-tag :type="processStatusTagType(scope.row.processStatus, scope.row)" size="small" effect="light">
              {{ processStatusLabel(scope.row.processStatus, scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="批次编号" prop="orderNo" min-width="180" show-overflow-tooltip align="center" />
        <el-table-column label="创建时间" width="152" align="center" show-overflow-tooltip>
          <template #default="scope">{{ formatAgentDateTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="处理时间" width="152" align="center" show-overflow-tooltip>
          <template #default="scope">{{ processedTimeText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="scope">
            <div v-if="canManualProcess(scope.row)" class="action-cell">
              <el-select
                v-model="rowActionMap[scope.row.id]"
                size="small"
                class="action-select"
                :placeholder="resolveRowActionPlaceholder(scope.row)"
                :loading="submittingId === scope.row.id"
                :disabled="submittingId === scope.row.id"
                v-hasPermi="['server:markAgent:item:feedback']"
                @change="(val) => handleActionChange(scope.row, val)"
              >
                <el-option
                  v-for="item in rowFeedbackOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </div>
            <span v-else class="processed-tip">{{ processStatusLabel(scope.row.processStatus, scope.row) }}</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="emptyDescription" :image-size="72" />
        </template>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :page-sizes="[30, 60, 90, 120]"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { listMarkAgentOrderItem, feedbackMarkOrderItem, batchProcessXiaomi, batchMarkSuccess } from '@/api/server/markAgent'
import {
  MARK_ITEM_PROCESS_STATUS_OPTIONS,
  MARK_ITEM_FEEDBACK_OPTIONS,
  buildMarkItemProcessStatusQuery,
  markItemProcessStatusLabel,
  markItemProcessStatusTagType,
  isXiaomiPlatform as isXiaomiPlatformRow,
  canBatchProcessXiaomi,
  canBatchSuccessXiaomi,
  XIAOMI_PLATFORM_CODE
} from '@/utils/markProcessStatus'

const props = defineProps({
  platformCode: { type: String, default: '' },
  pageTitle: { type: String, default: '' },
  supplyTabs: { type: Array, default: () => ([
    { key: 'tdx', label: '供应(TDX泰迪频)', platformCodes: 'mobile_gaopin,td_gaopin,td_second' },
    { key: 'qihu', label: '供应(360奇虎)', platformCodes: 'qihu_first,qihu_second' }
  ]) },
  defaultSupplyKey: { type: String, default: 'tdx' },
  showSupplyTabs: { type: Boolean, default: true },
  defaultProcessStatus: { type: String, default: null }
})

function resolvePlatformCodes() {
  const code = String(props.platformCode || '').trim()
  if (code) return code
  const tab = props.supplyTabs.find((item) => item.key === props.defaultSupplyKey) || props.supplyTabs[0]
  return tab?.platformCodes || ''
}

const { proxy } = getCurrentInstance()
const loading = ref(false)
const exportLoading = ref(false)
const submittingId = ref(null)
const batchSubmitting = ref(false)
const batchSuccessSubmitting = ref(false)
const selectedRows = ref([])
const total = ref(0)
const itemList = ref([])
const rowActionMap = ref({})
const deadlineDate = ref('')
const activeSupplyKey = ref(props.defaultSupplyKey)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 90,
  phone: null,
  processStatus: props.defaultProcessStatus,
  platformCodes: resolvePlatformCodes(),
  params: {}
})
const isXiaomiWorkbench = computed(() => {
  const code = String(props.platformCode || '').trim().toLowerCase()
  return code === XIAOMI_PLATFORM_CODE
})
const processStatusOptions = computed(() => MARK_ITEM_PROCESS_STATUS_OPTIONS)
const emptyDescription = '暂无订单数据'
const rowFeedbackOptions = MARK_ITEM_FEEDBACK_OPTIONS
const processStatusLabel = markItemProcessStatusLabel
const processStatusTagType = markItemProcessStatusTagType
const selectedProcessRows = computed(() => (selectedRows.value || []).filter((row) => canBatchProcessXiaomi(row)))
const selectedSuccessRows = computed(() => (selectedRows.value || []).filter((row) => canBatchSuccessXiaomi(row)))

function isXiaomiPlatform(row) {
  return isXiaomiPlatformRow(row) || isXiaomiWorkbench.value
}
function verifyCodeText(row) {
  const result = String(row?.processResult || '').trim()
  if (result) return result
  const remark = String(row?.remark || '').trim()
  if (/^\d{6}$/.test(remark)) return remark
  if (row?.processStatus === '1') return 'success'
  return '-'
}
function isTencentAutoPlatform(row) {
  const code = String(row?.platformCode || props.platformCode || '').trim().toLowerCase()
  return ['tencent_mark', 'tengxun', 'tencent', 'tx', 'txwz'].includes(code)
}
function isTdGaopinPlatform(row) {
  const code = String(row?.platformCode || props.platformCode || '').trim().toLowerCase()
  return code === 'td_gaopin'
}
function canManualProcess(row) {
  if (isTencentAutoPlatform(row)) return false
  if (isTdGaopinPlatform(row) && String(row?.processStatus || '0') === '0') return false
  const status = String(row?.processStatus || '0')
  return status === '0' || status === '1' || status === '2' || status === '3'
}
function rowSelectable(row) {
  return canBatchProcessXiaomi(row) || canBatchSuccessXiaomi(row)
}
function handleSelectionChange(rows) {
  selectedRows.value = rows || []
}
function hasXiaomiProcessingItems(items) {
  return (items || []).some((row) => isXiaomiPlatform(row) && String(row?.processStatus || '0') === '3')
}
function shouldRunTdGaopinAutoDetect() {
  if (!hasTdGaopinPending(itemList.value)) return false
  const status = queryParams.processStatus
  if (status === '1' || status === '2') return false
  return true
}
function shouldRunTencentAutoRefresh() {
  if (!hasTencentPending(itemList.value)) return false
  const status = queryParams.processStatus
  if (status === '1' || status === '2') return false
  return true
}
const tdGaopinAutoDetecting = computed(() => shouldRunTdGaopinAutoDetect())
const tencentAutoProcessing = computed(() => shouldRunTencentAutoRefresh())
const autoDetectBanner = computed(() => {
  if (tencentAutoProcessing.value) {
    return '腾讯订单提交后约30秒自动处理，本页面每30秒刷新成功/失败状态'
  }
  if (tdGaopinAutoDetecting.value) {
    return '泰迪高频后台每30秒自动检测，本页面同步刷新状态'
  }
  return ''
})
function submitRemarkText(row) {
  const note = String(row?.processNote || '').trim()
  if (note) return note
  const remark = String(row?.orderRemark || '').trim()
  return remark || '-'
}
function processedTimeText(row) {
  if (!row || row.processStatus === '0') return '-'
  return formatAgentDateTime(row.processedTime) || '-'
}
function formatAgentDateTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
function buildQueryParams(overrides = {}) {
  const raw = { ...queryParams, ...overrides }
  const params = buildMarkItemProcessStatusQuery(raw)
  params.params = params.params || {}
  if (deadlineDate.value) params.params.endTime = deadlineDate.value
  return params
}
function refreshItemList() {
  return listMarkAgentOrderItem(buildQueryParams()).then((res) => {
    itemList.value = res.rows || []
    total.value = res.total || 0
    syncRowActionMap(itemList.value)
  })
}
function resolveRowActionPlaceholder(row) {
  const status = String(row?.processStatus || '0')
  if (status === '0' || status === '3') return '请选择操作'
  return processStatusLabel(status, row)
}
function syncRowActionMap(rows) {
  const map = { ...rowActionMap.value }
  for (const row of rows || []) {
    if (!row?.id || !canManualProcess(row)) continue
    const status = String(row.processStatus || '0')
    map[row.id] = (status === '0' || status === '3') ? '' : status
  }
  rowActionMap.value = map
}
function resetRowAction(row) {
  if (!row?.id) return
  const status = String(row.processStatus || '0')
  rowActionMap.value[row.id] = (status === '0' || status === '3') ? '' : status
}
function getList() {
  loading.value = true
  refreshItemList()
    .finally(() => {
      loading.value = false
      setupTencentAutoRefresh()
      setupTdGaopinAutoRefresh()
    })
}
function hasTencentPending(items) {
  return (items || []).some((row) => row.processStatus === '0' && isTencentAutoPlatform(row))
}
let tencentRefreshTimer = null
function clearTencentAutoRefresh() {
  if (tencentRefreshTimer) {
    clearInterval(tencentRefreshTimer)
    tencentRefreshTimer = null
  }
}
function setupTencentAutoRefresh() {
  clearTencentAutoRefresh()
  if (!shouldRunTencentAutoRefresh()) return
  tencentRefreshTimer = setInterval(() => {
    refreshItemList().then(() => {
      if (!shouldRunTencentAutoRefresh()) {
        clearTencentAutoRefresh()
      }
    })
  }, 30000)
}
function hasTdGaopinPending(items) {
  return (items || []).some((row) => row.processStatus === '0' && isTdGaopinPlatform(row))
}
let tdGaopinRefreshTimer = null
function clearTdGaopinAutoRefresh() {
  if (tdGaopinRefreshTimer) {
    clearInterval(tdGaopinRefreshTimer)
    tdGaopinRefreshTimer = null
  }
}
function setupTdGaopinAutoRefresh() {
  clearTdGaopinAutoRefresh()
  if (!shouldRunTdGaopinAutoDetect()) return
  tdGaopinRefreshTimer = setInterval(() => {
    refreshItemList().then(() => {
      if (!shouldRunTdGaopinAutoDetect()) {
        clearTdGaopinAutoRefresh()
      }
    })
  }, 30000)
}
function handleBatchMarkSubmitted() {
  const itemIds = selectedProcessRows.value.map((row) => row.id).filter(Boolean)
  if (!itemIds.length) {
    proxy.$modal.msgWarning('请先勾选待处理号码')
    return
  }
  proxy.$modal.confirm(`确认对选中的 ${itemIds.length} 条号码开启批量处理？开启后将自动检测号码状态。`).then(() => {
    batchSubmitting.value = true
    return batchProcessXiaomi({ itemIds })
  }).then((res) => {
    const updatedCount = res?.data?.updatedCount ?? 0
    const skippedCount = res?.data?.skippedCount ?? 0
    proxy.$modal.msgSuccess(`已开启批量处理 ${updatedCount} 条${skippedCount ? `，跳过 ${skippedCount} 条` : ''}`)
    selectedRows.value = []
    getList()
  }).catch(() => {}).finally(() => {
    batchSubmitting.value = false
  })
}
function handleBatchSuccess() {
  const itemIds = selectedSuccessRows.value.map((row) => row.id).filter(Boolean)
  if (!itemIds.length) {
    proxy.$modal.msgWarning('请先勾选可标记成功的号码')
    return
  }
  proxy.$modal.confirm(`确认将选中的 ${itemIds.length} 条号码批量标记为处理完成？`).then(() => {
    batchSuccessSubmitting.value = true
    return batchMarkSuccess({ itemIds })
  }).then((res) => {
    const updatedCount = res?.data?.updatedCount ?? 0
    const skippedCount = res?.data?.skippedCount ?? 0
    proxy.$modal.msgSuccess(`批量标记成功 ${updatedCount} 条${skippedCount ? `，跳过 ${skippedCount} 条` : ''}`)
    selectedRows.value = []
    getList()
  }).catch(() => {}).finally(() => {
    batchSuccessSubmitting.value = false
  })
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
function buildExportRow(row) {
  return [
    row?.userName || '',
    row?.phone || '',
    verifyCodeText(row),
    submitRemarkText(row),
    processStatusLabel(row?.processStatus, row),
    row?.orderNo || '',
    formatAgentDateTime(row?.createTime),
    processedTimeText(row)
  ]
}
async function fetchAllExportRows() {
  const pageSize = 500
  let pageNum = 1
  let totalCount = 0
  const rows = []
  do {
    const res = await listMarkAgentOrderItem(buildQueryParams({ pageNum, pageSize }))
    const batch = res?.rows || []
    totalCount = res?.total || 0
    rows.push(...batch)
    if (batch.length === 0) break
    pageNum += 1
  } while (rows.length < totalCount)
  return rows
}
function resolveExportFilename() {
  const title = String(props.pageTitle || '').trim()
  const prefix = title || '代理处理明细'
  return `${prefix}_${Date.now()}.csv`
}
async function handleExport() {
  if (exportLoading.value) return
  exportLoading.value = true
  try {
    const rows = await fetchAllExportRows()
    if (!rows.length) {
      proxy.$modal.msgWarning('当前筛选条件下没有可导出数据')
      return
    }
    const header = ['提交用户', '手机/固话', '验证码', '备注', '处理状态', '批次编号', '创建时间', '处理时间']
    downloadCsv(resolveExportFilename(), [header, ...rows.map((row) => buildExportRow(row))])
    proxy.$modal.msgSuccess(`已导出 ${rows.length} 条记录`)
  } catch (error) {
    proxy.$modal.msgError('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}
function resetQuery() {
  queryParams.phone = null
  queryParams.processStatus = props.defaultProcessStatus
  deadlineDate.value = ''
  handleQuery()
}
function handleSupplyChange(key) {
  const tab = props.supplyTabs.find((item) => item.key === key) || props.supplyTabs[0]
  queryParams.platformCodes = tab?.platformCodes || ''
  handleQuery()
}
function handleActionChange(row, processStatus) {
  const currentStatus = String(row?.processStatus || '0')
  if (!processStatus || processStatus === currentStatus) {
    resetRowAction(row)
    return
  }
  submitFeedback(row, processStatus)
}
function submitFeedback(row, processStatus) {
  const itemId = row?.id
  if (!itemId) return
  const currentStatus = String(row?.processStatus || '0')
  if (currentStatus === processStatus) {
    proxy.$modal.msgWarning('当前已是该状态，无需重复操作')
    resetRowAction(row)
    return
  }
  const processResult = processStatus === '1' ? 'success' : 'failed'
  let confirmText = ''
  if (currentStatus === '0' || currentStatus === '3') {
    confirmText = processStatus === '1'
      ? '确认将该号码标记成功?'
      : '确认将该号码标记失败？失败将自动退回次数'
  } else if (processStatus === '2') {
    confirmText = '确认将已处理订单改为失败？将自动退回次数'
  } else {
    confirmText = '确认将失败订单改为成功？如已退回次数将重新扣费'
  }
  proxy.$modal.confirm(confirmText).then(() => {
    submittingId.value = itemId
    return feedbackMarkOrderItem(itemId, {
      processStatus,
      processResult: processResult
    })
  }).then(() => {
    let tip = '操作成功'
    if (processStatus === '2') tip = '操作成功，已自动退回次数'
    if (currentStatus === '2' && processStatus === '1') tip = '操作成功，已重新扣费'
    proxy.$modal.msgSuccess(tip)
    getList()
  }).catch(() => {
    resetRowAction(row)
  }).finally(() => {
    submittingId.value = null
  })
}
function applyPlatformFilter() {
  queryParams.platformCodes = resolvePlatformCodes()
}

watch(() => props.platformCode, () => {
  applyPlatformFilter()
  handleQuery()
})

onMounted(() => {
  applyPlatformFilter()
  getList()
})

onBeforeUnmount(() => {
  clearTencentAutoRefresh()
  clearTdGaopinAutoRefresh()
})
</script>

<style scoped>
.mark-agent-process-page {
  padding: 0;
  margin: 0;
  width: 100%;
  font-size: 13px;
}
.supply-tabs {
  margin-bottom: 6px;
}
.supply-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
.search-card,
.table-card {
  margin-bottom: 8px;
  border-radius: 0;
  border-left: 0;
  border-right: 0;
}
.search-card :deep(.el-card__body) {
  padding: 10px 12px;
}
.search-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.search-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.toolbar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.toolbar-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}
.auto-detect-alert {
  margin: 0;
}
.auto-detect-alert :deep(.el-alert__title) {
  font-size: 12px;
  line-height: 1.5;
}
.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 4px 0;
}
.search-form :deep(.el-form-item) {
  margin-right: 12px;
  margin-bottom: 0;
}
.search-form :deep(.el-form-item__label) {
  font-size: 12px;
  padding-right: 6px;
}
.field-phone {
  width: 132px;
}
.field-status {
  width: 120px;
}
.field-date {
  width: 140px;
}
.search-actions :deep(.el-form-item__content) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.batch-action-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 16px;
  padding-top: 8px;
  border-top: 1px dashed var(--el-border-color-lighter);
}
.batch-action-bar__hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.batch-action-bar__hint strong {
  color: var(--el-color-warning);
  font-size: 14px;
  margin: 0 2px;
}
.table-card :deep(.el-card__body) {
  padding: 0 0 4px;
}
.process-table {
  width: 100%;
}
.process-table :deep(.el-table__cell) {
  padding: 6px 0;
  font-size: 12px;
}
.process-table :deep(.el-table__header .cell) {
  white-space: nowrap;
  font-size: 12px;
}
.table-card :deep(.pagination-container) {
  margin-top: 8px;
  padding: 0 8px 4px;
  justify-content: flex-end;
}
.action-cell {
  display: inline-flex;
  align-items: center;
  flex-wrap: nowrap;
  justify-content: flex-start;
  gap: 4px;
}
.action-select {
  width: 118px;
}
.processed-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.auto-detect-tip {
  color: var(--el-color-primary);
  font-size: 11px;
  white-space: nowrap;
}
</style>
