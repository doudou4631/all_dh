<script setup>
import { computed, reactive, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import DataTable from '../components/DataTable.vue'
import FilterToolbar from '../components/FilterToolbar.vue'
import StatePanel from '../components/StatePanel.vue'
import { listTdxTasks } from '../api/client'
import { useAsyncData } from '../composables/useAsyncData'
import { authState } from '../stores/authStore'

const filters = reactive({
  line: 'all',
  status: 'all',
  date: chinaToday(),
  q: '',
})

const payloadDialogVisible = ref(false)
const payloadDialogRow = ref(null)

const { data, loading, error, refresh, lastLoadedAt } = useAsyncData(() => listTdxTasks({ status: filters.status, q: filters.q }), [])

const isAdmin = computed(() => {
  const user = authState.user || {}
  const values = [user.username, user.userName, user.name, user.role, user.roleName, user.access?.roleName]
    .filter(Boolean)
    .map((value) => String(value).trim().toLowerCase())
  return values.some((value) => value === 'admin' || value.includes('admin') || value.includes('管理员'))
})

function firstText(...values) {
  for (const value of values) {
    if (value === 0) return '0'
    const text = String(value ?? '').trim()
    if (text) return text
  }
  return ''
}

function chinaToday() {
  const parts = new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]))
  return `${values.year}-${values.month}-${values.day}`
}

function formatChinaDateTime(value) {
  const text = String(value ?? '').trim()
  if (!text) return ''

  const hasTimezone = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(text)
  const source = hasTimezone ? text : text.replace(' ', 'T').replace(/(\.\d{1,3})?$/, '$1+08:00')
  const date = new Date(source)
  if (Number.isNaN(date.getTime())) return text

  const parts = new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).formatToParts(date)
  const values = Object.fromEntries(parts.map(({ type, value: partValue }) => [type, partValue]))
  return `${values.year}-${values.month}-${values.day} ${values.hour}:${values.minute}:${values.second}`
}

function lineLabel(line) {
  if (String(line || '').trim().toLowerCase() === 'line3') return '线路3 / 失败订单复用'
  return String(line || '').trim().toLowerCase() === 'line2' ? '线路2' : '线路1'
}

function normalizeAudit(task = {}) {
  const audit = task.result?.upstreamAudit || task.upstreamAudit || {}
  const line = firstText(audit.line, task.line, 'line1')
  return {
    id: firstText(task.id),
    username: firstText(task.username, task.userName, task.operatorName, '未知用户'),
    phone: firstText(task.phone),
    localOrderId: firstText(task.orderpicinumber, task.batchLabel),
  provider: firstText(audit.provider, line === 'line2' ? 'Dengfeng' : line === 'line3' ? '原订单复用' : 'QMF'),
    line,
    lineLabel: lineLabel(line),
    upstreamOrderId: firstText(audit.orderId, task.orderId),
    wxid: firstText(audit.wxid),
    failureStep: firstText(audit.failureStep),
    qmfStage: firstText(audit.qmfStage),
    upstreamCode: audit.upstreamCode ?? '',
    upstreamMessage: firstText(audit.upstreamMessage),
    usedCount: audit.usedCount === null || audit.usedCount === undefined ? '' : audit.usedCount,
    reused: typeof audit.reused === 'boolean' ? (audit.reused ? '是' : '否') : '',
    capturedAt: formatChinaDateTime(firstText(audit.capturedAt, task.updatedAt)),
    status: firstText(task.status),
    currentStep: firstText(task.currentStep),
    upstreamPayload: audit.upstreamPayload || null,
  }
}

const allRows = computed(() => (Array.isArray(data.value) ? data.value : []).map(normalizeAudit))
const rows = computed(() => {
  const dateRows = filters.date
    ? allRows.value.filter((row) => String(row.capturedAt || '').startsWith(filters.date))
    : allRows.value
  if (filters.line === 'all') return dateRows
  return dateRows.filter((row) => row.line === filters.line)
})
const stats = computed(() => ({
  total: rows.value.length,
  line1: rows.value.filter((row) => row.line === 'line1').length,
  line2: rows.value.filter((row) => row.line === 'line2').length,
  line3: rows.value.filter((row) => row.line === 'line3').length,
  failed: rows.value.filter((row) => row.status === 'failed').length,
}))

