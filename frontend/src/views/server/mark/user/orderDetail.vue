<template>
  <div class="app-container mark-user-order-detail-page">
    <el-card shadow="never">
      <template #header>
        <span>订单详情</span>
      </template>

      <el-form :model="queryParams" :inline="true" v-show="showSearch" label-width="84px" class="order-detail-query-form">
        <el-form-item label="综合搜索">
          <el-input
            v-model="queryParams.keyword"
            clearable
            placeholder="订单号/手机号"
            style="width: 200px;"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="平台">
          <el-select v-model="queryParams.platformCode" clearable placeholder="全部平台" style="width: 160px;">
            <el-option
              v-for="item in platformOptions"
              :key="item.platformCode"
              :label="item.platformName"
              :value="item.platformCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="queryParams.orderStatus" clearable placeholder="全部状态" style="width: 130px;">
            <el-option label="待处理" value="0" />
            <el-option label="处理中" value="3" />
            <el-option label="处理完成" value="1" />
            <el-option label="处理失败" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="提交时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="-"
            start-placeholder="开始"
            end-placeholder="结束"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            style="width: 240px;"
            @change="handleDateRangeChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button icon="Download" @click="exportRows">导出</el-button>
        </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="orderList" :row-key="recordRowKey">
        <el-table-column label="序号" type="index" width="56" align="center" />
        <el-table-column label="平台" prop="platformName" min-width="110" show-overflow-tooltip />
        <el-table-column label="号码" min-width="130" show-overflow-tooltip>
          <template #default="scope">
            <el-button link type="primary" @click="copyText(scope.row.phonePreview || '')">
              {{ scope.row.phonePreview || '-' }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="处理状态" width="92" align="center">
          <template #default="scope">
            <el-tag :type="recordStatusType(scope.row)" size="small">
              {{ recordStatusLabel(scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单号" prop="orderNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="提交时间" min-width="160" align="center">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="88" align="center" fixed="right">
          <template #default="scope">
            <el-button
              link
              type="primary"
              v-hasPermi="['server:markUser:order:query']"
              @click="openDetail(scope.row)"
            >
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

    <el-dialog v-model="detailOpen" title="订单详情" width="920px" append-to-body destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions v-if="detailOrder" :column="3" border size="small" class="mb12">
          <el-descriptions-item label="订单号">{{ detailOrder.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="平台">{{ detailOrder.platformName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(detailOrder.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="明细数量">{{ detailOrder.totalCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="成功/失败">{{ detailOrder.successCount ?? 0 }} / {{ detailOrder.failedCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="扣费积分">{{ detailOrder.totalAmount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">{{ auditStatusLabel(detailOrder.auditStatus) }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ orderStatusLabel(detailOrder.orderStatus) }}</el-descriptions-item>
          <el-descriptions-item label="退款积分">{{ detailOrder.refundAmount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="3">{{ detailOrder.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="detailItems" border size="small" max-height="420" :row-class-name="detailRowClassName">
          <el-table-column label="号码" prop="phone" min-width="130" />
          <el-table-column label="单价" prop="unitPrice" width="70" align="center" />
          <el-table-column label="处理状态" width="92" align="center">
            <template #default="scope">
              <el-tag :type="itemStatusType(scope.row.processStatus)" size="small">
                {{ itemStatusLabel(scope.row.processStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="处理结果" prop="processResult" min-width="160" show-overflow-tooltip />
          <el-table-column label="处理备注" prop="processNote" min-width="120" show-overflow-tooltip />
          <el-table-column label="是否退款" width="88" align="center">
            <template #default="scope">
              {{ scope.row.refunded === '1' ? '是' : '否' }}
            </template>
          </el-table-column>
          <el-table-column label="处理时间" min-width="160" align="center">
            <template #default="scope">
              {{ formatDateTime(scope.row.processedTime) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="detailOpen = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MarkUserOrderDetail">
import { getMarkUserOrderDetail, listMarkUserOrder, listMarkUserPlatformPrice } from '@/api/server/markUser'

const { proxy } = getCurrentInstance()

const showSearch = ref(true)
const loading = ref(false)
const detailLoading = ref(false)
const detailOpen = ref(false)
const total = ref(0)
const orderList = ref([])
const platformOptions = ref([])
const dateRange = ref([])
const detailOrder = ref(null)
const detailItems = ref([])
const activeItemId = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: null,
  phone: null,
  platformCode: null,
  orderStatus: null,
  params: {}
})

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function isAutoProcessingRecord(row) {
  const code = String(row?.platformCode || '').trim().toLowerCase()
  return ['tencent_mark', 'tengxun', 'tencent', 'tx', 'txwz', 'td_gaopin'].includes(code)
}

function recordStatusLabel(row) {
  const auditStatus = String(row?.auditStatus ?? '1')
  if (auditStatus === '0') return '待审核'
  if (auditStatus === '2') return '已拒绝'
  if (auditStatus === '3') return '已打回'
  const itemStatus = String(row?.itemProcessStatus ?? '')
  if (itemStatus === '3') return '处理中'
  if (itemStatus === '0') return isAutoProcessingRecord(row) ? '处理中' : '待处理'
  if (itemStatus === '1') return '处理完成'
  if (itemStatus === '2') return '处理失败'
  const status = String(row?.orderStatus ?? '')
  const successCount = Number(row?.successCount ?? 0)
  const failedCount = Number(row?.failedCount ?? 0)
  if (status === '0') return '待处理'
  if (status === '1') return '处理中'
  if (status === '2') return failedCount > 0 && successCount <= 0 ? '处理失败' : (failedCount > 0 ? '处理失败' : '处理完成')
  if (status === '3') return '处理失败'
  return '待处理'
}

function recordStatusType(row) {
  const auditStatus = String(row?.auditStatus ?? '1')
  if (auditStatus === '2') return 'danger'
  if (auditStatus === '3') return 'warning'
  if (auditStatus === '0') return 'info'
  const label = recordStatusLabel(row)
  if (label === '处理完成') return 'success'
  if (label === '处理失败') return 'danger'
  if (label === '处理中') return 'warning'
  return 'info'
}

function itemStatusLabel(value) {
  const status = String(value ?? '')
  if (status === '1') return '处理完成'
  if (status === '2') return '处理失败'
  if (status === '3') return '处理中'
  if (status === '0') return '待处理'
  return status || '-'
}

function itemStatusType(value) {
  const status = String(value ?? '')
  if (status === '1') return 'success'
  if (status === '2') return 'danger'
  if (status === '3') return 'warning'
  if (status === '0') return 'warning'
  return 'info'
}

function auditStatusLabel(value) {
  const map = { '0': '待审核', '1': '通过', '2': '拒绝', '3': '打回' }
  return map[String(value ?? '')] || '-'
}

function orderStatusLabel(value) {
  const map = { '0': '待处理', '1': '处理中', '2': '处理完成', '3': '处理失败' }
  return map[String(value ?? '')] || '-'
}

function recordRowKey(row) {
  return `${row?.itemId ?? row?.id ?? ''}-${row?.phonePreview ?? ''}`
}

function normalizeKeyword() {
  const keyword = String(queryParams.keyword || '').trim()
  queryParams.keyword = keyword || null
  if (keyword && /^\d{7,15}$/.test(keyword)) {
    queryParams.phone = keyword
    return
  }
  queryParams.phone = null
}

function handleDateRangeChange(value) {
  if (Array.isArray(value) && value.length === 2) {
    queryParams.params = {
      beginTime: value[0],
      endTime: value[1]
    }
    return
  }
  queryParams.params = {}
}

async function copyText(text) {
  const value = String(text || '').trim()
  if (!value) {
    proxy.$modal.msgWarning('没有可复制内容')
    return
  }
  try {
    await navigator.clipboard.writeText(value)
    proxy.$modal.msgSuccess('已复制')
  } catch (error) {
    proxy.$modal.msgError('复制失败')
  }
}

function getList() {
  normalizeKeyword()
  loading.value = true
  return listMarkUserOrder(queryParams).then((res) => {
    orderList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function loadPlatformOptions() {
  return listMarkUserPlatformPrice().then((resp) => {
    platformOptions.value = Array.isArray(resp?.data) ? resp.data : []
  }).catch(() => {
    platformOptions.value = []
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.pageNum = 1
  queryParams.keyword = null
  queryParams.phone = null
  queryParams.platformCode = null
  queryParams.orderStatus = null
  queryParams.params = {}
  dateRange.value = []
  getList()
}

function openDetail(row) {
  const orderId = row?.id
  const itemId = row?.itemId
  if (!orderId) {
    proxy.$modal.msgWarning('订单不存在')
    return
  }
  activeItemId.value = itemId || null
  detailOpen.value = true
  detailLoading.value = true
  detailOrder.value = null
  detailItems.value = []
  getMarkUserOrderDetail(orderId).then((res) => {
    detailOrder.value = res?.data?.order || null
    detailItems.value = Array.isArray(res?.data?.items) ? res.data.items : []
  }).finally(() => {
    detailLoading.value = false
  })
}

function detailRowClassName({ row }) {
  if (activeItemId.value && row?.id === activeItemId.value) {
    return 'order-detail-row--active'
  }
  return ''
}

function exportRows() {
  if (!orderList.value.length) {
    proxy.$modal.msgWarning('暂无可导出数据')
    return
  }
  const header = ['平台', '号码', '处理状态', '订单号', '提交时间']
  const body = orderList.value.map((row) => [
    row.platformName || '',
    row.phonePreview || '',
    recordStatusLabel(row),
    row.orderNo || '',
    formatDateTime(row.createTime)
  ])
  const csv = [header, ...body]
    .map((line) => line.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    .join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `mark-order-detail-${Date.now()}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

onMounted(() => {
  loadPlatformOptions()
  getList()
})
</script>

<style scoped>
.mark-user-order-detail-page .order-detail-query-form {
  margin-bottom: 8px;
}

.mb12 {
  margin-bottom: 12px;
}

:deep(.order-detail-row--active > td) {
  background: #ecf5ff !important;
}
</style>
