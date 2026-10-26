<template>
  <div class="app-container">
    <el-form :model="query" inline @submit.prevent="search">
      <el-form-item label="策略名称"><el-input v-model="query.policyName" clearable @keyup.enter="search"/></el-form-item>
      <el-form-item label="执行状态"><el-select v-model="query.runStatus" clearable style="width:140px" @change="search"><el-option label="成功" value="SUCCESS"/><el-option label="部分失败" value="PARTIAL"/><el-option label="失败" value="FAILED"/></el-select></el-form-item>
      <el-form-item label="成功"><el-select v-model="query.successFilter" clearable style="width:120px" @change="search"><el-option label="有成功" value="SUCCESS"/><el-option label="无成功" value="NO_SUCCESS"/></el-select></el-form-item>
      <el-form-item label="显示记录"><el-select v-model="query.latestOnly" style="width:145px" @change="search"><el-option label="每个平台最新" value="1"/><el-option label="全部历史记录" value="0"/></el-select></el-form-item>
      <el-form-item label="执行方式"><el-select v-model="query.triggerType" clearable style="width:130px" @change="search"><el-option label="定时执行" value="SCHEDULED"/><el-option label="手动执行" value="MANUAL"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" :icon="Search" @click="search">查询</el-button><el-button :icon="Refresh" @click="reset">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column label="执行编号" prop="runNo" min-width="155"/>
      <el-table-column label="策略名称" prop="policyName" min-width="150"/>
      <el-table-column label="平台" min-width="140"><template #default="{row}"><div>{{row.platformName}}</div><code>{{row.platformCode}}</code></template></el-table-column>
      <el-table-column label="执行方式" width="100" align="center"><template #default="{row}">{{row.triggerType==='MANUAL'?'手动执行':'定时执行'}}</template></el-table-column>
      <el-table-column label="扫描" prop="scannedCount" width="72" align="center"/><el-table-column label="实际检测" prop="checkedCount" width="90" align="center"/><el-table-column label="仍有标记" prop="markedCount" width="90" align="center"/>
      <el-table-column label="已处理" prop="successCount" width="80" align="center"/><el-table-column label="检测失败" prop="failedCount" width="90" align="center"/><el-table-column label="跳过" prop="skippedCount" width="72" align="center"/>
      <el-table-column label="状态" width="100" align="center"><template #default="{row}"><el-tag :type="statusType(row.runStatus)">{{statusText(row.runStatus)}}</el-tag></template></el-table-column>
      <el-table-column label="开始时间" prop="startTime" min-width="165"/><el-table-column label="结束时间" prop="endTime" min-width="165"/>
      <el-table-column label="失败原因" prop="errorMessage" min-width="180" show-overflow-tooltip/>
      <el-table-column label="操作" width="90" fixed="right" align="center">
        <template #default="{row}"><el-button link type="primary" :icon="View" @click="openDetails(row)">详情</el-button></template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="load"/>

    <el-dialog v-model="detailOpen" :title="`巡检明细 - ${activeLog.runNo || ''}`" width="1120px" append-to-body destroy-on-close>
      <el-descriptions :column="4" border class="detail-summary">
        <el-descriptions-item label="策略">{{ activeLog.policyName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="平台">{{ activeLog.platformName || activeLog.platformCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际检测">{{ activeLog.checkedCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="已处理">{{ activeLog.successCount || 0 }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-loading="detailLoading" :data="detailRows" border stripe max-height="520">
        <el-table-column label="号码" prop="phone" min-width="125"/>
        <el-table-column label="订单号" prop="orderNo" min-width="180" show-overflow-tooltip/>
        <el-table-column label="检测结果" width="110" align="center">
          <template #default="{row}"><el-tag :type="detectionType(row.detectionResult)">{{ row.detectionResult || '未返回结果' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="符合条件" width="100" align="center">
          <template #default="{row}"><el-tag :type="row.qualified === '1' ? 'success' : 'info'">{{ row.qualified === '1' ? '符合' : '不符合' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="本次处理" width="110" align="center">
          <template #default="{row}"><el-tag :type="submissionType(row.submissionStatus)">{{ submissionText(row.submissionStatus) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="处理结果" prop="submissionResult" min-width="115"/>
        <el-table-column label="详细说明" prop="processNote" min-width="260" show-overflow-tooltip/>
        <el-table-column label="检测时间" prop="detectedTime" min-width="165"/>
      </el-table>
      <pagination v-show="detailTotal>0" :total="detailTotal" v-model:page="detailQuery.pageNum" v-model:limit="detailQuery.pageSize" @pagination="loadDetails"/>
      <template #footer><el-button @click="detailOpen=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup name="MarkInspectionLog">
import { Refresh, Search, View } from '@element-plus/icons-vue'; import { listInspectionLogs, listInspectionLogDetails } from '@/api/server/markInspection'
const loading=ref(false),rows=ref([]),total=ref(0);const query=reactive({pageNum:1,pageSize:10,policyName:'',runStatus:'',successFilter:'',latestOnly:'1',triggerType:''})
const detailOpen=ref(false),detailLoading=ref(false),detailRows=ref([]),detailTotal=ref(0),activeLog=ref({});const detailQuery=reactive({pageNum:1,pageSize:10})
async function load(){loading.value=true;try{const params={pageNum:query.pageNum,pageSize:query.pageSize,policyName:query.policyName.trim(),runStatus:query.runStatus||'',successFilter:query.successFilter||'',latestOnly:query.latestOnly||'0',triggerType:query.triggerType||''};const r=await listInspectionLogs(params);rows.value=r.rows||[];total.value=Number(r.total)||0}finally{loading.value=false}}
async function loadDetails(){if(!activeLog.value.id)return;detailLoading.value=true;try{const r=await listInspectionLogDetails(activeLog.value.id,detailQuery);detailRows.value=r.rows||[];detailTotal.value=r.total||0}finally{detailLoading.value=false}}
function openDetails(row){activeLog.value={...row};Object.assign(detailQuery,{pageNum:1,pageSize:10});detailRows.value=[];detailTotal.value=0;detailOpen.value=true;loadDetails()}
async function search(){query.pageNum=1;await load()}async function reset(){Object.assign(query,{pageNum:1,pageSize:10,policyName:'',runStatus:'',successFilter:'',latestOnly:'1',triggerType:''});await load()}
function statusText(v){return {RUNNING:'执行中',SUCCESS:'执行完成',PARTIAL:'存在检测失败',FAILED:'执行失败'}[v]||v}function statusType(v){return {SUCCESS:'success',PARTIAL:'warning',FAILED:'danger',RUNNING:'primary'}[v]||'info'}load()
function detectionType(v){return v==='无标记'?'success':v==='有标记'?'danger':v==='查询失败'?'warning':'info'}
function submissionText(v){return {SUCCESS:'已处理',FAILED:'检测或处理失败',SKIPPED:'等待或已跳过'}[v]||v||'等待或已跳过'}
function submissionType(v){return {SUCCESS:'success',FAILED:'danger',SKIPPED:'info'}[v]||'info'}
</script>
<style scoped>code{color:var(--el-text-color-secondary)}.detail-summary{margin-bottom:16px}</style>
