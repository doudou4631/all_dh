<template>
  <div class="app-container interface-management">
    <el-form ref="queryRef" :model="queryParams" :inline="true" class="filter-row">
      <el-form-item label="平台名称" prop="platformName">
        <el-input v-model="queryParams.platformName" placeholder="请输入平台名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="平台编码" prop="platformKey">
        <el-input v-model="queryParams.platformKey" placeholder="请输入平台编码" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="启用" value="0" />
          <el-option label="禁用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item class="filter-actions">
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
      <el-form-item class="add-action">
        <el-button type="primary" icon="Plus" @click="handleAdd" v-hasPermi="['server:platformConfig:add']">
          新增平台
        </el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="platformList" border stripe>
      <el-table-column label="序号" type="index" width="80" align="center" :index="tableIndex" />
      <el-table-column label="排序" prop="bindingSort" width="76" align="center" />
      <el-table-column label="平台编码" prop="platformKey" min-width="160" align="center" />
      <el-table-column label="API名称" prop="platformName" min-width="180" align="center">
        <template #default="scope">
          <el-tag :type="tagType(scope.$index)" effect="light">{{ scope.row.platformName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="绑定平台" min-width="180" align="center">
        <template #default="scope">
          <el-tag :type="bindingEnabled(scope.row) ? 'success' : 'warning'" effect="plain">
            {{ bindingName(scope.row) }}{{ bindingEnabled(scope.row) ? '' : '（已关闭）' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="接口状态" prop="status" width="140" align="center">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            inline-prompt
            active-text="启用"
            inactive-text="禁用"
            @change="handleStatusChange(scope.row)"
            v-hasPermi="['server:platformConfig:edit']"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['server:platformConfig:edit']">编辑</el-button>
          <el-button
            link
            type="success"
            icon="Connection"
            :loading="testingId === scope.row.id"
            @click="handleTest(scope.row)"
            v-hasPermi="['server:platformConfig:test']"
          >测试连接</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['server:platformConfig:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="680px" append-to-body destroy-on-close>
      <el-form ref="platformFormRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="API名称" prop="platformName">
              <el-input v-model="form.platformName" placeholder="请输入API固定名称" maxlength="100" :disabled="Boolean(form.id)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="绑定平台" prop="platformKey">
              <el-select v-model="form.platformKey" placeholder="请选择代理处理平台" style="width: 100%">
                <el-option
                  v-for="item in bindingOptions"
                  :key="item.platformKey"
                  :label="item.platformName"
                  :value="item.platformKey"
                  :disabled="!item.enabled"
                >
                  <span>{{ item.platformName }}{{ item.enabled ? '' : '（已关闭）' }}</span>
                  <span class="binding-code">{{ item.platformKey }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="接口地址" prop="url">
          <el-input v-model="form.url" placeholder="https://example.com/api/query" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-switch v-model="form.status" active-value="0" inactive-value="1" active-text="启用" inactive-text="禁用" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="超时(ms)" prop="timeoutMs">
              <el-input-number v-model="form.timeoutMs" :min="1000" :max="15000" :step="500" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="重试次数" prop="retryCount">
              <el-input-number v-model="form.retryCount" :min="0" :max="10" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="InterfaceManagement">
import {
  addPlatformConfig,
  delPlatformConfig,
  getPlatformConfig,
  listPlatformBindingOptions,
  listPlatformConfig,
  testPlatformConfig,
  updatePlatformConfig
} from '@/api/server/platformConfig'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const submitting = ref(false)
const testingId = ref(null)
const dialogOpen = ref(false)
const dialogTitle = ref('')
const total = ref(0)
const platformList = ref([])
const bindingOptions = ref([])

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10, platformName: null, platformKey: null, status: null },
  form: {},
  rules: {
    platformKey: [{ required: true, message: '请选择绑定平台', trigger: 'change' }],
    platformName: [{ required: true, message: 'API名称不能为空', trigger: 'blur' }],
    url: [
      { required: true, message: '接口地址不能为空', trigger: 'blur' },
      { pattern: /^https?:\/\//i, message: '接口地址必须以 http:// 或 https:// 开头', trigger: 'blur' }
    ]
  }
})
const { queryParams, form, rules } = toRefs(data)

function emptyForm() {
  return {
    id: null,
    platformKey: '',
    platformName: '',
    url: '',
    status: '0',
    requestIntervalMs: 1000,
    timeoutMs: 5000,
    retryCount: 3,
    preActionType: 0,
    concurrencyLimit: 1,
    sort: 0,
    remark: ''
  }
}

function getList() {
  loading.value = true
  listPlatformConfig(queryParams.value).then((response) => {
    platformList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => { loading.value = false })
}

function getBindingOptions() {
  return listPlatformBindingOptions().then((response) => {
    bindingOptions.value = Array.isArray(response.data) ? response.data : []
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleAdd() {
  form.value = emptyForm()
  dialogTitle.value = '新增平台'
  dialogOpen.value = true
}

function handleUpdate(row) {
  getPlatformConfig(row.id).then((response) => {
    form.value = { ...emptyForm(), ...(response.data || {}) }
    dialogTitle.value = '编辑平台'
    dialogOpen.value = true
  })
}

function submitForm() {
  proxy.$refs.platformFormRef.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    const request = form.value.id ? updatePlatformConfig : addPlatformConfig
    try {
      await request(form.value)
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    } finally {
      submitting.value = false
    }
  })
}

async function handleStatusChange(row) {
  const nextLabel = row.status === '0' ? '启用' : '禁用'
  try {
    await proxy.$modal.confirm(`确认要${nextLabel}平台“${row.platformName}”吗？`)
    await updatePlatformConfig({ id: row.id, status: row.status })
    proxy.$modal.msgSuccess(`${nextLabel}成功`)
  } catch (error) {
    row.status = row.status === '0' ? '1' : '0'
  }
}

async function handleTest(row) {
  testingId.value = row.id
  try {
    const response = await testPlatformConfig(row.id)
    const elapsed = response.data?.elapsedMs
    proxy.$modal.msgSuccess(`${response.msg || '连接成功'}${elapsed != null ? `，耗时 ${elapsed}ms` : ''}`)
  } finally {
    testingId.value = null
  }
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除平台“${row.platformName}”吗？删除后其编码将无法用于查询调用。`).then(() => {
    return delPlatformConfig(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function tableIndex(index) {
  return (queryParams.value.pageNum - 1) * queryParams.value.pageSize + index + 1
}

function tagType(index) {
  return ['primary', 'success', 'warning', 'danger', 'info'][index % 5]
}

function bindingOption(platformKey) {
  return bindingOptions.value.find((item) => item.platformKey === platformKey)
}

function bindingName(row) {
  return row.bindingPlatformName || bindingOption(row.platformKey)?.platformName || '未绑定'
}

function bindingEnabled(row) {
  if (typeof row.bindingEnabled === 'boolean') return row.bindingEnabled
  return Boolean(bindingOption(row.platformKey)?.enabled)
}

getBindingOptions()
getList()
</script>

<style scoped>
.interface-management { min-width: 760px; }
.filter-row { display: flex; align-items: center; gap: 0 4px; margin-bottom: 16px; padding-bottom: 4px; border-bottom: 1px solid var(--el-border-color-lighter); }
.filter-row :deep(.el-form-item) { margin-bottom: 12px; }
.filter-row :deep(.el-input) { width: 190px; }
.add-action { margin-left: auto; }
:deep(.el-tag) { min-width: 88px; }
.binding-code { float: right; margin-left: 24px; color: var(--el-text-color-secondary); }
@media (max-width: 1100px) {
  .filter-row { flex-wrap: wrap; }
  .add-action { margin-left: 0; }
}
</style>
