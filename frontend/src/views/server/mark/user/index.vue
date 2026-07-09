<template>
  <div class="app-container mark-user-order-page">
    <el-card shadow="never" class="platform-card" :body-style="{ padding: '0' }">
      <el-empty v-if="platformOptions.length === 0" description="当前未配置可用平台" />
      <div v-else :class="['platform-layout', { 'platform-layout--single': !showPlatformSwitcher }]">
        <el-tabs
          v-if="showPlatformSwitcher"
          v-model="activePlatformCode"
          tab-position="left"
          class="platform-nav-tabs"
        >
          <el-tab-pane
            v-for="item in platformOptions"
            :key="item.platformCode"
            :label="item.platformName"
            :name="item.platformCode"
          />
        </el-tabs>

        <div class="platform-main">
          <div
            v-if="activeSubTab === 'submit' && platformOptions.length > 0"
            class="platform-main-remain"
          >
            <div class="submit-right-remain">
              当前剩余：<span>{{ activeRemainCount }}</span> 次
            </div>
          </div>
          <el-tabs v-model="activeSubTab" class="sub-tabs">
            <el-tab-pane :label="`${activePlatformName} - 提交号码`" name="submit">
              <div class="submit-pane">
                <div class="submit-content-shell">
                  <div class="submit-grid">
                    <div class="submit-left">
                      <div class="submit-input-panel">
                        <div class="submit-panel-head">号码录入</div>
                        <el-input
                          v-model="submitForm.phonesText"
                          type="textarea"
                          :rows="11"
                          resize="vertical"
                          class="submit-textarea"
                          placeholder="可粘贴混合文本，点击「号码提取」整理号码；确认后再点「一键批量查询」"
                        />
                        <div class="submit-stats">
                          <span>输入 <strong>{{ submitPhoneStats.inputCount }}</strong></span>
                          <span>有效 <strong>{{ submitPhoneStats.validCount }}</strong></span>
                          <span>重复 {{ submitPhoneStats.duplicateCount }}</span>
                          <span>无效 {{ submitPhoneStats.invalidCount }}</span>
                        </div>
                        <div v-if="!hasSubmitQuota" class="submit-warning-bar">
                          当前平台剩余次数不足，无法查询提交。
                        </div>
                        <div v-else-if="insufficientRemain" class="submit-warning-bar">
                          当前平台剩余次数不足，请减少号码后再提交。
                        </div>
                        <div class="submit-actions">
                          <div class="submit-action-row submit-action-row--primary">
                            <el-button
                              type="warning"
                              class="submit-action-btn submit-action-btn--extract"
                              :disabled="!String(submitForm.phonesText || '').trim()"
                              @click="extractSubmitPhones"
                            >
                              号码提取
                            </el-button>
                            <el-button
                              type="primary"
                              class="submit-action-btn"
                              :loading="precheckLoading"
                              :disabled="!canSubmit"
                              @click="submitBatchOrder"
                              v-hasPermi="['server:markUser:order:add']"
                            >
                              一键批量查询
                            </el-button>
                          </div>
                          <el-button
                            class="submit-action-btn submit-action-btn--ghost"
                            :disabled="!String(submitForm.phonesText || '').trim()"
                            @click="clearSubmitPhones"
                          >
                            清空
                          </el-button>
                        </div>
                      </div>
                    </div>

                    <div class="submit-right">
                      <div class="query-result-panel">
                        <div class="query-result-head">
                          <div class="query-result-title">查询结果</div>
                          <div class="query-result-actions">
                            <el-button
                              type="success"
                              size="small"
                              class="query-result-btn query-result-btn--primary"
                              :disabled="precheckSubmittableSelectedCount === 0 || clearSubmitLoading || precheckLoading || !hasSubmitQuota"
                              :loading="clearSubmitLoading"
                              @click="submitSelectedMarkedPhones"
                            >
                              提交消除
                            </el-button>
                            <el-button
                              type="warning"
                              plain
                              size="small"
                              class="query-result-btn"
                              :disabled="!hasPrecheckResult"
                              @click="togglePrecheckSelectAll"
                            >
                              全选
                            </el-button>
                            <el-button
                              size="small"
                              plain
                              class="query-result-btn"
                              :disabled="!hasPrecheckResult"
                              @click="resetPrecheckPanel"
                            >
                              清空结果
                            </el-button>
                            <el-button
                              size="small"
                              plain
                              class="query-result-btn"
                              :disabled="!hasPrecheckResult"
                              @click="copyPrecheckPhones"
                            >
                              复制
                            </el-button>
                            <el-button
                              size="small"
                              plain
                              class="query-result-btn"
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
                            stripe
                            size="small"
                            max-height="430"
                            row-key="phone"
                            @selection-change="handlePrecheckSelectionChange"
                          >
                            <el-table-column type="selection" width="48" reserve-selection :selectable="precheckRowSelectable" />
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
                <div class="record-search-bar">
                  <div class="record-search-field record-search-field--keyword">
                    <span class="record-search-field__label">综合搜索</span>
                    <el-input
                      v-model="queryParams.keyword"
                      size="small"
                      clearable
                      placeholder="订单号/手机号/用户名"
                      @keyup.enter="handleQuery"
                    />
                  </div>
                  <div class="record-search-field record-search-field--status">
                    <span class="record-search-field__label">处理状态</span>
                    <el-select
                      v-model="queryParams.orderStatus"
                      size="small"
                      clearable
                      placeholder="状态"
                    >
                      <el-option label="待处理" value="0" />
                      <el-option label="处理中" value="3" />
                      <el-option label="处理完成" value="1" />
                      <el-option label="处理失败" value="2" />
                    </el-select>
                  </div>
                  <div class="record-search-field record-search-field--date">
                    <span class="record-search-field__label">提交时间</span>
                    <el-date-picker
                      v-model="recordDateRange"
                      type="daterange"
                      size="small"
                      range-separator="-"
                      start-placeholder="开始"
                      end-placeholder="结束"
                      format="YYYY/MM/DD"
                      value-format="YYYY-MM-DD"
                      @change="handleRecordDateRangeChange"
                    />
                  </div>
                  <div class="record-search-field record-search-field--actions">
                    <div class="record-action-group">
                      <el-button size="small" @click="exportRecordRows">导出</el-button>
                      <el-button size="small" @click="resetQuery">重置</el-button>
                      <el-button type="primary" size="small" icon="Search" @click="handleQuery">搜索</el-button>
                    </div>
                  </div>
                </div>
              </div>

              <div class="record-table-wrap">
              <el-table
                v-loading="loading"
                :data="orderList"
                :row-key="recordRowKey"
                @selection-change="handleRecordSelectionChange"
              >
                <el-table-column type="selection" width="48" />
                <el-table-column label="用户名" prop="userName" min-width="110" show-overflow-tooltip />
                <el-table-column label="号码（点击复制）" min-width="150" show-overflow-tooltip>
                  <template #default="scope">
                    <el-button link type="primary" @click="copyText(scope.row.phonePreview || '')">
                      {{ scope.row.phonePreview || '-' }}
                    </el-button>
                  </template>
                </el-table-column>
                <el-table-column label="平台" prop="platformName" min-width="110" show-overflow-tooltip />
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
              </el-table>
              </div>

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
  listMarkUserPlatformPrice
} from '@/api/server/markUser'
import {
  filterLegacyMarkPlatforms,
  isDedicatedTencentPlatformCode,
  TENCENT_DEDICATED_ROUTE,
  TENCENT_DEDICATED_ROUTE_NAME
} from '@/utils/markTencentPlatform'
import {
  isDedicatedXiaomiPlatformCode,
  XIAOMI_DEDICATED_ROUTE,
  XIAOMI_DEDICATED_ROUTE_NAME
} from '@/utils/markXiaomiPlatform'
import {
  isDedicatedBaiduPlatformCode,
  BAIDU_DEDICATED_ROUTE,
  BAIDU_DEDICATED_ROUTE_NAME
} from '@/utils/markBaiduPlatform'
import {
  isDedicated360PlatformCode,
  resolve360DedicatedRouteName,
  resolve360DedicatedRoutePathByCode
} from '@/utils/markQihu360Platform'
import { markItemProcessStatusLabel, markItemProcessStatusTagType } from '@/utils/markProcessStatus'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import useMarkUserPageStore from '@/store/modules/markUserPage'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const markUserPageStore = useMarkUserPageStore()

