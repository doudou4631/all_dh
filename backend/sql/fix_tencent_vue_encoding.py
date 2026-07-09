# -*- coding: utf-8 -*-
"""Regenerate tencent.vue with UTF-8 Chinese (avoids Windows editor encoding issues)."""
from pathlib import Path


def zh(*codes: int) -> str:
    return "".join(chr(c) for c in codes)


T = {
    "tab_submit": zh(0x817E, 0x8BAF, 0x20, 0x2D, 0x20, 0x63D0, 0x4EA4, 0x53F7, 0x7801),
    "tab_record": zh(0x817E, 0x8BAF, 0x20, 0x2D, 0x20, 0x4EFB, 0x52A1, 0x8BB0, 0x5F55),
    "remain_label": zh(0x5F53, 0x524D, 0x5269, 0x4F59, 0xFF1A),
    "title": zh(
        0x817E, 0x8BAF, 0x5E73, 0x53F0, 0x6E05, 0x9664, 0x28, 0x666E, 0x901A,
        0x26, 0x9AD8, 0x9891, 0x29,
    ),
    "subtitle": "",
    "step1_prefix": zh(0x5728, 0x3010),
    "step1_suffix": zh(
        0x3011, 0x9875, 0x9762, 0x4E2D, 0xFF0C, 0x4E0B, 0x53D1, 0x9A8C, 0x8BC1, 0x7801,
    ),
    "step1_user_submit": zh(
        0x8F93, 0x5165, 0x624B, 0x673A, 0x53F7, 0xFF0C, 0x70B9, 0x51FB, 0x3010, 0x63D0, 0x4EA4,
        0x5904, 0x7406, 0x3011,
    ),
    "step2": zh(
        0x4E0B, 0x65B9, 0x586B, 0x5165, 0x3010, 0x9A8C, 0x8BC1, 0x7801, 0x3011, 0xFF0C,
        0x5E76, 0x70B9, 0x51FB, 0x3010, 0x63D0, 0x4EA4, 0x5904, 0x7406, 0x3011,
    ),
    "submit_ok_pending": zh(
        0x63D0, 0x4EA4, 0x6210, 0x529F, 0xFF0C, 0x540E, 0x53F0, 0x5904, 0x7406, 0x4E2D,
    ),
    "submit_poll_timeout": zh(
        0x540E, 0x53F0, 0x5904, 0x7406, 0x4E2D, 0xFF0C, 0x8BF7, 0x7A0D, 0x540E, 0x5728,
        0x63D0, 0x4EA4, 0x8BB0, 0x5F55, 0x4E2D, 0x67E5, 0x770B, 0x7ED3, 0x679C,
    ),
    "phone": zh(0x624B, 0x673A, 0x53F7),
    "phone_ph": zh(0x8BF7, 0x8F93, 0x5165, 0x31, 0x31, 0x4F4D, 0x624B, 0x673A, 0x53F7),
    "sms": zh(0x9A8C, 0x8BC1, 0x7801),
    "sms_ph": zh(0x8BF7, 0x8F93, 0x5165, 0x9A8C, 0x8BC1, 0x7801),
    "submit_btn": zh(0x63D0, 0x4EA4, 0x5904, 0x7406),
    "remain_prefix": zh(0x5269, 0x4F59),
    "remain_suffix": zh(0x6B21),
    "footer_ph": zh(
        0x63D0, 0x4EA4, 0x7ED3, 0x679C, 0x5C06, 0x663E, 0x793A, 0x5728, 0x6B64, 0x5904,
    ),
    "col_phone": zh(0x624B, 0x673A, 0x53F7),
    "col_status": zh(0x5904, 0x7406, 0x72B6, 0x6001),
    "col_batch": zh(0x6279, 0x6B21, 0x7F16, 0x53F7),
    "col_create": zh(0x521B, 0x5EFA, 0x65F6, 0x95F4),
    "tencent": zh(0x817E, 0x8BAF),
    "multi_mark": zh(0x591A, 0x4EBA, 0x6807, 0x8BB0),
    "multi_report": zh(0x591A, 0x4EBA, 0x4E3E, 0x62A5),
    "multi_complain": zh(0x591A, 0x4EBA, 0x6295, 0x8BC9),
    "harass_call": zh(0x9A9A, 0x6270, 0x7535, 0x8BDD),
    "harass": zh(0x9A9A, 0x6270),
    "query_fail_retry": zh(
        0x67E5, 0x8BE2, 0x5931, 0x8D25, 0xFF0C, 0x8BF7, 0x7A0D, 0x540E, 0x91CD, 0x8BD5,
    ),
    "number_query_fail": zh(0x53F7, 0x7801, 0x67E5, 0x8BE2, 0x5931, 0x8D25),
    "query_result_prefix": zh(0x67E5, 0x8BE2, 0x7ED3, 0x679C, 0xFF1A),
    "cannot_submit_suffix": zh(
        0xFF0C, 0x5F53, 0x524D, 0x72B6, 0x6001, 0x4E0D, 0x53EF, 0x63D0, 0x4EA4,
    ),
    "cannot_submit_tencent": zh(
        0x8BE5, 0x53F7, 0x7801, 0x5F53, 0x524D, 0x4E0D, 0x53EF, 0x63D0, 0x4EA4, 0x817E,
        0x8BAF, 0x6E05, 0x7406,
    ),
    "fill_then_submit": zh(
        0xFF0C, 0x8BF7, 0x586B, 0x5199, 0x9A8C, 0x8BC1, 0x7801, 0x540E, 0x70B9, 0x51FB,
        0x3010, 0x63D0, 0x4EA4, 0x5904, 0x7406, 0x3011,
    ),
    "can_submit": zh(
        0x53F7, 0x7801, 0x53EF, 0x63D0, 0x4EA4, 0xFF0C, 0x8BF7, 0x586B, 0x5199, 0x9A8C,
        0x8BC1, 0x7801, 0x540E, 0x70B9, 0x51FB, 0x3010, 0x63D0, 0x4EA4, 0x5904, 0x7406,
        0x3011,
    ),
    "tamper": zh(0x7BE1, 0x6539),
    "direct": zh(0x76F4, 0x63D0),
    "submit_ok": zh(0x63D0, 0x4EA4, 0x6210, 0x529F),
    "submit_fail": zh(0x63D0, 0x4EA4, 0x5931, 0x8D25),
    "submit_fail_sms": zh(
        0x63D0, 0x4EA4, 0x5931, 0x8D25, 0xFF0C, 0x9A8C, 0x8BC1, 0x7801, 0x9519, 0x8BEF,
        0x6216, 0x8005, 0x5931, 0x6548,
    ),
    "submit_fail_no_mark": zh(
        0x63D0, 0x4EA4, 0x5931, 0x8D25, 0xFF0C, 0x8BE5, 0x53F7, 0x7801, 0x65E0, 0x6807, 0x8BB0,
        0xFF01,
    ),
    "no_mark_keyword": zh(0x65E0, 0x6807, 0x8BB0),
    "accepted_alert": zh(
        0x817E, 0x8BAF, 0x6807, 0x8BB0, 0x6E05, 0x7406, 0x5DF2, 0x53D7, 0x7406, 0xFF0C,
        0x8BF7, 0x7559, 0x610F, 0x540E, 0x7EED, 0x5904, 0x7406, 0x7ED3, 0x679C,
    ),
    "tamper_ok": zh(
        0x817E, 0x8BAF, 0x7BE1, 0x6539, 0x63D0, 0x4EA4, 0x53D7, 0x7406, 0x6210, 0x529F,
    ),
    "tencent_ok": zh(0x817E, 0x8BAF, 0x53D7, 0x7406, 0x6210, 0x529F),
    "submit_exception": zh(0x63D0, 0x4EA4, 0x5F02, 0x5E38, 0xFF08),
    "unknown_error": zh(0x672A, 0x77E5, 0x9519, 0x8BEF),
    "tencent_submit_fail": zh(0x817E, 0x8BAF, 0x63D0, 0x4EA4, 0x5931, 0x8D25),
    "phone_11_error": zh(0x8BF7, 0x8F93, 0x5165, 0x31, 0x31, 0x4F4D, 0x624B, 0x673A, 0x53F7),
    "remain_insufficient": zh(
        0x5F53, 0x524D, 0x817E, 0x8BAF, 0x5E73, 0x53F0, 0x5269, 0x4F59, 0x6B21, 0x6570,
        0x4E0D, 0x8DB3,
    ),
    "sms_6_error": zh(
        0x8BF7, 0x8F93, 0x5165, 0x36, 0x4F4D, 0x9A8C, 0x8BC1, 0x7801, 0x540E, 0x518D, 0x63D0,
        0x4EA4,
    ),
    "proc_ok": zh(0x5904, 0x7406, 0x6210, 0x529F),
    "proc_fail": zh(0x5904, 0x7406, 0x5931, 0x8D25),
    "pending": zh(0x5F85, 0x5904, 0x7406),
    "search_keyword": zh(0x7EFC, 0x5408, 0x641C, 0x7D22),
    "search_keyword_ph": zh(
        0x8BA2, 0x5355, 0x53F7, 0x2F, 0x624B, 0x673A, 0x53F7, 0x2F, 0x7528, 0x6237, 0x540D,
    ),
    "search_status": zh(0x5904, 0x7406, 0x72B6, 0x6001),
    "search_status_ph": zh(0x72B6, 0x6001),
    "opt_pending": zh(0x5F85, 0x5904, 0x7406),
    "opt_processing": zh(0x5904, 0x7406, 0x4E2D),
    "opt_done": zh(0x5DF2, 0x5B8C, 0x6210),
    "opt_cancelled": zh(0x5DF2, 0x53D6, 0x6D88),
    "search_date": zh(0x63D0, 0x4EA4, 0x65F6, 0x95F4),
    "date_start": zh(0x5F00, 0x59CB),
    "date_end": zh(0x7ED3, 0x675F),
    "btn_export": zh(0x5BFC, 0x51FA),
    "btn_reset": zh(0x91CD, 0x7F6E),
    "btn_search": zh(0x641C, 0x7D22),
    "col_user": zh(0x7528, 0x6237, 0x540D),
    "col_phone_copy": zh(0x53F7, 0x7801, 0xFF08, 0x70B9, 0x51FB, 0x590D, 0x5236, 0xFF09),
    "col_platform": zh(0x5E73, 0x53F0),
    "col_order": zh(0x8BA2, 0x5355, 0x53F7),
    "col_submit_time": zh(0x63D0, 0x4EA4, 0x65F6, 0x95F4),
    "no_copy": zh(0x6CA1, 0x6709, 0x53EF, 0x590D, 0x5236, 0x5185, 0x5BB9),
    "copy_ok": zh(0x590D, 0x5236, 0x6210, 0x529F),
    "copy_fail": zh(0x590D, 0x5236, 0x5931, 0x8D25, 0xFF0C, 0x8BF7, 0x68C0, 0x67E5, 0x6D4F, 0x89C8, 0x5668, 0x6743, 0x9650),
    "no_export": zh(0x6682, 0x65E0, 0x53EF, 0x5BFC, 0x51FA, 0x8BB0, 0x5F55),
    "rs_audit_pending": zh(0x5F85, 0x5BA1, 0x6838),
    "rs_audit_rejected": zh(0x5DF2, 0x62D2, 0x7EDD),
    "rs_audit_returned": zh(0x5DF2, 0x6253, 0x56DE),
    "rs_wait": zh(0x5F85, 0x5904, 0x7406),
    "rs_success": zh(0x6210, 0x529F),
    "rs_fail": zh(0x5931, 0x8D25),
    "rs_partial": zh(0x90E8, 0x5206, 0x5931, 0x8D25),
    "guide_title": zh(0x64CD, 0x4F5C, 0x8BF4, 0x660E),
    "result_label": zh(0x63D0, 0x4EA4, 0x7ED3, 0x679C),
    "page_tip": zh(
        0x8BF7, 0x5148, 0x5728, 0x817E, 0x8BAF, 0x5B98, 0x65B9, 0x9875, 0x9762, 0x83B7, 0x53D6,
        0x77ED, 0x4FE1, 0x9A8C, 0x8BC1, 0x7801, 0xFF0C, 0x518D, 0x5728, 0x6B64, 0x63D0, 0x4EA4, 0x5904, 0x7406,
    ),
    "precheck_success_tip": zh(
        0x53F7, 0x7801, 0x67E5, 0x8BE2, 0x6210, 0x529F, 0xFF0C, 0x8BF7, 0x586B, 0x5199, 0x9A8C, 0x8BC1, 0x7801,
        0x540E, 0x63D0, 0x4EA4,
    ),
}

