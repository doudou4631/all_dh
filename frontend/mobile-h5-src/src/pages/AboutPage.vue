<template>
  <div class="sub-page">
    <a class="sub-back" :href="resolveHref('/profile/')">‹ 返回个人中心</a>
    <div class="sub-card">
      <h1>关于我们</h1>
      <p>我们专注于号码标记查询与清除服务，帮助用户提升号码接听效率。</p>
      <p>支持查询泰迪熊、腾讯、360、百度、搜狗、移动高频、联通管家、电话邦等主流平台标记情况。</p>
      <p>如需开通批量查询或企业合作，请联系在线客服。</p>
      <p>
        服务电话：
        <a :href="phoneHref" style="color: #3b71fe">{{ servicePhone }}</a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useMobileRuntimeConfig } from '@/runtime/mobileRuntimeConfig'

const { state, resolveHref, loadMobileRuntimeConfig } = useMobileRuntimeConfig()
const servicePhone = computed(() => String(state.config.servicePhone || '--'))
const phoneHref = computed(() => (servicePhone.value && servicePhone.value !== '--' ? `tel:${servicePhone.value}` : 'tel:'))

onMounted(() => {
  loadMobileRuntimeConfig()
})
</script>

<style scoped>
.sub-page {
  min-height: 100vh;
  background: #f3f6fb;
  padding: 16px 15px;
  box-sizing: border-box;
}

.sub-back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #3b71fe;
  text-decoration: none;
  font-size: 14px;
  margin-bottom: 16px;
}

.sub-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px 16px;
  box-shadow: 0 2px 12px rgba(15, 35, 80, 0.05);
}

.sub-card h1 {
  margin: 0 0 12px;
  font-size: 1.125rem;
  color: #111;
}

.sub-card p {
  margin: 0 0 12px;
  font-size: 14px;
  line-height: 1.7;
  color: #555;
}

.sub-card p:last-child {
  margin-bottom: 0;
}
</style>