const loading = ref(false)
const precheckLoading = ref(false)
const clearSubmitLoading = ref(false)
const total = ref(0)
const orderList = ref([])
const recordSelectedRows = ref([])
const platformOptions = ref([])
const activePlatformCode = ref('')
const activeSubTab = ref('submit')
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

const routePlatformCode = computed(() => resolvePlatformCodeFromRoute(route))
const showPlatformSwitcher = computed(() => !routePlatformCode.value)
const lastRestoredStateKey = ref('')

function parseMarkRouteQuery(query) {
  if (!query) return {}
  if (typeof query === 'string') {
    try {
      return JSON.parse(query)
    } catch (e) {
      return {}
    }
  }
  if (typeof query === 'object') {
    return query
  }
  return {}
}

function resolvePlatformCodeFromRoute(sourceRoute = route) {
  const parsed = parseMarkRouteQuery(sourceRoute?.query)
  return String(parsed.platformCode || sourceRoute?.query?.platformCode || '').trim()
}

function collectPhonesFromSegment(segment) {
  const trimmed = String(segment || '').trim()
  if (!trimmed) return []

  const digitsOnly = trimmed.replace(/[^\d]/g, '')
  if (digitsOnly.length >= 7 && digitsOnly.length <= 15) {
    return [digitsOnly]
  }

  const candidates = []
  const mobileMatches = trimmed.match(/1[3-9]\d{9}/g) || []
  candidates.push(...mobileMatches)

  const landlineMatches = trimmed.match(/0\d{2,3}[-\s.]?\d{7,8}/g) || []
  candidates.push(...landlineMatches)

  const specialMatches = trimmed.match(/[48]00[-\s.]?\d{7}/g) || []
  candidates.push(...specialMatches)

  const digitRuns = trimmed.match(/\d{7,15}/g) || []
  candidates.push(...digitRuns)

  return candidates
    .map((item) => String(item || '').replace(/[^\d]/g, ''))
    .filter((phone) => phone.length >= 7 && phone.length <= 15)
}

