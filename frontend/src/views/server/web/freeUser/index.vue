<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="账号" prop="account">
          <el-input v-model="queryParams.account" placeholder="请输入账号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="queryParams.phone" placeholder="请输入手机号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 140px;">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
          <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['server:freeQueryUser:add']">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['server:freeQueryUser:edit']">修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['server:freeQueryUser:remove']">删除</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="ID" align="center" prop="id" width="80" />
        <el-table-column label="账号" align="center" prop="account" min-width="140" />
        <el-table-column label="昵称" align="center" prop="nickName" min-width="120" />
        <el-table-column label="手机号" align="center" prop="phone" min-width="130" />
        <el-table-column label="积分" align="center" prop="points" width="90" />
        <el-table-column label="状态" align="center" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
              {{ scope.row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" min-width="170" />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" min-width="320">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['server:freeQueryUser:edit']">修改</el-button>
            <el-button link type="primary" icon="EditPen" @click="handleAdjust(scope.row)" v-hasPermi="['server:freeQueryUser:adjust']">积分调整</el-button>
            <el-button link type="primary" icon="Lock" @click="handleResetPwd(scope.row)" v-hasPermi="['server:freeQueryUser:resetPwd']">重置密码</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['server:freeQueryUser:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>

    <el-dialog :title="title" v-model="open" width="560px" append-to-body>
      <el-form ref="userRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="account">
          <el-input v-model="form.account" placeholder="请输入账号" maxlength="64" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" placeholder="请输入密码（至少6位）" show-password maxlength="64" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickName">
          <el-input v-model="form.nickName" placeholder="请输入昵称" maxlength="64" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="20" />
        </el-form-item>
        <el-form-item label="初始积分" prop="points" v-if="!form.id">
          <el-input-number v-model="form.points" :min="0" :max="9999999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="item in statusOptions" :key="item.value" :label="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="积分调整" v-model="adjustOpen" width="520px" append-to-body>
      <el-form ref="adjustRef" :model="adjustForm" :rules="adjustRules" label-width="100px">
        <el-form-item label="用户账号">
          <el-input v-model="adjustForm.account" disabled />
        </el-form-item>
        <el-form-item label="当前积分">
          <el-input v-model="adjustForm.currentPoints" disabled />
        </el-form-item>
        <el-form-item label="变动类型" prop="pointType">
          <el-radio-group v-model="adjustForm.pointType">
            <el-radio label="1">增加</el-radio>
            <el-radio label="2">扣减</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="积分值" prop="pointAmount">
          <el-input-number v-model="adjustForm.pointAmount" :min="1" :max="9999999" controls-position="right" />
        </el-form-item>
        <el-form-item label="变动原因" prop="reason">
          <el-input v-model="adjustForm.reason" placeholder="请输入变动原因" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitAdjust">确 定</el-button>
          <el-button @click="adjustOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="重置密码" v-model="resetPwdOpen" width="460px" append-to-body>
      <el-form ref="resetPwdRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="90px">
        <el-form-item label="用户账号">
          <el-input v-model="resetPwdForm.account" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetPwdForm.password" show-password placeholder="请输入新密码（至少6位）" maxlength="64" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitResetPwd">确 定</el-button>
          <el-button @click="resetPwdOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FreeQueryUser">
import {
  listFreeQueryUser,
  getFreeQueryUser,
  addFreeQueryUser,
  updateFreeQueryUser,
  delFreeQueryUser,
  adjustFreeQueryUserPoints,
  resetFreeQueryUserPwd
} from '@/api/server/freeQueryUser'

const { proxy } = getCurrentInstance()

const userList = ref([])
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const open = ref(false)
const adjustOpen = ref(false)
const resetPwdOpen = ref(false)
const title = ref('')

const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' }
]

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    account: '',
    phone: '',
    status: ''
  },
  form: {},
  adjustForm: {},
  resetPwdForm: {},
  rules: {
    account: [{ required: true, message: '账号不能为空', trigger: 'blur' }],
    password: [{ required: true, message: '密码不能为空', trigger: 'blur' }]
  },
  adjustRules: {
    pointType: [{ required: true, message: '请选择变动类型', trigger: 'change' }],
    pointAmount: [{ required: true, message: '积分值不能为空', trigger: 'change' }],
    reason: [{ required: true, message: '变动原因不能为空', trigger: 'blur' }]
  },
  resetPwdRules: {
    password: [
      { required: true, message: '新密码不能为空', trigger: 'blur' },
      { min: 6, message: '密码长度至少6位', trigger: 'blur' }
    ]
  }
})

const { queryParams, form, adjustForm, resetPwdForm, rules, adjustRules, resetPwdRules } = toRefs(data)

function getList() {
  loading.value = true
  listFreeQueryUser(queryParams.value).then((response) => {
    userList.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function reset() {
  form.value = {
    id: null,
    account: '',
    password: '',
    nickName: '',
    phone: '',
    points: 0,
    status: '0',
    remark: ''
  }
  proxy.resetForm('userRef')
}

function cancel() {
  open.value = false
  reset()
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
  ids.value = selection.map((item) => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增免费查询用户'
}

function handleUpdate(row) {
  reset()
  const _id = row?.id || ids.value[0]
  if (!_id) return
  getFreeQueryUser(_id).then((response) => {
    form.value = {
      ...form.value,
      ...response.data,
      password: '',
      points: response.data?.points ?? 0
    }
    open.value = true
    title.value = '修改免费查询用户'
  })
}

function submitForm() {
  proxy.$refs.userRef.validate((valid) => {
    if (!valid) return
    if (form.value.id) {
      updateFreeQueryUser(form.value).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addFreeQueryUser(form.value).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function handleDelete(row) {
  const _ids = row?.id ? [row.id] : ids.value
  if (!_ids || _ids.length === 0) return
  proxy.$modal.confirm(`是否确认删除免费查询用户编号为"${_ids.join(',')}"的数据项？`).then(() => {
    return delFreeQueryUser(_ids.join(','))
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function handleAdjust(row) {
  adjustForm.value = {
    userId: row.id,
    account: row.account,
    currentPoints: row.points ?? 0,
    pointType: '1',
    pointAmount: 1,
    reason: ''
  }
  adjustOpen.value = true
  proxy.resetForm('adjustRef')
}

function submitAdjust() {
  proxy.$refs.adjustRef.validate((valid) => {
    if (!valid) return
    adjustFreeQueryUserPoints({
      userId: adjustForm.value.userId,
      pointType: adjustForm.value.pointType,
      pointAmount: adjustForm.value.pointAmount,
      reason: adjustForm.value.reason
    }).then(() => {
      proxy.$modal.msgSuccess('积分调整成功')
      adjustOpen.value = false
      getList()
    })
  })
}

function handleResetPwd(row) {
  resetPwdForm.value = {
    userId: row.id,
    account: row.account,
    password: ''
  }
  resetPwdOpen.value = true
  proxy.resetForm('resetPwdRef')
}

function submitResetPwd() {
  proxy.$refs.resetPwdRef.validate((valid) => {
    if (!valid) return
    resetFreeQueryUserPwd({
      userId: resetPwdForm.value.userId,
      password: resetPwdForm.value.password
    }).then(() => {
      proxy.$modal.msgSuccess('密码重置成功')
      resetPwdOpen.value = false
    })
  })
}

getList()
</script>
