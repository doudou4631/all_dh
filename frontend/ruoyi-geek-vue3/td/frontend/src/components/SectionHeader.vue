<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  pill: { type: String, default: '' },
})

const displaySubtitle = computed(() => {
  const text = String(props.subtitle || '').trim()
  if (!text) return ''
  if (text.includes('按旧版')) return ''
  return text
})
</script>

<template>
  <div class="panel-header">
    <div>
      <div class="panel-title">{{ title }}</div>
      <div v-if="displaySubtitle" class="panel-subtitle">{{ displaySubtitle }}</div>
    </div>
    <span v-if="pill" class="pill">{{ pill }}</span>
  </div>
</template>
