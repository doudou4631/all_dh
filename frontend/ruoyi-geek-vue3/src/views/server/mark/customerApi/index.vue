<template>
  <div class="app-container">
    <div class="toolbar">
      <div>
        <h2>客户 API 管理</h2>
        <span>客户订单会分配给配置的责任代理；Secret 仅在创建或轮换时展示一次</span>
      </div>
      <el-button v-hasPermi="['server:markCustomerApi:add']" type="primary" :icon="Plus" @click="openCreate">新增客户 API</el-button>
    </div>

    <el-form :inline="true" @submit.prevent>
      <el-form-item label="名称"><el-input v-model="query.name" clearable @keyup.enter="load" /></el-form-item>
      <el-form-item label="状态"><el-select v-model="query.status" clearable style="width: 120px"><el-option label="启用" value="0"/><el-option label="停用" value="1"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" :icon="Search" @click="load">查询</el-button></el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="apiKey" label="API Key" min-width="230" />
      <el-table-column prop="userName" label="客户账号" min-width="120" />
      <el-table-column prop="assignedAgentName" label="责任代理" min-width="120"><template #default="{ row }">{{ row.assignedAgentName || '-' }}</template></el-table-column>
      <el-table-column label="授权平台" min-width="200"><template #default="{ row }"><el-tag v-for="code in row.platformCodes || []" :key="code" class="platform-tag">{{ platformLabel(code) }}</el-tag></template></el-table-column>
      <el-table-column prop="expireTime" label="到期时间" min-width="170"><template #default="{ row }">{{ row.expireTime || '永不过期' }}</template></el-table-column>
      <el-table-column prop="lastUsedTime" label="最近调用" min-width="170"><template #default="{ row }">{{ row.lastUsedTime || '-' }}</template></el-table-column>
      <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === '0' ? 'success' : 'danger'">{{ row.status === '0' ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="310" fixed="right"><template #default="{ row }">
        <el-button v-hasPermi="['server:markCustomerApi:edit']" link type="primary" @click="openEdit(row)">编辑</el-button>
        <el-button v-hasPermi="['server:markCustomerApi:edit']" link :type="row.status === '0' ? 'danger' : 'success'" @click="toggle(row)">{{ row.status === '0' ? '停用' : '启用' }}</el-button>
        <el-button v-hasPermi="['server:markCustomerApi:rotate']" link type="warning" @click="rotate(row)">轮换 Secret</el-button>
        <el-button v-hasPermi="['server:markCustomerApi:remove']" link type="danger" :loading="removingId === row.id" :disabled="removingId !== null" @click="remove(row)">删除</el-button>
      </template></el-table-column>
    </el-table>

    <el-dialog v-model="formOpen" :title="form.id ? '编辑客户 API' : '新增客户 API'" width="620px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="105px">
        <el-form-item label="API 名称" prop="name"><el-input v-model="form.name" maxlength="64" /></el-form-item>
        <el-form-item label="客户账号" prop="userName"><el-input v-model="form.userName" :disabled="Boolean(form.id)" maxlength="30" placeholder="例如 customer001" /></el-form-item>
        <el-form-item label="责任代理" prop="assignedAgentId"><el-select v-model="form.assignedAgentId" filterable placeholder="请选择负责处理该客户订单的代理" style="width: 100%"><el-option v-for="agent in agents" :key="agent.userId" :label="agentLabel(agent)" :value="agent.userId" /></el-select></el-form-item>
        <el-form-item label="授权平台" prop="platformCodes"><el-select v-model="form.platformCodes" multiple filterable placeholder="请选择客户可提交的平台" style="width: 100%"><el-option v-for="platform in platforms" :key="platform.platformCode" :label="platformLabel(platform.platformCode)" :value="platform.platformCode" /></el-select></el-form-item>
        <el-form-item label="到期时间"><el-date-picker v-model="form.expireTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" clearable style="width: 100%" /></el-form-item>
        <el-alert type="info" :closable="false" title="修改责任代理只影响后续客户 API 订单，历史订单仍保留原处理代理。" />
      </el-form>
      <template #footer><el-button @click="formOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="secretOpen" title="请立即保存客户 API Secret" width="650px" :close-on-click-modal="false" append-to-body>
      <el-alert type="warning" :closable="false" title="关闭后无法再次查看，只能轮换生成新的 Secret。" />
      <el-descriptions :column="1" border class="secret"><el-descriptions-item label="API Key">{{ secret.apiKey }}</el-descriptions-item><el-descriptions-item label="API Secret"><span class="secret-value">{{ secret.apiSecret }}</span></el-descriptions-item></el-descriptions>
      <template #footer><el-button type="primary" @click="secretOpen = false">我已保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="MarkCustomerApi">
