<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { listNotice } from '@/api/system/notice'
import { ElIcon } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'

const notices = ref<any[]>([])
const showNotice = ref(false)

const getNotices = async () => {
  try {
    const response = await listNotice({ status: '0', pageNum: 1, pageSize: 10 })
    if (response.code === 200) {
      notices.value = response.rows || []
      showNotice.value = notices.value.length > 0
    }
  } catch (error) {
    console.error('获取公告列表失败:', error)
  }
}

function stripHtml(html: string) {
  if (!html) return ''
  const temp = document.createElement('div')
  temp.innerHTML = html
  return temp.textContent || temp.innerText || ''
}

const noticeText = computed(() => {
  return notices.value
    .map((notice) => `${notice.noticeTitle}：${stripHtml(notice.noticeContent)}`)
    .join('    ')
})

onMounted(() => {
  getNotices()
})
</script>

<template>
  <div v-if="showNotice" class="notice-marquee-container">
    <el-icon class="notice-icon">
      <Bell />
    </el-icon>
    <div class="notice-marquee-track">
      <span class="notice-marquee-text">{{ noticeText }}</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.notice-marquee-container {
  position: relative;
  height: 100%;
  width: 100%;
  overflow: hidden;
  background: transparent;
  display: flex;
  align-items: center;
  pointer-events: none;
}

.notice-icon {
  color: var(--el-color-warning);
  margin-right: 8px;
  font-size: 16px;
  flex-shrink: 0;
}

.notice-marquee-track {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
}

.notice-marquee-text {
  display: inline-block;
  padding-left: 100%;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1;
  animation: notice-marquee 28s linear infinite;
}

@keyframes notice-marquee {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-100%);
  }
}
</style>
