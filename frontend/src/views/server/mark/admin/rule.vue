<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="queryParams.ruleName" placeholder="请输入规则名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="规则键" prop="ruleKey">
          <el-input v-model="queryParams.ruleKey" placeholder="请输入规则键" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" clearable placeholder="请选择状态" style="width: 150px;">
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
          <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['server:markAdmin:rule:add']">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['server:markAdmin:rule:edit']">修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['server:markAdmin:rule:remove']">删除</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="ruleList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="ID" prop="id" width="80" align="center" />
        <el-table-column label="规则名称" prop="ruleName" min-width="140" show-overflow-tooltip />
        <el-table-column label="规则键" prop="ruleKey" min-width="180" show-overflow-tooltip />
        <el-table-column label="规则值" prop="ruleValue" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="86" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="small">
              {{ scope.row.status === '0' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160" align="center">
          <template #default="scope">
            {{ formatDateTime(scope.row.updateTime || scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['server:markAdmin:rule:edit']">修改</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['server:markAdmin:rule:remove']">删除</el-button>
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
    </el-card>

    <el-dialog v-model="open" :title="title" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" maxlength="100" />
        </el-form-item>
        <el-form-item label="规则键" prop="ruleKey">
          <el-input v-model="form.ruleKey" maxlength="100" />
        </el-form-item>
        <el-form-item label="规则值" prop="ruleValue">
          <el-input v-model="form.ruleValue" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="open = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MarkGovernRule">
import {
  listMarkGovernRule,
  getMarkGovernRule,
  addMarkGovernRule,
  updateMarkGovernRule,
  delMarkGovernRule
} from '@/api/server/markAdmin'

const { proxy } = getCurrentInstance()

const showSearch = ref(true)
const loading = ref(false)
const total = ref(0)
const ruleList = ref([])
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const open = ref(false)
const title = ref('')

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    ruleName: null,
    ruleKey: null,
    status: null
  },
  form: {
    id: null,
    ruleName: '',
    ruleKey: '',
    ruleValue: '',
    status: '0',
    remark: ''
  },
  rules: {
    ruleName: [{ required: true, message: '规则名称不能为空', trigger: 'blur' }],
    ruleKey: [{ required: true, message: '规则键不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function getList() {
  loading.value = true
  listMarkGovernRule(queryParams.value).then((res) => {
    ruleList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function resetForm() {
  form.value = {
    id: null,
    ruleName: '',
    ruleKey: '',
    ruleValue: '',
    status: '0',
    remark: ''
  }
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetForm()
  open.value = true
  title.value = '新增治理规则'
}

function handleUpdate(row) {
  resetForm()
  const id = row?.id || ids.value[0]
  getMarkGovernRule(id).then((res) => {
    form.value = { ...res.data, status: res.data?.status || '0' }
    open.value = true
    title.value = '修改治理规则'
  })
}

function submitForm() {
  proxy.$refs.formRef.validate((valid) => {
    if (!valid) return
    const req = form.value.id ? updateMarkGovernRule(form.value) : addMarkGovernRule(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  const delIds = row?.id ? [row.id] : ids.value
  if (!delIds.length) return
  proxy.$modal.confirm(`确认删除选中的 ${delIds.length} 条规则吗？`).then(() => {
    return delMarkGovernRule(delIds.join(','))
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.mt10 {
  margin-top: 10px;
}

.mb8 {
  margin-bottom: 8px;
}
</style>
