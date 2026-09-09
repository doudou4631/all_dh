<template>
  <div class="app-container mark-fund-subordinate-page">
    <div class="page-title">资金流水(下级)</div>
    <AgentWalletPanel />
  </div>
</template>

<script setup name="MarkFundSubordinate">
import AgentWalletPanel from '@/views/server/mark/agent/components/AgentWalletPanel.vue'
</script>

<style scoped>
.page-title { margin-bottom: 14px; color: var(--el-text-color-primary); font-size: 20px; font-weight: 600; letter-spacing: 0; }
</style>
