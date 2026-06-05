<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="70px">
        <el-form-item label="来源IP">
          <el-input v-model="queryParams.ip" placeholder="请输入IP" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="queryParams.phone" placeholder="请输入手机号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="查询时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            range-separator="-"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="mt10">
      <el-row :gutter="12" class="metric-row">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">总查询量</div>
            <div class="metric-value">{{ dashboardOverview.totalQuery }}</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">当天查询</div>
            <div class="metric-value">{{ dashboardOverview.todayQuery }}</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">成功率</div>
            <div class="metric-value">{{ dashboardSuccessRate }}</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">失败数</div>
            <div class="metric-value">{{ dashboardOverview.failedCount }}</div>
          </el-card>
        </el-col>
      </el-row>
      <el-row :gutter="10" class="mb8">
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </el-row>

      <el-table v-loading="loading" :data="logList">
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="查询来源" align="center" width="110">
          <template #default="{ row }">
            <el-tag :type="getSourceTypeTag(row).type" size="small">
              {{ getSourceTypeTag(row).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="IP" align="center" min-width="150">
          <template #default="{ row }">
            {{ parseIp(row) }}
          </template>
        </el-table-column>
        <el-table-column label="设备/浏览器指纹ID" align="center" width="250">
          <template #default="{ row }">
            <el-tooltip :content="parseDeviceId(row)" placement="top" :disabled="parseDeviceId(row) === '-'">
              <span class="ellipsis-cell">{{ formatDeviceIdShort(parseDeviceId(row)) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="IP当日已用次数" align="center" width="130">
          <template #default="{ row }">
            {{ parseUsedAfter(row) }}
          </template>
        </el-table-column>
        <el-table-column label="手机号" align="center" prop="phone" min-width="140" />
        <el-table-column label="批次号" align="center" prop="taskId" width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.taskId || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="耗时(ms)" align="center" width="100">
          <template #default="{ row }">
            {{ row.requestTime ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="查询时间" align="center" prop="createTime" min-width="170" />
        <el-table-column label="结果" align="center" prop="results" min-width="220" show-overflow-tooltip />
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script setup name="FreeQueryLog">
import { getFreeQueryLogsDashboard, listFreeQueryLogs } from '@/api/server/freeQueryApi'

const logList = ref([])
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const dateRange = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  ip: '',
  phone: ''
})
const defaultOverview = () => ({
  totalQuery: 0,
  successCount: 0,
  failedCount: 0,
  successRate: 0,
  p95Ms: 0,
  todayQuery: 0
})
const dashboardOverview = ref(defaultOverview())
const dashboardSuccessRate = computed(() => `${Number(dashboardOverview.value?.successRate || 0).toFixed(1)}%`)

function parseFromRequestParams(row, key) {
  const text = row?.requestParams || ''
  const parts = String(text).split(',')
  for (const p of parts) {
    const idx = p.indexOf('=')
    if (idx < 0) continue
    const k = p.slice(0, idx).trim()
    const v = p.slice(idx + 1).trim()
    if (k === key) {
      return v
    }
  }
  return ''
}
function parseIp(row) {
  return row?.ipAddr || parseFromRequestParams(row, 'ip') || row?.createBy?.replace('free-ip-', '') || '-'
}
function parseDeviceId(row) {
  return row?.deviceId || parseFromRequestParams(row, 'deviceId') || '-'
}
function parseUsedAfter(row) {
  if (row?.usedAfter !== null && row?.usedAfter !== undefined && `${row.usedAfter}` !== '') {
    return row.usedAfter
  }
  return parseFromRequestParams(row, 'usedAfter') || '-'
}
function parseSourceType(row) {
  return row?.sourceType || parseFromRequestParams(row, 'sourceType') || ''
}
function formatDeviceIdShort(value) {
  const text = String(value || '')
  if (!text || text === '-') return '-'
  if (text.length <= 24) return text
  return `${text.slice(0, 12)}...${text.slice(-8)}`
}
function getSourceTypeTag(row) {
  const raw = parseSourceType(row).toUpperCase()
  if (raw === 'FREE_BATCH') return { label: '批量查询', type: 'primary' }
  if (raw === 'FREE_SINGLE') return { label: '免费查询', type: 'success' }
  const deviceId = parseDeviceId(row)
  if (deviceId.startsWith('fqu#') || deviceId.startsWith('mark-user-')) {
    return { label: '批量查询', type: 'primary' }
  }
  if (row?.queryType === '9' && String(row?.createBy || '').startsWith('free-ip-')) {
    return { label: '免费查询', type: 'success' }
  }
  return { label: '未知', type: 'info' }
}
function buildFilterParams() {
  const params = {
    ip: queryParams.ip || undefined,
    phone: queryParams.phone || undefined
  }
  if (dateRange.value?.length === 2) {
    params.beginTime = dateRange.value[0]
    params.endTime = dateRange.value[1]
  }
  return params
}
function buildListParams() {
  return {
    ...buildFilterParams(),
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize
  }
}
async function getList() {
  loading.value = true
  try {
    const res = await listFreeQueryLogs(buildListParams())
    logList.value = res.rows || []
    total.value = Number(res.total || 0)
    try {
      const dashboardRes = await getFreeQueryLogsDashboard(buildFilterParams())
      const overview = dashboardRes?.data?.overview || {}
      dashboardOverview.value = {
        ...defaultOverview(),
        ...overview,
        totalQuery: Number(overview.totalQuery ?? total.value ?? 0)
      }
      total.value = dashboardOverview.value.totalQuery
    } catch {
      dashboardOverview.value = {
        ...defaultOverview(),
        totalQuery: Number(total.value || 0)
      }
    }
  } finally {
    loading.value = false
  }
}
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}
function resetQuery() {
  queryParams.ip = ''
  queryParams.phone = ''
  dateRange.value = []
  handleQuery()
}
getList()
</script>
<style scoped>
.ellipsis-cell {
  display: inline-block;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
.metric-row {
  margin-bottom: 12px;
}
.metric-card :deep(.el-card__body) {
  padding: 12px 14px;
}
.metric-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
}
.metric-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
}
</style>