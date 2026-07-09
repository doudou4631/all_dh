<template>
  <div class="app-container mark-user-notice-page">
    <el-card shadow="never">
      <template #header>
        <div class="notice-card-head">
          <span class="notice-card-head__title">我的消息</span>
          <div class="notice-card-head__actions">
            <el-tag type="danger" effect="plain" size="small">未读 {{ unreadCount }}</el-tag>
            <el-button
              type="primary"
              plain
              size="small"
              :disabled="unreadCount === 0"
              v-hasPermi="['server:markUser:notice:read']"
              @click="handleReadAll"
            >
              全部已读
            </el-button>
            <el-button size="small" icon="Refresh" @click="getList">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="notice-search-bar">
        <el-form :model="queryParams" :inline="true" size="small" class="notice-query-form">
          <el-form-item label="标题">
            <el-input
              v-model="queryParams.title"
              placeholder="标题关键词"
              clearable
              style="width: 180px;"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.readFlag" clearable placeholder="全部" style="width: 110px;">
              <el-option label="未读" value="0" />
              <el-option label="已读" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        v-loading="loading"
        :data="noticeList"
        :row-class-name="noticeRowClass"
        @row-click="openDetail"
      >
        <el-table-column label="状态" width="72" align="center">
          <template #default="scope">
            <span :class="['notice-dot', scope.row.readFlag === '1' ? 'notice-dot--read' : 'notice-dot--unread']" />
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="scope">
            <el-tag :type="noticeTypeTagType(scope.row.noticeType)" size="small">
              {{ noticeTypeLabel(scope.row.noticeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题" prop="title" min-width="160" show-overflow-tooltip>
          <template #default="scope">
            <span :class="{ 'notice-title--unread': scope.row.readFlag !== '1' }">
              {{ scope.row.title || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="内容摘要" min-width="260" show-overflow-tooltip>
          <template #default="scope">
            {{ contentPreview(scope.row.content) }}
          </template>
        </el-table-column>
        <el-table-column label="时间" width="168" align="center">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="72" align="center">
          <template #default="scope">
            <el-button link type="primary" @click.stop="openDetail(scope.row)">查看</el-button>
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

    <el-dialog v-model="detailOpen" title="消息详情" width="560px" append-to-body destroy-on-close>
      <div class="notice-detail">
        <div class="notice-detail__meta">
          <el-tag :type="noticeTypeTagType(detailData.noticeType)" size="small">
            {{ noticeTypeLabel(detailData.noticeType) }}
          </el-tag>
          <el-tag :type="detailData.readFlag === '1' ? 'info' : 'danger'" size="small" effect="plain">
            {{ detailData.readFlag === '1' ? '已读' : '未读' }}
          </el-tag>
          <span class="notice-detail__time">{{ formatDateTime(detailData.createTime) }}</span>
        </div>
        <h4 class="notice-detail__title">{{ detailData.title || '-' }}</h4>
        <div class="notice-detail__content">{{ detailData.content || '-' }}</div>
      </div>
      <template #footer>
        <el-button @click="detailOpen = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MarkUserNotice">
import {
  listMarkUserNotice,
  getMarkUserNoticeUnreadCount,
  getMarkUserNoticeDetail,
  readMarkUserNotice,
  readAllMarkUserNotice
} from '@/api/server/markUser'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const total = ref(0)
const unreadCount = ref(0)
const noticeList = ref([])
const detailOpen = ref(false)
const detailData = ref({})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: null,
  readFlag: null
})

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function noticeTypeLabel(value) {
  const map = {
    ORDER_AUDIT: '订单通知'
  }
  return map[value] || '系统消息'
}

function noticeTypeTagType(value) {
  return value === 'ORDER_AUDIT' ? 'warning' : 'info'
}

function contentPreview(value) {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  if (!text) return '-'
  return text.length > 80 ? `${text.slice(0, 80)}...` : text
}

function noticeRowClass({ row }) {
  return row?.readFlag === '1' ? '' : 'notice-row-unread'
}

function loadUnreadCount() {
  return getMarkUserNoticeUnreadCount().then((res) => {
    unreadCount.value = Number(res?.data ?? 0)
  }).catch(() => {
    unreadCount.value = 0
  })
}

function getList() {
  loading.value = true
  return listMarkUserNotice(queryParams).then((res) => {
    noticeList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
    loadUnreadCount()
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.title = null
  queryParams.readFlag = null
  handleQuery()
}

async function openDetail(row) {
  const noticeId = row?.id
  if (!noticeId) return
  try {
    const res = await getMarkUserNoticeDetail(noticeId)
    detailData.value = res.data || {}
    detailOpen.value = true
    if (row.readFlag !== '1') {
      await readMarkUserNotice(noticeId)
      row.readFlag = '1'
      loadUnreadCount()
    }
  } catch (error) {
    proxy.$modal.msgError(error?.message || '加载消息失败')
  }
}

function handleReadAll() {
  readAllMarkUserNotice().then(() => {
    proxy.$modal.msgSuccess('已全部标记为已读')
    getList()
  })
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.notice-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.notice-card-head__title {
  font-size: 15px;
  font-weight: 600;
}

.notice-card-head__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.notice-search-bar {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.notice-query-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 12px;
}

.notice-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.notice-dot--unread {
  background: var(--el-color-danger);
}

.notice-dot--read {
  background: var(--el-border-color);
}

.notice-title--unread {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.notice-detail__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.notice-detail__time {
  margin-left: auto;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.notice-detail__title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.notice-detail__content {
  padding: 12px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

:deep(.notice-row-unread) {
  background: #fef6f6;
}

:deep(.notice-row-unread:hover > td.el-table__cell) {
  background: #fdeeee !important;
}
</style>
