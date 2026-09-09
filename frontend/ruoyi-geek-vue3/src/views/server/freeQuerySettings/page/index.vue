<template>
  <div class="app-container query-page-settings">
    <div class="page-toolbar">
      <div>
        <h2>查询页面管理</h2>
        <span class="updated-at">共 {{ pages.length }} 个查询页面</span>
      </div>
      <div class="toolbar-actions">
        <el-button :icon="Plus" type="primary" @click="openAddPage">添加页面</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="loadConfigs">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" class="settings-panel" v-loading="loading">
      <el-tabs v-model="activePageCode" type="card">
        <el-tab-pane v-for="page in pages" :key="page.pageCode" :label="`页面 ${page.pageCode}`" :name="page.pageCode" />
      </el-tabs>
      <el-form v-if="pages.length" ref="pageFormRef" :model="activeForm" :rules="rules" label-width="120px">
            <el-form-item label="页面编号">
              <el-input :model-value="activeForm.pageCode" disabled />
            </el-form-item>

            <el-form-item label="页面链接">
              <el-input :model-value="isPersisted ? pageUrl : '保存页面后生成链接'" readonly>
                <template #append>
                  <el-button
                    :icon="CopyDocument"
                    :disabled="!isPersisted"
                    title="复制页面链接"
                    @click="copyPageUrl"
                  />
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="联系电话" prop="phone">
              <el-input v-model.trim="activeForm.phone" maxlength="32" placeholder="选填，不填则不显示联系电话" />
            </el-form-item>

            <el-form-item label="在线客服">
              <el-switch
                v-model="activeForm.showOnlineService"
                inline-prompt
                active-text="显示"
                inactive-text="隐藏"
              />
            </el-form-item>

            <el-form-item label="客服二维码" prop="qrCode">
              <div v-if="isStaticQr" class="legacy-qr-preview">
                <img :src="imagePreviewUrl" alt="客服二维码" />
                <el-button link type="primary" @click="activeForm.qrCode = ''">更换二维码</el-button>
              </div>
              <ImageUpload
                v-else
                v-model="activeForm.qrCode"
                :limit="1"
                :file-size="2"
                :file-type="['png', 'jpg', 'jpeg', 'webp']"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :icon="Check"
                :loading="saving"
                @click="saveConfig"
                v-hasPermi="['freeQuery:page:edit']"
              >保存页面 {{ activeForm.pageCode }}</el-button>
            </el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="addDialogVisible" title="添加查询页面" width="460px" @closed="resetAddForm">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px">
        <el-form-item label="页面编号" prop="pageCode">
          <el-input v-model.trim="addForm.pageCode" maxlength="32" placeholder="例如 2、brand-a" />
        </el-form-item>
        <div class="dialog-help">保存后会生成 /query/{{ addForm.pageCode || '页面编号' }}/ 链接，再为该页面设置电话和二维码。</div>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addPage">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FreeQueryPageSettings">
import { Check, CopyDocument, Plus, Refresh } from '@element-plus/icons-vue'
import { listFreeQueryPageConfigs, updateFreeQueryPageConfig } from '@/api/server/freeQueryPage'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const saving = ref(false)
const pages = ref([])
const activePageCode = ref('1')
const addDialogVisible = ref(false)
const addFormRef = ref()
const pageFormRef = ref()
const addForm = reactive({ pageCode: '' })

const activeForm = computed(() => pages.value.find(page => page.pageCode === activePageCode.value) || {
  pageCode: activePageCode.value,
  phone: '',
  qrCode: ''
})
const queryBaseUrl = computed(() => {
  if (!import.meta.env.DEV) return window.location.origin
  return `${window.location.protocol}//${window.location.hostname}:18765`
})
const pageUrl = computed(() => `${queryBaseUrl.value}/query/${activeForm.value.pageCode}/`)
const isPersisted = computed(() => activeForm.value.persisted === true)
const isStaticQr = computed(() => String(activeForm.value.qrCode || '').startsWith('/static/'))
const imagePreviewUrl = computed(() => isStaticQr.value ? `${queryBaseUrl.value}${activeForm.value.qrCode}` : activeForm.value.qrCode)
function validateOptionalPhone(_rule, value, callback) {
  const phone = String(value || '').trim()
  if (!phone || /^[0-9+() -]{5,32}$/.test(phone)) callback()
  else callback(new Error('联系电话格式不正确'))
}
const rules = {
  phone: [{ validator: validateOptionalPhone, trigger: 'blur' }],
  qrCode: [{ required: true, message: '请上传客服二维码', trigger: 'change' }]
}
const addRules = {
  pageCode: [
    { required: true, message: '请输入页面编号', trigger: 'blur' },
    { pattern: /^[a-z0-9][a-z0-9_-]{0,31}$/, message: '仅支持小写字母、数字、下划线和短横线', trigger: 'blur' }
  ]
}

