<template>
  <div class="app-container ip-policy-page">
    <div class="page-toolbar">
      <div>
        <h2>IP访问策略</h2>
        <span class="updated-at">更新于 {{ form.updateTime || form.createTime || '-' }}</span>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadConfig">刷新</el-button>
    </div>

    <el-card shadow="never" class="policy-panel" v-loading="loading">
      <el-form ref="policyFormRef" :model="form" :rules="rules" label-width="150px">
        <el-form-item label="每日查询限额" prop="dailyLimit">
          <el-input-number v-model="form.dailyLimit" :min="1" :max="1000000" controls-position="right" />
          <span class="unit">次 / IP / 天</span>
        </el-form-item>

        <el-form-item label="结果显示延时" prop="intervalMs">
          <el-input-number v-model="form.intervalMs" :min="0" :max="600000" :step="100" controls-position="right" />
          <span class="unit">毫秒</span>
          <div class="field-help">每次查询达到该时长后才显示结果，设置为 0 表示不延时。</div>
        </el-form-item>

        <el-form-item label="配额重置时间" prop="resetTime">
          <el-time-picker
            v-model="form.resetTime"
            format="HH:mm"
            value-format="HH:mm:ss"
            placeholder="请选择每日重置时间"
          />
        </el-form-item>

        <el-form-item label="超限提示文案" prop="exceedMessage">
          <el-input
            v-model="form.exceedMessage"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请输入超过每日限额时显示的提示"
          />
        </el-form-item>

        <el-form-item prop="whitelistIps">
          <template #label>
            <span class="label-with-help">
              白名单IP
              <el-tooltip content="白名单仅不受每日查询限额限制，结果显示延时仍然生效" placement="top">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-input
            v-model="form.whitelistIps"
            type="textarea"
            :rows="7"
            placeholder="每行填写一个IP地址"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Check" :loading="saving" @click="saveConfig"
            v-hasPermi="['freeQuery:ipPolicy:edit']">保存策略</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup name="FreeQueryIpPolicy">
import { Check, QuestionFilled, Refresh } from '@element-plus/icons-vue'
import { getFreeQueryIpPolicy, updateFreeQueryIpPolicy } from '@/api/server/freeQueryIpPolicy'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  id: 1,
  dailyLimit: 20,
  intervalMs: 500,
  resetTime: '00:00:00',
  exceedMessage: '今日查询次数已用完，请明日再试。',
  whitelistIps: '',
  createTime: '',
  updateTime: ''
})

const rules = {
  dailyLimit: [{ required: true, message: '请输入每日查询限额', trigger: 'change' }],
  intervalMs: [{ required: true, message: '请输入结果显示延时', trigger: 'change' }],
  resetTime: [{ required: true, message: '请选择配额重置时间', trigger: 'change' }],
  exceedMessage: [{ required: true, message: '请输入超限提示文案', trigger: 'blur' }]
}

async function loadConfig() {
  loading.value = true
  try {
    const response = await getFreeQueryIpPolicy()
    Object.assign(form, response.data || {})
    await nextTick()
    proxy.$refs.policyFormRef?.clearValidate()
  } finally {
    loading.value = false
  }
}

function saveConfig() {
  proxy.$refs.policyFormRef.validate(async valid => {
    if (!valid) return
    saving.value = true
    try {
      await updateFreeQueryIpPolicy({
        dailyLimit: form.dailyLimit,
        intervalMs: form.intervalMs,
        resetTime: form.resetTime,
        exceedMessage: form.exceedMessage,
        whitelistIps: form.whitelistIps
      })
      proxy.$modal.msgSuccess('IP访问策略已保存')
      await loadConfig()
    } finally {
      saving.value = false
    }
  })
}

loadConfig()
</script>

<style scoped>
.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-toolbar h2 {
  margin: 0 0 4px;
  font-size: 20px;
  line-height: 28px;
  letter-spacing: 0;
}

.updated-at {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.policy-panel {
  max-width: 880px;
  border-radius: 6px;
}

.policy-panel :deep(.el-card__body) {
  padding: 28px 30px 10px;
}

.policy-panel :deep(.el-form-item) {
  margin-bottom: 24px;
}

.policy-panel :deep(.el-textarea),
.policy-panel :deep(.el-time-editor) {
  width: 100%;
}

.unit {
  margin-left: 10px;
  color: var(--el-text-color-secondary);
}

.field-help {
  flex-basis: 100%;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}

.label-with-help {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 5px;
}

.label-with-help .el-icon {
  color: var(--el-text-color-secondary);
  cursor: help;
}

@media (max-width: 680px) {
  .policy-panel :deep(.el-card__body) {
    padding: 20px 16px 4px;
  }

  .policy-panel :deep(.el-form-item__label) {
    float: none;
    display: block;
    width: 100% !important;
    text-align: left;
  }

  .policy-panel :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }
}
</style>