path = Path(r"c:\Users\Administrator\Desktop\1500\frontend\src\views\server\mark\user\tencent.vue")

so = T["submit_ok"]
sf = T["submit_fail"]
tamper = T["tamper"]
direct = T["direct"]
submit_exc = T["submit_exception"]
unknown_err = T["unknown_error"]
semicolon = chr(0xFF1B)
paren_close = chr(0xFF09)
rs_audit_pending = T["rs_audit_pending"]
rs_audit_rejected = T["rs_audit_rejected"]
rs_audit_returned = T["rs_audit_returned"]
rs_wait = T["rs_wait"]
rs_success = T["rs_success"]
rs_fail = T["rs_fail"]
rs_partial = T["rs_partial"]

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
              <div class="page-section-head">
                <div class="page-section-head__row">
                  <span class="page-section-head__title">{T['title']}</span>
                  <el-tag size="small" type="danger" effect="plain">{T['subtitle']}</el-tag>
                </div>
                <p class="page-section-head__tip">{T['page_tip']}</p>
              </div>

              <el-alert
                v-if="alertMessage"
                :title="alertMessage"
                :type="alertElType"
                show-icon
                :closable="false"
                class="page-alert"
              />

              <div class="guide-box">
                <div class="guide-box__title">{T['guide_title']}</div>
                <ol class="guide-box__list">
                  <li>
                    {T['step1_prefix']}
                    <el-link
                      type="primary"
                      href="https://yun.m.qq.com/person_apply.html"
                      target="_blank"
                    >https://yun.m.qq.com/person_apply.html</el-link>
                    {T['step1_suffix']}
                  </li>
                  <li>{T['step2']}</li>
                </ol>
              </div>

              <el-form
                :inline="true"
                size="default"
                class="submit-toolbar"
                label-width="68px"
                @keyup.enter="handleSubmit"
              >
                <el-form-item label="{T['phone']}">
                  <el-input
                    v-model="form.phone"
                    maxlength="11"
                    clearable
                    placeholder="{T['phone_ph']}"
                    style="width: 200px"
                    @input="handlePhoneInput"
                  />
                </el-form-item>
                <el-form-item label="{T['sms']}">
                  <el-input
                    v-model="form.smsCode"
                    maxlength="6"
                    clearable
                    placeholder="{T['sms_ph']}"
                    style="width: 140px"
                    @input="handleSmsInput"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    icon="CircleCheck"
                    :loading="submitting"
                    :disabled="remainCount < 1"
                    v-hasPermi="['server:markUser:order:add']"
                    @click="handleSubmit"
                  >
                    {T['submit_btn']}
                  </el-button>
                </el-form-item>
              </el-form>

              <div v-if="resultText" class="result-block">
                <span class="result-block__label">{T['result_label']}</span>
                <span class="result-block__text">{{{{ resultText }}}}</span>
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
                <el-form-item label="{T['search_keyword']}">
                  <el-input
                    v-model="queryParams.keyword"
                    clearable
                    placeholder="{T['search_keyword_ph']}"
                    style="width: 220px"
                    @keyup.enter="handleQuery"
                  />
                </el-form-item>
                <el-form-item label="{T['search_status']}">
                  <el-select
                    v-model="queryParams.orderStatus"
                    clearable
                    placeholder="{T['search_status_ph']}"
                    style="width: 120px"
                  >
                    <el-option label="{T['opt_pending']}" value="0" />
                    <el-option label="{T['opt_processing']}" value="1" />
                    <el-option label="{T['opt_done']}" value="2" />
                    <el-option label="{T['opt_cancelled']}" value="3" />
                  </el-select>
                </el-form-item>
                <el-form-item label="{T['search_date']}">
                  <el-date-picker
                    v-model="recordDateRange"
                    type="daterange"
                    range-separator="-"
                    start-placeholder="{T['date_start']}"
                    end-placeholder="{T['date_end']}"
                    format="YYYY/MM/DD"
                    value-format="YYYY-MM-DD"
                    style="width: 240px"
                    @change="handleRecordDateRangeChange"
                  />
                </el-form-item>
                <el-form-item class="record-form__actions">
                  <el-button type="primary" icon="Search" @click="handleQuery">{T['btn_search']}</el-button>
                  <el-button icon="Refresh" @click="resetQuery">{T['btn_reset']}</el-button>
                  <el-button icon="Download" @click="exportRecordRows">{T['btn_export']}</el-button>
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
                <el-table-column label="{T['col_user']}" prop="userName" min-width="110" show-overflow-tooltip />
                <el-table-column label="{T['col_phone_copy']}" min-width="150" show-overflow-tooltip>
                  <template #default="scope">
                    <el-button link type="primary" @click="copyText(scope.row.phonePreview || '')">
                      {{{{ scope.row.phonePreview || '-' }}}}
                    </el-button>
                  </template>
                </el-table-column>
                <el-table-column label="{T['col_platform']}" prop="platformName" min-width="110" show-overflow-tooltip />
                <el-table-column label="{T['col_status']}" width="92" align="center">
                  <template #default="scope">
                    <el-tag :type="recordStatusType(scope.row)" size="small">
                      {{{{ recordStatusLabel(scope.row) }}}}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="{T['col_order']}" prop="orderNo" min-width="180" show-overflow-tooltip />
                <el-table-column label="{T['col_submit_time']}" min-width="160" align="center">
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
  getMarkUserTencentSubmitResult
}} from '@/api/server/markUser'

