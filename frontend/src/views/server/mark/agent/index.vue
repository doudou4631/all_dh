<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="queryParams.orderNo" placeholder="请输入订单号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="幂等号" prop="requestNo">
          <el-input v-model="queryParams.requestNo" placeholder="请输入幂等号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="号码" prop="phone">
          <el-input v-model="queryParams.phone" placeholder="请输入号码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="orderStatus">
          <el-select v-model="queryParams.orderStatus" clearable placeholder="请选择状态" style="width: 150px;">
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
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>
      <el-table v-loading="loading" :data="orderList">
        <el-table-column label="序号" type="index" width="56" align="center" />
        <el-table-column label="订单号" prop="orderNo" min-width="160" show-overflow-tooltip />
        <el-table-column label="平台" prop="platformName" min-width="120" show-overflow-tooltip />
        <el-table-column label="总数" prop="totalCount" width="80" align="center" />
        <el-table-column label="成功" prop="successCount" width="80" align="center" />
        <el-table-column label="失败" prop="failedCount" width="80" align="center" />
        <el-table-column label="状态" width="92" align="center">
          <template #default="scope">
            <el-tag :type="orderStatusType(scope.row.orderStatus)" size="small">
              {{ orderStatusLabel(scope.row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="代理ID" prop="assignedAgentId" width="100" align="center" />
        <el-table-column label="创建时间" min-width="160" align="center">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="160" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="openDetail(scope.row)" v-hasPermi="['server:markAgent:order:query']">
              详情
            </el-button>
            <el-button
              link
              type="success"
              icon="Select"
              @click="handleComplete(scope.row)"
              v-hasPermi="['server:markAgent:order:complete']"
            >
              完成
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

    <el-dialog v-model="detailOpen" title="订单处理详情" width="1080px" append-to-body>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="订单号">{{ detailData.order?.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="平台">{{ detailData.order?.platformName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="orderStatusType(detailData.order?.orderStatus)" size="small">
            {{ orderStatusLabel(detailData.order?.orderStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="代理ID">{{ detailData.order?.assignedAgentId || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-table class="mt10" :data="detailData.items || []" max-height="440">
        <el-table-column label="序号" type="index" width="56" align="center" />
        <el-table-column label="号码" prop="phone" min-width="130" />
        <el-table-column label="单价" prop="unitPrice" width="86" align="center" />
        <el-table-column label="金额" prop="itemAmount" width="86" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="itemStatusType(scope.row.processStatus)" size="small">
              {{ itemStatusLabel(scope.row.processStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理结果" prop="processResult" min-width="140" show-overflow-tooltip />
        <el-table-column label="处理备注" prop="processNote" min-width="180" show-overflow-tooltip />
        <el-table-column label="处理人" prop="processedBy" width="100" align="center" />
        <el-table-column label="处理时间" width="160" align="center">
          <template #default="scope">
            {{ formatDateTime(scope.row.processedTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="90" align="center">
          <template #default="scope">
            <el-button
              v-if="scope.row.processStatus === '0'"
              link
              type="primary"
              icon="Edit"
              @click="openFeedback(scope.row)"
              v-hasPermi="['server:markAgent:item:feedback']"
            >
              回填
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <div class="dialog-footer">
          <el-button
            type="success"
            @click="handleComplete(detailData.order)"
            v-hasPermi="['server:markAgent:order:complete']"
          >
            完成整单
          </el-button>
          <el-button @click="detailOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="feedbackOpen" title="回填处理结果" width="520px" append-to-body>
      <el-form ref="feedbackRef" :model="feedbackForm" :rules="feedbackRules" label-width="96px">
        <el-form-item label="处理状态" prop="processStatus">
          <el-radio-group v-model="feedbackForm.processStatus">
            <el-radio label="1">成功</el-radio>
            <el-radio label="2">失败</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理结果" prop="processResult">
          <el-input v-model="feedbackForm.processResult" placeholder="请输入处理结果描述" maxlength="255" />
        </el-form-item>
        <el-form-item label="处理备注" prop="processNote">
          <el-input v-model="feedbackForm.processNote" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="feedbackSubmitting" @click="submitFeedback">提 交</el-button>
          <el-button @click="feedbackOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MarkAgentOrder">
import {
  listMarkAgentOrder,
  getMarkAgentOrderDetail,
  feedbackMarkOrderItem,
  completeMarkOrder
} from '@/api/server/markAgent'

const { proxy } = getCurrentInstance()

const showSearch = ref(true)
const loading = ref(false)
const total = ref(0)
const orderList = ref([])
const detailOpen = ref(false)
const detailData = ref({ order: {}, items: [] })
const feedbackOpen = ref(false)
const feedbackSubmitting = ref(false)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: null,
    requestNo: null,
    phone: null,
    orderStatus: null
  },
  feedbackForm: {
    itemId: null,
    processStatus: '1',
    processResult: '',
    processNote: ''
  },
  feedbackRules: {
    processStatus: [{ required: true, message: '请选择状态', trigger: 'change' }]
  }
})

const { queryParams, feedbackForm, feedbackRules } = toRefs(data)

const orderStatusOptions = [
  { label: '待处理', value: '0' },
  { label: '处理中', value: '1' },
  { label: '已完成', value: '2' },
  { label: '已取消', value: '3' }
]

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
  listMarkAgentOrder(queryParams.value).then((res) => {
    orderList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
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

function openDetail(row) {
  getMarkAgentOrderDetail(row.id).then((res) => {
    detailData.value = res.data || { order: {}, items: [] }
    detailOpen.value = true
  })
}

function refreshDetailIfOpened(orderId) {
  if (!detailOpen.value || !orderId) return
  getMarkAgentOrderDetail(orderId).then((res) => {
    detailData.value = res.data || { order: {}, items: [] }
  })
}

function openFeedback(item) {
  feedbackForm.value = {
    itemId: item.id,
    processStatus: '1',
    processResult: '',
    processNote: ''
  }
  proxy.resetForm('feedbackRef')
  feedbackOpen.value = true
}

function submitFeedback() {
  proxy.$refs.feedbackRef.validate((valid) => {
    if (!valid) return
    const payload = {
      processStatus: feedbackForm.value.processStatus,
      processResult: feedbackForm.value.processResult || '',
      processNote: feedbackForm.value.processNote || ''
    }
    feedbackSubmitting.value = true
    feedbackMarkOrderItem(feedbackForm.value.itemId, payload).then((res) => {
      proxy.$modal.msgSuccess(res.msg || '回填成功')
      feedbackOpen.value = false
      getList()
      refreshDetailIfOpened(detailData.value.order?.id)
    }).finally(() => {
      feedbackSubmitting.value = false
    })
  })
}

function handleComplete(row) {
  const id = row?.id
  if (!id) return
  proxy.$modal.confirm('确认将该订单标记为已完成？').then(() => {
    return completeMarkOrder(id)
  }).then((res) => {
    proxy.$modal.msgSuccess(res.msg || '整单已完成')
    getList()
    refreshDetailIfOpened(id)
  }).catch(() => {})
}

onMounted(() => {
  getList()
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
