<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getFreeQueryQuota, queryFreeSingle } from '@/api/server/freeQueryApi'

const platformOrder = [
  '泰迪熊',
  '腾讯',
  '360手机卫士',
  '百度',
  '搜狗',
  '移动高频',
  '联通管家',
  '电话邦',
  '小米手机'
]

const platformKeyToName = {
  teddy: '泰迪熊',
  taidixiong: '泰迪熊',
  tengxun: '腾讯',
  tencent: '腾讯',
  '360': '360手机卫士',
  sanliuling: '360手机卫士',
  baidu: '百度',
  sogou: '搜狗',
  sghmt: '搜狗',
  mobile_hf: '移动高频',
  yidonggaopin: '移动高频',
  unicom: '联通管家',
  ltgj: '联通管家',
  dianhuabang: '电话邦',
  xiaomi: '小米手机'
}

const platformNameToKeys = Object.entries(platformKeyToName).reduce((acc, [key, name]) => {
  if (!acc[name]) {
    acc[name] = []
  }
  acc[name].push(key)
  return acc
}, {})

const canceledMessageKeywords = ['泰迪未来标记已取消', '同步时间']
const props = defineProps({
  /** 公开页底部二维码图片地址（public 目录下的静态资源路径） */
  customerWechatImage: { type: String, default: '/wechat/customer-wechat1.png' }
})

const phone = ref('')
const querying = ref(false)
const messageText = ref('')
const messageType = ref('')
const rows = ref([])
const route = useRoute()
const router = useRouter()
const isPublicShell = computed(() => /^\/free[-_]query2?(\/|$)/i.test(route.path || ''))
const quota = ref({
  ip: '-',
  limit: 20,
  used: 0,
  remaining: 20,
  resetAt: '-',
  /** 配置了字典 daily_all_limit 时由后端返回：平台当日总次数上限（全站共用） */
  allLimit: undefined,
  allUsed: undefined,
  allRemaining: undefined
})
/** 免费查询不发起外呼的平台展示名（与后端 UserPlatformUrlConfig.platformName 一致，由 /quota 与查询接口返回） */
const disabledPlatforms = ref([])
/** 最近一次成功查询返回的 results 原数组；loadQuota 刷新额度时据此重绘，避免把结果行刷成 “-” */
const lastQueryResults = ref([])
/** 是否已完成过至少一次成功查询；用于「未开放」平台在查询后展示「无标记」 */
const hadSuccessfulFreeQuery = ref(false)

const messageClass = computed(() => (messageType.value ? `msg ${messageType.value}` : 'msg'))

function norm(value) {
  return typeof value === 'string' ? value.replace(/\s+/g, '').trim() : ''
}

function isCanceledMessage(value) {
  const normalized = norm(value)
  return canceledMessageKeywords.every((token) => normalized.includes(token))
}

function parseOne(item) {
  if (!item || typeof item !== 'object') {
    return { status: '-', message: '-' }
  }
  if (typeof item.error === 'string' && item.error.trim()) {
    return { status: '查询失败', message: item.error.trim() }
  }
  const payload = item.data && typeof item.data === 'object' ? item.data : null
  const platformResults = payload?.platformResults
  const rawResult = Array.isArray(platformResults) && platformResults.length > 0 ? platformResults[0]?.status : ''

  if (rawResult) {
    const statusText = String(rawResult)
    if (statusText.startsWith('yes-')) {
      const msg = statusText.slice(4).trim()
      if (isCanceledMessage(msg)) return { status: '无标记', message: '无标记' }
      return { status: '有标记', message: msg || '普通标记' }
    }
    if (statusText.startsWith('no-')) return { status: '无标记', message: '无标记' }
    if (statusText === 'yes') return { status: '有标记', message: '普通标记' }
    if (statusText === 'no' || /no/i.test(statusText)) return { status: '无标记', message: '无标记' }
  }

  return { status: '查询失败', message: item.error || '查询失败' }
}

function buildPlatformMap(list) {
  const map = {}
  if (!Array.isArray(list)) {
    return map
  }
  list.forEach((item) => {
    const displayName = item.platformName || platformKeyToName[item.platform] || platformKeyToName[item.name]
    if (displayName) {
      map[displayName] = item
      return
    }
    const innerStatus = item?.data?.platformResults?.[0]?.platform
    const name = platformKeyToName[innerStatus] || innerStatus
    if (name) {
      map[name] = item
    }
  })
  return map
}

function rank(status) {
  if (status === '有标记') return 0
  if (status === '无标记') return 1
  if (status === '查询失败') return 2
  if (status === '未开放') return 4
  return 3
}

