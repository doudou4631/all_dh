<script setup>
defineProps({
  title: { type: String, default: 'Loading...' },
  description: { type: String, default: '' },
  variant: { type: String, default: 'info' },
})
</script>

<template>
  <div class="state-panel" :class="`state-panel-${variant}`">
    <div class="state-panel-title">{{ title }}</div>
    <div v-if="description" class="state-panel-description">{{ description }}</div>
    <slot />
  </div>
</template>