import { Plus, Search } from '@element-plus/icons-vue'
import {
  createMarkCustomerApiKey,
  deleteMarkCustomerApiKey,
  listMarkCustomerApiAgents,
  listMarkCustomerApiKeys,
  listMarkCustomerApiPlatforms,
  rotateMarkCustomerApiSecret,
  updateMarkCustomerApiKey,
  updateMarkCustomerApiKeyStatus
} from '@/api/server/markCustomerApi'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const saving = ref(false)
const removingId = ref(null)
const formOpen = ref(false)
const secretOpen = ref(false)
const rows = ref([])
const agents = ref([])
const platforms = ref([])
const query = reactive({ name: '', status: '' })
const form = reactive({ id: null, name: '', userName: '', assignedAgentId: null, platformCodes: [], expireTime: null })
const secret = reactive({ apiKey: '', apiSecret: '' })
const rules = {
  name: [{ required: true, message: '请输入 API 名称', trigger: 'blur' }],
  userName: [{ required: true, message: '请输入客户账号', trigger: 'blur' }],
  assignedAgentId: [{ required: true, message: '请选择责任代理', trigger: 'change' }],
  platformCodes: [{ type: 'array', required: true, min: 1, message: '请选择至少一个平台', trigger: 'change' }]
}

function platformLabel(code) {
  const platform = platforms.value.find((item) => item.platformCode === code)
  return platform ? platform.platformName + '（' + code + '）' : code
}
function agentLabel(agent) {
  return agent.nickName && agent.nickName !== agent.userName ? agent.userName + '（' + agent.nickName + '）' : agent.userName
}
async function load() {
  loading.value = true
  try {
    const response = await listMarkCustomerApiKeys(query)
    rows.value = response.data || []
  } finally {
    loading.value = false
  }
}
async function loadOptions() {
  const results = await Promise.all([listMarkCustomerApiAgents(), listMarkCustomerApiPlatforms()])
  agents.value = results[0].data || []
  platforms.value = results[1].data || []
}
function resetForm() {
  Object.assign(form, { id: null, name: '', userName: '', assignedAgentId: null, platformCodes: [], expireTime: null })
}
function openCreate() {
  resetForm()
  formOpen.value = true
  nextTick(() => proxy.$refs.formRef?.clearValidate())
}
function openEdit(row) {
  Object.assign(form, { id: row.id, name: row.name, userName: row.userName, assignedAgentId: row.assignedAgentId, platformCodes: [...(row.platformCodes || [])], expireTime: row.expireTime || null })
  formOpen.value = true
  nextTick(() => proxy.$refs.formRef?.clearValidate())
}
function showSecret(data) {
  Object.assign(secret, data || {})
  secretOpen.value = true
}
function save() {
  proxy.$refs.formRef.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (form.id) {
        await updateMarkCustomerApiKey(form)
        proxy.$modal.msgSuccess('保存成功')
      } else {
        const response = await createMarkCustomerApiKey(form)
        showSecret(response.data)
      }
      formOpen.value = false
      await load()
    } finally {
      saving.value = false
    }
  })
}
async function toggle(row) {
  await proxy.$modal.confirm('确认' + (row.status === '0' ? '停用 ' : '启用 ') + row.name + ' 吗？')
  await updateMarkCustomerApiKeyStatus({ id: row.id, status: row.status === '0' ? '1' : '0' })
  proxy.$modal.msgSuccess('状态已更新')
  load()
}
async function rotate(row) {
  await proxy.$modal.confirm('轮换 ' + row.name + ' 的 Secret 后，旧 Secret 会立即失效。')
  const response = await rotateMarkCustomerApiSecret(row.id)
  showSecret(response.data)
}
async function remove(row) {
  if (removingId.value !== null) return
  const confirmed = await proxy.$modal.confirm('确认删除客户 API「' + row.name + '」吗？删除后此 Key 和 Secret 将无法继续调用接口，历史订单、积分及调用记录保留。').then(() => true).catch(() => false)
  if (!confirmed || removingId.value !== null) return
  removingId.value = row.id
  try {
    await deleteMarkCustomerApiKey(row.id)
    proxy.$modal.msgSuccess('删除成功')
    await load()
  } finally {
    removingId.value = null
  }
}

onMounted(async () => {
  await loadOptions()
  await load()
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.toolbar h2 { margin: 0 0 4px; font-size: 20px; }
.toolbar span { font-size: 12px; color: var(--el-text-color-secondary); }
.platform-tag { margin: 2px 4px 2px 0; }
.secret { margin-top: 16px; }
.secret-value { word-break: break-all; font-family: monospace; }
</style>
