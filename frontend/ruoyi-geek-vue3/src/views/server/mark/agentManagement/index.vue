<template>
  <div class="app-container agent-management-page">
    <div class="toolbar">
      <div><h2>{{ levelName }}管理</h2><span>{{ rows.length }} 个账号</span></div>
      <div class="toolbar-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadList">刷新</el-button>
        <el-button v-if="canCreateHere" type="primary" :icon="Plus" @click="openCreate" v-hasPermi="['server:markAgentManagement:add']">创建{{ levelName }}</el-button>
      </div>
    </div>
    <el-form :inline="true" class="search-form" @submit.prevent>
      <el-form-item label="账号"><el-input v-model="query.keyword" clearable placeholder="账号/昵称/手机号" @keyup.enter="loadList" /></el-form-item>
      <el-form-item label="状态"><el-select v-model="query.status" clearable style="width:130px"><el-option label="正常" value="0"/><el-option label="停用" value="1"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" :icon="Search" @click="loadList">查询</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column label="账号" prop="userName" min-width="130" />
      <el-table-column label="昵称" prop="nickName" min-width="130" />
      <el-table-column label="手机号" prop="phonenumber" min-width="130"><template #default="{row}">{{row.phonenumber||'-'}}</template></el-table-column>
      <el-table-column label="直属上级" prop="parentUserName" min-width="120" />
      <el-table-column label="平台次数合计" prop="totalRemainCount" min-width="125" align="center" />
      <el-table-column label="状态" width="95" align="center"><template #default="{row}"><el-tag :type="row.status==='0'?'success':'danger'">{{row.status==='0'?'正常':'停用'}}</el-tag></template></el-table-column>
      <el-table-column label="创建时间" prop="createTime" min-width="165" />
      <el-table-column label="操作" min-width="270" fixed="right" align="center">
        <template #default="{row}">
          <el-button link type="primary" :icon="View" @click="viewRow(row)">查看</el-button>
          <el-button link :type="row.status==='0'?'danger':'success'" @click="toggleStatus(row)" v-hasPermi="['server:markAgentManagement:edit']">{{row.status==='0'?'停用':'启用'}}</el-button>
          <el-button link type="warning" @click="resetPassword(row)" v-hasPermi="['server:markAgentManagement:resetPwd']">初始化密码</el-button>
          <el-button v-if="canAllocateRow(row)" link type="success" :icon="Coin" @click="openAllocate(row)" v-hasPermi="['server:markAgentManagement:allocate']">{{allocateButton}}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createOpen" :title="`创建${levelName}`" width="500px" append-to-body>
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item v-if="agentLevel>=2" label="账号角色">
          <el-input :model-value="agentLevel===2?'下级代理（mark_user，固定）':'普通用户（mark_user，固定）'" disabled />
        </el-form-item>
        <el-form-item label="登录账号" prop="userName"><el-input v-model="createForm.userName" maxlength="20" /></el-form-item>
        <el-form-item label="账号昵称" prop="nickName"><el-input v-model="createForm.nickName" maxlength="30" /></el-form-item>
        <el-form-item label="手机号" prop="phonenumber"><el-input v-model="createForm.phonenumber" maxlength="11" /></el-form-item>
        <el-form-item label="初始密码" prop="password"><el-input v-model="createForm.password" type="password" show-password maxlength="20" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="createOpen=false">取消</el-button><el-button type="primary" :loading="saving" @click="submitCreate">创建</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailOpen" title="账号详情" width="480px" append-to-body>
      <el-descriptions :column="1" border><el-descriptions-item label="账号">{{activeRow.userName}}</el-descriptions-item><el-descriptions-item label="昵称">{{activeRow.nickName}}</el-descriptions-item><el-descriptions-item label="类型">{{levelName}}</el-descriptions-item><el-descriptions-item label="直属上级">{{activeRow.parentUserName||'-'}}</el-descriptions-item><el-descriptions-item label="平台次数合计">{{activeRow.totalRemainCount||0}}</el-descriptions-item></el-descriptions>
    </el-dialog>

    <el-dialog v-model="allocateOpen" :title="`${allocateButton} - ${activeRow.userName||''}`" width="620px" append-to-body>
      <el-table v-loading="platformLoading" :data="platforms" border max-height="360">
        <el-table-column label="平台" prop="platformName" min-width="160"/>
        <el-table-column v-if="capabilities.operationType==='TRANSFER'" label="我的剩余次数" prop="operatorBalance" width="130" align="center"/>
        <el-table-column label="对方剩余次数" prop="targetBalance" width="130" align="center"/>
        <el-table-column label="操作" width="90" align="center"><template #default="{row}"><el-button link type="primary" :disabled="capabilities.operationType==='TRANSFER' && Number(row.operatorBalance||0)<=0" @click="selectPlatform(row)">选择</el-button></template></el-table-column>
      </el-table>
      <el-form v-if="allocateForm.platformCode" class="allocate-form" label-width="90px">
        <el-form-item label="平台">{{allocateForm.platformName}}</el-form-item>
        <el-form-item label="充值次数"><el-input-number v-model="allocateForm.amount" :min="1" :max="maxAmount" :precision="0" style="width:200px"/></el-form-item>
        <el-form-item label="原因"><el-input v-model="allocateForm.reason" maxlength="100"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="allocateOpen=false">取消</el-button><el-button type="primary" :disabled="!allocateForm.platformCode" :loading="saving" @click="submitAllocate">确认{{allocateButton}}</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="MarkAgentManagement">
