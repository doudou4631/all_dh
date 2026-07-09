# -*- coding: utf-8 -*-
"""Regenerate agent processed-order overview page with UTF-8 Chinese."""
from pathlib import Path


def zh(*codes: int) -> str:
    return "".join(chr(c) for c in codes)


T = {
    "overview_title": zh(0x5DF2, 0x5904, 0x7406, 0x8BA2, 0x5355, 0x603B, 0x89C8),
    "overview_tip": zh(0x663E, 0x793A, 0x6240, 0x6709, 0x4E0B, 0x7EA7, 0x7528, 0x6237, 0x5DF2, 0x5904, 0x7406, 0x5B8C, 0x6210, 0x7684, 0x8BA2, 0x5355, 0x660E, 0x7EC6),
    "stat_total": zh(0x5DF2, 0x5904, 0x7406, 0x603B, 0x6570),
    "stat_success": zh(0x5904, 0x7406, 0x5B8C, 0x6210),
    "stat_failed": zh(0x5904, 0x7406, 0x5931, 0x8D25),
    "pending": zh(0x5F85, 0x5904, 0x7406),
    "processing": zh(0x5904, 0x7406, 0x4E2D),
    "completed": zh(0x5904, 0x7406, 0x5B8C, 0x6210),
    "phone": zh(0x53F7, 0x7801),
    "phone_ph": zh(0x8BF7, 0x8F93, 0x5165, 0x53F7, 0x7801),
    "status": zh(0x5904, 0x7406, 0x72B6, 0x6001),
    "all": zh(0x5168, 0x90E8),
    "success": zh(0x5904, 0x7406, 0x6210, 0x529F),
    "failed": zh(0x5904, 0x7406, 0x5931, 0x8D25),
    "platform": zh(0x5E73, 0x53F0),
    "platform_ph": zh(0x5E73, 0x53F0, 0x7F16, 0x7801),
    "end_date": zh(0x622A, 0x6B62, 0x65E5, 0x671F),
    "end_date_ph": zh(0x8BF7, 0x9009, 0x62E9, 0x65E5, 0x671F),
    "search": zh(0x641C, 0x7D22),
    "reset": zh(0x91CD, 0x7F6E),
    "refresh": zh(0x5237, 0x65B0),
    "export": zh(0x5BFC, 0x51FA),
    "list_total": zh(0x5F53, 0x524D, 0x5217, 0x8868, 0x5171),
    "list_suffix": zh(0x6761, 0x8BB0, 0x5F55),
    "submit_user": zh(0x63D0, 0x4EA4, 0x7528, 0x6237),
    "processor": zh(0x5904, 0x7406, 0x4EBA),
    "phone_col": zh(0x624B, 0x673A, 0x2F, 0x56FA, 0x8BDD),
    "verify_code": zh(0x9A8C, 0x8BC1, 0x7801),
    "remark": zh(0x5907, 0x6CE8),
    "batch_no": zh(0x6279, 0x6B21, 0x7F16, 0x53F7),
    "create_time": zh(0x521B, 0x5EFA, 0x65F6, 0x95F4),
    "process_time": zh(0x5904, 0x7406, 0x65F6, 0x95F4),
    "empty": zh(0x6682, 0x65E0, 0x5DF2, 0x5904, 0x7406, 0x8BB0, 0x5F55),
}

