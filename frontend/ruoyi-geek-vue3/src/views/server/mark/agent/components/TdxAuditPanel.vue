<template>
  <div class="tdx-audit-panel">
    <el-card shadow="never" class="audit-filter-card">
      <el-form :inline="true" :model="filters" size="small" @submit.prevent>
        <el-form-item label="日期">
          <el-date-picker v-model="filters.date" type="date" value-format="YYYY-MM-DD" placeholder="全部日期" clearable />
        </el-form-item>
        <el-form-item label="线路">
          <el-select v-model="filters.line" clearable placeholder="全部线路" class="audit-line-select">
            <el-option label="线路1 / QMF" value="line1" />
            <el-option label="线路2 / 外部 API" value="line2" />
            <el-option label="线路3 / 失败复用" value="line3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" class="audit-status-select">
            <el-option label="queued" value="queued" />
            <el-option label="processing" value="processing" />
            <el-option label="completed" value="completed" />
            <el-option label="failed" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input v-model="filters.q" clearable placeholder="手机号 / 订单号 / wxid" class="audit-search-input" @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" :loading="loading" @click="load">查询</el-button>
          <el-button icon="Refresh" @click="reset">重置</el-button>
          <el-button icon="Download" :disabled="!rows.length" @click="exportCsv">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="audit-table-card">
      <div class="audit-summary">
        <span>当前记录 {{ filteredRows.length }}</span>
        <span>线路1 {{ stats.line1 }}</span>
        <span>线路2 {{ stats.line2 }}</span>
        <span>线路3 {{ stats.line3 }}</span>
        <span class="audit-summary__failed">失败 {{ stats.failed }}</span>
      </div>
      <el-table v-loading="loading" :data="filteredRows" border stripe size="small" height="calc(100vh - 310px)">
        <el-table-column label="本地任务" prop="id" min-width="170" show-overflow-tooltip />
        <el-table-column label="操作用户" prop="username" min-width="110" show-overflow-tooltip />
        <el-table-column label="手机号" prop="phone" min-width="120" />
        <el-table-column label="线路" prop="lineLabel" width="120" />
        <el-table-column label="上游服务" prop="provider" width="110" />
        <el-table-column label="上游订单号" prop="upstreamOrderId" min-width="180" show-overflow-tooltip />
        <el-table-column label="微信 wxid" prop="wxid" min-width="150" show-overflow-tooltip />
        <el-table-column label="使用次数" prop="usedCount" width="85" align="center" />
        <el-table-column label="任务状态" prop="status" width="100" align="center">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)" size="small" effect="plain">{{ scope.row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="失败步骤" prop="failureStep" min-width="125" show-overflow-tooltip />
        <el-table-column label="上游提示" prop="upstreamMessage" min-width="220" show-overflow-tooltip />
        <el-table-column label="记录时间" prop="capturedAt" min-width="155" />
        <el-table-column label="回包" width="70" align="center">
          <template #default="scope">
            <el-button v-if="scope.row.upstreamPayload" link type="primary" @click="showPayload(scope.row)">查看</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无审计记录" :image-size="72" /></template>
      </el-table>
    </el-card>

    <el-dialog v-model="payloadVisible" title="上游回包" width="min(760px, 92vw)" destroy-on-close>
      <pre v-if="payloadRow" class="audit-payload">{{ JSON.stringify(payloadRow.upstreamPayload, null, 2) }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { listMarkAgentTdxAudit } from '@/api/server/markAgent'

const loading = ref(false)
const rows = ref([])

function todayDate() {
  const parts = Object.fromEntries(new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).formatToParts(new Date()).map((part) => [part.type, part.value]))
  return `${parts.year}-${parts.month}-${parts.day}`
}

const filters = reactive({ date: todayDate(), line: '', status: '', q: '' })
const payloadVisible = ref(false)
const payloadRow = ref(null)

function text(...values) {
  for (const value of values) {
    if (value === 0) return '0'
    const result = String(value ?? '').trim()
    if (result) return result
  }
  return ''
}

