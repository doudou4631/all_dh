<template>
  <div class="app-container mark-agent-process-detail-page">
    <el-card shadow="never" class="overview-card">
      <div class="overview-head">
        <div>
          <h3 class="overview-title">已处理订单总览</h3>
          <p class="overview-tip">显示所有下级用户已处理完成的订单明细</p>
        </div>
      </div>
      <div class="overview-stats">
        <div class="overview-stat" :class="{ 'is-active': activeStatus === '' }" @click="filterByStatus('')">
          <span class="overview-stat__label">已处理总数</span>
          <span class="overview-stat__value">{{ summary.total }}</span>
        </div>
        <div class="overview-stat overview-stat--success" :class="{ 'is-active': activeStatus === '1' }" @click="filterByStatus('1')">
          <span class="overview-stat__label">处理完成</span>
          <span class="overview-stat__value">{{ summary.success }}</span>
        </div>
        <div class="overview-stat overview-stat--failed" :class="{ 'is-active': activeStatus === '2' }" @click="filterByStatus('2')">
          <span class="overview-stat__label">处理失败</span>
          <span class="overview-stat__value">{{ summary.failed }}</span>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="search-card">
      <div class="search-panel">
        <el-form
          :model="queryParams"
          :inline="true"
          size="default"
          label-width="72px"
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
          <el-form-item label="平台">
            <el-select
              v-model="queryParams.platformCode"
              clearable
              filterable
              placeholder="全部"
              class="field-platform"
            >
              <el-option
                v-for="item in platformOptions"
                :key="item.platformCode"
                :label="item.platformName"
                :value="item.platformCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="截止日期">
            <AgentPastDateRangePicker v-model="dateRange" placeholder="请选择日期" />
          </el-form-item>
        </el-form>
        <div class="search-panel__bar">
          <div class="search-panel__actions">
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            <el-button icon="RefreshRight" @click="refreshAll">刷新</el-button>
            <el-button icon="Download" :loading="exportLoading" @click="handleExport">导出</el-button>
          </div>
          <span v-if="total > 0" class="search-panel__meta">当前列表共 {{ total }} 条记录</span>
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
      >
        <el-table-column label="提交用户" prop="userName" width="100" align="center" show-overflow-tooltip />
        <el-table-column label="处理人" prop="processedBy" width="100" align="center" show-overflow-tooltip />
        <el-table-column label="平台" prop="platformName" min-width="96" align="center" show-overflow-tooltip />
        <el-table-column label="手机/固话" prop="phone" min-width="118" align="center" show-overflow-tooltip />
        <el-table-column label="验证码" min-width="88" align="center" show-overflow-tooltip>
          <template #default="scope">{{ verifyCodeText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="100" align="center" show-overflow-tooltip>
          <template #default="scope">{{ submitRemarkText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="处理状态" width="100" align="center">
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
        <template #empty>
          <el-empty description="暂无已处理记录" :image-size="72" />
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

<script setup name="MarkAgentProcessDetail">
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue'
import { listMarkAgentOrderItem, listMarkAgentPlatformOptions } from '@/api/server/markAgent'
import AgentPastDateRangePicker from '../components/AgentPastDateRangePicker.vue'
import {
  MARK_ITEM_PROCESS_STATUS_OPTIONS,
  markItemProcessStatusLabel,
  markItemProcessStatusTagType,
  buildMarkItemProcessStatusQuery
} from '@/utils/markProcessStatus'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const exportLoading = ref(false)
const total = ref(0)
const itemList = ref([])
const dateRange = ref([])
const platformOptions = ref([])
const summary = reactive({
  total: 0,
  success: 0,
  failed: 0
})
const queryParams = reactive({
  pageNum: 1,
  pageSize: 90,
  phone: null,
  processStatus: null,
  platformCode: null,
  params: {}
})
const processStatusOptions = MARK_ITEM_PROCESS_STATUS_OPTIONS
const activeStatus = computed(() => String(queryParams.processStatus || ''))

