<template>
  <div class="public-shell">
    <ApiQueryPage customer-wechat-image="/wechat/customer-wechat2.png" />
  </div>
</template>

<script setup name="PublicFreeQuery2Shell">
import ApiQueryPage from '@/views/server/web/apiquery.vue'
</script>

<style scoped>
.public-shell {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 12px;
  box-sizing: border-box;
}
</style>

