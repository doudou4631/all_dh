<template>
  <div class="app-container schedule-page">
    <el-card shadow="never" body-class="search-card">
      <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="72px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="queryParams.taskName" placeholder="请输入任务名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="任务类型" prop="taskType">
          <el-select v-model="queryParams.taskType" placeholder="全部类型" clearable style="width: 180px">
            <el-option v-for="item in taskTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="启用" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="mt10">
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" @click="handleAdd"
            v-hasPermi="['freeQuery:schedule:add']">新增任务</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()"
            v-hasPermi="['freeQuery:schedule:edit']">修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()"
            v-hasPermi="['freeQuery:schedule:remove']">删除</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </el-row>

      <el-table v-loading="loading" :data="scheduleList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" align="center" />
        <el-table-column label="任务名称" prop="taskName" min-width="150" show-overflow-tooltip />
        <el-table-column label="任务类型" prop="taskType" width="120" align="center">
          <template #default="scope">
            <el-tag :type="taskTypeTag(scope.row.taskType)" effect="plain">{{ taskTypeLabel(scope.row.taskType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="执行计划" min-width="210">
          <template #default="scope">
            <span v-if="scope.row.taskType === 'QUERY_WINDOW'">
              {{ formatTime(scope.row.startTime) }} - {{ formatTime(scope.row.endTime) }}
            </span>
            <code v-else>{{ scope.row.cronExpression || '-' }}</code>
          </template>
        </el-table-column>
        <el-table-column label="任务参数" min-width="220" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.taskType === 'DATA_IMPORT'">{{ scope.row.dataSource || '-' }}</span>
            <span v-else-if="scope.row.taskType === 'DATA_CLEANUP'">保留 {{ scope.row.retentionDays }} 天</span>
            <span v-else>查询服务开放时段</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <el-switch v-model="scope.row.status" active-value="0" inactive-value="1"
              @change="handleStatusChange(scope.row)"
              v-hasPermi="['freeQuery:schedule:changeStatus']" />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" prop="updateTime" width="165" align="center">
          <template #default="scope">{{ scope.row.updateTime || scope.row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
              v-hasPermi="['freeQuery:schedule:edit']">修改</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
              v-hasPermi="['freeQuery:schedule:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>

    <el-dialog v-model="open" :title="title" width="620px" append-to-body destroy-on-close>
      <el-form ref="scheduleRef" :model="form" :rules="rules" label-width="106px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" maxlength="100" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务类型" prop="taskType">
          <el-segmented v-model="form.taskType" :options="taskTypeOptions" @change="handleTaskTypeChange" />
        </el-form-item>

        <template v-if="form.taskType === 'QUERY_WINDOW'">
          <el-form-item label="开放时段" prop="startTime">
            <div class="time-range">
              <el-time-picker v-model="form.startTime" format="HH:mm" value-format="HH:mm:ss" placeholder="开始时间" />
              <span>至</span>
              <el-time-picker v-model="form.endTime" format="HH:mm" value-format="HH:mm:ss" placeholder="停止时间" />
            </div>
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="Cron表达式" prop="cronExpression">
            <el-select v-model="form.cronExpression" filterable allow-create default-first-option
              placeholder="请选择或输入6位Cron表达式" style="width: 100%">
              <el-option label="每天 02:00" value="0 0 2 * * ?" />
              <el-option label="每天 03:00" value="0 0 3 * * ?" />
              <el-option label="每小时整点" value="0 0 * * * ?" />
              <el-option label="每周一 02:00" value="0 0 2 ? * MON" />
            </el-select>
          </el-form-item>
        </template>

        <el-form-item v-if="form.taskType === 'DATA_IMPORT'" label="数据源地址" prop="dataSource">
          <el-input v-model="form.dataSource" maxlength="500" placeholder="请输入文件地址或数据接口地址" />
        </el-form-item>
        <el-form-item v-if="form.taskType === 'DATA_CLEANUP'" label="数据保留" prop="retentionDays">
          <el-input-number v-model="form.retentionDays" :min="1" :max="3650" controls-position="right" />
          <span class="unit">天</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确定</el-button>
        <el-button @click="cancel">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FreeQuerySchedule">
import {
  addFreeQuerySchedule,
  changeFreeQueryScheduleStatus,
  delFreeQuerySchedule,
  getFreeQuerySchedule,
  listFreeQuerySchedule,
  updateFreeQuerySchedule
} from '@/api/server/freeQuerySchedule'

const { proxy } = getCurrentInstance()

const taskTypes = [
  { label: '查询时段', value: 'QUERY_WINDOW' },
  { label: '定时导入', value: 'DATA_IMPORT' },
  { label: '定时清理', value: 'DATA_CLEANUP' }
]
const taskTypeOptions = taskTypes.map(item => ({ label: item.label, value: item.value }))

const loading = ref(false)
const open = ref(false)
const showSearch = ref(true)
const scheduleList = ref([])
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    taskName: null,
    taskType: null,
    status: null
  },
  rules: {
    taskName: [{ required: true, message: '任务名称不能为空', trigger: 'blur' }],
    taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
    startTime: [{ validator: validateTimeRange, trigger: 'change' }],
    cronExpression: [{ validator: validateCron, trigger: 'change' }],
    dataSource: [{ validator: validateDataSource, trigger: 'blur' }],
    retentionDays: [{ validator: validateRetentionDays, trigger: 'change' }]
  }
})

