<template>
  <div class="app-container">
    <el-card shadow="never" class="mb10">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="8">
          <el-statistic title="当前积分" :value="summary.pointsBalance || 0" />
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-statistic title="累计扣费" :value="summary.totalDeductAmount || 0" />
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-statistic title="累计退款" :value="summary.totalRefundAmount || 0" />
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" class="mb10">
      <template #header>
        <span>平台单价</span>
      </template>
      <el-table :data="summary.platformPrices || []" max-height="260">
        <el-table-column label="序号" type="index" width="56" align="center" />
        <el-table-column label="平台编码" prop="platformCode" min-width="160" />
        <el-table-column label="平台名称" prop="platformName" min-width="140" />
        <el-table-column label="单价" prop="unitPrice" width="100" align="center" />
      </el-table>
    </el-card>

    <el-card shadow="never" body-class="search-card">
      <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
        <el-form-item label="业务类型" prop="bizType">
          <el-select v-model="queryParams.bizType" clearable placeholder="请选择" style="width: 150px;">
            <el-option label="扣费" value="DEDUCT" />
            <el-option label="退款" value="REFUND" />
            <el-option label="调整" value="ADJUST" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单ID" prop="orderId">
          <el-input v-model="queryParams.orderId" placeholder="请输入订单ID" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="mt10">
      <el-row :gutter="10" class="mb8">
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getLogList"></right-toolbar>
      </el-row>
      <el-table v-loading="loading" :data="logList">
        <el-table-column label="序号" type="index" width="56" align="center" />
        <el-table-column label="业务类型" prop="bizType" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.bizType === 'REFUND' ? 'success' : (scope.row.bizType === 'DEDUCT' ? 'warning' : 'info')" size="small">
              {{ scope.row.bizType || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单ID" prop="orderId" min-width="120" />
        <el-table-column label="明细ID" prop="orderItemId" min-width="120" />
        <el-table-column label="变动金额" prop="changeAmount" width="110" align="center" />
        <el-table-column label="变动前" prop="balanceBefore" width="100" align="center" />
        <el-table-column label="变动后" prop="balanceAfter" width="100" align="center" />
        <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip />
        <el-table-column label="时间" min-width="160" align="center">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getLogList"
      />
    </el-card>
  </div>
</template>

<script setup name="MarkUserWallet">
import { getMarkUserWalletSummary, listMarkUserWalletLog } from '@/api/server/markUser'

const { proxy } = getCurrentInstance()

const showSearch = ref(true)
const loading = ref(false)
const total = ref(0)
const summary = ref({})
const logList = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    bizType: null,
    orderId: null
  }
})

const { queryParams } = toRefs(data)

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function loadSummary() {
  getMarkUserWalletSummary().then((res) => {
    summary.value = res.data || {}
  })
}

function getLogList() {
  loading.value = true
  listMarkUserWalletLog(queryParams.value).then((res) => {
    logList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getLogList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

onMounted(() => {
  loadSummary()
  getLogList()
})
</script>

<style scoped>
.mt10 {
  margin-top: 10px;
}

.mb8 {
  margin-bottom: 8px;
}

.mb10 {
  margin-bottom: 10px;
}
</style>
