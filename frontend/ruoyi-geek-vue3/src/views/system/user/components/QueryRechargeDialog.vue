<template>
  <el-dialog
    :model-value="modelValue"
    title="查询充值"
    width="430px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
    @open="resetForm"
  >
    <el-form label-width="100px">
      <el-form-item label="用户">
        <span>{{ displayUserName }}</span>
      </el-form-item>
      <el-form-item label="当前积分">
        <span class="query-points">{{ currentPoints }}</span>
      </el-form-item>
      <el-form-item label="充值积分" required>
        <el-input-number
          v-model="form.amount"
          :min="1"
          :max="10000000"
          :step="1"
          :precision="0"
          controls-position="right"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="充值后积分">
        <span class="query-points">{{ expectedPoints }}</span>
      </el-form-item>
      <el-form-item label="备注">
        <el-input
          v-model="form.remark"
          maxlength="255"
          show-word-limit
          placeholder="选填"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitRecharge">确定充值</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { rechargeQueryPoints } from '@/api/server/pointRecord'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  user: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'changed'])
const { proxy } = getCurrentInstance()
const saving = ref(false)
const form = reactive({ amount: 1, remark: '用户管理查询充值' })

const displayUserName = computed(() => props.user?.userName || props.user?.nickName || '-')
const currentPoints = computed(() => Number(props.user?.points ?? 0))
const expectedPoints = computed(() => currentPoints.value + Number(form.amount || 0))

function resetForm() {
  form.amount = 1
  form.remark = '用户管理查询充值'
}

async function submitRecharge() {
  const amount = Number(form.amount || 0)
  if (!props.user?.userId || !Number.isInteger(amount) || amount <= 0) {
    proxy.$modal.msgError('充值积分必须是大于0的整数')
    return
  }

  saving.value = true
  try {
    await rechargeQueryPoints({
      userId: props.user.userId,
      amount,
      remark: form.remark
    })
    proxy.$modal.msgSuccess('查询积分充值成功')
    emit('update:modelValue', false)
    emit('changed')
  } catch (error) {
    proxy.$modal.msgError(error?.message || '查询积分充值失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.query-points {
  color: var(--el-color-success);
  font-weight: 600;
}
</style>