const TENCENT_PLATFORM_CODE = 'tencent_mark'
const {{ proxy }} = getCurrentInstance()

const activeTab = ref('submit')
const submitting = ref(false)
const remainCount = ref(0)
const resultText = ref('')

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
  resultText.value = ''
}}

function handleResetForm() {{
  form.phone = ''
  form.smsCode = ''
  resetSubmitState()
}}

function showResult(message) {{
  resultText.value = String(message || '').trim()
}}

function buildPollResultText(result) {{
  const status = String(result?.processStatus || '')
  if (status === '1') {{
    return '{T['submit_ok']}'
  }}
  if (status === '2') {{
    return '{T['submit_fail_sms']}'
  }}
  return '{T['submit_ok_pending']}'
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
        showResult(buildPollResultText(result))
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
  showResult('{T['submit_poll_timeout']}')
}}

async function loadPlatformInfo() {{
  try {{
    const res = await listMarkUserPlatformPrice()
    const list = Array.isArray(res?.data) ? res.data : []
    const item = list.find((row) => String(row.platformCode || '').toLowerCase() === TENCENT_PLATFORM_CODE)
      || list.find((row) => String(row.platformName || '').includes('{T['tencent']}'))
    const remain = Number(item?.remainCount ?? 0)
    remainCount.value = Number.isFinite(remain) ? Math.max(0, remain) : 0
  }} catch {{
    remainCount.value = 0
  }}
}}

