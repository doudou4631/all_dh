<template>
  <div class="app-container ip-record-page">
    <el-card shadow="never" body-class="search-card">
      <el-form ref="queryRef" :model="queryParams" label-position="top" class="filter-form">
        <el-form-item label="IP地址" prop="ipAddr">
          <el-input v-model="queryParams.ipAddr" placeholder="请输入IP地址" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="查询号码" prop="queryPhone">
          <el-input v-model="queryParams.queryPhone" placeholder="请输入手机或座机号码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="标记筛选" prop="markedOnly" class="marked-filter">
          <el-checkbox v-model="queryParams.markedOnly" true-value="1" false-value="0">仅看有标记</el-checkbox>
        </el-form-item>
        <el-form-item label=" " class="filter-actions">
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="record-panel">
      <template #header>
        <div class="panel-header">
          <div class="panel-title">
            <span>查询日志列表</span>
            <span class="record-count">共 {{ total }} 条</span>
          </div>
          <el-button text :icon="Refresh" :loading="loading" @click="getList">刷新</el-button>
        </div>
      </template>

      <div class="table-wrap">
      <el-table
        v-loading="loading"
        :data="records"
        border
        stripe
        size="small"
        table-layout="fixed"
        empty-text="暂无查询记录"
        class="record-table"
      >
        <el-table-column label="序号" type="index" width="64" align="center" :index="tableIndex" />
        <el-table-column label="IP地址" prop="ipAddr" min-width="170" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.ipAddr || '-' }}</template>
        </el-table-column>
        <el-table-column label="查询号码" prop="queryPhone" width="160" align="center">
          <template #default="scope">
            <span class="phone-number">{{ scope.row.queryPhone || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="查询时间" prop="createTime" width="188" align="center">
          <template #default="scope">
            <span class="record-time">{{ formatRecordTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="耗时" prop="responseTime" width="100" align="center">
          <template #default="scope">{{ formatDuration(scope.row.responseTime) }}</template>
        </el-table-column>
        <el-table-column label="查询状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.queryStatus === '0' ? 'success' : 'danger'" effect="plain">
              {{ scope.row.queryStatus === '0' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标记结果" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.hasResult === '1'" type="warning" effect="plain">
              {{ scope.row.markedCount }} 个平台
            </el-tag>
            <span v-else class="empty-result">暂无标记</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="scope">
            <el-button link type="primary" :icon="View" @click="showDetails(scope.row)"
              v-hasPermi="['freeQuery:ipRecord:query']">标记详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <el-dialog v-model="detailOpen" title="号码标记详情" width="620px" append-to-body destroy-on-close>
      <div class="detail-summary">
        <div><span>查询号码</span><strong>{{ currentRecord.queryPhone || '-' }}</strong></div>
        <div><span>IP地址</span><strong>{{ currentRecord.ipAddr || '-' }}</strong></div>
      </div>

      <el-table v-if="markDetails.length" v-loading="detailLoading" :data="markDetails" border>
        <el-table-column label="标记平台" prop="platformName" min-width="180" />
        <el-table-column label="标记类型" prop="markType" min-width="280">
          <template #default="scope">
            <span class="mark-type">{{ scope.row.markType || '存在标记' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else v-loading="detailLoading" description="暂无标记" :image-size="90" />

      <template #footer>
        <el-button @click="detailOpen = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FreeQueryIpRecord">
import { Refresh, Search, View } from '@element-plus/icons-vue'
import { getFreeQueryIpRecordDetails, listFreeQueryIpRecords } from '@/api/server/freeQueryIpRecord'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const detailLoading = ref(false)
const detailOpen = ref(false)
const records = ref([])
const markDetails = ref([])
const total = ref(0)
const currentRecord = reactive({ id: null, ipAddr: '', queryPhone: '' })
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  ipAddr: '',
  queryPhone: '',
  markedOnly: '0'
})

async function getList() {
  loading.value = true
  try {
    const response = await listFreeQueryIpRecords(queryParams)
    records.value = response.rows || []
    total.value = response.total || 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.markedOnly = '0'
  handleQuery()
}

async function showDetails(row) {
  Object.assign(currentRecord, row)
  markDetails.value = []
  detailOpen.value = true
  detailLoading.value = true
  try {
    const response = await getFreeQueryIpRecordDetails(row.id)
    markDetails.value = response.data || []
  } finally {
    detailLoading.value = false
  }
}

function formatDuration(value) {
  const milliseconds = Number(value || 0)
  if (milliseconds < 1000) return `${milliseconds} ms`
  return `${(milliseconds / 1000).toFixed(2)} s`
}

function formatRecordTime(value) {
  if (!value) return '-'
  const matched = String(value).match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2}:\d{2})/)
  return matched ? `${matched[1]} ${matched[2]}` : proxy.parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}')
}

function tableIndex(index) {
  return (queryParams.pageNum - 1) * queryParams.pageSize + index + 1
}

getList()
</script>

<style scoped>
.ip-record-page :deep(.el-card) {
  border-radius: 6px;
}

.filter-form {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(260px, 1.4fr) 150px auto;
  align-items: end;
  gap: 0 18px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-form :deep(.el-form-item__label) {
  height: 30px;
  padding: 0;
  font-weight: 500;
  line-height: 22px;
}

.marked-filter :deep(.el-form-item__content),
.filter-actions :deep(.el-form-item__content) {
  min-height: 32px;
}

.filter-actions :deep(.el-form-item__content) {
  flex-wrap: nowrap;
}

.record-panel {
  margin-top: 12px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-weight: 600;
}

.record-count {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  font-weight: 400;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.record-table {
  min-width: 1050px;
}

.record-table :deep(.el-table__header th) {
  height: 42px;
  color: var(--el-text-color-primary);
  background: var(--el-fill-color-light);
}

.record-table :deep(.el-table__row td) {
  height: 48px;
}

.record-table :deep(.cell) {
  line-height: 22px;
}

.phone-number {
  font-family: Consolas, monospace;
  color: var(--el-text-color-primary);
}

.record-time {
  white-space: nowrap;
}

.empty-result {
  color: var(--el-text-color-secondary);
}

.detail-summary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
}

.detail-summary div {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.detail-summary span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.detail-summary strong,
.mark-type {
  overflow-wrap: anywhere;
}

@media (max-width: 1100px) {
  .filter-form {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
    row-gap: 14px;
  }
}

@media (max-width: 680px) {
  .filter-form {
    grid-template-columns: 1fr;
    row-gap: 12px;
  }

  .filter-actions :deep(.el-form-item__label) {
    display: none;
  }

  .filter-actions :deep(.el-button) {
    flex: 1;
  }

  .detail-summary {
    grid-template-columns: 1fr;
  }
}
</style>
