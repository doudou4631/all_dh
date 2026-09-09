<script setup>
defineProps({
  fields: { type: Array, default: () => [] },
})
</script>

<template>
  <div class="form-grid">
    <div v-for="field in fields" :key="field.label" class="field">
      <label>{{ field.label }}</label>
      <input :value="field.value" />
    </div>
  </div>
</template>
