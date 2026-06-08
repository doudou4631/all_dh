<template>
  <div class="batch-page">
    <h1 class="batch-title">批量查询</h1>
    <div class="batch-card">
      <textarea
        v-model="phonesText"
        class="batch-textarea"
        placeholder="请输入手机号码，每行一个&#10;例如：&#10;13800138000&#10;13900139000"
      ></textarea>
      <div class="batch-toolbar">
        <p class="batch-count">已输入 {{ inputCount }} 个号码</p>
        <div class="batch-sub-btns">
          <button type="button" class="batch-sub-btn" @click="extractNumbers">号码提取</button>
          <button type="button" class="batch-sub-btn" @click="pasteNumbers">粘贴号码</button>
          <button type="button" class="batch-sub-btn" @click="clearList">清空列表</button>
        </div>
      </div>
      <button type="button" class="batch-btn" :disabled="submitting" @click="submitBatchQuery">
        {{ submitting ? '查询中...' : '开始批量查询' }}
      </button>
    </div>

    <div v-if="progressTotal > 0" class="batch-result">
      <div class="batch-task-card">
        <div class="batch-task-row"><span>任务ID</span><span>{{ currentTaskId || (queryDone ? '—' : '生成中') }}</span></div>
        <div class="batch-task-row">
          <span>状态</span>
          <span class="batch-status" :class="queryDone ? 'batch-status--done' : 'batch-status--running'">
            {{ queryDone ? '已完成' : '查询中' }}
          </span>
        </div>
        <div class="batch-task-row"><span>进度</span><span>{{ progressCurrent }}/{{ progressTotal }}</span></div>
        <div class="batch-progress">
          <div class="batch-progress-bar" :style="{ width: `${progressPercent}%` }"></div>
        </div>
      </div>

      <div v-if="queryDone" class="batch-results-panel">
        <div class="batch-results-head">
          <h2>查询结果 ({{ currentResults.length }}条)</h2>
          <button type="button" class="batch-csv-btn" @click="downloadCsv">下载CSV</button>
        </div>
        <div class="batch-results-list">
          <button
            v-for="(item, index) in currentResults"
            :key="`${item.phone}-${index}`"
            type="button"
            class="batch-result-row"
            @click="openDetailModal(item)"
          >
            <div class="batch-result-main">
              <p class="batch-result-phone">{{ item.phone }}</p>
              <p class="batch-result-tags" :class="{ 'is-muted': item.error || !item.markedItems.length }">
                {{ getTagText(item) }}
              </p>
            </div>
            <span class="batch-result-badge" :class="getBadgeClass(item)">{{ getBadgeText(item) }}</span>
            <span class="batch-result-arrow">›</span>
          </button>
        </div>
      </div>
    </div>
  </div>

  <div class="batch-detail-modal" :hidden="!detailVisible">
    <div class="batch-detail-mask" @click="closeDetailModal"></div>
    <div class="batch-detail-panel" role="dialog" aria-modal="true" aria-label="号码详情">
      <div class="batch-detail-header">
        <h3 class="batch-detail-phone">{{ detailItem?.phone || '--' }}</h3>
        <button type="button" class="batch-detail-close" aria-label="关闭" @click="closeDetailModal">×</button>
      </div>
      <div class="batch-detail-list">
        <p v-if="detailItem?.error" class="batch-detail-empty">{{ detailItem.errorMessage || '查询失败，请稍后重试' }}</p>
        <p v-else-if="!detailItem || !detailItem.markedItems.length" class="batch-detail-empty">该号码未发现被标记平台</p>
        <template v-else>
          <div v-for="(mark, index) in detailItem.markedItems" :key="`${mark.platform}-${index}`" class="batch-detail-item">
            <div class="batch-detail-item-main">
              <p class="batch-detail-platform">{{ mark.platform }}</p>
              <p class="batch-detail-type">{{ mark.markType }}</p>
            </div>
            <span class="batch-result-badge batch-result-badge--marked">有标记</span>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useMobileRuntimeConfig } from '@/runtime/mobileRuntimeConfig'
import { fetchBatchQuery } from '@/services/freeQueryBridge'
import { addBatchRecords } from '@/services/queryRecords'
import { recordQueryForCurrentUser } from '@/services/queryStats'
import { getAccount, getToken, isLoggedIn, setPoints, setToken } from '@/services/profileSession'
import { notify } from '@/services/bottomNavUi'

const { state, resolveHref, toPath, loadMobileRuntimeConfig } = useMobileRuntimeConfig()

