<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <span>资金转账明细</span>
      </template>
      <AgentWalletPanel fixed-biz-type="ADJUST" />
    </el-card>
  </div>
</template>

<script setup name="MarkAgentTransferLog">
import AgentWalletPanel from '../components/AgentWalletPanel.vue'
</script>