const processStatusLabel = markItemProcessStatusLabel
const processStatusTagType = markItemProcessStatusTagType
function verifyCodeText(row) {
  const result = String(row?.processResult || '').trim()
  if (result) return result
  const remark = String(row?.remark || '').trim()
  if (/^\d{6}$/.test(remark)) return remark
  if (row?.processStatus === '1') return 'success'
  return '-'
}
function submitRemarkText(row) {
  const itemRemark = String(row?.remark || '').trim()
  if (itemRemark) return itemRemark
  const orderRemark = String(row?.orderRemark || '').trim()
  return orderRemark || '-'
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
  const [beginTime, endTime] = dateRange.value || []
  if (beginTime) params.params.beginTime = beginTime
  if (endTime) params.params.endTime = endTime
  return params
}
function buildSummaryParams(processStatus = null, extraParams = {}) {
  return {
    pageNum: 1,
    pageSize: 1,
    processStatus,
    params: { ...extraParams }
  }
}
async function loadSummary() {
  const [allRes, okRes, failRes] = await Promise.all([
    listMarkAgentOrderItem(buildSummaryParams()),
    listMarkAgentOrderItem(buildSummaryParams('1')),
    listMarkAgentOrderItem(buildSummaryParams('2'))
  ])
  summary.total = allRes?.total || 0
  summary.success = okRes?.total || 0
  summary.failed = failRes?.total || 0
}
function getList() {
  loading.value = true
  return listMarkAgentOrderItem(buildQueryParams())
    .then((res) => {
      itemList.value = res.rows || []
      total.value = res.total || 0
    })
    .finally(() => {
      loading.value = false
    })
}
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}
function resetQuery() {
  queryParams.phone = null
  queryParams.processStatus = null
  queryParams.platformCode = null
  dateRange.value = []
  handleQuery()
}
function loadPlatformOptions() {
  return listMarkAgentPlatformOptions()
    .then((resp) => {
      platformOptions.value = Array.isArray(resp?.data) ? resp.data : []
    })
    .catch(() => {
      platformOptions.value = []
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
    row?.processedBy || '',
    row?.platformName || '',
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
async function handleExport() {
  if (exportLoading.value) return
  exportLoading.value = true
  try {
    const rows = await fetchAllExportRows()
    if (!rows.length) {
      proxy.$modal.msgWarning('当前筛选条件下没有可导出数据')
      return
    }
    const header = ['提交用户', '处理人', '平台', '手机/固话', '验证码', '备注', '处理状态', '批次编号', '创建时间', '处理时间']
    downloadCsv(`处理总览_${Date.now()}.csv`, [header, ...rows.map((row) => buildExportRow(row))])
    proxy.$modal.msgSuccess(`已导出 ${rows.length} 条记录`)
  } catch (error) {
    proxy.$modal.msgError('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}
function filterByStatus(status) {
  queryParams.processStatus = status || null
  handleQuery()
}
async function refreshAll() {
  await Promise.all([loadSummary(), getList()])
}

onMounted(() => {
  loadPlatformOptions()
  refreshAll()
})
</script>

<style scoped lang="scss">
.mark-agent-process-detail-page {
  .overview-card {
    margin-bottom: 10px;
  }

  .overview-head {
    margin-bottom: 12px;
  }

  .overview-title {
    margin: 0 0 4px;
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .overview-tip {
    margin: 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .overview-stats {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
  }

  .overview-stat {
    padding: 12px 14px;
    border-radius: 8px;
    background: var(--el-fill-color-light);
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover,
    &.is-active {
      background: #ecf5ff;
      box-shadow: inset 0 0 0 1px #d9ecff;
    }
  }

  .overview-stat--success.is-active {
    background: #f0f9eb;
    box-shadow: inset 0 0 0 1px #e1f3d8;
  }

  .overview-stat--failed.is-active {
    background: #fef0f0;
    box-shadow: inset 0 0 0 1px #fde2e2;
  }

  .overview-stat__label {
    display: block;
    margin-bottom: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .overview-stat__value {
    font-size: 24px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .overview-stat--success .overview-stat__value {
    color: var(--el-color-success);
  }

  .overview-stat--failed .overview-stat__value {
    color: var(--el-color-danger);
  }

  .search-card {
    margin-bottom: 10px;

    :deep(.el-card__body) {
      padding: 14px 16px 12px;
    }
  }

  .search-panel {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .search-form {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 4px 0;

    :deep(.el-form-item) {
      margin-right: 16px;
      margin-bottom: 0;
    }

    :deep(.el-form-item__label) {
      white-space: nowrap;
      font-size: 13px;
      color: var(--el-text-color-regular);
      padding-right: 8px;
    }
  }

  .search-panel__bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding-top: 12px;
    border-top: 1px solid var(--el-border-color-lighter);
  }

  .search-panel__actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
  }

  .search-panel__meta {
    flex-shrink: 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }

  .field-phone {
    width: 168px;
  }

  .field-status {
    width: 148px;
  }

  .field-platform {
    width: 168px;
  }
}

@media (max-width: 768px) {
  .mark-agent-process-detail-page {
    .overview-stats {
      grid-template-columns: 1fr;
    }

    .search-panel__bar {
      flex-direction: column;
      align-items: flex-start;
    }
  }
}
</style>
