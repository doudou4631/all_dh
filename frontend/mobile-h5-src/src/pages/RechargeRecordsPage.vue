<template>
  <div class="profile-sub-page">
    <a class="profile-sub-back" :href="resolveHref('/profile/')">‹ 返回个人中心</a>
    <h1 class="profile-sub-title">充值记录</h1>
    <div v-if="!records.length" class="profile-sub-empty">暂无充值记录</div>
    <div v-else class="profile-record-list">
      <div v-for="(item, index) in records" :key="`${item.title}-${item.time}-${index}`" class="profile-record-item">
        <div class="profile-record-head">
          <strong>{{ item.title || '--' }}</strong>
          <span>{{ item.amount || '--' }}</span>
        </div>
        <div class="profile-record-meta">{{ item.time || '--' }} · {{ item.remark || '' }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useMobileRuntimeConfig } from '@/runtime/mobileRuntimeConfig'
import { getRechargeRecords, isLoggedIn } from '@/services/profileSession'
import { notify } from '@/services/bottomNavUi'

const { resolveHref, loadMobileRuntimeConfig } = useMobileRuntimeConfig()
const records = ref([])

onMounted(() => {
  loadMobileRuntimeConfig().finally(() => {
    if (!isLoggedIn()) {
      notify('请先登录')
      setTimeout(() => {
        window.location.replace(resolveHref('/profile/'))
      }, 400)
      return
    }
    records.value = getRechargeRecords()
  })
})
</script>
