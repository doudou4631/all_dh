<template>
  <div class="app-container statistics-page" v-loading="loading">
    <div class="page-toolbar">
      <div>
        <h2>查询数据统计</h2>
        <span class="updated-at">更新于 {{ generatedAt || '-' }}</span>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadStatistics">刷新</el-button>
    </div>

    <section class="metrics-grid">
      <article v-for="metric in metrics" :key="metric.key" class="metric-tile" :class="`metric-tile--${metric.tone}`">
        <div class="metric-icon"><el-icon><component :is="metric.icon" /></el-icon></div>
        <div class="metric-content">
          <span class="metric-label">{{ metric.label }}</span>
          <strong class="metric-value">{{ metric.value }}</strong>
          <span class="metric-note">{{ metric.note }}</span>
        </div>
      </article>
    </section>

    <el-row :gutter="16" class="detail-row">
      <el-col :xs="24" :lg="17">
        <el-card shadow="never" class="trend-panel">
          <template #header>
            <div class="panel-header">
              <span>最近 7 天趋势</span>
              <div class="legend">
                <span><i class="legend-query"></i>查询量</span>
                <span><i class="legend-visitor"></i>UV</span>
              </div>
            </div>
          </template>
          <div ref="trendChartRef" class="trend-chart"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="7">
        <el-card shadow="never" class="visitor-panel">
          <template #header><span>今日访客构成</span></template>
          <div class="visitor-total">
            <span>今日 UV</span>
            <strong>{{ formatInteger(summary.todayVisitors) }}</strong>
          </div>
          <div class="visitor-row">
            <span><i class="visitor-dot visitor-dot--new"></i>新访客</span>
            <strong>{{ formatInteger(summary.newVisitorsToday) }}</strong>
          </div>
          <div class="visitor-row">
            <span><i class="visitor-dot visitor-dot--returning"></i>回访用户</span>
            <strong>{{ formatInteger(summary.returningVisitorsToday) }}</strong>
          </div>
          <div class="visitor-bar" aria-hidden="true">
            <span class="visitor-bar__new" :style="{ width: `${newVisitorPercent}%` }"></span>
          </div>
          <div class="visitor-percent">
            <span>新增占比</span>
            <strong>{{ newVisitorPercent.toFixed(1) }}%</strong>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="FreeQueryStatistics">
import * as echarts from 'echarts'
import {
  Calendar,
  CircleCheck,
  DataAnalysis,
  Histogram,
  Refresh,
  TrendCharts,
  User,
  Warning
} from '@element-plus/icons-vue'
import { getFreeQueryStatistics } from '@/api/server/freeQueryStatistics'

const loading = ref(false)
const generatedAt = ref('')
const trendChartRef = ref(null)
const summary = reactive({
  totalQueryCount: 0,
  todayQueryCount: 0,
  weekQueryCount: 0,
  uniqueVisitors: 0,
  todayVisitors: 0,
  newVisitorsToday: 0,
  returningVisitorsToday: 0,
  avgQueriesPerUser: 0,
  successRate: 0,
  noResultRate: 0
})
const trend = ref([])
let chartInstance

const metrics = computed(() => [
  { key: 'total', label: '总查询量', value: formatInteger(summary.totalQueryCount), note: '历史累计', icon: DataAnalysis, tone: 'blue' },
  { key: 'today', label: '今日查询量', value: formatInteger(summary.todayQueryCount), note: '当日实时', icon: Calendar, tone: 'cyan' },
  { key: 'week', label: '本周查询量', value: formatInteger(summary.weekQueryCount), note: '周一至今', icon: TrendCharts, tone: 'indigo' },
  { key: 'uv', label: '独立用户数', value: formatInteger(summary.uniqueVisitors), note: '累计 UV', icon: User, tone: 'green' },
  { key: 'average', label: '人均查询次数', value: formatDecimal(summary.avgQueriesPerUser), note: '总量 / UV', icon: Histogram, tone: 'gray' },
  { key: 'success', label: '查询成功率', value: formatRate(summary.successRate), note: '成功返回 / 总查询', icon: CircleCheck, tone: 'success' },
  { key: 'empty', label: '无结果率', value: formatRate(summary.noResultRate), note: '无命中 / 总查询', icon: Warning, tone: 'warning' }
])

const newVisitorPercent = computed(() => {
  const total = Number(summary.todayVisitors) || 0
  return total ? Math.min(100, (Number(summary.newVisitorsToday) || 0) * 100 / total) : 0
})

