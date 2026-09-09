<template>
  <el-popover
    v-model:visible="popoverVisible"
    placement="bottom-start"
    :width="348"
    trigger="click"
    popper-class="agent-past-date-range-popper"
  >
    <template #reference>
      <el-input
        :model-value="displayText"
        readonly
        clearable
        :placeholder="placeholder"
        class="agent-past-date-range__trigger"
        @clear="handleClear"
      />
    </template>
    <div class="agent-past-date-range">
      <div class="agent-past-date-range__row">
        <span class="agent-past-date-range__label">开始时间</span>
        <el-date-picker
          v-model="draftStart"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="开始时间"
          size="small"
          class="agent-past-date-range__picker"
        />
      </div>
      <div class="agent-past-date-range__row">
        <span class="agent-past-date-range__label">结束时间</span>
        <el-date-picker
          v-model="draftEnd"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="结束时间"
          size="small"
          class="agent-past-date-range__picker"
        />
      </div>
      <div class="agent-past-date-range__quick">
        <div class="agent-past-date-range__quick-head">过去</div>
        <div class="agent-past-date-range__quick-grid">
          <button
            v-for="item in quickOptions"
            :key="item.days"
            type="button"
            class="agent-past-date-range__chip"
            :class="{ 'is-active': activeQuickDay === item.days }"
            @click="applyQuickDays(item.days)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
      <div class="agent-past-date-range__footer">
        <el-button type="primary" size="small" @click="handleConfirm">确定</el-button>
        <el-button size="small" @click="handleClear">清空</el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: '请选择日期'
  }
})

const emit = defineEmits(['update:modelValue'])

const quickOptions = [
  { days: 0, label: '今天' },
  { days: 1, label: `1天` },
  { days: 15, label: `15天` },
  { days: 30, label: `30天` },
  { days: 60, label: `60天` }
]
const popoverVisible = ref(false)
const draftStart = ref('')
const draftEnd = ref('')
const activeQuickDay = ref(null)

const displayText = computed(() => {
  const [start, end] = props.modelValue || []
  if (start && end) return `${start} ~ ${end}`
  if (start) return start
  if (end) return end
  return ''
})

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function applyQuickDays(days) {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - days)
  draftStart.value = formatDate(start)
  draftEnd.value = formatDate(end)
  activeQuickDay.value = days
}

function syncDraftFromModel() {
  const [start, end] = props.modelValue || []
  draftStart.value = start || ''
  draftEnd.value = end || ''
  activeQuickDay.value = null
}

function handleConfirm() {
  if (draftStart.value && draftEnd.value && draftStart.value > draftEnd.value) {
    draftEnd.value = draftStart.value
  }
  const next = draftStart.value || draftEnd.value
    ? [draftStart.value || draftEnd.value, draftEnd.value || draftStart.value]
    : []
  emit('update:modelValue', next)
  popoverVisible.value = false
}

function handleClear() {
  draftStart.value = ''
  draftEnd.value = ''
  activeQuickDay.value = null
  emit('update:modelValue', [])
  popoverVisible.value = false
}

watch(() => props.modelValue, syncDraftFromModel, { immediate: true, deep: true })
watch(popoverVisible, (visible) => {
  if (visible) syncDraftFromModel()
})
watch([draftStart, draftEnd], () => {
  activeQuickDay.value = null
})
</script>

<style scoped lang="scss">
.agent-past-date-range__trigger {
  width: 240px;
}

.agent-past-date-range__row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.agent-past-date-range__label {
  flex: 0 0 56px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.agent-past-date-range__picker {
  flex: 1;
}

.agent-past-date-range__quick {
  margin: 4px 0 12px;
  padding-top: 10px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.agent-past-date-range__quick-head {
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.agent-past-date-range__quick-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
}

.agent-past-date-range__chip {
  height: 28px;
  padding: 0;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  line-height: 26px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    color: var(--el-color-primary);
  }

  &.is-active {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    font-weight: 500;
  }
}

.agent-past-date-range__footer {
  display: flex;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
