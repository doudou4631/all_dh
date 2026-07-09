# -*- coding: utf-8 -*-
"""Generate user Xiaomi page with submit + record tabs (UTF-8 Chinese)."""
from pathlib import Path


def zh(*codes: int) -> str:
    return "".join(chr(c) for c in codes)


T = {
    "tab_submit": zh(0x5C0F, 0x7C73, 0x20, 0x2D, 0x20, 0x63D0, 0x4EA4, 0x53F7, 0x7801),
    "tab_record": zh(0x5C0F, 0x7C73, 0x20, 0x2D, 0x20, 0x4EFB, 0x52A1, 0x8BB0, 0x5F55),
    "remain_label": zh(0x5F53, 0x524D, 0x5269, 0x4F59, 0xFF1A),
    "remain_suffix": zh(0x6B21),
    "phone_label": zh(0x624B, 0x673A, 0x2F, 0x56FA, 0x8BDD, 0x53F7, 0x28, 0x6BCF, 0x884C, 0x31, 0x4E2A, 0x29),
    "submit_btn": zh(0x6279, 0x91CF, 0x6E05, 0x5C0F, 0x7C73),
    "reset_btn": zh(0x91CD, 0x7F6E),
    "result_title": zh(0x63D0, 0x4EA4, 0x7ED3, 0x679C),
    "col_item": zh(0x9879, 0x76EE),
    "col_remark": zh(
        0x4FE1, 0x606F, 0x5907, 0x6CE8, 0x28, 0x6279, 0x6B21, 0x7F16, 0x53F7, 0x7528, 0x4F5C, 0x540E, 0x7EED, 0x67E5, 0x8BE2, 0x29
    ),
    "batch_no": zh(0x6279, 0x91CF, 0x7F16, 0x53F7),
    "phone_count": zh(0x53F7, 0x7801, 0x6570, 0x91CF),
    "submit_time": zh(0x63D0, 0x4EA4, 0x65F6, 0x95F4),
    "count_tpl": zh(0x6B63, 0x5E38, 0x63D0, 0x4EA4, 0x4E86),
    "count_suffix": zh(0x4E2A, 0x624B, 0x673A, 0x53F7, 0x7801),
    "empty_result": zh(0x6682, 0x65E0, 0x63D0, 0x4EA4, 0x7ED3, 0x679C),
    "platform_name": zh(0x5C0F, 0x7C73, 0x624B, 0x673A),
    "search_label": zh(0x7EFC, 0x5408, 0x641C, 0x7D22),
    "search_ph": zh(0x8BA2, 0x5355, 0x53F7, 0x2F, 0x624B, 0x673A, 0x53F7, 0x2F, 0x7528, 0x6237, 0x540D),
    "status_label": zh(0x5904, 0x7406, 0x72B6, 0x6001),
    "status_ph": zh(0x72B6, 0x6001),
    "pending": zh(0x5F85, 0x5904, 0x7406),
    "processing": zh(0x5904, 0x7406, 0x4E2D),
    "done": zh(0x5DF2, 0x5B8C, 0x6210),
    "cancelled": zh(0x5DF2, 0x53D6, 0x6D88),
    "submit_time_label": zh(0x63D0, 0x4EA4, 0x65F6, 0x95F4),
    "start": zh(0x5F00, 0x59CB),
    "end": zh(0x7ED3, 0x675F),
    "search_btn": zh(0x641C, 0x7D22),
    "reset_query": zh(0x91CD, 0x7F6E),
    "export_btn": zh(0x5BFC, 0x51FA),
    "user_name": zh(0x7528, 0x6237, 0x540D),
    "phone_copy": zh(0x53F7, 0x7801, 0xFF08, 0x70B9, 0x51FB, 0x590D, 0x5236, 0xFF09),
    "platform": zh(0x5E73, 0x53F0),
    "order_no": zh(0x8BA2, 0x5355, 0x53F7),
    "audit_pending": zh(0x5F85, 0x5BA1, 0x6838),
    "rejected": zh(0x5DF2, 0x62D2, 0x7EDD),
    "returned": zh(0x5DF2, 0x6253, 0x56DE),
    "success": zh(0x6210, 0x529F),
    "fail": zh(0x5931, 0x8D25),
    "partial_fail": zh(0x90E8, 0x5206, 0x5931, 0x8D25),
    "no_copy": zh(0x6CA1, 0x6709, 0x53EF, 0x590D, 0x5236, 0x5185, 0x5BB9),
    "copied": zh(0x590D, 0x5236, 0x6210, 0x529F),
    "copy_fail": zh(0x590D, 0x5236, 0x5931, 0x8D25, 0xFF0C, 0x8BF7, 0x68C0, 0x67E5, 0x6D4F, 0x89C8, 0x5668, 0x6743, 0x9650),
    "no_export": zh(0x6682, 0x65E0, 0x53EF, 0x5BFC, 0x51FA, 0x8BB0, 0x5F55),
    "msg_no_valid_phones": zh(
        0x8BF7, 0x8F93, 0x5165, 0x6709, 0x6548, 0x53F7, 0x7801, 0xFF08, 0x6BCF, 0x884C, 0x31, 0x4E2A, 0xFF0C,
        0x37, 0x2D, 0x31, 0x35, 0x4F4D, 0x6570, 0x5B57, 0xFF09
    ),
    "msg_remain_insufficient": zh(
        0x5F53, 0x524D, 0x5E73, 0x53F0, 0x5269, 0x4F59, 0x6B21, 0x6570, 0x4E0D, 0x8DB3, 0xFF0C, 0x8BF7, 0x51CF,
        0x5C11, 0x53F7, 0x7801, 0x540E, 0x518D, 0x63D0, 0x4EA4
    ),
    "msg_submit_failed": zh(0x63D0, 0x4EA4, 0x5931, 0x8D25, 0xFF0C, 0x8BF7, 0x7A0D, 0x540E, 0x91CD, 0x8BD5),
}

