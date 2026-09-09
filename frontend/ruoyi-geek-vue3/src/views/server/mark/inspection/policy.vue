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
      <el-table-column label="检测订单" width="110" align="center"><template #default><el-tag type="info">仅待处理</el-tag></template></el-table-column>
      <el-table-column label="巡检间隔" width="110" align="center"><template #default="{row}">{{formatInterval(row)}}</template></el-table-column>
      <el-table-column label="单次上限" prop="batchLimit" width="100" align="center"/>
      <el-table-column label="自动执行" width="105" align="center"><template #default="{row}"><el-tag :type="row.autoAction==='FAILED'?'danger':'success'">{{row.autoAction==='FAILED'?'处理失败':'处理成功'}}</el-tag></template></el-table-column>
      <el-table-column label="运行状态" width="100" align="center"><template #default="{row}"><el-tag :type="row.running==='1'?'warning':'info'">{{row.running==='1'?'执行中':'空闲'}}</el-tag></template></el-table-column>
      <el-table-column label="策略状态" width="110" align="center"><template #default="{row}"><el-switch v-model="row.status" active-value="0" inactive-value="1" @change="changeStatus(row)" v-hasPermi="['server:markInspection:policy:edit']"/></template></el-table-column>
      <el-table-column label="下次执行" prop="nextRunTime" min-width="165"/>
      <el-table-column label="操作" width="210" fixed="right" align="center"><template #default="{row}">
        <el-button link type="primary" :icon="Edit" @click="openEdit(row)" v-hasPermi="['server:markInspection:policy:edit']">编辑</el-button>
        <el-button link type="success" :icon="VideoPlay" :disabled="row.status!=='0'||row.running==='1'" @click="runNow(row)" v-hasPermi="['server:markInspection:policy:run']">立即巡检</el-button>
        <el-button link type="danger" :icon="Delete" @click="remove(row)" v-hasPermi="['server:markInspection:policy:remove']">删除</el-button>
      </template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="load"/>
    <el-dialog v-model="dialog" :title="form.id?'编辑巡检策略':'新增巡检策略'" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="105px">
        <el-form-item label="策略名称" prop="policyName"><el-input v-model="form.policyName" maxlength="100"/></el-form-item>
        <el-form-item label="处理平台" prop="platformCode"><el-select v-model="form.platformCode" style="width:100%"><el-option v-for="p in platforms" :key="p.platformCode" :label="`${p.platformName} / ${p.platformCode}${p.enabled?'':'（已关闭）'}`" :value="p.platformCode" :disabled="!p.enabled"/></el-select></el-form-item>
        <el-form-item label="检测订单"><el-input model-value="待处理订单" disabled/></el-form-item>
        <el-form-item label="巡检间隔" prop="intervalValue">
          <div class="interval-control"><el-input-number v-model="form.intervalValue" :min="1" :max="9999" controls-position="right"/><el-select v-model="form.intervalUnit" style="width:100px"><el-option label="分钟" value="MINUTE"/><el-option label="小时" value="HOUR"/></el-select></div>
        </el-form-item>
        <el-form-item label="单次处理上限" prop="batchLimit"><el-input-number v-model="form.batchLimit" :min="1" :max="500" controls-position="right"/><span class="unit">条</span></el-form-item>
        <el-form-item label="自动执行" prop="autoAction"><el-select v-model="form.autoAction" style="width:100%"><el-option label="处理成功" value="SUCCESS"/><el-option label="处理失败" value="FAILED"/></el-select></el-form-item>
        <el-form-item label="策略状态"><el-switch v-model="form.status" active-value="0" inactive-value="1" active-text="启用" inactive-text="停用"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup name="MarkInspectionPolicy">
import { Delete, Edit, Plus, Refresh, Search, VideoPlay } from '@element-plus/icons-vue'
import { addInspectionPolicy, deleteInspectionPolicy, listInspectionPlatforms, listInspectionPolicies, runInspectionPolicy, updateInspectionPolicy } from '@/api/server/markInspection'
const {proxy}=getCurrentInstance(); const loading=ref(false),saving=ref(false),dialog=ref(false),rows=ref([]),total=ref(0),platforms=ref([])
const query=reactive({pageNum:1,pageSize:10,policyName:'',status:''}); const blank=()=>({id:null,policyName:'',platformCode:'',platformName:'',intervalValue:30,intervalUnit:'MINUTE',batchLimit:100,autoAction:'SUCCESS',status:'1'}); const form=reactive(blank())
const rules={policyName:[{required:true,message:'请输入策略名称',trigger:'blur'}],platformCode:[{required:true,message:'请选择处理平台',trigger:'change'}],intervalValue:[{required:true,message:'请输入巡检间隔',trigger:'change'}],batchLimit:[{required:true,message:'请输入单次处理上限',trigger:'change'}],autoAction:[{required:true,message:'请选择自动执行结果',trigger:'change'}]}
async function load(){loading.value=true;try{const r=await listInspectionPolicies(query);rows.value=r.rows||[];total.value=r.total||0}finally{loading.value=false}}
function search(){query.pageNum=1;load()} function reset(){Object.assign(query,{pageNum:1,pageSize:10,policyName:'',status:''});load()}
async function loadPlatforms(){const r=await listInspectionPlatforms();platforms.value=r.data||[]}
function openCreate(){Object.assign(form,blank());dialog.value=true} function openEdit(row){Object.assign(form,row);dialog.value=true}
function submit(){proxy.$refs.formRef.validate(async ok=>{if(!ok)return;saving.value=true;try{form.id?await updateInspectionPolicy(form):await addInspectionPolicy(form);proxy.$modal.msgSuccess('保存成功');dialog.value=false;load()}finally{saving.value=false}})}
async function changeStatus(row){try{await updateInspectionPolicy(row);proxy.$modal.msgSuccess(row.status==='0'?'策略已启用':'策略已停用');load()}catch(e){row.status=row.status==='0'?'1':'0';throw e}}
async function runNow(row){const action=row.autoAction==='FAILED'?'处理失败':'处理成功';await proxy.$modal.confirm(`确认立即巡检“${row.policyName}”的待处理订单，命中后${action}吗？`);const r=await runInspectionPolicy(row.id);proxy.$modal.msgSuccess(`巡检完成，扫描 ${r.data?.scannedCount||0} 条，执行 ${r.data?.successCount||0} 条`);load()}
async function remove(row){await proxy.$modal.confirm(`确认删除策略“${row.policyName}”吗？`);await deleteInspectionPolicy(row.id);proxy.$modal.msgSuccess('删除成功');load()}
function formatInterval(row){return `${row.intervalValue||30} ${row.intervalUnit==='HOUR'?'小时':'分钟'}`}
loadPlatforms();load()
</script>
<style scoped>.unit{margin-left:8px;color:var(--el-text-color-secondary)}.interval-control{display:flex;gap:8px} code{color:var(--el-text-color-secondary)}</style>
