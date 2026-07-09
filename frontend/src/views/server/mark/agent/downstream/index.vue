<template>
  <div class="app-container mark-agent-downstream-page">
    <el-card shadow="never">
      <template #header>
        <div class="panel-head">
          <span class="panel-head__title">下线账户</span>
          <div class="panel-head__actions">
            <el-button type="primary" plain size="small" @click="goAccountManage">账户管理</el-button>
            <el-button size="small" icon="Refresh" @click="getList">刷新</el-button>
          </div>
        </div>
      </template>
      <div class="search-bar">
        <el-form :inline="true" size="small" @submit.prevent>
          <el-form-item label="用户搜索">
            <el-input v-model="keyword" clearable placeholder="登录名/昵称" style="width: 180px;" @keyup.enter="handleFilter" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleFilter">搜索</el-button>
            <el-button icon="Refresh" @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      <el-table v-loading="loading" :data="filteredList" border stripe>
        <el-table-column label="Id" prop="userId" width="80" align="center" />
        <el-table-column label="登录账号" prop="userName" min-width="120" show-overflow-tooltip />
        <el-table-column label="昵称" prop="nickName" min-width="120" show-overflow-tooltip />
        <el-table-column label="剩余次数" prop="totalRemainCount" width="100" align="center">
          <template #default="scope"><span class="remain-value">{{ scope.row.totalRemainCount ?? 0 }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'" size="small">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="消息备注" prop="remark" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="goWallet(scope.row)">流水明细</el-button>
            <el-button link type="success" @click="goAccountManage">充值调整</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>
  </div>
</template>
<script setup name="MarkAgentDownstream">
import { listMarkAgentDownstreamSummary } from '@/api/server/markAgent'
import { useRouter } from 'vue-router'
const router = useRouter()
const loading = ref(false)
const total = ref(0)
const accountList = ref([])
const keyword = ref('')
const queryParams = reactive({ pageNum: 1, pageSize: 10 })
const filteredList = computed(() => {
  const text = String(keyword.value || '').trim().toLowerCase()
  if (!text) return accountList.value
  return accountList.value.filter((item) => {
    const source = [item.userName, item.nickName, item.remark].map((v) => String(v || '').toLowerCase()).join(' ')
    return source.includes(text)
  })
})
function getList() { loading.value = true; listMarkAgentDownstreamSummary(queryParams).then((res) => { accountList.value = res.rows || []; total.value = res.total || 0 }).finally(() => { loading.value = false }) }
function handleFilter() { queryParams.pageNum = 1; getList() }
function resetFilter() { keyword.value = ''; handleFilter() }
function goAccountManage() { router.push('/mark/agentAccount') }
function goWallet(row) { router.push({ path: '/mark/agentWallet', query: { userId: row?.userId } }) }
onMounted(() => { getList() })
</script>
<style scoped>
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.panel-head__title { font-size: 15px; font-weight: 600; }
.panel-head__actions { display: flex; gap: 8px; }
.search-bar { margin-bottom: 12px; padding-bottom: 12px; border-bottom: 1px solid var(--el-border-color-lighter); }
.search-bar :deep(.el-form-item) { margin-bottom: 0; }
.remain-value { color: var(--el-color-primary); font-weight: 600; }
</style>
