# -*- coding: utf-8 -*-
from pathlib import Path

T = {
    "title": "\u8ba2\u5355\u8be6\u60c5",
    "search": "\u7efc\u5408\u641c\u7d22",
    "searchPh": "\u8ba2\u5355\u53f7/\u624b\u673a\u53f7",
    "platform": "\u5e73\u53f0",
    "allPlatform": "\u5168\u90e8\u5e73\u53f0",
    "status": "\u5904\u7406\u72b6\u6001",
    "allStatus": "\u5168\u90e8\u72b6\u6001",
    "pending": "\u5f85\u5904\u7406",
    "processing": "\u5904\u7406\u4e2d",
    "done": "\u5df2\u5b8c\u6210",
    "cancelled": "\u5df2\u53d6\u6d88",
    "submitTime": "\u63d0\u4ea4\u65f6\u95f4",
    "start": "\u5f00\u59cb",
    "end": "\u7ed3\u675f",
    "searchBtn": "\u641c\u7d22",
    "resetBtn": "\u91cd\u7f6e",
    "exportBtn": "\u5bfc\u51fa",
    "index": "\u5e8f\u53f7",
    "phone": "\u53f7\u7801",
    "orderNo": "\u8ba2\u5355\u53f7",
    "action": "\u64cd\u4f5c",
    "detail": "\u8be6\u60c5",
    "close": "\u5173 \u95ed",
    "count": "\u660e\u7ec6\u6570\u91cf",
    "successFail": "\u6210\u529f/\u5931\u8d25",
    "deduct": "\u6263\u8d39\u79ef\u5206",
    "auditStatus": "\u5ba1\u6838\u72b6\u6001",
    "orderStatus": "\u8ba2\u5355\u72b6\u6001",
    "refund": "\u9000\u6b3e\u79ef\u5206",
    "remark": "\u5907\u6ce8",
    "unitPrice": "\u5355\u4ef7",
    "processResult": "\u5904\u7406\u7ed3\u679c",
    "processNote": "\u5904\u7406\u5907\u6ce8",
    "refunded": "\u662f\u5426\u9000\u6b3e",
    "processTime": "\u5904\u7406\u65f6\u95f4",
    "yes": "\u662f",
    "no": "\u5426",
    "noCopy": "\u6ca1\u6709\u53ef\u590d\u5236\u5185\u5bb9",
    "copied": "\u5df2\u590d\u5236",
    "copyFail": "\u590d\u5236\u5931\u8d25",
    "noOrder": "\u8ba2\u5355\u4e0d\u5b58\u5728",
    "noExport": "\u6682\u65e0\u53ef\u5bfc\u51fa\u6570\u636e",
    "auditPending": "\u5f85\u5ba1\u6838",
    "auditPass": "\u901a\u8fc7",
    "auditReject": "\u62d2\u7edd",
    "auditReturn": "\u6253\u56de",
    "rejected": "\u5df2\u62d2\u7edd",
    "returned": "\u5df2\u6253\u56de",
    "success": "\u6210\u529f",
    "fail": "\u5931\u8d25",
    "partialFail": "\u90e8\u5206\u5931\u8d25",
}

path = Path(r"c:\Users\Administrator\Desktop\1500\frontend\src\views\server\mark\user\orderDetail.vue")

