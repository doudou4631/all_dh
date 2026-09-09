<template>
  <div class="app-container mark-user-wallet-page">
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

    <el-card shadow="never">
      <template #header>
        <span>资金流水</span>
      </template>

      <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px" class="wallet-query-form">
        <el-form-item label="处理平台" prop="platformCode">
          <el-select v-model="queryParams.platformCode" placeholder="请选择平台" clearable filterable @change="handleQuery" style="width: 180px">
            <el-option v-for="item in platformOptions" :key="item.platformCode" :label="item.platformName" :value="item.platformCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="号码/固话" prop="phone">
          <el-input v-model="queryParams.phone" placeholder="手机号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
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
        <el-form-item class="wallet-query-actions">
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button link type="primary" @click="moreFilterVisible = true">更多筛选</el-button>
        </el-form-item>
      </el-form>

      <el-drawer v-model="moreFilterVisible" title="更多筛选" size="360px">
        <el-form :model="queryParams" label-width="84px">
          <el-form-item label="用户名">
            <el-input v-model="queryParams.userName" clearable placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker v-model="queryParams.params.beginTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择开始时间" style="width: 100%" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker v-model="queryParams.params.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择结束时间" style="width: 100%" />
          </el-form-item>
          <div class="wallet-filter-actions">
            <el-button @click="moreFilterVisible = false">取消</el-button>
            <el-button type="primary" @click="moreFilterVisible = false; handleQuery()">应用筛选</el-button>
          </div>
        </el-form>
      </el-drawer>

      <el-row :gutter="10" class="mb8">
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getLogList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="logList">
        <el-table-column label="序号" type="index" width="56" align="center" />
        <el-table-column label="处理号码" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            <div>{{ scope.row.phone || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="处理平台" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            <div>{{ scope.row.platformName || scope.row.platformCode || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="用户名" prop="userName" min-width="120" show-overflow-tooltip />
        <el-table-column label="变动前(分)" prop="balanceBefore" width="110" align="center" />
        <el-table-column label="变动金额(分)" prop="changeAmount" width="120" align="center">
          <template #default="scope">
            <span :class="changeAmountClass(scope.row.changeAmount)">
              {{ formatChangeAmount(scope.row.changeAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动后(分)" prop="balanceAfter" width="110" align="center" />
        <el-table-column label="变动类型" prop="bizType" width="100" align="center">
          <template #default="scope">
            <el-tag :type="bizTypeTagType(scope.row.bizType)" size="small">
              {{ bizTypeLabel(scope.row.bizType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单编号" prop="orderNo" min-width="145" show-overflow-tooltip />
        <el-table-column label="备注信息" prop="remark" min-width="180" show-overflow-tooltip />
        <el-table-column label="创建时间" min-width="160" align="center">
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
import { getMarkUserWalletSummary, listMarkUserWalletLog, listMarkUserPlatformPrice } from '@/api/server/markUser'

const { proxy } = getCurrentInstance()

const showSearch = ref(true)
const moreFilterVisible = ref(false)
const loading = ref(false)
const total = ref(0)
const summary = ref({})
const logList = ref([])
const platformOptions = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    bizType: null,
    orderId: null,
    platformCode: null,
    phone: null,
    userName: null,
    params: {}
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

function bizTypeLabel(value) {
  const map = {
    DEDUCT: '扣费',
    REFUND: '退款',
    ADJUST: '调整',
    TRANSFER_OUT: '下划充值',
    TRANSFER_IN: '上划充值'
  }
  return map[value] || value || '-'
}

function bizTypeTagType(value) {
  if (value === 'REFUND') return 'success'
  if (value === 'DEDUCT') return 'warning'
  return 'info'
}

function formatChangeAmount(value) {
  const amount = Number(value)
  if (!Number.isFinite(amount)) return value ?? '-'
  return amount > 0 ? `+${amount}` : String(amount)
}

function changeAmountClass(value) {
  const amount = Number(value)
  if (!Number.isFinite(amount)) return ''
  if (amount > 0) return 'amount-plus'
  if (amount < 0) return 'amount-minus'
  return ''
}

function loadSummary() {
  getMarkUserWalletSummary().then((res) => {
    summary.value = res.data || {}
  })
}

function loadPlatforms() {
  listMarkUserPlatformPrice().then((res) => {
    platformOptions.value = Array.isArray(res?.data) ? res.data : []
  }).catch(() => {
    platformOptions.value = []
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
  loadPlatforms()
  getLogList()
})
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}

.mb10 {
  margin-bottom: 10px;
}

.wallet-query-form {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 12px;
  margin-bottom: 4px;
  overflow-x: auto;
  white-space: nowrap;
}

.wallet-query-form :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 0;
  flex: 0 0 auto;
}

.wallet-query-form :deep(.el-input) {
  width: 190px;
}

.wallet-query-form :deep(.el-select) {
  width: 180px;
}

.wallet-query-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 1200px) {
  .wallet-query-form {
    flex-wrap: wrap;
    overflow-x: visible;
  }
}

.amount-plus {
  color: var(--el-color-success);
  font-weight: 600;
}

.amount-minus {
  color: var(--el-color-danger);
  font-weight: 600;
}
</style>