content = f"""<template>
  <div class="app-container mark-agent-process-detail-page">
    <el-card shadow="never" class="overview-card">
      <div class="overview-head">
        <div>
          <h3 class="overview-title">{T['overview_title']}</h3>
          <p class="overview-tip">{T['overview_tip']}</p>
        </div>
      </div>
      <div class="overview-stats">
        <div class="overview-stat" :class="{{ 'is-active': activeStatus === '' }}" @click="filterByStatus('')">
          <span class="overview-stat__label">{T['stat_total']}</span>
          <span class="overview-stat__value">{{{{ summary.total }}}}</span>
        </div>
        <div class="overview-stat overview-stat--success" :class="{{ 'is-active': activeStatus === '1' }}" @click="filterByStatus('1')">
          <span class="overview-stat__label">{T['stat_success']}</span>
          <span class="overview-stat__value">{{{{ summary.success }}}}</span>
        </div>
        <div class="overview-stat overview-stat--failed" :class="{{ 'is-active': activeStatus === '2' }}" @click="filterByStatus('2')">
          <span class="overview-stat__label">{T['stat_failed']}</span>
          <span class="overview-stat__value">{{{{ summary.failed }}}}</span>
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
          <el-form-item label="{T['phone']}">
            <el-input
              v-model="queryParams.phone"
              placeholder="{T['phone_ph']}"
              clearable
              class="field-phone"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="{T['status']}">
            <el-select v-model="queryParams.processStatus" clearable placeholder="{T['all']}" class="field-status">
              <el-option v-for="item in processStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="{T['platform']}">
            <el-select
              v-model="queryParams.platformCode"
              clearable
              filterable
              placeholder="{T['all']}"
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
          <el-form-item label="{T['end_date']}">
            <AgentPastDateRangePicker v-model="dateRange" placeholder="{T['end_date_ph']}" />
          </el-form-item>
        </el-form>
        <div class="search-panel__bar">
          <div class="search-panel__actions">
            <el-button type="primary" icon="Search" @click="handleQuery">{T['search']}</el-button>
            <el-button icon="Refresh" @click="resetQuery">{T['reset']}</el-button>
            <el-button icon="RefreshRight" @click="refreshAll">{T['refresh']}</el-button>
            <el-button icon="Download" :loading="exportLoading" @click="handleExport">{T['export']}</el-button>
          </div>
          <span v-if="total > 0" class="search-panel__meta">{T['list_total']} {{{{ total }}}} {T['list_suffix']}</span>
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
        <el-table-column label="{T['submit_user']}" prop="userName" width="100" align="center" show-overflow-tooltip />
        <el-table-column label="{T['processor']}" prop="processedBy" width="100" align="center" show-overflow-tooltip />
        <el-table-column label="{T['platform']}" prop="platformName" min-width="96" align="center" show-overflow-tooltip />
        <el-table-column label="{T['phone_col']}" prop="phone" min-width="118" align="center" show-overflow-tooltip />
        <el-table-column label="{T['verify_code']}" min-width="88" align="center" show-overflow-tooltip>
          <template #default="scope">{{{{ verifyCodeText(scope.row) }}}}</template>
        </el-table-column>
        <el-table-column label="{T['remark']}" min-width="100" align="center" show-overflow-tooltip>
          <template #default="scope">{{{{ submitRemarkText(scope.row) }}}}</template>
        </el-table-column>
        <el-table-column label="{T['status']}" width="100" align="center">
          <template #default="scope">
            <el-tag :type="processStatusTagType(scope.row.processStatus, scope.row)" size="small" effect="light">
              {{{{ processStatusLabel(scope.row.processStatus, scope.row) }}}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="{T['batch_no']}" prop="orderNo" min-width="180" show-overflow-tooltip align="center" />
        <el-table-column label="{T['create_time']}" width="152" align="center" show-overflow-tooltip>
          <template #default="scope">{{{{ formatAgentDateTime(scope.row.createTime) }}}}</template>
        </el-table-column>
        <el-table-column label="{T['process_time']}" width="152" align="center" show-overflow-tooltip>
          <template #default="scope">{{{{ processedTimeText(scope.row) }}}}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="{T['empty']}" :image-size="72" />
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

<script setup name="ProcessDetailOverview">
import {{ ref, reactive, computed, onMounted, getCurrentInstance }} from 'vue'
import {{ listMarkAgentOrderItem, listMarkAgentPlatformOptions }} from '@/api/server/markAgent'
import AgentPastDateRangePicker from './AgentPastDateRangePicker.vue'
import {{
  MARK_ITEM_PROCESS_STATUS_OPTIONS,
  markItemProcessStatusLabel,
  markItemProcessStatusTagType,
  buildMarkItemProcessStatusQuery
}} from '@/utils/markProcessStatus'

const {{ proxy }} = getCurrentInstance()
const loading = ref(false)
const exportLoading = ref(false)
const total = ref(0)
const itemList = ref([])
const dateRange = ref([])
const platformOptions = ref([])
const summary = reactive({{
  total: 0,
  success: 0,
  failed: 0
}})
const queryParams = reactive({{
  pageNum: 1,
  pageSize: 90,
  phone: null,
  processStatus: null,
  platformCode: null,
  params: {{}}
}})
const processStatusOptions = MARK_ITEM_PROCESS_STATUS_OPTIONS
const activeStatus = computed(() => String(queryParams.processStatus || ''))

const processStatusLabel = markItemProcessStatusLabel
const processStatusTagType = markItemProcessStatusTagType
function verifyCodeText(row) {{
  const result = String(row?.processResult || '').trim()
  if (result) return result
  const remark = String(row?.remark || '').trim()
  if (/^\\d{{6}}$/.test(remark)) return remark
  if (row?.processStatus === '1') return 'success'
  return '-'
}}
function submitRemarkText(row) {{
  const note = String(row?.processNote || '').trim()
  if (note) return note
  const remark = String(row?.orderRemark || '').trim()
  return remark || '-'
}}
function processedTimeText(row) {{
  if (!row || row.processStatus === '0') return '-'
  return formatAgentDateTime(row.processedTime) || '-'
}}
function formatAgentDateTime(value) {{
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${{d.getFullYear()}}/${{d.getMonth() + 1}}/${{d.getDate()}} ${{pad(d.getHours())}}:${{pad(d.getMinutes())}}:${{pad(d.getSeconds())}}`
}}
function buildQueryParams(overrides = {{}}) {{
  const raw = {{ ...queryParams, ...overrides }}
  const params = buildMarkItemProcessStatusQuery(raw)
  const [beginTime, endTime] = dateRange.value || []
  if (beginTime) params.params.beginTime = beginTime
  if (endTime) params.params.endTime = endTime
  return params
}}
function buildSummaryParams(processStatus = null, extraParams = {{}}) {{
  return {{
    pageNum: 1,
    pageSize: 1,
    processStatus,
    params: {{ ...extraParams }}
  }}
}}
async function loadSummary() {{
  const [allRes, okRes, failRes] = await Promise.all([
    listMarkAgentOrderItem(buildSummaryParams()),
    listMarkAgentOrderItem(buildSummaryParams('1')),
    listMarkAgentOrderItem(buildSummaryParams('2'))
  ])
  summary.total = allRes?.total || 0
  summary.success = okRes?.total || 0
  summary.failed = failRes?.total || 0
}}
function getList() {{
  loading.value = true
  return listMarkAgentOrderItem(buildQueryParams())
    .then((res) => {{
      itemList.value = res.rows || []
      total.value = res.total || 0
    }})
    .finally(() => {{
      loading.value = false
    }})
}}
function handleQuery() {{
  queryParams.pageNum = 1
  getList()
}}
function resetQuery() {{
  queryParams.phone = null
  queryParams.processStatus = null
  queryParams.platformCode = null
  dateRange.value = []
  handleQuery()
}}
function loadPlatformOptions() {{
  return listMarkAgentPlatformOptions()
    .then((resp) => {{
      platformOptions.value = Array.isArray(resp?.data) ? resp.data : []
    }})
    .catch(() => {{
      platformOptions.value = []
    }})
}}
function buildCsvCell(value) {{
  const text = String(value ?? '')
  return `"${{text.replace(/"/g, '""')}}"`
}}
function downloadCsv(filename, rows) {{
  const csvText = `\\ufeff${{rows.map((row) => row.map((cell) => buildCsvCell(cell)).join(',')).join('\\n')}}`
  const blob = new Blob([csvText], {{ type: 'text/csv;charset=utf-8;' }})
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}}
function buildExportRow(row) {{
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
}}
async function fetchAllExportRows() {{
  const pageSize = 500
  let pageNum = 1
  let totalCount = 0
  const rows = []
  do {{
    const res = await listMarkAgentOrderItem(buildQueryParams({{ pageNum, pageSize }}))
    const batch = res?.rows || []
    totalCount = res?.total || 0
    rows.push(...batch)
    if (batch.length === 0) break
    pageNum += 1
  }} while (rows.length < totalCount)
  return rows
}}
async function handleExport() {{
  if (exportLoading.value) return
  exportLoading.value = true
  try {{
    const rows = await fetchAllExportRows()
    if (!rows.length) {{
      proxy.$modal.msgWarning('{zh(0x5F53, 0x524D, 0x7B5B, 0x9009, 0x6761, 0x4EF6, 0x4E0B, 0x6CA1, 0x6709, 0x53EF, 0x5BFC, 0x51FA, 0x6570, 0x636E)}')
      return
    }}
    const header = ['{T["submit_user"]}', '{T["processor"]}', '{T["platform"]}', '{T["phone_col"]}', '{T["verify_code"]}', '{T["remark"]}', '{T["status"]}', '{T["batch_no"]}', '{T["create_time"]}', '{T["process_time"]}']
    downloadCsv(`{zh(0x5904, 0x7406, 0x603B, 0x89C8)}_${{Date.now()}}.csv`, [header, ...rows.map((row) => buildExportRow(row))])
    proxy.$modal.msgSuccess(`{zh(0x5DF2, 0x5BFC, 0x51FA)} ${{rows.length}} {T["list_suffix"]}`)
  }} catch (error) {{
    proxy.$modal.msgError('{zh(0x5BFC, 0x51FA, 0x5931, 0x8D25, 0xFF0C, 0x8BF7, 0x7A0D, 0x540E, 0x91CD, 0x8BD5)}')
  }} finally {{
    exportLoading.value = false
  }}
}}
function filterByStatus(status) {{
  queryParams.processStatus = status || null
  handleQuery()
}}
async function refreshAll() {{
  await Promise.all([loadSummary(), getList()])
}}

onMounted(() => {{
  loadPlatformOptions()
  refreshAll()
}})
</script>

<style scoped lang="scss">
.mark-agent-process-detail-page {{
  .overview-card {{
    margin-bottom: 10px;
  }}

  .overview-head {{
    margin-bottom: 12px;
  }}

  .overview-title {{
    margin: 0 0 4px;
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }}

  .overview-tip {{
    margin: 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }}

  .overview-stats {{
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
  }}

  .overview-stat {{
    padding: 12px 14px;
    border-radius: 8px;
    background: var(--el-fill-color-light);
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover,
    &.is-active {{
      background: #ecf5ff;
      box-shadow: inset 0 0 0 1px #d9ecff;
    }}
  }}

  .overview-stat--success.is-active {{
    background: #f0f9eb;
    box-shadow: inset 0 0 0 1px #e1f3d8;
  }}

  .overview-stat--failed.is-active {{
    background: #fef0f0;
    box-shadow: inset 0 0 0 1px #fde2e2;
  }}

  .overview-stat__label {{
    display: block;
    margin-bottom: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }}

  .overview-stat__value {{
    font-size: 24px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }}

  .overview-stat--success .overview-stat__value {{
    color: var(--el-color-success);
  }}

  .overview-stat--failed .overview-stat__value {{
    color: var(--el-color-danger);
  }}

  .search-card {{
    margin-bottom: 10px;

    :deep(.el-card__body) {{
      padding: 14px 16px 12px;
    }}
  }}

  .search-panel {{
    display: flex;
    flex-direction: column;
    gap: 12px;
  }}

  .search-form {{
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 4px 0;

    :deep(.el-form-item) {{
      margin-right: 16px;
      margin-bottom: 0;
    }}

    :deep(.el-form-item__label) {{
      white-space: nowrap;
      font-size: 13px;
      color: var(--el-text-color-regular);
      padding-right: 8px;
    }}
  }}

  .search-panel__bar {{
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding-top: 12px;
    border-top: 1px solid var(--el-border-color-lighter);
  }}

  .search-panel__actions {{
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
  }}

  .search-panel__meta {{
    flex-shrink: 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }}

  .field-phone {{
    width: 168px;
  }}

  .field-status {{
    width: 148px;
  }}

  .field-platform {{
    width: 168px;
  }}
}}

@media (max-width: 768px) {{
  .mark-agent-process-detail-page {{
    .overview-stats {{
      grid-template-columns: 1fr;
    }}

    .search-panel__bar {{
      flex-direction: column;
      align-items: flex-start;
    }}
  }}
}}
</style>
"""

targets = [
    (
        Path(__file__).resolve().parents[1] / "frontend/src/views/server/mark/agent/components/ProcessDetailOverview.vue",
        "ProcessDetailOverview",
    ),
    (
        Path(__file__).resolve().parents[1] / "frontend/src/views/server/mark/agent/process/detail.vue",
        "MarkAgentProcessDetail",
    ),
]

for target, component_name in targets:
    target.parent.mkdir(parents=True, exist_ok=True)
    file_content = content.replace('name="ProcessDetailOverview"', f'name="{component_name}"', 1)
    if component_name != "ProcessDetailOverview":
        file_content = file_content.replace(
            "import AgentPastDateRangePicker from './AgentPastDateRangePicker.vue'",
            "import AgentPastDateRangePicker from '../components/AgentPastDateRangePicker.vue'",
            1,
        )
    target.write_text(file_content, encoding="utf-8")
    print(f"Wrote {target}")
