<template>
  <div class="app-container mark-user-order-page">

    <el-card shadow="never" class="platform-card">

      <el-empty v-if="platformOptions.length === 0" description="当前未配置可用平台" />

      <el-tabs
        v-else
        v-model="activeSubTab"
        class="sub-tabs"
      >
        <el-tab-pane :label="`${activePlatformName} - 提交号码`" name="submit">
          <div class="submit-pane">
            <h3 class="submit-title">{{ activePlatformName }} - 提交号码</h3>

            <div class="submit-tip">
              {{ activePlatformHint }}
            </div>

            <el-input
              v-model="submitForm.phonesText"
              type="textarea"
              :rows="11"
              placeholder="请输入号码，每行一个或使用空格/逗号分隔"
            />

            <div class="submit-stats">
              输入：{{ submitPhoneStats.inputCount }} 个，
              有效：{{ submitPhoneStats.validCount }} 个，
              重复：{{ submitPhoneStats.duplicateCount }} 个，
              无效：{{ submitPhoneStats.invalidCount }} 个
            </div>

            <div class="submit-charge-bar">
              当前平台：{{ activePlatformName }}，每个号码扣 {{ activeUnitPrice }} 次，
              预计提交：{{ expectedSubmitCount }} 个，预计扣除：{{ expectedDeductAmount }} 次
            </div>

            <div class="submit-actions">
              <el-button
                type="primary"
                icon="Search"
                :loading="submitLoading"
                :disabled="!canSubmit"
                @click="submitBatchOrder"
                v-hasPermi="['server:markUser:order:add']"
              >
                一键批量查询
              </el-button>
              <el-button @click="clearSubmitPhones">清空</el-button>
            </div>
            <div class="submit-result-wrap">
              <el-divider content-position="left">查询结果</el-divider>
              <div class="result-pane">
                <div class="result-toolbar">
                  <el-select
                    v-model="selectedResultOrderId"
                    placeholder="请选择订单号"
                    clearable
                    filterable
                    style="width: 360px;"
                    @change="handleResultOrderChange"
                  >
                    <el-option
                      v-for="item in orderList"
                      :key="item.id"
                      :label="`${item.orderNo}（${formatDateTime(item.createTime)}）`"
                      :value="item.id"
                    />
                  </el-select>
                  <el-button icon="Refresh" :disabled="!selectedResultOrderId" @click="refreshResultDetail">刷新结果</el-button>
                </div>

                <el-empty
                  v-if="!selectedResultOrderId && !resultLoading"
                  description="当前平台暂无可查看的查询结果"
                />

                <template v-else>
                  <el-descriptions :column="4" border class="result-summary">
                    <el-descriptions-item label="订单号">{{ detailData.order?.orderNo || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="平台">{{ detailData.order?.platformName || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="状态">
                      <el-tag :type="orderStatusType(detailData.order?.orderStatus)" size="small">
                        {{ orderStatusLabel(detailData.order?.orderStatus) }}
                      </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="创建时间">{{ formatDateTime(detailData.order?.createTime) }}</el-descriptions-item>
                    <el-descriptions-item label="总数">{{ detailData.order?.totalCount ?? 0 }}</el-descriptions-item>
                    <el-descriptions-item label="成功">{{ detailData.order?.successCount ?? 0 }}</el-descriptions-item>
                    <el-descriptions-item label="失败">{{ detailData.order?.failedCount ?? 0 }}</el-descriptions-item>
                    <el-descriptions-item label="退款">{{ detailData.order?.refundAmount ?? 0 }}</el-descriptions-item>
                  </el-descriptions>

                  <el-table v-loading="resultLoading" :data="detailData.items || []" class="mt10">
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
                    <el-table-column label="处理时间" width="160" align="center">
                      <template #default="scope">
                        {{ formatDateTime(scope.row.processedTime) }}
                      </template>
                    </el-table-column>
                  </el-table>
                </template>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`${activePlatformName} - 任务记录`" name="record">
          <el-card shadow="never" body-class="search-card" class="record-search-card">
            <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
              <el-form-item label="订单号" prop="orderNo">
                <el-input v-model="queryParams.orderNo" placeholder="请输入订单号" clearable @keyup.enter="handleQuery" />
              </el-form-item>
              <el-form-item label="幂等号" prop="requestNo">
                <el-input v-model="queryParams.requestNo" placeholder="请输入请求幂等号" clearable @keyup.enter="handleQuery" />
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

          <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
              <el-button
                type="primary"
                plain
                icon="Plus"
                @click="activeSubTab = 'submit'"
                v-hasPermi="['server:markUser:order:add']"
              >
                提交订单
              </el-button>
            </el-col>
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
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
                <el-button
                  link
                  type="primary"
                  icon="View"
                  @click="openDetail(scope.row)"
                  v-hasPermi="['server:markUser:order:query']"
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
        </el-tab-pane>
      </el-tabs>
    </el-card>
    <el-dialog
      v-model="precheckDialogVisible"
      title="预查询结果确认"
      width="900px"
      destroy-on-close
      append-to-body
      @closed="handlePrecheckDialogClosed"
    >
      <div class="precheck-dialog-tip">
        平台：{{ precheckDialogData.platformName || activePlatformName }}，请确认预查询结果后决定是否提交。
      </div>

      <el-descriptions :column="5" border class="precheck-dialog-summary">
        <el-descriptions-item label="总查询">{{ precheckDialogData.totalCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="已标记">{{ precheckDialogData.markedCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="未标记">{{ precheckDialogData.unmarkedCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="失败">{{ precheckDialogData.failedCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="待提交">{{ precheckMarkedCount }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="precheckMarkedCount === 0"
        type="warning"
        :closable="false"
        show-icon
        title="本次预查询没有可提交的已标记号码。"
        class="mb10"
      />

      <el-table :data="precheckTableData" border max-height="360">
        <el-table-column label="序号" type="index" width="56" align="center" />
        <el-table-column label="号码" prop="phone" min-width="130" />
        <el-table-column label="查询状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="queryStatusType(scope.row)" size="small">
              {{ queryStatusLabel(scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标记结果" width="110" align="center">
          <template #default="scope">
            <el-tag :type="markStatusType(scope.row)" size="small">
              {{ markStatusLabel(scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态码" prop="status" width="120" align="center" show-overflow-tooltip />
        <el-table-column label="详情" prop="detail" min-width="180" show-overflow-tooltip />
        <el-table-column label="错误信息" prop="errorMessage" min-width="180" show-overflow-tooltip />
        <el-table-column label="响应时长(ms)" width="110" align="center">
          <template #default="scope">
            {{ scope.row.responseTime ?? '-' }}
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="precheckDialogVisible = false">不提交</el-button>
        <el-button
          type="primary"
          :disabled="precheckMarkedCount === 0"
          :loading="submitLoading"
          @click="confirmSubmitAfterPrecheck"
        >
          提交已标记号码（{{ precheckMarkedCount }}）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MarkUserOrder">
import {
  listMarkUserOrder,
  createMarkUserOrder,
  precheckMarkUserOrder,
  getMarkUserOrderDetail,
  listMarkUserPlatformPrice
} from '@/api/server/markUser'
import { useRoute } from 'vue-router'

const { proxy } = getCurrentInstance()
const route = useRoute()

const showSearch = ref(true)
const loading = ref(false)
const submitLoading = ref(false)
const resultLoading = ref(false)
const total = ref(0)
const orderList = ref([])
const platformOptions = ref([])
const activePlatformCode = ref('')
const activeSubTab = ref('submit')
const selectedResultOrderId = ref(null)
const loadedResultOrderId = ref(null)
const detailData = ref({ order: {}, items: [] })
const precheckDialogVisible = ref(false)
const pendingSubmitPayload = ref(null)

function createEmptyPrecheckData() {
  return {
    platformCode: '',
    platformName: '',
    totalCount: 0,
    markedCount: 0,
    unmarkedCount: 0,
    failedCount: 0,
    markedPhones: [],
    unmarkedPhones: [],
    failedPhones: [],
    items: []
  }
}

const precheckDialogData = ref(createEmptyPrecheckData())

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: null,
  requestNo: null,
  platformCode: null,
  orderStatus: null
})

const submitForm = reactive({
  phonesText: '',
  requestNo: '',
  remark: ''
})

const orderStatusOptions = [
  { label: '待处理', value: '0' },
  { label: '处理中', value: '1' },
  { label: '已完成', value: '2' },
  { label: '已取消', value: '3' }
]

const platformHintMap = {
  mobile_gaopin: '高频拦截处理移动高频、线路忙、用户忙等问题。',
  td_gaopin: '泰迪高频用于处理泰迪平台高频拦截问题。',
  td_second: '泰迪二次用于处理泰迪平台二次标记问题。',
  qihu_first: '360首次用于处理 360 首次标记问题。',
  qihu_second: '360二次用于处理 360 二次标记问题。',
  dianhuabang: '电话邦用于处理电话邦平台号码标记问题。',
  tencent_mark: '腾讯用于处理腾讯平台号码标记问题。'
}

const activePlatform = computed(() => {
  return platformOptions.value.find((item) => item.platformCode === activePlatformCode.value) || null
})

const activePlatformName = computed(() => {
  return activePlatform.value?.platformName || '平台'
})

const activeUnitPrice = computed(() => {
  const price = Number(activePlatform.value?.unitPrice ?? 1)
  return Number.isFinite(price) ? price : 1
})

const activePlatformHint = computed(() => {
  if (!activePlatform.value) return '请选择平台后提交号码。'
  return platformHintMap[activePlatform.value.platformCode] || `${activePlatformName.value}支持号码标记处理，请按行输入号码。`
})
const routePlatformCode = computed(() => String(route.query?.platformCode || '').trim())

function parsePhonesWithStats(text) {
  const source = String(text || '')
  const tokens = source
    .split(/[\n,，;；\s]+/)
    .map((item) => item.trim())
    .filter((item) => item.length > 0)

  const uniqueValid = []
  const validSet = new Set()
  let validRawCount = 0
  let invalidCount = 0

  tokens.forEach((token) => {
    const normalized = token.replace(/[^\d]/g, '')
    if (normalized.length < 7 || normalized.length > 15) {
      invalidCount += 1
      return
    }
    validRawCount += 1
    if (!validSet.has(normalized)) {
      validSet.add(normalized)
      uniqueValid.push(normalized)
    }
  })

  return {
    inputCount: tokens.length,
    validCount: uniqueValid.length,
    duplicateCount: Math.max(0, validRawCount - uniqueValid.length),
    invalidCount,
    validPhones: uniqueValid
  }
}

const submitPhoneStats = computed(() => parsePhonesWithStats(submitForm.phonesText))

const canSubmit = computed(() => {
  return !!activePlatform.value && submitPhoneStats.value.validCount > 0
})

const expectedSubmitCount = computed(() => submitPhoneStats.value.validCount)

const expectedDeductAmount = computed(() => expectedSubmitCount.value * activeUnitPrice.value)
const precheckTableData = computed(() => Array.isArray(precheckDialogData.value.items) ? precheckDialogData.value.items : [])
const precheckMarkedCount = computed(() => Array.isArray(precheckDialogData.value.markedPhones) ? precheckDialogData.value.markedPhones.length : 0)

function toSafeNumber(value, fallback = 0) {
  const num = Number(value)
  return Number.isFinite(num) ? num : fallback
}

function normalizePhoneList(source) {
  if (!Array.isArray(source)) return []
  const set = new Set()
  const result = []
  source.forEach((item) => {
    const phone = String(item || '').trim()
    if (!phone || set.has(phone)) return
    set.add(phone)
    result.push(phone)
  })
  return result
}

function queryStatusLabel(row) {
  if (row?.querySuccess === true) return '成功'
  if (row?.querySuccess === false) return '失败'
  return '-'
}

function queryStatusType(row) {
  if (row?.querySuccess === true) return 'success'
  if (row?.querySuccess === false) return 'danger'
  return 'info'
}

function markStatusLabel(row) {
  if (row?.querySuccess === false) return '失败'
  if (row?.marked === true) return '已标记'
  if (row?.marked === false) return '未标记'
  return '-'
}

function markStatusType(row) {
  if (row?.querySuccess === false) return 'danger'
  if (row?.marked === true) return 'success'
  if (row?.marked === false) return 'info'
  return ''
}

function handlePrecheckDialogClosed() {
  pendingSubmitPayload.value = null
  precheckDialogData.value = createEmptyPrecheckData()
}

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

function normalizeQueryPlatform() {
  queryParams.platformCode = activePlatformCode.value || routePlatformCode.value || null
}

function pickDefaultResultOrder() {
  if (!orderList.value.length) {
    selectedResultOrderId.value = null
    loadedResultOrderId.value = null
    detailData.value = { order: {}, items: [] }
    return
  }
  const exists = orderList.value.some((item) => item.id === selectedResultOrderId.value)
  if (!exists) {
    selectedResultOrderId.value = orderList.value[0].id
    loadedResultOrderId.value = null
  }
}

function getList() {
  normalizeQueryPlatform()
  loading.value = true
  return listMarkUserOrder(queryParams).then((res) => {
    orderList.value = res.rows || []
    total.value = res.total || 0
    pickDefaultResultOrder()
    if (activeSubTab.value === 'submit' && selectedResultOrderId.value) {
      loadOrderDetail(selectedResultOrderId.value)
    }
  }).finally(() => {
    loading.value = false
  })
}

function loadSummaryAndPrice() {
  return listMarkUserPlatformPrice().then((resp) => {
    const allList = Array.isArray(resp?.data) ? resp.data : []
    const menuPreferredPlatformCode = routePlatformCode.value
    if (menuPreferredPlatformCode) {
      const matched = allList.find((item) => item.platformCode === menuPreferredPlatformCode)
      platformOptions.value = matched ? [matched] : []
    } else {
      platformOptions.value = allList
    }

    if (platformOptions.value.length > 0) {
      const exists = platformOptions.value.some((item) => item.platformCode === activePlatformCode.value)
      if (!exists) {
        activePlatformCode.value = platformOptions.value[0].platformCode
      }
    } else {
      activePlatformCode.value = ''
    }
  }).catch((error) => {
    console.error('加载平台配置失败:', error)
    platformOptions.value = []
    activePlatformCode.value = ''
  })
}

function handlePlatformTabChange() {
  queryParams.pageNum = 1
  queryParams.orderNo = null
  queryParams.requestNo = null
  queryParams.orderStatus = null
  selectedResultOrderId.value = null
  loadedResultOrderId.value = null
  detailData.value = { order: {}, items: [] }
  if (proxy?.$refs?.queryRef) {
    proxy.resetForm('queryRef')
  }
  getList()
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.platformCode = activePlatformCode.value || routePlatformCode.value || null
  handleQuery()
}

function clearSubmitPhones() {
  submitForm.phonesText = ''
}

async function submitBatchOrder() {
  if (!activePlatform.value) {
    proxy.$modal.msgError('当前未选择平台')
    return
  }
  const phones = submitPhoneStats.value.validPhones
  if (phones.length === 0) {
    proxy.$modal.msgError('请输入有效号码')
    return
  }

  const payload = {
    platformCode: activePlatform.value.platformCode,
    platformName: activePlatform.value.platformName,
    requestNo: String(submitForm.requestNo || '').trim(),
    phones,
    remark: String(submitForm.remark || '').trim()
  }

  submitLoading.value = true
  try {
    const precheckRes = await precheckMarkUserOrder(payload)
    const source = precheckRes?.data || {}
    const markedPhones = normalizePhoneList(source.markedPhones)
    const unmarkedPhones = normalizePhoneList(source.unmarkedPhones)
    const failedPhones = normalizePhoneList(source.failedPhones)
    const items = Array.isArray(source.items) ? source.items : []

    precheckDialogData.value = {
      platformCode: source.platformCode || payload.platformCode,
      platformName: source.platformName || payload.platformName,
      totalCount: toSafeNumber(source.totalCount, phones.length),
      markedCount: toSafeNumber(source.markedCount, markedPhones.length),
      unmarkedCount: toSafeNumber(source.unmarkedCount, unmarkedPhones.length),
      failedCount: toSafeNumber(source.failedCount, failedPhones.length),
      markedPhones,
      unmarkedPhones,
      failedPhones,
      items
    }

    pendingSubmitPayload.value = {
      ...payload,
      phones: markedPhones
    }
    precheckDialogVisible.value = true

    if (markedPhones.length === 0) {
      if (precheckDialogData.value.failedCount > 0) {
        proxy.$modal.msgWarning(`预查询完成，失败 ${precheckDialogData.value.failedCount} 个，可提交 0 个`)
      } else {
        proxy.$modal.msgWarning('预查询未发现被标记号码，请确认后再决定是否重试')
      }
    }
  } catch (error) {
    console.error('预查询失败:', error)
    proxy.$modal.msgError(error?.message || '预查询失败')
  } finally {
    submitLoading.value = false
  }
}

async function confirmSubmitAfterPrecheck() {
  const payload = pendingSubmitPayload.value
  const markedCount = precheckMarkedCount.value
  if (!payload || markedCount <= 0) {
    proxy.$modal.msgWarning('预查询无可提交号码')
    return
  }

  submitLoading.value = true
  try {
    const res = await createMarkUserOrder(payload)
    proxy.$modal.msgSuccess(res.msg || '下单成功')
    const createdOrderId = res?.data?.order?.id
    precheckDialogVisible.value = false
    submitForm.phonesText = ''
    submitForm.requestNo = ''
    submitForm.remark = ''
    await loadSummaryAndPrice()
    await getList()
    if (createdOrderId) {
      selectedResultOrderId.value = createdOrderId
      loadOrderDetail(createdOrderId)
    }
    activeSubTab.value = 'submit'
  } catch (error) {
    console.error('提交订单失败:', error)
    proxy.$modal.msgError(error?.message || '提交失败')
  } finally {
    submitLoading.value = false
  }
}

function loadOrderDetail(orderId) {
  if (!orderId) return
  resultLoading.value = true
  return getMarkUserOrderDetail(orderId).then((res) => {
    detailData.value = res.data || { order: {}, items: [] }
    loadedResultOrderId.value = orderId
  }).finally(() => {
    resultLoading.value = false
  })
}

function handleResultOrderChange(orderId) {
  if (!orderId) {
    loadedResultOrderId.value = null
    detailData.value = { order: {}, items: [] }
    return
  }
  loadOrderDetail(orderId)
}

function refreshResultDetail() {
  if (!selectedResultOrderId.value) return
  loadOrderDetail(selectedResultOrderId.value)
}

function openDetail(row) {
  selectedResultOrderId.value = row.id
  activeSubTab.value = 'submit'
  loadOrderDetail(row.id)
}

watch(activeSubTab, (tab) => {
  if (tab !== 'submit') return
  if (!selectedResultOrderId.value && orderList.value.length > 0) {
    selectedResultOrderId.value = orderList.value[0].id
  }
  if (selectedResultOrderId.value && loadedResultOrderId.value !== selectedResultOrderId.value) {
    loadOrderDetail(selectedResultOrderId.value)
  }
})
watch(routePlatformCode, () => {
  loadSummaryAndPrice().then(() => {
    handlePlatformTabChange()
  })
})

onMounted(async () => {
  await loadSummaryAndPrice()
  handlePlatformTabChange()
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


.sub-tabs {
  margin-top: 2px;
}

.submit-pane {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 16px;
}

.submit-title {
  margin: 0 0 12px;
  font-size: 22px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.submit-tip {
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid #e6d8a6;
  border-radius: 4px;
  background: #faf4db;
  color: #606266;
  font-size: 14px;
}

.submit-stats {
  margin-top: 10px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.submit-charge-bar {
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid #a0cfff;
  border-radius: 4px;
  background: #ecf5ff;
  color: #303133;
  font-size: 14px;
}

.submit-actions {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.submit-result-wrap {
  margin-top: 16px;
}

.result-pane {
  padding-top: 4px;
}

.result-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.result-summary {
  margin-bottom: 10px;
}

.record-search-card {
  margin-bottom: 10px;
}

.precheck-dialog-tip {
  margin-bottom: 10px;
  color: var(--el-text-color-regular);
}

.precheck-dialog-summary {
  margin-bottom: 10px;
}

@media (max-width: 768px) {
  .submit-actions,
  .result-toolbar {
    flex-wrap: wrap;
  }

  .result-toolbar :deep(.el-select) {
    width: 100% !important;
  }
}
</style>
