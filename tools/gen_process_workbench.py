from pathlib import Path

content = r'''<template>
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
          <span v-if="total > 0" class="toolbar-count">\u5171 {{ total }} \u6761\u8bb0\u5f55</span>
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
          <el-form-item label="\u53f7\u7801">
            <el-input
              v-model="queryParams.phone"
              placeholder="\u8bf7\u8f93\u5165\u53f7\u7801"
              clearable
              class="field-phone"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="\u5904\u7406\u72b6\u6001">
            <el-select v-model="queryParams.processStatus" clearable placeholder="\u5168\u90e8" class="field-status">
              <el-option v-for="item in processStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="\u622a\u6b62\u65e5\u671f">
            <el-date-picker
              v-model="deadlineDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="\u8bf7\u9009\u62e9\u65e5\u671f"
              clearable
              class="field-date"
            />
          </el-form-item>
          <el-form-item class="search-actions">
            <el-button type="primary" icon="Search" @click="handleQuery">\u641c\u7d22</el-button>
            <el-button icon="RefreshLeft" @click="resetQuery">\u91cd\u7f6e</el-button>
            <el-button icon="RefreshRight" @click="getList">\u5237\u65b0</el-button>
          </el-form-item>
        </el-form>

        <div class="batch-action-bar">
          <span v-if="isXiaomiWorkbench" class="batch-action-bar__hint">
            \u5df2\u9009\u62e9 <strong>{{ selectedRows.length }}</strong> \u6761\u53f7\u7801
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
            \u6279\u91cf\u5904\u7406
          </el-button>
          <el-button
            v-if="isXiaomiWorkbench"
            type="primary"
            plain
            :disabled="!selectedDetectRows.length || batchDetecting"
            :loading="batchDetecting"
            v-hasPermi="['server:markAgent:item:feedback']"
            @click="handleBatchDetect"
          >
            \u6279\u91cf\u68c0\u6d4b
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
            \u6279\u91cf\u6210\u529f
          </el-button>
          <el-button
            icon="Download"
            :loading="exportLoading"
            v-hasPermi="['server:markAgent:order:list']"
            @click="handleExport"
          >
            \u5bfc\u51fa
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
        <el-table-column label="\u63d0\u4ea4\u7528\u6237" prop="userName" width="96" align="center" show-overflow-tooltip />
        <el-table-column label="\u624b\u673a/\u56fa\u8bdd" prop="phone" min-width="118" align="center" show-overflow-tooltip />
        <el-table-column label="\u9a8c\u8bc1\u7801" min-width="88" align="center" show-overflow-tooltip>
          <template #default="scope">{{ verifyCodeText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="\u5907\u6ce8" min-width="100" align="center" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.processStatus === '0' && isTdGaopinPlatform(scope.row)" class="auto-detect-tip">\u81ea\u52a8\u68c0\u6d4b</span>
            <span v-else>{{ submitRemarkText(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="\u5904\u7406\u72b6\u6001" width="96" align="center">
          <template #default="scope">
            <el-tag :type="processStatusTagType(scope.row.processStatus, scope.row)" size="small" effect="light">
              {{ processStatusLabel(scope.row.processStatus, scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="\u6279\u6b21\u7f16\u53f7" prop="orderNo" min-width="180" show-overflow-tooltip align="center" />
        <el-table-column label="\u521b\u5efa\u65f6\u95f4" width="152" align="center" show-overflow-tooltip>
          <template #default="scope">{{ formatAgentDateTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="\u5904\u7406\u65f6\u95f4" width="152" align="center" show-overflow-tooltip>
          <template #default="scope">{{ processedTimeText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="\u64cd\u4f5c" width="130" align="center" fixed="right">
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
            <span v-else-if="scope.row.processStatus === '0' && isTencentAutoPlatform(scope.row)" class="processed-tip">\u540e\u53f0\u5904\u7406\u4e2d</span>
            <span v-else-if="scope.row.processStatus === '3'" class="processed-tip">\u81ea\u52a8\u68c0\u6d4b\u4e2d</span>
            <span v-else class="processed-tip">\u5df2\u5904\u7406</span>
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
import { listMarkAgentOrderItem, feedbackMarkOrderItem, batchProcessXiaomi, batchDetectXiaomi, batchMarkSuccess } from '@/api/server/markAgent'
import {
  MARK_ITEM_PROCESS_STATUS_OPTIONS,
  MARK_ITEM_FEEDBACK_OPTIONS,
  buildMarkItemProcessStatusQuery,
  markItemProcessStatusLabel,
  markItemProcessStatusTagType,
  isXiaomiPlatform as isXiaomiPlatformRow,
  canBatchProcessXiaomi,
  canBatchDetectXiaomi,
  canBatchSuccessXiaomi,
  canSelectXiaomiBatchRow,
  XIAOMI_PLATFORM_CODE
} from '@/utils/markProcessStatus'

const props = defineProps({
  platformCode: { type: String, default: '' },
  pageTitle: { type: String, default: '' },
  supplyTabs: { type: Array, default: () => ([
    { key: 'tdx', label: '\u4f9b\u5e94(TDX\u6cf0\u8fea\u9891)', platformCodes: 'mobile_gaopin,td_gaopin,td_second' },
    { key: 'qihu', label: '\u4f9b\u5e94(360\u5947\u864e)', platformCodes: 'qihu_first,qihu_second' }
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
const batchDetecting = ref(false)
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
const processStatusOptions = computed(() => {
  if (isXiaomiWorkbench.value) {
    return MARK_ITEM_PROCESS_STATUS_OPTIONS.filter((item) => item.value !== 'processing')
  }
  return [
    { label: '\u5f85\u5904\u7406', value: '0' },
    { label: '\u5904\u7406\u6210\u529f', value: '1' },
    { label: '\u5904\u7406\u5931\u8d25', value: '2' }
  ]
})
const emptyDescription = '\u6682\u65e0\u8ba2\u5355\u6570\u636e'
const rowFeedbackOptions = MARK_ITEM_FEEDBACK_OPTIONS
const processStatusLabel = markItemProcessStatusLabel
const processStatusTagType = markItemProcessStatusTagType
const selectedProcessRows = computed(() => (selectedRows.value || []).filter((row) => canBatchProcessXiaomi(row)))
const selectedDetectRows = computed(() => (selectedRows.value || []).filter((row) => canBatchDetectXiaomi(row)))
const selectedSuccessRows = computed(() => (selectedRows.value || []).filter((row) => canBatchSuccessXiaomi(row)))

function isXiaomiPlatform(row) {
  return isXiaomiPlatformRow(row) || isXiaomiWorkbench.value
}
function verifyCodeText(row) {
  const result = String(row?.processResult || '').trim()
  if (result) return result
  const remark = String(row?.remark || '').trim()
  if (/^\\d{6}$/.test(remark)) return remark
  if (row?.processStatus === '1') return 'success'
  return '-'
}
function isTencentAutoPlatform(row) {
  const code = String(row?.platformCode || props.platformCode || '').trim().toLowerCase()
  return ['tencent_mark', 'tencent', 'tx', 'txwz'].includes(code)
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
  return canSelectXiaomiBatchRow(row)
}
function handleSelectionChange(rows) {
  selectedRows.value = rows || []
}
function hasXiaomiProcessingItems(items) {
  return (items || []).some((row) => isXiaomiPlatform(row) && String(row?.processStatus || '0') === '3')
}
function shouldRunXiaomiAutoRefresh() {
  if (!isXiaomiWorkbench.value) return false
  if (!hasXiaomiProcessingItems(itemList.value)) return false
  const status = queryParams.processStatus
  if (status === '1' || status === '2') return false
  return true
}
const xiaomiAutoDetecting = computed(() => shouldRunXiaomiAutoRefresh())
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
    return '\u817e\u8baf\u8ba2\u5355\u63d0\u4ea4\u540e\u7ea630\u79d2\u81ea\u52a8\u5904\u7406\uff0c\u672c\u9875\u9762\u6bcf30\u79d2\u5237\u65b0\u6210\u529f/\u5931\u8d25\u72b6\u6001'
  }
  if (tdGaopinAutoDetecting.value) {
    return '\u6cf0\u8fea\u9ad8\u9891\u540e\u53f0\u6bcf30\u79d2\u81ea\u52a8\u68c0\u6d4b\uff0c\u672c\u9875\u9762\u540c\u6b65\u5237\u65b0\u72b6\u6001'
  }
  if (xiaomiAutoDetecting.value) {
    return '\u5df2\u5f00\u542f\u81ea\u52a8\u68c0\u6d4b\uff0c\u6bcf30\u79d2\u67e5\u8be2\u53f7\u7801\u72b6\u6001\uff1b\u65e0\u6807\u8bb0\u81ea\u52a8\u6210\u529f\uff0c\u6709\u6807\u8bb0\u4fdd\u6301\u5904\u7406\u4e2d'
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
  const params = isXiaomiWorkbench.value
    ? buildMarkItemProcessStatusQuery(raw)
    : { ...raw, params: {} }
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
  if (status === '0' || status === '3') return '\u8bf7\u9009\u62e9\u64cd\u4f5c'
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
      setupXiaomiAutoRefresh()
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
let xiaomiRefreshTimer = null
function clearXiaomiAutoRefresh() {
  if (xiaomiRefreshTimer) {
    clearInterval(xiaomiRefreshTimer)
    xiaomiRefreshTimer = null
  }
}
function setupXiaomiAutoRefresh() {
  clearXiaomiAutoRefresh()
  if (!shouldRunXiaomiAutoRefresh()) return
  xiaomiRefreshTimer = setInterval(() => {
    refreshItemList().then(() => {
      if (!shouldRunXiaomiAutoRefresh()) {
        clearXiaomiAutoRefresh()
      }
    })
  }, 30000)
}
function handleBatchMarkSubmitted() {
  const itemIds = selectedProcessRows.value.map((row) => row.id).filter(Boolean)
  if (!itemIds.length) {
    proxy.$modal.msgWarning('\u8bf7\u5148\u52fe\u9009\u5f85\u5904\u7406\u53f7\u7801')
    return
  }
  proxy.$modal.confirm(`\u786e\u8ba4\u5bf9\u9009\u4e2d\u7684 ${itemIds.length} \u6761\u53f7\u7801\u5f00\u542f\u6279\u91cf\u5904\u7406\uff1f\u5f00\u542f\u540e\u5c06\u81ea\u52a8\u68c0\u6d4b\u53f7\u7801\u72b6\u6001\u3002`).then(() => {
    batchSubmitting.value = true
    return batchProcessXiaomi({ itemIds })
  }).then((res) => {
    const updatedCount = res?.data?.updatedCount ?? 0
    const skippedCount = res?.data?.skippedCount ?? 0
    proxy.$modal.msgSuccess(`\u5df2\u5f00\u542f\u6279\u91cf\u5904\u7406 ${updatedCount} \u6761${skippedCount ? `\uff0c\u8df3\u8fc7 ${skippedCount} \u6761` : ''}`)
    selectedRows.value = []
    getList()
  }).catch(() => {}).finally(() => {
    batchSubmitting.value = false
  })
}
function handleBatchDetect() {
  const itemIds = selectedDetectRows.value.map((row) => row.id).filter(Boolean)
  if (!itemIds.length) {
    proxy.$modal.msgWarning('\u8bf7\u5148\u52fe\u9009\u5904\u7406\u4e2d\u7684\u53f7\u7801')
    return
  }
  proxy.$modal.confirm(`\u786e\u8ba4\u5bf9\u9009\u4e2d\u7684 ${itemIds.length} \u6761\u53f7\u7801\u6267\u884c\u6279\u91cf\u68c0\u6d4b\uff1f\u5c06\u7acb\u5373\u67e5\u8be2\u6807\u8bb0\u72b6\u6001\u3002`).then(() => {
    batchDetecting.value = true
    return batchDetectXiaomi({ itemIds })
  }).then((res) => {
    const detectedCount = res?.data?.detectedCount ?? 0
    const successCount = res?.data?.successCount ?? 0
    const stillMarkedCount = res?.data?.stillMarkedCount ?? 0
    const skippedCount = res?.data?.skippedCount ?? 0
    proxy.$modal.msgSuccess(`\u6279\u91cf\u68c0\u6d4b\u5b8c\u6210\uff1a\u68c0\u6d4b ${detectedCount} \u6761\uff0c\u6210\u529f ${successCount} \u6761\uff0c\u4ecd\u6709\u6807\u8bb0 ${stillMarkedCount} \u6761${skippedCount ? `\uff0c\u8df3\u8fc7 ${skippedCount} \u6761` : ''}`)
    selectedRows.value = []
    getList()
  }).catch(() => {}).finally(() => {
    batchDetecting.value = false
  })
}
function handleBatchSuccess() {
  const itemIds = selectedSuccessRows.value.map((row) => row.id).filter(Boolean)
  if (!itemIds.length) {
    proxy.$modal.msgWarning('\u8bf7\u5148\u52fe\u9009\u53ef\u6807\u8bb0\u6210\u529f\u7684\u53f7\u7801')
    return
  }
  proxy.$modal.confirm(`\u786e\u8ba4\u5c06\u9009\u4e2d\u7684 ${itemIds.length} \u6761\u53f7\u7801\u6279\u91cf\u6807\u8bb0\u4e3a\u5904\u7406\u6210\u529f\uff1f`).then(() => {
    batchSuccessSubmitting.value = true
    return batchMarkSuccess({ itemIds })
  }).then((res) => {
    const updatedCount = res?.data?.updatedCount ?? 0
    const skippedCount = res?.data?.skippedCount ?? 0
    proxy.$modal.msgSuccess(`\u6279\u91cf\u6807\u8bb0\u6210\u529f ${updatedCount} \u6761${skippedCount ? `\uff0c\u8df3\u8fc7 ${skippedCount} \u6761` : ''}`)
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
  const csvText = `\\ufeff${rows.map((row) => row.map((cell) => buildCsvCell(cell)).join(',')).join('\\n')}`
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
  const prefix = title || '\u4ee3\u7406\u5904\u7406\u660e\u7ec6'
  return `${prefix}_${Date.now()}.csv`
}
async function handleExport() {
  if (exportLoading.value) return
  exportLoading.value = true
  try {
    const rows = await fetchAllExportRows()
    if (!rows.length) {
      proxy.$modal.msgWarning('\u5f53\u524d\u7b5b\u9009\u6761\u4ef6\u4e0b\u6ca1\u6709\u53ef\u5bfc\u51fa\u6570\u636e')
      return
    }
    const header = ['\u63d0\u4ea4\u7528\u6237', '\u624b\u673a/\u56fa\u8bdd', '\u9a8c\u8bc1\u7801', '\u5907\u6ce8', '\u5904\u7406\u72b6\u6001', '\u6279\u6b21\u7f16\u53f7', '\u521b\u5efa\u65f6\u95f4', '\u5904\u7406\u65f6\u95f4']
    downloadCsv(resolveExportFilename(), [header, ...rows.map((row) => buildExportRow(row))])
    proxy.$modal.msgSuccess(`\u5df2\u5bfc\u51fa ${rows.length} \u6761\u8bb0\u5f55`)
  } catch (error) {
    proxy.$modal.msgError('\u5bfc\u51fa\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5')
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
    proxy.$modal.msgWarning('\u5f53\u524d\u5df2\u662f\u8be5\u72b6\u6001\uff0c\u65e0\u9700\u91cd\u590d\u64cd\u4f5c')
    resetRowAction(row)
    return
  }
  const processResult = processStatus === '1' ? 'success' : 'failed'
  let confirmText = ''
  if (currentStatus === '0' || currentStatus === '3') {
    confirmText = processStatus === '1'
      ? '\u786e\u8ba4\u5c06\u8be5\u53f7\u7801\u6807\u8bb0\u6210\u529f?'
      : '\u786e\u8ba4\u5c06\u8be5\u53f7\u7801\u6807\u8bb0\u5931\u8d25\uff1f\u5931\u8d25\u5c06\u81ea\u52a8\u9000\u56de\u6b21\u6570'
  } else if (processStatus === '2') {
    confirmText = '\u786e\u8ba4\u5c06\u5df2\u5904\u7406\u8ba2\u5355\u6539\u4e3a\u5931\u8d25\uff1f\u5c06\u81ea\u52a8\u9000\u56de\u6b21\u6570'
  } else {
    confirmText = '\u786e\u8ba4\u5c06\u5931\u8d25\u8ba2\u5355\u6539\u4e3a\u6210\u529f\uff1f\u5982\u5df2\u9000\u56de\u6b21\u6570\u5c06\u91cd\u65b0\u6263\u8d39'
  }
  proxy.$modal.confirm(confirmText).then(() => {
    submittingId.value = itemId
    return feedbackMarkOrderItem(itemId, {
      processStatus,
      processResult: processResult
    })
  }).then(() => {
    let tip = '\u64cd\u4f5c\u6210\u529f'
    if (processStatus === '2') tip = '\u64cd\u4f5c\u6210\u529f\uff0c\u5df2\u81ea\u52a8\u9000\u56de\u6b21\u6570'
    if (currentStatus === '2' && processStatus === '1') tip = '\u64cd\u4f5c\u6210\u529f\uff0c\u5df2\u91cd\u65b0\u6263\u8d39'
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
  clearXiaomiAutoRefresh()
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
'''

out = Path(r"c:\Users\Administrator\Desktop\1500\frontend\src\views\server\mark\agent\components\ProcessWorkbench.vue")
out.write_text(content.encode('utf-8').decode('unicode_escape'), encoding='utf-8')
print('written', out)
