<template>
  <div class="order-remark-editor">
    <button
      type="button"
      :class="['order-remark-editor__trigger', { 'is-empty': !displayRemark }]"
      :title="displayRemark || '点击备注'"
      @click="openEditor"
    >
      {{ displayRemark || '点击备注' }}
    </button>

    <el-dialog
      v-model="open"
      title="号码备注"
      width="min(440px, calc(100vw - 32px))"
      append-to-body
      destroy-on-close
      @closed="resetValue"
    >
      <el-input
        v-model="value"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        resize="none"
        placeholder="请输入备注"
        @keyup.ctrl.enter="save"
      />
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { updateMarkUserOrderItemRemark } from '@/api/server/markUser'

const props = defineProps({ row: { type: Object, required: true } })
const emit = defineEmits(['saved'])
const open = ref(false)
const value = ref('')
const saving = ref(false)
const { proxy } = getCurrentInstance()
const displayRemark = computed(() => String(props.row?.userRemark || '').trim())

function resetValue() {
  value.value = displayRemark.value
}

function openEditor() {
  resetValue()
  open.value = true
}

async function save() {
  if (!props.row?.itemId || saving.value) return
  saving.value = true
  try {
    const remark = String(value.value || '').trim()
    await updateMarkUserOrderItemRemark(props.row.itemId, remark)
    props.row.userRemark = remark
    emit('saved', remark)
    open.value = false
    proxy.$modal.msgSuccess('备注保存成功')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.order-remark-editor {
  width: 100%;
  min-width: 0;
  text-align: center;
}

.order-remark-editor__trigger {
  display: block;
  width: 100%;
  padding: 4px 0;
  overflow: hidden;
  color: var(--el-color-primary);
  font: inherit;
  line-height: 22px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.order-remark-editor__trigger.is-empty {
  color: var(--el-text-color-placeholder);
}

.order-remark-editor__trigger:hover {
  color: var(--el-color-primary-light-3);
}
</style>
