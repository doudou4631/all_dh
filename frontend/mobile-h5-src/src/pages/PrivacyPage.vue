<template>
  <div class="sub-page">
    <a class="sub-back" :href="resolveHref('/profile/')">‹ 返回个人中心</a>
    <div class="sub-card">
      <h1>隐私协议</h1>
      <p class="sub-subtitle">我们重视您的个人信息保护</p>
      <h2>一、信息收集</h2>
      <p>为向您提供服务，我们可能收集手机号码、查询记录、设备信息等必要数据。仅在实现功能所必需的范围内收集。</p>
      <h2>二、信息使用</h2>
      <p>收集的信息用于账号登录、号码查询、订单处理、客服联系及服务质量改进，不会用于与上述目的无关的用途。</p>
      <h2>三、信息存储与保护</h2>
      <p>我们采取合理的技术与管理措施保护您的个人信息安全，防止未经授权的访问、披露或丢失。</p>
      <h2>四、信息共享</h2>
      <p>除法律法规要求或取得您明确同意外，我们不会向第三方出售或共享您的个人信息。</p>
      <h2>五、您的权利</h2>
      <p>您有权查询、更正或注销账号信息。注销后，账号数据记录将会清零。</p>
      <h2>六、联系我们</h2>
      <p>
        如有隐私相关问题，请通过在线客服或服务电话
        <a :href="phoneHref" style="color: #3b71fe">{{ servicePhone }}</a>
        联系我们。
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
  background: #f5f7fa;
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
  margin: 0 0 8px;
  font-size: 1.125rem;
  color: #2c3e50;
  text-align: center;
}

.sub-card h2 {
  margin: 20px 0 8px;
  font-size: 15px;
  color: #333;
}

.sub-card p {
  margin: 0 0 10px;
  font-size: 14px;
  line-height: 1.75;
  color: #555;
}

.sub-subtitle {
  text-align: center;
  color: #999;
  font-size: 13px;
  margin-bottom: 16px;
}
</style>