import { Coin, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import { adminRechargeAgent, changeAgentStatus, createAgent, getAgentCapabilities, getAgentPlatformOptions, listAgents, resetAgentPassword, transferAgentPoints } from '@/api/server/markAgentManagement'
const route=useRoute(); const {proxy}=getCurrentInstance()
const loading=ref(false),saving=ref(false),platformLoading=ref(false),createOpen=ref(false),detailOpen=ref(false),allocateOpen=ref(false)
const rows=ref([]),platforms=ref([]),activeRow=reactive({}),capabilities=reactive({agentLevel:0,canCreate:false,createLevel:null,canAllocate:false,operationType:'NONE'})
const query=reactive({keyword:'',status:''}); const createForm=reactive({userName:'',nickName:'',phonenumber:'',password:'',agentLevel:null}); const allocateForm=reactive({platformCode:'',platformName:'',amount:1,reason:'平台次数充值',operatorBalance:null})
const agentLevel=computed(()=>Number(route.query.agentLevel||1)); const levelName=computed(()=>['','一级代理','下级代理','普通账户'][agentLevel.value]); const canCreateHere=computed(()=>capabilities.canCreate&&Number(capabilities.createLevel)===agentLevel.value)
const allocateButton=computed(()=>capabilities.operationType==='RECHARGE'?'平台充值次数':'充值次数'); const maxAmount=computed(()=>capabilities.operationType==='TRANSFER'?Number(allocateForm.operatorBalance||0):999999999)
const createRules={userName:[{required:true,message:'请输入登录账号',trigger:'blur'},{min:2,max:20,message:'长度为2到20个字符',trigger:'blur'}],nickName:[{required:true,message:'请输入账号昵称',trigger:'blur'}],password:[{required:true,message:'请输入初始密码',trigger:'blur'},{min:5,max:20,message:'长度为5到20个字符',trigger:'blur'}],phonenumber:[{pattern:/^1[3-9]\d{9}$/,message:'手机号格式不正确',trigger:'blur'}]}
async function loadCapabilities(){const r=await getAgentCapabilities();Object.assign(capabilities,r.data||{})}
async function loadList(){loading.value=true;try{const r=await listAgents({agentLevel:agentLevel.value,...query});rows.value=r.data||[]}finally{loading.value=false}}
function openCreate(){Object.assign(createForm,{userName:'',nickName:'',phonenumber:'',password:'123456',agentLevel:agentLevel.value});createOpen.value=true;nextTick(()=>proxy.$refs.createRef?.clearValidate())}
function submitCreate(){proxy.$refs.createRef.validate(async ok=>{if(!ok)return;saving.value=true;try{await createAgent(createForm);proxy.$modal.msgSuccess('创建成功');createOpen.value=false;await loadList()}finally{saving.value=false}})}
function viewRow(row){Object.assign(activeRow,row);detailOpen.value=true}
function canAllocateRow(row){return capabilities.canAllocate && (!capabilities.isAdmin || Number(row.agentLevel)===1)}
async function toggleStatus(row){const next=row.status==='0'?'1':'0';await proxy.$modal.confirm(`确认${next==='1'?'停用':'启用'}账号 ${row.userName} 吗？`);await changeAgentStatus({userId:row.userId,status:next});proxy.$modal.msgSuccess('状态已更新');await loadList()}
async function resetPassword(row){const {value}=await proxy.$prompt('请输入新的初始密码','初始化密码',{inputValue:'123456',inputPattern:/^.{5,20}$/,inputErrorMessage:'密码长度为5到20个字符'});await resetAgentPassword({userId:row.userId,password:value});proxy.$modal.msgSuccess('密码已初始化')}
async function openAllocate(row){Object.assign(activeRow,row);Object.assign(allocateForm,{platformCode:'',platformName:'',amount:1,reason:capabilities.operationType==='RECHARGE'?'管理员平台次数充值':'补充次数',operatorBalance:null});allocateOpen.value=true;platformLoading.value=true;try{const r=await getAgentPlatformOptions(row.userId);platforms.value=r.data?.platforms||[]}finally{platformLoading.value=false}}
function selectPlatform(row){Object.assign(allocateForm,{platformCode:row.platformCode,platformName:row.platformName,amount:1,operatorBalance:row.operatorBalance})}
async function submitAllocate(){if(capabilities.operationType==='TRANSFER'&&allocateForm.amount>Number(allocateForm.operatorBalance||0)){proxy.$modal.msgError('当前平台剩余次数不足，无法充值');return}saving.value=true;try{const data={requestNo:crypto.randomUUID(),targetUserId:activeRow.userId,platformCode:allocateForm.platformCode,platformName:allocateForm.platformName,amount:allocateForm.amount,reason:allocateForm.reason};capabilities.operationType==='RECHARGE'?await adminRechargeAgent(data):await transferAgentPoints(data);proxy.$modal.msgSuccess(`${allocateButton.value}成功`);allocateOpen.value=false;await loadList()}finally{saving.value=false}}
watch(agentLevel,loadList); onMounted(async()=>{await loadCapabilities();await loadList()})
</script>
<style scoped>
.toolbar{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px}.toolbar h2{margin:0 0 3px;font-size:20px;letter-spacing:0}.toolbar span{color:var(--el-text-color-secondary);font-size:12px}.toolbar-actions{display:flex;gap:8px}.search-form{margin-bottom:4px}.allocate-form{margin-top:18px;border-top:1px solid var(--el-border-color-lighter);padding-top:16px}@media(max-width:700px){.toolbar{align-items:flex-start;flex-direction:column}.toolbar-actions{width:100%}}
</style>
