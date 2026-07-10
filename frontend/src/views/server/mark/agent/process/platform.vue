<template>
  <div class="app-container mark-agent-process-platform">
    <ProcessWorkbench
      :platform-code="platformCode"
      :page-title="platformTitle"
      :show-supply-tabs="false"
    />
  </div>
</template>

<script setup name="MarkAgentProcessPlatform">
import ProcessWorkbench from '../components/ProcessWorkbench.vue'
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const platformCode = computed(() => String(route.query.platformCode || '').trim())
const platformTitle = computed(() => String(route.query.platformName || route.meta?.title || '').trim())
</script>

<style scoped>
.mark-agent-process-platform {
  padding: 0 12px 12px 12px;
  margin: 0;
  max-width: none;
}

@media (max-width: 768px) {
  .mark-agent-process-platform {
    padding: 8px !important;
  }
}
</style>
