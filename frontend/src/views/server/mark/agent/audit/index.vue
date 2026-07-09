<template>
  <div class="app-container mark-agent-audit-page">
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="8" :md="4"><el-card shadow="never"><el-statistic title="待审核" :value="stats.pendingCount || 0" /></el-card></el-col>
      <el-col :xs="12" :sm="8" :md="4"><el-card shadow="never"><el-statistic title="今日审核" :value="stats.todayAuditCount || 0" /></el-card></el-col>
      <el-col :xs="12" :sm="8" :md="4"><el-card shadow="never"><el-statistic title="今日通过" :value="stats.todayPassCount || 0" /></el-card></el-col>
      <el-col :xs="12" :sm="8" :md="4"><el-card shadow="never"><el-statistic title="今日拒绝" :value="stats.todayRejectCount || 0" /></el-card></el-col>
      <el-col :xs="12" :sm="8" :md="4"><el-card shadow="never"><el-statistic title="今日打回" :value="stats.todayReturnCount || 0" /></el-card></el-col>
      <el-col :xs="12" :sm="8" :md="4"><el-card shadow="never"><el-statistic title="通过率(%)" :value="stats.passRate || 0" :precision="2" /></el-card></el-col>
    </el-row>
    <el-tabs v-model="activeTab" class="audit-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="待审核" name="pending" />
      <el-tab-pane label="审核历史" name="history" />
    </el-tabs>
    <div class="search-panel">
      <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="72px" @submit.prevent>
        <el-form-item label="综合搜索" prop="keyword">
          <el-input v-model="queryParams.keyword" placeholder="订单号/用户名/手机号" clearable style="width: 220px;" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="平台" prop="platformCode">
          <el-input v-model="queryParams.platformCode" placeholder="平台编码" clearable style="width: 160px;" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="提交时间">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="-" :start-placeholder="Z['start_date']" :end-placeholder="Z['end_date']" style="width: 240px;" @change="handleDateRangeChange" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-table v-loading="loading" :data="orderList" border stripe>
      <el-table-column label="订单号" prop="orderNo" min-width="190" show-overflow-tooltip />
      <el-table-column label="用户名" prop="userName" min-width="110" show-overflow-tooltip />
      <el-table-column label="号码预览" prop="phonePreview" min-width="130" show-overflow-tooltip />
      <el-table-column label="平台" prop="platformName" min-width="120" show-overflow-tooltip />
      <el-table-column label="数量" prop="totalCount" width="70" align="center" />
      <el-table-column label="提交时间" min-width="165" align="center">
        <template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column v-if="activeTab === 'history'" label="审核状态" width="100" align="center">
        <template #default="scope"><el-tag :type="auditStatusType(scope.row)" size="small">{{ auditStatusLabel(scope.row) }}</el-tag></template>
      </el-table-column>
      <el-table-column v-if="activeTab === 'history'" label="审核意见" prop="auditOpinion" min-width="180" show-overflow-tooltip />
      <el-table-column v-if="activeTab === 'history'" label="审核时间" min-width="165" align="center">
        <template #default="scope">{{ formatDateTime(scope.row.auditTime) }}</template>
      </el-table-column>
      <el-table-column v-if="activeTab === 'pending'" label="操作" width="220" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="success" v-hasPermi="['server:markAgent:audit:pass']" @click="handlePass(scope.row)">通过</el-button>
          <el-button link type="danger" v-hasPermi="['server:markAgent:audit:reject']" @click="openAuditDialog(scope.row, 'reject')">拒绝</el-button>
          <el-button link type="warning" v-hasPermi="['server:markAgent:audit:return']" @click="openAuditDialog(scope.row, 'return')">打回</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog v-model="auditDialogOpen" :title="auditDialogTitle" width="520px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="订单号"><span>{{ currentOrder?.orderNo || '-' }}</span></el-form-item>
        <el-form-item label="审核意见" required>
          <el-input v-model="auditOpinion" type="textarea" :rows="4" maxlength="500" show-word-limit :placeholder="Z['opinion_ph']" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="auditSubmitting" @click="submitAuditDialog">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name="MarkAgentAudit">
import { listMarkAgentAuditPending, listMarkAgentAuditHistory, passMarkAgentAudit, rejectMarkAgentAudit, returnMarkAgentAudit, getMarkAgentAuditStats } from '@/api/server/markAgent'
const { proxy } = getCurrentInstance()
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
const stats = ref({ pendingCount: 0, todayAuditCount: 0, todayPassCount: 0, todayRejectCount: 0, todayReturnCount: 0, passRate: 0 })
const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: null, platformCode: null, params: {} })
const auditDialogTitle = computed(() => { if (auditDialogAction.value === 'reject') return '审核拒绝'; if (auditDialogAction.value === 'return') return '审核打回'; return '审核状态' })
function formatDateTime(value) { if (!value) return '-'; const d = new Date(value); if (Number.isNaN(d.getTime())) return value; const p = (n) => String(n).padStart(2, '0'); return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}` }
function auditStatusLabel(row) { const map = { '1': '已通过', '2': '已拒绝', '3': '已打回' }; return map[String(row?.auditStatus ?? '')] || '-' }
function auditStatusType(row) { const status = String(row?.auditStatus ?? ''); if (status === '1') return 'success'; if (status === '2') return 'danger'; return 'warning' }
function getListApi() { return activeTab.value === 'pending' ? listMarkAgentAuditPending : listMarkAgentAuditHistory }
function loadStats() { return getMarkAgentAuditStats().then((res) => { stats.value = res.data || {} }).catch(() => {}) }
function getList() { loading.value = true; return getListApi()(queryParams).then((res) => { orderList.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false; loadStats() }) }
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { queryParams.keyword = null; queryParams.platformCode = null; queryParams.params = {}; dateRange.value = []; handleQuery() }
function handleDateRangeChange(value) { if (Array.isArray(value) && value.length === 2) queryParams.params = { beginTime: value[0], endTime: value[1] }; else queryParams.params = {} }
function handleTabChange() { queryParams.pageNum = 1; getList() }
function handlePass(row) { proxy.$modal.confirm(`确认通过订单 ${row.orderNo} ?`).then(() => passMarkAgentAudit(row.id, {})).then(() => { proxy.$modal.msgSuccess('审核通过'); getList() }).catch(() => {}) }
function openAuditDialog(row, action) { currentOrder.value = row; auditDialogAction.value = action; auditOpinion.value = ''; auditDialogOpen.value = true }
function submitAuditDialog() { const opinion = String(auditOpinion.value || '').trim(); if (!opinion) { proxy.$modal.msgWarning('请填写审核意见'); return }; const orderId = currentOrder.value?.id; if (!orderId) return; const payload = { auditOpinion: opinion }; const request = auditDialogAction.value === 'reject' ? rejectMarkAgentAudit(orderId, payload) : returnMarkAgentAudit(orderId, payload); auditSubmitting.value = true; request.then(() => { proxy.$modal.msgSuccess(auditDialogAction.value === 'reject' ? '已拒绝' : '已打回'); auditDialogOpen.value = false; getList() }).finally(() => { auditSubmitting.value = false }) }
onMounted(() => { getList() })
</script>
<style scoped>.stats-row { margin-bottom: 12px; }.search-panel { margin-bottom: 12px; }.audit-tabs { margin-bottom: 8px; }</style>
