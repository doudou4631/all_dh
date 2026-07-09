# -*- coding: utf-8 -*-
"""Fix ProcessWorkbench.vue UTF-8 Chinese labels."""
from pathlib import Path


def zh(*codes: int) -> str:
    return "".join(chr(c) for c in codes)


T = {
    "phone": zh(0x53F7, 0x7801),
    "phone_ph": zh(0x8BF7, 0x8F93, 0x5165, 0x53F7, 0x7801),
    "status": zh(0x5904, 0x7406, 0x72B6, 0x6001),
    "all": zh(0x5168, 0x90E8),
    "deadline": zh(0x622A, 0x6B62, 0x65E5, 0x671F),
    "date_ph": zh(0x8BF7, 0x9009, 0x62E9, 0x65E5, 0x671F),
    "search": zh(0x641C, 0x7D22),
    "reset": zh(0x91CD, 0x7F6E),
    "refresh": zh(0x5237, 0x65B0),
    "col_user": zh(0x63D0, 0x4EA4, 0x7528, 0x6237),
    "col_phone": zh(0x624B, 0x673A, 0x2F, 0x56FA, 0x8BDD),
    "col_code": zh(0x9A8C, 0x8BC1, 0x7801),
    "col_remark": zh(0x5907, 0x6CE8),
    "col_status": zh(0x5904, 0x7406, 0x72B6, 0x6001),
    "col_batch": zh(0x6279, 0x6B21, 0x7F16, 0x53F7),
    "col_create": zh(0x521B, 0x5EFA, 0x65F6, 0x95F4),
    "col_process": zh(0x5904, 0x7406, 0x65F6, 0x95F4),
    "col_action": zh(0x64CD, 0x4F5C),
    "code_ph": zh(0x9A8C, 0x8BC1, 0x7801, 0x2F, 0x5907, 0x6CE8),
    "success": zh(0x6210, 0x529F),
    "fail": zh(0x5931, 0x8D25),
    "auto_processing": zh(0x540E, 0x53F0, 0x5904, 0x7406, 0x4E2D),
    "processed": zh(0x5DF2, 0x5904, 0x7406),
    "tab_tdx": zh(0x4F9B, 0x5E94, 0x28, 0x54, 0x44, 0x58, 0x6CF0, 0x8FEA, 0x9891, 0x29),
    "tab_qihu": zh(0x4F9B, 0x5E94, 0x28, 0x33, 0x36, 0x30, 0x5947, 0x864E, 0x29),
    "pending": zh(0x5F85, 0x5904, 0x7406),
    "proc_ok": zh(0x5904, 0x7406, 0x6210, 0x529F),
    "proc_fail": zh(0x5904, 0x7406, 0x5931, 0x8D25),
    "mark_ok": zh(0x6807, 0x8BB0, 0x6210, 0x529F),
    "mark_fail": zh(0x6807, 0x8BB0, 0x5931, 0x8D25),
    "confirm": zh(0x786E, 0x8BA4, 0x5C06, 0x8BE5, 0x53F7, 0x7801),
    "op_ok": zh(0x64CD, 0x4F5C, 0x6210, 0x529F),
    "empty_desc": zh(0x6682, 0x65E0, 0x8BA2, 0x5355, 0x6570, 0x636E),
    "total_prefix": zh(0x5171, 0x627E, 0x5230),
    "total_suffix": zh(0x6761, 0x8BB0, 0x5F55),
}

path = Path(r"c:\Users\Administrator\Desktop\1500\frontend\src\views\server\mark\agent\components\ProcessWorkbench.vue")

