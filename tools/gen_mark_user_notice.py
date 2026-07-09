# -*- coding: utf-8 -*-
"""Regenerate mark user notice page with UTF-8 Chinese."""
from pathlib import Path


def zh(*codes: int) -> str:
    return "".join(chr(c) for c in codes)


T = {
    "page_title": zh(0x6211, 0x7684, 0x6D88, 0x606F),
    "unread": zh(0x672A, 0x8BFB),
    "read_all": zh(0x5168, 0x90E8, 0x5DF2, 0x8BFB),
    "refresh": zh(0x5237, 0x65B0),
    "title_label": zh(0x6807, 0x9898),
    "title_ph": zh(0x6807, 0x9898, 0x5173, 0x952E, 0x8BCD),
    "status_label": zh(0x72B6, 0x6001),
    "all": zh(0x5168, 0x90E8),
    "unread_opt": zh(0x672A, 0x8BFB),
    "read_opt": zh(0x5DF2, 0x8BFB),
    "search": zh(0x641C, 0x7D22),
    "reset": zh(0x91CD, 0x7F6E),
    "col_status": zh(0x72B6, 0x6001),
    "col_type": zh(0x7C7B, 0x578B),
    "col_title": zh(0x6807, 0x9898),
    "col_summary": zh(0x5185, 0x5BB9, 0x6458, 0x8981),
    "col_time": zh(0x65F6, 0x95F4),
    "col_action": zh(0x64CD, 0x4F5C),
    "view": zh(0x67E5, 0x770B),
    "detail_title": zh(0x6D88, 0x606F, 0x8BE6, 0x60C5),
    "close": zh(0x5173, 0x95ED),
    "type_order": zh(0x8BA2, 0x5355, 0x901A, 0x77E5),
    "type_system": zh(0x7CFB, 0x7EDF, 0x6D88, 0x606F),
    "load_fail": zh(0x52A0, 0x8F7D, 0x6D88, 0x606F, 0x5931, 0x8D25),
    "read_all_ok": zh(0x5DF2, 0x5168, 0x90E8, 0x6807, 0x8BB0, 0x4E3A, 0x5DF2, 0x8BFB),
}

