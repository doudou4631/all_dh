<script setup lang="ts">
import { ref, onMounted, computed, nextTick, onUnmounted } from 'vue'
import { listNotice } from '@/api/system/notice' // 引入公告API
import { ElIcon } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'

// 公告列表数据
const notices = ref([])
// 是否显示通知栏
const showNotice = ref(false)
// 滚动容器的位置
const scrollPosition = ref(0)
// 容器宽度
const containerWidth = ref(0)
// 内容总宽度
const contentWidth = ref(0)
// 滚动动画ID
const animationId = ref(null)
// 是否正在滚动
const isScrolling = ref(false)

// 获取公告列表
const getNotices = async () => {
  try {
    const response = await listNotice({ status: '0', pageNum: 1, pageSize: 10 }) // 只获取已发布的公告
    if (response.code === 200) {
      notices.value = response.rows
      showNotice.value = notices.value.length > 0
      // 等DOM更新后计算宽度
      nextTick(() => {
        calculateWidths()
        startScroll()
      })
    }
  } catch (error) {
    console.error('获取公告列表失败:', error)
  }
}

// 计算容器和内容宽度
const calculateWidths = () => {
  const container = document.querySelector('.notice-marquee-container')
  const content = document.querySelector('.notice-marquee-content')
  if (container && content) {
    containerWidth.value = container.offsetWidth
    contentWidth.value = content.offsetWidth
    // 设置CSS变量用于动画
    const wrapper = document.querySelector('.notice-marquee-wrapper')
    if (wrapper) {
      wrapper.style.setProperty('--content-width', `${contentWidth.value}px`)
    }
  }
}

// 开始滚动
const startScroll = () => {
  if (contentWidth.value <= containerWidth.value) return // 内容不足时不滚动

  isScrolling.value = true
  const scrollSpeed = 1 // 滚动速度（像素/帧）

  const animate = () => {
    // 持续滚动
    scrollPosition.value += scrollSpeed

    // 当滚动距离超过内容宽度时，重置到起始位置，实现无缝滚动
    if (scrollPosition.value >= contentWidth.value) {
      scrollPosition.value = 0
    }

    animationId.value = requestAnimationFrame(animate)
  }

  animate()
}

// 暂停滚动
const stopScroll = () => {
  if (animationId.value) {
    cancelAnimationFrame(animationId.value)
    animationId.value = null
    isScrolling.value = false
  }
}

// 去除HTML标签函数
const stripHtml = (html) => {
  if (!html) return ''
  const temp = document.createElement('div')
  temp.innerHTML = html
  return temp.textContent || temp.innerText || ''
}

// 组合所有公告内容
const combinedContent = computed(() => {
  return notices.value.map(notice =>
    `<span class="notice-item"><span class="notice-title">${notice.noticeTitle}</span><span class="notice-content">${stripHtml(notice.noticeContent)}</span></span>`
  ).join('')
})

// 生命周期钩子：组件挂载后获取公告数据
onMounted(() => {
  getNotices()

  // 监听窗口大小变化，重新计算宽度
  window.addEventListener('resize', calculateWidths)
})

// 组件卸载时清理
onUnmounted(() => {
  stopScroll()
  window.removeEventListener('resize', calculateWidths)
})


</script>

<template>
  <div v-if="showNotice" class="notice-marquee-container" ref="containerRef">
    <div class="notice-marquee-wrapper">
      <el-icon class="notice-icon">
        <Bell />
      </el-icon>
      <div class="notice-marquee-content" v-html="combinedContent"></div>
      <!-- 复制一份内容用于无缝滚动 -->
      <div class="notice-marquee-content duplicate" v-html="combinedContent"></div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use "@/assets/styles/variables.module.scss";

.notice-marquee-container {
  background-color: #fff3cd; // 浅黄色背景
  border: 1px solid #ffeaa7;
  padding: 8px 0;
  width: 100%;
  overflow: hidden;
  position: relative;
  z-index: 10;
  height: 40px;
  display: flex;
  align-items: center;
}

.notice-marquee-wrapper {
  display: flex;
  align-items: center;
  height: 100%;
  white-space: nowrap;
  animation: marquee 60s linear infinite;
}

.notice-icon {
  color: #f39c12;
  margin: 0 8px 0 50px;
  font-size: 16px;
  flex-shrink: 0;
}

.notice-marquee-content {
  display: inline-flex;
  align-items: center;
}

.notice-marquee-content.duplicate {
  margin-left: 20px; // 与原内容之间的间距
}

.notice-item {
  display: inline-flex;
  align-items: center;
  margin-right: 200px;
  /* 设置每条内容之间的间距为200px */
}

/* 最后一条内容不需要右边距 */
.notice-item:last-child {
  margin-right: 0;
}

.notice-title {
  font-weight: bold;
  margin-right: 12px;
  color: #856404;
}

.notice-content {
  color: #856404;
}

// 跑马灯动画
@keyframes marquee {
  0% {
    transform: translateX(100%);
  }

  100% {
    transform: translateX(-100%);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .notice-icon {
    margin-left: 20px;
  }
}

@media (max-width: 480px) {
  .notice-title {
    display: none;
  }
}
</style>