<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
        <el-form-item label="订单ID" prop="orderId">
          <el-input v-model="queryParams.orderId" placeholder="请输入订单ID" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="queryParams.userId" placeholder="请输入用户ID" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="代理ID" prop="agentId">
          <el-input v-model="queryParams.agentId" placeholder="请输入代理ID" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="仲裁状态" prop="caseStatus">
          <el-select v-model="queryParams.caseStatus" clearable placeholder="请选择状态" style="width: 150px;">
            <el-option label="待处理" value="0" />
            <el-option label="已裁决" value="1" />
            <el-option label="已驳回" value="2" />
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
          <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['server:markAdmin:case:add']">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['server:markAdmin:case:edit']">修改</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="caseList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="ID" prop="id" width="80" align="center" />
        <el-table-column label="订单ID" prop="orderId" min-width="110" />
        <el-table-column label="明细ID" prop="orderItemId" min-width="110" />
        <el-table-column label="用户ID" prop="userId" width="100" align="center" />
        <el-table-column label="代理ID" prop="agentId" width="100" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="caseStatusType(scope.row.caseStatus)" size="small">{{ caseStatusLabel(scope.row.caseStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="问题描述" prop="issueDesc" min-width="180" show-overflow-tooltip />
        <el-table-column label="裁决人" prop="decidedBy" width="100" align="center" />
        <el-table-column label="裁决时间" width="160" align="center">
          <template #default="scope">
            {{ formatDateTime(scope.row.decidedTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="146" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['server:markAdmin:case:query']">查看</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['server:markAdmin:case:edit']">修改</el-button>
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

    <el-dialog v-model="open" :title="title" width="700px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="订单ID" prop="orderId">
          <el-input v-model="form.orderId" />
        </el-form-item>
        <el-form-item label="明细ID" prop="orderItemId">
          <el-input v-model="form.orderItemId" />
        </el-form-item>
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="form.userId" />
        </el-form-item>
        <el-form-item label="代理ID" prop="agentId">
          <el-input v-model="form.agentId" />
        </el-form-item>
        <el-form-item label="仲裁状态" prop="caseStatus">
          <el-radio-group v-model="form.caseStatus">
            <el-radio label="0">待处理</el-radio>
            <el-radio label="1">已裁决</el-radio>
            <el-radio label="2">已驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="问题描述" prop="issueDesc">
          <el-input v-model="form.issueDesc" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="证据内容" prop="evidenceText">
          <el-input v-model="form.evidenceText" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="裁决内容" prop="decisionText">
          <el-input v-model="form.decisionText" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm" v-if="!viewMode">确 定</el-button>
          <el-button @click="open = false">{{ viewMode ? '关 闭' : '取 消' }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MarkArbitrationCase">
import {
  listMarkArbitrationCase,
  getMarkArbitrationCase,
  addMarkArbitrationCase,
  updateMarkArbitrationCase
} from '@/api/server/markAdmin'

const { proxy } = getCurrentInstance()

const showSearch = ref(true)
const loading = ref(false)
const total = ref(0)
const caseList = ref([])
const ids = ref([])
const single = ref(true)
const open = ref(false)
const title = ref('')
const viewMode = ref(false)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderId: null,
    userId: null,
    agentId: null,
    caseStatus: null
  },
  form: {
    id: null,
    orderId: null,
    orderItemId: null,
    userId: null,
    agentId: null,
    caseStatus: '0',
    issueDesc: '',
    evidenceText: '',
    decisionText: '',
    remark: ''
  },
  rules: {
    caseStatus: [{ required: true, message: '请选择状态', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function caseStatusLabel(status) {
  const map = { '0': '待处理', '1': '已裁决', '2': '已驳回' }
  return map[status] || '-'
}

function caseStatusType(status) {
  if (status === '1') return 'success'
  if (status === '2') return 'info'
  return 'warning'
}

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function getList() {
  loading.value = true
  listMarkArbitrationCase(queryParams.value).then((res) => {
    caseList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id)
  single.value = selection.length !== 1
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
    orderId: null,
    orderItemId: null,
    userId: null,
    agentId: null,
    caseStatus: '0',
    issueDesc: '',
    evidenceText: '',
    decisionText: '',
    remark: ''
  }
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetForm()
  viewMode.value = false
  title.value = '新增仲裁工单'
  open.value = true
}

function fillFormAndOpen(id, mode) {
  resetForm()
  getMarkArbitrationCase(id).then((res) => {
    form.value = { ...form.value, ...res.data, caseStatus: res.data?.caseStatus || '0' }
    viewMode.value = mode === 'view'
    title.value = mode === 'view' ? '查看仲裁工单' : '修改仲裁工单'
    open.value = true
  })
}

function handleView(row) {
  fillFormAndOpen(row.id, 'view')
}

function handleUpdate(row) {
  const id = row?.id || ids.value[0]
  if (!id) return
  fillFormAndOpen(id, 'edit')
}

function submitForm() {
  if (viewMode.value) {
    open.value = false
    return
  }
  proxy.$refs.formRef.validate((valid) => {
    if (!valid) return
    const req = form.value.id ? updateMarkArbitrationCase(form.value) : addMarkArbitrationCase(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
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
