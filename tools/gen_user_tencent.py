# -*- coding: utf-8 -*-
"""Generate user Tencent page with optimized submit layout (UTF-8 Chinese)."""
from pathlib import Path


def zh(*codes: int) -> str:
    return "".join(chr(c) for c in codes)


T = {
    "tab_submit": zh(0x817E, 0x8BAF, 0x20, 0x2D, 0x20, 0x63D0, 0x4EA4, 0x53F7, 0x7801),
    "tab_record": zh(0x817E, 0x8BAF, 0x20, 0x2D, 0x20, 0x4EFB, 0x52A1, 0x8BB0, 0x5F55),
    "remain_label": zh(0x5F53, 0x524D, 0x5269, 0x4F59, 0xFF1A),
    "remain_suffix": zh(0x6B21),
    "title": zh(0x817E, 0x8BAF, 0x5E73, 0x53F0, 0x6E05, 0x9664, 0x28, 0x666E, 0x901A, 0x26, 0x9AD8, 0x9891, 0x29),
    "tip": zh(
        0x8BF7, 0x5148, 0x5728, 0x817E, 0x8BAF, 0x5B98, 0x65B9, 0x9875, 0x9762, 0x83B7, 0x53D6, 0x77ED, 0x4FE1, 0x9A8C,
        0x8BC1, 0x7801, 0xFF0C, 0x518D, 0x5728, 0x6B64, 0x63D0, 0x4EA4, 0x5904, 0x7406
    ),
    "guide_title": zh(0x64CD, 0x4F5C, 0x8BF4, 0x660E),
    "guide_1_prefix": zh(0x5728),
    "guide_1_link": zh(0x817E, 0x8BAF, 0x5B98, 0x65B9, 0x9875, 0x9762),
    "guide_1_suffix": zh(0x4E0B, 0x53D1, 0x9A8C, 0x8BC1, 0x7801),
    "guide_2": zh(0x586B, 0x5199, 0x624B, 0x673A, 0x53F7, 0x4E0E, 0x9A8C, 0x8BC1, 0x7801, 0xFF0C, 0x70B9, 0x51FB, 0x300C, 0x63D0, 0x4EA4, 0x5904, 0x7406, 0x300D),
    "phone_label": zh(0x624B, 0x673A, 0x53F7),
    "sms_label": zh(0x9A8C, 0x8BC1, 0x7801),
    "phone_ph": zh(0x8BF7, 0x8F93, 0x5165, 0x31, 0x31, 0x4F4D, 0x624B, 0x673A, 0x53F7),
    "sms_ph": zh(0x8BF7, 0x8F93, 0x5165, 0x36, 0x4F4D, 0x9A8C, 0x8BC1, 0x7801),
    "quota_warn": zh(0x5F53, 0x524D, 0x817E, 0x8BAF, 0x5E73, 0x53F0, 0x5269, 0x4F59, 0x6B21, 0x6570, 0x4E0D, 0x8DB3, 0xFF0C, 0x65E0, 0x6CD5, 0x63D0, 0x4EA4, 0x3002),
    "submit_btn": zh(0x63D0, 0x4EA4, 0x5904, 0x7406),
    "reset_btn": zh(0x91CD, 0x7F6E),
    "result_title": zh(0x63D0, 0x4EA4, 0x7ED3, 0x679C),
    "col_item": zh(0x9879, 0x76EE),
    "col_remark": zh(0x4FE1, 0x606F, 0x5907, 0x6CE8),
    "empty_result": zh(0x6682, 0x65E0, 0x63D0, 0x4EA4, 0x7ED3, 0x679C),
    "row_submit_order_no": zh(0x63D0, 0x4EA4, 0x8BA2, 0x5355, 0x53F7),
    "row_submit_count": zh(0x63D0, 0x4EA4, 0x6570, 0x91CF),
    "count_tpl": zh(0x6B63, 0x5E38, 0x63D0, 0x4EA4, 0x4E86),
    "count_suffix": zh(0x4E2A, 0x624B, 0x673A, 0x53F7, 0x7801),
    "row_phone": zh(0x624B, 0x673A, 0x53F7),
    "row_result": zh(0x5904, 0x7406, 0x7ED3, 0x679C),
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
    "tencent_name": zh(0x817E, 0x8BAF),
    "no_copy": zh(0x6CA1, 0x6709, 0x53EF, 0x590D, 0x5236, 0x5185, 0x5BB9),
    "copied": zh(0x590D, 0x5236, 0x6210, 0x529F),
    "copy_fail": zh(0x590D, 0x5236, 0x5931, 0x8D25, 0xFF0C, 0x8BF7, 0x68C0, 0x67E5, 0x6D4F, 0x89C8, 0x5668, 0x6743, 0x9650),
    "no_export": zh(0x6682, 0x65E0, 0x53EF, 0x5BFC, 0x51FA, 0x8BB0, 0x5F55),
    "msg_phone": zh(0x8BF7, 0x8F93, 0x5165, 0x31, 0x31, 0x4F4D, 0x624B, 0x673A, 0x53F7),
    "msg_quota": zh(0x5F53, 0x524D, 0x817E, 0x8BAF, 0x5E73, 0x53F0, 0x5269, 0x4F59, 0x6B21, 0x6570, 0x4E0D, 0x8DB3),
    "msg_sms": zh(0x8BF7, 0x8F93, 0x5165, 0x36, 0x4F4D, 0x9A8C, 0x8BC1, 0x7801, 0x540E, 0x518D, 0x63D0, 0x4EA4),
    "msg_processing": zh(0x63D0, 0x4EA4, 0x6210, 0x529F, 0xFF0C, 0x540E, 0x53F0, 0x5904, 0x7406, 0x4E2D),
    "msg_submit_fail": zh(0x817E, 0x8BAF, 0x63D0, 0x4EA4, 0x5931, 0x8D25),
    "poll_success": zh(0x63D0, 0x4EA4, 0x6210, 0x529F),
    "poll_fail": zh(0x63D0, 0x4EA4, 0x5931, 0x8D25, 0xFF0C, 0x9A8C, 0x8BC1, 0x7801, 0x9519, 0x8BEF, 0x6216, 0x8005, 0x5931, 0x6548),
    "poll_pending": zh(0x63D0, 0x4EA4, 0x6210, 0x529F, 0xFF0C, 0x540E, 0x53F0, 0x5904, 0x7406, 0x4E2D),
    "poll_timeout": zh(0x540E, 0x53F0, 0x5904, 0x7406, 0x4E2D, 0xFF0C, 0x8BF7, 0x7A0D, 0x540E, 0x5728, 0x63D0, 0x4EA4, 0x8BB0, 0x5F55, 0x4E2D, 0x67E5, 0x770B, 0x7ED3, 0x679C),
}