function dedupePhones(phones) {
  const uniquePhones = []
  const seen = new Set()
  ;(phones || []).forEach((phone) => {
    if (!phone || seen.has(phone)) return
    seen.add(phone)
    uniquePhones.push(phone)
  })
  return uniquePhones
}

function parsePhonesWithStats(text) {
  const source = String(text || '')
  const segments = source
    .split(/[\n,，;；]+/)
    .map((item) => item.trim())
    .filter((item) => item.length > 0)

  const allPhones = []
  let invalidCount = 0

  segments.forEach((segment) => {
    const phones = collectPhonesFromSegment(segment)
    if (phones.length === 0) {
      invalidCount += 1
      return
    }
    allPhones.push(...phones)
  })

  const uniqueValid = dedupePhones(allPhones)

  return {
    inputCount: segments.length,
    validCount: uniqueValid.length,
    duplicateCount: Math.max(0, allPhones.length - uniqueValid.length),
    invalidCount,
    validPhones: uniqueValid
  }
}

function extractPhoneNumbersFromText(text) {
  const source = String(text || '')
  if (!source.trim()) return []

  const segments = source
    .split(/[\n,，;；]+/)
    .map((item) => item.trim())
    .filter((item) => item.length > 0)

  const allPhones = segments.flatMap((segment) => collectPhonesFromSegment(segment))
  return dedupePhones(allPhones)
}

const submitPhoneStats = computed(() => parsePhonesWithStats(submitForm.phonesText))
const canSubmit = computed(() => !!activePlatform.value && submitPhoneStats.value.validCount > 0 && hasSubmitQuota.value)
const expectedSubmitCount = computed(() => submitPhoneStats.value.validCount)
const expectedDeductAmount = computed(() => expectedSubmitCount.value * activeUnitPrice.value)
const insufficientRemain = computed(() => expectedDeductAmount.value > activeRemainCount.value)
const hasSubmitQuota = computed(() => activeRemainCount.value >= activeUnitPrice.value)
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
const TEDDY_GAOPIN_PLATFORM_CODE = 'td_gaopin'
const TEDDY_GAOPIN_DISPLAY_LABEL = '\u6cf0\u8fea\u718a\u9ad8\u9891'
const TEDDY_GAOPIN_NO_LABEL = '\u65e0'
const TEDDY_GAOPIN_HF_KEY = '\u9ad8\u9891\u6807\u8bb0\u81f3\u5c11\u9700\u898110\u4e2a\u5de5\u4f5c\u65e5\u6216\u627e\u5e73\u53f0\u65b9\u5e2e\u5fd9\u5904\u7406'
const TEDDY_GAOPIN_FRAUD_KEY = '\u7591\u4f3c\u8bc8\u9a97'
const TEDDY_GAOPIN_HF_SHORT_KEY = '\u9ad8\u9891'
const isTeddyGaopinActive = computed(() => String(activePlatformCode.value || '').trim() === TEDDY_GAOPIN_PLATFORM_CODE)
const MOBILE_GAOPIN_PLATFORM_CODES = ['mobile_gaopin', 'yidonggaopin']
const MOBILE_GAOPIN_MARKED_LABEL = '\u6709\u6807\u8bb0'
const MOBILE_GAOPIN_NO_LABEL = '\u65e0'
const isMobileGaopinActive = computed(() => MOBILE_GAOPIN_PLATFORM_CODES.includes(String(activePlatformCode.value || '').trim()))

function resolvePrecheckRawDetail(row) {
  const status = String(row?.status || '').trim()
  if (status.toLowerCase().startsWith('yes-')) {
    return status.slice(4).trim()
  }
  if (status.toLowerCase().startsWith('no-')) {
    return status.slice(3).trim()
  }
  const detail = stripPlatformPrefix(row?.detail || '')
  if (detail && detail !== TEDDY_GAOPIN_DISPLAY_LABEL) {
    return detail
  }
  return detail || status
}

function isTeddyGaopinNoResult(row) {
  const status = String(row?.status || '').trim().toLowerCase()
  if (status === 'no' || status.startsWith('no-')) {
    return true
  }
  const detail = stripPlatformPrefix(row?.detail || '').trim()
  return detail === TEDDY_GAOPIN_NO_LABEL
    || detail === '\u672a\u6807\u8bb0'
    || detail === '\u65e0\u6807\u8bb0'
}

function isTeddyGaopinSubmittableRaw(raw) {
  const value = String(raw || '').trim()
  if (value.includes(TEDDY_GAOPIN_HF_KEY)) return true
  return value.includes(TEDDY_GAOPIN_FRAUD_KEY) && value.includes(TEDDY_GAOPIN_HF_SHORT_KEY)
}

function isTeddyGaopinSubmittableRow(row) {
  if (row?.querySuccess !== true) return false
  if (!isTeddyGaopinActive.value) return true
  return isTeddyGaopinSubmittableRaw(resolvePrecheckRawDetail(row))
}

function isMobileGaopinNoResult(row) {
  const status = String(row?.status || '').trim().toLowerCase()
  if (status === 'no' || status.startsWith('no-')) {
    return true
  }
  const detail = stripPlatformPrefix(row?.detail || '').trim()
  return detail === MOBILE_GAOPIN_NO_LABEL
    || detail === '\u672a\u6807\u8bb0'
    || detail === '\u65e0\u6807\u8bb0'
}

