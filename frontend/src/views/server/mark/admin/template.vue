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
        <el-table-column label="默认模板" width="130" align="center">
          <template #default="scope">
            <el-tag v-if="isDefaultFlag(scope.row.isDefault)" type="success" size="small">默认</el-tag>
            <el-button
              v-else-if="canEditTemplate"
              link
              type="primary"
              :loading="defaultUpdatingId === scope.row.id"
              @click="handleSetDefault(scope.row)"
            >
              设为默认
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="平台范围" min-width="360" show-overflow-tooltip>
          <template #default="scope">
            <el-tag v-for="item in parseTemplatePlatforms(scope.row.templateInfo)" :key="item.code" size="small" style="margin-right: 6px; margin-bottom: 4px;">
              {{ item.name || platformNameMap[item.code] || item.code }} [{{ item.code }}]（{{ item.unitPrice }}分）
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

    <el-dialog v-model="open" :title="title" width="980px" append-to-body>
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
              <span class="platform-option-label" :title="`${resolvePlatformCodeRemark(item.code)} (${item.code})`">
                {{ item.name }}
                <span class="platform-option-code">[{{ item.code }}]</span>
              </span>
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
            <el-input-number
              v-model="customPlatformForm.unitPrice"
              :min="1"
              :step="1"
              :precision="0"
              controls-position="right"
            />
            <el-button type="primary" plain icon="Plus" @click="handleAddCustomPlatform">新增平台</el-button>
          </div>
          <div class="platform-custom-tip">新增平台会加入当前模板候选并自动勾选，且可设置每号码扣积分。绑定平台编码保存后不可在此修改，仅展示名称可调整。</div>
          <div v-if="selectedPlatformCodes.length > 0" class="platform-unit-editor">
            <div v-for="(code, index) in selectedPlatformCodes" :key="`unit-${code}`" class="platform-unit-row">
              <span class="platform-code-label">绑定编码</span>
              <el-input
                :model-value="code"
                class="platform-unit-code-input"
                disabled
              />
              <span class="platform-code-label">平台备注</span>
              <el-input
                :model-value="resolvePlatformCodeRemark(code)"
                class="platform-unit-remark-input"
                disabled
              />
              <el-input
                v-model="platformDisplayNameMap[code]"
                class="platform-unit-name-input"
                placeholder="平台展示名称"
                maxlength="64"
              />
              <el-input-number
                v-model="platformUnitPriceMap[code]"
                :min="1"
                :step="1"
                :precision="0"
                controls-position="right"
              />
              <span class="platform-unit-suffix">积分/号码</span>
              <span class="platform-order-label">排序</span>
              <el-input-number
                :model-value="index + 1"
                :min="1"
                :max="selectedPlatformCodes.length"
                :step="1"
                :precision="0"
                controls-position="right"
                class="platform-order-input"
                @change="(value) => handlePlatformOrderChange(code, value)"
              />
              <el-button
                v-if="canRemoveCustomOption(code)"
                link
                type="danger"
                class="platform-unit-remove"
                @click.stop.prevent="handleRemoveCustomPlatform(code)"
              >
                移除
              </el-button>
            </div>
          </div>
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
const canEditTemplate = computed(() => proxy.$auth.hasPermi('server:markTemplate:edit'))

const platformOptions = ref([])
const platformNameMap = ref({})
const platformSystemMap = ref({})
const customPlatformForm = reactive({
  code: '',
  name: '',
  unitPrice: 1
})
const platformUnitPriceMap = reactive({})
const platformDisplayNameMap = reactive({})