const phonesText = ref('')
const submitting = ref(false)
const currentTaskId = ref('')
const currentResults = ref([])
const progressCurrent = ref(0)
const progressTotal = ref(0)
const queryDone = ref(false)
const detailVisible = ref(false)
const detailItem = ref(null)

const inputCount = computed(() => parsePhones(phonesText.value).length)
const progressPercent = computed(() => {
  if (!progressTotal.value) return 0
  return Math.round((progressCurrent.value / progressTotal.value) * 100)
})

function parsePhones(text) {
  return String(text || '')
    .split(/[\r\n,，;；\s]+/)
    .map((item) => item.trim())
    .filter((item) => /^\d{7,15}$/.test(item))
    .slice(0, 20)
}

function extractNumbers() {
  const matches = String(phonesText.value || '').match(/\d{7,15}/g) || []
  const seen = {}
  const list = []
  matches.forEach((num) => {
    if (seen[num]) return
    seen[num] = true
    list.push(num)
  })
  phonesText.value = list.slice(0, 20).join('\n')
  if (!list.length) notify('未识别到有效号码')
}

function pasteNumbers() {
  if (navigator.clipboard && navigator.clipboard.readText) {
    navigator.clipboard
      .readText()
      .then((text) => {
        const current = String(phonesText.value || '').replace(/\s+$/, '')
        phonesText.value = current ? `${current}\n${text}` : text
      })
      .catch(() => {
        notify('无法读取剪贴板，请长按输入框手动粘贴')
      })
    return
  }
  notify('请长按输入框手动粘贴号码')
}

function clearList() {
  phonesText.value = ''
}

function getBatchLoginRedirectUrl() {
  const redirectTarget = toPath(resolveHref('/batch/'))
  const redirect = encodeURIComponent(redirectTarget || '/batch/')
  return resolveHref(`/profile/?redirect=${redirect}`)
}

function ensureBatchLogin() {
  if (isLoggedIn()) return true
  window.location.replace(getBatchLoginRedirectUrl())
  return false
}

function clearTokenAndRedirect(message) {
  setToken('')
  notify(message || '登录已失效，请重新登录')
  setTimeout(() => {
    window.location.assign(getBatchLoginRedirectUrl())
  }, 800)
}

function getPlatformDisplayName(name) {
  if (!name) return '未知平台'
  if (name === '360' || name === '360手机卫士') return '360手机卫士'
  if (name.indexOf('360') >= 0 && name.indexOf('卫士') < 0) return '360手机卫士'
  return name
}

function parseMarkedInfo(platformResult) {
  if (platformResult && typeof platformResult.error === 'string' && platformResult.error.trim()) {
    return { marked: false, markType: '' }
  }
  const firstResult = platformResult?.data?.platformResults?.[0]
  if (!firstResult) {
    return { marked: false, markType: '' }
  }
  const platformName = getPlatformDisplayName(platformResult?.platformName || platformResult?.platform || '')
  const isMobileHighFreq = platformName === '移动高频'
  const status = String(firstResult.status || '').trim()
  if (!status) {
    return { marked: false, markType: '' }
  }
  if (status.indexOf('yes-') === 0) {
    const markType = status.slice(4).trim()
    if (!markType || markType.indexOf('泰迪未来标记已取消') >= 0 || markType.indexOf('同步时间') >= 0) {
      return { marked: false, markType: '' }
    }
    if (isMobileHighFreq && markType === '普通标记') {
      return { marked: true, markType: '高频拦截' }
    }
    return { marked: true, markType: markType }
  }
  if (status === 'yes') {
    if (isMobileHighFreq) {
      return { marked: true, markType: '高频拦截' }
    }
    return { marked: true, markType: '普通标记' }
  }
  if (status.indexOf('no') === 0) {
    return { marked: false, markType: '' }
  }
  return { marked: false, markType: '' }
}

function getMarkedItems(results) {
  if (!Array.isArray(results) || !results.length) return []
  const list = []
  results.forEach((item) => {
    const parsed = parseMarkedInfo(item)
    if (!parsed.marked) return
    list.push({
      platform: getPlatformDisplayName(item?.platformName || item?.platform || ''),
      markType: parsed.markType || '普通标记'
    })
  })
  return list
}

function mapBatchResultItem(item) {
  const code = Number(item?.code)
  const success = code === 0 || code === 200
  const platformResults = item?.data && Array.isArray(item.data.results) ? item.data.results : []
  return {
    phone: String(item?.phone || ''),
    error: !success,
    errorMessage: success ? '' : String(item?.message || '查询失败').trim() || '查询失败',
    markedItems: success ? getMarkedItems(platformResults) : [],
    failedEntries: Number(item?.failedEntries) || 0
  }
}

