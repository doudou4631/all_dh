<template>
  <div class="app-container mark-agent-summary-page">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="panel-head">
          <span class="panel-head__title">总代理信息</span>
          <el-button size="small" icon="Refresh" @click="loadSummary">刷新</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="Id">{{ summary.userId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="登录账号">{{ summary.userName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ summary.nickName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="代理层级">
          <el-tag type="success" size="small">{{ summary.agentLevelLabel || '一级总代' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="账户余额(次数)">
          <span class="balance-value">{{ summary.totalRemainCount ?? 0 }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="单TD单价">
          {{ summary.sampleUnitPrice ?? '-' }}
          <span v-if="summary.samplePlatformName" class="unit-platform">（{{ summary.samplePlatformName }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="下线用户数">{{ summary.downstreamCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="待审核订单">{{ summary.pendingAuditCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="消息备注" :span="2">{{ summary.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="quick-actions">
        <el-button type="primary" plain @click="goPage('/mark/agentDownstream')">下级代理</el-button>
        <el-button type="success" plain @click="goPage('/mark/agentAccount')">账户管理</el-button>
        <el-button @click="goPage('/mark/agentWallet')">资金流水</el-button>
        <el-button @click="goPage('/mark/agentAudit')">订单审核</el-button>
      </div>
    </el-card>
  </div>
</template>
<script setup name="MarkAgentSummary">
import { getMarkAgentMeSummary } from '@/api/server/markAgent'
import { useRouter } from 'vue-router'
const router = useRouter()
const loading = ref(false)
const summary = ref({})
function loadSummary() { loading.value = true; getMarkAgentMeSummary().then((res) => { summary.value = res.data || {} }).finally(() => { loading.value = false }) }
function goPage(path) { router.push(path) }
onMounted(() => { loadSummary() })
</script>
<style scoped>
.panel-head { display: flex; align-items: center; justify-content: space-between; }
.panel-head__title { font-size: 15px; font-weight: 600; }
.balance-value { color: var(--el-color-primary); font-size: 18px; font-weight: 600; }
.unit-platform { margin-left: 4px; color: var(--el-text-color-secondary); font-size: 12px; }
.quick-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 16px; }
</style>
