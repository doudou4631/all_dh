<template>
  <div class="app-container">
    <div class="toolbar">
      <div><h2>自动化 API 管理</h2><span>Secret 仅在创建或轮换时展示一次</span></div>
      <el-button v-hasPermi="['server:markOpenApi:add']" type="primary" :icon="Plus" @click="openCreate">新增 API Key</el-button>
    </div>
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="名称"><el-input v-model="query.name" clearable @keyup.enter="load" /></el-form-item>
      <el-form-item label="状态"><el-select v-model="query.status" clearable style="width:120px"><el-option label="启用" value="0"/><el-option label="停用" value="1"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" :icon="Search" @click="load">查询</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="name" label="名称" min-width="150"/><el-table-column prop="apiKey" label="API Key" min-width="250"/>
      <el-table-column prop="userName" label="绑定账号" min-width="120"/><el-table-column prop="scopes" label="权限范围" min-width="170"/>
      <el-table-column prop="expireTime" label="到期时间" min-width="170"><template #default="{row}">{{row.expireTime||'永不过期'}}</template></el-table-column>
      <el-table-column prop="lastUsedTime" label="最近调用" min-width="170"><template #default="{row}">{{row.lastUsedTime||'-'}}</template></el-table-column>
      <el-table-column label="状态" width="90"><template #default="{row}"><el-tag :type="row.status==='0'?'success':'danger'">{{row.status==='0'?'启用':'停用'}}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="180" fixed="right"><template #default="{row}"><el-button v-hasPermi="['server:markOpenApi:edit']" link :type="row.status==='0'?'danger':'success'" @click="toggle(row)">{{row.status==='0'?'停用':'启用'}}</el-button><el-button v-hasPermi="['server:markOpenApi:rotate']" link type="warning" @click="rotate(row)">轮换 Secret</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="createOpen" title="新增自动化 API Key" width="580px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px"><el-form-item label="名称" prop="name"><el-input v-model="form.name" maxlength="64"/></el-form-item><el-form-item label="绑定用户名" prop="userName"><el-input v-model="form.userName" maxlength="30" placeholder="例如 110125"/></el-form-item><el-form-item label="平台编码" prop="platformCodes"><el-select v-model="form.platformCodes" multiple filterable allow-create default-first-option placeholder="例如 xiaomi" style="width:100%"/></el-form-item><el-form-item label="接口权限" prop="scopes"><el-checkbox-group v-model="form.scopes"><el-checkbox label="MARK_READ">读取待处理项</el-checkbox><el-checkbox label="MARK_FEEDBACK">回填处理结果</el-checkbox></el-checkbox-group></el-form-item><el-form-item label="到期时间"><el-date-picker v-model="form.expireTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" clearable/></el-form-item></el-form>
      <template #footer><el-button @click="createOpen=false">取消</el-button><el-button type="primary" :loading="saving" @click="create">创建</el-button></template>
    </el-dialog>
    <el-dialog v-model="secretOpen" title="请立即保存 API Secret" width="650px" :close-on-click-modal="false" append-to-body><el-alert type="warning" :closable="false" title="关闭后无法再次查看，只能轮换生成新的 Secret。"/><el-descriptions :column="1" border class="secret"><el-descriptions-item label="API Key">{{secret.apiKey}}</el-descriptions-item><el-descriptions-item label="API Secret"><span class="secret-value">{{secret.apiSecret}}</span></el-descriptions-item></el-descriptions><template #footer><el-button type="primary" @click="secretOpen=false">我已保存</el-button></template></el-dialog>
  </div>
</template>
<script setup name="MarkOpenApi">
import { Plus, Search } from '@element-plus/icons-vue'
import { createMarkOpenApiKey, listMarkOpenApiKeys, rotateMarkOpenApiSecret, updateMarkOpenApiKeyStatus } from '@/api/server/markOpenApi'
const { proxy } = getCurrentInstance(); const loading=ref(false),saving=ref(false),createOpen=ref(false),secretOpen=ref(false); const rows=ref([]); const query=reactive({name:'',status:''}); const form=reactive({name:'',userName:'',platformCodes:[],scopes:['MARK_READ','MARK_FEEDBACK'],expireTime:null}); const secret=reactive({apiKey:'',apiSecret:''}); const rules={name:[{required:true,message:'请输入名称',trigger:'blur'}],userName:[{required:true,message:'请输入绑定用户名',trigger:'blur'}],platformCodes:[{type:'array',required:true,min:1,message:'请选择至少一个平台',trigger:'change'}],scopes:[{type:'array',required:true,min:1,message:'请选择至少一个权限',trigger:'change'}]};
async function load(){loading.value=true;try{const r=await listMarkOpenApiKeys(query);rows.value=r.data||[]}finally{loading.value=false}} function openCreate(){Object.assign(form,{name:'',userName:'',platformCodes:[],scopes:['MARK_READ','MARK_FEEDBACK'],expireTime:null});createOpen.value=true;nextTick(()=>proxy.$refs.formRef?.clearValidate())} function showSecret(data){Object.assign(secret,data||{});secretOpen.value=true} function create(){proxy.$refs.formRef.validate(async ok=>{if(!ok)return;saving.value=true;try{const r=await createMarkOpenApiKey(form);createOpen.value=false;showSecret(r.data);await load()}finally{saving.value=false}})} async function toggle(row){await proxy.$modal.confirm(`确认${row.status==='0'?'停用':'启用'} ${row.name} 吗？`);await updateMarkOpenApiKeyStatus({id:row.id,status:row.status==='0'?'1':'0'});proxy.$modal.msgSuccess('状态已更新');load()} async function rotate(row){await proxy.$modal.confirm(`轮换 ${row.name} 的 Secret 后，旧 Secret 会立即失效。`);const r=await rotateMarkOpenApiSecret(row.id);showSecret(r.data)} onMounted(load)
</script>
<style scoped>.toolbar{display:flex;justify-content:space-between;align-items:center;margin-bottom:18px}.toolbar h2{margin:0 0 4px;font-size:20px}.toolbar span{font-size:12px;color:var(--el-text-color-secondary)}.secret{margin-top:16px}.secret-value{word-break:break-all;font-family:monospace}</style>