async function loadStatistics() {
  loading.value = true
  try {
    const response = await getFreeQueryStatistics()
    Object.assign(summary, response.data?.summary || {})
    trend.value = response.data?.trend || []
    generatedAt.value = response.data?.generatedAt || ''
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!trendChartRef.value) return
  chartInstance ||= echarts.init(trendChartRef.value)
  const days = buildSevenDays()
  const trendMap = new Map(trend.value.map(item => [item.statDate, item]))
  chartInstance.setOption({
    animationDuration: 500,
    grid: { left: 44, right: 22, top: 30, bottom: 34 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      boundaryGap: true,
      data: days.map(item => item.label),
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#ebeef5', type: 'dashed' } }
    },
    series: [
      {
        name: '查询量',
        type: 'bar',
        barMaxWidth: 30,
        itemStyle: { color: '#3b82e8', borderRadius: [3, 3, 0, 0] },
        data: days.map(item => Number(trendMap.get(item.key)?.queryCount || 0))
      },
      {
        name: 'UV',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        lineStyle: { width: 2, color: '#16a085' },
        itemStyle: { color: '#16a085' },
        data: days.map(item => Number(trendMap.get(item.key)?.visitorCount || 0))
      }
    ]
  }, true)
}

function buildSevenDays() {
  const result = []
  for (let offset = 6; offset >= 0; offset--) {
    const date = new Date()
    date.setHours(0, 0, 0, 0)
    date.setDate(date.getDate() - offset)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    result.push({ key: `${year}-${month}-${day}`, label: `${month}-${day}` })
  }
  return result
}

function formatInteger(value) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

function formatDecimal(value) {
  return Number(value || 0).toFixed(2)
}

function formatRate(value) {
  return `${Number(value || 0).toFixed(2)}%`
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  loadStatistics()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.statistics-page {
  color: var(--el-text-color-primary);
}

.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-toolbar h2 {
  margin: 0 0 4px;
  font-size: 20px;
  line-height: 28px;
  letter-spacing: 0;
}

.updated-at {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 12px;
}

.metric-tile {
  display: flex;
  align-items: center;
  min-height: 112px;
  padding: 18px;
  box-sizing: border-box;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
}

.metric-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  margin-right: 14px;
  border-radius: 6px;
  font-size: 22px;
  background: #edf4ff;
  color: #3b82e8;
}

.metric-content {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.metric-label,
.metric-note {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.metric-value {
  margin: 5px 0 3px;
  font-size: 25px;
  line-height: 30px;
  letter-spacing: 0;
  white-space: nowrap;
}

.metric-tile--cyan .metric-icon { color: #0891b2; background: #ecfeff; }
.metric-tile--indigo .metric-icon { color: #6366f1; background: #eef2ff; }
.metric-tile--green .metric-icon { color: #0f9f6e; background: #ecfdf5; }
.metric-tile--gray .metric-icon { color: #64748b; background: #f1f5f9; }
.metric-tile--success .metric-icon { color: #16a34a; background: #f0fdf4; }
.metric-tile--warning .metric-icon { color: #d97706; background: #fffbeb; }

.detail-row {
  margin-top: 16px;
}

.trend-panel,
.visitor-panel {
  border-radius: 6px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.legend {
  display: flex;
  gap: 16px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.legend i,
.visitor-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-query { background: #3b82e8; }
.legend-visitor { background: #16a085; }

.trend-chart {
  width: 100%;
  height: 310px;
}

.visitor-panel {
  min-height: 368px;
}

.visitor-total {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 10px 0 22px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.visitor-total span,
.visitor-row span,
.visitor-percent span {
  color: var(--el-text-color-secondary);
}

.visitor-total strong {
  font-size: 30px;
  letter-spacing: 0;
}

.visitor-row,
.visitor-percent {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 22px;
}

.visitor-row span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.visitor-dot--new { background: #3b82e8; }
.visitor-dot--returning { background: #16a085; }

.visitor-bar {
  width: 100%;
  height: 8px;
  margin-top: 24px;
  overflow: hidden;
  border-radius: 4px;
  background: #16a085;
}

.visitor-bar__new {
  display: block;
  height: 100%;
  background: #3b82e8;
}

.visitor-percent {
  padding-top: 12px;
  font-size: 13px;
}

@media (max-width: 1199px) {
  .visitor-panel { margin-top: 16px; min-height: 0; }
}

@media (max-width: 640px) {
  .page-toolbar { align-items: flex-start; }
  .metrics-grid { grid-template-columns: 1fr; }
  .trend-chart { height: 260px; }
}
</style>
