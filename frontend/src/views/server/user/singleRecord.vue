<template>
  <div class="app-container batch-record-page">
    <el-card shadow="never" body-class="search-card">
      <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="72px">
        <el-form-item label="日期">
          <el-date-picker
            v-model="dateRange"
            value-format="YYYY-MM-DD"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="queryParams.phone"
            placeholder="输入号码"
            clearable
            style="width: 160px"
            maxlength="20"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="查询类型" prop="queryType">
          <el-select v-model="queryParams.queryType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="dict in use_exec_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="请求状态" prop="requestStatus">
          <el-select v-model="queryParams.requestStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="dict in sys_common_status" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" :loading="loading" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="mt10 batch-record-table-card">
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete"
            v-hasPermi="['server:apiRecord:remove']">删除</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="warning" plain icon="Download" @click="handleExport"
            v-hasPermi="['server:apiRecord:export']">导出</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </el-row>

      <el-table
        v-loading="loading"
        :data="recordList"
        border
        stripe
        size="small"
        class="task-manage-table record-aggregate-table"
        row-key="rowUid"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" align="center" reserve-selection />
        <el-table-column label="序号" align="center" type="index" width="50" min-width="80" show-overflow-tooltip />
        <el-table-column label="手机号" align="center" min-width="120" show-overflow-tooltip>
          <template #default="scope">
            {{ displayPhone(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="类型" align="center" min-width="88">
          <template #default="scope">
            <el-tag :type="queryTypeElTagType(scope.row)" size="small" effect="light">
              {{ queryTypeLabel(scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="查询结果" align="center" min-width="160" show-overflow-tooltip prop="resultSummary" />
        <el-table-column label="查询时间" align="center" min-width="158">
          <template #default="scope">
            {{ formatQueryTime(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="100" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button size="small" class="detail-action-btn" @click="openDetail(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
        :page-sizes="[10, 20, 50]"
        :default-page-size="20"
      />
    </el-card>

    <el-dialog
      v-model="detailVisible"
      title="查询详情"
      width="640px"
      append-to-body
      destroy-on-close
      class="record-detail-dialog"
    >
      <div v-loading="detailLoading" class="detail-dialog-inner">
        <div class="detail-meta">
          <div class="detail-meta-row">
            <span class="detail-meta-label">手机号</span>
            <span class="detail-meta-value">{{ displayPhone(detailGroup) }}</span>
          </div>
          <div class="detail-meta-row">
            <span class="detail-meta-label">查询时间</span>
            <span class="detail-meta-value">{{ formatQueryTime(detailGroup) }}</span>
          </div>
        </div>
        <div class="detail-section-title">各平台结果：</div>
        <el-table :data="detailPlatformRows" border stripe size="small" class="platform-detail-table" max-height="420">
          <el-table-column label="平台" prop="platformName" min-width="120" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag
                :type="scope.row.marked ? 'danger' : 'success'"
                size="small"
                effect="light"
                class="platform-status-tag"
              >
                {{ scope.row.statusText }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="结果" prop="resultText" min-width="140" show-overflow-tooltip />
        </el-table>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="SingleRecord">
import { listSingleQueryBatch, getSingleQueryBatchDetail, delApiRecord } from '@/api/server/apiRecord'
import { parseTime } from '@/utils/ruoyi'
import { useRoute, useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()
const { use_exec_type, sys_common_status } = proxy.useDict('use_exec_type', 'sys_common_status')
const route = useRoute()
const router = useRouter()

const queryRef = ref(null)
const loading = ref(false)
const showSearch = ref(true)
const dateRange = ref([])
const recordList = ref([])
const total = ref(0)
const ids = ref([])
const selectedRows = ref([])
const multiple = ref(true)

const queryParams = ref({
  pageNum: 1,
  pageSize: 20,
  phone: undefined,
  taskId: undefined,
  queryType: undefined,
  requestStatus: undefined,
  params: {}
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailGroup = ref(null)
const detailPlatformRows = ref([])

function withRowUid(rows) {
  const list = rows ?? []
  return list.map((row) => ({
    ...row,
    rowUid: `${row.phone ?? ''}__${row.batchKey ?? ''}`
  }))
}

/** 展示号码时去掉中间的连字符（固话常见 区号-号码 格式） */
function displayPhone(row) {
  if (!row) return '-'
  const raw = row.phone ?? row.mobile ?? row.phonenumber
  if (raw == null || raw === '') return '-'
  return String(raw).replace(/-/g, '')
}

function formatQueryTime(row) {
  if (!row) return '-'
  const t = row.queryTime ?? row.createTime
  if (!t) return '-'
  return parseTime(t, '{y}/{m}/{d} {h}:{i}:{s}')
}

function queryTypeLabel(row) {
  const dictList = use_exec_type.value || []
  const v = row.queryType ?? row.type
  const hit = dictList.find((d) => String(d.value) === String(v))
  return hit?.label || '单条'
}

function queryTypeElTagType(row) {
  const label = queryTypeLabel(row)
  if (label.includes('批量')) return 'success'
  if (label.includes('单条') || label.includes('单次')) return 'primary'
  return 'info'
}

function parseResponsePayload(raw) {
  if (raw == null) return null
  if (typeof raw === 'object' && !Array.isArray(raw)) return raw
  try {
    return JSON.parse(String(raw))
  } catch {
    return null
  }
}

/**
 * 查询结果展示：含 yes → 去掉 yes / yes- 前缀后展示（无后缀则「有标记」）；否则含 no →「无标记」；其它原样
 * yes 优先于 no（避免 yes-xxx 中含字母 no 误判）
 */
function parseResultDisplayAndMarked(raw) {
  if (raw == null || String(raw).trim() === '') {
    return { marked: false, resultText: '—' }
  }
  const s = String(raw).trim()
  const lower = s.toLowerCase()

  const yesIdx = lower.indexOf('yes')
  if (yesIdx >= 0) {
    let tail = s.slice(yesIdx + 3)
    if (tail.startsWith('-')) {
      tail = tail.slice(1)
    }
    tail = tail.trim()
    return {
      marked: true,
      resultText: tail || '有标记'
    }
  }

  if (lower.includes('no')) {
    return { marked: false, resultText: '无标记' }
  }

  return { marked: false, resultText: s }
}

/**
 * 详情接口与列表接口字段可能不一致：列表用 rows，详情可能把数组放在 data 或 rows。
 * 只读 res.data 会导致部分记录（尤其批量）明细始终为空。
 */
function extractSingleBatchDetailList(res) {
  if (!res) return []
  const payload = res.data
  if (Array.isArray(payload)) return payload
  if (payload != null && typeof payload === 'object') {
    if (Array.isArray(payload.rows)) return payload.rows
    if (Array.isArray(payload.list)) return payload.list
    if (Array.isArray(payload.records)) return payload.records
  }
  if (Array.isArray(res.rows)) return res.rows
  return []
}

function buildPlatformRowsFromRecords(records) {
  const rows = []
  for (const rec of records) {
    if (!rec) continue
    const name = rec.platformName || '—'
    const rs = rec.results == null ? '' : String(rec.results).trim()
    let marked = false
    let resultText = '—'
    if (rs) {
      const r = parseResultDisplayAndMarked(rec.results)
      marked = r.marked
      resultText = r.resultText
    } else {
      const payload = parseResponsePayload(rec.responseResult)
      const pr = payload?.platformResults?.[0]
      const status = pr?.status
      const raw =
        status != null && String(status) !== ''
          ? String(status)
          : pr?.result != null
            ? String(pr.result)
            : ''
      if (!raw) {
        marked = false
        resultText = '—'
      } else {
        const r = parseResultDisplayAndMarked(raw)
        marked = r.marked
        resultText = r.resultText
      }
    }
    rows.push({
      platformName: name,
      marked,
      statusText: marked ? '有标记' : '无标记',
      resultText
    })
  }
  rows.sort((a, b) => a.platformName.localeCompare(b.platformName, 'zh-CN'))
  return rows
}

function resolveSingleQueryPath() {
  const p = route.path || ''
  if (/singleRecord/i.test(p)) {
    return p.replace(/singleRecord\/?$/i, 'single')
  }
  const i = p.lastIndexOf('/')
  if (i > 0) {
    return `${p.slice(0, i)}/single`
  }
  return '/single'
}

function goSingleQuery() {
  router.push(resolveSingleQueryPath())
}

function normalizeDeleteIds(raw) {
  return raw
    .map((id) => Number(id))
    .filter((n) => Number.isInteger(n) && n > 0)
}

function handleSelectionChange(selection) {
  selectedRows.value = selection || []
  const idSet = new Set()
  for (const row of selection) {
    const parts = String(row.memberIds || '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
    for (const p of parts) idSet.add(p)
  }
  ids.value = [...idSet]
  multiple.value = !ids.value.length
}

function handleDelete() {
  if (!ids.value.length) {
    proxy.$modal.msgWarning('请先选择要删除的记录')
    return
  }
  const idList = normalizeDeleteIds(ids.value)
  if (!idList.length) {
    proxy.$modal.msgWarning('所选记录主键 id 无效，无法删除')
    return
  }
  proxy.$modal
    .confirm('是否确认删除选中的查询记录？')
    .then(() => delApiRecord(idList.join(',')))
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('删除成功')
    })
    .catch(() => {})
}

function buildListQuery() {
  const base = {
    ...queryParams.value,
    params: { ...(queryParams.value.params || {}) }
  }
  const q = proxy.addDateRange(base, dateRange.value)
  if (!q.phone || String(q.phone).trim() === '') {
    delete q.phone
  }
  if (!q.taskId || String(q.taskId).trim() === '') {
    delete q.taskId
  }
  if (q.queryType === undefined || q.queryType === null || String(q.queryType).trim() === '') {
    delete q.queryType
  }
  return q
}

function handleExport() {
  if (selectedRows.value.length !== 1) {
    proxy.$modal.msgWarning('每次只允许导出一行记录，请勾选1条数据')
    return
  }
  const row = selectedRows.value[0] || {}
  const phone = String(row.phone ?? row.mobile ?? row.phonenumber ?? '').trim()
  const taskId = String(row.taskId ?? row.task_id ?? row.batchKey ?? '').trim()
  if (!phone) {
    proxy.$modal.msgWarning('当前记录缺少号码，无法导出')
    return
  }
  if (!taskId) {
    proxy.$modal.msgWarning('当前记录缺少任务ID，无法导出')
    return
  }
  try {
    proxy.download(
      'server/apiRecord/export',
      { phone, taskId },
      `查询记录_${phone}_${new Date().getTime()}.xlsx`
    )
    proxy.$modal.msgSuccess('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    proxy.$modal.msgError('导出失败')
  }
}

function getList() {
  loading.value = true
  listSingleQueryBatch(buildListQuery())
    .then((res) => {
      recordList.value = withRowUid(res.rows ?? [])
      total.value = res.total ?? 0
    })
    .catch(() => {
      recordList.value = []
      total.value = 0
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  dateRange.value = []
  queryParams.value.phone = undefined
  queryParams.value.taskId = undefined
  queryParams.value.queryType = undefined
  queryParams.value.requestStatus = undefined
  queryParams.value.pageNum = 1
  queryParams.value.pageSize = 20
  queryParams.value.params = {}
  proxy.resetForm('queryRef')
  getList()
}

async function openDetail(row) {
  detailGroup.value = { ...row }
  detailPlatformRows.value = []
  detailVisible.value = true
  detailLoading.value = true
  try {
    // 批量任务常与 taskId 关联；列表若未带 batchKey，用 taskId 兜底
    const batchKey = row.batchKey ?? row.taskId ?? row.task_id
    const res = await getSingleQueryBatchDetail(displayPhone(row), batchKey)
    const list = extractSingleBatchDetailList(res)
    detailPlatformRows.value = buildPlatformRowsFromRecords(list)
  } catch {
    proxy.$modal?.msgError?.('获取详情失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.batch-record-page {
  width: 100%;
}

.task-manage-table {
  width: 100%;
}

.batch-record-table-card :deep(.el-card__body) {
  padding-top: 12px;
}

.record-aggregate-table :deep(.el-table td.el-table__cell),
.record-aggregate-table :deep(.el-table th.el-table__cell) {
  padding: 8px 10px;
}

.record-aggregate-table :deep(.el-table .cell) {
  line-height: 1.4;
  font-size: 13px;
}

.record-aggregate-table :deep(.el-table__header th.el-table__cell) {
  background: #f5f7fa;
  color: #303133;
  font-weight: 600;
}

.detail-action-btn {
  padding: 5px 14px;
  background: #fff;
  border: 1px solid #dcdfe6;
}

.detail-dialog-inner {
  min-width: 0;
}

.detail-meta {
  margin-bottom: 14px;
}

.detail-meta-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 14px;
}

.detail-meta-label {
  color: #606266;
  min-width: 72px;
}

.detail-meta-value {
  color: #303133;
  font-weight: 500;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.platform-detail-table :deep(.el-table .cell) {
  font-size: 13px;
}

.platform-status-tag.el-tag--danger {
  --el-tag-text-color: #f56c6c;
  --el-tag-bg-color: #fef0f0;
  --el-tag-border-color: #fde2e2;
}

.platform-status-tag.el-tag--success {
  --el-tag-text-color: #67c23a;
  --el-tag-bg-color: #f0f9eb;
  --el-tag-border-color: #e1f3d8;
}

.mb8 {
  margin-bottom: 8px;
}

.mt10 {
  margin-top: 10px;
}

.record-detail-dialog :deep(.el-dialog__body) {
  padding-top: 12px;
}
</style>
