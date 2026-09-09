<template>
  <el-dialog
    :model-value="modelValue"
    title="提交确认"
    width="min(520px, calc(100vw - 32px))"
    :close-on-click-modal="false"
    :close-on-press-escape="!loading"
    :show-close="!loading"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="submit-confirm-summary">
      <span class="submit-confirm-summary__label">提交平台</span>
      <el-tag type="primary" size="large" class="submit-confirm-platform">{{ platformName }}</el-tag>
      <span class="submit-confirm-summary__count">共 {{ phones.length }} 个号码</span>
    </div>

    <div class="submit-confirm-list" role="list" aria-label="待提交号码">
      <div v-for="(phone, index) in phones" :key="`${phone}-${index}`" class="submit-confirm-list__item" role="listitem">
        <span>{{ index + 1 }}</span>
        <strong>{{ phone }}</strong>
      </div>
    </div>

    <el-checkbox v-model="checked" class="submit-confirm-check" :disabled="loading">
      我已核对平台和号码
    </el-checkbox>

    <template #footer>
      <el-button :disabled="loading" @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="loading" :disabled="!checked" @click="emit('confirm')">
        确认提交{{ platformName }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  platformName: {
    type: String,
    default: ''
  },
  phones: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])
const checked = ref(false)

watch(() => props.modelValue, (open) => {
  if (open) checked.value = false
})

function handleCancel() {
  if (props.loading) return
  emit('cancel')
  emit('update:modelValue', false)
}
</script>

<style scoped>
.submit-confirm-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.submit-confirm-summary__label {
  color: #606266;
}

.submit-confirm-platform {
  height: 40px;
  padding: 0 18px;
  font-size: 16px;
  font-weight: 600;
}

.submit-confirm-summary__count {
  margin-left: auto;
  color: #606266;
  font-size: 13px;
}

.submit-confirm-list {
  max-height: 260px;
  overflow-y: auto;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
}

.submit-confirm-list__item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: center;
  min-height: 38px;
  padding: 0 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.submit-confirm-list__item:last-child {
  border-bottom: 0;
}

.submit-confirm-list__item span {
  color: #909399;
  font-size: 12px;
}

.submit-confirm-list__item strong {
  color: #303133;
  font-weight: 500;
}

.submit-confirm-check {
  margin-top: 16px;
}
</style>