async function loadConfigs() {
  loading.value = true
  try {
    const response = await listFreeQueryPageConfigs()
    pages.value = (response.data || []).map(page => ({
      ...page,
      showOnlineService: page.showOnlineService === true || page.showOnlineService === 1,
      persisted: true
    }))
    if (!pages.value.some(page => page.pageCode === activePageCode.value)) {
      activePageCode.value = pages.value[0]?.pageCode || '1'
    }
    await nextTick()
    pageFormRef.value?.clearValidate?.()
  } finally {
    loading.value = false
  }
}

function openAddPage() {
  addDialogVisible.value = true
}

function resetAddForm() {
  addForm.pageCode = ''
  addFormRef.value?.resetFields?.()
}

function addPage() {
  addFormRef.value.validate(valid => {
    if (!valid) return
    const pageCode = addForm.pageCode.toLowerCase()
    if (pages.value.some(page => page.pageCode === pageCode)) {
      proxy.$modal.msgError('该页面编号已经存在')
      return
    }
    pages.value.push({
      pageCode,
      phone: '',
      qrCode: '',
      showOnlineService: false,
      createTime: '',
      updateTime: '',
      persisted: false
    })
    activePageCode.value = pageCode
    addDialogVisible.value = false
  })
}

function saveConfig() {
  pageFormRef.value.validate(async valid => {
    if (!valid) return
    saving.value = true
    try {
      await updateFreeQueryPageConfig({
        pageCode: activeForm.value.pageCode,
        phone: activeForm.value.phone,
        qrCode: activeForm.value.qrCode,
        showOnlineService: activeForm.value.showOnlineService === true
      })
      proxy.$modal.msgSuccess(`页面 ${activeForm.value.pageCode} 配置已保存`)
      await loadConfigs()
    } finally {
      saving.value = false
    }
  })
}

async function copyPageUrl() {
  if (!isPersisted.value) {
    proxy.$modal.msgWarning('请先保存页面配置')
    return
  }
  try {
    await navigator.clipboard.writeText(pageUrl.value)
    proxy.$modal.msgSuccess('页面链接已复制')
  } catch {
    proxy.$modal.msgError('复制失败，请手动复制页面链接')
  }
}

loadConfigs()
</script>

<style scoped>
.page-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.toolbar-actions { display: flex; gap: 10px; }
.page-toolbar h2 { margin: 0 0 4px; font-size: 20px; line-height: 28px; letter-spacing: 0; }
.updated-at { color: var(--el-text-color-secondary); font-size: 12px; }
.settings-panel { max-width: 900px; border-radius: 6px; }
.settings-panel :deep(.el-card__body) { padding: 20px 30px 10px; }
.settings-panel :deep(.el-form-item) { margin-bottom: 24px; }
.settings-panel :deep(.el-input) { max-width: 640px; }
.legacy-qr-preview { display: flex; align-items: flex-end; gap: 12px; }
.legacy-qr-preview img { display: block; width: 146px; height: 146px; object-fit: contain; border: 1px solid var(--el-border-color); border-radius: 4px; }
.dialog-help { color: var(--el-text-color-secondary); font-size: 12px; line-height: 18px; }
@media (max-width: 680px) {
  .toolbar-actions { flex-wrap: wrap; justify-content: flex-end; }
  .settings-panel :deep(.el-card__body) { padding: 16px; }
  .settings-panel :deep(.el-form-item__label) { float: none; display: block; width: 100% !important; text-align: left; }
  .settings-panel :deep(.el-form-item__content) { margin-left: 0 !important; }
}
</style>
