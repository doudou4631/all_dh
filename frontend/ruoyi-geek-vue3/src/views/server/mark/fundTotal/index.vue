<template>
  <div class="app-container fund-total-page">
    <div class="page-heading">
      <div>
        <h2>资金明细（总）</h2>
        <p>全部用户的标记业务充值、消费、退款与转账流水</p>
      </div>
      <el-button :icon="Refresh" circle title="刷新" @click="loadData" />
    </div>

    <div class="summary-grid" v-loading="summaryLoading">
      <div v-for="item in summaryItems" :key="item.key" class="summary-item">
        <span>{{ item.label }}</span>
        <strong :class="item.className">{{ formatSummary(item.value) }}</strong>
      </div>
    </div>

    <div v-show="showSearch" class="filter-band">
      <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="72px">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="queryParams.userName" placeholder="请输入用户名" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="queryParams.userId" placeholder="请输入用户ID" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="平台" prop="platformCode">
          <el-select v-model="queryParams.platformCode" placeholder="全部平台" clearable filterable style="width: 180px">
            <el-option v-for="item in platformOptions" :key="item.platformCode" :label="item.platformName || item.platformCode" :value="item.platformCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="流水类型" prop="bizType">
          <el-select v-model="queryParams.bizType" placeholder="全部类型" clearable style="width: 160px">
            <el-option v-for="item in bizTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 250px"
          />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button link type="primary" @click="moreFilterVisible = true">更多筛选</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-drawer v-model="moreFilterVisible" title="更多筛选" size="380px">
      <el-form :model="queryParams" label-width="84px">
        <el-form-item label="处理号码" prop="phone">
          <el-input v-model="queryParams.phone" placeholder="手机号或固话" clearable />
        </el-form-item>
        <el-form-item label="订单ID" prop="orderId">
          <el-input v-model="queryParams.orderId" placeholder="请输入订单ID" clearable />
        </el-form-item>
        <el-form-item label="操作人" prop="createBy">
          <el-input v-model="queryParams.createBy" placeholder="请输入操作账号" clearable />
        </el-form-item>
        <div class="drawer-actions">
          <el-button @click="moreFilterVisible = false">取消</el-button>
          <el-button type="primary" @click="applyMoreFilters">应用筛选</el-button>
        </div>
      </el-form>
    </el-drawer>

    <div class="table-toolbar">
      <span>共 {{ total }} 条流水</span>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="loadData" />
    </div>

    <el-table v-loading="loading" :data="logList" border>
      <el-table-column label="序号" type="index" width="56" align="center" />
      <el-table-column label="用户名" prop="userName" min-width="125" show-overflow-tooltip />
      <el-table-column label="用户ID" prop="userId" min-width="105" show-overflow-tooltip />
      <el-table-column label="处理平台" min-width="145" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.platformName || scope.row.platformCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="流水类型" width="110" align="center">
        <template #default="scope">
          <el-tag :type="bizTypeTagType(scope.row)" size="small">{{ bizTypeLabel(scope.row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="变动前" prop="balanceBefore" width="100" align="right" />
      <el-table-column label="变动数量" width="110" align="right">
        <template #default="scope">
          <span :class="amountClass(scope.row.changeAmount)">{{ formatSignedAmount(scope.row.changeAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="变动后" prop="balanceAfter" width="100" align="right" />
      <el-table-column label="处理号码" prop="phone" min-width="135" show-overflow-tooltip />
      <el-table-column label="订单编号" prop="orderNo" min-width="155" show-overflow-tooltip />
      <el-table-column label="操作人" prop="createBy" width="110" show-overflow-tooltip />
      <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
      <el-table-column label="创建时间" min-width="165" align="center">
        <template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="loadList"
    />
  </div>
</template>

<script setup name="MarkFundDetailsTotal">
import { Refresh, Search } from '@element-plus/icons-vue'
import { getMarkFundTotalSummary, listMarkFundPlatformOptions, listMarkFundTotal } from '@/api/server/markFund'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const summaryLoading = ref(false)
const showSearch = ref(true)
const moreFilterVisible = ref(false)
const total = ref(0)
const logList = ref([])
const platformOptions = ref([])
const dateRange = ref([])
const summary = ref({})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  userName: null,
  userId: null,
  platformCode: null,
  bizType: null,
  phone: null,
  orderId: null,
  createBy: null,
  params: {}
})

const bizTypeOptions = [
  { label: '管理员充值', value: 'ADMIN_RECHARGE' },
  { label: '消费扣减', value: 'DEDUCT' },
  { label: '退款', value: 'REFUND' },
  { label: '上划充值', value: 'TRANSFER_IN' },
  { label: '下划充值', value: 'TRANSFER_OUT' },
  { label: '账户调整', value: 'ADJUST' }
]

const summaryItems = computed(() => [
  { key: 'recharge', label: '充值总量', value: summary.value.totalRechargeAmount, className: 'amount-positive' },
  { key: 'deduct', label: '消费总量', value: summary.value.totalDeductAmount, className: 'amount-negative' },
  { key: 'refund', label: '退款总量', value: summary.value.totalRefundAmount, className: 'amount-positive' },
  { key: 'transferIn', label: '转入总量', value: summary.value.totalTransferInAmount, className: 'amount-positive' },
  { key: 'transferOut', label: '转出总量', value: summary.value.totalTransferOutAmount, className: 'amount-negative' },
  { key: 'net', label: '净变动', value: summary.value.netChangeAmount, className: Number(summary.value.netChangeAmount || 0) < 0 ? 'amount-negative' : 'amount-positive' },
  { key: 'users', label: '涉及用户', value: summary.value.affectedUserCount, className: '' },
  { key: 'records', label: '流水记录', value: summary.value.recordCount, className: '' }
])

function buildQuery(includePage = true) {
  const query = { ...queryParams, params: {} }
  if (!includePage) {
    delete query.pageNum
    delete query.pageSize
  }
  if (dateRange.value?.length === 2) {
    query.params.beginTime = dateRange.value[0]
    query.params.endTime = dateRange.value[1]
  }
  return query
}

function loadList() {
  loading.value = true
  return listMarkFundTotal(buildQuery()).then((res) => {
    logList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function loadSummary() {
  summaryLoading.value = true
  return getMarkFundTotalSummary(buildQuery(false)).then((res) => {
    summary.value = res.data || {}
  }).finally(() => {
    summaryLoading.value = false
  })
}

function loadData() {
  return Promise.all([loadList(), loadSummary()])
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.phone = null
  queryParams.orderId = null
  queryParams.createBy = null
  dateRange.value = []
  handleQuery()
}

function applyMoreFilters() {
  moreFilterVisible.value = false
  handleQuery()
}

function bizTypeLabel(row) {
  if (row.bizType === 'ADJUST') return Number(row.changeAmount || 0) >= 0 ? '账户充值' : '账户扣减'
  if (row.bizType === 'TRANSFER_OUT') return '下划充值'
  if (row.bizType === 'TRANSFER_IN') return '上划充值'
  return bizTypeOptions.find(item => item.value === row.bizType)?.label || row.bizType || '-'
}

function bizTypeTagType(row) {
  if (['ADMIN_RECHARGE', 'TRANSFER_IN', 'REFUND'].includes(row.bizType)) return 'success'
  if (['DEDUCT', 'TRANSFER_OUT'].includes(row.bizType)) return 'danger'
  if (row.bizType === 'ADJUST') return Number(row.changeAmount || 0) >= 0 ? 'success' : 'danger'
  return 'info'
}

function formatSignedAmount(value) {
  const amount = Number(value || 0)
  return amount > 0 ? `+${amount}` : String(amount)
}

function amountClass(value) {
  const amount = Number(value || 0)
  if (amount > 0) return 'amount-positive'
  if (amount < 0) return 'amount-negative'
  return ''
}

function formatSummary(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

onMounted(() => {
  listMarkFundPlatformOptions().then((res) => {
    platformOptions.value = Array.isArray(res.data) ? res.data : []
  })
  loadData()
})
</script>

<style scoped>
.page-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-heading h2 { margin: 0; font-size: 22px; font-weight: 600; }
.page-heading p { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 14px; }
.summary-grid { display: grid; grid-template-columns: repeat(8, minmax(120px, 1fr)); border: 1px solid var(--el-border-color-light); background: var(--el-bg-color); margin-bottom: 16px; }
.summary-item { min-height: 82px; padding: 14px 16px; border-right: 1px solid var(--el-border-color-light); display: flex; flex-direction: column; justify-content: center; }
.summary-item:last-child { border-right: 0; }
.summary-item span { color: var(--el-text-color-secondary); font-size: 13px; }
.summary-item strong { margin-top: 7px; font-size: 21px; font-weight: 600; }
.filter-band { padding: 14px 16px 0; border: 1px solid var(--el-border-color-light); border-bottom: 0; background: var(--el-fill-color-extra-light); }
.filter-band :deep(.el-form-item) { margin-right: 14px; }
.filter-actions { margin-left: auto; }
.drawer-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 8px; }
.table-toolbar { height: 48px; padding: 0 12px; border: 1px solid var(--el-border-color-light); display: flex; align-items: center; justify-content: space-between; color: var(--el-text-color-secondary); }
.amount-positive { color: var(--el-color-success); font-weight: 600; }
.amount-negative { color: var(--el-color-danger); font-weight: 600; }
@media (max-width: 1500px) { .summary-grid { grid-template-columns: repeat(4, minmax(140px, 1fr)); } .summary-item:nth-child(4) { border-right: 0; } .summary-item:nth-child(-n+4) { border-bottom: 1px solid var(--el-border-color-light); } }
@media (max-width: 900px) { .summary-grid { grid-template-columns: repeat(2, minmax(130px, 1fr)); } .summary-item:nth-child(even) { border-right: 0; } .summary-item { border-bottom: 1px solid var(--el-border-color-light); } .summary-item:nth-last-child(-n+2) { border-bottom: 0; } }
</style>
