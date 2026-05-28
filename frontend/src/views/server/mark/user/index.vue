<template>
  <div class="app-container">
    <el-card shadow="never" class="mb10">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="8">
          <el-statistic title="当前积分" :value="walletSummary.pointsBalance || 0" />
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-statistic title="累计扣费" :value="walletSummary.totalDeductAmount || 0" />
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-statistic title="累计退款" :value="walletSummary.totalRefundAmount || 0" />
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" body-class="search-card">
      <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="queryParams.orderNo" placeholder="请输入订单号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="幂等号" prop="requestNo">
          <el-input v-model="queryParams.requestNo" placeholder="请输入请求幂等号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="平台编码" prop="platformCode">
          <el-input v-model="queryParams.platformCode" placeholder="请输入平台编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="订单状态" prop="orderStatus">
          <el-select v-model="queryParams.orderStatus" placeholder="请选择状态" clearable style="width: 150px;">
            <el-option v-for="item in orderStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="mt10">
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" @click="openCreateDialog" v-hasPermi="['server:markUser:order:add']">
            提交订单
          </el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="orderList">
        <el-table-column label="序号" type="index" width="56" align="center" />
        <el-table-column label="订单号" prop="orderNo" min-width="160" show-overflow-tooltip />
        <el-table-column label="幂等号" prop="requestNo" min-width="140" show-overflow-tooltip />
        <el-table-column label="平台" prop="platformName" min-width="120" show-overflow-tooltip />
        <el-table-column label="总数" prop="totalCount" width="80" align="center" />
        <el-table-column label="成功" prop="successCount" width="80" align="center" />
        <el-table-column label="失败" prop="failedCount" width="80" align="center" />
        <el-table-column label="扣费" prop="totalAmount" width="90" align="center" />
        <el-table-column label="退款" prop="refundAmount" width="90" align="center" />
        <el-table-column label="状态" width="92" align="center">
          <template #default="scope">
            <el-tag :type="orderStatusType(scope.row.orderStatus)" size="small">
              {{ orderStatusLabel(scope.row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160" align="center">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="96" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="openDetail(scope.row)" v-hasPermi="['server:markUser:order:query']">
              详情
            </el-button>
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
    </el-card>

    <el-dialog v-model="createOpen" title="提交订单" width="640px" append-to-body>
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="98px">
        <el-form-item label="平台" prop="platformCode">
          <el-select v-model="createForm.platformCode" filterable placeholder="请选择平台" style="width: 100%;">
            <el-option
              v-for="item in platformOptions"
              :key="item.platformCode"
              :label="`${item.platformName} (${item.platformCode}) 单价:${item.unitPrice ?? 1}`"
              :value="item.platformCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="请求幂等号" prop="requestNo">
          <el-input v-model="createForm.requestNo" placeholder="可选，建议传业务唯一号" maxlength="64" />
        </el-form-item>
        <el-form-item label="号码列表" prop="phonesText">
          <el-input
            v-model="createForm.phonesText"
            type="textarea"
            :rows="8"
            placeholder="支持逗号/空格/换行分隔，例如：13800000000,13900000000"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="createSubmitting" @click="submitCreate">提 交</el-button>
          <el-button @click="createOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailOpen" title="订单详情" width="980px" append-to-body>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="订单号">{{ detailData.order?.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="平台">{{ detailData.order?.platformName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="orderStatusType(detailData.order?.orderStatus)" size="small">
            {{ orderStatusLabel(detailData.order?.orderStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总数">{{ detailData.order?.totalCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="成功">{{ detailData.order?.successCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="失败">{{ detailData.order?.failedCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="扣费">{{ detailData.order?.totalAmount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="退款">{{ detailData.order?.refundAmount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detailData.order?.createTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-table class="mt10" :data="detailData.items || []" max-height="420">
        <el-table-column label="序号" type="index" width="56" align="center" />
        <el-table-column label="号码" prop="phone" min-width="130" />
        <el-table-column label="单价" prop="unitPrice" width="90" align="center" />
        <el-table-column label="金额" prop="itemAmount" width="90" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="itemStatusType(scope.row.processStatus)" size="small">
              {{ itemStatusLabel(scope.row.processStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理结果" prop="processResult" min-width="160" show-overflow-tooltip />
        <el-table-column label="处理备注" prop="processNote" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="MarkUserOrder">
import {
  listMarkUserOrder,
  createMarkUserOrder,
  getMarkUserOrderDetail,
  getMarkUserWalletSummary,
  listMarkUserPlatformPrice
} from '@/api/server/markUser'

const { proxy } = getCurrentInstance()

const showSearch = ref(true)
const loading = ref(false)
const total = ref(0)
const orderList = ref([])
const createOpen = ref(false)
const createSubmitting = ref(false)
const detailOpen = ref(false)
const platformOptions = ref([])
const walletSummary = ref({})
const detailData = ref({ order: {}, items: [] })

const orderStatusOptions = [
  { label: '待处理', value: '0' },
  { label: '处理中', value: '1' },
  { label: '已完成', value: '2' },
  { label: '已取消', value: '3' }
]

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: null,
    requestNo: null,
    platformCode: null,
    orderStatus: null
  },
  createForm: {
    platformCode: '',
    requestNo: '',
    phonesText: '',
    remark: ''
  },
  createRules: {
    platformCode: [{ required: true, message: '请选择平台', trigger: 'change' }],
    phonesText: [{ required: true, message: '请输入号码列表', trigger: 'blur' }]
  }
})

const { queryParams, createForm, createRules } = toRefs(data)

function orderStatusLabel(status) {
  const map = { '0': '待处理', '1': '处理中', '2': '已完成', '3': '已取消' }
  return map[status] || '-'
}

function orderStatusType(status) {
  if (status === '2') return 'success'
  if (status === '1') return 'warning'
  if (status === '3') return 'info'
  return ''
}

function itemStatusLabel(status) {
  const map = { '0': '待处理', '1': '成功', '2': '失败' }
  return map[status] || '-'
}

function itemStatusType(status) {
  if (status === '1') return 'success'
  if (status === '2') return 'danger'
  return 'info'
}

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function getList() {
  loading.value = true
  listMarkUserOrder(queryParams.value).then((res) => {
    orderList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function loadSummaryAndPrice() {
  getMarkUserWalletSummary().then((res) => {
    walletSummary.value = res.data || {}
    if (Array.isArray(res.data?.platformPrices) && res.data.platformPrices.length > 0) {
      platformOptions.value = res.data.platformPrices
    }
  })
  listMarkUserPlatformPrice().then((res) => {
    if (Array.isArray(res.data) && res.data.length > 0) {
      platformOptions.value = res.data
    }
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function openCreateDialog() {
  createForm.value = {
    platformCode: '',
    requestNo: '',
    phonesText: '',
    remark: ''
  }
  proxy.resetForm('createRef')
  createOpen.value = true
}

function parsePhones(text) {
  if (!text) return []
  return Array.from(new Set(
    text
      .split(/[\n,，;；\s]+/)
      .map((item) => item.trim())
      .filter((item) => item.length > 0)
  ))
}

function submitCreate() {
  proxy.$refs.createRef.validate((valid) => {
    if (!valid) return
    const phones = parsePhones(createForm.value.phonesText)
    if (phones.length === 0) {
      proxy.$modal.msgError('未解析到有效号码')
      return
    }
    const platform = platformOptions.value.find((x) => x.platformCode === createForm.value.platformCode)
    const payload = {
      platformCode: createForm.value.platformCode,
      platformName: platform?.platformName || '',
      requestNo: createForm.value.requestNo || '',
      phones,
      remark: createForm.value.remark || ''
    }
    createSubmitting.value = true
    createMarkUserOrder(payload).then((res) => {
      proxy.$modal.msgSuccess(res.msg || '下单成功')
      createOpen.value = false
      getList()
      loadSummaryAndPrice()
    }).finally(() => {
      createSubmitting.value = false
    })
  })
}

function openDetail(row) {
  getMarkUserOrderDetail(row.id).then((res) => {
    detailData.value = res.data || { order: {}, items: [] }
    detailOpen.value = true
  })
}

onMounted(() => {
  getList()
  loadSummaryAndPrice()
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