function normalizeBatchResults(resultRows, fallbackPhones) {
  if (Array.isArray(resultRows) && resultRows.length) {
    return resultRows.map(mapBatchResultItem)
  }
  return (fallbackPhones || []).map((phone) => ({
    phone,
    error: true,
    errorMessage: '查询失败',
    markedItems: [],
    failedEntries: 0
  }))
}

function isAuthErrorMessage(message) {
  const text = String(message || '')
  return text.indexOf('登录') >= 0 || text.indexOf('失效') >= 0 || text.indexOf('401') >= 0
}

function applyBatchResponse(data, phones) {
  currentTaskId.value = String(data?.taskId || '').trim()
  currentResults.value = normalizeBatchResults(data?.results, phones)
  progressCurrent.value = currentResults.value.length || phones.length
  progressTotal.value = phones.length
  queryDone.value = true

  addBatchRecords(currentResults.value)
  recordQueryForCurrentUser(currentResults.value.length || phones.length)

  const remainingPoints = Number(data?.remainingPoints)
  if (!Number.isNaN(remainingPoints) && getAccount()) {
    setPoints(remainingPoints)
  }
}

async function submitBatchQuery() {
  const list = parsePhones(phonesText.value)
  if (!list.length) {
    notify('请输入至少一个有效号码')
    return
  }

  const token = getToken()
  if (!token) {
    clearTokenAndRedirect('请先登录后再批量查询')
    return
  }

  submitting.value = true
  currentTaskId.value = ''
  currentResults.value = []
  progressCurrent.value = 0
  progressTotal.value = list.length
  queryDone.value = false

  try {
    const data = await fetchBatchQuery(list, token, state.config.apiBase)
    applyBatchResponse(data || {}, list)
  } catch (error) {
    const message = String(error?.message || '').trim() || '批量查询失败'
    if (isAuthErrorMessage(message)) {
      clearTokenAndRedirect(message)
      return
    }
    notify(message)
  } finally {
    submitting.value = false
  }
}

function getTagText(item) {
  if (item.error) return item.errorMessage || '查询失败，请稍后重试'
  if (item.markedItems.length) {
    return item.markedItems
      .map((mark) => mark.platform)
      .join(' · ')
  }
  return '未发现平台标记'
}

function getBadgeClass(item) {
  if (item.error) return 'batch-result-badge--fail'
  if (item.markedItems.length) return 'batch-result-badge--marked'
  return 'batch-result-badge--clean'
}

function getBadgeText(item) {
  if (item.error) return '查询失败'
  if (item.markedItems.length) return '有标记'
  return '无标记'
}

function openDetailModal(item) {
  detailItem.value = item || null
  detailVisible.value = true
}

function closeDetailModal() {
  detailVisible.value = false
}

function csvCell(value) {
  const text = String(value || '')
  if (text.indexOf(',') >= 0 || text.indexOf('"') >= 0) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

function downloadCsv() {
  if (!currentResults.value.length) return
  const lines = ['号码,标记状态,平台,标记类型']
  currentResults.value.forEach((item) => {
    if (item.error) {
      lines.push([item.phone, '查询失败', '', item.errorMessage || '查询失败'].map(csvCell).join(','))
      return
    }
    if (!item.markedItems.length) {
      lines.push([item.phone, '无标记', '', ''].map(csvCell).join(','))
      return
    }
    item.markedItems.forEach((mark, index) => {
      lines.push(
        [index === 0 ? item.phone : '', index === 0 ? '有标记' : '', mark.platform, mark.markType].map(csvCell).join(',')
      )
    })
  })
  const blob = new Blob(['\ufeff' + lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  const taskIdForFile = currentTaskId.value ? currentTaskId.value.replace('#', '') : String(Date.now())
  anchor.href = url
  anchor.download = `批量查询结果_${taskIdForFile}.csv`
  anchor.click()
  URL.revokeObjectURL(url)
}

const stopDetailWatch = watch(
  detailVisible,
  (visible) => {
    document.body.classList.toggle('batch-detail-open', !!visible)
  },
  { immediate: true }
)

onMounted(() => {
  loadMobileRuntimeConfig().finally(() => {
    ensureBatchLogin()
  })
})

onBeforeUnmount(() => {
  stopDetailWatch()
  document.body.classList.remove('batch-detail-open')
})
</script>
