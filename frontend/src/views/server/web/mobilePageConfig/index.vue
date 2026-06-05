<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
        <el-form-item label="页面编码" prop="pageCode">
          <el-input v-model="queryParams.pageCode" placeholder="请输入页面编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="页面名称" prop="pageName">
          <el-input v-model="queryParams.pageName" placeholder="请输入页面名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 150px;">
            <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
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
          <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['server:mobilePageConfig:add']">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['server:mobilePageConfig:edit']">修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['server:mobilePageConfig:remove']">删除</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="mobilePageConfigList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="ID" align="center" prop="id" width="70" />
        <el-table-column label="页面编码" align="center" prop="pageCode" min-width="120" />
        <el-table-column label="页面名称" align="center" prop="pageName" min-width="120" />
        <el-table-column label="客服电话" align="center" prop="servicePhone" min-width="120" />
        <el-table-column label="二维码地址" align="center" prop="wechatQrUrl" min-width="180" show-overflow-tooltip />
        <el-table-column label="访问链接" align="center" min-width="240">
          <template #default="scope">
            <div class="entry-link-wrap">
              <span class="entry-link-text">{{ buildEntryPath(scope.row.pageCode) }}</span>
              <el-button link type="primary" @click="copyEntryLink(scope.row)">复制</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status" width="90">
          <template #default="scope">
            <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="排序" align="center" prop="sort" width="90" />
        <el-table-column label="备注" align="center" prop="remark" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" min-width="160">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['server:mobilePageConfig:edit']">修改</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['server:mobilePageConfig:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>

    <el-dialog :title="title" v-model="open" width="760px" append-to-body>
      <el-form ref="mobilePageConfigRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="页面编码" prop="pageCode">
              <el-input v-model="form.pageCode" placeholder="仅支持小写字母/数字/-" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="页面名称" prop="pageName">
              <el-input v-model="form.pageName" placeholder="请输入页面名称" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="客服电话" prop="servicePhone">
              <el-input v-model="form.servicePhone" placeholder="请输入客服电话" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="二维码地址" prop="wechatQrUrl">
              <el-input v-model="form.wechatQrUrl" placeholder="支持 /path 或 http(s)://..." maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="首页链接" prop="navHomeUrl">
              <el-input v-model="form.navHomeUrl" placeholder="/ 或 http(s)://..." maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="免费查询链接" prop="navQueryUrl">
              <el-input v-model="form.navQueryUrl" placeholder="/?tab=query 或 http(s)://..." maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="批量查询链接" prop="navBatchUrl">
              <el-input v-model="form.navBatchUrl" placeholder="/batch/ 或 http(s)://..." maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="个人中心链接" prop="navProfileUrl">
              <el-input v-model="form.navProfileUrl" placeholder="/profile/ 或 http(s)://..." maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="结果返回链接" prop="resultBackUrl">
              <el-input v-model="form.resultBackUrl" placeholder="/ 或 http(s)://..." maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" :min="0" :max="999999" controls-position="right" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MobilePageConfig">
import { listMobilePageConfig, getMobilePageConfig, addMobilePageConfig, updateMobilePageConfig, delMobilePageConfig } from '@/api/server/mobilePageConfig'

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict('sys_normal_disable')

const pageCodeReg = /^[a-z0-9-]{2,32}$/
const phoneReg = /^(1[3-9]\d{9}|(0\d{2,3}-?)?\d{7,8}|400-?\d{7}|800-?\d{7}|1[0-9]{1,4})$/

const mobilePageConfigList = ref([])
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const open = ref(false)
const title = ref('')

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    pageCode: '',
    pageName: '',
    status: ''
  },
  form: {},
  rules: {
    pageCode: [
      { required: true, message: '页面编码不能为空', trigger: 'blur' },
      {
        validator: (rule, value, callback) => {
          const code = String(value || '').trim()
          if (!pageCodeReg.test(code)) {
            callback(new Error('页面编码仅支持小写字母、数字、中划线，长度2-32'))
            return
          }
          callback()
        },
        trigger: 'blur'
      }
    ],
    pageName: [
      { required: true, message: '页面名称不能为空', trigger: 'blur' },
      { min: 1, max: 64, message: '页面名称长度不能超过64', trigger: 'blur' }
    ],
    servicePhone: [
      { required: true, message: '客服电话不能为空', trigger: 'blur' },
      {
        validator: (rule, value, callback) => {
          const phone = String(value || '').trim()
          if (!phoneReg.test(phone)) {
            callback(new Error('客服电话格式不正确'))
            return
          }
          callback()
        },
        trigger: 'blur'
      }
    ],
    wechatQrUrl: [
      { required: true, message: '客服二维码地址不能为空', trigger: 'blur' },
      {
        validator: (rule, value, callback) => validateUrlOrPath(value, callback, true),
        trigger: 'blur'
      }
    ],
    navHomeUrl: [{ validator: (rule, value, callback) => validateUrlOrPath(value, callback, false), trigger: 'blur' }],
    navQueryUrl: [{ validator: (rule, value, callback) => validateUrlOrPath(value, callback, false), trigger: 'blur' }],
    navBatchUrl: [{ validator: (rule, value, callback) => validateUrlOrPath(value, callback, false), trigger: 'blur' }],
    navProfileUrl: [{ validator: (rule, value, callback) => validateUrlOrPath(value, callback, false), trigger: 'blur' }],
    resultBackUrl: [{ validator: (rule, value, callback) => validateUrlOrPath(value, callback, false), trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function validateUrlOrPath(value, callback, required) {
  const text = String(value || '').trim()
  if (!text) {
    if (required) {
      callback(new Error('该字段不能为空'))
      return
    }
    callback()
    return
  }
  if (text.startsWith('/') || /^https?:\/\//i.test(text)) {
    callback()
    return
  }
  callback(new Error('仅支持 / 开头路径或 http(s):// 链接'))
}

function getList() {
  loading.value = true
  listMobilePageConfig(queryParams.value).then((response) => {
    mobilePageConfigList.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function reset() {
  form.value = {
    id: null,
    pageCode: '',
    pageName: '',
    servicePhone: '',
    wechatQrUrl: '',
    navHomeUrl: '/',
    navQueryUrl: '/?tab=query',
    navBatchUrl: '/batch/',
    navProfileUrl: '/profile/',
    resultBackUrl: '/',
    status: '0',
    sort: 0,
    remark: ''
  }
  proxy.resetForm('mobilePageConfigRef')
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
  title.value = '新增手机页配置'
}

function handleUpdate(row) {
  reset()
  const id = row?.id || ids.value[0]
  if (!id) return
  getMobilePageConfig(id).then((response) => {
    form.value = {
      ...form.value,
      ...response.data,
      sort: Number(response.data?.sort ?? 0)
    }
    open.value = true
    title.value = '修改手机页配置'
  })
}

function submitForm() {
  proxy.$refs.mobilePageConfigRef.validate((valid) => {
    if (!valid) return
    const payload = {
      ...form.value,
      pageCode: String(form.value.pageCode || '').trim().toLowerCase(),
      pageName: String(form.value.pageName || '').trim(),
      servicePhone: String(form.value.servicePhone || '').trim(),
      wechatQrUrl: String(form.value.wechatQrUrl || '').trim(),
      navHomeUrl: String(form.value.navHomeUrl || '').trim(),
      navQueryUrl: String(form.value.navQueryUrl || '').trim(),
      navBatchUrl: String(form.value.navBatchUrl || '').trim(),
      navProfileUrl: String(form.value.navProfileUrl || '').trim(),
      resultBackUrl: String(form.value.resultBackUrl || '').trim(),
      sort: Number(form.value.sort ?? 0)
    }
    if (payload.id) {
      updateMobilePageConfig(payload).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addMobilePageConfig(payload).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function handleDelete(row) {
  const selectedIds = row?.id ? [row.id] : ids.value
  if (!selectedIds.length) return
  proxy.$modal.confirm('是否确认删除手机页配置编号为\"' + selectedIds.join(',') + '\"的数据项？').then(() => {
    return delMobilePageConfig(selectedIds.join(','))
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function buildEntryPath(pageCode) {
  const code = String(pageCode || '').trim()
  if (!code) return ''
  return '/mobile-h5/?page=' + encodeURIComponent(code)
}

function buildEntryFullUrl(pageCode) {
  const path = buildEntryPath(pageCode)
  if (!path) return ''
  const origin = window.location && window.location.origin ? window.location.origin : ''
  return origin ? origin + path : path
}

function copyEntryLink(row) {
  const text = buildEntryFullUrl(row?.pageCode)
  if (!text) {
    proxy.$modal.msgWarning('页面编码为空，无法复制')
    return
  }
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(() => {
      proxy.$modal.msgSuccess('访问链接已复制')
    }).catch(() => {
      fallbackCopy(text)
    })
    return
  }
  fallbackCopy(text)
}

function fallbackCopy(text) {
  const input = document.createElement('textarea')
  input.value = text
  input.style.position = 'fixed'
  input.style.opacity = '0'
  document.body.appendChild(input)
  input.select()
  try {
    document.execCommand('copy')
    proxy.$modal.msgSuccess('访问链接已复制')
  } catch (e) {
    proxy.$modal.msgError('复制失败，请手动复制')
  } finally {
    document.body.removeChild(input)
  }
}

getList()
</script>

<style scoped>
.entry-link-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.entry-link-text {
  display: inline-block;
  max-width: 190px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