const { form, queryParams, rules } = toRefs(data)

function validateTimeRange(_rule, _value, callback) {
  if (form.value.taskType === 'QUERY_WINDOW' && (!form.value.startTime || !form.value.endTime)) {
    callback(new Error('请选择完整的开始和停止时间'))
  } else {
    callback()
  }
}

function validateCron(_rule, value, callback) {
  if (form.value.taskType !== 'QUERY_WINDOW' && !value) {
    callback(new Error('Cron表达式不能为空'))
  } else {
    callback()
  }
}

function validateDataSource(_rule, value, callback) {
  if (form.value.taskType === 'DATA_IMPORT' && !value) {
    callback(new Error('数据源地址不能为空'))
  } else {
    callback()
  }
}

function validateRetentionDays(_rule, value, callback) {
  if (form.value.taskType === 'DATA_CLEANUP' && (!value || value < 1)) {
    callback(new Error('数据保留天数必须大于0'))
  } else {
    callback()
  }
}

function getList() {
  loading.value = true
  listFreeQuerySchedule(queryParams.value).then(response => {
    scheduleList.value = response.rows
    total.value = response.total
  }).finally(() => {
    loading.value = false
  })
}

function reset() {
  form.value = {
    id: null,
    taskName: '',
    taskType: 'QUERY_WINDOW',
    startTime: '08:00:00',
    endTime: '22:00:00',
    cronExpression: null,
    dataSource: null,
    retentionDays: 90,
    status: '0',
    remark: ''
  }
  proxy.resetForm('scheduleRef')
}

function handleTaskTypeChange() {
  proxy.$nextTick(() => proxy.$refs.scheduleRef?.clearValidate())
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = selection.length === 0
}

function handleAdd() {
  reset()
  title.value = '新增定时任务'
  open.value = true
}

function handleUpdate(row) {
  reset()
  const id = row?.id || ids.value[0]
  getFreeQuerySchedule(id).then(response => {
    form.value = response.data
    title.value = '修改定时任务'
    open.value = true
  })
}

function submitForm() {
  proxy.$refs.scheduleRef.validate(valid => {
    if (!valid) return
    const request = form.value.id ? updateFreeQuerySchedule : addFreeQuerySchedule
    request(form.value).then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  const targetIds = row?.id || ids.value
  proxy.$modal.confirm(`是否确认删除选中的定时任务？`).then(() => {
    return delFreeQuerySchedule(targetIds)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function handleStatusChange(row) {
  const statusText = row.status === '0' ? '启用' : '停用'
  proxy.$modal.confirm(`确认${statusText}“${row.taskName}”吗？`).then(() => {
    return changeFreeQueryScheduleStatus(row.id, row.status)
  }).then(() => {
    proxy.$modal.msgSuccess(`${statusText}成功`)
  }).catch(() => {
    row.status = row.status === '0' ? '1' : '0'
  })
}

function cancel() {
  open.value = false
  reset()
}

function taskTypeLabel(type) {
  return taskTypes.find(item => item.value === type)?.label || type
}

function taskTypeTag(type) {
  return { QUERY_WINDOW: 'primary', DATA_IMPORT: 'success', DATA_CLEANUP: 'warning' }[type] || 'info'
}

function formatTime(value) {
  return value ? String(value).slice(0, 5) : '--:--'
}

getList()
</script>

<style scoped>
.schedule-page :deep(.el-card) {
  border-radius: 6px;
}

.time-range {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.time-range .el-date-editor {
  flex: 1;
}

.unit {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
}

code {
  color: var(--el-text-color-regular);
  font-family: Consolas, monospace;
}

@media (max-width: 680px) {
  .time-range {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
