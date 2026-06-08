<template>
  <div class="profile-sub-page">
    <a class="profile-sub-back" :href="resolveHref('/profile/')">‹ 返回个人中心</a>
    <h1 class="profile-sub-title">查询记录</h1>
    <div v-if="loading" class="profile-sub-empty">加载中...</div>
    <div v-else-if="!records.length" class="profile-sub-empty">暂无查询记录</div>
    <div v-else class="profile-record-list">
      <div v-for="(item, index) in records" :key="`${item.phone}-${item.time}-${index}`" class="profile-record-item">
        <div class="profile-record-head">
          <strong>{{ item.phone || '--' }}</strong>
          <span>{{ item.time || '--' }}</span>
        </div>
        <div class="profile-record-meta">类型：{{ item.type || item.queryType || '单号查询' }} · 标记平台：{{ normalizeMarked(item.marked ?? item.markedCount) }} 个</div>
        <div class="profile-record-meta">被标记平台：{{ renderMarkedPlatforms(item.markedPlatforms || item.platforms) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useMobileRuntimeConfig } from '@/runtime/mobileRuntimeConfig'
import { fetchQueryRecords } from '@/services/freeQueryBridge'
import { getToken, isLoggedIn } from '@/services/profileSession'
import { notify } from '@/services/bottomNavUi'

const { state, resolveHref, loadMobileRuntimeConfig } = useMobileRuntimeConfig()

const loading = ref(true)
const records = ref([])

const apiBase = computed(() => state.config.apiBase)

function normalizeMarked(value) {
  const count = Number(value)
  if (Number.isNaN(count) || count < 0) return 0
  return Math.round(count)
}

function renderMarkedPlatforms(value) {
  if (!Array.isArray(value) || !value.length) return '无'
  return value.map((item) => String(item || '').trim()).filter(Boolean).join('、') || '无'
}

onMounted(() => {
  loadMobileRuntimeConfig().finally(async () => {
    if (!isLoggedIn()) {
      loading.value = false
      notify('请先登录')
      setTimeout(() => {
        window.location.replace(resolveHref('/profile/'))
      }, 400)
      return
    }

    loading.value = true
    try {
      records.value = await fetchQueryRecords(getToken(), apiBase.value)
    } catch (error) {
      notify(error?.message || '获取查询记录失败')
      records.value = []
    } finally {
      loading.value = false
    }
  })
})
</script>
