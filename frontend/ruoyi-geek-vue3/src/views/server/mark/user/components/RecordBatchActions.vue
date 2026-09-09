<template>
  <div class="record-batch-actions">
    <span class="record-batch-actions__count">共 {{ total }} 条记录</span>
    <span class="record-batch-actions__selected">已选 {{ selectedRows.length }} 个</span>
    <el-button
      v-if="allowRemark"
      type="primary"
      plain
      size="small"
      :disabled="!selectedRows.length"
      @click="openRemarkDialog"
    >
      批量备注
    </el-button>
    <el-button size="small" :disabled="!selectedRows.length" @click="copySelectedPhones">选取复制</el-button>

    <el-dialog
      v-model="dialogOpen"
      :title="`批量备注（已选 ${selectedRows.length} 个）`"
      width="min(440px, calc(100vw - 32px))"
      append-to-body
      destroy-on-close
    >
      <el-input
        v-model="remark"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        resize="none"
        placeholder="请输入备注，留空保存可清空备注"
        @keyup.ctrl.enter="saveRemark"
      />
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRemark">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { updateMarkUserOrderItemRemarks } from '@/api/server/markUser'

const props = defineProps({
  total: { type: Number, default: 0 },
  selectedRows: { type: Array, default: () => [] },
  allowRemark: { type: Boolean, default: true }
})
const emit = defineEmits(['remark-saved'])
const { proxy } = getCurrentInstance()
const dialogOpen = ref(false)
const remark = ref('')
const saving = ref(false)

function openRemarkDialog() {
  if (!props.selectedRows.length) return
  remark.value = ''
  dialogOpen.value = true
}

async function copyToClipboard(text) {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text)
    return
  }
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}

async function copySelectedPhones() {
  const phones = props.selectedRows
    .map((row) => String(row.phonePreview || row.phone || '').trim())
    .filter(Boolean)
  if (!phones.length) {
    proxy.$modal.msgWarning('选中记录中没有可复制的号码')
    return
  }
  try {
    await copyToClipboard(phones.join('\n'))
    proxy.$modal.msgSuccess(`已复制${phones.length}个号码`)
  } catch (error) {
    proxy.$modal.msgError('复制失败，请重试')
  }
}

async function saveRemark() {
  const itemIds = props.selectedRows.map((row) => row.itemId).filter(Boolean)
  if (!itemIds.length || saving.value) return
  saving.value = true
  try {
    const value = String(remark.value || '').trim()
    await updateMarkUserOrderItemRemarks(itemIds, value)
    props.selectedRows.forEach((row) => { row.userRemark = value })
    emit('remark-saved', value)
    dialogOpen.value = false
    proxy.$modal.msgSuccess(`已更新${itemIds.length}条备注`)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.record-batch-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  min-height: 40px;
  padding: 2px 0 8px;
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.record-batch-actions__count,
.record-batch-actions__selected {
  white-space: nowrap;
}

@media (max-width: 640px) {
  .record-batch-actions {
    gap: 8px;
  }
}
</style>
