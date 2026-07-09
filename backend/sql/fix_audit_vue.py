from pathlib import Path

ROOT = Path(r"c:\Users\Administrator\Desktop\1500\frontend\src\views\server\mark\agent\audit\index.vue")


def zh(*codes: int) -> str:
    return "".join(chr(c) for c in codes)


Z = {
    "pending": zh(0x5F85, 0x5BA1, 0x6838),
    "today_audit": zh(0x4ECA, 0x65E5, 0x5BA1, 0x6838),
    "today_pass": zh(0x4ECA, 0x65E5, 0x901A, 0x8FC7),
    "today_reject": zh(0x4ECA, 0x65E5, 0x62D2, 0x7EDD),
    "today_return": zh(0x4ECA, 0x65E5, 0x6253, 0x56DE),
    "pass_rate": zh(0x901A, 0x8FC7, 0x7387, 0x28, 0x25, 0x29),
    "history": zh(0x5BA1, 0x6838, 0x5386, 0x53F2),
    "keyword": zh(0x7EFC, 0x5408, 0x641C, 0x7D22),
    "keyword_ph": zh(0x8BA2, 0x5355, 0x53F7, 0x2F, 0x7528, 0x6237, 0x540D, 0x2F, 0x624B, 0x673A, 0x53F7),
    "platform": zh(0x5E73, 0x53F0),
    "platform_ph": zh(0x5E73, 0x53F0, 0x7F16, 0x7801),
    "submit_time": zh(0x63D0, 0x4EA4, 0x65F6, 0x95F4),
    "start_date": zh(0x5F00, 0x59CB, 0x65E5, 0x671F),
    "end_date": zh(0x7ED3, 0x675F, 0x65E5, 0x671F),
    "search": zh(0x641C, 0x7D22),
    "reset": zh(0x91CD, 0x7F6E),
    "order_no": zh(0x8BA2, 0x5355, 0x53F7),
    "username": zh(0x7528, 0x6237, 0x540D),
    "phone_preview": zh(0x53F7, 0x7801, 0x9884, 0x89C8),
    "count": zh(0x6570, 0x91CF),
    "audit_status": zh(0x5BA1, 0x6838, 0x72B6, 0x6001),
    "audit_opinion": zh(0x5BA1, 0x6838, 0x610F, 0x89C1),
    "audit_time": zh(0x5BA1, 0x6838, 0x65F6, 0x95F4),
    "action": zh(0x64CD, 0x4F5C),
    "pass": zh(0x901A, 0x8FC7),
    "reject": zh(0x62D2, 0x7EDD),
    "return": zh(0x6253, 0x56DE),
    "cancel": zh(0x53D6, 0x6D88),
    "confirm": zh(0x786E, 0x5B9A),
    "opinion_ph": zh(0x8BF7, 0x586B, 0x5199, 0x5BA1, 0x6838, 0x610F, 0x89C1),
    "passed": zh(0x5DF2, 0x901A, 0x8FC7),
    "rejected": zh(0x5DF2, 0x62D2, 0x7EDD),
    "returned": zh(0x5DF2, 0x6253, 0x56DE),
    "audit_pass": zh(0x5BA1, 0x6838, 0x901A, 0x8FC7),
    "audit_reject": zh(0x5BA1, 0x6838, 0x62D2, 0x7EDD),
    "audit_return": zh(0x5BA1, 0x6838, 0x6253, 0x56DE),
    "confirm_pass": zh(0x786E, 0x8BA4, 0x901A, 0x8FC7, 0x8BA2, 0x5355),
    "need_opinion": zh(0x8BF7, 0x586B, 0x5199, 0x5BA1, 0x6838, 0x610F, 0x89C1),
}

