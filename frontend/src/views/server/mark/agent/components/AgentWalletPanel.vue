<template>
  <div>
    <div class="search-bar">
      <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
        <el-form-item label="下线账号" prop="userId">
          <el-select
            v-model="queryParams.userId"
            filterable
            clearable
            placeholder="请选择下线账号"
            style="width: 220px;"
            :loading="userLoading"
          >
            <el-option
              v-for="user in downstreamUsers"
              :key="user.userId"
              :label="user.nickName ? `${user.userName}（${user.nickName}）` : user.userName"
              :value="user.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!fixedBizType" label="业务类型" prop="bizType">
          <el-select v-model="queryParams.bizType" clearable placeholder="请选择业务类型" style="width: 160px;">
            <el-option v-for="item in bizTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单ID" prop="orderId">
          <el-input v-model="queryParams.orderId" clearable placeholder="请输入订单ID" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="logList">
      <el-table-column label="序号" type="index" width="56" align="center" />
      <el-table-column label="下线账号" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          {{ downstreamUserNameMap[scope.row.userId] || scope.row.userId }}
        </template>
      </el-table-column>
      <el-table-column label="平台名称" prop="platformName" min-width="130" show-overflow-tooltip />
      <el-table-column label="业务类型" width="100" align="center">
        <template #default="scope">
          <el-tag :type="bizTypeTagType(scope.row.bizType)" size="small">
            {{ bizTypeLabel(scope.row.bizType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="变动次数" width="100" align="center">
        <template #default="scope">
          <span :style="changeAmountStyle(scope.row.changeAmount)">
            {{ formatSignedAmount(scope.row.changeAmount) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="变动前" prop="balanceBefore" width="90" align="center" />
      <el-table-column label="变动后" prop="balanceAfter" width="90" align="center" />
      <el-table-column label="订单ID" prop="orderId" min-width="120" />
      <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
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
      @pagination="getList"
    />
  </div>
</template>

<script setup>
import { listMarkAgentWalletLog } from '@/api/server/markAgent'
import { listUser } from '@/api/system/user'
import { useRoute } from 'vue-router'

const props = defineProps({
  fixedBizType: {
    type: String,
    default: null
  },
  defaultUserId: {
    type: [Number, String],
    default: null
  }
})

const route = useRoute()
const { proxy } = getCurrentInstance()

const showSearch = ref(true)
const loading = ref(false)
const total = ref(0)
const logList = ref([])
const userLoading = ref(false)
const downstreamUsers = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  userId: null,
  orderId: null,
  bizType: props.fixedBizType
})

const bizTypeOptions = [
  { label: '扣费', value: 'DEDUCT' },
  { label: '退款', value: 'REFUND' },
  { label: '调整', value: 'ADJUST' }
]

const downstreamUserNameMap = computed(() => {
  const map = {}
  for (const item of downstreamUsers.value) {
    if (!item?.userId) continue
    map[item.userId] = item.nickName ? `${item.userName}（${item.nickName}）` : item.userName
  }
  return map
})

function bizTypeLabel(type) {
  const matched = bizTypeOptions.find(item => item.value === type)
  return matched?.label || type || '-'
}

function bizTypeTagType(type) {
  if (type === 'DEDUCT') return 'warning'
  if (type === 'REFUND') return 'success'
  return 'info'
}

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function formatSignedAmount(value) {
  const amount = Number(value || 0)
  if (!Number.isFinite(amount)) return '0'
  return amount > 0 ? `+${amount}` : `${amount}`
}

function changeAmountStyle(value) {
  const amount = Number(value || 0)
  if (amount > 0) return { color: '#67C23A', fontWeight: '600' }
  if (amount < 0) return { color: '#F56C6C', fontWeight: '600' }
  return { color: '#606266' }
}

function getList() {
  loading.value = true
  listMarkAgentWalletLog(queryParams).then((res) => {
    logList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function loadDownstreamUsers() {
  userLoading.value = true
  return listUser({
    pageNum: 1,
    pageSize: 500,
    roleKey: 'user,mark_user'
  }).then((res) => {
    const rows = Array.isArray(res?.rows) ? res.rows : []
    const map = new Map()
    rows.forEach((item) => {
      if (!item?.userId || map.has(item.userId)) return
      map.set(item.userId, {
        userId: item.userId,
        userName: item.userName,
        nickName: item.nickName
      })
    })
    downstreamUsers.value = Array.from(map.values())
  }).finally(() => {
    userLoading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.bizType = props.fixedBizType
  handleQuery()
}

function applyDefaultUserId() {
  const fromProp = Number(props.defaultUserId)
  const fromRoute = Number(route.query.userId)
  const userId = Number.isFinite(fromProp) && fromProp > 0 ? fromProp : fromRoute
  if (Number.isFinite(userId) && userId > 0) {
    queryParams.userId = userId
  }
}

onMounted(() => {
  loadDownstreamUsers().then(() => {
    applyDefaultUserId()
    getList()
  })
})
</script>

<style scoped>
.search-bar {
  margin-bottom: 12px;
}

.mb8 {
  margin-bottom: 8px;
}
</style>