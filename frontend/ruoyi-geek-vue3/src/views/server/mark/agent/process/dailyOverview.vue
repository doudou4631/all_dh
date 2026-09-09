<template>
  <div class="app-container daily-overview-page">
    <div class="filter-band">
      <el-form :model="queryParams" :inline="true" label-width="72px" @submit.prevent>
        <el-form-item label="处理日期">
          <AgentPastDateRangePicker v-model="dateRange" placeholder="请选择处理日期" />
        </el-form-item>
        <el-form-item label="用户">
          <el-select
            v-model="queryParams.userId"
            clearable
            filterable
            placeholder="全部用户"
            class="field-user"
            :loading="userLoading"
          >
            <el-option
              v-for="item in userOptions"
              :key="item.userId"
              :label="userLabel(item)"
              :value="item.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="平台">
          <el-select
            v-model="queryParams.platformCode"
            clearable
            filterable
            placeholder="全部平台"
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
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button icon="Download" :loading="exportLoading" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-toolbar">
      <span class="table-toolbar__count">共 {{ total }} 条用户日记录</span>
      <el-button icon="RefreshRight" text :loading="loading" @click="getList">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="overviewList" border stripe size="small">
      <el-table-column label="日期" prop="statDate" width="112" align="center" fixed="left" />
      <el-table-column label="用户" min-width="160" fixed="left" show-overflow-tooltip>
        <template #default="scope">
          <div class="user-cell">
            <span class="user-cell__name">{{ scope.row.userName || scope.row.userId }}</span>
            <span v-if="scope.row.nickName" class="user-cell__nick">{{ scope.row.nickName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="处理平台" min-width="360">
        <template #default="scope">
          <div class="platform-list">
            <el-tag
              v-for="platform in sortedPlatforms(scope.row.platforms)"
              :key="platform.platformCode"
              type="primary"
              effect="plain"
              class="platform-tag"
              @click="openDetail(scope.row, platform)"
            >
              {{ platform.platformName || platform.platformCode }} {{ platform.itemCount || 0 }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="平台数" prop="platformCount" width="78" align="center" />
      <el-table-column label="批次数" prop="orderCount" width="78" align="center" />
      <el-table-column label="号码数" prop="itemCount" width="78" align="center" />
      <el-table-column label="完成" prop="successCount" width="72" align="center">
        <template #default="scope"><span class="count-success">{{ scope.row.successCount || 0 }}</span></template>
      </el-table-column>
      <el-table-column label="失败" prop="failedCount" width="72" align="center">
        <template #default="scope"><span class="count-failed">{{ scope.row.failedCount || 0 }}</span></template>
      </el-table-column>
      <el-table-column label="处理中" prop="processingCount" width="78" align="center" />
      <el-table-column label="待处理" prop="pendingCount" width="78" align="center" />
      <el-table-column label="最后处理时间" width="158" align="center">
        <template #default="scope">{{ formatDateTime(scope.row.lastProcessedTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="84" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="openDetail(scope.row)">明细</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无用户日处理记录" :image-size="72" />
      </template>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :page-sizes="[20, 50, 100]"
      @pagination="getList"
    />

    <el-drawer v-model="detailVisible" :title="detailTitle" size="min(920px, 92vw)" destroy-on-close>
      <el-table v-loading="detailLoading" :data="detailList" border stripe size="small">
        <el-table-column label="平台" prop="platformName" min-width="110" show-overflow-tooltip />
        <el-table-column label="手机/固话" prop="phone" min-width="120" show-overflow-tooltip />
        <el-table-column label="处理状态" width="94" align="center">
          <template #default="scope">
            <el-tag :type="markItemProcessStatusTagType(scope.row.processStatus, scope.row)" size="small">
              {{ markItemProcessStatusLabel(scope.row.processStatus, scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理人" prop="processedBy" width="110" show-overflow-tooltip />
        <el-table-column label="批次编号" prop="orderNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="处理结果" min-width="150" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.processResult || scope.row.processNote || '-' }}</template>
        </el-table-column>
        <el-table-column label="处理时间" width="158" align="center">
          <template #default="scope">{{ formatDateTime(scope.row.processedTime || scope.row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="detailTotal > 0"
        :total="detailTotal"
        v-model:page="detailQuery.pageNum"
        v-model:limit="detailQuery.pageSize"
        :page-sizes="[20, 50, 100]"
        @pagination="loadDetail"
      />
    </el-drawer>
  </div>
</template>

<script setup name="MarkAgentDailyOverview">
import { getCurrentInstance, onMounted, reactive, ref } from 'vue'
import { listMarkAgentDownstreamSummary } from '@/api/server/markAgent'
import {
  listMarkAgentDailyOverview,
  listMarkAgentOrderItem,
  listMarkAgentPlatformOptions
} from '@/api/server/markAgent'
import {
  markItemProcessStatusLabel,
  markItemProcessStatusTagType
} from '@/utils/markProcessStatus'
import AgentPastDateRangePicker from '../components/AgentPastDateRangePicker.vue'

const { proxy } = getCurrentInstance()
const today = formatDate(new Date())
const loading = ref(false)
const exportLoading = ref(false)
const total = ref(0)
const overviewList = ref([])
const dateRange = ref([today, today])
const userLoading = ref(false)
const userOptions = ref([])
const platformOptions = ref([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailTitle = ref('处理明细')
const detailTotal = ref(0)
const detailList = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  userId: null,
  platformCode: null,
  params: {}
})

const detailQuery = reactive({
  pageNum: 1,
  pageSize: 20,
  userId: null,
  platformCode: null,
  params: {}
})

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function userLabel(user) {
  return user.nickName ? `${user.userName}（${user.nickName}）` : user.userName
}

function sortedPlatforms(platforms) {
  return [...(Array.isArray(platforms) ? platforms : [])]
    .sort((left, right) => Number(right.itemCount || 0) - Number(left.itemCount || 0))
}

function buildOverviewParams(overrides = {}) {
  const [beginTime, endTime] = dateRange.value || []
  return {
    ...queryParams,
    ...overrides,
    params: {
      beginTime: beginTime || null,
      endTime: endTime || null
    }
  }
}

function getList() {
  loading.value = true
  return listMarkAgentDailyOverview(buildOverviewParams())
    .then((res) => {
      overviewList.value = res.rows || []
      total.value = Number(res.total || 0)
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
  queryParams.userId = null
  queryParams.platformCode = null
  dateRange.value = [today, today]
  handleQuery()
}

function loadUsers() {
  userLoading.value = true
  return listMarkAgentDownstreamSummary({ pageNum: 1, pageSize: 500 })
    .then((res) => {
      const seen = new Set()
      userOptions.value = (res.rows || []).filter((item) => {
        if (!item?.userId || seen.has(item.userId)) return false
        seen.add(item.userId)
        return true
      })
    })
    .finally(() => {
      userLoading.value = false
    })
}

function loadPlatforms() {
  return listMarkAgentPlatformOptions()
    .then((res) => {
      platformOptions.value = Array.isArray(res.data) ? res.data : []
    })
    .catch(() => {
      platformOptions.value = []
    })
}

function openDetail(row, platform = null) {
  detailQuery.pageNum = 1
  detailQuery.userId = row.userId
  detailQuery.platformCode = platform?.platformCode || null
  detailQuery.params = { beginTime: row.statDate, endTime: row.statDate, dailyOverview: '1' }
  const platformName = platform?.platformName || '全部平台'
  detailTitle.value = `${row.statDate} ${row.userName || row.userId} · ${platformName}`
  detailVisible.value = true
  loadDetail()
}

function loadDetail() {
  detailLoading.value = true
  return listMarkAgentOrderItem({ ...detailQuery, params: { ...detailQuery.params } })
    .then((res) => {
      detailList.value = res.rows || []
      detailTotal.value = Number(res.total || 0)
    })
    .finally(() => {
      detailLoading.value = false
    })
}

function csvCell(value) {
  return `"${String(value ?? '').replace(/"/g, '""')}"`
}

function downloadCsv(filename, rows) {
  const content = `\ufeff${rows.map((row) => row.map(csvCell).join(',')).join('\n')}`
  const url = URL.createObjectURL(new Blob([content], { type: 'text/csv;charset=utf-8;' }))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

async function fetchAllOverviewRows() {
  const rows = []
  let pageNum = 1
  let totalCount = 0
  do {
    const res = await listMarkAgentDailyOverview(buildOverviewParams({ pageNum, pageSize: 500 }))
    const batch = res.rows || []
    totalCount = Number(res.total || 0)
    rows.push(...batch)
    if (!batch.length) break
    pageNum += 1
  } while (rows.length < totalCount)
  return rows
}

async function handleExport() {
  if (exportLoading.value) return
  exportLoading.value = true
  try {
    const rows = await fetchAllOverviewRows()
    if (!rows.length) {
      proxy.$modal.msgWarning('当前筛选条件下没有可导出数据')
      return
    }
    const header = ['日期', '用户', '昵称', '处理平台', '平台数', '批次数', '号码数', '完成', '失败', '处理中', '待处理', '最后处理时间']
    const data = rows.map((row) => [
      row.statDate,
      row.userName,
      row.nickName,
      sortedPlatforms(row.platforms).map((item) => `${item.platformName || item.platformCode} ${item.itemCount || 0}`).join('；'),
      row.platformCount,
      row.orderCount,
      row.itemCount,
      row.successCount,
      row.failedCount,
      row.processingCount,
      row.pendingCount,
      formatDateTime(row.lastProcessedTime)
    ])
    downloadCsv(`用户总览_${Date.now()}.csv`, [header, ...data])
    proxy.$modal.msgSuccess(`已导出 ${rows.length} 条记录`)
  } catch (error) {
    proxy.$modal.msgError('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => {
  Promise.all([loadUsers(), loadPlatforms()])
  getList()
})
</script>

<style scoped lang="scss">
.daily-overview-page {
  .filter-band {
    padding: 14px 16px 0;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 4px;
    background: var(--el-fill-color-blank);
  }

  .field-user {
    width: 220px;
  }

  .field-platform {
    width: 180px;
  }

  .table-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 44px;
  }

  .table-toolbar__count {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .user-cell {
    display: flex;
    flex-direction: column;
    min-width: 0;
    line-height: 1.4;
  }

  .user-cell__name {
    overflow: hidden;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .user-cell__nick {
    overflow: hidden;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .platform-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    padding: 3px 0;
  }

  .platform-tag {
    cursor: pointer;
  }

  .count-success {
    color: var(--el-color-success);
    font-weight: 600;
  }

  .count-failed {
    color: var(--el-color-danger);
    font-weight: 600;
  }
}

@media (max-width: 900px) {
  .daily-overview-page {
    .filter-band :deep(.el-form-item) {
      display: flex;
      margin-right: 0;
      width: 100%;
    }

    .field-user,
    .field-platform {
      width: 100%;
    }
  }
}
</style>