content = f"""<template>
  <div class="app-container tencent-mark-page">
    <el-card shadow="never" class="page-card" :body-style="{{ padding: '0' }}">
      <div class="platform-main">
        <div v-if="activeTab === 'submit'" class="platform-main-remain">
          <div class="submit-right-remain">
            {T['remain_label']}<span>{{{{ remainCount }}}}</span> {T['remain_suffix']}
          </div>
        </div>
        <el-tabs v-model="activeTab" class="sub-tabs">
          <el-tab-pane label="{T['tab_submit']}" name="submit">
            <div class="submit-pane">
              <div class="tencent-content-shell">
                <div class="tencent-submit-section">
                  <div class="tencent-submit-head">
                    <div class="tencent-submit-title">{T['title']}</div>
                    <p class="tencent-submit-tip">{T['tip']}</p>

                    <div class="tencent-guide-box">
                      <div class="tencent-guide-box__title">{T['guide_title']}</div>
                      <ol class="tencent-guide-box__list">
                        <li>
                          {T['guide_1_prefix']}
                          <el-link
                            type="primary"
                            href="https://yun.m.qq.com/person_apply.html"
                            target="_blank"
                          >{T['guide_1_link']}</el-link>
                          {T['guide_1_suffix']}
                        </li>
                        <li>{T['guide_2']}</li>
                      </ol>
                    </div>
                  </div>

                  <div class="tencent-form-panel">
                    <div v-if="remainCount < 1" class="submit-warning-bar">
                      {T['quota_warn']}
                    </div>

                    <el-form
                      label-width="72px"
                      size="default"
                      class="tencent-submit-form"
                      @keyup.enter="handleSubmit"
                    >
                      <el-form-item label="{T['phone_label']}">
                        <el-input
                          v-model="form.phone"
                          maxlength="11"
                          clearable
                          placeholder="{T['phone_ph']}"
                          @input="handlePhoneInput"
                        />
                      </el-form-item>
                      <el-form-item label="{T['sms_label']}">
                        <el-input
                          v-model="form.smsCode"
                          maxlength="6"
                          clearable
                          placeholder="{T['sms_ph']}"
                          @input="handleSmsInput"
                        />
                      </el-form-item>
                    </el-form>

                    <div class="tencent-submit-buttons">
                      <el-button
                        type="primary"
                        class="tencent-action-btn"
                        :loading="submitting"
                        :disabled="remainCount < 1"
                        v-hasPermi="['server:markUser:order:add']"
                        @click="handleSubmit"
                      >
                        {T['submit_btn']}
                      </el-button>
                      <el-button
                        class="tencent-action-btn"
                        :disabled="submitting || (!form.phone && !form.smsCode)"
                        @click="handleResetForm"
                      >
                        {T['reset_btn']}
                      </el-button>
                    </div>
                  </div>
                </div>

                <div class="tencent-result-block">
                  <div class="tencent-result-head">{T['result_title']}</div>
                  <el-table :data="resultRows" border stripe class="tencent-result-table">
                    <el-table-column prop="label" label="{T['col_item']}" width="108" align="center" />
                    <el-table-column prop="value" label="{T['col_remark']}" min-width="200" show-overflow-tooltip />
                    <template #empty>
                      <span class="tencent-result-empty">{T['empty_result']}</span>
                    </template>
                  </el-table>
                </div>
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
                  <el-select
                    v-model="queryParams.orderStatus"
                    clearable
                    placeholder="{T['status_ph']}"
                    style="width: 120px"
                  >
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

<script setup name="MarkUserTencent">
import {{
  listMarkUserPlatformPrice,
  listMarkUserOrder,
  submitMarkUserTencent,
  getMarkUserTencentSubmitResult,
  getMarkUserOrderDetail
}} from '@/api/server/markUser'

const TENCENT_PLATFORM_CODE = 'tencent_mark'
const {{ proxy }} = getCurrentInstance()

const activeTab = ref('submit')
const submitting = ref(false)
const remainCount = ref(0)
const submitResult = ref(null)

const form = reactive({{
  phone: '',
  smsCode: ''
}})

const loading = ref(false)
const total = ref(0)
const orderList = ref([])
const recordSelectedRows = ref([])
const recordDateRange = ref([])
const queryParams = reactive({{
  pageNum: 1,
  pageSize: 10,
  orderNo: null,
  requestNo: null,
  keyword: null,
  phone: null,
  platformCode: TENCENT_PLATFORM_CODE,
  orderStatus: null,
  params: {{}}
}})

const resultRows = computed(() => {{
  if (!submitResult.value) return []
  const phoneCount = Number(submitResult.value.phoneCount || 0)
  return [
    {{ label: '{T['row_submit_order_no']}', value: submitResult.value.orderNo || '-' }},
    {{
      label: '{T['row_submit_count']}',
      value: phoneCount > 0 ? `{T['count_tpl']}${{phoneCount}}{T['count_suffix']}` : '-'
    }}
  ]
}})

function normalizePhone(value) {{
  return String(value || '').replace(/[^\\d]/g, '')
}}

function handlePhoneInput(value) {{
  form.phone = normalizePhone(value).slice(0, 11)
}}

function handleSmsInput(value) {{
  form.smsCode = normalizePhone(value).slice(0, 6)
}}

function resetSubmitState() {{
  submitResult.value = null
}}

function handleResetForm() {{
  form.phone = ''
  form.smsCode = ''
  resetSubmitState()
}}

async function resolveSubmitOrderNo(payload) {{
  let orderNo = String(payload?.orderNo || '').trim()
  const orderId = payload?.orderId
  if (!orderNo && orderId) {{
    try {{
      const detailRes = await getMarkUserOrderDetail(orderId)
      orderNo = String(detailRes?.data?.order?.orderNo || '').trim()
    }} catch (error) {{
      // ignore
    }}
  }}
  return orderNo || '-'
}}

async function applySubmitResultFromPayload(payload) {{
  if (!payload?.itemId) return null
  submitResult.value = {{
    orderNo: await resolveSubmitOrderNo(payload),
    phoneCount: 1,
    itemId: payload.itemId
  }}
  return submitResult.value
}}

function buildPollResultText(result) {{
  const status = String(result?.processStatus || '')
  if (status === '1') {{
    return '{T['poll_success']}'
  }}
  if (status === '2') {{
    return '{T['poll_fail']}'
  }}
  return '{T['poll_pending']}'
}}

let pollAborted = false

async function pollTencentSubmitResult(itemId) {{
  const maxAttempts = 40
  if (!itemId) {{
    return
  }}
  for (let attempt = 0; attempt < maxAttempts; attempt++) {{
    if (pollAborted) {{
      return
    }}
    if (attempt > 0) {{
      await new Promise((resolve) => setTimeout(resolve, 30000))
    }}
    try {{
      const res = await getMarkUserTencentSubmitResult(itemId)
      const result = res?.data || null
      const status = String(result?.processStatus || '')
      if (status === '1' || status === '2') {{
        if (submitResult.value && submitResult.value.orderNo === '-' && result?.orderId) {{
          const orderNo = await resolveSubmitOrderNo(result)
          if (orderNo !== '-') {{
            submitResult.value = {{
              ...submitResult.value,
              orderNo
            }}
          }}
        }}
        if (status === '1') {{
          proxy.$modal.msgSuccess(buildPollResultText(result))
        }}
        await loadPlatformInfo()
        if (activeTab.value === 'record') {{
          await getList()
        }}
        return
      }}
    }} catch {{
      // ignore and retry
    }}
  }}
  proxy.$modal.msgWarning('{T['poll_timeout']}')
}}

async function loadPlatformInfo() {{
  try {{
    const res = await listMarkUserPlatformPrice()
    const list = Array.isArray(res?.data) ? res.data : []
    const item = list.find((row) => String(row.platformCode || '').toLowerCase() === TENCENT_PLATFORM_CODE)
      || list.find((row) => String(row.platformName || '').includes('{T['tencent_name']}'))
    const remain = Number(item?.remainCount ?? 0)
    remainCount.value = Number.isFinite(remain) ? Math.max(0, remain) : 0
  }} catch {{
    remainCount.value = 0
  }}
}}

async function handleSubmit() {{
  const phone = normalizePhone(form.phone)
  if (!/^\\d{{11}}$/.test(phone)) {{
    proxy.$modal.msgWarning('{T['msg_phone']}')
    return
  }}
  if (remainCount.value < 1) {{
    proxy.$modal.msgError('{T['msg_quota']}')
    return
  }}
  if (!/^\\d{{6}}$/.test(form.smsCode)) {{
    proxy.$modal.msgWarning('{T['msg_sms']}')
    return
  }}

  pollAborted = false
  submitting.value = true
  try {{
    const res = await submitMarkUserTencent({{
      phone,
      smsCode: form.smsCode
    }})
    const result = res?.data || null
    if (result?.itemId) {{
      await applySubmitResultFromPayload(result)
      form.phone = ''
      form.smsCode = ''
      proxy.$modal.msgSuccess('{T['msg_processing']}')
      await pollTencentSubmitResult(result.itemId)
    }}
  }} catch {{
    // submit failure: no floating toast
  }} finally {{
    submitting.value = false
  }}
}}

async function copyText(text) {{
  const value = String(text || '').trim()
  if (!value) {{
    proxy.$modal.msgWarning('{T['no_copy']}')
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
    proxy.$modal.msgSuccess('{T['copied']}')
  }} catch (error) {{
    proxy.$modal.msgError('{T['copy_fail']}')
  }}
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

function recordStatusLabel(row) {{
  const auditStatus = String(row?.auditStatus ?? '1')
  if (auditStatus === '0') return '{T['audit_pending']}'
  if (auditStatus === '2') return '{T['rejected']}'
  if (auditStatus === '3') return '{T['returned']}'
  const itemStatus = String(row?.itemProcessStatus ?? '')
  if (itemStatus === '0') return '{T['pending']}'
  if (itemStatus === '1') return '{T['success']}'
  if (itemStatus === '2') return '{T['fail']}'
  const status = String(row?.orderStatus ?? '')
  const successCount = Number(row?.successCount ?? 0)
  const failedCount = Number(row?.failedCount ?? 0)
  if (status === '0' || status === '1') return '{T['pending']}'
  if (status === '2') return failedCount > 0 && successCount <= 0 ? '{T['fail']}' : (failedCount > 0 ? '{T['partial_fail']}' : '{T['success']}')
  if (status === '3') return '{T['fail']}'
  return '{T['pending']}'
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
  if (label === '{T['success']}') return 'success'
  if (label === '{T['fail']}' || label === '{T['partial_fail']}') return 'danger'
  return 'warning'
}}

function formatDateTime(value) {{
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${{d.getFullYear()}}-${{p(d.getMonth() + 1)}}-${{p(d.getDate())}} ${{p(d.getHours())}}:${{p(d.getMinutes())}}:${{p(d.getSeconds())}}`
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
  queryParams.platformCode = TENCENT_PLATFORM_CODE
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
  queryParams.orderNo = null
  queryParams.requestNo = null
  queryParams.keyword = null
  queryParams.phone = null
  queryParams.orderStatus = null
  queryParams.params = {{}}
  recordDateRange.value = []
  recordSelectedRows.value = []
  queryParams.platformCode = TENCENT_PLATFORM_CODE
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

function exportRecordRows() {{
  const rows = recordSelectedRows.value.length > 0 ? recordSelectedRows.value : orderList.value
  if (!rows.length) {{
    proxy.$modal.msgWarning('{T['no_export']}')
    return
  }}
  const header = ['{T['user_name']}', '{T['phone_label']}', '{T['platform']}', '{T['status_label']}', '{T['order_no']}', '{T['submit_time_label']}']
  const body = rows.map((item) => [
    item.userName || '',
    item.phonePreview || '',
    item.platformName || '',
    recordStatusLabel(item),
    item.orderNo || '',
    formatDateTime(item.createTime)
  ])
  downloadCsv(`tencent-record-${{Date.now()}}.csv`, [header, ...body])
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
  pollAborted = true
  stopRecordPolling()
}})
</script>

<style scoped lang="scss">
.tencent-mark-page {{
  padding: 0 !important;
  background: #f5f7fa;
}}

.page-card {{
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
  padding: 14px 16px 18px;
  background: #fff;
}}

.tencent-content-shell {{
  width: 66%;
  max-width: 660px;
  min-width: 420px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}}

.tencent-submit-section {{
  width: 100%;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
}}

.tencent-submit-head {{
  margin-bottom: 12px;
}}

.tencent-form-panel {{
  margin-top: 2px;
  padding: 12px 14px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #fafbfc;
  box-sizing: border-box;
}}

.tencent-submit-title {{
  margin-bottom: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
  text-align: center;
}}

.tencent-submit-tip {{
  margin: 0 0 10px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  text-align: center;
}}

.tencent-guide-box {{
  margin-bottom: 10px;
  padding: 8px 10px;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  background: #ecf5ff;
  text-align: center;
}}

.tencent-guide-box__title {{
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  text-align: center;
}}

.tencent-guide-box__list {{
  margin: 0;
  padding-left: 0;
  list-style-position: inside;
  color: #606266;
  font-size: 12px;
  line-height: 1.7;
  text-align: center;
}}

.tencent-guide-box__list li {{
  margin-bottom: 2px;
}}

.tencent-submit-form {{
  width: 100%;
  margin-bottom: 10px;

  :deep(.el-form-item) {{
    width: 100%;
    margin-right: 0;
    margin-bottom: 10px;
  }}

  :deep(.el-form-item:last-child) {{
    margin-bottom: 0;
  }}

  :deep(.el-form-item__content) {{
    flex: 1;
  }}

  :deep(.el-input) {{
    width: 100%;
  }}

  :deep(.el-form-item__label) {{
    font-size: 13px;
    color: #606266;
    padding-right: 10px;
  }}

  :deep(.el-input__wrapper) {{
    font-size: 13px;
  }}
}}

.submit-warning-bar {{
  width: 100%;
  margin-bottom: 10px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 13px;
  line-height: 1.5;
}}

.tencent-submit-buttons {{
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-left: 72px;
}}

.tencent-action-btn {{
  min-width: 88px;
  height: 32px;
  padding: 0 14px;
  margin: 0;
  font-size: 13px;
}}

.tencent-result-block {{
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-top: 12px;
  padding: 12px 14px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #fff;
  box-sizing: border-box;
  min-height: 188px;
}}

.tencent-result-head {{
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}}

.tencent-result-table {{
  width: 100%;
  flex: 1;

  :deep(.el-table__header th) {{
    background: #eef3f8;
    color: #303133;
    font-weight: 600;
    font-size: 13px;
    padding: 6px 0;
  }}

  :deep(.el-table__body td) {{
    font-size: 13px;
    padding: 6px 0;
  }}

  :deep(.el-table__empty-block) {{
    min-height: 148px;
  }}

  :deep(.el-table__empty-text) {{
    line-height: 148px;
  }}
}}

.tencent-result-empty {{
  color: #909399;
  font-size: 13px;
}}

.tencent-result-time {{
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}}

.record-form {{
  margin-bottom: 12px;
  padding: 14px 16px 6px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: #fafafa;
}}

.record-form :deep(.el-form-item) {{
  margin-bottom: 10px;
  margin-right: 12px;
}}

.record-form__actions :deep(.el-button + .el-button) {{
  margin-left: 8px;
}}

.record-table {{
  width: 100%;
}}

.record-pane :deep(.pagination-container) {{
  margin-top: 12px;
  padding: 0;
}}

@media (max-width: 768px) {{
  .tencent-content-shell {{
    width: 100%;
    max-width: none;
    min-width: 0;
  }}

  .tencent-submit-buttons {{
    padding-left: 0;
  }}

  .platform-main:has(.platform-main-remain) .sub-tabs :deep(.el-tabs__header) {{
    padding-right: 16px;
  }}

  .platform-main-remain {{
    position: static;
    justify-content: flex-end;
    height: auto;
    padding: 8px 16px 0;
  }}

  .submit-pane,
  .record-pane {{
    padding: 12px;
  }}
}}
</style>
"""

out = Path(__file__).resolve().parents[1] / "frontend" / "src" / "views" / "server" / "mark" / "user" / "tencent.vue"
out.write_text(content, encoding="utf-8")
print(f"Wrote {out}")
