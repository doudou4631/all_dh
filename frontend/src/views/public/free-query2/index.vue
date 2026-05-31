<template>
  <div class="redirecting">正在进入免费查询页面...</div>
</template>

<script setup name="PublicFreeQuery2Shell">
import { onMounted } from 'vue'

onMounted(() => {
  window.location.replace('/mobile-h5/')
})
</script>

<style scoped>
.redirecting {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-size: 14px;
}
</style>