function isMobileGaopinSubmittableRow(row) {
  if (row?.querySuccess !== true) return false
  if (!isMobileGaopinActive.value) return true
  if (isMobileGaopinNoResult(row)) return false
  if (row?.marked === true) return true
  const detail = stripPlatformPrefix(row?.detail || '').trim()
  return detail === MOBILE_GAOPIN_MARKED_LABEL || detail === '\u9ad8\u9891\u62e6\u622a'
}

function isPrecheckRowSubmittable(row) {
  if (row?.querySuccess !== true) return false
  if (isTeddyGaopinActive.value) {
    return isTeddyGaopinSubmittableRow(row)
  }
  if (isMobileGaopinActive.value) {
    return isMobileGaopinSubmittableRow(row)
  }
  return true
}

function precheckRowSelectable(row) {
  if (isTeddyGaopinActive.value) {
    return isTeddyGaopinSubmittableRow(row)
  }
  if (isMobileGaopinActive.value) {
    return isMobileGaopinSubmittableRow(row)
  }
  return row?.querySuccess === true
}

const precheckMarkedVisibleRows = computed(() => {
  return precheckFilteredTableData.value.filter((row) => {
    if (isTeddyGaopinActive.value) {
      return isTeddyGaopinSubmittableRow(row)
    }
    if (isMobileGaopinActive.value) {
      return isMobileGaopinSubmittableRow(row)
    }
    return row?.querySuccess === true && row?.marked === true
  })
})
const precheckSubmittableSelectedPhones = computed(() => {
  return normalizePhoneList(
    precheckSelectedRows.value
      .filter((row) => isPrecheckRowSubmittable(row))
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

function normalizeMarkDetailText(text) {
  const value = String(text || '').trim()
  if (!value) return value
  const prefix = '\u666e\u901a\u6807\u8bb0'
  if (value === prefix || value.startsWith(`${prefix}-`) || value.startsWith(`${prefix}\u2014`)) {
    return '\u6709\u6807\u8bb0'
  }
  return value
}

function precheckResultLabel(row) {
  if (row?.querySuccess === false) return row?.errorMessage || row?.detail || '查询失败'
  if (isTeddyGaopinActive.value) {
    if (isTeddyGaopinNoResult(row)) {
      return TEDDY_GAOPIN_NO_LABEL
    }
    if (isTeddyGaopinSubmittableRaw(resolvePrecheckRawDetail(row))) {
      return TEDDY_GAOPIN_DISPLAY_LABEL
    }
  }
  if (isMobileGaopinActive.value) {
    if (isMobileGaopinNoResult(row)) {
      return MOBILE_GAOPIN_NO_LABEL
    }
    if (isMobileGaopinSubmittableRow(row)) {
      return MOBILE_GAOPIN_MARKED_LABEL
    }
  }
  if (row?.detail) return normalizeMarkDetailText(stripPlatformPrefix(row.detail))
  const statusText = markStatusLabel(row)
  return statusText === '-' ? '查询成功' : statusText
}

function stripPlatformPrefix(text) {
  const value = String(text || '').trim()
  if (!value) return value
  const matched = value.match(/^[\w.-]+[：:]\s*(.+)$/)
  return matched ? matched[1].trim() : value
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
    precheckSelectedRows.value = []
    precheckTableRef.value?.clearSelection?.()

    await nextTick()
    autoSelectMarkedPrecheckRows()

    if (!silentWarning) {
      notifyPrecheckSummary()
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
  const nextRows = Array.isArray(rows) ? rows : []
  if (isTeddyGaopinActive.value) {
    precheckSelectedRows.value = nextRows.filter((row) => isTeddyGaopinSubmittableRow(row))
    return
  }
  if (isMobileGaopinActive.value) {
    precheckSelectedRows.value = nextRows.filter((row) => isMobileGaopinSubmittableRow(row))
    return
  }
  precheckSelectedRows.value = nextRows
}

function notifyPrecheckSummary() {
  const total = precheckTableData.value.length
  const failedCount = precheckDialogData.value.failedCount || 0
  if (isTeddyGaopinActive.value) {
    const submittableCount = precheckTableData.value.filter((row) => isTeddyGaopinSubmittableRow(row)).length
    if (submittableCount === 0) {
      proxy.$modal.msgWarning(`查询完成，共 ${total} 个号码，可提交 0 个（仅「泰迪熊高频」结果可提交并扣次）`)
      return
    }
    if (submittableCount < total) {
      proxy.$modal.msgWarning(`查询完成，共 ${total} 个号码，仅 ${submittableCount} 个为「泰迪熊高频」可提交扣次，已自动勾选`)
      return
    }
    proxy.$modal.msgSuccess(`查询完成，${submittableCount} 个号码可提交，已自动勾选`)
    return
  }
  if (isMobileGaopinActive.value) {
    const submittableCount = precheckTableData.value.filter((row) => isMobileGaopinSubmittableRow(row)).length
    if (submittableCount === 0) {
      proxy.$modal.msgWarning(`查询完成，共 ${total} 个号码，可提交 0 个（仅「有标记」结果可提交并扣次）`)
      return
    }
    if (submittableCount < total) {
      proxy.$modal.msgWarning(`查询完成，共 ${total} 个号码，仅 ${submittableCount} 个为「有标记」可提交扣次，已自动勾选`)
      return
    }
    proxy.$modal.msgSuccess(`查询完成，${submittableCount} 个号码可提交，已自动勾选`)
    return
  }
  const markedCount = precheckDialogData.value.markedCount || 0
  if (markedCount === 0) {
    if (failedCount > 0) {
      proxy.$modal.msgWarning(`预查询完成，失败 ${failedCount} 个，可提交 0 个`)
    } else {
      proxy.$modal.msgWarning('预查询未发现被标记号码，请确认后再决定是否重试')
    }
  }
}

function autoSelectMarkedPrecheckRows() {
  if (!isTeddyGaopinActive.value && !isMobileGaopinActive.value) return
  precheckTableRef.value?.clearSelection?.()
  const markedRows = precheckTableData.value.filter((row) => isPrecheckRowSubmittable(row))
  markedRows.forEach((row) => {
    precheckTableRef.value?.toggleRowSelection?.(row, true)
  })
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
    precheckResultLabel(item),
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
  const auditStatus = String(row?.auditStatus ?? '1')
  if (auditStatus === '0') return '待审核'
  if (auditStatus === '2') return '已拒绝'
  if (auditStatus === '3') return '已打回'
  const itemStatus = String(row?.itemProcessStatus ?? '')
  if (itemStatus === '0' || itemStatus === '1' || itemStatus === '2' || itemStatus === '3') {
    return markItemProcessStatusLabel(itemStatus, row)
  }
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
  const itemStatus = String(row?.itemProcessStatus ?? '')
  if (itemStatus === '0' || itemStatus === '1' || itemStatus === '2' || itemStatus === '3') {
    return markItemProcessStatusTagType(itemStatus, row)
  }
  const label = recordStatusLabel(row)
  if (label === '处理完成' || label === '成功') return 'success'
  if (label === '处理失败' || label === '失败' || label === '部分失败') return 'danger'
  return 'warning'
}

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function recordRowKey(row) {
  return `${row?.itemId ?? row?.id ?? ''}-${row?.phonePreview ?? ''}`
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
    const allList = filterLegacyMarkPlatforms(Array.isArray(resp?.data) ? resp.data : [])
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

function resolveStateKey(platformCode) {
  const code = String(
    platformCode
    || resolvePlatformCodeFromRoute(route)
    || activePlatformCode.value
    || ''
  ).trim()
  return code || '__default__'
}

function savePageState(platformCode) {
  const key = String(platformCode || '').trim() || resolveStateKey()
  markUserPageStore.saveSnapshot(key, collectPageSnapshot())
}

function refreshPlatformPageState(platformCode, { force = false } = {}) {
  const key = resolveStateKey(platformCode)
  if (!force && key === lastRestoredStateKey.value) {
    syncRecordTabData()
    return
  }
  restorePageState(key)
  lastRestoredStateKey.value = key
  syncRecordTabData()
}

function switchPlatformPageState(previousCode, nextCode) {
  if (previousCode) {
    savePageState(previousCode)
  }
  refreshPlatformPageState(nextCode, { force: true })
}

function collectPageSnapshot() {
  return {
    submitForm: {
      phonesText: submitForm.phonesText,
      requestNo: submitForm.requestNo,
      remark: submitForm.remark
    },
    precheckDialogData: JSON.parse(JSON.stringify(precheckDialogData.value)),
    precheckSourcePayload: precheckSourcePayload.value
      ? {
          ...precheckSourcePayload.value,
          phones: Array.isArray(precheckSourcePayload.value.phones)
            ? [...precheckSourcePayload.value.phones]
            : []
        }
      : null,
    precheckKeyword: precheckKeyword.value,
    precheckQueryStatus: precheckQueryStatus.value,
    precheckMarkStatus: precheckMarkStatus.value,
    precheckSelectedPhones: normalizePhoneList(precheckSelectedRows.value.map((row) => row.phone)),
    activeSubTab: activeSubTab.value,
    queryParams: {
      ...queryParams,
      params: { ...(queryParams.params || {}) }
    },
    recordDateRange: Array.isArray(recordDateRange.value) ? [...recordDateRange.value] : []
  }
}

function applyDefaultRecordQuery() {
  queryParams.pageNum = 1
  queryParams.orderNo = null
  queryParams.requestNo = null
  queryParams.keyword = null
  queryParams.phone = null
  queryParams.orderStatus = null
  queryParams.params = {}
  recordDateRange.value = []
  recordSelectedRows.value = []
}

function applyDefaultPageState(platformCode) {
  submitForm.phonesText = ''
  submitForm.requestNo = ''
  submitForm.remark = ''
  resetPrecheckPanel()
  activeSubTab.value = 'submit'
  applyDefaultRecordQuery()
  queryParams.platformCode = platformCode || activePlatformCode.value || routePlatformCode.value || null
}

function restorePrecheckSelection(phones) {
  const selectedPhones = normalizePhoneList(Array.isArray(phones) ? phones : [])
  precheckSelectedRows.value = []
  precheckTableRef.value?.clearSelection?.()
  if (!selectedPhones.length) return
  const phoneSet = new Set(selectedPhones)
  precheckTableData.value
    .filter((row) => phoneSet.has(row.phone) && isPrecheckRowSubmittable(row))
    .forEach((row) => {
      precheckTableRef.value?.toggleRowSelection?.(row, true)
    })
}

function restorePageState(platformCode) {
  const key = resolveStateKey(platformCode)
  const snapshot = markUserPageStore.getSnapshot(key)
  if (!snapshot) {
    applyDefaultPageState(platformCode)
    return
  }

  const snapshotPlatform = String(
    snapshot.precheckDialogData?.platformCode
    || snapshot.precheckSourcePayload?.platformCode
    || snapshot.queryParams?.platformCode
    || ''
  ).trim()
  if (snapshotPlatform && snapshotPlatform !== key && key !== '__default__') {
    markUserPageStore.clearSnapshot(key)
    applyDefaultPageState(platformCode)
    return
  }

  submitForm.phonesText = snapshot.submitForm?.phonesText || ''
  submitForm.requestNo = snapshot.submitForm?.requestNo || ''
  submitForm.remark = snapshot.submitForm?.remark || ''
  precheckDialogData.value = snapshot.precheckDialogData || createEmptyPrecheckData()
  precheckSourcePayload.value = snapshot.precheckSourcePayload || null
  precheckKeyword.value = snapshot.precheckKeyword || ''
  precheckQueryStatus.value = snapshot.precheckQueryStatus || ''
  precheckMarkStatus.value = snapshot.precheckMarkStatus || ''
  activeSubTab.value = snapshot.activeSubTab || 'submit'
  recordDateRange.value = Array.isArray(snapshot.recordDateRange) ? [...snapshot.recordDateRange] : []
  recordSelectedRows.value = []

  Object.assign(queryParams, {
    pageNum: snapshot.queryParams?.pageNum ?? 1,
    pageSize: snapshot.queryParams?.pageSize ?? 10,
    orderNo: snapshot.queryParams?.orderNo ?? null,
    requestNo: snapshot.queryParams?.requestNo ?? null,
    keyword: snapshot.queryParams?.keyword ?? null,
    phone: snapshot.queryParams?.phone ?? null,
    platformCode: platformCode || activePlatformCode.value || routePlatformCode.value || null,
    orderStatus: snapshot.queryParams?.orderStatus ?? null,
    params: { ...(snapshot.queryParams?.params || {}) }
  })

  nextTick(() => {
    restorePrecheckSelection(snapshot.precheckSelectedPhones)
  })
}

function syncRecordTabData() {
  if (activeSubTab.value === 'record') {
    getList()
    startRecordPolling()
  } else {
    stopRecordPolling()
  }
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
  queryParams.platformCode = activePlatformCode.value || routePlatformCode.value || null
  handleQuery()
}

function handleRecordDateRangeChange(value) {
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

function extractSubmitPhones() {
  const raw = String(submitForm.phonesText || '').trim()
  if (!raw) {
    proxy.$modal.msgWarning('请先粘贴或输入号码内容')
    return
  }
  const phones = extractPhoneNumbersFromText(raw)
  if (!phones.length) {
    proxy.$modal.msgWarning('未提取到有效号码（7-15位数字）')
    return
  }
  submitForm.phonesText = phones.join('\n')
  proxy.$modal.msgSuccess(`已整理 ${phones.length} 个号码，请确认后点击「一键批量查询」`)
}

async function submitBatchOrder() {
  if (!activePlatform.value) {
    proxy.$modal.msgError('当前未选择平台')
    return
  }
  if (!hasSubmitQuota.value) {
    proxy.$modal.msgError('当前平台剩余次数不足，无法查询提交')
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

async function afterCreateOrderSuccess(res, submittedCount = 0) {
  await loadSummaryAndPrice()
  const count = Number(submittedCount || res?.data?.order?.totalCount || res?.data?.totalCount || 0)
  const remain = activeRemainCount.value
  if (count > 0) {
    proxy.$modal.msgSuccess(`提交成功，已提交 ${count} 个号码，扣除 ${count} 次，当前剩余 ${remain} 次`)
  } else {
    proxy.$modal.msgSuccess(res?.msg || '下单成功')
  }
  resetPrecheckPanel()
  submitForm.phonesText = ''
  submitForm.requestNo = ''
  submitForm.remark = ''
  await getList()
  activeSubTab.value = 'submit'
}

async function submitSelectedMarkedPhones() {
  const selectedPhones = precheckSubmittableSelectedPhones.value
  if (!selectedPhones.length) {
    const tip = isTeddyGaopinActive.value
      ? '仅支持提交泰迪熊高频结果（高频标记/疑似诈骗高频标记），其他结果不可提交'
      : isMobileGaopinActive.value
        ? '仅支持提交「有标记」结果，显示「无」的号码不可提交'
        : '请先勾选可提交号码（仅支持查询成功记录）'
    proxy.$modal.msgWarning(tip)
    return
  }
  if (!precheckSourcePayload.value) {
    proxy.$modal.msgWarning('暂无可提交结果，请先执行预查询')
    return
  }
  if (!hasSubmitQuota.value) {
    proxy.$modal.msgError('当前平台剩余次数不足，无法提交')
    return
  }
  const finalDeductAmount = selectedPhones.length * activeUnitPrice.value
  if (finalDeductAmount > activeRemainCount.value) {
    proxy.$modal.msgError('当前平台剩余次数不足，请减少提交数量')
    return
  }

  const payload = {
    ...precheckSourcePayload.value,
    phones: selectedPhones,
    requestNo: ''
  }
  clearSubmitLoading.value = true
  try {
    const res = await createMarkUserClearOrder(payload)
    await afterCreateOrderSuccess(res, selectedPhones.length)
  } catch (error) {
    console.error('提交订单失败:', error)
    proxy.$modal.msgError(error?.message || '提交失败')
  } finally {
    clearSubmitLoading.value = false
  }
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
  const header = ['用户名', '手机号', '平台', '处理状态', '订单号', '提交时间']
  const body = rows.map((item) => [
    item.userName || '',
    item.phonePreview || '',
    item.platformName || '',
    recordStatusLabel(item),
    item.orderNo || '',
    formatDateTime(item.createTime)
  ])
  downloadCsv(`mark-record-${Date.now()}.csv`, [header, ...body])
}

watch(routePlatformCode, async (newCode, oldCode) => {
  if (newCode === oldCode) return
  await loadSummaryAndPrice()
  switchPlatformPageState(oldCode, newCode)
})

watch(activePlatformCode, (newCode, oldCode) => {
  if (!showPlatformSwitcher.value || !oldCode || oldCode === newCode) {
    return
  }
  switchPlatformPageState(oldCode, newCode)
})

let recordPollTimer = null
function startRecordPolling() {
  stopRecordPolling()
  recordPollTimer = window.setInterval(() => {
    if (activeSubTab.value !== 'record') return
    getList()
  }, 5000)
}
function stopRecordPolling() {
  if (recordPollTimer) {
    window.clearInterval(recordPollTimer)
    recordPollTimer = null
  }
}

watch(routePlatformCode, (code) => {
  if (isDedicatedTencentPlatformCode(code)) {
    router.replace({ name: TENCENT_DEDICATED_ROUTE_NAME }).catch(() => {
      router.replace({ path: TENCENT_DEDICATED_ROUTE }).catch(() => {})
    })
    return
  }
  if (isDedicatedXiaomiPlatformCode(code)) {
    router.replace({ name: XIAOMI_DEDICATED_ROUTE_NAME }).catch(() => {
      router.replace({ path: XIAOMI_DEDICATED_ROUTE }).catch(() => {})
    })
    return
  }
  if (isDedicatedBaiduPlatformCode(code)) {
    router.replace({ name: BAIDU_DEDICATED_ROUTE_NAME }).catch(() => {
      router.replace({ path: BAIDU_DEDICATED_ROUTE }).catch(() => {})
    })
    return
  }
  if (isDedicated360PlatformCode(code)) {
    router.replace({ name: resolve360DedicatedRouteName(code) }).catch(() => {
      router.replace({ path: resolve360DedicatedRoutePathByCode(code) }).catch(() => {})
    })
  }
})

watch(activeSubTab, (tab) => {
  if (tab === 'record') {
    getList()
    startRecordPolling()
  } else {
    stopRecordPolling()
  }
})

onMounted(async () => {
  if (isDedicatedTencentPlatformCode(routePlatformCode.value)) {
    const redirected = await router.replace({ name: TENCENT_DEDICATED_ROUTE_NAME }).catch(() => null)
    if (!redirected) {
      await router.replace({ path: TENCENT_DEDICATED_ROUTE }).catch(() => {})
    }
    return
  }
  if (isDedicatedXiaomiPlatformCode(routePlatformCode.value)) {
    const redirected = await router.replace({ name: XIAOMI_DEDICATED_ROUTE_NAME }).catch(() => null)
    if (!redirected) {
      await router.replace({ path: XIAOMI_DEDICATED_ROUTE }).catch(() => {})
    }
    return
  }
  if (isDedicatedBaiduPlatformCode(routePlatformCode.value)) {
    const redirected = await router.replace({ name: BAIDU_DEDICATED_ROUTE_NAME }).catch(() => null)
    if (!redirected) {
      await router.replace({ path: BAIDU_DEDICATED_ROUTE }).catch(() => {})
    }
    return
  }
  if (isDedicated360PlatformCode(routePlatformCode.value)) {
    const code = routePlatformCode.value
    const redirected = await router.replace({ name: resolve360DedicatedRouteName(code) }).catch(() => null)
    if (!redirected) {
      await router.replace({ path: resolve360DedicatedRoutePathByCode(code) }).catch(() => {})
    }
    return
  }
  await loadSummaryAndPrice()
  refreshPlatformPageState(resolveStateKey(), { force: true })
})

onBeforeRouteLeave((_to, from) => {
  savePageState(resolvePlatformCodeFromRoute(from) || activePlatformCode.value)
})

onBeforeUnmount(() => {
  stopRecordPolling()
})
</script>

<style scoped>
.mark-user-order-page {
  padding: 0 !important;
  margin: 0;
  width: 100%;
}

.mark-user-order-page :deep(.platform-card) {
  border: none;
  border-radius: 0;
  box-shadow: none;
}

.mark-user-order-page :deep(.platform-card > .el-card__body) {
  padding: 0 !important;
}

.mark-user-order-page :deep(.pagination-container) {
  margin-top: 12px;
}

.platform-layout {
  display: flex;
  align-items: flex-start;
  gap: 8px;
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

.platform-main-remain {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  height: 40px;
  padding-right: 4px;
}

.sub-tabs {
  margin-top: 0;
}

.sub-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0;
}

.sub-tabs :deep(.el-tabs__nav-wrap) {
  padding: 0;
}

.sub-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.sub-tabs :deep(.el-tabs__content) {
  padding: 0;
}

.sub-tabs :deep(.el-tabs__item) {
  height: 40px;
  line-height: 40px;
  padding: 0 16px;
}

.platform-main:has(.platform-main-remain) .sub-tabs :deep(.el-tabs__header) {
  padding-right: 150px;
}

.submit-pane {
  padding: 14px 16px 18px;
  background: #f5f7fa;
}

.submit-content-shell {
  width: 100%;
  max-width: 1280px;
}

.submit-grid {
  display: grid;
  grid-template-columns: minmax(300px, 36%) minmax(0, 1fr);
  width: 100%;
  min-height: 520px;
  gap: 14px;
  align-items: stretch;
}

.submit-left,
.submit-right {
  min-width: 0;
}

.submit-right {
  display: flex;
}

.submit-input-panel,
.query-result-panel {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: #fff;
  box-sizing: border-box;
}

.submit-input-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 12px 14px 14px;
}

.submit-panel-head,
.query-result-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
}

.submit-textarea {
  width: 100%;

  :deep(.el-textarea__inner) {
    min-height: 220px;
    padding: 10px 12px;
    font-size: 13px;
    line-height: 1.55;
    box-sizing: border-box;
  }
}

.submit-right-remain {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
  background: #ecf5ff;
  color: #606266;
  font-size: 13px;
  line-height: 1;
}

.submit-right-remain span {
  color: #409eff;
  font-weight: 600;
}

.submit-warning-bar {
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 13px;
  line-height: 1.5;
}

.submit-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.submit-stats strong {
  color: var(--el-color-primary);
  font-weight: 600;
}

.submit-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.submit-action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.submit-action-btn {
  min-width: 108px;
  height: 32px;
  margin: 0;
  font-size: 13px;
}

.submit-action-btn--extract {
  --el-button-bg-color: #d48806;
  --el-button-border-color: #d48806;
  --el-button-hover-bg-color: #b8740a;
  --el-button-hover-border-color: #b8740a;
  --el-button-active-bg-color: #9c6509;
  --el-button-active-border-color: #9c6509;
  --el-button-disabled-bg-color: #e8c98a;
  --el-button-disabled-border-color: #e8c98a;
  color: #fff;
}

.submit-action-btn--ghost {
  align-self: flex-start;
}

.query-result-panel {
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
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: #fafbfc;
}

.query-result-title {
  margin-bottom: 0;
  flex-shrink: 0;
}

.query-result-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.query-result-btn {
  min-height: 28px;
  padding: 0 10px;
  margin: 0;
  font-size: 12px;
}

.query-result-btn--primary {
  min-width: 84px;
}

.query-result-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 10px 12px 12px;
}

.query-result-table :deep(.el-table__header th) {
  background: #eef3f8;
  color: #303133;
  font-weight: 600;
  font-size: 13px;
}

.query-result-table :deep(.el-table__header .cell) {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.query-result-table :deep(.el-table__body .cell) {
  font-size: 13px;
}

.query-result-table :deep(.el-table__empty-text) {
  font-size: 13px;
  color: var(--el-text-color-secondary);
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
  margin-top: 0;
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 0;
  border-top: none;
  background: #fff;
  padding: 8px 10px;
  overflow-x: auto;
  overflow-y: visible;
}

.record-search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  width: max-content;
  min-width: 100%;
}

.record-search-field {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
}

.record-search-field--keyword {
  flex: 0 1 200px;
  width: 200px;
  min-width: 140px;
}

.record-search-field--keyword :deep(.el-input) {
  width: 100%;
}

.record-search-field--status :deep(.el-select) {
  width: 96px;
}

.record-search-field--date :deep(.el-date-editor) {
  width: 210px !important;
  flex-shrink: 0;
}

.record-search-field--date :deep(.el-range-input) {
  font-size: 12px;
}

.record-search-field--actions {
  margin-left: auto;
  flex-shrink: 0;
}

.record-search-field__label {
  flex-shrink: 0;
  white-space: nowrap;
  font-size: 12px;
  color: var(--el-text-color-regular);
  line-height: 1;
}

.record-action-group {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
}

.record-action-group :deep(.el-button + .el-button) {
  margin-left: 0;
}

.record-table-wrap {
  width: 100%;
  overflow-x: auto;
}

.record-table-wrap :deep(.el-table) {
  min-width: 860px;
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

  .submit-pane {
    padding: 12px;
  }

  .submit-grid {
    grid-template-columns: 1fr;
  }

  .query-result-panel {
    min-height: 420px;
  }

  .query-result-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .query-result-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .query-result-actions :deep(.el-button) {
    flex: 1 1 calc(50% - 8px);
  }

  .precheck-filter-bar :deep(.el-select),
  .precheck-filter-bar :deep(.el-input) {
    width: 100% !important;
  }
}
</style>
