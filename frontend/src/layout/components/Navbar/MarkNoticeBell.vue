<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import auth from '@/plugins/auth'
import { getMarkUserNoticeUnreadCount } from '@/api/server/markUser'

const router = useRouter()
const unreadCount = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const visible = auth.hasPermi('server:markUser:notice:list')

async function loadUnreadCount() {
  if (!visible) {
    unreadCount.value = 0
    return
  }
  try {
    const res = await getMarkUserNoticeUnreadCount()
    unreadCount.value = Number(res?.data ?? 0)
  } catch {
    unreadCount.value = 0
  }
}

function goNoticePage() {
  router.push('/mark/userNotice')
}

onMounted(() => {
  loadUnreadCount()
  timer = setInterval(loadUnreadCount, 60000)
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<template>
  <el-tooltip v-if="visible" content="\u6211\u7684\u6d88\u606f" effect="dark" placement="bottom">
    <div class="mark-notice-bell right-menu-item hover-effect svg-menu-item" @click="goNoticePage">
      <el-badge :value="unreadCount" :hidden="unreadCount <= 0" :max="99">
        <el-icon :size="18"><Bell /></el-icon>
      </el-badge>
    </div>
  </el-tooltip>
</template>

<style scoped>
.mark-notice-bell {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