content = f"""<template>
  <div class="app-container mark-user-notice-page">
    <el-card shadow="never">
      <template #header>
        <div class="notice-card-head">
          <span class="notice-card-head__title">{T['page_title']}</span>
          <div class="notice-card-head__actions">
            <el-tag type="danger" effect="plain" size="small">{T['unread']} {{{{ unreadCount }}}}</el-tag>
            <el-button
              type="primary"
              plain
              size="small"
              :disabled="unreadCount === 0"
              v-hasPermi="['server:markUser:notice:read']"
              @click="handleReadAll"
            >
              {T['read_all']}
            </el-button>
            <el-button size="small" icon="Refresh" @click="getList">{T['refresh']}</el-button>
          </div>
        </div>
      </template>

      <div class="notice-search-bar">
        <el-form :model="queryParams" :inline="true" size="small" class="notice-query-form">
          <el-form-item label="{T['title_label']}">
            <el-input
              v-model="queryParams.title"
              placeholder="{T['title_ph']}"
              clearable
              style="width: 180px;"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="{T['status_label']}">
            <el-select v-model="queryParams.readFlag" clearable placeholder="{T['all']}" style="width: 110px;">
              <el-option label="{T['unread_opt']}" value="0" />
              <el-option label="{T['read_opt']}" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">{T['search']}</el-button>
            <el-button icon="Refresh" @click="resetQuery">{T['reset']}</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        v-loading="loading"
        :data="noticeList"
        :row-class-name="noticeRowClass"
        @row-click="openDetail"
      >
        <el-table-column label="{T['col_status']}" width="72" align="center">
          <template #default="scope">
            <span :class="['notice-dot', scope.row.readFlag === '1' ? 'notice-dot--read' : 'notice-dot--unread']" />
          </template>
        </el-table-column>
        <el-table-column label="{T['col_type']}" width="100" align="center">
          <template #default="scope">
            <el-tag :type="noticeTypeTagType(scope.row.noticeType)" size="small">
              {{{{ noticeTypeLabel(scope.row.noticeType) }}}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="{T['col_title']}" prop="title" min-width="160" show-overflow-tooltip>
          <template #default="scope">
            <span :class="{ 'notice-title--unread': scope.row.readFlag !== '1' }">
              {{{{ scope.row.title || '-' }}}}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="{T['col_summary']}" min-width="260" show-overflow-tooltip>
          <template #default="scope">
            {{{{ contentPreview(scope.row.content) }}}}
          </template>
        </el-table-column>
        <el-table-column label="{T['col_time']}" width="168" align="center">
          <template #default="scope">
            {{{{ formatDateTime(scope.row.createTime) }}}}
          </template>
        </el-table-column>
        <el-table-column label="{T['col_action']}" width="72" align="center">
          <template #default="scope">
            <el-button link type="primary" @click.stop="openDetail(scope.row)">{T['view']}</el-button>
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

    <el-dialog v-model="detailOpen" title="{T['detail_title']}" width="560px" append-to-body destroy-on-close>
      <div class="notice-detail">
        <div class="notice-detail__meta">
          <el-tag :type="noticeTypeTagType(detailData.noticeType)" size="small">
            {{{{ noticeTypeLabel(detailData.noticeType) }}}}
          </el-tag>
          <el-tag :type="detailData.readFlag === '1' ? 'info' : 'danger'" size="small" effect="plain">
            {{{{ detailData.readFlag === '1' ? '{T['read_opt']}' : '{T['unread_opt']}' }}}}
          </el-tag>
          <span class="notice-detail__time">{{{{ formatDateTime(detailData.createTime) }}}}</span>
        </div>
        <h4 class="notice-detail__title">{{{{ detailData.title || '-' }}}}</h4>
        <div class="notice-detail__content">{{{{ detailData.content || '-' }}}}</div>
      </div>
      <template #footer>
        <el-button @click="detailOpen = false">{T['close']}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MarkUserNotice">
import {{
  listMarkUserNotice,
  getMarkUserNoticeUnreadCount,
  getMarkUserNoticeDetail,
  readMarkUserNotice,
  readAllMarkUserNotice
}} from '@/api/server/markUser'

const {{ proxy }} = getCurrentInstance()

const loading = ref(false)
const total = ref(0)
const unreadCount = ref(0)
const noticeList = ref([])
const detailOpen = ref(false)
const detailData = ref({{}})

const queryParams = reactive({{
  pageNum: 1,
  pageSize: 10,
  title: null,
  readFlag: null
}})

function formatDateTime(value) {{
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${{d.getFullYear()}}-${{p(d.getMonth() + 1)}}-${{p(d.getDate())}} ${{p(d.getHours())}}:${{p(d.getMinutes())}}:${{p(d.getSeconds())}}`
}}

function noticeTypeLabel(value) {{
  const map = {{
    ORDER_AUDIT: '{T['type_order']}'
  }}
  return map[value] || '{T['type_system']}'
}}

function noticeTypeTagType(value) {{
  return value === 'ORDER_AUDIT' ? 'warning' : 'info'
}}

function contentPreview(value) {{
  const text = String(value || '').replace(/\\s+/g, ' ').trim()
  if (!text) return '-'
  return text.length > 80 ? `${{text.slice(0, 80)}}...` : text
}}

function noticeRowClass({{ row }}) {{
  return row?.readFlag === '1' ? '' : 'notice-row-unread'
}}

function loadUnreadCount() {{
  return getMarkUserNoticeUnreadCount().then((res) => {{
    unreadCount.value = Number(res?.data ?? 0)
  }}).catch(() => {{
    unreadCount.value = 0
  }})
}}

function getList() {{
  loading.value = true
  return listMarkUserNotice(queryParams).then((res) => {{
    noticeList.value = res.rows || []
    total.value = res.total || 0
  }}).finally(() => {{
    loading.value = false
    loadUnreadCount()
  }})
}}

function handleQuery() {{
  queryParams.pageNum = 1
  getList()
}}

function resetQuery() {{
  queryParams.title = null
  queryParams.readFlag = null
  handleQuery()
}}

async function openDetail(row) {{
  const noticeId = row?.id
  if (!noticeId) return
  try {{
    const res = await getMarkUserNoticeDetail(noticeId)
    detailData.value = res.data || {{}}
    detailOpen.value = true
    if (row.readFlag !== '1') {{
      await readMarkUserNotice(noticeId)
      row.readFlag = '1'
      loadUnreadCount()
    }}
  }} catch (error) {{
    proxy.$modal.msgError(error?.message || '{T['load_fail']}')
  }}
}}

function handleReadAll() {{
  readAllMarkUserNotice().then(() => {{
    proxy.$modal.msgSuccess('{T['read_all_ok']}')
    getList()
  }})
}}

onMounted(() => {{
  getList()
}})
</script>

<style scoped>
.notice-card-head {{
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}}

.notice-card-head__title {{
  font-size: 15px;
  font-weight: 600;
}}

.notice-card-head__actions {{
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}}

.notice-search-bar {{
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}}

.notice-query-form :deep(.el-form-item) {{
  margin-bottom: 0;
  margin-right: 12px;
}}

.notice-dot {{
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}}

.notice-dot--unread {{
  background: var(--el-color-danger);
}}

.notice-dot--read {{
  background: var(--el-border-color);
}}

.notice-title--unread {{
  font-weight: 600;
  color: var(--el-text-color-primary);
}}

.notice-detail__meta {{
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}}

.notice-detail__time {{
  margin-left: auto;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}}

.notice-detail__title {{
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}}

.notice-detail__content {{
  padding: 12px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}}

:deep(.notice-row-unread) {{
  background: #fef6f6;
}}

:deep(.notice-row-unread:hover > td.el-table__cell) {{
  background: #fdeeee !important;
}}
</style>
"""

path = Path(r"c:\Users\Administrator\Desktop\1500\frontend\src\views\server\mark\user\notice\index.vue")
path.write_text(content, encoding="utf-8")
print(f"written {path}")
