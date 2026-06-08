<template>
  <div class="app-container mark-user-order-page">

    <el-card shadow="never" class="platform-card">

      <el-empty v-if="platformOptions.length === 0" description="当前未配置可用平台" />
      <div v-else :class="['platform-layout', { 'platform-layout--single': !showPlatformSwitcher }]">
        <el-tabs
          v-if="showPlatformSwitcher"
          v-model="activePlatformCode"
          tab-position="left"
          class="platform-nav-tabs"
          @tab-change="handlePlatformTabChange"
        >
          <el-tab-pane
            v-for="item in platformOptions"
            :key="item.platformCode"
            :label="item.platformName"
            :name="item.platformCode"
          />
        </el-tabs>

        <div class="platform-main">
          <el-tabs
            v-model="activeSubTab"
            class="sub-tabs"
          >
            <el-tab-pane :label="`${activePlatformName} - 提交号码`" name="submit">
              <div class="submit-pane">
                <h3 class="submit-title">{{ activePlatformName }} - 提交号码</h3>
                <div class="submit-grid">
                  <div class="submit-left">
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
                    <div v-if="insufficientRemain" class="submit-warning-bar">
                      当前平台剩余次数不足，请减少号码后再提交。
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
                      <el-button
                        type="warning"
                        icon="Promotion"
                        :loading="submitLoading"
                        :disabled="!canDirectSubmit"
                        @click="submitDirectOrder"
                        v-hasPermi="['server:markUser:order:add']"
                      >
                        直接提交号码
                      </el-button>
                      <el-button @click="clearSubmitPhones">清空</el-button>
                    </div>
                  </div>
                  <div class="submit-right">
                    <div class="submit-right-remain-wrap">
                      <div class="submit-right-remain">
                        当前剩余：<span>{{ activeRemainCount }}</span> 次
                      </div>
                    </div>
                    <div class="precheck-panel">
                      <div class="precheck-panel-head">
                        <span class="precheck-panel-title">提交号码状态</span>
                        <div class="precheck-panel-toolbar">
                          <el-button
                            link
                            icon="Refresh"
                            :disabled="!hasPrecheckResult || submitLoading"
                            @click="refreshPrecheckResult"
                          >
                            刷新
                          </el-button>
                          <el-button
                            link
                            :disabled="!hasPrecheckResult || submitLoading"
                            @click="resetPrecheckPanel"
                          >
                            清空
                          </el-button>
                        </div>
                      </div>
                      <template v-if="hasPrecheckResult">
                        <el-descriptions :column="5" border class="precheck-panel-summary">
                          <el-descriptions-item label="总查询">{{ precheckDialogData.totalCount || 0 }}</el-descriptions-item>
                          <el-descriptions-item label="已标记">{{ precheckDialogData.markedCount || 0 }}</el-descriptions-item>
                          <el-descriptions-item label="未标记">{{ precheckDialogData.unmarkedCount || 0 }}</el-descriptions-item>
                          <el-descriptions-item label="失败">{{ precheckDialogData.failedCount || 0 }}</el-descriptions-item>
                          <el-descriptions-item label="待提交">{{ precheckMarkedCount }}</el-descriptions-item>
                        </el-descriptions>

                        <div class="precheck-filter-bar">
                          <el-input
                            v-model="precheckKeyword"
                            clearable
                            placeholder="搜索号码/状态码/详情"
                            style="width: 240px;"
                          />
                          <el-select
                            v-model="precheckQueryStatus"
                            clearable
                            placeholder="查询状态"
                            style="width: 120px;"
                          >
                            <el-option label="成功" value="success" />
                            <el-option label="失败" value="failed" />
                          </el-select>
                          <el-select
                            v-model="precheckMarkStatus"
                            clearable
                            placeholder="标记状态"
                            style="width: 120px;"
                          >
                            <el-option label="已标记" value="marked" />
                            <el-option label="未标记" value="unmarked" />
                            <el-option label="失败" value="failed" />
                          </el-select>
                        </div>

                        <el-table :data="precheckFilteredTableData" border max-height="360">
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

                        <div class="precheck-submit-actions">
                          <el-button
                            type="primary"
                            :disabled="precheckMarkedCount === 0"
                            :loading="submitLoading"
                            @click="confirmSubmitAfterPrecheck"
                          >
                            提交已标记号码（{{ precheckMarkedCount }}）
                          </el-button>
                        </div>
                      </template>
                      <el-empty v-else description="暂未生成预查询结果，请先点击左侧“一键批量查询”" />
                    </div>
                  </div>
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
        </div>
      </div>
    </el-card>
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
import { useRoute, useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

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
const pendingSubmitPayload = ref(null)
const precheckSourcePayload = ref(null)
const precheckKeyword = ref('')
const precheckQueryStatus = ref('')
const precheckMarkStatus = ref('')

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
const activeRemainCount = computed(() => {
  const remain = Number(activePlatform.value?.remainCount ?? 0)
  return Number.isFinite(remain) ? Math.max(0, remain) : 0
})

const activePlatformHint = computed(() => {
  if (!activePlatform.value) return '请选择平台后提交号码。'
  return platformHintMap[activePlatform.value.platformCode] || `${activePlatformName.value}支持号码标记处理，请按行输入号码。`
})
const routePlatformCode = computed(() => String(route.query?.platformCode || '').trim())
const showPlatformSwitcher = computed(() => !routePlatformCode.value)

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
const expectedRemainAfterSubmit = computed(() => activeRemainCount.value - expectedDeductAmount.value)
const insufficientRemain = computed(() => expectedDeductAmount.value > activeRemainCount.value)
const canDirectSubmit = computed(() => canSubmit.value && !insufficientRemain.value)
const precheckTableData = computed(() => Array.isArray(precheckDialogData.value.items) ? precheckDialogData.value.items : [])
const precheckMarkedCount = computed(() => Array.isArray(precheckDialogData.value.markedPhones) ? precheckDialogData.value.markedPhones.length : 0)
const hasPrecheckResult = computed(() => precheckTableData.value.length > 0 || precheckDialogData.value.totalCount > 0)
const precheckFilteredTableData = computed(() => {
  const keyword = String(precheckKeyword.value || '').trim().toLowerCase()
  return precheckTableData.value.filter((row) => {
    if (precheckQueryStatus.value && resolvePrecheckQueryStatus(row) !== precheckQueryStatus.value) {
      return false
    }
    if (precheckMarkStatus.value && resolvePrecheckMarkStatus(row) !== precheckMarkStatus.value) {
      return false
    }
    if (!keyword) return true
    const searchable = [row?.phone, row?.status, row?.detail, row?.errorMessage]
      .map((item) => String(item || '').toLowerCase())
      .join(' ')
    return searchable.includes(keyword)
  })
})

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
function resolvePrecheckQueryStatus(row) {
  if (row?.querySuccess === true) return 'success'
  if (row?.querySuccess === false) return 'failed'
  return ''
}

function resolvePrecheckMarkStatus(row) {
  if (row?.querySuccess === false) return 'failed'
  if (row?.marked === true) return 'marked'
  if (row?.marked === false) return 'unmarked'
  return ''
}

function resetPrecheckPanel() {
  pendingSubmitPayload.value = null
  precheckSourcePayload.value = null
  precheckKeyword.value = ''
  precheckQueryStatus.value = ''
  precheckMarkStatus.value = ''
  precheckDialogData.value = createEmptyPrecheckData()
}

async function executePrecheck(payload, { silentWarning = false } = {}) {
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
      totalCount: toSafeNumber(source.totalCount, payload.phones.length),
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
    precheckSourcePayload.value = {
      ...payload,
      phones: [...payload.phones]
    }
    precheckKeyword.value = ''
    precheckQueryStatus.value = ''
    precheckMarkStatus.value = ''

    if (!silentWarning && markedPhones.length === 0) {
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

async function refreshPrecheckResult() {
  if (!precheckSourcePayload.value) {
    proxy.$modal.msgWarning('暂无可刷新的预查询结果')
    return
  }
  await executePrecheck(
    { ...precheckSourcePayload.value, phones: [...precheckSourcePayload.value.phones] },
    { silentWarning: true }
  )
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
      if (matched) {
        platformOptions.value = [matched]
      } else if (allList.length > 0) {
        const fallback = allList[0]
        platformOptions.value = [fallback]
        const nextQuery = {
          ...route.query,
          platformCode: fallback.platformCode,
          platformName: fallback.platformName
        }
        const routeCode = String(route.query?.platformCode || '')
        const routeName = String(route.query?.platformName || '')
        if (routeCode !== fallback.platformCode || routeName !== fallback.platformName) {
          router.replace({ path: route.path, query: nextQuery }).catch(() => {})
          proxy?.$modal?.msgWarning('当前平台已变更，已自动切换到可用平台')
        }
      } else {
        platformOptions.value = []
      }
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
  await executePrecheck(payload)
}

async function submitDirectOrder() {
  if (!activePlatform.value) {
    proxy.$modal.msgError('当前未选择平台')
    return
  }
  const phones = submitPhoneStats.value.validPhones
  if (phones.length === 0) {
    proxy.$modal.msgError('请输入有效号码')
    return
  }
  if (insufficientRemain.value) {
    proxy.$modal.msgError('当前平台剩余次数不足')
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
    const res = await createMarkUserOrder(payload)
    await afterCreateOrderSuccess(res)
  } catch (error) {
    console.error('直接提交订单失败:', error)
    proxy.$modal.msgError(error?.message || '提交失败')
  } finally {
    submitLoading.value = false
  }
}

async function afterCreateOrderSuccess(res) {
  proxy.$modal.msgSuccess(res?.msg || '下单成功')
  const createdOrderId = res?.data?.order?.id
  resetPrecheckPanel()
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
}

async function confirmSubmitAfterPrecheck() {
  const payload = pendingSubmitPayload.value
  const markedCount = precheckMarkedCount.value
  if (!payload || markedCount <= 0) {
    proxy.$modal.msgWarning('预查询无可提交号码')
    return
  }
  const finalDeductAmount = markedCount * activeUnitPrice.value
  if (finalDeductAmount > activeRemainCount.value) {
    proxy.$modal.msgError('当前平台剩余次数不足，请减少提交数量')
    return
  }

  submitLoading.value = true
  try {
    const res = await createMarkUserOrder(payload)
    await afterCreateOrderSuccess(res)
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
.platform-layout {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.platform-layout--single {
  display: block;
}

.platform-nav-tabs {
  flex: 0 0 220px;
  max-width: 220px;
}

.platform-nav-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.platform-nav-tabs :deep(.el-tabs__content) {
  display: none;
}

.platform-nav-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.platform-nav-tabs :deep(.el-tabs__item) {
  justify-content: flex-start;
  text-align: left;
  white-space: normal;
  line-height: 20px;
  height: auto;
  padding: 10px 12px;
}

.platform-main {
  flex: 1;
  min-width: 0;
  position: relative;
}


.sub-tabs {
  margin-top: 2px;
}

.submit-pane {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 16px;
}

.submit-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 520px);
  width: 100%;
  max-width: none;
  gap: 16px;
  align-items: start;
}

.submit-left,
.submit-right {
  min-width: 0;
}

.submit-right {
  width: 100%;
  max-width: 520px;
  justify-self: end;
}

.submit-right-remain-wrap {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.submit-right-remain {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border: 1px solid #a0cfff;
  border-radius: 4px;
  background: #ecf5ff;
  color: #303133;
  font-size: 13px;
  line-height: 1;
}

.submit-right-remain span {
  color: #409eff;
  font-weight: 600;
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

.submit-warning-bar {
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid #f56c6c;
  border-radius: 4px;
  background: #fef0f0;
  color: #c45656;
  font-size: 14px;
}

.submit-stats {
  margin-top: 10px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.submit-actions {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.precheck-panel {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 12px;
  background: var(--el-fill-color-blank);
}

.precheck-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.precheck-panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.precheck-panel-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.precheck-panel-tip {
  margin-bottom: 10px;
  color: var(--el-text-color-regular);
}

.precheck-panel-summary {
  margin-bottom: 10px;
}

.precheck-filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.precheck-submit-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
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

@media (max-width: 768px) {
  .platform-layout {
    display: block;
  }

  .platform-nav-tabs {
    max-width: none;
    margin-bottom: 12px;
  }

  .platform-nav-tabs :deep(.el-tabs__header) {
    margin-bottom: 0;
  }
  .submit-grid {
    grid-template-columns: 1fr;
  }
  .submit-actions,
  .result-toolbar {
    flex-wrap: wrap;
  }

  .precheck-submit-actions {
    justify-content: flex-start;
  }

  .result-toolbar :deep(.el-select),
  .precheck-filter-bar :deep(.el-select),
  .precheck-filter-bar :deep(.el-input) {
    width: 100% !important;
  }
}
</style>