content = f"""<template>
  <div class="app-container mark-user-xiaomi-page">
    <el-card shadow="never" class="xiaomi-page-card" :body-style="{{ padding: '0' }}">
      <div class="platform-main">
        <div v-if="activeTab === 'submit'" class="platform-main-remain">
          <div class="submit-right-remain">
            {T['remain_label']}<span>{{{{ remainCount }}}}</span> {T['remain_suffix']}
          </div>
        </div>
        <el-tabs v-model="activeTab" class="sub-tabs">
          <el-tab-pane label="{T['tab_submit']}" name="submit">
            <div class="submit-pane">
              <div class="xiaomi-submit-panel">
                <div class="xiaomi-submit-row">
                  <label class="xiaomi-submit-label">{T['phone_label']}</label>
                  <el-input
                    v-model="phonesText"
                    type="textarea"
                    :rows="10"
                    resize="vertical"
                    class="xiaomi-submit-textarea"
                    placeholder=""
                  />
                </div>
                <div class="xiaomi-submit-actions">
                  <el-button
                    type="primary"
                    class="xiaomi-action-btn"
                    :loading="submitting"
                    v-hasPermi="['server:markUser:order:clear']"
                    @click="handleSubmit"
                  >
                    {T['submit_btn']}
                  </el-button>
                  <el-button class="xiaomi-action-btn" :disabled="submitting" @click="handleReset">{T['reset_btn']}</el-button>
                </div>
              </div>

              <div class="xiaomi-result-block">
                <div class="xiaomi-result-head">{T['result_title']}</div>
                <el-table :data="resultRows" border stripe class="xiaomi-result-table" style="width: 100%;">
                  <el-table-column prop="label" label="{T['col_item']}" width="140" align="center" />
                  <el-table-column prop="value" label="{T['col_remark']}" min-width="320" show-overflow-tooltip />
                  <template #empty>
                    <span class="xiaomi-result-empty">{T['empty_result']}</span>
                  </template>
                </el-table>
                <div v-if="submitTimeText" class="xiaomi-result-time">{T['submit_time']}{zh(0xFF1A)}{{{{ submitTimeText }}}}</div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="{T['tab_record']}" name="record">
            <div class="record-pane">
              <el-form
                :model="queryParams"
                :inline="true"
                size="small"
                label-width="68px"
                class="record-form"
                @submit.prevent
              >
                <el-form-item label="{T['search_label']}">
                  <el-input
                    v-model="queryParams.keyword"
                    clearable
                    placeholder="{T['search_ph']}"
                    style="width: 220px"
                    @keyup.enter="handleQuery"
                  />
                </el-form-item>
                <el-form-item label="{T['status_label']}">
                  <el-select v-model="queryParams.orderStatus" clearable placeholder="{T['status_ph']}" style="width: 120px">
                    <el-option label="{T['pending']}" value="0" />
                    <el-option label="{T['processing']}" value="1" />
                    <el-option label="{T['done']}" value="2" />
                    <el-option label="{T['cancelled']}" value="3" />
                  </el-select>
                </el-form-item>
                <el-form-item label="{T['submit_time_label']}">
                  <el-date-picker
                    v-model="recordDateRange"
                    type="daterange"
                    range-separator="-"
                    start-placeholder="{T['start']}"
                    end-placeholder="{T['end']}"
                    format="YYYY/MM/DD"
                    value-format="YYYY-MM-DD"
                    style="width: 240px"
                    @change="handleRecordDateRangeChange"
                  />
                </el-form-item>
                <el-form-item class="record-form__actions">
                  <el-button type="primary" icon="Search" @click="handleQuery">{T['search_btn']}</el-button>
                  <el-button icon="Refresh" @click="resetQuery">{T['reset_query']}</el-button>
                  <el-button icon="Download" @click="exportRecordRows">{T['export_btn']}</el-button>
                </el-form-item>
              </el-form>

              <el-table
                v-loading="loading"
                :data="orderList"
                border
                stripe
                :row-key="recordRowKey"
                class="record-table"
                @selection-change="handleRecordSelectionChange"
              >
                <el-table-column type="selection" width="48" align="center" />
                <el-table-column label="{T['user_name']}" prop="userName" min-width="110" show-overflow-tooltip />
                <el-table-column label="{T['phone_copy']}" min-width="150" show-overflow-tooltip>
                  <template #default="scope">
                    <el-button link type="primary" @click="copyText(scope.row.phonePreview || '')">
                      {{{{ scope.row.phonePreview || '-' }}}}
                    </el-button>
                  </template>
                </el-table-column>
                <el-table-column label="{T['platform']}" prop="platformName" min-width="110" show-overflow-tooltip />
                <el-table-column label="{T['status_label']}" width="92" align="center">
                  <template #default="scope">
                    <el-tag :type="recordStatusType(scope.row)" size="small">
                      {{{{ recordStatusLabel(scope.row) }}}}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="{T['order_no']}" prop="orderNo" min-width="180" show-overflow-tooltip />
                <el-table-column label="{T['submit_time_label']}" min-width="160" align="center">
                  <template #default="scope">
                    {{{{ formatDateTime(scope.row.createTime) }}}}
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
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </div>
</template>

<script setup name="MarkUserXiaomi">
import {{ computed, ref, reactive, watch, onMounted, onBeforeUnmount, getCurrentInstance }} from 'vue'
import {{ createMarkUserClearOrder, listMarkUserPlatformPrice, listMarkUserOrder }} from '@/api/server/markUser'
import {{ XIAOMI_PLATFORM_CODE }} from '@/utils/markXiaomiPlatform'

const {{ proxy }} = getCurrentInstance()

const activeTab = ref('submit')
const phonesText = ref('')
const submitting = ref(false)
const remainCount = ref(0)
const platformName = ref('{T["platform_name"]}')
const submitResult = ref(null)

const loading = ref(false)
const total = ref(0)
const orderList = ref([])
const recordSelectedRows = ref([])
const recordDateRange = ref([])
const queryParams = reactive({{
  pageNum: 1,
  pageSize: 10,
  keyword: null,
  phone: null,
  platformCode: XIAOMI_PLATFORM_CODE,
  orderStatus: null,
  params: {{}}
}})

const validPhones = computed(() => {{
  const seen = new Set()
  const result = []
  String(phonesText.value || '')
    .split(/\\r?\\n/)
    .forEach((line) => {{
      const digits = String(line || '').replace(/\\D/g, '')
      if (digits.length < 7 || digits.length > 15) return
      if (seen.has(digits)) return
      seen.add(digits)
      result.push(digits)
    }})
  return result
}})

const submitTimeText = computed(() => submitResult.value?.submitTime || '')

const resultRows = computed(() => {{
  if (!submitResult.value) return []
  return [
    {{ label: '{T["batch_no"]}', value: submitResult.value.orderNo || '-' }},
    {{
      label: '{T["phone_count"]}',
      value: `{T["count_tpl"]}${{submitResult.value.phoneCount || 0}}{T["count_suffix"]}`
    }}
  ]
}})

function formatDateTime(value) {{
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${{d.getFullYear()}}-${{p(d.getMonth() + 1)}}-${{p(d.getDate())}} ${{p(d.getHours())}}:${{p(d.getMinutes())}}:${{p(d.getSeconds())}}`
}}

async function loadPlatformInfo() {{
  try {{
    const resp = await listMarkUserPlatformPrice()
    const list = Array.isArray(resp?.data) ? resp.data : []
    const matched = list.find((item) => String(item.platformCode || '').toLowerCase() === XIAOMI_PLATFORM_CODE)
      || list.find((item) => String(item.platformName || '').includes('{zh(0x5C0F, 0x7C73)}'))
    if (matched) {{
      platformName.value = matched.platformName || platformName.value
      remainCount.value = Number(matched.remainCount ?? 0)
    }}
  }} catch (error) {{
    remainCount.value = 0
  }}
}}

async function handleSubmit() {{
  const phones = validPhones.value
  if (!phones.length) {{
    proxy.$modal.msgWarning('{T["msg_no_valid_phones"]}')
    return
  }}
  if (remainCount.value > 0 && phones.length > remainCount.value) {{
    proxy.$modal.msgError('{T["msg_remain_insufficient"]}')
    return
  }}
  submitting.value = true
  try {{
    const res = await createMarkUserClearOrder({{
      platformCode: XIAOMI_PLATFORM_CODE,
      platformName: platformName.value,
      phones,
      requestNo: '',
      remark: ''
    }})
    const order = res?.data?.order || res?.data || {{}}
    const phoneCount = Number(order.totalCount || phones.length || 0)
    submitResult.value = {{
      orderNo: order.orderNo || '-',
      phoneCount,
      submitTime: formatDateTime(order.createTime || new Date())
    }}
    phonesText.value = ''
    await loadPlatformInfo()
    proxy.$modal.msgSuccess(`{T["count_tpl"]}${{phoneCount}}{T["count_suffix"]}`)
    if (activeTab.value === 'record') {{
      await getList()
    }}
  }} catch (error) {{
    proxy.$modal.msgError(error?.message || '{T["msg_submit_failed"]}')
  }} finally {{
    submitting.value = false
  }}
}}

function handleReset() {{
  phonesText.value = ''
}}

async function copyText(text) {{
  const value = String(text || '').trim()
  if (!value) {{
    proxy.$modal.msgWarning('{T["no_copy"]}')
    return
  }}
  try {{
    if (navigator?.clipboard?.writeText) {{
      await navigator.clipboard.writeText(value)
    }} else {{
      const textarea = document.createElement('textarea')
      textarea.value = value
      textarea.style.position = 'fixed'
      textarea.style.left = '-9999px'
      document.body.appendChild(textarea)
      textarea.focus()
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }}
    proxy.$modal.msgSuccess('{T["copied"]}')
  }} catch (error) {{
    proxy.$modal.msgError('{T["copy_fail"]}')
  }}
}}

function recordStatusLabel(row) {{
  const auditStatus = String(row?.auditStatus ?? '1')
  if (auditStatus === '0') return '{T["audit_pending"]}'
  if (auditStatus === '2') return '{T["rejected"]}'
  if (auditStatus === '3') return '{T["returned"]}'
  const itemStatus = String(row?.itemProcessStatus ?? '')
  if (itemStatus === '0') return '{T["pending"]}'
  if (itemStatus === '1') return '{T["success"]}'
  if (itemStatus === '2') return '{T["fail"]}'
  const status = String(row?.orderStatus ?? '')
  const successCount = Number(row?.successCount ?? 0)
  const failedCount = Number(row?.failedCount ?? 0)
  if (status === '0' || status === '1') return '{T["pending"]}'
  if (status === '2') return failedCount > 0 && successCount <= 0 ? '{T["fail"]}' : (failedCount > 0 ? '{T["partial_fail"]}' : '{T["success"]}')
  if (status === '3') return '{T["fail"]}'
  return '{T["pending"]}'
}}

function recordStatusType(row) {{
  const auditStatus = String(row?.auditStatus ?? '1')
  if (auditStatus === '2') return 'danger'
  if (auditStatus === '3') return 'warning'
  if (auditStatus === '0') return 'info'
  const itemStatus = String(row?.itemProcessStatus ?? '')
  if (itemStatus === '1') return 'success'
  if (itemStatus === '2') return 'danger'
  if (itemStatus === '0') return 'warning'
  const label = recordStatusLabel(row)
  if (label === '{T["success"]}') return 'success'
  if (label === '{T["fail"]}' || label === '{T["partial_fail"]}') return 'danger'
  return 'warning'
}}

function recordRowKey(row) {{
  return `${{row?.itemId ?? row?.id ?? ''}}-${{row?.phonePreview ?? ''}}`
}}

function normalizeRecordKeyword() {{
  const keyword = String(queryParams.keyword || '').trim()
  queryParams.keyword = keyword || null
  if (keyword && /^\\d{{7,15}}$/.test(keyword)) {{
    queryParams.phone = keyword
    return
  }}
  queryParams.phone = null
}}

function getList() {{
  queryParams.platformCode = XIAOMI_PLATFORM_CODE
  normalizeRecordKeyword()
  loading.value = true
  return listMarkUserOrder(queryParams).then((res) => {{
    orderList.value = res.rows || []
    total.value = res.total || 0
  }}).finally(() => {{
    loading.value = false
  }})
}}

function handleQuery() {{
  queryParams.pageNum = 1
  getList()
}}

function resetQuery() {{
  queryParams.keyword = null
  queryParams.phone = null
  queryParams.orderStatus = null
  queryParams.params = {{}}
  recordDateRange.value = []
  recordSelectedRows.value = []
  queryParams.platformCode = XIAOMI_PLATFORM_CODE
  handleQuery()
}}

function handleRecordDateRangeChange(value) {{
  if (!Array.isArray(value) || value.length !== 2 || !value[0] || !value[1]) {{
    recordDateRange.value = []
    queryParams.params = {{}}
    return
  }}
  queryParams.params = {{
    beginTime: value[0],
    endTime: value[1]
  }}
}}

function handleRecordSelectionChange(rows) {{
  recordSelectedRows.value = Array.isArray(rows) ? rows : []
}}

function buildCsvCell(value) {{
  const text = String(value ?? '')
  return `"${{text.replace(/"/g, '""')}}"`
}}

function downloadCsv(filename, rows) {{
  const csvText = `\\ufeff${{rows.map((row) => row.map((cell) => buildCsvCell(cell)).join(',')).join('\\n')}}`
  const blob = new Blob([csvText], {{ type: 'text/csv;charset=utf-8;' }})
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}}

function exportRecordRows() {{
  const rows = recordSelectedRows.value.length > 0 ? recordSelectedRows.value : orderList.value
  if (!rows.length) {{
    proxy.$modal.msgWarning('{T["no_export"]}')
    return
  }}
  const header = ['{T["user_name"]}', '{T["phone_copy"]}', '{T["platform"]}', '{T["status_label"]}', '{T["order_no"]}', '{T["submit_time_label"]}']
  const body = rows.map((item) => [
    item.userName || '',
    item.phonePreview || '',
    item.platformName || '',
    recordStatusLabel(item),
    item.orderNo || '',
    formatDateTime(item.createTime)
  ])
  downloadCsv(`xiaomi-record-${{Date.now()}}.csv`, [header, ...body])
}}

let recordPollTimer = null
function startRecordPolling() {{
  stopRecordPolling()
  recordPollTimer = window.setInterval(() => {{
    if (activeTab.value !== 'record') return
    getList()
  }}, 5000)
}}

function stopRecordPolling() {{
  if (recordPollTimer) {{
    window.clearInterval(recordPollTimer)
    recordPollTimer = null
  }}
}}

function syncRecordTabData() {{
  if (activeTab.value === 'record') {{
    getList()
    startRecordPolling()
  }} else {{
    stopRecordPolling()
  }}
}}

watch(activeTab, () => {{
  syncRecordTabData()
}})

onMounted(() => {{
  loadPlatformInfo()
  syncRecordTabData()
}})

onBeforeUnmount(() => {{
  stopRecordPolling()
}})
</script>

<style scoped lang="scss">
.mark-user-xiaomi-page {{
  padding: 0 !important;
  background: #f5f7fa;
}}

.xiaomi-page-card {{
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background: #fff;
}}

.platform-main {{
  position: relative;
}}

.platform-main-remain {{
  position: absolute;
  top: 0;
  right: 16px;
  z-index: 2;
  display: flex;
  align-items: center;
  height: 40px;
}}

.sub-tabs :deep(.el-tabs__header) {{
  margin: 0;
  padding: 0 16px;
  background: #fff;
}}

.sub-tabs :deep(.el-tabs__nav-wrap::after) {{
  height: 1px;
  background-color: var(--el-border-color-light);
}}

.sub-tabs :deep(.el-tabs__item) {{
  height: 42px;
  line-height: 42px;
  padding: 0 18px;
  font-size: 14px;
}}

.sub-tabs :deep(.el-tabs__item.is-active) {{
  color: var(--el-color-primary);
  font-weight: 600;
}}

.sub-tabs :deep(.el-tabs__content) {{
  padding: 0;
}}

.platform-main:has(.platform-main-remain) .sub-tabs :deep(.el-tabs__header) {{
  padding-right: 160px;
}}

.submit-right-remain {{
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
}}

.submit-right-remain span {{
  color: #409eff;
  font-weight: 600;
}}

.submit-pane,
.record-pane {{
  padding: 16px 20px 20px;
  background: #fff;
}}

.xiaomi-submit-panel {{
  max-width: 980px;
}}

.xiaomi-submit-row {{
  display: flex;
  align-items: flex-start;
  gap: 16px;
}}

.xiaomi-submit-label {{
  flex: 0 0 auto;
  padding-top: 8px;
  font-size: 14px;
  line-height: 1.5;
  color: #303133;
  white-space: nowrap;
}}

.xiaomi-submit-textarea {{
  flex: 1;
  min-width: 0;

  :deep(.el-textarea__inner) {{
    min-height: 220px;
    font-size: 14px;
    line-height: 1.6;
  }}
}}

.xiaomi-submit-actions {{
  display: flex;
  gap: 12px;
  margin-top: 14px;
}}

.xiaomi-action-btn {{
  min-width: 108px;
  height: 36px;
}}

.xiaomi-result-block {{
  margin-top: 18px;
}}

.xiaomi-result-head {{
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}}

.xiaomi-result-table {{
  :deep(.el-table__header th) {{
    background: #eef3f8;
    color: #303133;
    font-weight: 600;
  }}
}}

.xiaomi-result-empty {{
  color: #909399;
  font-size: 13px;
}}

.xiaomi-result-time {{
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
}}

.record-form {{
  margin-bottom: 12px;
}}

.record-form__actions {{
  margin-left: auto;
}}

.record-table {{
  width: 100%;
}}
</style>
"""

target = Path(__file__).resolve().parents[1] / "frontend/src/views/server/mark/user/xiaomi.vue"
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(content, encoding="utf-8")
print(f"Wrote {target}")
