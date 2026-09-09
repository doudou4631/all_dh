<script setup>
import { RouterLink } from 'vue-router'

defineProps({
  items: { type: Array, default: () => [] },
  compact: { type: Boolean, default: false },
})
</script>

<template>
  <div class="timeline-list" :class="{ compact }">
    <div v-for="(item, index) in items" :key="item.title + index" class="timeline-item">
      <div class="timeline-node"></div>
      <div class="timeline-body">
        <div class="timeline-title-row">
          <strong>{{ item.title }}</strong>
          <span v-if="item.meta" class="muted timeline-meta">{{ item.meta }}</span>
        </div>
        <div class="muted">{{ item.description }}</div>
        <div v-if="item.to || item.href" class="timeline-action-row">
          <RouterLink v-if="item.to" :to="item.to" class="timeline-action-link">
            {{ item.actionLabel || 'Open' }}
          </RouterLink>
          <a v-else :href="item.href" class="timeline-action-link" target="_blank" rel="noreferrer">
            {{ item.actionLabel || 'Open' }}
          </a>
        </div>
      </div>
    </div>
  </div>
</template>
