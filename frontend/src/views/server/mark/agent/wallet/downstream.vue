<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <span>资金明细(下级)</span>
      </template>
      <AgentWalletPanel />
    </el-card>
  </div>
</template>

<script setup name="MarkAgentWalletDownstream">
import AgentWalletPanel from '../components/AgentWalletPanel.vue'
</script>