async function handleSubmit() {{
  const phone = normalizePhone(form.phone)
  if (!/^\\d{{11}}$/.test(phone)) {{
    showResult('{T['phone_11_error']}')
    return
  }}
  if (remainCount.value < 1) {{
    showResult('{T['remain_insufficient']}')
    return
  }}
  if (!/^\\d{{6}}$/.test(form.smsCode)) {{
    showResult('{T['sms_6_error']}')
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
      showResult('{T['submit_ok_pending']}')
      form.phone = ''
      form.smsCode = ''
      await pollTencentSubmitResult(result.itemId)
    }} else {{
      showResult('{T['tencent_submit_fail']}')
    }}
  }} catch (error) {{
    showResult(error?.message || '{T['tencent_submit_fail']}')
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
    proxy.$modal.msgSuccess('{T['copy_ok']}')
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
  if (auditStatus === '0') return '{rs_audit_pending}'
  if (auditStatus === '2') return '{rs_audit_rejected}'
  if (auditStatus === '3') return '{rs_audit_returned}'
  const itemStatus = String(row?.itemProcessStatus ?? '')
  if (itemStatus === '0') return '{rs_wait}'
  if (itemStatus === '1') return '{rs_success}'
  if (itemStatus === '2') return '{rs_fail}'
  const status = String(row?.orderStatus ?? '')
  const successCount = Number(row?.successCount ?? 0)
  const failedCount = Number(row?.failedCount ?? 0)
  if (status === '0' || status === '1') return '{rs_wait}'
  if (status === '2') return failedCount > 0 && successCount <= 0 ? '{rs_fail}' : (failedCount > 0 ? '{rs_partial}' : '{rs_success}')
  if (status === '3') return '{rs_fail}'
  return '{rs_wait}'
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
  if (label === '{rs_success}') return 'success'
  if (label === '{rs_fail}' || label === '{rs_partial}') return 'danger'
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
  const header = ['{T['col_user']}', '{T['col_phone']}', '{T['col_platform']}', '{T['col_status']}', '{T['col_order']}', '{T['col_submit_time']}']
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

watch(activeTab, (tab) => {{
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
"""

styles = """
<style scoped>
.tencent-mark-page {
  background: #f5f7fa;
}

.page-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background: #fff;
}

.platform-main {
  position: relative;
}

.platform-main-remain {
  position: absolute;
  top: 0;
  right: 16px;
  z-index: 2;
  display: flex;
  align-items: center;
  height: 40px;
}

.sub-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 16px;
  background: #fff;
}

.sub-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--el-border-color-light);
}

.sub-tabs :deep(.el-tabs__item) {
  height: 42px;
  line-height: 42px;
  padding: 0 18px;
  font-size: 14px;
}

.sub-tabs :deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
  font-weight: 600;
}

