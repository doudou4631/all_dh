<template>
  <div class="app-container">
    <ProcessWorkbench
      :supply-tabs="tdTabs"
      default-supply-key="tdx"
      :show-supply-tabs="false"
    />
  </div>
</template>

<script setup name="MarkAgentTdCaptcha">
import ProcessWorkbench from '../components/ProcessWorkbench.vue'

const tdTabs = [
  { key: 'tdx', label: '清TD收验证码', platformCodes: 'yidonggaopin,td_gaopin,td_second' }
]
</script>
