<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
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
  <section class="card panel">
    <div v-if="title || displaySubtitle || pill" class="panel-header">
      <div>
        <div v-if="title" class="panel-title">{{ title }}</div>
        <div v-if="displaySubtitle" class="panel-subtitle">{{ displaySubtitle }}</div>
      </div>
      <span v-if="pill" class="pill">{{ pill }}</span>
    </div>

    <div class="table-toolbar">
      <slot />
    </div>
  </section>
</template>
