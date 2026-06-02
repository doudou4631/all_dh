<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="84px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="queryParams.templateName" placeholder="请输入模板名称" clearable @keyup.enter="handleQuery" />
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
          <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['server:markTemplate:add']">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['server:markTemplate:edit']">修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['server:markTemplate:remove']">删除</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="templateList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="ID" prop="id" width="80" align="center" />
        <el-table-column label="模板名称" prop="templateName" min-width="180" show-overflow-tooltip />
        <el-table-column label="平台范围" min-width="300" show-overflow-tooltip>
          <template #default="scope">
            <el-tag v-for="item in parseTemplatePlatforms(scope.row.templateInfo)" :key="item.code" size="small" style="margin-right: 6px; margin-bottom: 4px;">
              {{ item.name || platformNameMap[item.code] || item.code }}
            </el-tag>
            <span v-if="parseTemplatePlatforms(scope.row.templateInfo).length === 0">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['server:markTemplate:edit']">修改</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['server:markTemplate:remove']">删除</el-button>
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

    <el-dialog v-model="open" :title="title" width="760px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" maxlength="100" />
        </el-form-item>
        <el-form-item label="平台范围" prop="templateInfo">
          <el-checkbox-group v-model="selectedPlatformCodes">
            <el-checkbox
              v-for="item in platformOptions"
              :key="item.code"
              :label="item.code"
              style="margin-right: 18px; margin-bottom: 6px;"
            >
              <span class="platform-option-label">{{ item.name }}</span>
              <el-button
                v-if="canRemoveCustomOption(item.code)"
                link
                type="danger"
                size="small"
                class="platform-option-remove"
                @click.stop.prevent="handleRemoveCustomPlatform(item.code)"
              >
                删除
              </el-button>
            </el-checkbox>
          </el-checkbox-group>
          <div class="platform-custom-editor">
            <el-input
              v-model="customPlatformForm.code"
              placeholder="新增平台编码（如 new_platform）"
              clearable
              @keyup.enter="handleAddCustomPlatform"
            />
            <el-input
              v-model="customPlatformForm.name"
              placeholder="新增平台名称（选填）"
              clearable
              @keyup.enter="handleAddCustomPlatform"
            />
            <el-button type="primary" plain icon="Plus" @click="handleAddCustomPlatform">新增平台</el-button>
          </div>
          <div class="platform-custom-tip">新增平台会加入当前模板候选并自动勾选。</div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
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

<script setup name="MarkTemplate">
import { addMarkTemplate, delMarkTemplate, getMarkTemplate, listMarkPlatformOptions, listMarkTemplate, updateMarkTemplate } from '@/api/server/markTemplate'

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict('sys_normal_disable')

const platformOptions = ref([])
const platformNameMap = ref({})
const systemPlatformCodes = ref([])
const customPlatformForm = reactive({
  code: '',
  name: ''
})
const customAddedPlatformCodes = ref([])