function formatDate(value) {
  const raw = text(value)
  if (!raw) return ''
  const date = new Date(/(?:Z|[+-]\d{2}:?\d{2})$/i.test(raw) ? raw : raw.replace(' ', 'T') + '+08:00')
  if (Number.isNaN(date.getTime())) return raw
  return new Intl.DateTimeFormat('zh-CN', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(date).replaceAll('/', '-')
}

function dateKey(value) {
  const raw = text(value)
  if (!raw) return ''
  // Values generated by the executor are normally local China-time strings,
  // while imported/upstream records can be ISO timestamps with a timezone.
  // Convert timezone-qualified values before taking the date; otherwise a
  // late-night UTC value can be displayed as the next day in China while the
  // filter still compares against the UTC date prefix.
  const hasTimezone = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(raw)
  if (!hasTimezone) {
    const direct = raw.match(/^(\d{4}-\d{2}-\d{2})/)
    if (direct) return direct[1]
  }
  const formatted = formatDate(raw)
  return /^(\d{4}-\d{2}-\d{2})/.test(formatted) ? formatted.slice(0, 10) : ''
}

function normalize(task = {}) {
  const audit = task.result?.upstreamAudit || task.upstreamAudit || {}
  const line = text(audit.line, task.line, 'line1').toLowerCase()
  const capturedAtValue = audit.capturedAt || task.updatedAt || task.createdAt
  return {
    id: text(task.id),
    username: text(task.username, task.userName, task.operatorName, '未知用户'),
    phone: text(task.phone),
    line,
    lineLabel: line === 'line3' ? '线路3 / 失败复用' : line === 'line2' ? '线路2 / 外部 API' : '线路1 / QMF',
    provider: text(audit.provider, line === 'line2' ? '外部 API' : line === 'line3' ? '原订单复用' : 'QMF'),
    upstreamOrderId: text(audit.orderId, task.orderId),
    wxid: text(audit.wxid),
    usedCount: audit.usedCount ?? '',
    status: text(task.status),
    failureStep: text(audit.failureStep),
    upstreamMessage: text(audit.upstreamMessage, task.currentStep),
    capturedAt: formatDate(capturedAtValue),
    capturedDate: dateKey(capturedAtValue),
    upstreamPayload: audit.upstreamPayload || null
  }
}

const filteredRows = computed(() => rows.value.filter((row) => {
  if (filters.date && row.capturedDate !== filters.date) return false
  if (filters.line && row.line !== filters.line) return false
  if (filters.status && row.status !== filters.status) return false
  return true
}))
const stats = computed(() => ({
  line1: filteredRows.value.filter((row) => row.line === 'line1').length,
  line2: filteredRows.value.filter((row) => row.line === 'line2').length,
  line3: filteredRows.value.filter((row) => row.line === 'line3').length,
  failed: filteredRows.value.filter((row) => row.status === 'failed').length
}))

async function load() {
  loading.value = true
  try {
    const response = await listMarkAgentTdxAudit({ date: filters.date, status: filters.status, q: filters.q })
    const data = response?.data ?? response
    rows.value = Array.isArray(data) ? data.map(normalize) : []
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(filters, { date: todayDate(), line: '', status: '', q: '' })
  load()
}

function statusType(status) {
  return status === 'failed' ? 'danger' : status === 'completed' ? 'success' : 'warning'
}

function showPayload(row) {
  payloadRow.value = row
  payloadVisible.value = true
}

function exportCsv() {
  const fields = ['id', 'username', 'phone', 'lineLabel', 'provider', 'upstreamOrderId', 'wxid', 'usedCount', 'status', 'failureStep', 'upstreamMessage', 'capturedAt']
  const quote = (value) => `"${String(value ?? '').replaceAll('"', '""')}"`
  const content = [fields.join(','), ...filteredRows.value.map((row) => fields.map((field) => quote(row[field])).join(','))].join('\n')
  const url = URL.createObjectURL(new Blob([`\ufeff${content}`], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `tdx-audit-${filters.date || 'all'}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

onMounted(load)
</script>

<style scoped>
.tdx-audit-panel { padding: 0; }
.audit-filter-card { margin-bottom: 10px; }
.audit-filter-card :deep(.el-card__body) { padding: 12px 14px 2px; }
.audit-filter-card :deep(.el-form-item) { margin-bottom: 10px; }
.audit-line-select, .audit-status-select { width: 130px; }
.audit-search-input { width: 230px; }
.audit-table-card :deep(.el-card__body) { padding: 0 0 6px; }
.audit-summary { display: flex; flex-wrap: wrap; gap: 18px; padding: 10px 12px; color: var(--el-text-color-secondary); font-size: 12px; }
.audit-summary__failed { color: var(--el-color-danger); }
.audit-payload { max-height: 60vh; overflow: auto; padding: 12px; border-radius: 4px; background: #0f172a; color: #e2e8f0; font: 12px/1.5 ui-monospace, Consolas, monospace; white-space: pre-wrap; }
@media (max-width: 768px) { .audit-search-input { width: 180px; } }
</style>