function applyDisabledFromPayload(payload) {
  if (!payload || typeof payload !== 'object') {
    return
  }
  const raw = payload.disabledPlatforms
  if (Array.isArray(raw)) {
    disabledPlatforms.value = raw.map((x) => String(x).trim()).filter(Boolean)
    return
  }
  if (typeof raw === 'string' && raw.trim()) {
    disabledPlatforms.value = raw.split(',').map((s) => s.trim()).filter(Boolean)
  }
}

function renderRows(platformMap) {
  const disabled = new Set(disabledPlatforms.value)
  const nextRows = platformOrder.map((name) => {
    if (disabled.has(name)) {
      const msg = hadSuccessfulFreeQuery.value ? '无标记' : '-'
      return {
        name,
        skipped: true,
        parsed: { status: '未开放', message: msg }
      }
    }
    let item = platformMap[name]
    if (!item && platformNameToKeys[name]) {
      for (const key of platformNameToKeys[name]) {
        if (platformMap[key]) {
          item = platformMap[key]
          break
        }
      }
    }
    return { name, parsed: parseOne(item) }
  })
  nextRows.sort((a, b) => {
    const ra = rank(a.parsed.status)
    const rb = rank(b.parsed.status)
    if (ra !== rb) return ra - rb
    return platformOrder.indexOf(a.name) - platformOrder.indexOf(b.name)
  })
  rows.value = nextRows
}

function setMsg(text, type = '') {
  messageText.value = text
  messageType.value = type
}

function clearInput() {
  phone.value = ''
  setMsg('', '')
  lastQueryResults.value = []
  hadSuccessfulFreeQuery.value = false
  renderRows(buildPlatformMap([]))
}

async function loadQuota() {
  const res = await getFreeQueryQuota()
  const payload = res?.data ?? res
  if (payload && typeof payload === 'object') {
    quota.value = {
      ...quota.value,
      ...payload
    }
    applyDisabledFromPayload(payload)
    renderRows(buildPlatformMap(lastQueryResults.value))
  }
}

async function doQuery() {
  if (!phone.value.trim()) {
    setMsg('请输入号码', 'err')
    return
  }
  querying.value = true
  setMsg('查询中...', '')
  try {
    const res = await queryFreeSingle({ phone: phone.value.trim() })
    if (res?.quota) {
      quota.value = { ...quota.value, ...res.quota }
      applyDisabledFromPayload(res.quota)
    }
    if (res?.code === 42901) {
      window.alert(res?.msg || '当前IP今日免费查询次数已达上限，请添加客服微信查询。')
      setMsg(res?.msg || '查询失败', 'err')
      lastQueryResults.value = []
      hadSuccessfulFreeQuery.value = false
      renderRows(buildPlatformMap([]))
      return
    }
    if (res?.code === 42902) {
      window.alert(res?.msg || '平台当日免费额度已用完，请联系客服')
      setMsg(res?.msg || '查询失败', 'err')
      lastQueryResults.value = []
      hadSuccessfulFreeQuery.value = false
      renderRows(buildPlatformMap([]))
      await loadQuota()
      return
    }
    const okCode = Number(res?.code)
    if (okCode !== 200 && okCode !== 0) {
      setMsg(res?.msg || '查询失败', 'err')
      lastQueryResults.value = []
      hadSuccessfulFreeQuery.value = false
      renderRows(buildPlatformMap([]))
      return
    }
    const resultList = Array.isArray(res?.data?.results) ? res.data.results : []
    lastQueryResults.value = resultList
    hadSuccessfulFreeQuery.value = true
    if (res?.data && typeof res.data === 'object') {
      applyDisabledFromPayload(res.data)
    }
    renderRows(buildPlatformMap(resultList))
    setMsg('查询成功', 'ok')
    await loadQuota()
  } catch (error) {
    setMsg(`请求失败：${error.message || error}`, 'err')
  } finally {
    querying.value = false
  }
}

function goLogManage() {
  const currentPath = route.path || ''
  const target = /apiquery\/?$/i.test(currentPath)
    ? currentPath.replace(/apiquery\/?$/i, 'apilog')
    : '/server/web/apilog'
  router.push(target).catch(() => {})
}

lastQueryResults.value = []
renderRows(buildPlatformMap([]))
loadQuota()
</script>