const showSearch = ref(true)
const loading = ref(false)
const total = ref(0)
const templateList = ref([])
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const open = ref(false)
const title = ref('')
const selectedPlatformCodes = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    templateName: null,
    status: null
  },
  form: {
    id: null,
    templateName: '',
    templateInfo: '',
    status: '0',
    remark: ''
  },
  rules: {
    templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }],
    templateInfo: [{ required: true, message: '请至少选择一个平台', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function parseTemplatePlatforms(templateInfo) {
  if (!templateInfo) return []
  try {
    const arr = typeof templateInfo === 'string' ? JSON.parse(templateInfo) : templateInfo
    if (!Array.isArray(arr)) return []
    const seen = new Map()
    arr.forEach((item) => {
      const code = String(typeof item === 'string' ? item : item?.platformCode || item?.code || item?.value || '').trim()
      const name = String(typeof item === 'object' ? item?.platformName || item?.name || item?.label || '' : '').trim()
      if (!code) return
      if (!seen.has(code)) {
        seen.set(code, { code, name })
        return
      }
      if (!seen.get(code).name && name) {
        seen.set(code, { code, name })
      }
    })
    return Array.from(seen.values())
  } catch {
    return []
  }
}

function parseTemplateCodes(templateInfo) {
  return parseTemplatePlatforms(templateInfo).map((item) => item.code)
}

function rebuildPlatformNameMap() {
  platformNameMap.value = platformOptions.value.reduce((acc, item) => {
    acc[item.code] = item.name
    return acc
  }, {})
}

function mergePlatformOptions(extraOptions = []) {
  const merged = new Map()
  platformOptions.value.forEach((item) => {
    if (!item?.code) return
    merged.set(item.code, item.name || item.code)
  })
  extraOptions.forEach((item) => {
    const code = String(item?.code || '').trim()
    if (!code) return
    const name = String(item?.name || '').trim() || code
    if (!merged.has(code)) {
      merged.set(code, name)
      return
    }
    const currentName = merged.get(code)
    if ((!currentName || currentName === code) && name) {
      merged.set(code, name)
    }
  })
  platformOptions.value = Array.from(merged.entries()).map(([code, name]) => ({ code, name }))
  rebuildPlatformNameMap()
}

function ensureTemplateOptions(templateInfo) {
  const extraOptions = parseTemplatePlatforms(templateInfo).map((item) => ({
    code: item.code,
    name: item.name || platformNameMap.value[item.code] || item.code
  }))
  if (extraOptions.length > 0) {
    mergePlatformOptions(extraOptions)
  }
}

function handleAddCustomPlatform() {
  const code = String(customPlatformForm.code || '').trim()
  if (!code) {
    proxy.$modal.msgError('请先输入平台编码')
    return
  }
  if (!/^[a-zA-Z0-9_:-]+$/.test(code)) {
    proxy.$modal.msgError('平台编码仅支持字母、数字、下划线、冒号、中划线')
    return
  }
  const name = String(customPlatformForm.name || '').trim() || code
  const existed = platformOptions.value.some((item) => item.code === code)
  mergePlatformOptions([{ code, name }])
  if (!existed) {
    customAddedPlatformCodes.value = Array.from(new Set([...(customAddedPlatformCodes.value || []), code]))
  }
  selectedPlatformCodes.value = Array.from(new Set([...(selectedPlatformCodes.value || []), code]))
  customPlatformForm.code = ''
  customPlatformForm.name = ''
  proxy.$modal.msgSuccess(existed ? '平台已存在，已自动勾选' : '新增平台成功并已勾选')
}

function canRemoveCustomOption(code) {
  return customAddedPlatformCodes.value.includes(code)
}

function handleRemoveCustomPlatform(code) {
  const targetCode = String(code || '').trim()
  if (!targetCode || !canRemoveCustomOption(targetCode)) return
  platformOptions.value = platformOptions.value.filter((item) => item.code !== targetCode)
  selectedPlatformCodes.value = (selectedPlatformCodes.value || []).filter((item) => item !== targetCode)
  customAddedPlatformCodes.value = customAddedPlatformCodes.value.filter((item) => item !== targetCode)
  if (customPlatformForm.code === targetCode) {
    customPlatformForm.code = ''
  }
  rebuildPlatformNameMap()
  proxy.$modal.msgSuccess('已删除自定义平台')
}

function loadPlatformOptions() {
  return listMarkPlatformOptions().then((res) => {
    const options = Array.isArray(res?.data) ? res.data : []
    platformOptions.value = options
      .map((item) => {
        const code = typeof item?.code === 'string' ? item.code.trim() : ''
        const name = typeof item?.name === 'string' && item.name.trim() ? item.name.trim() : code
        return { code, name }
      })
      .filter((item) => item.code)
    systemPlatformCodes.value = platformOptions.value.map((item) => item.code)
    rebuildPlatformNameMap()
  }).catch(() => {
    platformOptions.value = []
    systemPlatformCodes.value = []
    platformNameMap.value = {}
  })
}
function buildTemplateInfoPayload(selectedCodes) {
  const systemSet = new Set(systemPlatformCodes.value || [])
  return (selectedCodes || [])
    .map((code) => String(code || '').trim())
    .filter((code) => code.length > 0)
    .map((code) => {
      if (systemSet.has(code)) return code
      return {
        platformCode: code,
        platformName: platformNameMap.value[code] || code
      }
    })
}
function getList() {
  loading.value = true
  listMarkTemplate(queryParams.value).then((res) => {
    templateList.value = res.rows || []
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
    templateName: '',
    templateInfo: '',
    status: '0',
    remark: ''
  }
  selectedPlatformCodes.value = []
  customPlatformForm.code = ''
  customPlatformForm.name = ''
  customAddedPlatformCodes.value = []
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetForm()
  loadPlatformOptions().finally(() => {
    title.value = '新增标记模板'
    open.value = true
  })
}

function handleUpdate(row) {
  resetForm()
  const id = row?.id || ids.value[0]
  if (!id) return
  loadPlatformOptions().finally(() => {
    getMarkTemplate(id).then((res) => {
      form.value = { ...form.value, ...res.data, status: res.data?.status || '0' }
      ensureTemplateOptions(form.value.templateInfo)
      selectedPlatformCodes.value = parseTemplateCodes(form.value.templateInfo)
      title.value = '修改标记模板'
      open.value = true
    })
  })
}

function submitForm() {
  const selectedCodes = selectedPlatformCodes.value || []
  const payload = buildTemplateInfoPayload(selectedCodes)
  form.value.templateInfo = payload.length > 0 ? JSON.stringify(payload) : ''
  proxy.$refs.formRef.validate((valid) => {
    if (!valid) return
    const req = form.value.id ? updateMarkTemplate(form.value) : addMarkTemplate(form.value)
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
  proxy.$modal.confirm(`确认删除选中的 ${delIds.length} 条模板吗？`).then(() => {
    return delMarkTemplate(delIds.join(','))
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

onMounted(() => {
  loadPlatformOptions()
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

.platform-custom-editor {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.platform-custom-tip {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.platform-option-label {
  margin-right: 6px;
}

.platform-option-remove {
  padding: 0;
  min-height: auto;
}
</style>
