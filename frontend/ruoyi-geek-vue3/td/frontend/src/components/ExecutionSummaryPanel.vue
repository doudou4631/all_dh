<script setup>
import DetailFieldGrid from './DetailFieldGrid.vue'
import StatePanel from './StatePanel.vue'

const props = defineProps({
  summary: { type: Object, default: null },
  title: { type: String, default: 'Execution Summary' },
  subtitle: { type: String, default: 'Track completed, active, blocked, and pending execution steps.' },
})

function toFields(summary) {
  if (!summary) return []
  return [
    { label: 'Overall Status', value: summary.overallStatus || 'pending' },
    { label: 'Completed Steps', value: `${summary.completedSteps || 0} / ${summary.totalSteps || 0}` },
    { label: 'Active Steps', value: String(summary.activeSteps || 0) },
    { label: 'Blocked Steps', value: String(summary.blockedSteps || 0) },
    { label: 'Ready Steps', value: String(summary.readySteps || 0) },
    { label: 'Pending Steps', value: String(summary.pendingSteps || 0) },
    { label: 'Next Step', value: summary.nextStepTitle || '(none)' },
  ]
}
</script>

<template>
  <StatePanel
    v-if="summary"
    :title="title"
    :description="subtitle"
  >
    <DetailFieldGrid :fields="toFields(summary)" />
  </StatePanel>
</template>
