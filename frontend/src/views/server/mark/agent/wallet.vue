<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <span>资金流水明细</span>
      </template>
      <AgentWalletPanel />
    </el-card>
  </div>
</template>

<script setup name="MarkAgentWalletLog">
import AgentWalletPanel from './components/AgentWalletPanel.vue'
</script>
