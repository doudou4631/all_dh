<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="订单审计" name="order">
        <el-card shadow="never" body-class="search-card">
          <el-form ref="orderQueryRef" :model="orderQuery" :inline="true" v-show="showSearch" label-width="84px">
            <el-form-item label="订单号" prop="orderNo">
              <el-input v-model="orderQuery.orderNo" clearable @keyup.enter="handleOrderQuery" />
            </el-form-item>
            <el-form-item label="用户ID" prop="userId">
              <el-input v-model="orderQuery.userId" clearable @keyup.enter="handleOrderQuery" />
            </el-form-item>
            <el-form-item label="代理ID" prop="assignedAgentId">
              <el-input v-model="orderQuery.assignedAgentId" clearable @keyup.enter="handleOrderQuery" />
            </el-form-item>
            <el-form-item label="状态" prop="orderStatus">
              <el-select v-model="orderQuery.orderStatus" clearable style="width: 150px;">
                <el-option label="待处理" value="0" />
                <el-option label="处理中" value="1" />
                <el-option label="处理完成" value="2" />
                <el-option label="处理失败" value="3" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleOrderQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetOrderQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="mt10">
          <el-row :gutter="10" class="mb8">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getOrderList"></right-toolbar>
          </el-row>
          <el-table v-loading="orderLoading" :data="orderList">
            <el-table-column label="序号" type="index" width="56" align="center" />
            <el-table-column label="订单号" prop="orderNo" min-width="160" show-overflow-tooltip />
            <el-table-column label="用户ID" prop="userId" width="90" align="center" />
            <el-table-column label="代理ID" prop="assignedAgentId" width="90" align="center" />
            <el-table-column label="平台" prop="platformName" min-width="120" />
            <el-table-column label="总数" prop="totalCount" width="76" align="center" />
            <el-table-column label="成功" prop="successCount" width="76" align="center" />
            <el-table-column label="失败" prop="failedCount" width="76" align="center" />
            <el-table-column label="扣费" prop="totalAmount" width="90" align="center" />
            <el-table-column label="退款" prop="refundAmount" width="90" align="center" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="scope">
                <el-tag :type="orderStatusType(scope.row.orderStatus)" size="small">
                  {{ orderStatusLabel(scope.row.orderStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时间" min-width="160" align="center">
              <template #default="scope">
                {{ formatDateTime(scope.row.createTime) }}
              </template>
            </el-table-column>
          </el-table>
          <pagination
            v-show="orderTotal > 0"
            :total="orderTotal"
            v-model:page="orderQuery.pageNum"
            v-model:limit="orderQuery.pageSize"
            @pagination="getOrderList"
          />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="流水审计" name="wallet">
        <el-card shadow="never" body-class="search-card">
          <el-form ref="walletQueryRef" :model="walletQuery" :inline="true" v-show="showSearch" label-width="84px">
            <el-form-item label="用户ID" prop="userId">
              <el-input v-model="walletQuery.userId" clearable @keyup.enter="handleWalletQuery" />
            </el-form-item>
            <el-form-item label="订单ID" prop="orderId">
              <el-input v-model="walletQuery.orderId" clearable @keyup.enter="handleWalletQuery" />
            </el-form-item>
            <el-form-item label="业务类型" prop="bizType">
              <el-select v-model="walletQuery.bizType" clearable style="width: 150px;">
                <el-option label="扣费" value="DEDUCT" />
                <el-option label="退款" value="REFUND" />
                <el-option label="调整" value="ADJUST" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleWalletQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetWalletQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="mt10">
          <el-row :gutter="10" class="mb8">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getWalletList"></right-toolbar>
          </el-row>
          <el-table v-loading="walletLoading" :data="walletList">
            <el-table-column label="序号" type="index" width="56" align="center" />
            <el-table-column label="用户ID" prop="userId" width="90" align="center" />
            <el-table-column label="订单ID" prop="orderId" width="120" />
            <el-table-column label="明细ID" prop="orderItemId" width="120" />
            <el-table-column label="业务类型" prop="bizType" width="100" align="center" />
            <el-table-column label="变动金额" prop="changeAmount" width="100" align="center" />
            <el-table-column label="变动前" prop="balanceBefore" width="100" align="center" />
            <el-table-column label="变动后" prop="balanceAfter" width="100" align="center" />
            <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
            <el-table-column label="时间" min-width="160" align="center">
              <template #default="scope">
                {{ formatDateTime(scope.row.createTime) }}
              </template>
            </el-table-column>
          </el-table>
          <pagination
            v-show="walletTotal > 0"
            :total="walletTotal"
            v-model:page="walletQuery.pageNum"
            v-model:limit="walletQuery.pageSize"
            @pagination="getWalletList"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="MarkAudit">
import { listMarkAuditOrder, listMarkAuditWallet } from '@/api/server/markAdmin'

const { proxy } = getCurrentInstance()

const activeTab = ref('order')
const showSearch = ref(true)

const orderLoading = ref(false)
const orderTotal = ref(0)
const orderList = ref([])

const walletLoading = ref(false)
const walletTotal = ref(0)
const walletList = ref([])

const orderQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: null,
  userId: null,
  assignedAgentId: null,
  orderStatus: null
})

const walletQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  userId: null,
  orderId: null,
  bizType: null
})

function orderStatusLabel(status) {
  const map = { '0': '待处理', '1': '处理中', '2': '处理完成', '3': '处理失败' }
  return map[status] || '-'
}

function orderStatusType(status) {
  if (status === '2') return 'success'
  if (status === '1') return 'warning'
  if (status === '3') return 'info'
  return ''
}

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function getOrderList() {
  orderLoading.value = true
  listMarkAuditOrder(orderQuery).then((res) => {
    orderList.value = res.rows || []
    orderTotal.value = res.total || 0
  }).finally(() => {
    orderLoading.value = false
  })
}

function getWalletList() {
  walletLoading.value = true
  listMarkAuditWallet(walletQuery).then((res) => {
    walletList.value = res.rows || []
    walletTotal.value = res.total || 0
  }).finally(() => {
    walletLoading.value = false
  })
}

function handleOrderQuery() {
  orderQuery.pageNum = 1
  getOrderList()
}

function resetOrderQuery() {
  proxy.resetForm('orderQueryRef')
  handleOrderQuery()
}

function handleWalletQuery() {
  walletQuery.pageNum = 1
  getWalletList()
}

function resetWalletQuery() {
  proxy.resetForm('walletQueryRef')
  handleWalletQuery()
}

onMounted(() => {
  getOrderList()
  getWalletList()
})
</script>

<style scoped>
.mt10 {
  margin-top: 10px;
}

.mb8 {
  margin-bottom: 8px;
}
</style>