const STANDARD_PLATFORM_REMARKS = {
  taidixiong: '泰迪熊',
  td_gaopin: '泰迪熊高频',
  td_second: '泰迪熊二次',
  tengxun: '腾讯',
  tencent_mark: '腾讯速解',
  tencent: '腾讯',
  tx: '腾讯',
  txwz: '腾讯',
  sanliuling: '360',
  '360': '360',
  qihu_first: '360首次/覆盖',
  qihu_second: '360二次',
  baidu: '百度',
  sghmt: '搜狗号码通',
  sougou: '搜狗（编码应为 sghmt）',
  yidonggaopin: '移动高频',
  mobile_gaopin: '移动高频',
  xiaomi: '小米手机',
  ltgj: '联通管家',
  liantongguanjia: '联通管家（编码应为 ltgj）',
  dianhuabang: '电话邦'
}

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
const defaultUpdatingId = ref(null)

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
    isDefault: '0',
    status: '0',
    remark: ''
  },
  rules: {
    templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }],
    templateInfo: [{ required: true, message: '请至少选择一个平台', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)
function normalizeUnitPrice(value, fallback = 1) {
  const num = Number(value)
  if (!Number.isFinite(num) || num <= 0) return fallback
  return Math.max(1, Math.floor(num))
}
function isDefaultFlag(value) {
  const normalized = String(value ?? '').trim().toLowerCase()
  return normalized === '1' || normalized === 'true'
}

function parseTemplatePlatforms(templateInfo) {
  if (!templateInfo) return []
  try {
    const arr = typeof templateInfo === 'string' ? JSON.parse(templateInfo) : templateInfo
    if (!Array.isArray(arr)) return []
    const seen = new Map()
    arr.forEach((item) => {
      const code = String(typeof item === 'string' ? item : item?.platformCode || item?.code || item?.value || '').trim()
      const name = String(typeof item === 'object' ? item?.platformName || item?.name || item?.label || '' : '').trim()
      const unitPrice = normalizeUnitPrice(typeof item === 'object' ? item?.unitPrice : 1, 1)
      if (!code) return
      if (!seen.has(code)) {
        seen.set(code, { code, name, unitPrice })
        return
      }
      const current = seen.get(code)
      if (!current.name && name) {
        current.name = name
      }
      if (!current.unitPrice || current.unitPrice <= 0) {
        current.unitPrice = unitPrice
      }
    })
    return Array.from(seen.values())
  } catch {
    return []
  }
}
function isSystemCode(code) {
  return !!platformSystemMap.value[String(code || '').trim()]
}

function resolvePlatformCodeRemark(code) {
  const normalized = String(code || '').trim()
  if (!normalized) return ''
  const fromOptions = String(platformNameMap.value[normalized] || '').trim()
  if (fromOptions && fromOptions !== normalized) {
    return fromOptions
  }
  const fromStandard = STANDARD_PLATFORM_REMARKS[normalized] || STANDARD_PLATFORM_REMARKS[normalized.toLowerCase()]
  if (fromStandard) {
    return fromStandard
  }
  return isSystemCode(normalized) ? (fromOptions || normalized) : '自定义平台（请确认编码是否正确）'
}

function parseTemplateCodes(templateInfo) {
  return parseTemplatePlatforms(templateInfo).map((item) => item.code)
}
function resetPlatformUnitPriceMap() {
  Object.keys(platformUnitPriceMap).forEach((key) => {
    delete platformUnitPriceMap[key]
  })
}
function resetPlatformDisplayNameMap() {
  Object.keys(platformDisplayNameMap).forEach((key) => {
    delete platformDisplayNameMap[key]
  })
}

function syncSelectedPlatformMeta() {
  const selectedSet = new Set((selectedPlatformCodes.value || []).map((item) => String(item)))
  selectedSet.forEach((code) => {
    platformUnitPriceMap[code] = normalizeUnitPrice(platformUnitPriceMap[code], 1)
    const fallbackName = String(platformDisplayNameMap[code] || platformNameMap.value[code] || code).trim() || code
    platformDisplayNameMap[code] = fallbackName
  })
  Object.keys(platformUnitPriceMap).forEach((code) => {
    if (!selectedSet.has(code)) {
      delete platformUnitPriceMap[code]
    }
  })
  Object.keys(platformDisplayNameMap).forEach((code) => {
    if (!selectedSet.has(code)) {
      delete platformDisplayNameMap[code]
    }
  })
}

watch(selectedPlatformCodes, () => {
  syncSelectedPlatformMeta()
}, { deep: true })

function rebuildPlatformMeta() {
  platformNameMap.value = platformOptions.value.reduce((acc, item) => {
    acc[item.code] = item.name
    return acc
  }, {})
  platformSystemMap.value = platformOptions.value.reduce((acc, item) => {
    acc[item.code] = !!item.isSystem
    return acc
  }, {})
}

function mergePlatformOptions(extraOptions = []) {
  const merged = new Map()
  platformOptions.value.forEach((item) => {
    if (!item?.code) return
    const code = String(item.code).trim()
    if (!code) return
    merged.set(code, {
      code,
      name: item.name || code,
      isSystem: !!item.isSystem
    })
  })
  extraOptions.forEach((item) => {
    const code = String(item?.code || '').trim()
    if (!code) return
    const name = String(item?.name || '').trim() || code
    const isSystem = !!item?.isSystem
    if (!merged.has(code)) {
      merged.set(code, { code, name, isSystem })
      return
    }
    const current = merged.get(code)
    const currentName = current?.name || code
    if ((!currentName || currentName === code) && name) {
      current.name = name
    }
    current.isSystem = !!current.isSystem || isSystem
    merged.set(code, current)
  })
  platformOptions.value = Array.from(merged.values())
  rebuildPlatformMeta()
}

function ensureTemplateOptions(templateInfo) {
  const extraOptions = parseTemplatePlatforms(templateInfo).map((item) => ({
    code: item.code,
    name: item.name || platformNameMap.value[item.code] || item.code,
    isSystem: isSystemCode(item.code)
  }))
  if (extraOptions.length > 0) {
    mergePlatformOptions(extraOptions)
  }
}
function applyTemplateUnitPrice(templateInfo) {
  resetPlatformUnitPriceMap()
  resetPlatformDisplayNameMap()
  parseTemplatePlatforms(templateInfo).forEach((item) => {
    platformUnitPriceMap[item.code] = normalizeUnitPrice(item.unitPrice, 1)
    platformDisplayNameMap[item.code] = String(item.name || platformNameMap.value[item.code] || item.code).trim() || item.code
  })
  syncSelectedPlatformMeta()
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
  const unitPrice = normalizeUnitPrice(customPlatformForm.unitPrice, 1)
  const existed = platformOptions.value.some((item) => item.code === code)
  mergePlatformOptions([{ code, name, isSystem: false }])
  selectedPlatformCodes.value = Array.from(new Set([...(selectedPlatformCodes.value || []), code]))
  platformDisplayNameMap[code] = name
  platformUnitPriceMap[code] = unitPrice
  customPlatformForm.code = ''
  customPlatformForm.name = ''
  customPlatformForm.unitPrice = 1
  proxy.$modal.msgSuccess(existed ? '平台已存在，已自动勾选' : '新增平台成功并已勾选')
}

function canRemoveCustomOption(code) {
  return !isSystemCode(code)
}
function handlePlatformOrderChange(code, inputOrder) {
  const targetCode = String(code || '').trim()
  if (!targetCode) return
  const list = [...(selectedPlatformCodes.value || [])]
  const currentIndex = list.indexOf(targetCode)
  if (currentIndex < 0) return
  const parsedOrder = Number(inputOrder)
  if (!Number.isFinite(parsedOrder)) return
  const maxOrder = list.length
  const normalizedOrder = Math.min(maxOrder, Math.max(1, Math.floor(parsedOrder)))
  const targetIndex = normalizedOrder - 1
  if (targetIndex === currentIndex) return
  list.splice(currentIndex, 1)
  list.splice(targetIndex, 0, targetCode)
  selectedPlatformCodes.value = list
}

function handleRemoveCustomPlatform(code) {
  const targetCode = String(code || '').trim()
  if (!targetCode || !canRemoveCustomOption(targetCode)) return
  platformOptions.value = platformOptions.value.filter((item) => item.code !== targetCode)
  selectedPlatformCodes.value = (selectedPlatformCodes.value || []).filter((item) => item !== targetCode)
  delete platformUnitPriceMap[targetCode]
  delete platformDisplayNameMap[targetCode]
  if (customPlatformForm.code === targetCode) {
    customPlatformForm.code = ''
  }
  rebuildPlatformMeta()
  syncSelectedPlatformMeta()
  proxy.$modal.msgSuccess('已删除自定义平台')
}
function parseOptionIsSystem(value) {
  if (value === true || value === 1 || value === '1' || value === 'true') return true
  return false
}

function loadPlatformOptions() {
  return listMarkPlatformOptions().then((res) => {
    const options = Array.isArray(res?.data) ? res.data : []
    platformOptions.value = options
      .map((item) => {
        const code = typeof item?.code === 'string' ? item.code.trim() : ''
        const name = typeof item?.name === 'string' && item.name.trim() ? item.name.trim() : code
        const isSystem = parseOptionIsSystem(item?.isSystem)
        return { code, name, isSystem }
      })
      .filter((item) => item.code)
    rebuildPlatformMeta()
  }).catch(() => {
    platformOptions.value = []
    platformNameMap.value = {}
    platformSystemMap.value = {}
    resetPlatformUnitPriceMap()
    resetPlatformDisplayNameMap()
  })
}
function buildTemplateInfoPayload(selectedCodes) {
  return (selectedCodes || [])
    .map((code) => String(code || '').trim())
    .filter((code) => code.length > 0)
    .map((code) => ({
      platformCode: code,
      platformName: String(platformDisplayNameMap[code] || platformNameMap.value[code] || code).trim() || code,
      unitPrice: normalizeUnitPrice(platformUnitPriceMap[code], 1)
    }))
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
    isDefault: '0',
    status: '0',
    remark: ''
  }
  selectedPlatformCodes.value = []
  customPlatformForm.code = ''
  customPlatformForm.name = ''
  customPlatformForm.unitPrice = 1
  resetPlatformUnitPriceMap()
  resetPlatformDisplayNameMap()
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetForm()
  loadPlatformOptions().finally(() => {
    selectedPlatformCodes.value = (platformOptions.value || []).map((item) => item.code)
    syncSelectedPlatformMeta()
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
      form.value = {
        ...form.value,
        ...res.data,
        status: res.data?.status || '0',
        isDefault: isDefaultFlag(res.data?.isDefault) ? '1' : '0'
      }
      ensureTemplateOptions(form.value.templateInfo)
      selectedPlatformCodes.value = parseTemplateCodes(form.value.templateInfo)
      applyTemplateUnitPrice(form.value.templateInfo)
      title.value = '修改标记模板'
      open.value = true
    })
  })
}
function handleSetDefault(row) {
  const id = row?.id
  if (!id || isDefaultFlag(row?.isDefault)) return
  const templateName = String(row?.templateName || '').trim() || `#${id}`
  proxy.$modal.confirm(`确认将模板「${templateName}」设为默认吗？`).then(() => {
    defaultUpdatingId.value = id
    return updateMarkTemplate({ id, isDefault: '1' })
  }).then(() => {
    proxy.$modal.msgSuccess('已设为默认模板')
    getList()
  }).catch(() => {}).finally(() => {
    if (defaultUpdatingId.value === id) {
      defaultUpdatingId.value = null
    }
  })
}

