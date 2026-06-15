<template>
  <div class="app-container mark-user-order-page">
    <div v-if="activeSubTab === 'submit' && platformOptions.length > 0" class="page-top-remain">
      <div class="submit-right-remain">
        当前剩余：<span>{{ activeRemainCount }}</span> 次
      </div>
    </div>
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
          <el-tabs v-model="activeSubTab" class="sub-tabs">
            <el-tab-pane :label="`${activePlatformName} - 提交号码`" name="submit">
              <div class="submit-pane">
                <div class="submit-grid">
                  <div class="submit-left">
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
                    <div v-if="insufficientRemain" class="submit-warning-bar">
                      当前平台剩余次数不足，请减少号码后再提交。
                    </div>
                    <div class="submit-actions">
                      <el-button
                        type="primary"
                        icon="Search"
                        :loading="precheckLoading"
                        :disabled="!canSubmit"
                        @click="submitBatchOrder"
                        v-hasPermi="['server:markUser:order:add']"
                      >
                        一键批量查询
                      </el-button>
                      <el-button @click="clearSubmitPhones">清空</el-button>
                    </div>
                  </div>

                  <div class="submit-right">
                    <div class="submit-right-content">
                      <h3 class="submit-title">查询结果</h3>
                      <div class="query-result-panel">
                        <div class="query-result-head">
                          <div class="query-result-actions">
                            <el-button
                              type="success"
                              size="small"
                              icon="CircleCheck"
                              :disabled="precheckSubmittableSelectedCount === 0 || clearSubmitLoading || precheckLoading"
                              :loading="clearSubmitLoading"
                              @click="submitSelectedMarkedPhones"
                            >
                              提交消除
                            </el-button>
                            <el-button
                              type="warning"
                              size="small"
                              icon="Select"
                              :disabled="!hasPrecheckResult"
                              @click="togglePrecheckSelectAll"
                            >
                              全选
                            </el-button>
                            <el-button
                              size="small"
                              icon="Delete"
                              :disabled="!hasPrecheckResult"
                              @click="resetPrecheckPanel"
                            >
                              清空结果
                            </el-button>
                            <el-button
                              size="small"
                              icon="DocumentCopy"
                              :disabled="!hasPrecheckResult"
                              @click="copyPrecheckPhones"
                            >
                              复制
                            </el-button>
                            <el-button
                              size="small"
                              icon="Download"
                              :disabled="!hasPrecheckResult"
                              @click="exportPrecheckRows"
                            >
                              导出
                            </el-button>
                          </div>
                        </div>
                        <div class="query-result-body">
                          <el-table
                            ref="precheckTableRef"
                            class="query-result-table"
                            :data="precheckTableData"
                            border
                            max-height="430"
                            row-key="phone"
                            @selection-change="handlePrecheckSelectionChange"
                          >
                            <el-table-column type="selection" width="48" reserve-selection />
                            <el-table-column label="手机号码" prop="phone" min-width="130" />
                            <el-table-column label="状态" width="80" align="center">
                              <template #default="scope">
                                <el-tag :type="queryStatusType(scope.row)" size="small">
                                  {{ queryStatusLabel(scope.row) }}
                                </el-tag>
                              </template>
                            </el-table-column>
                            <el-table-column :label="`查询结果(${activePlatformName})`" min-width="180" show-overflow-tooltip>
                              <template #default="scope">
                                {{ precheckResultLabel(scope.row) }}
                              </template>
                            </el-table-column>
                            <el-table-column v-if="isTencentPlatform" label="验证码" width="190" align="center">
                              <template #default="scope">
                                <template v-if="canSubmitTencentByRow(scope.row)">
                                  <el-input
                                    class="tencent-row-sms-input"
                                    :model-value="getTencentRowSmsCode(scope.row.phone)"
                                    maxlength="6"
                                    placeholder="请输入 6 位验证码"
                                    @update:model-value="(value) => handleTencentRowSmsCodeChange(scope.row.phone, value)"
                                  />
                                </template>
                                <span v-else class="tencent-row-disabled">-</span>
                              </template>
                            </el-table-column>
                            <el-table-column v-if="isTencentPlatform" label="提交" width="90" align="center">
                              <template #default="scope">
                                <template v-if="canSubmitTencentByRow(scope.row)">
                                  <el-button
                                    type="primary"
                                    size="small"
                                    :loading="getTencentRowLoading(scope.row.phone)"
                                    :disabled="activeRemainCount < 1 || !isTencentRowSmsCodeValid(scope.row.phone)"
                                    @click="submitTencentPhoneByRow(scope.row)"
                                    v-hasPermi="['server:markUser:order:add']"
                                  >
                                    提交
                                  </el-button>
                                </template>
                                <span v-else class="tencent-row-disabled">不可提交</span>
                              </template>
                            </el-table-column>
                            <el-table-column v-if="isTencentPlatform" label="显示结果" min-width="150" show-overflow-tooltip>
                              <template #default="scope">
                                {{ getTencentRowResultText(scope.row.phone) }}
                              </template>
                            </el-table-column>
                          </el-table>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="`${activePlatformName} - 任务记录`" name="record">
              <div class="record-search-panel">
                <div class="record-search-grid">
                  <div class="record-search-item record-search-item--keyword">
                    <label class="record-search-item__label">综合搜索（订单号/手机号/用户名）</label>
                    <el-input
                      v-model="queryParams.keyword"
                      clearable
                      placeholder="输入订单号、手机号或用户名"
                      @keyup.enter="handleQuery"
                    />
                  </div>
                  <div class="record-search-item">
                    <label class="record-search-item__label">处理状态</label>
                    <el-select
                      v-model="queryParams.orderStatus"
                      clearable
                      placeholder="选择或搜索状态"
                      style="width: 100%;"
                    >
                      <el-option label="待处理" value="0" />
                      <el-option label="处理中" value="1" />
                      <el-option label="已完成" value="2" />
                      <el-option label="已取消" value="3" />
                    </el-select>
                  </div>
                  <div class="record-search-item record-search-item--date">
                    <label class="record-search-item__label">提交时间</label>
                    <el-date-picker
                      v-model="recordDateRange"
                      type="daterange"
                      range-separator="-"
                      start-placeholder="yyyy / mm / dd"
                      end-placeholder="yyyy / mm / dd"
                      format="YYYY / MM / DD"
                      value-format="YYYY-MM-DD"
                      style="width: 100%;"
                      @change="handleRecordDateRangeChange"
                    />
                  </div>
                  <div class="record-search-item record-search-item--quick">
                    <label class="record-search-item__label">快捷筛选</label>
                    <div class="record-quick-group">
                      <el-button
                        :type="activeQuickRange === 'today' ? 'primary' : 'default'"
                        plain
                        size="small"
                        @click="setRecordQuickRange('today')"
                      >
                        今天
                      </el-button>
                      <el-button
                        :type="activeQuickRange === 'yesterday' ? 'primary' : 'default'"
                        plain
                        size="small"
                        @click="setRecordQuickRange('yesterday')"
                      >
                        昨天
                      </el-button>
                      <el-button
                        :type="activeQuickRange === 'week' ? 'primary' : 'default'"
                        plain
                        size="small"
                        @click="setRecordQuickRange('week')"
                      >
                        最近7天
                      </el-button>
                    </div>
                  </div>
                  <div class="record-search-item record-search-item--actions">
                    <label class="record-search-item__label">操作</label>
                    <div class="record-action-group">
                      <el-button @click="exportRecordRows">导出</el-button>
                      <el-button @click="resetQuery">重置/刷新</el-button>
                      <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
                    </div>
                  </div>
                </div>
              </div>

              <el-table
                v-loading="loading"
                :data="orderList"
                :row-key="recordRowKey"
                @selection-change="handleRecordSelectionChange"
              >
                <el-table-column type="selection" width="48" />
                <el-table-column label="订单号" prop="orderNo" min-width="180" show-overflow-tooltip />
                <el-table-column label="用户名" prop="userName" min-width="120" show-overflow-tooltip />
                <el-table-column label="号码（点击复制）" min-width="160" show-overflow-tooltip>
                  <template #default="scope">
                    <el-button link type="primary" @click="copyText(scope.row.phonePreview || '')">
                      {{ scope.row.phonePreview || '-' }}
                    </el-button>
                  </template>
                </el-table-column>
                <el-table-column label="平台" prop="platformName" min-width="120" show-overflow-tooltip />
                <el-table-column label="提交时间" min-width="160" align="center">
                  <template #default="scope">
                    {{ formatDateTime(scope.row.createTime) }}
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="92" align="center">
                  <template #default="scope">
                    <el-tag :type="recordStatusType(scope.row)" size="small">
                      {{ recordStatusLabel(scope.row) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="center">
                  <template #default="scope">
                    <el-button
                      link
                      type="primary"
                      icon="View"
                      @click="openRecordDetail(scope.row)"
                      v-hasPermi="['server:markUser:order:query']"
                    >
                      详情
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <el-dialog v-model="recordDetailOpen" title="任务详情" width="980px" append-to-body>
                <div v-loading="recordDetailLoading">
                  <el-descriptions :column="4" border>
                    <el-descriptions-item label="订单号">{{ recordDetailData.order?.orderNo || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="平台">{{ recordDetailData.order?.platformName || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="提交时间">{{ formatDateTime(recordDetailData.order?.createTime) }}</el-descriptions-item>
                    <el-descriptions-item label="状态">
                      <el-tag :type="recordStatusType(recordDetailData.order)" size="small">
                        {{ recordStatusLabel(recordDetailData.order) }}
                      </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="总数">{{ recordDetailData.order?.totalCount ?? 0 }}</el-descriptions-item>
                    <el-descriptions-item label="成功">{{ recordDetailData.order?.successCount ?? 0 }}</el-descriptions-item>
                    <el-descriptions-item label="失败">{{ recordDetailData.order?.failedCount ?? 0 }}</el-descriptions-item>
                    <el-descriptions-item label="退款">{{ recordDetailData.order?.refundAmount ?? 0 }}</el-descriptions-item>
                  </el-descriptions>

                  <el-table class="mt10" :data="recordDetailData.items || []" max-height="420">
                    <el-table-column label="序号" type="index" width="56" align="center" />
                    <el-table-column label="号码" prop="phone" min-width="130" />
                    <el-table-column label="状态" width="90" align="center">
                      <template #default="scope">
                        <el-tag :type="itemStatusType(scope.row.processStatus)" size="small">
                          {{ itemStatusLabel(scope.row.processStatus) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="处理结果" min-width="220">
                      <template #default="scope">
                        <div class="record-detail-text">{{ scope.row.processResult || '-' }}</div>
                      </template>
                    </el-table-column>
                    <el-table-column label="处理备注" min-width="320">
                      <template #default="scope">
                        <div class="record-detail-text">{{ scope.row.processNote || '-' }}</div>
                      </template>
                    </el-table-column>
                    <el-table-column label="处理时间" width="160" align="center">
                      <template #default="scope">
                        {{ formatDateTime(scope.row.processedTime) }}
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
                <template #footer>
                  <div class="dialog-footer">
                    <el-button @click="recordDetailOpen = false">关 闭</el-button>
                  </div>
                </template>
              </el-dialog>

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
  createMarkUserClearOrder,
  precheckMarkUserOrder,
  getMarkUserOrderDetail,
  listMarkUserPlatformPrice,
  submitMarkUserTencent
} from '@/api/server/markUser'
import { useRoute, useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const precheckLoading = ref(false)
const clearSubmitLoading = ref(false)
const total = ref(0)
const orderList = ref([])
const recordSelectedRows = ref([])
const platformOptions = ref([])
const activePlatformCode = ref('')
const activeSubTab = ref('submit')
const activeQuickRange = ref('')
const recordDateRange = ref([])
const precheckTableRef = ref(null)
const precheckSelectedRows = ref([])
const precheckSourcePayload = ref(null)
const precheckKeyword = ref('')
const precheckQueryStatus = ref('')
const precheckMarkStatus = ref('')
const recordDetailOpen = ref(false)
const recordDetailLoading = ref(false)
const recordDetailData = ref({ order: {}, items: [] })
const tencentRowStateMap = ref({})

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

function openRecordDetail(row) {
  const orderId = row?.id
  if (!orderId) return
  recordDetailOpen.value = true
  recordDetailLoading.value = true
  getMarkUserOrderDetail(orderId).then((res) => {
    recordDetailData.value = res.data || { order: {}, items: [] }
  }).catch((error) => {
    proxy.$modal.msgError(error?.message || '加载详情失败')
  }).finally(() => {
    recordDetailLoading.value = false
  })
}

const precheckDialogData = ref(createEmptyPrecheckData())

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: null,
  requestNo: null,
  keyword: null,
  phone: null,
  platformCode: null,
  orderStatus: null,
  params: {}
})

const submitForm = reactive({
  phonesText: '',
  requestNo: '',
  remark: ''
})

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
const isTencentPlatform = computed(() => {
  const code = String(activePlatform.value?.platformCode || '').toLowerCase()
  const name = String(activePlatform.value?.platformName || '')
  return ['tencent_mark', 'tencent', 'tx', 'txwz'].includes(code) || name.includes('腾讯')
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
const canSubmit = computed(() => !!activePlatform.value && submitPhoneStats.value.validCount > 0)
const expectedSubmitCount = computed(() => submitPhoneStats.value.validCount)
const expectedDeductAmount = computed(() => expectedSubmitCount.value * activeUnitPrice.value)
const insufficientRemain = computed(() => expectedDeductAmount.value > activeRemainCount.value)
const precheckTableData = computed(() => Array.isArray(precheckDialogData.value.items) ? precheckDialogData.value.items : [])
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

const precheckSelectedPhones = computed(() => normalizePhoneList(precheckSelectedRows.value.map((item) => item.phone)))
const precheckMarkedVisibleRows = computed(() => {
  return precheckFilteredTableData.value.filter((row) => row?.querySuccess === true && row?.marked === true)
})
const precheckSubmittableSelectedPhones = computed(() => {
  return normalizePhoneList(
    precheckSelectedRows.value
      .filter((row) => row?.querySuccess === true)
      .map((item) => item.phone)
  )
})
const precheckSubmittableSelectedCount = computed(() => precheckSubmittableSelectedPhones.value.length)

const allVisibleMarkedPrecheckSelected = computed(() => {
  const visibleMarked = normalizePhoneList(precheckMarkedVisibleRows.value.map((item) => item.phone))
  if (visibleMarked.length === 0) return false
  const selectedSet = new Set(precheckSelectedPhones.value)
  return visibleMarked.every((phone) => selectedSet.has(phone))
})

function toSafeNumber(value, fallback = 0) {
  const num = Number(value)
  return Number.isFinite(num) ? num : fallback
}

function displayValue(value) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'boolean') return value ? 'true' : 'false'
  return String(value)
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

function resetTencentRowStates() {
  tencentRowStateMap.value = {}
}

function getTencentRowState(phone, createIfMissing = false) {
  const normalizedPhone = normalizeTencentPhone(phone)
  if (!normalizedPhone) {
    return null
  }
  let state = tencentRowStateMap.value[normalizedPhone]
  if (!state && createIfMissing) {
    state = {
      smsCode: '',
      loading: false,
      resultText: '-'
    }
    tencentRowStateMap.value[normalizedPhone] = state
  }
  return state
}

function resolveTencentRowSubmitMode(row) {
  if (!isTencentPlatform.value || row?.querySuccess !== true) {
    return ''
  }
  const resultText = `${row?.status || ''} ${row?.detail || ''}`.replace(/\s+/g, '')
  if (resultText.includes('多人标记') || resultText.includes('多人举报') || resultText.includes('多人投诉')) {
    return 'tamper'
  }
  if (resultText.includes('骚扰电话') || resultText.includes('骚扰')) {
    return 'normal'
  }
  return ''
}

function canSubmitTencentByRow(row) {
  return !!resolveTencentRowSubmitMode(row)
}

function getTencentRowSmsCode(phone) {
  return getTencentRowState(phone, false)?.smsCode || ''
}

function handleTencentRowSmsCodeChange(phone, value) {
  const state = getTencentRowState(phone, true)
  if (!state) {
    return
  }
  state.smsCode = normalizeTencentPhone(value).slice(0, 6)
}

function getTencentRowLoading(phone) {
  return getTencentRowState(phone, false)?.loading === true
}

function isTencentRowSmsCodeValid(phone) {
  return /^\d{6}$/.test(getTencentRowSmsCode(phone))
}

function getTencentRowResultText(phone) {
  return getTencentRowState(phone, false)?.resultText || '-'
}

function buildTencentRowResultText(result, mode) {
  const accepted = result?.accepted === true
  const reCode = displayValue(result?.submitReCode)
  const data = displayValue(result?.submitData)
  const modeText = mode === 'tamper' ? '篡改' : '直提'
  return `${accepted ? '成功' : '失败'}（${modeText}，reCode=${reCode}，data=${data}）`
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

function precheckResultLabel(row) {
  if (row?.querySuccess === false) return row?.errorMessage || row?.detail || '查询失败'
  if (row?.detail) return row.detail
  const statusText = markStatusLabel(row)
  return statusText === '-' ? '查询成功' : statusText
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
  precheckSourcePayload.value = null
  precheckKeyword.value = ''
  precheckQueryStatus.value = ''
  precheckMarkStatus.value = ''
  resetTencentRowStates()
  precheckDialogData.value = createEmptyPrecheckData()
  precheckSelectedRows.value = []
  precheckTableRef.value?.clearSelection?.()
}

async function executePrecheck(payload, { silentWarning = false } = {}) {
  precheckLoading.value = true
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

    precheckSourcePayload.value = {
      ...payload,
      phones: [...payload.phones]
    }

    precheckKeyword.value = ''
    precheckQueryStatus.value = ''
    precheckMarkStatus.value = ''
    resetTencentRowStates()
    precheckSelectedRows.value = []
    precheckTableRef.value?.clearSelection?.()

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
    precheckLoading.value = false
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

function handlePrecheckSelectionChange(rows) {
  precheckSelectedRows.value = Array.isArray(rows) ? rows : []
}

function togglePrecheckSelectAll() {
  if (!hasPrecheckResult.value) return
  const markedRows = precheckMarkedVisibleRows.value
  if (allVisibleMarkedPrecheckSelected.value) {
    precheckTableRef.value?.clearSelection?.()
    return
  }
  precheckTableRef.value?.clearSelection?.()
  if (!markedRows.length) return
  markedRows.forEach((row) => {
    precheckTableRef.value?.toggleRowSelection?.(row, true)
  })
}

async function copyText(text) {
  const value = String(text || '').trim()
  if (!value) {
    proxy.$modal.msgWarning('没有可复制内容')
    return
  }
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(value)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = value
      textarea.style.position = 'fixed'
      textarea.style.left = '-9999px'
      document.body.appendChild(textarea)
      textarea.focus()
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    proxy.$modal.msgSuccess('复制成功')
  } catch (error) {
    proxy.$modal.msgError('复制失败，请检查浏览器权限')
  }
}

function buildCsvCell(value) {
  const text = String(value ?? '')
  return `"${text.replace(/"/g, '""')}"`
}

function downloadCsv(filename, rows) {
  const csvText = `\ufeff${rows.map((row) => row.map((cell) => buildCsvCell(cell)).join(',')).join('\n')}`
  const blob = new Blob([csvText], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

function exportPrecheckRows() {
  const rows = precheckSelectedRows.value.length > 0 ? precheckSelectedRows.value : precheckFilteredTableData.value
  if (!rows.length) {
    proxy.$modal.msgWarning('暂无可导出数据')
    return
  }
  const header = ['号码', '查询状态', '标记结果', '状态码', '详情', '错误信息', '响应时长(ms)']
  const body = rows.map((item) => [
    item.phone || '',
    queryStatusLabel(item),
    markStatusLabel(item),
    item.status || '',
    item.detail || '',
    item.errorMessage || '',
    item.responseTime ?? ''
  ])
  downloadCsv(`mark-precheck-${Date.now()}.csv`, [header, ...body])
}

async function copyPrecheckPhones() {
  const phones = precheckSelectedRows.value.length > 0
    ? precheckSelectedPhones.value
    : normalizePhoneList(precheckFilteredTableData.value.map((item) => item.phone))
  if (!phones.length) {
    proxy.$modal.msgWarning('没有可复制号码')
    return
  }
  await copyText(phones.join('\n'))
}

function recordStatusLabel(row) {
  const status = String(row?.orderStatus ?? '')
  const successCount = Number(row?.successCount ?? 0)
  const failedCount = Number(row?.failedCount ?? 0)
  if (status === '0' || status === '1') return '待处理'
  if (status === '2') return failedCount > 0 && successCount <= 0 ? '失败' : (failedCount > 0 ? '失败' : '成功')
  if (status === '3') return '失败'
  return '待处理'
}

function recordStatusType(row) {
  const label = recordStatusLabel(row)
  if (label === '成功') return 'success'
  if (label === '失败') return 'danger'
  return 'warning'
}

function itemStatusLabel(status) {
  const map = { '0': '待处理', '1': '成功', '2': '失败' }
  return map[status] || '-'
}

function itemStatusType(status) {
  if (status === '1') return 'success'
  if (status === '2') return 'danger'
  return 'warning'
}

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function formatDateOnly(value) {
  const d = new Date(value)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function recordRowKey(row) {
  return `${row?.id ?? ''}-${row?.phonePreview ?? ''}`
}

function normalizeQueryPlatform() {
  queryParams.platformCode = activePlatformCode.value || routePlatformCode.value || null
}

function normalizeRecordKeyword() {
  const keyword = String(queryParams.keyword || '').trim()
  queryParams.keyword = keyword || null
  if (keyword && /^\d{7,15}$/.test(keyword)) {
    queryParams.phone = keyword
    return
  }
  queryParams.phone = null
}

function getList() {
  normalizeQueryPlatform()
  normalizeRecordKeyword()
  loading.value = true
  return listMarkUserOrder(queryParams).then((res) => {
    orderList.value = res.rows || []
    total.value = res.total || 0
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
  queryParams.keyword = null
  queryParams.phone = null
  queryParams.orderStatus = null
  queryParams.params = {}
  recordDateRange.value = []
  activeQuickRange.value = ''
  recordSelectedRows.value = []
  getList()
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.orderNo = null
  queryParams.requestNo = null
  queryParams.keyword = null
  queryParams.phone = null
  queryParams.orderStatus = null
  queryParams.params = {}
  recordDateRange.value = []
  activeQuickRange.value = ''
  queryParams.platformCode = activePlatformCode.value || routePlatformCode.value || null
  handleQuery()
}

function handleRecordDateRangeChange(value) {
  activeQuickRange.value = ''
  if (!Array.isArray(value) || value.length !== 2 || !value[0] || !value[1]) {
    recordDateRange.value = []
    queryParams.params = {}
    return
  }
  queryParams.params = {
    beginTime: value[0],
    endTime: value[1]
  }
}

function clearSubmitPhones() {
  submitForm.phonesText = ''
}

function normalizeTencentPhone(value) {
  return String(value || '').replace(/[^\d]/g, '')
}
async function submitTencentPhoneByRow(row) {
  if (!canSubmitTencentByRow(row)) {
    proxy.$modal.msgWarning('该查询结果不支持短信提交')
    return
  }
  const phone = normalizeTencentPhone(row?.phone)
  if (!/^\d{7,15}$/.test(phone)) {
    proxy.$modal.msgError('手机号格式不正确，请输入 7-15 位数字')
    return
  }
  const state = getTencentRowState(phone, true)
  if (!state) {
    proxy.$modal.msgError('初始化行状态失败')
    return
  }
  const smsCode = normalizeTencentPhone(state.smsCode)
  state.smsCode = smsCode
  if (!/^\d{6}$/.test(smsCode)) {
    proxy.$modal.msgError('验证码应为 6 位数字')
    return
  }
  const mode = resolveTencentRowSubmitMode(row)
  const forceTamper = mode === 'tamper'
  state.loading = true
  try {
    const res = await submitMarkUserTencent({ phone, smsCode, forceTamper })
    const result = res?.data || null
    state.resultText = buildTencentRowResultText(result, mode)
    if (result?.accepted === true) {
      await Promise.all([loadSummaryAndPrice(), getList()])
      proxy.$modal.msgSuccess(forceTamper ? '腾讯篡改提交受理成功' : '腾讯受理成功')
    } else {
      await getList()
      proxy.$modal.msgWarning('腾讯受理失败，请查看显示结果')
    }
  } catch (error) {
    state.resultText = `提交异常（${String(error?.message || '未知错误')}）`
    console.error('腾讯提交失败:', error)
    proxy.$modal.msgError(error?.message || '腾讯提交失败')
  } finally {
    state.loading = false
  }
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

async function afterCreateOrderSuccess(res) {
  proxy.$modal.msgSuccess(res?.msg || '下单成功')
  resetPrecheckPanel()
  submitForm.phonesText = ''
  submitForm.requestNo = ''
  submitForm.remark = ''
  await loadSummaryAndPrice()
  await getList()
  activeSubTab.value = 'submit'
}

async function submitSelectedMarkedPhones() {
  const selectedPhones = precheckSubmittableSelectedPhones.value
  if (!selectedPhones.length) {
    proxy.$modal.msgWarning('请先勾选可提交号码（仅支持查询成功记录）')
    return
  }
  if (!precheckSourcePayload.value) {
    proxy.$modal.msgWarning('暂无可提交结果，请先执行预查询')
    return
  }
  const finalDeductAmount = selectedPhones.length * activeUnitPrice.value
  if (finalDeductAmount > activeRemainCount.value) {
    proxy.$modal.msgError('当前平台剩余次数不足，请减少提交数量')
    return
  }

  const payload = {
    ...precheckSourcePayload.value,
    phones: selectedPhones
  }
  clearSubmitLoading.value = true
  try {
    const res = await createMarkUserClearOrder(payload)
    await afterCreateOrderSuccess(res)
  } catch (error) {
    console.error('提交订单失败:', error)
    proxy.$modal.msgError(error?.message || '提交失败')
  } finally {
    clearSubmitLoading.value = false
  }
}

function setRecordQuickRange(type) {
  if (activeQuickRange.value === type) {
    activeQuickRange.value = ''
    recordDateRange.value = []
    queryParams.params = {}
    handleQuery()
    return
  }

  const now = new Date()
  const today = formatDateOnly(now)
  let beginTime = today
  let endTime = today

  if (type === 'yesterday') {
    const yesterday = new Date(now)
    yesterday.setDate(yesterday.getDate() - 1)
    beginTime = formatDateOnly(yesterday)
    endTime = beginTime
  } else if (type === 'week') {
    const weekStart = new Date(now)
    weekStart.setDate(weekStart.getDate() - 6)
    beginTime = formatDateOnly(weekStart)
    endTime = today
  }

  activeQuickRange.value = type
  recordDateRange.value = [beginTime, endTime]
  queryParams.params = { beginTime, endTime }
  handleQuery()
}

function handleRecordSelectionChange(rows) {
  recordSelectedRows.value = Array.isArray(rows) ? rows : []
}

function exportRecordRows() {
  const rows = recordSelectedRows.value.length > 0 ? recordSelectedRows.value : orderList.value
  if (!rows.length) {
    proxy.$modal.msgWarning('暂无可导出记录')
    return
  }
  const header = ['订单号', '用户名', '手机号', '平台', '提交时间', '状态']
  const body = rows.map((item) => [
    item.orderNo || '',
    item.userName || '',
    item.phonePreview || '',
    item.platformName || '',
    formatDateTime(item.createTime),
    recordStatusLabel(item)
  ])
  downloadCsv(`mark-record-${Date.now()}.csv`, [header, ...body])
}

watch(routePlatformCode, () => {
  loadSummaryAndPrice().then(() => {
    handlePlatformTabChange()
  })
})
watch(activePlatformCode, () => {
  resetTencentRowStates()
})

onMounted(async () => {
  await loadSummaryAndPrice()
  handlePlatformTabChange()
})
</script>

<style scoped>
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
.page-top-remain {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
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
  grid-template-columns: minmax(0, 540px) minmax(0, 1fr);
  width: 100%;
  min-height: 560px;
  gap: 16px;
  align-items: stretch;
}

.submit-left,
.submit-right {
  min-width: 0;
}

.submit-right {
  display: flex;
  width: 100%;
}
.submit-right-content {
  display: flex;
  flex-direction: column;
  width: 100%;
  flex: 1;
  min-width: 0;
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
  flex-wrap: wrap;
}

.tencent-row-sms-input {
  width: 150px;
}

.tencent-row-disabled {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.record-detail-text {
  white-space: pre-wrap;
  line-height: 1.45;
  color: var(--el-text-color-regular);
  word-break: break-word;
}

.query-result-panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
  width: 100%;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  color: var(--el-text-color-primary);
}

.query-result-head {
  display: flex;
  align-items: center;
  padding: 12px 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: #f8fbff;
}

.query-result-head-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-width: 0;
}



.query-result-actions {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}
.query-result-actions :deep(.el-button) {
  min-height: 34px;
  padding: 0 16px;
  font-size: 14px;
}

.query-result-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 12px;
}

.query-result-table :deep(.el-scrollbar__bar.is-horizontal) {
  display: none !important;
}

.query-result-summary {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.precheck-filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}


.query-result-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  color: var(--el-text-color-secondary);
}

.record-search-panel {
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: #fff;
  padding: 12px 14px;
}

.record-search-grid {
  display: grid;
  grid-template-columns: minmax(260px, 2.2fr) minmax(160px, 1fr) minmax(320px, 2fr) auto auto;
  gap: 12px;
  align-items: end;
}

.record-search-item {
  min-width: 0;
}

.record-search-item__label {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.record-quick-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.record-action-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
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

  .query-result-panel {
    min-height: 420px;
  }

  .query-result-actions {
    width: 100%;
  }

  .query-result-actions :deep(.el-button) {
    flex: 1 1 calc(50% - 8px);
  }

  .precheck-filter-bar :deep(.el-select),
  .precheck-filter-bar :deep(.el-input) {
    width: 100% !important;
  }
  .record-search-grid {
    grid-template-columns: 1fr;
  }

  .record-action-group,
  .record-quick-group {
    width: 100%;
    min-width: 100%;
  }
}
</style>