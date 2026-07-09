<template>
  <div class="app-container user-sys-notice-page">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <span class="page-title">系统公告</span>
      </template>

      <div v-if="noticeList.length" class="message-list">
        <div
          v-for="item in noticeList"
          :key="item.noticeId"
          class="message-item"
          @click="showNoticeDetail(item)"
        >
          <el-tag size="small" :type="item.noticeType === '1' ? 'danger' : 'success'">
            {{ item.noticeType === '1' ? '通知' : '公告' }}
          </el-tag>
          <div class="message-item__main">
            <div class="message-item__title">{{ item.noticeTitle }}</div>
          </div>
          <span class="message-item__time">{{ formatTime(item.createTime) }}</span>
        </div>
      </div>
      <el-empty v-else description="暂无公告" :image-size="64" />

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="currentNotice.noticeTitle" width="50%" destroy-on-close>
      <div class="notice-dialog-content" v-html="currentNotice.noticeContent"></div>
    </el-dialog>
  </div>
</template>

<script setup name="UserSysNotice" lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listNotice } from '@/api/system/notice'
import { parseTime } from '@/utils/ruoyi'
import { GeekResponseForList } from '@/types/request'

interface Notice {
  noticeId: number
  noticeTitle: string
  noticeType: string
  noticeContent: string
  status: string
  createBy: string
  createTime: string
}

const loading = ref(false)
const total = ref(0)
const noticeList = ref<Notice[]>([])
const dialogVisible = ref(false)
const currentNotice = ref<Notice>({} as Notice)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

function formatTime(value?: string) {
  if (!value) return '-'
  return (parseTime(value) as string) || '-'
}

async function getList() {
  loading.value = true
  try {
    const res: GeekResponseForList<Notice> = await listNotice({ ...queryParams })
    noticeList.value = res.rows || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取公告列表失败:', error)
    noticeList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function showNoticeDetail(notice: Notice) {
  currentNotice.value = notice
  dialogVisible.value = true
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.user-sys-notice-page {
  .page-title {
    font-size: 15px;
    font-weight: 600;
  }

  .message-list {
    display: flex;
    flex-direction: column;
  }

  .message-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
    cursor: pointer;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      opacity: 0.88;
    }
  }

  .message-item__main {
    flex: 1;
    min-width: 0;
  }

  .message-item__title {
    font-size: 14px;
    color: var(--el-text-color-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .message-item__time {
    flex-shrink: 0;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }

  .notice-dialog-content {
    line-height: 1.7;
    word-break: break-word;
  }
}
</style>