function submitForm() {
  const selectedCodes = selectedPlatformCodes.value || []
  const payload = buildTemplateInfoPayload(selectedCodes)
  form.value.templateInfo = payload.length > 0 ? JSON.stringify(payload) : ''
  form.value.isDefault = isDefaultFlag(form.value.isDefault) ? '1' : '0'
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
  flex-wrap: wrap;
  gap: 10px;
}

.platform-custom-tip {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.platform-unit-editor {
  margin-top: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 10px 12px;
  background: var(--el-fill-color-lighter);
}

.platform-unit-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 8px;
}

.platform-code-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.platform-unit-code-input {
  width: 150px;
  flex: 0 0 150px;
}

.platform-unit-remark-input {
  width: 170px;
  flex: 0 0 170px;
}

.platform-unit-code-input :deep(.el-input__inner) {
  font-family: Consolas, Monaco, monospace;
}

.platform-option-code {
  margin-left: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.platform-unit-row:last-child {
  margin-bottom: 0;
}

.platform-unit-name-input {
  width: 180px;
  flex: 0 0 180px;
}

.platform-unit-suffix,
.platform-order-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.platform-order-input {
  width: 106px;
  flex: 0 0 106px;
}

.platform-option-label {
  margin-right: 6px;
}

.platform-option-remove {
  padding: 0;
  min-height: auto;
}
.platform-unit-remove {
  padding: 0;
  min-height: auto;
}
</style>
