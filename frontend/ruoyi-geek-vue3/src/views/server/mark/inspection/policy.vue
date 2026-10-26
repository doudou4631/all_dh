<template>
  <div class="app-container">
    <el-form :model="query" inline>
      <el-form-item label="策略名称"><el-input v-model="query.policyName" clearable @keyup.enter="load" /></el-form-item>
      <el-form-item label="状态"><el-select v-model="query.status" clearable style="width:130px"><el-option label="启用" value="0"/><el-option label="停用" value="1"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" :icon="Search" @click="search">查询</el-button><el-button :icon="Refresh" @click="reset">重置</el-button></el-form-item>
      <el-form-item><el-button type="primary" :icon="Plus" @click="openCreate" v-hasPermi="['server:markInspection:policy:add']">新增策略</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column label="策略名称" prop="policyName" min-width="160"/>
      <el-table-column label="处理平台" min-width="150"><template #default="{row}"><div>{{row.platformName}}</div><code>{{row.platformCode}}</code></template></el-table-column>
      <el-table-column label="检测订单" min-width="155" align="center"><template #default="{row}"><el-tag type="info">{{inspectionScopeLabel(row)}}</el-tag></template></el-table-column>
      <el-table-column label="自动接管" width="100" align="center"><template #default="{row}"><el-tag :type="row.autoTakeover?'success':'info'">{{row.autoTakeover?'已开启':'已关闭'}}</el-tag></template></el-table-column>
      <el-table-column label="巡检间隔" width="110" align="center"><template #default="{row}">{{formatInterval(row)}}</template></el-table-column>
      <el-table-column label="单次上限" prop="batchLimit" width="100" align="center"/>
      <el-table-column label="无标记后操作" width="120" align="center"><template #default="{row}"><el-tag :type="row.autoAction==='FAILED'?'danger':'success'">{{row.autoAction==='FAILED'?'处理失败':'处理成功'}}</el-tag></template></el-table-column>
      <el-table-column label="运行状态" width="100" align="center"><template #default="{row}"><el-tag :type="row.running==='1'?'warning':'info'">{{row.running==='1'?'执行中':'空闲'}}</el-tag></template></el-table-column>
      <el-table-column label="策略状态" width="110" align="center"><template #default="{row}"><el-switch v-model="row.status" :disabled="row.running==='1'||runningIds.has(row.id)" active-value="0" inactive-value="1" @change="changeStatus(row)" v-hasPermi="['server:markInspection:policy:edit']"/></template></el-table-column>
      <el-table-column label="下次执行" prop="nextRunTime" min-width="165"/>
      <el-table-column label="操作" width="210" fixed="right" align="center"><template #default="{row}">
        <el-button link type="primary" :icon="Edit" :disabled="row.running==='1'||runningIds.has(row.id)" @click="openEdit(row)" v-hasPermi="['server:markInspection:policy:edit']">编辑</el-button>
        <el-button link type="success" :icon="VideoPlay" :loading="runningIds.has(row.id)" :disabled="row.status!=='0'||row.running==='1'||runningIds.has(row.id)" @click="runNow(row)" v-hasPermi="['server:markInspection:policy:run']">立即巡检</el-button>
        <el-button link type="danger" :icon="Delete" :disabled="row.running==='1'||runningIds.has(row.id)" @click="remove(row)" v-hasPermi="['server:markInspection:policy:remove']">删除</el-button>
      </template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="load"/>
    <el-dialog v-model="dialog" :title="form.id?'编辑巡检策略':'新增巡检策略'" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="105px">
        <el-form-item label="策略名称" prop="policyName"><el-input v-model="form.policyName" maxlength="100"/></el-form-item>
        <el-form-item label="处理平台" prop="platformCode"><el-select v-model="form.platformCode" style="width:100%" @change="changePlatform"><el-option v-for="p in platforms" :key="p.platformCode" :label="`${p.platformName} / ${p.platformCode}${p.enabled ? '' : '（已关闭）'}`" :value="p.platformCode" :disabled="!p.enabled"/></el-select></el-form-item>
        <el-form-item label="检测订单" prop="orderScope"><el-select v-model="form.orderScope" style="width:100%" @change="changeScope"><el-option v-for="option in INSPECTION_SCOPE_OPTIONS" :key="option.value" :label="option.label" :value="option.value" :disabled="form.platformCode==='td_gaopin'&&option.value!=='PENDING'"/></el-select></el-form-item>
        <el-form-item label="自动接管"><el-switch v-model="form.autoTakeover" :disabled="form.platformCode==='td_gaopin'" active-text="开启" inactive-text="关闭" @change="changeTakeover"/><div class="form-help">开启后无需代理点击，系统直接检测待处理订单。检测范围包含处理中时，接管后显示为处理中；仅待处理时保留待处理以继续重试。</div></el-form-item>
        <el-form-item label="巡检间隔" prop="intervalValue">
          <div class="interval-control"><el-input-number v-model="form.intervalValue" :min="1" :max="9999" controls-position="right"/><el-select v-model="form.intervalUnit" style="width:100px"><el-option label="分钟" value="MINUTE"/><el-option label="小时" value="HOUR"/></el-select></div>
        </el-form-item>
        <el-form-item label="单次处理上限" prop="batchLimit"><el-input-number v-model="form.batchLimit" :min="1" :max="500" controls-position="right"/><span class="unit">条</span></el-form-item>
        <el-form-item label="无标记后操作" prop="autoAction"><el-select v-model="form.autoAction" style="width:100%"><el-option label="处理成功" value="SUCCESS"/><el-option label="处理失败（退回次数）" value="FAILED"/></el-select><div class="form-help">仅在明确检测到无标记时执行；仍有标记或查询失败会等待下次巡检。</div></el-form-item>
        <el-form-item label="策略状态"><el-switch v-model="form.status" active-value="0" inactive-value="1" active-text="启用" inactive-text="停用"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup name="MarkInspectionPolicy">
