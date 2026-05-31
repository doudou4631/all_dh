<template>
  <div class="public-shell">
    <ApiQueryPage :show-public-result="true" :public-marked-only="true" />
  </div>
</template>

<script setup name="PublicFreeQueryMarkedShell">
import ApiQueryPage from '@/views/server/web/apiquery.vue'
</script>

<style scoped>
.public-shell {
  min-height: 100vh;
}
</style>