content = f"""<template>
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
      <div class="search-toolbar">
        <span v-if="pageTitle" class="toolbar-title">{{{{ pageTitle }}}}</span>
        <el-form
          :model="queryParams"
          :inline="true"
          size="small"
          label-width="56px"
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
          <el-form-item label="{T['deadline']}">
            <el-date-picker
              v-model="deadlineDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="{T['date_ph']}"
              clearable
              class="field-date"
            />
          </el-form-item>
          <el-form-item class="search-actions">
            <el-button type="primary" icon="Search" @click="handleQuery">{T['search']}</el-button>
            <el-button icon="Refresh" @click="resetQuery">{T['reset']}</el-button>
            <el-button icon="RefreshRight" @click="getList">{T['refresh']}</el-button>
          </el-form-item>
        </el-form>
        <span v-if="total > 0" class="toolbar-meta">{T['total_prefix']} {{{{ total }}}} {T['total_suffix']}</span>
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
        <el-table-column label="{T['col_user']}" prop="userName" width="96" align="center" show-overflow-tooltip />
        <el-table-column label="{T['col_phone']}" prop="phone" min-width="118" align="center" show-overflow-tooltip />
        <el-table-column label="{T['col_code']}" min-width="88" align="center" show-overflow-tooltip>
          <template #default="scope">{{{{ verifyCodeText(scope.row) }}}}</template>
        </el-table-column>
        <el-table-column label="{T['col_remark']}" min-width="100" align="center" show-overflow-tooltip>
          <template #default="scope">{{{{ submitRemarkText(scope.row) }}}}</template>
        </el-table-column>
        <el-table-column label="{T['col_status']}" width="96" align="center">
          <template #default="scope">
            <el-tag :type="processStatusTagType(scope.row.processStatus)" size="small" effect="light">
              {{{{ processStatusLabel(scope.row.processStatus) }}}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="{T['col_batch']}" prop="orderNo" min-width="180" show-overflow-tooltip align="center" />
        <el-table-column label="{T['col_create']}" width="152" align="center" show-overflow-tooltip>
          <template #default="scope">{{{{ formatAgentDateTime(scope.row.createTime) }}}}</template>
        </el-table-column>
        <el-table-column label="{T['col_process']}" width="152" align="center" show-overflow-tooltip>
          <template #default="scope">{{{{ processedTimeText(scope.row) }}}}</template>
        </el-table-column>
        <el-table-column label="{T['col_action']}" width="210" align="center" fixed="right">
          <template #default="scope">
            <div v-if="scope.row.processStatus === '0' && !isTencentAutoPlatform(scope.row)" class="action-cell">
              <el-input
                v-model="rowInputMap[scope.row.id]"
                size="small"
                placeholder="{T['code_ph']}"
                class="action-input"
              />
              <el-button
                type="success"
                size="small"
                :loading="submittingId === scope.row.id"
                v-hasPermi="['server:markAgent:item:feedback']"
                @click="submitFeedback(scope.row, '1')"
              >
                {T['success']}
              </el-button>
              <el-button
                type="danger"
                size="small"
                plain
                :loading="submittingId === scope.row.id"
                v-hasPermi="['server:markAgent:item:feedback']"
                @click="submitFeedback(scope.row, '2')"
              >
                {T['fail']}
              </el-button>
            </div>
            <span v-else-if="scope.row.processStatus === '0' && isTencentAutoPlatform(scope.row)" class="processed-tip">{T['auto_processing']}</span>
            <span v-else class="processed-tip">{T['processed']}</span>
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
import {{ listMarkAgentOrderItem, feedbackMarkOrderItem }} from '@/api/server/markAgent'

const props = defineProps({{
  platformCode: {{ type: String, default: '' }},
  pageTitle: {{ type: String, default: '' }},
  supplyTabs: {{ type: Array, default: () => ([
    {{ key: 'tdx', label: '{T['tab_tdx']}', platformCodes: 'mobile_gaopin,td_gaopin,td_second' }},
    {{ key: 'qihu', label: '{T['tab_qihu']}', platformCodes: 'qihu_first,qihu_second' }}
  ]) }},
  defaultSupplyKey: {{ type: String, default: 'tdx' }},
  showSupplyTabs: {{ type: Boolean, default: true }},
  defaultProcessStatus: {{ type: String, default: null }}
}})

function resolvePlatformCodes() {{
  const code = String(props.platformCode || '').trim()
  if (code) return code
  const tab = props.supplyTabs.find((item) => item.key === props.defaultSupplyKey) || props.supplyTabs[0]
  return tab?.platformCodes || ''
}}

const {{ proxy }} = getCurrentInstance()
const loading = ref(false)
const submittingId = ref(null)
const total = ref(0)
const itemList = ref([])
const rowInputMap = ref({{}})
const deadlineDate = ref('')
const activeSupplyKey = ref(props.defaultSupplyKey)
const queryParams = reactive({{
  pageNum: 1,
  pageSize: 90,
  phone: null,
  processStatus: props.defaultProcessStatus,
  platformCodes: resolvePlatformCodes(),
  params: {{}}
}})
const processStatusOptions = [
  {{ label: '{T['pending']}', value: '0' }},
  {{ label: '{T['proc_ok']}', value: '1' }},
  {{ label: '{T['proc_fail']}', value: '2' }}
]
const emptyDescription = '{T['empty_desc']}'

function processStatusLabel(status) {{
  const map = {{ '0': '{T['pending']}', '1': '{T['proc_ok']}', '2': '{T['proc_fail']}' }}
  return map[status] || '-'
}}
function processStatusTagType(status) {{
  if (status === '1') return 'success'
  if (status === '2') return 'danger'
  return 'info'
}}
function verifyCodeText(row) {{
  const result = String(row?.processResult || '').trim()
  if (result) return result
  const remark = String(row?.remark || '').trim()
  if (/^\\d{{6}}$/.test(remark)) return remark
  if (row?.processStatus === '1') return 'success'
  return '-'
}}
function isTencentAutoPlatform(row) {{
  const code = String(row?.platformCode || props.platformCode || '').trim().toLowerCase()
  return ['tencent_mark', 'tencent', 'tx', 'txwz'].includes(code)
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
function buildQueryParams() {{
  const params = {{ ...queryParams }}
  params.params = {{}}
  if (deadlineDate.value) params.params.endTime = deadlineDate.value
  return params
}}
function getList() {{
  loading.value = true
  listMarkAgentOrderItem(buildQueryParams()).then((res) => {{
    itemList.value = res.rows || []
    total.value = res.total || 0
    setupTencentAutoRefresh()
  }}).finally(() => {{
    loading.value = false
  }})
}}
function hasTencentPending(items) {{
  return (items || []).some((row) => row.processStatus === '0' && isTencentAutoPlatform(row))
}}
let tencentRefreshTimer = null
function clearTencentAutoRefresh() {{
  if (tencentRefreshTimer) {{
    clearInterval(tencentRefreshTimer)
    tencentRefreshTimer = null
  }}
}}
function setupTencentAutoRefresh() {{
  clearTencentAutoRefresh()
  const code = String(props.platformCode || '').trim().toLowerCase()
  if (!['tencent_mark', 'tencent', 'tx', 'txwz'].includes(code)) return
  if (!hasTencentPending(itemList.value)) return
  tencentRefreshTimer = setInterval(() => {{
    listMarkAgentOrderItem(buildQueryParams()).then((res) => {{
      itemList.value = res.rows || []
      total.value = res.total || 0
      if (!hasTencentPending(itemList.value)) {{
        clearTencentAutoRefresh()
      }}
    }})
  }}, 3000)
}}
function handleQuery() {{
  queryParams.pageNum = 1
  getList()
}}
function resetQuery() {{
  queryParams.phone = null
  queryParams.processStatus = props.defaultProcessStatus
  deadlineDate.value = ''
  handleQuery()
}}
function handleSupplyChange(key) {{
  const tab = props.supplyTabs.find((item) => item.key === key) || props.supplyTabs[0]
  queryParams.platformCodes = tab?.platformCodes || ''
  handleQuery()
}}
function submitFeedback(row, processStatus) {{
  const itemId = row?.id
  if (!itemId) return
  const processResult = String(rowInputMap.value[itemId] || '').trim()
  const actionText = processStatus === '1' ? '{T['mark_ok']}' : '{T['mark_fail']}'
  proxy.$modal.confirm(`{T['confirm']}${{actionText}}?`).then(() => {{
    submittingId.value = itemId
    return feedbackMarkOrderItem(itemId, {{
      processStatus,
      processResult: processResult || (processStatus === '1' ? 'success' : 'failed')
    }})
  }}).then(() => {{
    proxy.$modal.msgSuccess('{T['op_ok']}')
    delete rowInputMap.value[itemId]
    getList()
  }}).catch(() => {{}}).finally(() => {{
    submittingId.value = null
  }})
}}
function applyPlatformFilter() {{
  queryParams.platformCodes = resolvePlatformCodes()
}}

watch(() => props.platformCode, () => {{
  applyPlatformFilter()
  handleQuery()
}})

onMounted(() => {{
  applyPlatformFilter()
  getList()
}})

onBeforeUnmount(() => {{
  clearTencentAutoRefresh()
}})
</script>

<style scoped>
.mark-agent-process-page {{
  padding: 0;
  margin: 0;
  width: 100%;
  font-size: 13px;
}}
.supply-tabs {{
  margin-bottom: 6px;
}}
.supply-tabs :deep(.el-tabs__header) {{
  margin-bottom: 0;
}}
.search-card,
.table-card {{
  margin-bottom: 8px;
  border-radius: 0;
  border-left: 0;
  border-right: 0;
}}
.search-card :deep(.el-card__body) {{
  padding: 6px 8px;
}}
.search-toolbar {{
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: nowrap;
  gap: 8px;
  overflow-x: auto;
}}
.toolbar-title {{
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
}}
.toolbar-meta {{
  flex-shrink: 0;
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}}
.search-form {{
  flex: 0 1 auto;
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  min-width: 0;
}}
.search-form :deep(.el-form-item) {{
  margin-right: 6px;
  margin-bottom: 0;
}}
.search-form :deep(.el-form-item__label) {{
  font-size: 12px;
  padding-right: 4px;
}}
.field-phone {{
  width: 118px;
}}
.field-status {{
  width: 108px;
}}
.field-date {{
  width: 128px;
}}
.search-actions :deep(.el-form-item__content) {{
  flex-wrap: nowrap;
  gap: 4px;
}}
.table-card :deep(.el-card__body) {{
  padding: 0 0 4px;
}}
.process-table {{
  width: 100%;
}}
.process-table :deep(.el-table__cell) {{
  padding: 6px 0;
  font-size: 12px;
}}
.process-table :deep(.el-table__header .cell) {{
  white-space: nowrap;
  font-size: 12px;
}}
.table-card :deep(.pagination-container) {{
  margin-top: 8px;
  padding: 0 8px 4px;
  justify-content: flex-end;
}}
.action-cell {{
  display: inline-flex;
  align-items: center;
  flex-wrap: nowrap;
  justify-content: flex-start;
  gap: 4px;
}}
.action-input {{
  width: 88px;
}}
.processed-tip {{
  color: var(--el-text-color-secondary);
  font-size: 12px;
}}
</style>
"""

path.write_text(content, encoding="utf-8")
print("fixed", path)