<template>
  <div class="app-container free-query-wrap" :class="{ 'public-free-query-wrap': isPublicShell }">
    <el-card shadow="never" class="query-card" :class="{ 'public-query-card': isPublicShell }">
      <div class="title">号码标记免费查询平台</div>
      <div class="quota-row" :class="{ 'public-quota-row': isPublicShell }">
        <!-- <el-tag type="success">当前IP：{{ quota.ip }}</el-tag> -->
        <el-tag type="warning">今日免费额度：{{ quota.remaining }}/{{ quota.limit }}</el-tag>
        <!-- <el-tag type="info">已使用：{{ quota.used }}</el-tag> -->
        <!-- <el-tag
          v-if="quota.allLimit != null && quota.allRemaining != null"
          type="danger"
          effect="plain"
        >
          平台今日剩余免费额度：{{ quota.allRemaining }}/{{ quota.allLimit }}
        </el-tag> -->
        <!-- <el-tag>重置时间：{{ quota.resetAt }}</el-tag> -->
      </div>

      <div class="query-row">
        <el-input
          v-model.trim="phone"
          placeholder="输入手机号或固话号码"
          clearable
          @keyup.enter="doQuery"
        />
        <el-button class="query-btn-start" :loading="querying" type="primary" @click="doQuery">
          {{ querying ? '查询中...' : '开始查询' }}
        </el-button>
        <el-button class="query-btn-clear" :disabled="querying" @click="clearInput">清空</el-button>
        <el-button v-if="!isPublicShell" type="primary" plain @click="goLogManage">日志管理</el-button>
      </div>

      <div :class="messageClass">{{ messageText }}</div>

      <div class="result-list">
        <div v-for="row in rows" :key="row.name" class="result-card">
          <div class="result-card-top">
            <div class="platform-name" :title="row.name">{{ row.name }}</div>
            <div
              :class="{
                'result-text': true,
                marked: row.parsed.status === '有标记',
                muted: row.skipped || row.parsed.message === '-',
                'platform-free-disabled': row.skipped
              }"
            >
              {{ row.parsed.message || '-' }}
            </div>
          </div>
        </div>
      </div>

      <div v-if="isPublicShell" class="wechat-wrap">
        <div class="wechat-title">客服微信（扫码添加）</div>
        <img class="wechat-img" :src="props.customerWechatImage" alt="客服微信二维码" />
        <div class="wechat-desc">批量解除号码标记 批量免费查询号码标记</div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.free-query-wrap {
  max-width: 1080px;
  margin: 0 auto;
}

.query-card {
  border-radius: 8px;
}

.title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 16px;
}

.quota-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.query-row {
  display: grid;
  grid-template-columns: 1fr auto auto auto;
  gap: 10px;
  margin-bottom: 10px;
}

.msg {
  min-height: 24px;
  line-height: 24px;
  color: var(--el-text-color-regular);
  margin-bottom: 10px;
}

.msg.ok {
  color: var(--el-color-success);
}

.msg.err {
  color: var(--el-color-danger);
}

.result-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.result-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 10px;
  background: #fff;
}

.result-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.platform-name {
  font-weight: 600;
}

.result-text {
  color: var(--el-text-color-primary);
}

.result-text.marked {
  color: var(--el-color-danger);
  font-weight: 600;
}

.result-text.muted {
  color: var(--el-text-color-secondary);
}

.result-text.platform-free-disabled {
  font-style: normal;
  color: var(--el-text-color-secondary);
}

.public-free-query-wrap {
  max-width: 420px;
  margin: 0 auto;
  padding: 8px 0;
}

.public-query-card {
  border-radius: 14px;
  background: #f7f7f8;
  border: 1px solid #ededf0;
}

.public-query-card :deep(.el-card__body) {
  padding: 14px 12px 16px;
}

.public-query-card .title {
  font-size: 20px;
  line-height: 1.2;
  text-align: center;
  margin-bottom: 14px;
}

.public-quota-row {
  gap: 6px;
  margin-bottom: 12px;
}

.public-quota-row :deep(.el-tag) {
  font-size: 12px;
  line-height: 18px;
  padding: 0 8px;
}

.public-query-card .query-row {
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.public-query-card .query-row :deep(.el-button) {
  min-height: 40px;
  border-radius: 20px;
}

.public-query-card .query-row :deep(.el-input__wrapper) {
  min-height: 40px;
  border-radius: 10px;
}

.public-query-card .query-row .query-btn-clear {
  grid-column: 2;
  grid-row: 1;
}

.public-query-card .query-row .query-btn-start {
  grid-column: 1 / -1;
  grid-row: 2;
}

.public-query-card .result-list {
  grid-template-columns: 1fr;
  gap: 4px;
}

.public-query-card .result-card {
  border-radius: 10px;
  background: #ffffff;
  padding: 8px 12px;
}

.public-query-card .platform-name {
  font-size: 14px;
}

.public-query-card .result-text {
  font-size: 12px;
}

.wechat-wrap {
  margin-top: 10px;
  padding: 12px 10px 4px;
  border-top: 1px solid #ececf0;
  text-align: center;
}

.wechat-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.wechat-img {
  width: 180px;
  height: 180px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.wechat-desc {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

@media (max-width: 980px) {
  .result-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .query-row {
    grid-template-columns: 1fr;
  }
  .result-list {
    grid-template-columns: 1fr;
  }
}
</style>