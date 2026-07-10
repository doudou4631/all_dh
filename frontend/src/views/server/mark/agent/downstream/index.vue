<template>
  <div class="app-container mark-agent-downstream-page">
    <el-card shadow="never">
      <template #header>
        <div class="panel-head">
          <div>
            <span class="panel-head__title">用户管理</span>
            <span class="panel-head__desc">管理下线用户的平台开关、剩余次数和资金流水</span>
          </div>
          <div class="panel-head__actions">
            <el-button type="primary" plain size="small" @click="goAccountManage">账号资料</el-button>
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
        <el-table-column label="总剩余次数" prop="totalRemainCount" width="110" align="center">
          <template #default="scope"><span class="remain-value">{{ scope.row.totalRemainCount ?? 0 }}</span></template>
        </el-table-column>
        <el-table-column label="账号状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'" size="small">
              {{ scope.row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openPlatformManage(scope.row)">平台管理</el-button>
            <el-button link type="success" @click="openPlatformManage(scope.row)">充值/扣减</el-button>
            <el-button link @click="goWallet(scope.row)">流水</el-button>
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

    <el-drawer v-model="platformDrawer.open" :title="platformDrawerTitle" size="760px" append-to-body>
      <div class="platform-toolbar">
        <div class="platform-toolbar__tip">
          关闭某个平台后，该用户打开对应提交页会提示“平台未开启，请联系管理员”，不能提交订单。
        </div>
        <el-button size="small" icon="Refresh" @click="loadPlatformOptions">刷新平台</el-button>
      </div>

      <el-table v-loading="platformLoading" :data="platformOptions" border stripe>
        <el-table-column label="平台" prop="platformName" min-width="150" show-overflow-tooltip />
        <el-table-column label="编码" prop="platformCode" min-width="130" show-overflow-tooltip />
        <el-table-column label="状态" width="110" align="center">
          <template #default="scope">
            <el-switch
              :model-value="isPlatformEnabled(scope.row)"
              active-text="开启"
              inactive-text="关闭"
              inline-prompt
              :loading="statusLoadingKey === platformRowKey(scope.row)"
              @change="(val) => handlePlatformStatusChange(scope.row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="单价" prop="unitPrice" width="80" align="center" />
        <el-table-column label="剩余次数" prop="remainCount" width="100" align="center">
          <template #default="scope"><span class="remain-value">{{ scope.row.remainCount ?? 0 }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="scope">
            <el-button link type="success" @click="openAdjustDialog(scope.row, 'ADD')">充值</el-button>
            <el-button link type="danger" @click="openAdjustDialog(scope.row, 'SUBTRACT')">扣减</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog v-model="adjustDialog.open" :title="adjustDialogTitle" width="420px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="用户">
          <span>{{ selectedUser?.userName || '-' }}</span>
        </el-form-item>
        <el-form-item label="平台">
          <span>{{ adjustForm.platformName || '-' }}</span>
        </el-form-item>
        <el-form-item label="当前剩余">
          <span class="remain-value">{{ adjustForm.currentRemain }}</span>
        </el-form-item>
        <el-form-item label="变动次数">
          <el-input-number v-model="adjustForm.changeCount" :min="1" :step="1" controls-position="right" style="width: 180px;" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="adjustForm.remark" maxlength="100" show-word-limit placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialog.open = false">取消</el-button>
        <el-button type="primary" :loading="adjustLoading" @click="submitAdjust">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MarkAgentDownstream">
import {
  adjustMarkAgentQuota,
  listMarkAgentDownstreamSummary,
  listMarkAgentQuotaPlatformOptions,
  setMarkAgentPlatformStatus
} from '@/api/server/markAgent'
import { useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()
const router = useRouter()
const loading = ref(false)
const total = ref(0)
const accountList = ref([])
const keyword = ref('')
const queryParams = reactive({ pageNum: 1, pageSize: 10 })

const selectedUser = ref(null)
const platformDrawer = reactive({ open: false })
const platformLoading = ref(false)
const platformOptions = ref([])
const statusLoadingKey = ref('')

const adjustDialog = reactive({ open: false, type: 'ADD' })
const adjustLoading = ref(false)
const adjustForm = reactive({
  platformCode: '',
  platformName: '',
  currentRemain: 0,
  changeCount: 1,
  remark: ''
})

const filteredList = computed(() => {
  const text = String(keyword.value || '').trim().toLowerCase()
  if (!text) return accountList.value
  return accountList.value.filter((item) => {
    const source = [item.userName, item.nickName, item.remark].map((v) => String(v || '').toLowerCase()).join(' ')
    return source.includes(text)
  })
})

const platformDrawerTitle = computed(() => {
  const user = selectedUser.value
  return user ? `平台管理 - ${user.userName || user.nickName || user.userId}` : '平台管理'
})

const adjustDialogTitle = computed(() => {
  return adjustDialog.type === 'ADD' ? '平台充值' : '平台扣减'
})

function getList() {
  loading.value = true
  listMarkAgentDownstreamSummary(queryParams).then((res) => {
    accountList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleFilter() {
  queryParams.pageNum = 1
  getList()
}

function resetFilter() {
  keyword.value = ''
  handleFilter()
}

function goAccountManage() {
  router.push('/mark/agentAccount')
}

function goWallet(row) {
  router.push({ path: '/mark/agentWallet', query: { userId: row?.userId } })
}

function platformRowKey(row) {
  return `${selectedUser.value?.userId || ''}:${String(row?.platformCode || '').toLowerCase()}`
}

function isPlatformEnabled(row) {
  return String(row?.status ?? '0') !== '1'
}

function openPlatformManage(row) {
  selectedUser.value = row
  platformDrawer.open = true
  loadPlatformOptions()
}

function loadPlatformOptions() {
  if (!selectedUser.value?.userId) return
  platformLoading.value = true
  listMarkAgentQuotaPlatformOptions(selectedUser.value.userId).then((res) => {
    platformOptions.value = (Array.isArray(res?.data) ? res.data : []).map((item) => ({
      ...item,
      status: String(item?.status ?? '0')
    }))
  }).finally(() => {
    platformLoading.value = false
  })
}

async function handlePlatformStatusChange(row, enabled) {
  if (!selectedUser.value?.userId || !row?.platformCode) return
  const key = platformRowKey(row)
  statusLoadingKey.value = key
  const nextStatus = enabled ? '0' : '1'
  const oldStatus = String(row.status ?? '0')
  row.status = nextStatus
  try {
    await setMarkAgentPlatformStatus({
      userId: selectedUser.value.userId,
      platformCode: row.platformCode,
      platformName: row.platformName,
      status: nextStatus
    })
    proxy.$modal.msgSuccess(enabled ? '平台已开启' : '平台已关闭')
  } catch (error) {
    row.status = oldStatus
    proxy.$modal.msgError(error?.message || '平台状态更新失败')
  } finally {
    statusLoadingKey.value = ''
  }
}

function openAdjustDialog(row, type) {
  adjustDialog.type = type
  adjustForm.platformCode = row.platformCode
  adjustForm.platformName = row.platformName
  adjustForm.currentRemain = Number(row.remainCount ?? 0)
  adjustForm.changeCount = 1
  adjustForm.remark = type === 'ADD' ? '代理平台充值' : '代理平台扣减'
  adjustDialog.open = true
}

async function submitAdjust() {
  if (!selectedUser.value?.userId) return
  if (!adjustForm.platformCode) {
    proxy.$modal.msgError('请选择平台')
    return
  }
  const count = Number(adjustForm.changeCount || 0)
  if (!Number.isFinite(count) || count <= 0) {
    proxy.$modal.msgError('变动次数必须大于0')
    return
  }
  if (adjustDialog.type === 'SUBTRACT' && count > Number(adjustForm.currentRemain || 0)) {
    proxy.$modal.msgError('当前平台剩余次数不足')
    return
  }
  adjustLoading.value = true
  try {
    await adjustMarkAgentQuota({
      userId: selectedUser.value.userId,
      platformCode: adjustForm.platformCode,
      platformName: adjustForm.platformName,
      adjustType: adjustDialog.type,
      changeCount: count,
      remark: adjustForm.remark
    })
    proxy.$modal.msgSuccess(adjustDialog.type === 'ADD' ? '充值成功' : '扣减成功')
    adjustDialog.open = false
    await loadPlatformOptions()
    await getList()
  } catch (error) {
    proxy.$modal.msgError(error?.message || '操作失败')
  } finally {
    adjustLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.panel-head__title { font-size: 15px; font-weight: 600; }
.panel-head__desc { margin-left: 10px; color: var(--el-text-color-secondary); font-size: 12px; }
.panel-head__actions { display: flex; gap: 8px; }
.search-bar { margin-bottom: 12px; padding-bottom: 12px; border-bottom: 1px solid var(--el-border-color-lighter); }
.search-bar :deep(.el-form-item) { margin-bottom: 0; }
.remain-value { color: var(--el-color-primary); font-weight: 600; }
.platform-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.platform-toolbar__tip { color: var(--el-text-color-secondary); font-size: 13px; line-height: 1.5; }

@media (max-width: 768px) {
  .panel-head,
  .platform-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .panel-head__desc {
    display: block;
    margin-left: 0;
    margin-top: 4px;
  }

  .panel-head__actions,
  .platform-toolbar :deep(.el-button-group),
  .quick-actions {
    width: 100%;
  }

  .panel-head__actions :deep(.el-button),
  .search-bar :deep(.el-button),
  .platform-toolbar :deep(.el-button) {
    flex: 1;
  }

  .search-bar :deep(.el-form) {
    width: 100%;
  }

  .mark-agent-downstream-page :deep(.el-table) {
    min-width: 860px;
  }
}
</style>
