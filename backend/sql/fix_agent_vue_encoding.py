# ASCII-only source; Chinese via unicode escapes
from pathlib import Path

ROOT = Path(r"c:\Users\Administrator\Desktop\1500\frontend\src\views\server\mark\agent")


def zh(*codes: int) -> str:
    return "".join(chr(c) for c in codes)


def write(rel: str, content: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    print("wrote", path)


write(
    "wallet/downstream.vue",
    f"""<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <span>{zh(0x8D44, 0x91D1, 0x660E, 0x7EC6, 0x28, 0x4E0B, 0x7EA7, 0x29)}</span>
      </template>
      <AgentWalletPanel />
    </el-card>
  </div>
</template>

<script setup name="MarkAgentWalletDownstream">
import AgentWalletPanel from '../components/AgentWalletPanel.vue'
</script>
""",
)

write(
    "transfer/index.vue",
    f"""<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <span>{zh(0x8D44, 0x91D1, 0x8F6C, 0x8D26, 0x660E, 0x7EC6)}</span>
      </template>
      <AgentWalletPanel fixed-biz-type="ADJUST" />
    </el-card>
  </div>
</template>

<script setup name="MarkAgentTransferLog">
import AgentWalletPanel from '../components/AgentWalletPanel.vue'
</script>
""",
)

write(
    "td/index.vue",
    f"""<template>
  <div class="app-container">
    <ProcessWorkbench
      :supply-tabs="tdTabs"
      default-supply-key="tdx"
      :show-supply-tabs="false"
    />
  </div>
</template>

<script setup name="MarkAgentTdCaptcha">
import ProcessWorkbench from '../components/ProcessWorkbench.vue'

const tdTabs = [
  {{ key: 'tdx', label: '{zh(0x6E05, 0x54, 0x44, 0x6536, 0x9A8C, 0x8BC1, 0x7801)}', platformCodes: 'mobile_gaopin,td_gaopin,td_second' }}
]
</script>
""",
)

T = {
    "phone": zh(0x53F7, 0x7801),
    "phone_ph": zh(0x8BF7, 0x8F93, 0x5165, 0x53F7, 0x7801),
    "status": zh(0x5904, 0x7406, 0x72B6, 0x6001),
    "select": zh(0x8BF7, 0x9009, 0x62E9),
    "deadline": zh(0x622A, 0x6B62, 0x65E5, 0x671F),
    "date_ph": zh(0x8BF7, 0x9009, 0x62E9, 0x65E5, 0x671F),
    "search": zh(0x641C, 0x7D22),
    "reset": zh(0x91CD, 0x7F6E),
    "col_phone": zh(0x624B, 0x673A, 0x2F, 0x56FA, 0x8BDD),
    "col_code": zh(0x9A8C, 0x8BC1, 0x7801),
    "col_status": zh(0x5904, 0x7406, 0x72B6, 0x6001),
    "col_batch": zh(0x6279, 0x6B21, 0x7F16, 0x53F7),
    "col_create": zh(0x521B, 0x5EFA, 0x65F6, 0x95F4),
    "col_success": zh(0x6210, 0x529F, 0x65F6, 0x95F4),
    "col_fail": zh(0x5931, 0x8D25, 0x65F6, 0x95F4),
    "col_action": zh(0x64CD, 0x4F5C),
    "code_ph": zh(0x9A8C, 0x8BC1, 0x7801, 0x2F, 0x5907, 0x6CE8),
    "success": zh(0x6210, 0x529F),
    "fail": zh(0x5931, 0x8D25),
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
}

write(
    "components/ProcessWorkbench.vue",
    f"""<template>
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

    <div class="search-panel">
      <el-form :model="queryParams" :inline="true" label-width="72px" @submit.prevent>
        <el-form-item label="{T['phone']}">
          <el-input v-model="queryParams.phone" placeholder="{T['phone_ph']}" clearable style="width: 180px;" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="{T['status']}">
          <el-select v-model="queryParams.processStatus" clearable placeholder="{T['select']}" style="width: 140px;">
            <el-option v-for="item in processStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="{T['deadline']}">
          <el-date-picker v-model="deadlineDate" type="date" value-format="YYYY-MM-DD" placeholder="{T['date_ph']}" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">{T['search']}</el-button>
          <el-button icon="Refresh" @click="resetQuery">{T['reset']}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table v-loading="loading" :data="itemList" border stripe class="process-table">
      <el-table-column label="{T['col_phone']}" prop="phone" min-width="130" align="center" />
      <el-table-column label="{T['col_code']}" min-width="100" align="center">
        <template #default="scope">{{{{ verifyCodeText(scope.row) }}}}</template>
      </el-table-column>
      <el-table-column label="{T['col_status']}" width="110" align="center">
        <template #default="scope">
          <span :class="['status-tag', processStatusClass(scope.row.processStatus)]">{{{{ processStatusLabel(scope.row.processStatus) }}}}</span>
        </template>
      </el-table-column>
      <el-table-column label="{T['col_batch']}" prop="orderNo" min-width="210" show-overflow-tooltip align="center" />
      <el-table-column label="{T['col_create']}" min-width="165" align="center">
        <template #default="scope">{{{{ formatAgentDateTime(scope.row.createTime) }}}}</template>
      </el-table-column>
      <el-table-column label="{T['col_success']}" min-width="165" align="center">
        <template #default="scope">{{{{ scope.row.processStatus === '1' ? formatAgentDateTime(scope.row.processedTime) : '' }}}}</template>
      </el-table-column>
      <el-table-column label="{T['col_fail']}" min-width="165" align="center">
        <template #default="scope">{{{{ scope.row.processStatus === '2' ? formatAgentDateTime(scope.row.processedTime) : '' }}}}</template>
      </el-table-column>
      <el-table-column label="{T['col_action']}" min-width="260" align="center" fixed="right">
        <template #default="scope">
          <template v-if="scope.row.processStatus === '0'">
            <el-input v-model="rowInputMap[scope.row.id]" size="small" placeholder="{T['code_ph']}" style="width: 110px; margin-right: 6px;" />
            <el-button type="success" size="small" :loading="submittingId === scope.row.id" v-hasPermi="['server:markAgent:item:feedback']" @click="submitFeedback(scope.row, '1')">{T['success']}</el-button>
            <el-button type="danger" size="small" plain :loading="submittingId === scope.row.id" v-hasPermi="['server:markAgent:item:feedback']" @click="submitFeedback(scope.row, '2')">{T['fail']}</el-button>
          </template>
          <span v-else class="processed-tip">{T['processed']}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :page-sizes="[30, 60, 90, 120]" @pagination="getList" />
  </div>
</template>

<script setup>
import {{ listMarkAgentOrderItem, feedbackMarkOrderItem }} from '@/api/server/markAgent'

const props = defineProps({{
  supplyTabs: {{ type: Array, default: () => ([{{ key: 'tdx', label: '{T['tab_tdx']}', platformCodes: 'mobile_gaopin,td_gaopin,td_second' }}, {{ key: 'qihu', label: '{T['tab_qihu']}', platformCodes: 'qihu_first,qihu_second' }}]) }},
  defaultSupplyKey: {{ type: String, default: 'tdx' }},
  showSupplyTabs: {{ type: Boolean, default: true }},
  defaultProcessStatus: {{ type: String, default: null }}
}})

const {{ proxy }} = getCurrentInstance()
const loading = ref(false)
const submittingId = ref(null)
const total = ref(0)
const itemList = ref([])
const rowInputMap = ref({{}})
const deadlineDate = ref('')
const activeSupplyKey = ref(props.defaultSupplyKey)
const queryParams = reactive({{ pageNum: 1, pageSize: 90, phone: null, processStatus: props.defaultProcessStatus, platformCodes: props.supplyTabs[0]?.platformCodes || '', params: {{}} }})
const processStatusOptions = [{{ label: '{T['pending']}', value: '0' }}, {{ label: '{T['proc_ok']}', value: '1' }}, {{ label: '{T['proc_fail']}', value: '2' }}]

function processStatusLabel(status) {{ const map = {{ '0': '{T['pending']}', '1': '{T['proc_ok']}', '2': '{T['proc_fail']}' }}; return map[status] || '-' }}
function processStatusClass(status) {{ if (status === '1') return 'is-success'; if (status === '2') return 'is-failed'; return 'is-pending' }}
function verifyCodeText(row) {{ const result = String(row?.processResult || '').trim(); if (result) return result; if (row?.processStatus === '1') return 'success'; return '-' }}
function formatAgentDateTime(value) {{ if (!value) return ''; const d = new Date(value); if (Number.isNaN(d.getTime())) return value; const pad = (n) => String(n).padStart(2, '0'); return `${{d.getFullYear()}}/${{d.getMonth() + 1}}/${{d.getDate()}} ${{pad(d.getHours())}}:${{pad(d.getMinutes())}}:${{pad(d.getSeconds())}}` }}
function buildQueryParams() {{ const params = {{ ...queryParams }}; params.params = {{}}; if (deadlineDate.value) params.params.endTime = deadlineDate.value; return params }}
function getList() {{ loading.value = true; listMarkAgentOrderItem(buildQueryParams()).then((res) => {{ itemList.value = res.rows || []; total.value = res.total || 0 }}).finally(() => {{ loading.value = false }}) }}
function handleQuery() {{ queryParams.pageNum = 1; getList() }}
function resetQuery() {{ queryParams.phone = null; queryParams.processStatus = props.defaultProcessStatus; deadlineDate.value = ''; handleQuery() }}
function handleSupplyChange(key) {{ const tab = props.supplyTabs.find((item) => item.key === key) || props.supplyTabs[0]; queryParams.platformCodes = tab?.platformCodes || ''; handleQuery() }}
function submitFeedback(row, processStatus) {{
  const itemId = row?.id; if (!itemId) return
  const processResult = String(rowInputMap.value[itemId] || '').trim()
  const actionText = processStatus === '1' ? '{T['mark_ok']}' : '{T['mark_fail']}'
  proxy.$modal.confirm(`{T['confirm']}${{actionText}}?`).then(() => {{
    submittingId.value = itemId
    return feedbackMarkOrderItem(itemId, {{ processStatus, processResult: processResult || (processStatus === '1' ? 'success' : 'failed') }})
  }}).then(() => {{ proxy.$modal.msgSuccess('{T['op_ok']}'); delete rowInputMap.value[itemId]; getList() }}).catch(() => {{}}).finally(() => {{ submittingId.value = null }})
}}
onMounted(() => {{ const tab = props.supplyTabs.find((item) => item.key === props.defaultSupplyKey) || props.supplyTabs[0]; queryParams.platformCodes = tab?.platformCodes || ''; getList() }})
</script>

<style scoped>
.mark-agent-process-page {{ padding-top: 8px; }}
.supply-tabs {{ margin-bottom: 4px; }}
.supply-tabs :deep(.el-tabs__header) {{ margin-bottom: 12px; }}
.search-panel {{ margin-bottom: 12px; }}
.search-panel :deep(.el-form-item) {{ margin-bottom: 10px; }}
.process-table :deep(.el-table__cell) {{ padding: 10px 0; }}
.status-tag {{ display: inline-block; min-width: 72px; padding: 4px 10px; border-radius: 2px; font-size: 13px; line-height: 1.2; color: #fff; }}
.status-tag.is-success {{ background: #13ce66; }}
.status-tag.is-failed {{ background: #ff4949; }}
.status-tag.is-pending {{ background: #909399; }}
.processed-tip {{ color: var(--el-text-color-secondary); font-size: 12px; }}
</style>
""",
)

write(
    "downstream/index.vue",
    f"""<template>
  <div class="app-container mark-agent-downstream-page">
    <el-card shadow="never">
      <template #header>
        <div class="panel-head">
          <span class="panel-head__title">{zh(0x4E0B, 0x7EBF, 0x8D26, 0x6237)}</span>
          <div class="panel-head__actions">
            <el-button type="primary" plain size="small" @click="goAccountManage">{zh(0x8D26, 0x6237, 0x7BA1, 0x7406)}</el-button>
            <el-button size="small" icon="Refresh" @click="getList">{zh(0x5237, 0x65B0)}</el-button>
          </div>
        </div>
      </template>
      <div class="search-bar">
        <el-form :inline="true" size="small" @submit.prevent>
          <el-form-item label="{zh(0x7528, 0x6237, 0x641C, 0x7D22)}">
            <el-input v-model="keyword" clearable placeholder="{zh(0x767B, 0x5F55, 0x540D, 0x2F, 0x6635, 0x79F0)}" style="width: 180px;" @keyup.enter="handleFilter" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleFilter">{zh(0x641C, 0x7D22)}</el-button>
            <el-button icon="Refresh" @click="resetFilter">{zh(0x91CD, 0x7F6E)}</el-button>
          </el-form-item>
        </el-form>
      </div>
      <el-table v-loading="loading" :data="filteredList" border stripe>
        <el-table-column label="Id" prop="userId" width="80" align="center" />
        <el-table-column label="{zh(0x767B, 0x5F55, 0x8D26, 0x53F7)}" prop="userName" min-width="120" show-overflow-tooltip />
        <el-table-column label="{zh(0x6635, 0x79F0)}" prop="nickName" min-width="120" show-overflow-tooltip />
        <el-table-column label="{zh(0x5269, 0x4F59, 0x6B21, 0x6570)}" prop="totalRemainCount" width="100" align="center">
          <template #default="scope"><span class="remain-value">{{{{ scope.row.totalRemainCount ?? 0 }}}}</span></template>
        </el-table-column>
        <el-table-column label="{zh(0x72B6, 0x6001)}" width="90" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'" size="small">{{{{ scope.row.status === '0' ? '{zh(0x6B63, 0x5E38)}' : '{zh(0x505C, 0x7528)}' }}}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="{zh(0x6D88, 0x606F, 0x5907, 0x6CE8)}" prop="remark" min-width="160" show-overflow-tooltip />
        <el-table-column label="{zh(0x64CD, 0x4F5C)}" width="180" align="center" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="goWallet(scope.row)">{zh(0x6D41, 0x6C34, 0x660E, 0x7EC6)}</el-button>
            <el-button link type="success" @click="goAccountManage">{zh(0x5145, 0x503C, 0x8C03, 0x6574)}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>
  </div>
</template>
<script setup name="MarkAgentDownstream">
import {{ listMarkAgentDownstreamSummary }} from '@/api/server/markAgent'
import {{ useRouter }} from 'vue-router'
const router = useRouter()
const loading = ref(false)
const total = ref(0)
const accountList = ref([])
const keyword = ref('')
const queryParams = reactive({{ pageNum: 1, pageSize: 10 }})
const filteredList = computed(() => {{
  const text = String(keyword.value || '').trim().toLowerCase()
  if (!text) return accountList.value
  return accountList.value.filter((item) => {{
    const source = [item.userName, item.nickName, item.remark].map((v) => String(v || '').toLowerCase()).join(' ')
    return source.includes(text)
  }})
}})
function getList() {{ loading.value = true; listMarkAgentDownstreamSummary(queryParams).then((res) => {{ accountList.value = res.rows || []; total.value = res.total || 0 }}).finally(() => {{ loading.value = false }}) }}
function handleFilter() {{ queryParams.pageNum = 1; getList() }}
function resetFilter() {{ keyword.value = ''; handleFilter() }}
function goAccountManage() {{ router.push('/mark/agentAccount') }}
function goWallet(row) {{ router.push({{ path: '/mark/agentWallet', query: {{ userId: row?.userId }} }}) }}
onMounted(() => {{ getList() }})
</script>
<style scoped>
.panel-head {{ display: flex; align-items: center; justify-content: space-between; gap: 12px; }}
.panel-head__title {{ font-size: 15px; font-weight: 600; }}
.panel-head__actions {{ display: flex; gap: 8px; }}
.search-bar {{ margin-bottom: 12px; padding-bottom: 12px; border-bottom: 1px solid var(--el-border-color-lighter); }}
.search-bar :deep(.el-form-item) {{ margin-bottom: 0; }}
.remain-value {{ color: var(--el-color-primary); font-weight: 600; }}
</style>
""",
)

write(
    "summary/index.vue",
    f"""<template>
  <div class="app-container mark-agent-summary-page">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="panel-head">
          <span class="panel-head__title">{zh(0x603B, 0x4EE3, 0x7406, 0x4FE1, 0x606F)}</span>
          <el-button size="small" icon="Refresh" @click="loadSummary">{zh(0x5237, 0x65B0)}</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="Id">{{{{ summary.userId || '-' }}}}</el-descriptions-item>
        <el-descriptions-item label="{zh(0x767B, 0x5F55, 0x8D26, 0x53F7)}">{{{{ summary.userName || '-' }}}}</el-descriptions-item>
        <el-descriptions-item label="{zh(0x6635, 0x79F0)}">{{{{ summary.nickName || '-' }}}}</el-descriptions-item>
        <el-descriptions-item label="{zh(0x4EE3, 0x7406, 0x5C42, 0x7EA7)}">
          <el-tag type="success" size="small">{{{{ summary.agentLevelLabel || '{zh(0x4E00, 0x7EA7, 0x603B, 0x4EE3)}' }}}}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="{zh(0x8D26, 0x6237, 0x4F59, 0x989D, 0x28, 0x6B21, 0x6570, 0x29)}">
          <span class="balance-value">{{{{ summary.totalRemainCount ?? 0 }}}}</span>
        </el-descriptions-item>
        <el-descriptions-item label="{zh(0x5355, 0x54, 0x44, 0x5355, 0x4EF7)}">
          {{{{ summary.sampleUnitPrice ?? '-' }}}}
          <span v-if="summary.samplePlatformName" class="unit-platform">{zh(0xFF08)}{{{{ summary.samplePlatformName }}}}{zh(0xFF09)}</span>
        </el-descriptions-item>
        <el-descriptions-item label="{zh(0x4E0B, 0x7EBF, 0x7528, 0x6237, 0x6570)}">{{{{ summary.downstreamCount ?? 0 }}}}</el-descriptions-item>
        <el-descriptions-item label="{zh(0x5F85, 0x5BA1, 0x6838, 0x8BA2, 0x5355)}">{{{{ summary.pendingAuditCount ?? 0 }}}}</el-descriptions-item>
        <el-descriptions-item label="{zh(0x6D88, 0x606F, 0x5907, 0x6CE8)}" :span="2">{{{{ summary.remark || '-' }}}}</el-descriptions-item>
      </el-descriptions>
      <div class="quick-actions">
        <el-button type="primary" plain @click="goPage('/mark/agentDownstream')">{zh(0x4E0B, 0x7EA7, 0x4EE3, 0x7406)}</el-button>
        <el-button type="success" plain @click="goPage('/mark/agentAccount')">{zh(0x8D26, 0x6237, 0x7BA1, 0x7406)}</el-button>
        <el-button @click="goPage('/mark/agentWallet')">{zh(0x8D44, 0x91D1, 0x6D41, 0x6C34)}</el-button>
        <el-button @click="goPage('/mark/agentAudit')">{zh(0x8BA2, 0x5355, 0x5BA1, 0x6838)}</el-button>
      </div>
    </el-card>
  </div>
</template>
<script setup name="MarkAgentSummary">
import {{ getMarkAgentMeSummary }} from '@/api/server/markAgent'
import {{ useRouter }} from 'vue-router'
const router = useRouter()
const loading = ref(false)
const summary = ref({{}})
function loadSummary() {{ loading.value = true; getMarkAgentMeSummary().then((res) => {{ summary.value = res.data || {{}} }}).finally(() => {{ loading.value = false }}) }}
function goPage(path) {{ router.push(path) }}
onMounted(() => {{ loadSummary() }})
</script>
<style scoped>
.panel-head {{ display: flex; align-items: center; justify-content: space-between; }}
.panel-head__title {{ font-size: 15px; font-weight: 600; }}
.balance-value {{ color: var(--el-color-primary); font-size: 18px; font-weight: 600; }}
.unit-platform {{ margin-left: 4px; color: var(--el-text-color-secondary); font-size: 12px; }}
.quick-actions {{ display: flex; flex-wrap: wrap; gap: 10px; margin-top: 16px; }}
</style>
""",
)

print("done")

import subprocess
subprocess.run(["python", str(Path(__file__).with_name("fix_process_workbench.py"))], check=True)