content = f'''<template>
  <div class="app-container mark-user-order-detail-page">
    <el-card shadow="never">
      <template #header>
        <span>{T["title"]}</span>
      </template>

      <el-form :model="queryParams" :inline="true" v-show="showSearch" label-width="84px" class="order-detail-query-form">
        <el-form-item label="{T["search"]}">
          <el-input
            v-model="queryParams.keyword"
            clearable
            placeholder="{T["searchPh"]}"
            style="width: 200px;"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="{T["platform"]}">
          <el-select v-model="queryParams.platformCode" clearable placeholder="{T["allPlatform"]}" style="width: 160px;">
            <el-option
              v-for="item in platformOptions"
              :key="item.platformCode"
              :label="item.platformName"
              :value="item.platformCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="{T["status"]}">
          <el-select v-model="queryParams.orderStatus" clearable placeholder="{T["allStatus"]}" style="width: 130px;">
            <el-option label="{T["pending"]}" value="0" />
            <el-option label="{T["processing"]}" value="1" />
            <el-option label="{T["done"]}" value="2" />
            <el-option label="{T["cancelled"]}" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="{T["submitTime"]}">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="-"
            start-placeholder="{T["start"]}"
            end-placeholder="{T["end"]}"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            style="width: 240px;"
            @change="handleDateRangeChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">{T["searchBtn"]}</el-button>
          <el-button icon="Refresh" @click="resetQuery">{T["resetBtn"]}</el-button>
          <el-button icon="Download" @click="exportRows">{T["exportBtn"]}</el-button>
        </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="orderList" :row-key="recordRowKey">
        <el-table-column label="{T["index"]}" type="index" width="56" align="center" />
        <el-table-column label="{T["platform"]}" prop="platformName" min-width="110" show-overflow-tooltip />
        <el-table-column label="{T["phone"]}" min-width="130" show-overflow-tooltip>
          <template #default="scope">
            <el-button link type="primary" @click="copyText(scope.row.phonePreview || '')">
              {{{{ scope.row.phonePreview || '-' }}}}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="{T["status"]}" width="92" align="center">
          <template #default="scope">
            <el-tag :type="recordStatusType(scope.row)" size="small">
              {{{{ recordStatusLabel(scope.row) }}}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="{T["orderNo"]}" prop="orderNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="{T["submitTime"]}" min-width="160" align="center">
          <template #default="scope">
            {{{{ formatDateTime(scope.row.createTime) }}}}
          </template>
        </el-table-column>
        <el-table-column label="{T["action"]}" width="88" align="center" fixed="right">
          <template #default="scope">
            <el-button
              link
              type="primary"
              v-hasPermi="['server:markUser:order:query']"
              @click="openDetail(scope.row)"
            >
              {T["detail"]}
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

    <el-dialog v-model="detailOpen" title="{T["title"]}" width="920px" append-to-body destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions v-if="detailOrder" :column="3" border size="small" class="mb12">
          <el-descriptions-item label="{T["orderNo"]}">{{{{ detailOrder.orderNo || '-' }}}}</el-descriptions-item>
          <el-descriptions-item label="{T["platform"]}">{{{{ detailOrder.platformName || '-' }}}}</el-descriptions-item>
          <el-descriptions-item label="{T["submitTime"]}">{{{{ formatDateTime(detailOrder.createTime) }}}}</el-descriptions-item>
          <el-descriptions-item label="{T["count"]}">{{{{ detailOrder.totalCount ?? 0 }}}}</el-descriptions-item>
          <el-descriptions-item label="{T["successFail"]}">{{{{ detailOrder.successCount ?? 0 }}}} / {{{{ detailOrder.failedCount ?? 0 }}}}</el-descriptions-item>
          <el-descriptions-item label="{T["deduct"]}">{{{{ detailOrder.totalAmount ?? 0 }}}}</el-descriptions-item>
          <el-descriptions-item label="{T["auditStatus"]}">{{{{ auditStatusLabel(detailOrder.auditStatus) }}}}</el-descriptions-item>
          <el-descriptions-item label="{T["orderStatus"]}">{{{{ orderStatusLabel(detailOrder.orderStatus) }}}}</el-descriptions-item>
          <el-descriptions-item label="{T["refund"]}">{{{{ detailOrder.refundAmount ?? 0 }}}}</el-descriptions-item>
          <el-descriptions-item label="{T["remark"]}" :span="3">{{{{ detailOrder.remark || '-' }}}}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="detailItems" border size="small" max-height="420" :row-class-name="detailRowClassName">
          <el-table-column label="{T["phone"]}" prop="phone" min-width="130" />
          <el-table-column label="{T["unitPrice"]}" prop="unitPrice" width="70" align="center" />
          <el-table-column label="{T["status"]}" width="92" align="center">
            <template #default="scope">
              <el-tag :type="itemStatusType(scope.row.processStatus)" size="small">
                {{{{ itemStatusLabel(scope.row.processStatus) }}}}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="{T["processResult"]}" prop="processResult" min-width="160" show-overflow-tooltip />
          <el-table-column label="{T["processNote"]}" prop="processNote" min-width="120" show-overflow-tooltip />
          <el-table-column label="{T["refunded"]}" width="88" align="center">
            <template #default="scope">
              {{{{ scope.row.refunded === '1' ? '{T["yes"]}' : '{T["no"]}' }}}}
            </template>
          </el-table-column>
          <el-table-column label="{T["processTime"]}" min-width="160" align="center">
            <template #default="scope">
              {{{{ formatDateTime(scope.row.processedTime) }}}}
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="detailOpen = false">{T["close"]}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MarkUserOrderDetail">
import {{ getMarkUserOrderDetail, listMarkUserOrder, listMarkUserPlatformPrice }} from '@/api/server/markUser'

const {{ proxy }} = getCurrentInstance()

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

const queryParams = reactive({{
  pageNum: 1,
  pageSize: 10,
  keyword: null,
  phone: null,
  platformCode: null,
  orderStatus: null,
  params: {{}}
}})

function formatDateTime(value) {{
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${{d.getFullYear()}}-${{p(d.getMonth() + 1)}}-${{p(d.getDate())}} ${{p(d.getHours())}}:${{p(d.getMinutes())}}:${{p(d.getSeconds())}}`
}}

function recordStatusLabel(row) {{
  const auditStatus = String(row?.auditStatus ?? '1')
  if (auditStatus === '0') return '{T["auditPending"]}'
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
  if (status === '2') return failedCount > 0 && successCount <= 0 ? '{T["fail"]}' : (failedCount > 0 ? '{T["partialFail"]}' : '{T["success"]}')
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
  if (label === '{T["fail"]}' || label === '{T["partialFail"]}') return 'danger'
  return 'warning'
}}

function itemStatusLabel(value) {{
  const status = String(value ?? '')
  if (status === '1') return '{T["success"]}'
  if (status === '2') return '{T["fail"]}'
  if (status === '3') return '{T["processing"]}'
  if (status === '0') return '{T["pending"]}'
  return status || '-'
}}

function itemStatusType(value) {{
  const status = String(value ?? '')
  if (status === '1') return 'success'
  if (status === '2') return 'danger'
  if (status === '3') return 'warning'
  if (status === '0') return 'warning'
  return 'info'
}}

function auditStatusLabel(value) {{
  const map = {{ '0': '{T["auditPending"]}', '1': '{T["auditPass"]}', '2': '{T["auditReject"]}', '3': '{T["auditReturn"]}' }}
  return map[String(value ?? '')] || '-'
}}

function orderStatusLabel(value) {{
  const map = {{ '0': '{T["pending"]}', '1': '{T["processing"]}', '2': '{T["done"]}', '3': '{T["cancelled"]}' }}
  return map[String(value ?? '')] || '-'
}}

function recordRowKey(row) {{
  return `${{row?.itemId ?? row?.id ?? ''}}-${{row?.phonePreview ?? ''}}`
}}

function normalizeKeyword() {{
  const keyword = String(queryParams.keyword || '').trim()
  queryParams.keyword = keyword || null
  if (keyword && /^\\d{{7,15}}$/.test(keyword)) {{
    queryParams.phone = keyword
    return
  }}
  queryParams.phone = null
}}

function handleDateRangeChange(value) {{
  if (Array.isArray(value) && value.length === 2) {{
    queryParams.params = {{
      beginTime: value[0],
      endTime: value[1]
    }}
    return
  }}
  queryParams.params = {{}}
}}

async function copyText(text) {{
  const value = String(text || '').trim()
  if (!value) {{
    proxy.$modal.msgWarning('{T["noCopy"]}')
    return
  }}
  try {{
    await navigator.clipboard.writeText(value)
    proxy.$modal.msgSuccess('{T["copied"]}')
  }} catch (error) {{
    proxy.$modal.msgError('{T["copyFail"]}')
  }}
}}

function getList() {{
  normalizeKeyword()
  loading.value = true
  return listMarkUserOrder(queryParams).then((res) => {{
    orderList.value = res.rows || []
    total.value = res.total || 0
  }}).finally(() => {{
    loading.value = false
  }})
}}

function loadPlatformOptions() {{
  return listMarkUserPlatformPrice().then((resp) => {{
    platformOptions.value = Array.isArray(resp?.data) ? resp.data : []
  }}).catch(() => {{
    platformOptions.value = []
  }})
}}

function handleQuery() {{
  queryParams.pageNum = 1
  getList()
}}

function resetQuery() {{
  queryParams.pageNum = 1
  queryParams.keyword = null
  queryParams.phone = null
  queryParams.platformCode = null
  queryParams.orderStatus = null
  queryParams.params = {{}}
  dateRange.value = []
  getList()
}}

function openDetail(row) {{
  const orderId = row?.id
  const itemId = row?.itemId
  if (!orderId) {{
    proxy.$modal.msgWarning('{T["noOrder"]}')
    return
  }}
  activeItemId.value = itemId || null
  detailOpen.value = true
  detailLoading.value = true
  detailOrder.value = null
  detailItems.value = []
  getMarkUserOrderDetail(orderId).then((res) => {{
    detailOrder.value = res?.data?.order || null
    detailItems.value = Array.isArray(res?.data?.items) ? res.data.items : []
  }}).finally(() => {{
    detailLoading.value = false
  }})
}}

function detailRowClassName({{ row }}) {{
  if (activeItemId.value && row?.id === activeItemId.value) {{
    return 'order-detail-row--active'
  }}
  return ''
}}

function exportRows() {{
  if (!orderList.value.length) {{
    proxy.$modal.msgWarning('{T["noExport"]}')
    return
  }}
  const header = ['{T["platform"]}', '{T["phone"]}', '{T["status"]}', '{T["orderNo"]}', '{T["submitTime"]}']
  const body = orderList.value.map((row) => [
    row.platformName || '',
    row.phonePreview || '',
    recordStatusLabel(row),
    row.orderNo || '',
    formatDateTime(row.createTime)
  ])
  const csv = [header, ...body]
    .map((line) => line.map((cell) => `"${{String(cell).replace(/"/g, '""')}}"`).join(','))
    .join('\\n')
  const blob = new Blob(['\\ufeff' + csv], {{ type: 'text/csv;charset=utf-8;' }})
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `mark-order-detail-${{Date.now()}}.csv`
  link.click()
  URL.revokeObjectURL(url)
}}

onMounted(() => {{
  loadPlatformOptions()
  getList()
}})
</script>

<style scoped>
.mark-user-order-detail-page .order-detail-query-form {{
  margin-bottom: 8px;
}}

.mb12 {{
  margin-bottom: 12px;
}}

:deep(.order-detail-row--active > td) {{
  background: #ecf5ff !important;
}}
</style>
'''

path.write_text(content, encoding='utf-8')
print('ok', path)
