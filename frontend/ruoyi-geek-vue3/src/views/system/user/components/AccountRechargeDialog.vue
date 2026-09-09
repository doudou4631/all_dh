<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="min(1080px, 94vw)"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
    @open="loadPlatformOptions"
  >
    <div class="account-recharge-toolbar">
      <div class="account-recharge-tip">
        关闭某个平台后，该用户打开对应提交页会提示“平台未开启，请联系管理员”，不能提交订单。
      </div>
      <el-button :loading="platformLoading" icon="Refresh" @click="loadPlatformOptions">刷新平台</el-button>
    </div>

    <el-table v-loading="platformLoading" :data="platformOptions" border stripe max-height="560">
      <el-table-column label="平台" prop="platformName" min-width="170" show-overflow-tooltip />
      <el-table-column label="编码" prop="platformCode" min-width="145" show-overflow-tooltip />
      <el-table-column label="状态" width="120" align="center">
        <template #default="scope">
          <el-switch
            :model-value="isPlatformEnabled(scope.row)"
            active-text="开启"
            inactive-text="关闭"
            inline-prompt
            :loading="statusLoadingKey === platformRowKey(scope.row)"
            @change="(value) => handlePlatformStatusChange(scope.row, value)"
          />
        </template>
      </el-table-column>
      <el-table-column label="单价" prop="unitPrice" width="100" align="center" />
      <el-table-column label="剩余次数" prop="remainCount" width="130" align="center">
        <template #default="scope">
          <span class="remain-value">{{ scope.row.remainCount ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="success" @click="openAdjustDialog(scope.row, 'ADD')">充值</el-button>
          <el-button link type="danger" @click="openAdjustDialog(scope.row, 'SUBTRACT')">扣减</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
    </template>

    <el-dialog
      v-model="adjustDialog.open"
      :title="adjustDialogTitle"
      width="430px"
      append-to-body
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item label="用户">
          <span>{{ displayUserName }}</span>
        </el-form-item>
        <el-form-item label="平台">
          <span>{{ adjustForm.platformName || '-' }}</span>
        </el-form-item>
        <el-form-item label="当前剩余">
          <span class="remain-value">{{ adjustForm.currentRemain }}</span>
        </el-form-item>
        <el-form-item label="变动次数" required>
          <el-input-number
            v-model="adjustForm.changeCount"
            :min="1"
            :step="1"
            :precision="0"
            controls-position="right"
            style="width: 190px"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="adjustForm.remark" maxlength="100" show-word-limit placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialog.open = false">取消</el-button>
        <el-button type="primary" :loading="adjustLoading" @click="submitAdjust">确定</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import {
  adjustMarkAgentQuota,
  listMarkAgentQuotaPlatformOptions,
  setMarkAgentPlatformStatus
} from '@/api/server/markAgent'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  user: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'changed'])
const { proxy } = getCurrentInstance()

const platformLoading = ref(false)
const platformOptions = ref([])
const statusLoadingKey = ref('')
const adjustLoading = ref(false)
const adjustDialog = reactive({ open: false, type: 'ADD' })
const adjustForm = reactive({
  platformCode: '',
  platformName: '',
  currentRemain: 0,
  changeCount: 1,
  remark: ''
})

const displayUserName = computed(() => props.user?.userName || props.user?.nickName || '-')
const dialogTitle = computed(() => `账户充值 - ${displayUserName.value}`)
const adjustDialogTitle = computed(() => adjustDialog.type === 'ADD' ? '平台充值' : '平台扣减')

function platformRowKey(row) {
  return `${props.user?.userId || ''}:${String(row?.platformCode || '').toLowerCase()}`
}

function isPlatformEnabled(row) {
  return String(row?.status ?? '0') !== '1'
}

async function loadPlatformOptions() {
  if (!props.user?.userId) return
  platformLoading.value = true
  try {
    const response = await listMarkAgentQuotaPlatformOptions(props.user.userId)
    platformOptions.value = (Array.isArray(response?.data) ? response.data : []).map((item) => ({
      ...item,
      status: String(item?.status ?? '0'),
      remainCount: Number(item?.remainCount ?? 0),
      unitPrice: Number(item?.unitPrice ?? 1)
    }))
  } finally {
    platformLoading.value = false
  }
}

async function handlePlatformStatusChange(row, enabled) {
  if (!props.user?.userId || !row?.platformCode) return
  const oldStatus = String(row.status ?? '0')
  const nextStatus = enabled ? '0' : '1'
  statusLoadingKey.value = platformRowKey(row)
  row.status = nextStatus
  try {
    await setMarkAgentPlatformStatus({
      userId: props.user.userId,
      platformCode: row.platformCode,
      platformName: row.platformName,
      status: nextStatus
    })
    proxy.$modal.msgSuccess(enabled ? '平台已开启' : '平台已关闭')
    emit('changed')
  } catch (error) {
    row.status = oldStatus
    proxy.$modal.msgError(error?.message || '平台状态更新失败')
  } finally {
    statusLoadingKey.value = ''
  }
}

function openAdjustDialog(row, type) {
  adjustDialog.type = type
  adjustForm.platformCode = row.platformCode
  adjustForm.platformName = row.platformName
  adjustForm.currentRemain = Number(row.remainCount ?? 0)
  adjustForm.changeCount = 1
  adjustForm.remark = type === 'ADD' ? '账户充值' : '账户扣减'
  adjustDialog.open = true
}

async function submitAdjust() {
  if (!props.user?.userId || !adjustForm.platformCode) return
  const count = Number(adjustForm.changeCount || 0)
  if (!Number.isInteger(count) || count <= 0) {
    proxy.$modal.msgError('变动次数必须是大于 0 的整数')
    return
  }
  if (adjustDialog.type === 'SUBTRACT' && count > Number(adjustForm.currentRemain || 0)) {
    proxy.$modal.msgError('当前平台剩余次数不足')
    return
  }

  adjustLoading.value = true
  try {
    await adjustMarkAgentQuota({
      userId: props.user.userId,
      platformCode: adjustForm.platformCode,
      platformName: adjustForm.platformName,
      adjustType: adjustDialog.type,
      changeCount: count,
      remark: adjustForm.remark
    })
    proxy.$modal.msgSuccess(adjustDialog.type === 'ADD' ? '充值成功' : '扣减成功')
    adjustDialog.open = false
    await loadPlatformOptions()
    emit('changed')
  } catch (error) {
    proxy.$modal.msgError(error?.message || '操作失败')
  } finally {
    adjustLoading.value = false
  }
}
</script>

<style scoped>
.account-recharge-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.account-recharge-tip {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  line-height: 1.5;
}

.remain-value {
  color: var(--el-color-success);
  font-weight: 600;
}

@media (max-width: 768px) {
  .account-recharge-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
