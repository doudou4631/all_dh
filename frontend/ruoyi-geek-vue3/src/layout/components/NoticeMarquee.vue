<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { listNotice } from '@/api/system/notice'

interface NoticeItem {
  noticeId: number | string
  noticeTitle: string
  noticeContent: string
}

const emit = defineEmits<{
  (event: 'visibility-change', visible: boolean): void
}>()

const notices = ref<NoticeItem[]>([])
const showNotice = ref(false)
const shouldScroll = ref(false)
const viewportRef = ref<HTMLElement>()
const groupRef = ref<HTMLElement>()
const animationDuration = ref(24)
const marqueeStart = ref(0)
let resizeObserver: ResizeObserver | undefined

const stripHtml = (html = '') => {
  const element = document.createElement('div')
  element.innerHTML = html
  return element.textContent || element.innerText || ''
}

const displayNotices = computed(() => notices.value.map((notice) => ({
  ...notice,
  noticeTitle: String(notice.noticeTitle || '').trim(),
  noticeContent: stripHtml(notice.noticeContent).trim()
})))

const measureTrack = () => {
  const viewport = viewportRef.value
  const group = groupRef.value
  if (!viewport || !group) return

  const contentWidth = group.scrollWidth
  marqueeStart.value = viewport.clientWidth
  shouldScroll.value = contentWidth > 0
  animationDuration.value = Math.max(12, Math.round((contentWidth + viewport.clientWidth) / 55))
}

const setVisibility = (visible: boolean) => {
  showNotice.value = visible
  emit('visibility-change', visible)
}

const getNotices = async () => {
  try {
    const response = await listNotice({ status: '0', pageNum: 1, pageSize: 10 })
    notices.value = Array.isArray(response.rows) ? response.rows : []
    setVisibility(notices.value.length > 0)
    await nextTick()
    measureTrack()
    resizeObserver?.disconnect()
    if (viewportRef.value) resizeObserver?.observe(viewportRef.value)
  } catch (error) {
    notices.value = []
    setVisibility(false)
    console.error('获取公告列表失败:', error)
  }
}

onMounted(() => {
  resizeObserver = new ResizeObserver(measureTrack)
  getNotices()
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  emit('visibility-change', false)
})
</script>

<template>
  <div v-if="showNotice" class="notice-bar" role="status" aria-live="polite">
    <div class="notice-label" title="通知公告">
      <el-icon><Bell /></el-icon>
      <span>公告</span>
    </div>
    <div ref="viewportRef" class="notice-viewport">
      <div
        class="notice-track"
        :class="{ 'is-scrolling': shouldScroll }"
        :style="{
          '--marquee-duration': `${animationDuration}s`,
          '--marquee-start': `${marqueeStart}px`
        }"
      >
        <div ref="groupRef" class="notice-group">
          <span v-for="notice in displayNotices" :key="notice.noticeId" class="notice-item">
            <strong>{{ notice.noticeTitle }}</strong>
            <span v-if="notice.noticeContent">{{ notice.noticeContent }}</span>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.notice-bar {
  box-sizing: border-box;
  display: flex;
  align-items: center;
  width: 100%;
  height: 40px;
  overflow: hidden;
  color: #475569;
  background: rgba(255, 255, 255, 0.66);
  backdrop-filter: blur(10px) saturate(120%);
  border-top: 1px solid rgba(255, 255, 255, 0.78);
  border-bottom: 1px solid rgba(148, 163, 184, 0.28);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
}

.notice-label {
  z-index: 1;
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  align-self: stretch;
  gap: 8px;
  padding: 0 16px;
  color: #1d4ed8;
  font-size: 14px;
  font-weight: 600;
  background: rgba(239, 246, 255, 0.76);
  border-right: 1px solid rgba(96, 165, 250, 0.32);

  .el-icon {
    width: 24px;
    height: 24px;
    color: #fff;
    font-size: 14px;
    background: #2563eb;
    border-radius: 6px;
    box-shadow: 0 2px 6px rgba(37, 99, 235, 0.22);
  }
}

.notice-viewport {
  min-width: 0;
  overflow: hidden;
  flex: 1;
}

.notice-track {
  display: flex;
  align-items: center;
  width: max-content;
  white-space: nowrap;

  &.is-scrolling {
    animation: notice-marquee var(--marquee-duration) linear infinite;
  }
}

.notice-viewport:hover .notice-track.is-scrolling {
  animation-play-state: paused;
}

.notice-group {
  box-sizing: border-box;
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 56px;
  min-width: max-content;
  padding-right: 56px;
}

.notice-item {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding-left: 12px;
  color: #64748b;
  font-size: 14px;
  line-height: 38px;
  border-left: 2px solid rgba(37, 99, 235, 0.7);

  strong {
    color: #1f2937;
    font-weight: 600;
  }
}

@keyframes notice-marquee {
  from {
    transform: translateX(var(--marquee-start));
  }

  to {
    transform: translateX(-100%);
  }
}

@media (max-width: 768px) {
  .notice-label {
    width: 40px;
    justify-content: center;
    padding: 0;

    span {
      display: none;
    }
  }

  .notice-group {
    gap: 36px;
    padding-right: 36px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .notice-track.is-scrolling {
    animation: none;
  }

  .notice-track {
    transform: translateX(0);
  }
}
</style>
