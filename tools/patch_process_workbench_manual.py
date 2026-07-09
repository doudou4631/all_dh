from pathlib import Path

path = Path(r"c:\Users\Administrator\Desktop\1500\frontend\src\views\server\mark\agent\components\ProcessWorkbench.vue")
text = path.read_text(encoding='utf-8')

old_action = '''        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="scope">
            <div v-if="scope.row.processStatus === '0' && !isTencentAutoPlatform(scope.row)" class="action-cell">
              <el-input
                v-model="rowInputMap[scope.row.id]"
                size="small"
                placeholder="验证码/备注"
                class="action-input"
              />
              <el-button
                type="success"
                size="small"
                :loading="submittingId === scope.row.id"
                v-hasPermi="['server:markAgent:item:feedback']"
                @click="submitFeedback(scope.row, '1')"
              >
                成功
              </el-button>
              <el-button
                type="danger"
                size="small"
                plain
                :loading="submittingId === scope.row.id"
                v-hasPermi="['server:markAgent:item:feedback']"
                @click="submitFeedback(scope.row, '2')"
              >
                失败
              </el-button>
              <span v-if="isTdGaopinPlatform(scope.row)" class="auto-detect-tip">自动检测中</span>
            </div>
            <span v-else-if="scope.row.processStatus === '0' && isTencentAutoPlatform(scope.row)" class="processed-tip">后台处理中</span>
            <span v-else class="processed-tip">已处理</span>
          </template>
        </el-table-column>'''

new_action = '''        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="scope">
            <div v-if="canManualProcess(scope.row)" class="action-cell">
              <el-input
                v-model="rowInputMap[scope.row.id]"
                size="small"
                placeholder="验证码/备注"
                class="action-input"
              />
              <el-button
                type="success"
                size="small"
                :loading="submittingId === scope.row.id"
                v-hasPermi="['server:markAgent:item:feedback']"
                @click="submitFeedback(scope.row, '1')"
              >
                {{ scope.row.processStatus === '0' ? '成功' : '改成功' }}
              </el-button>
              <el-button
                type="danger"
                size="small"
                plain
                :loading="submittingId === scope.row.id"
                v-hasPermi="['server:markAgent:item:feedback']"
                @click="submitFeedback(scope.row, '2')"
              >
                {{ scope.row.processStatus === '0' ? '失败' : '改失败' }}
              </el-button>
              <span v-if="scope.row.processStatus === '0' && isTdGaopinPlatform(scope.row)" class="auto-detect-tip">自动检测中</span>
            </div>
            <span v-else-if="scope.row.processStatus === '0' && isTencentAutoPlatform(scope.row)" class="processed-tip">后台处理中</span>
            <span v-else class="processed-tip">已处理</span>
          </template>
        </el-table-column>'''

old_funcs = '''function isTdGaopinPlatform(row) {
  const code = String(row?.platformCode || props.platformCode || '').trim().toLowerCase()
  return code === 'td_gaopin'
}
const tdGaopinAutoDetecting = computed(() => hasTdGaopinPending(itemList.value))'''

new_funcs = '''function isTdGaopinPlatform(row) {
  const code = String(row?.platformCode || props.platformCode || '').trim().toLowerCase()
  return code === 'td_gaopin'
}
function canManualProcess(row) {
  return !isTencentAutoPlatform(row)
}
function shouldRunTdGaopinAutoDetect() {
  if (!hasTdGaopinPending(itemList.value)) return false
  const status = queryParams.processStatus
  if (status === '1' || status === '2') return false
  return true
}
const tdGaopinAutoDetecting = computed(() => shouldRunTdGaopinAutoDetect())'''

old_trigger = '''function triggerTdGaopinAutoDetect() {
  if (!hasTdGaopinPending(itemList.value)) {
    return Promise.resolve()
  }
  return autoDetectTdGaopinItems().catch(() => {})
}'''

new_trigger = '''function triggerTdGaopinAutoDetect() {
  if (!shouldRunTdGaopinAutoDetect()) {
    return Promise.resolve()
  }
  return autoDetectTdGaopinItems().catch(() => {})
}'''

old_setup = '''function setupTdGaopinAutoRefresh() {
  clearTdGaopinAutoRefresh()
  if (!hasTdGaopinPending(itemList.value)) return'''

new_setup = '''function setupTdGaopinAutoRefresh() {
  clearTdGaopinAutoRefresh()
  if (!shouldRunTdGaopinAutoDetect()) return'''

old_submit = '''function submitFeedback(row, processStatus) {
  const itemId = row?.id
  if (!itemId) return
  const processResult = String(rowInputMap.value[itemId] || '').trim()
  const actionText = processStatus === '1' ? '标记成功' : '标记失败'
  proxy.$modal.confirm(`确认将该号码${actionText}?`).then(() => {
    submittingId.value = itemId
    return feedbackMarkOrderItem(itemId, {
      processStatus,
      processResult: processResult || (processStatus === '1' ? 'success' : 'failed')
    })
  }).then(() => {
    const tip = processStatus === '2'
      ? '操作成功，已自动退回次数'
      : '操作成功'
    proxy.$modal.msgSuccess(tip)
    delete rowInputMap.value[itemId]
    getList()
  }).catch(() => {}).finally(() => {
    submittingId.value = null
  })
}'''

new_submit = '''function submitFeedback(row, processStatus) {
  const itemId = row?.id
  if (!itemId) return
  const currentStatus = String(row?.processStatus || '0')
  if (currentStatus === processStatus) {
    proxy.$modal.msgWarning('当前已是该状态，无需重复操作')
    return
  }
  const processResult = String(rowInputMap.value[itemId] || '').trim()
  let confirmText = ''
  if (currentStatus === '0') {
    confirmText = processStatus === '1' ? '确认将该号码标记成功?' : '确认将该号码标记失败？失败将自动退回次数'
  } else if (processStatus === '2') {
    confirmText = '确认将已处理订单改为失败？将自动退回次数'
  } else {
    confirmText = '确认将失败订单改为成功？如已退回次数将重新扣费'
  }
  proxy.$modal.confirm(confirmText).then(() => {
    submittingId.value = itemId
    return feedbackMarkOrderItem(itemId, {
      processStatus,
      processResult: processResult || (processStatus === '1' ? 'success' : 'failed')
    })
  }).then(() => {
    let tip = '操作成功'
    if (processStatus === '2') tip = '操作成功，已自动退回次数'
    if (currentStatus === '2' && processStatus === '1') tip = '操作成功，已重新扣费'
    proxy.$modal.msgSuccess(tip)
    delete rowInputMap.value[itemId]
    getList()
  }).catch(() => {}).finally(() => {
    submittingId.value = null
  })
}'''

for old, new in [
    (old_action, new_action),
    (old_funcs, new_funcs),
    (old_trigger, new_trigger),
    (old_setup, new_setup),
    (old_submit, new_submit),
]:
    if old not in text:
        raise SystemExit('block not found')
    text = text.replace(old, new, 1)

path.write_text(text, encoding='utf-8')
print('patched')