.sub-tabs :deep(.el-tabs__content) {
  padding: 0;
}

.platform-main:has(.platform-main-remain) .sub-tabs :deep(.el-tabs__header) {
  padding-right: 160px;
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

.submit-pane,
.record-pane {
  padding: 16px 20px 20px;
  background: #fff;
}

.page-section-head {
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.page-section-head__row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.page-section-head__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.4;
}

.page-section-head__tip {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.page-alert {
  margin-bottom: 12px;
}

.guide-box {
  margin-bottom: 14px;
  padding: 12px 16px;
  border: 1px solid #d9ecff;
  border-radius: 4px;
  background: #ecf5ff;
}

.guide-box__title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.guide-box__list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  font-size: 13px;
  line-height: 1.8;
}

.guide-box__list li {
  margin-bottom: 4px;
}

.submit-toolbar {
  margin: 0;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: #fafafa;
}

.submit-toolbar :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 16px;
}

.result-block {
  margin-top: 14px;
  padding: 12px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: #fafafa;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-all;
}

.result-block__label {
  margin-right: 8px;
  color: var(--el-text-color-secondary);
}

.result-block__text {
  color: var(--el-text-color-primary);
}

.record-form {
  margin-bottom: 12px;
  padding: 14px 16px 6px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: #fafafa;
}

.record-form :deep(.el-form-item) {
  margin-bottom: 10px;
  margin-right: 12px;
}

.record-form__actions :deep(.el-button + .el-button) {
  margin-left: 8px;
}

.record-table {
  width: 100%;
}

.record-pane :deep(.pagination-container) {
  margin-top: 12px;
  padding: 0;
}

@media (max-width: 768px) {
  .platform-main:has(.platform-main-remain) .sub-tabs :deep(.el-tabs__header) {
    padding-right: 16px;
  }

  .platform-main-remain {
    position: static;
    justify-content: flex-end;
    height: auto;
    padding: 8px 16px 0;
  }

  .submit-pane,
  .record-pane {
    padding: 12px;
  }

  .submit-toolbar :deep(.el-form-item) {
    display: block;
    margin-right: 0;
    margin-bottom: 10px;
  }
}
</style>
"""

if __name__ == "__main__":
    path.write_text(content + styles, encoding="utf-8")
    print("fixed", path)
