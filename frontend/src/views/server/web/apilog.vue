<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="70px">
        <el-form-item label="来源IP">
          <el-input v-model="queryParams.ip" placeholder="请输入IP" clearable style="width: 220px" />
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
      <el-row :gutter="10" class="mb8">
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </el-row>

      <el-table v-loading="loading" :data="logList">
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="IP" align="center" min-width="150">
          <template #default="{ row }">
            {{ parseIp(row) }}
          </template>
        </el-table-column>
        <el-table-column label="设备ID" align="center" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ parseDeviceId(row) }}
          </template>
        </el-table-column>
        <el-table-column label="次数" align="center" width="90">
          <template #default="{ row }">
            {{ parseUsedAfter(row) }}
          </template>
        </el-table-column>
        <el-table-column label="手机号" align="center" prop="phone" min-width="140" />
        <el-table-column label="状态" align="center" width="100">
          <template #default="{ row }">
            <el-tag :type="row.requestStatus === '0' ? 'success' : 'danger'" size="small">
              {{ row.requestStatus === '0' ? '成功' : '失败' }}
            </el-tag>
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
import { listFreeQueryLogs } from '@/api/server/freeQueryApi'

const logList = ref([])
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const dateRange = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  ip: ''
})

function parseFromRequestParams(row, key) {
  const text = row?.requestParams || ''
  const parts = String(text).split(',')
  for (const p of parts) {
    const [k, v] = p.split('=')
    if (String(k).trim() === key) {
      return String(v || '').trim()
    }
  }
  return ''
}

function parseIp(row) {
  return parseFromRequestParams(row, 'ip') || row?.createBy?.replace('free-ip-', '') || '-'
}
function parseDeviceId(row) {
  return parseFromRequestParams(row, 'deviceId') || '-'
}

function parseUsedAfter(row) {
  return parseFromRequestParams(row, 'usedAfter') || '-'
}

function buildParams() {
  const params = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
    ip: queryParams.ip || undefined
  }
  if (dateRange.value?.length === 2) {
    params.beginTime = dateRange.value[0]
    params.endTime = dateRange.value[1]
  }
  return params
}

async function getList() {
  loading.value = true
  try {
    const res = await listFreeQueryLogs(buildParams())
    logList.value = res.rows || []
    total.value = res.total || 0
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
  dateRange.value = []
  handleQuery()
}

getList()
</script>