import { getCurrentInstance, reactive, ref } from 'vue'
import { Delete, Edit, Plus, Refresh, Search, VideoPlay } from '@element-plus/icons-vue'
import { addInspectionPolicy, deleteInspectionPolicy, listInspectionPlatforms, listInspectionPolicies, runInspectionPolicy, updateInspectionPolicy } from '@/api/server/markInspection'
import { INSPECTION_SCOPE_OPTIONS, inspectionDefaults, inspectionForm, inspectionRunSummary, inspectionScopeLabel } from '@/utils/markInspectionPolicy'
import { WUMA_PLATFORM_CODES } from '@/utils/markWumaPlatform'
const {proxy}=getCurrentInstance(); const loading=ref(false),saving=ref(false),dialog=ref(false),rows=ref([]),total=ref(0),platforms=ref([])
const query=reactive({pageNum:1,pageSize:10,policyName:'',status:''}); const form=reactive(inspectionForm())
const runningIds=reactive(new Set())
const rules={policyName:[{required:true,message:'请输入策略名称',trigger:'blur'}],platformCode:[{required:true,message:'请选择处理平台',trigger:'change'}],intervalValue:[{required:true,message:'请输入巡检间隔',trigger:'change'}],batchLimit:[{required:true,message:'请输入单次处理上限',trigger:'change'}],autoAction:[{required:true,message:'请选择自动执行结果',trigger:'change'}]}
async function load(){loading.value=true;try{const r=await listInspectionPolicies(query);rows.value=r.rows||[];total.value=r.total||0}finally{loading.value=false}}
function search(){query.pageNum=1;load()} function reset(){Object.assign(query,{pageNum:1,pageSize:10,policyName:'',status:''});load()}
async function loadPlatforms(){const r=await listInspectionPlatforms();platforms.value=r.data||[]}
function openCreate(){Object.assign(form,inspectionForm());dialog.value=true} function openEdit(row){Object.assign(form,inspectionForm(row));dialog.value=true}
function changePlatform(code){Object.assign(form,inspectionDefaults(code))}
function changeScope(scope){if(WUMA_PLATFORM_CODES.includes(form.platformCode)&&scope!=='PROCESSING')form.autoTakeover=true}
function changeTakeover(enabled){if(enabled&&form.orderScope==='PROCESSING')form.orderScope='BOTH';if(!enabled&&WUMA_PLATFORM_CODES.includes(form.platformCode))form.orderScope='PROCESSING'}
function submit(){proxy.$refs.formRef.validate(async ok=>{if(!ok)return;saving.value=true;try{form.id?await updateInspectionPolicy(form):await addInspectionPolicy(form);proxy.$modal.msgSuccess('保存成功');dialog.value=false;load()}finally{saving.value=false}})}
async function changeStatus(row){try{await updateInspectionPolicy(row);proxy.$modal.msgSuccess(row.status==='0'?'策略已启用':'策略已停用');load()}catch(e){row.status=row.status==='0'?'1':'0';throw e}}
async function runNow(row){
  if(runningIds.has(row.id)||row.running==='1'||row.status!=='0')return
  runningIds.add(row.id)
  try{
    const action=row.autoAction==='FAILED'?'处理失败并退回次数':'处理成功'
    await proxy.$modal.confirm(`确认立即巡检“${row.policyName}”的${inspectionScopeLabel(row)}订单，检测到无标记后${action}吗？`)
    const r=await runInspectionPolicy(row.id)
    const message=inspectionRunSummary(r.data)
    r.data?.failedCount>0?proxy.$modal.msgWarning(message):proxy.$modal.msgSuccess(message)
  }finally{runningIds.delete(row.id);load()}
}
async function remove(row){await proxy.$modal.confirm(`确认删除策略“${row.policyName}”吗？`);await deleteInspectionPolicy(row.id);proxy.$modal.msgSuccess('删除成功');load()}
function formatInterval(row){return `${row.intervalValue||30} ${row.intervalUnit==='HOUR'?'小时':'分钟'}`}
loadPlatforms();load()
</script>
<style scoped>.unit{margin-left:8px;color:var(--el-text-color-secondary)}.interval-control{display:flex;gap:8px} code{color:var(--el-text-color-secondary)}.form-help{width:100%;font-size:12px;color:var(--el-text-color-secondary);line-height:1.6;margin-top:6px}</style>
