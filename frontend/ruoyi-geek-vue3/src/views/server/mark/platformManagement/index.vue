<template>
  <div class="app-container platform-management">
    <div class="toolbar">
      <div>
        <h2>平台管理</h2>
        <span>{{ rows.length }} 条平台映射</span>
      </div>
      <div class="toolbar-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate" v-hasPermi="['server:markPlatform:add']">新增映射</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column type="index" label="序号" width="68" align="center" />
      <el-table-column label="排序" prop="sortNum" width="78" align="center" />
      <el-table-column label="用户端来源 (Source)" min-width="210">
        <template #default="{ row }">
          <div class="platform-name">{{ row.sourcePlatformName }}</div>
          <code>{{ row.sourcePlatformCode }}</code>
          <el-switch v-model="row.sourceEnabled" active-value="0" inactive-value="1" active-text="显示" inactive-text="关闭" @change="updateSource(row)" v-hasPermi="['server:markPlatform:status']" />
        </template>
      </el-table-column>
      <el-table-column label="映射状态" width="150" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.mappingEnabled" active-value="0" inactive-value="1" inline-prompt active-text="启用" inactive-text="停用" @change="updateMapping(row)" v-hasPermi="['server:markPlatform:status']" />
          <div class="status-label"><el-tag :type="effectiveType(row)" size="small">{{ effectiveLabel(row) }}</el-tag></div>
        </template>
      </el-table-column>
      <el-table-column label="后台关联平台 (Target)" min-width="220">
        <template #default="{ row }">
          <div class="platform-name">{{ row.targetPlatformName }}</div>
          <code>{{ row.targetPlatformCode }}</code>
          <el-switch v-model="row.targetEnabled" active-value="0" inactive-value="1" active-text="显示" inactive-text="关闭" @change="updateTarget(row)" v-hasPermi="['server:markPlatform:status']" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-tooltip content="编辑映射"><el-button text circle type="primary" :icon="Edit" @click="openEdit(row)" v-hasPermi="['server:markPlatform:edit']" /></el-tooltip>
          <el-tooltip content="删除映射"><el-button text circle type="danger" :icon="Delete" @click="removeRow(row)" v-hasPermi="['server:markPlatform:remove']" /></el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogOpen" :title="form.id ? '编辑平台映射' : '新增平台映射'" width="560px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="来源平台名称" prop="sourcePlatformName"><el-input v-model="form.sourcePlatformName" maxlength="50" /></el-form-item>
        <el-form-item label="来源平台编码" prop="sourcePlatformCode"><el-input v-model="form.sourcePlatformCode" maxlength="64" /></el-form-item>
        <el-form-item label="目标平台名称" prop="targetPlatformName"><el-input v-model="form.targetPlatformName" maxlength="50" /></el-form-item>
        <el-form-item label="目标平台编码" prop="targetPlatformCode"><el-input v-model="form.targetPlatformCode" maxlength="64" /></el-form-item>
        <el-form-item label="排序" prop="sortNum"><el-input-number v-model="form.sortNum" :min="0" :max="9999" controls-position="right" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogOpen=false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="MarkPlatformManagement">
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { addMarkPlatformMapping, changeMappingStatus, changeSourceStatus, changeTargetStatus, deleteMarkPlatformMapping, listMarkPlatformMappings, updateMarkPlatformMapping } from '@/api/server/markPlatformMapping'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const saving = ref(false)
const dialogOpen = ref(false)
const rows = ref([])
const emptyForm = () => ({ id: null, sourcePlatformName: '', sourcePlatformCode: '', targetPlatformName: '', targetPlatformCode: '', sortNum: 0 })
const form = reactive(emptyForm())
const codeRule = { pattern: /^[A-Za-z0-9_]+$/, message: '编码只能包含字母、数字和下划线', trigger: 'blur' }
const rules = {
  sourcePlatformName: [{ required: true, message: '请输入来源平台名称', trigger: 'blur' }],
  sourcePlatformCode: [{ required: true, message: '请输入来源平台编码', trigger: 'blur' }, codeRule],
  targetPlatformName: [{ required: true, message: '请输入目标平台名称', trigger: 'blur' }],
  targetPlatformCode: [{ required: true, message: '请输入目标平台编码', trigger: 'blur' }, codeRule]
}

async function loadData() { loading.value=true; try { const res=await listMarkPlatformMappings(); rows.value=res.data || [] } finally { loading.value=false } }
function effectiveLabel(row) { if (row.sourceEnabled==='1') return '来源关闭'; if (row.targetEnabled==='1') return '目标关闭'; if (row.mappingEnabled==='1') return '映射停用'; return '生效' }
function effectiveType(row) { return effectiveLabel(row)==='生效' ? 'success' : effectiveLabel(row)==='映射停用' ? 'info' : 'warning' }
async function updateSource(row) { try { await changeSourceStatus({ id: row.id, sourceEnabled: row.sourceEnabled }); await loadData() } catch (e) { await loadData(); throw e } }
async function updateTarget(row) { try { await changeTargetStatus({ id: row.id, targetEnabled: row.targetEnabled }); await loadData() } catch (e) { await loadData(); throw e } }
async function updateMapping(row) { try { await changeMappingStatus({ id: row.id, mappingEnabled: row.mappingEnabled }) } catch (e) { await loadData(); throw e } }
function openCreate() { Object.assign(form, emptyForm()); dialogOpen.value=true; nextTick(() => proxy.$refs.formRef?.clearValidate()) }
function openEdit(row) { Object.assign(form, { id: row.id, sourcePlatformName: row.sourcePlatformName, sourcePlatformCode: row.sourcePlatformCode, targetPlatformName: row.targetPlatformName, targetPlatformCode: row.targetPlatformCode, sortNum: row.sortNum ?? 0 }); dialogOpen.value=true; nextTick(() => proxy.$refs.formRef?.clearValidate()) }
function submit() { proxy.$refs.formRef.validate(async valid => { if (!valid) return; saving.value=true; try { form.id ? await updateMarkPlatformMapping(form) : await addMarkPlatformMapping(form); dialogOpen.value=false; proxy.$modal.msgSuccess('保存成功'); await loadData() } finally { saving.value=false } }) }
async function removeRow(row) { await proxy.$modal.confirm(`确认删除 ${row.sourcePlatformName} 到 ${row.targetPlatformName} 的映射吗？`); await deleteMarkPlatformMapping(row.id); proxy.$modal.msgSuccess('删除成功'); await loadData() }
loadData()
</script>

<style scoped>
.toolbar { display:flex; align-items:center; justify-content:space-between; gap:16px; margin-bottom:16px; }
.toolbar h2 { margin:0 0 3px; font-size:20px; letter-spacing:0; }
.toolbar span { color:var(--el-text-color-secondary); font-size:12px; }
.toolbar-actions { display:flex; gap:8px; }
.platform-name { margin-bottom:4px; font-weight:600; }
code { display:block; margin-bottom:8px; color:var(--el-text-color-secondary); }
.status-label { margin-top:8px; }
@media (max-width:700px) { .toolbar { align-items:flex-start; flex-direction:column; } .toolbar-actions { width:100%; } }
</style>