content = f"""<template>
  <div class="app-container mark-agent-audit-page">
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="8" :md="4"><el-card shadow="never"><el-statistic title="{Z['pending']}" :value="stats.pendingCount || 0" /></el-card></el-col>
      <el-col :xs="12" :sm="8" :md="4"><el-card shadow="never"><el-statistic title="{Z['today_audit']}" :value="stats.todayAuditCount || 0" /></el-card></el-col>
      <el-col :xs="12" :sm="8" :md="4"><el-card shadow="never"><el-statistic title="{Z['today_pass']}" :value="stats.todayPassCount || 0" /></el-card></el-col>
      <el-col :xs="12" :sm="8" :md="4"><el-card shadow="never"><el-statistic title="{Z['today_reject']}" :value="stats.todayRejectCount || 0" /></el-card></el-col>
      <el-col :xs="12" :sm="8" :md="4"><el-card shadow="never"><el-statistic title="{Z['today_return']}" :value="stats.todayReturnCount || 0" /></el-card></el-col>
      <el-col :xs="12" :sm="8" :md="4"><el-card shadow="never"><el-statistic title="{Z['pass_rate']}" :value="stats.passRate || 0" :precision="2" /></el-card></el-col>
    </el-row>
    <el-tabs v-model="activeTab" class="audit-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="{Z['pending']}" name="pending" />
      <el-tab-pane label="{Z['history']}" name="history" />
    </el-tabs>
    <div class="search-panel">
      <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="72px" @submit.prevent>
        <el-form-item label="{Z['keyword']}" prop="keyword">
          <el-input v-model="queryParams.keyword" placeholder="{Z['keyword_ph']}" clearable style="width: 220px;" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="{Z['platform']}" prop="platformCode">
          <el-input v-model="queryParams.platformCode" placeholder="{Z['platform_ph']}" clearable style="width: 160px;" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="{Z['submit_time']}">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="-" :start-placeholder="Z['start_date']" :end-placeholder="Z['end_date']" style="width: 240px;" @change="handleDateRangeChange" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">{Z['search']}</el-button>
          <el-button icon="Refresh" @click="resetQuery">{Z['reset']}</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-table v-loading="loading" :data="orderList" border stripe>
      <el-table-column label="{Z['order_no']}" prop="orderNo" min-width="190" show-overflow-tooltip />
      <el-table-column label="{Z['username']}" prop="userName" min-width="110" show-overflow-tooltip />
      <el-table-column label="{Z['phone_preview']}" prop="phonePreview" min-width="130" show-overflow-tooltip />
      <el-table-column label="{Z['platform']}" prop="platformName" min-width="120" show-overflow-tooltip />
      <el-table-column label="{Z['count']}" prop="totalCount" width="70" align="center" />
      <el-table-column label="{Z['submit_time']}" min-width="165" align="center">
        <template #default="scope">{{{{ formatDateTime(scope.row.createTime) }}}}</template>
      </el-table-column>
      <el-table-column v-if="activeTab === 'history'" label="{Z['audit_status']}" width="100" align="center">
        <template #default="scope"><el-tag :type="auditStatusType(scope.row)" size="small">{{{{ auditStatusLabel(scope.row) }}}}</el-tag></template>
      </el-table-column>
      <el-table-column v-if="activeTab === 'history'" label="{Z['audit_opinion']}" prop="auditOpinion" min-width="180" show-overflow-tooltip />
      <el-table-column v-if="activeTab === 'history'" label="{Z['audit_time']}" min-width="165" align="center">
        <template #default="scope">{{{{ formatDateTime(scope.row.auditTime) }}}}</template>
      </el-table-column>
      <el-table-column v-if="activeTab === 'pending'" label="{Z['action']}" width="220" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="success" v-hasPermi="['server:markAgent:audit:pass']" @click="handlePass(scope.row)">{Z['pass']}</el-button>
          <el-button link type="danger" v-hasPermi="['server:markAgent:audit:reject']" @click="openAuditDialog(scope.row, 'reject')">{Z['reject']}</el-button>
          <el-button link type="warning" v-hasPermi="['server:markAgent:audit:return']" @click="openAuditDialog(scope.row, 'return')">{Z['return']}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog v-model="auditDialogOpen" :title="auditDialogTitle" width="520px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="{Z['order_no']}"><span>{{{{ currentOrder?.orderNo || '-' }}}}</span></el-form-item>
        <el-form-item label="{Z['audit_opinion']}" required>
          <el-input v-model="auditOpinion" type="textarea" :rows="4" maxlength="500" show-word-limit :placeholder="Z['opinion_ph']" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogOpen = false">{Z['cancel']}</el-button>
        <el-button type="primary" :loading="auditSubmitting" @click="submitAuditDialog">{Z['confirm']}</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name="MarkAgentAudit">
import {{ listMarkAgentAuditPending, listMarkAgentAuditHistory, passMarkAgentAudit, rejectMarkAgentAudit, returnMarkAgentAudit, getMarkAgentAuditStats }} from '@/api/server/markAgent'
const {{ proxy }} = getCurrentInstance()
const loading = ref(false)
const total = ref(0)
const orderList = ref([])
const activeTab = ref('pending')
const dateRange = ref([])
const auditDialogOpen = ref(false)
const auditDialogAction = ref('')
const auditOpinion = ref('')
const auditSubmitting = ref(false)
const currentOrder = ref(null)
const stats = ref({{ pendingCount: 0, todayAuditCount: 0, todayPassCount: 0, todayRejectCount: 0, todayReturnCount: 0, passRate: 0 }})
const queryParams = reactive({{ pageNum: 1, pageSize: 10, keyword: null, platformCode: null, params: {{}} }})
const auditDialogTitle = computed(() => {{ if (auditDialogAction.value === 'reject') return '{Z['audit_reject']}'; if (auditDialogAction.value === 'return') return '{Z['audit_return']}'; return '{Z['audit_status']}' }})
function formatDateTime(value) {{ if (!value) return '-'; const d = new Date(value); if (Number.isNaN(d.getTime())) return value; const p = (n) => String(n).padStart(2, '0'); return `${{d.getFullYear()}}-${{p(d.getMonth() + 1)}}-${{p(d.getDate())}} ${{p(d.getHours())}}:${{p(d.getMinutes())}}:${{p(d.getSeconds())}}` }}
function auditStatusLabel(row) {{ const map = {{ '1': '{Z['passed']}', '2': '{Z['rejected']}', '3': '{Z['returned']}' }}; return map[String(row?.auditStatus ?? '')] || '-' }}
function auditStatusType(row) {{ const status = String(row?.auditStatus ?? ''); if (status === '1') return 'success'; if (status === '2') return 'danger'; return 'warning' }}
function getListApi() {{ return activeTab.value === 'pending' ? listMarkAgentAuditPending : listMarkAgentAuditHistory }}
function loadStats() {{ return getMarkAgentAuditStats().then((res) => {{ stats.value = res.data || {{}} }}).catch(() => {{}}) }}
function getList() {{ loading.value = true; return getListApi()(queryParams).then((res) => {{ orderList.value = res.rows || []; total.value = res.total || 0 }}).finally(() => {{ loading.value = false; loadStats() }}) }}
function handleQuery() {{ queryParams.pageNum = 1; getList() }}
function resetQuery() {{ queryParams.keyword = null; queryParams.platformCode = null; queryParams.params = {{}}; dateRange.value = []; handleQuery() }}
function handleDateRangeChange(value) {{ if (Array.isArray(value) && value.length === 2) queryParams.params = {{ beginTime: value[0], endTime: value[1] }}; else queryParams.params = {{}} }}
function handleTabChange() {{ queryParams.pageNum = 1; getList() }}
function handlePass(row) {{ proxy.$modal.confirm(`{Z['confirm_pass']} ${{row.orderNo}} ?`).then(() => passMarkAgentAudit(row.id, {{}})).then(() => {{ proxy.$modal.msgSuccess('{Z['audit_pass']}'); getList() }}).catch(() => {{}}) }}
function openAuditDialog(row, action) {{ currentOrder.value = row; auditDialogAction.value = action; auditOpinion.value = ''; auditDialogOpen.value = true }}
function submitAuditDialog() {{ const opinion = String(auditOpinion.value || '').trim(); if (!opinion) {{ proxy.$modal.msgWarning('{Z['need_opinion']}'); return }}; const orderId = currentOrder.value?.id; if (!orderId) return; const payload = {{ auditOpinion: opinion }}; const request = auditDialogAction.value === 'reject' ? rejectMarkAgentAudit(orderId, payload) : returnMarkAgentAudit(orderId, payload); auditSubmitting.value = true; request.then(() => {{ proxy.$modal.msgSuccess(auditDialogAction.value === 'reject' ? '{Z['rejected']}' : '{Z['returned']}'); auditDialogOpen.value = false; getList() }}).finally(() => {{ auditSubmitting.value = false }}) }}
onMounted(() => {{ getList() }})
</script>
<style scoped>.stats-row {{ margin-bottom: 12px; }}.search-panel {{ margin-bottom: 12px; }}.audit-tabs {{ margin-bottom: 8px; }}</style>
"""

ROOT.write_text(content, encoding="utf-8")
print("wrote", ROOT)
