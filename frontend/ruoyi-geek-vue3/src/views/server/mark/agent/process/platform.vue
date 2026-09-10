<template>
  <div class="app-container mark-agent-process-platform">
    <el-tabs v-if="isTdxSecond" v-model="activeView" class="tdx-second-tabs">
      <el-tab-pane label="处理" name="process">
        <ProcessWorkbench
          :key="workbenchKey"
          :platform-code="platformCode"
          :platform-codes="platformCodes"
          :page-title="platformTitle"
          :show-supply-tabs="false"
        />
      </el-tab-pane>
      <el-tab-pane v-if="canViewAudit" label="上游审计" name="audit">
        <TdxAuditPanel />
      </el-tab-pane>
    </el-tabs>
    <ProcessWorkbench
      v-else
      :key="workbenchKey"
      :platform-code="platformCode"
      :platform-codes="platformCodes"
      :page-title="platformTitle"
      :show-supply-tabs="false"
    />
  </div>
</template>

<script setup name="MarkAgentProcessPlatform">
import ProcessWorkbench from '../components/ProcessWorkbench.vue'
import TdxAuditPanel from '../components/TdxAuditPanel.vue'
import { computed } from 'vue'
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import useUserStore from '@/store/modules/user'
import { resolveMarkPlatformCodeFromRoute } from '@/utils/markPlatformRoute'

const route = useRoute()
const userStore = useUserStore()
const activeView = ref('process')
const platformCodes = computed(() => String(route.query.platformCodes || '').trim())
const platformCode = computed(() => platformCodes.value ? '' : resolveMarkPlatformCodeFromRoute(route))
const isTdxSecond = computed(() => platformCode.value === 'td_second' || String(route.query.platformCode || '').toLowerCase() === 'td_second')
const canViewAudit = computed(() => {
  const name = String(userStore.name || '').trim().toLowerCase()
  return name === 'admin' || userStore.roles.some((role) => String(role).toLowerCase() === 'admin' || String(role).toLowerCase() === 'role_admin')
})
const platformNames = Object.freeze({
  taidixiong: '泰迪熊',
  td_gaopin: 'TdGaopin高频',
  td_second: 'Taidixiong二次',
  tengxun: 'Tengxun腾讯',
  qihu_second: '360二次',
  baidu: 'Baidu百度',
  sogou: 'Sougou',
  yidonggaopin: 'YidongGaopin移动高频',
  dianhuabang: 'Dianhuabang',
  ltgj: 'LiantongGuanjia',
  xiaomi: 'Xiaomi小米'
})
const platformTitle = computed(() => platformNames[platformCode.value] || String(route.meta?.title || route.query.platformName || '').trim())
const workbenchKey = computed(() => `${route.path}|${platformCode.value}|${platformCodes.value}`)
</script>

<style scoped>
.mark-agent-process-platform {
  padding: 0 12px 12px 12px;
  margin: 0;
  max-width: none;
}

@media (max-width: 768px) {
  .mark-agent-process-platform {
    padding: 8px !important;
  }
}
</style>