const columns = [
  { key: 'id', label: '本地任务', minWidth: 170 },
  { key: 'username', label: '操作用户', minWidth: 120 },
  { key: 'phone', label: '手机号', minWidth: 125 },
  { key: 'localOrderId', label: '本地订单号', minWidth: 170 },
  { key: 'lineLabel', label: '线路', width: 78 },
  { key: 'provider', label: '上游服务', width: 110 },
  { key: 'upstreamOrderId', label: '上游订单号', minWidth: 190 },
  { key: 'wxid', label: '微信号 wxid', minWidth: 150 },
  { key: 'usedCount', label: '使用次数', width: 90, align: 'center' },
  { key: 'status', label: '任务状态', width: 105 },
  { key: 'failureStep', label: '失败步骤', minWidth: 130 },
  { key: 'upstreamMessage', label: '上游提示', minWidth: 240 },
  { key: 'capturedAt', label: '记录时间', minWidth: 165 },
  { key: 'payload', label: '回包', width: 80, align: 'center', showOverflowTooltip: false },
]

function resetFilters() {
  filters.line = 'all'
  filters.status = 'all'
  filters.date = chinaToday()
  filters.q = ''
  refresh()
}

function showPayload(row) {
  payloadDialogRow.value = row
  payloadDialogVisible.value = true
}

