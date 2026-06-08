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

                    <div class="query-result-panel">
                      <div class="query-result-head">
                        <div class="query-result-head-left">
                          <span class="query-result-title">查询结果</span>
                          <span class="query-result-platform">{{ activePlatformName }}</span>
                        </div>
                        <div class="query-result-actions">
                          <el-button
                            type="success"
                            size="small"
                            :disabled="precheckMarkedSelectedCount === 0 || submitLoading"
                            :loading="submitLoading"
                            @click="submitSelectedMarkedPhones"
                          >
                            提交消除
                          </el-button>
                          <el-button
                            type="warning"
                            size="small"
                            :disabled="!hasPrecheckResult"
                            @click="togglePrecheckSelectAll"
                          >
                            全选
                          </el-button>
                          <el-button
                            size="small"
                            :disabled="!hasPrecheckResult"
                            @click="resetPrecheckPanel"
                          >
                            清空结果
                          </el-button>
                          <el-button
                            size="small"
                            :disabled="!hasPrecheckResult"
                            @click="copyPrecheckPhones"
                          >
                            复制
                          </el-button>
                          <el-button
                            size="small"
                            :disabled="!hasPrecheckResult"
                            @click="exportPrecheckRows"
                          >
                            导出
                          </el-button>
                        </div>
                      </div>

                      <div class="query-result-body">
                        <template v-if="hasPrecheckResult">
                          <div class="query-result-summary">
                            <span>总查询：{{ precheckDialogData.totalCount || 0 }}</span>
                            <span>已标记：{{ precheckDialogData.markedCount || 0 }}</span>
                            <span>未标记：{{ precheckDialogData.unmarkedCount || 0 }}</span>
                            <span>失败：{{ precheckDialogData.failedCount || 0 }}</span>
                            <span>已选可提交：{{ precheckMarkedSelectedCount }}</span>
                          </div>

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
                            <el-button
                              link
                              icon="Refresh"
                              :disabled="submitLoading"
                              @click="refreshPrecheckResult"
                            >
                              刷新
                            </el-button>
                          </div>

                          <el-table
                            ref="precheckTableRef"
                            :data="precheckFilteredTableData"
                            border
                            max-height="430"
                            row-key="phone"
                            @selection-change="handlePrecheckSelectionChange"
                          >
                            <el-table-column type="selection" width="48" reserve-selection />
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
                        </template>
                        <div v-else class="query-result-empty">
                          <el-empty :description="`暂无【${activePlatformName}】查询结果，请输入手机号查询`" />
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
                    <el-tag :type="orderStatusType(scope.row.orderStatus)" size="small">
                      {{ orderStatusLabel(scope.row.orderStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="center">
                  <template #default>
                    -
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
  listMarkUserPlatformPrice
} from '@/api/server/markUser'
import { useRoute, useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitLoading = ref(false)
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
const canDirectSubmit = computed(() => canSubmit.value && !insufficientRemain.value)
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
const precheckMarkedSelectedPhones = computed(() => {
  const markedSet = new Set(normalizePhoneList(precheckDialogData.value.markedPhones))
  return precheckSelectedPhones.value.filter((phone) => markedSet.has(phone))
})
const precheckMarkedSelectedCount = computed(() => precheckMarkedSelectedPhones.value.length)

const allVisiblePrecheckSelected = computed(() => {
  const visible = normalizePhoneList(precheckFilteredTableData.value.map((item) => item.phone))
  if (visible.length === 0) return false
  const selectedSet = new Set(precheckSelectedPhones.value)
  return visible.every((phone) => selectedSet.has(phone))
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
  precheckSourcePayload.value = null
  precheckKeyword.value = ''
  precheckQueryStatus.value = ''
  precheckMarkStatus.value = ''
  precheckDialogData.value = createEmptyPrecheckData()
  precheckSelectedRows.value = []
  precheckTableRef.value?.clearSelection?.()
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

    precheckSourcePayload.value = {
      ...payload,
      phones: [...payload.phones]
    }

    precheckKeyword.value = ''
    precheckQueryStatus.value = ''
    precheckMarkStatus.value = ''
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

function handlePrecheckSelectionChange(rows) {
  precheckSelectedRows.value = Array.isArray(rows) ? rows : []
}

function togglePrecheckSelectAll() {
  if (!hasPrecheckResult.value) return
  if (allVisiblePrecheckSelected.value) {
    precheckTableRef.value?.clearSelection?.()
    return
  }
  precheckTableRef.value?.toggleAllSelection?.()
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
  resetPrecheckPanel()
  submitForm.phonesText = ''
  submitForm.requestNo = ''
  submitForm.remark = ''
  await loadSummaryAndPrice()
  await getList()
  activeSubTab.value = 'submit'
}

async function submitSelectedMarkedPhones() {
  const selectedPhones = precheckMarkedSelectedPhones.value
  if (!selectedPhones.length) {
    proxy.$modal.msgWarning('请先勾选可提交的已标记号码')
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
    orderStatusLabel(item.orderStatus)
  ])
  downloadCsv(`mark-record-${Date.now()}.csv`, [header, ...body])
}

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
  gap: 16px;
  align-items: start;
}

.submit-left,
.submit-right {
  min-width: 0;
}

.submit-right {
  width: 100%;
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

.query-result-panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
  min-height: 560px;
  display: flex;
  flex-direction: column;
  color: var(--el-text-color-primary);
}

.query-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: #f8fbff;
}

.query-result-head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.query-result-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.query-result-platform {
  padding: 2px 10px;
  border-radius: 10px;
  border: 1px solid #b9d3ff;
  font-size: 13px;
  color: #2f76d2;
  line-height: 1.5;
  background: #ecf5ff;
}

.query-result-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.query-result-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 12px;
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
  border: 1px solid #284367;
  border-radius: 8px;
  background: linear-gradient(180deg, #122746 0%, #0f213d 100%);
  padding: 14px 16px;
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
  color: #ceddf6;
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
.record-search-panel :deep(.el-input__wrapper),
.record-search-panel :deep(.el-select__wrapper),
.record-search-panel :deep(.el-range-editor.el-input__wrapper) {
  background: #132949;
  box-shadow: 0 0 0 1px #35608d inset;
}

.record-search-panel :deep(.el-input__inner),
.record-search-panel :deep(.el-select__placeholder),
.record-search-panel :deep(.el-range-input),
.record-search-panel :deep(.el-range-separator),
.record-search-panel :deep(.el-date-editor .el-input__prefix) {
  color: #dce9ff;
}

.record-search-panel :deep(.el-button:not(.el-button--primary)) {
  background: rgba(255, 255, 255, 0.04);
  border-color: #355275;
  color: #dce9ff;
}

.record-search-panel :deep(.el-button--primary) {
  border-color: #36bbff;
  background: #36bbff;
  color: #fff;
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