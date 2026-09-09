<template>
  <div class="app-container number-query-features-page">
    <el-card shadow="never" class="settings-panel">
      <template #header>
        <span class="settings-title">单次查询设置</span>
      </template>

      <el-form label-width="130px">
        <el-form-item label="查询结果延迟">
          <div class="delay-control">
            <el-input-number
              v-model="resultDelaySeconds"
              :min="0"
              :max="MAX_SINGLE_QUERY_RESULT_DELAY_SECONDS"
              :step="1"
              :precision="0"
              controls-position="right"
            />
            <span class="delay-unit">秒</span>
          </div>
        </el-form-item>
      </el-form>

      <div class="settings-actions">
        <el-button type="primary" icon="Check" @click="handleSave">保存</el-button>
        <el-button icon="RefreshLeft" @click="handleReset">恢复默认</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="NumberQueryFeatures">
import {
  DEFAULT_SINGLE_QUERY_RESULT_DELAY_SECONDS,
  MAX_SINGLE_QUERY_RESULT_DELAY_SECONDS,
  getSingleQueryResultDelaySeconds,
  setSingleQueryResultDelaySeconds
} from '@/utils/numberQuerySettings'

const { proxy } = getCurrentInstance()
const resultDelaySeconds = ref(getSingleQueryResultDelaySeconds())

function handleSave() {
  resultDelaySeconds.value = setSingleQueryResultDelaySeconds(resultDelaySeconds.value)
  proxy.$modal.msgSuccess('保存成功')
}

function handleReset() {
  resultDelaySeconds.value = setSingleQueryResultDelaySeconds(DEFAULT_SINGLE_QUERY_RESULT_DELAY_SECONDS)
  proxy.$modal.msgSuccess('已恢复默认设置')
}
</script>

<style scoped>
.number-query-features-page {
  width: 100%;
}

.settings-panel {
  max-width: 680px;
}

.settings-title {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.delay-control {
  display: flex;
  align-items: center;
  gap: 10px;
}

.delay-control :deep(.el-input-number) {
  width: 180px;
}

.delay-unit {
  color: var(--el-text-color-secondary);
}

.settings-actions {
  padding-left: 130px;
}

@media (max-width: 600px) {
  .settings-panel {
    max-width: none;
  }

  .settings-actions {
    padding-left: 0;
  }
}
</style>