function exportCsv() {
  const fields = ['id', 'username', 'phone', 'localOrderId', 'lineLabel', 'provider', 'upstreamOrderId', 'wxid', 'usedCount', 'reused', 'failureStep', 'qmfStage', 'upstreamCode', 'upstreamMessage', 'status', 'capturedAt']
  const header = fields.join(',')
  const quote = (value) => `"${String(value ?? '').replaceAll('"', '""')}"`
  const content = [
    header,
    ...rows.value.map((row) => fields.map((field) => quote(row[field])).join(',')),
  ].join('\n')
  const blob = new Blob([`\uFEFF${content}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `tdx-upstream-audit-${filters.date || chinaToday()}.csv`
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <AppShell active="tdx-audit" title="泰迪熊对账审计" subtitle="按本地订单追踪线路1、线路2及线路3的上游订单。">
    <template #actions>
      <button class="btn btn-soft" type="button" @click="refresh" :disabled="loading">刷新</button>
      <button class="btn btn-primary" type="button" @click="exportCsv" :disabled="loading || !rows.length">导出 CSV</button>
    </template>

    <StatePanel
      v-if="!isAdmin"
      variant="error"
      title="仅管理员可查看"
      description="上游订单号和审计回包只对管理员开放。"
    />

    <template v-else>
      <section class="audit-stat-grid">
        <article class="audit-stat"><span>当前记录</span><strong>{{ stats.total }}</strong></article>
        <article class="audit-stat"><span>线路1 / QMF</span><strong>{{ stats.line1 }}</strong></article>
        <article class="audit-stat"><span>线路2 / 登峰</span><strong>{{ stats.line2 }}</strong></article>
        <article class="audit-stat"><span>线路3 / 失败复用</span><strong>{{ stats.line3 }}</strong></article>
        <article class="audit-stat audit-stat--danger"><span>失败记录</span><strong>{{ stats.failed }}</strong></article>
      </section>

      <FilterToolbar>
        <input v-model="filters.date" class="toolbar-select audit-date" type="date" aria-label="选择审计日期" />
        <select v-model="filters.line" class="toolbar-select" aria-label="筛选线路">
          <option value="all">全部线路</option>
          <option value="line1">线路1 / QMF</option>
          <option value="line2">线路2 / 登峰</option>
          <option value="line3">线路3 / 失败订单复用</option>
        </select>
        <select v-model="filters.status" class="toolbar-select" aria-label="筛选状态">
          <option value="all">全部状态</option>
          <option value="queued">queued</option>
          <option value="processing">processing</option>
          <option value="completed">completed</option>
          <option value="failed">failed</option>
        </select>
        <input v-model="filters.q" class="toolbar-select audit-search" placeholder="手机号 / 本地订单号 / 上游订单号" @keyup.enter="refresh" />
        <button class="toolbar-chip active" type="button" @click="refresh">查询</button>
        <button class="toolbar-chip" type="button" @click="resetFilters">重置</button>
      </FilterToolbar>

      <section class="card panel audit-panel">
        <StatePanel v-if="loading" title="正在加载审计记录..." description="正在读取泰迪熊任务和上游订单映射。" />
        <StatePanel v-else-if="error" variant="error" title="审计记录加载失败" :description="error" />
        <StatePanel v-else-if="!rows.length" title="暂无审计记录" description="当前筛选条件下没有可对账的泰迪熊任务。" />
        <DataTable v-else :columns="columns" :rows="rows" row-key="id" empty-text="暂无审计记录">
          <template #cell-status="{ row }">
            <el-tag :type="row.status === 'failed' ? 'danger' : row.status === 'completed' ? 'success' : 'warning'" effect="plain">
              {{ row.status || '-' }}
            </el-tag>
          </template>
          <template #cell-upstreamOrderId="{ row }">
            <span class="audit-order-id">{{ row.upstreamOrderId || '未分配' }}</span>
          </template>
          <template #cell-wxid="{ row }">
            <span class="audit-wxid" :title="row.wxid || ''">{{ row.wxid || '未返回' }}</span>
          </template>
          <template #cell-upstreamMessage="{ row }">
            <span :title="row.upstreamMessage || row.currentStep">{{ row.upstreamMessage || row.currentStep || '-' }}</span>
          </template>
          <template #cell-payload="{ row }">
            <button v-if="row.upstreamPayload" class="audit-payload-trigger" type="button" @click="showPayload(row)">查看</button>
            <span v-else class="muted">-</span>
          </template>
        </DataTable>
        <div v-if="lastLoadedAt" class="panel-sync-meta">最近同步：{{ lastLoadedAt }}</div>
      </section>

      <el-dialog
        v-model="payloadDialogVisible"
        title="上游回包"
        width="min(760px, 92vw)"
        align-center
        destroy-on-close
      >
        <div v-if="payloadDialogRow" class="audit-payload-dialog-meta">
          <span>{{ payloadDialogRow.phone || '-' }}</span>
          <span>{{ payloadDialogRow.upstreamOrderId || '未分配' }}</span>
          <span>{{ payloadDialogRow.provider || '-' }}</span>
        </div>
        <pre v-if="payloadDialogRow" class="audit-payload-dialog-content">{{ JSON.stringify(payloadDialogRow.upstreamPayload, null, 2) }}</pre>
      </el-dialog>
    </template>
  </AppShell>
</template>

<style scoped>
.audit-stat-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.audit-stat {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 66px;
  padding: 14px 16px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  background: #fff;
  color: #475569;
}

.audit-stat strong {
  color: #0f172a;
  font-size: 22px;
}

.audit-stat--danger strong {
  color: #dc2626;
}

.audit-search {
  min-width: 280px;
}

.audit-date {
  min-width: 148px;
}

.audit-panel {
  overflow: visible;
}

.audit-order-id {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
}

.audit-wxid {
  display: inline-block;
  max-width: 180px;
  overflow: hidden;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.audit-payload-trigger {
  border: 0;
  padding: 0;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font: inherit;
}

.audit-payload-trigger:hover {
  color: #1d4ed8;
  text-decoration: underline;
}

.audit-payload-dialog-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  margin-bottom: 10px;
  color: #64748b;
  font-size: 13px;
}

.audit-payload-dialog-content {
  width: 100%;
  max-height: 58vh;
  overflow: auto;
  padding: 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #0f172a;
  color: #e2e8f0;
  font: 12px/1.5 ui-monospace, SFMono-Regular, Consolas, monospace;
  white-space: pre-wrap;
  text-align: left;
}

.muted {
  color: #94a3b8;
}

@media (max-width: 900px) {
  .audit-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .audit-search {
    min-width: 180px;
  }

  .audit-date {
    min-width: 148px;
  }
}

@media (max-width: 560px) {
  .audit-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
