<script setup>
import { computed, reactive } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import DetailFieldGrid from '../components/DetailFieldGrid.vue'
import ExecutionSummaryPanel from '../components/ExecutionSummaryPanel.vue'
import SectionHeader from '../components/SectionHeader.vue'
import StatePanel from '../components/StatePanel.vue'
import TimelineList from '../components/TimelineList.vue'
import {
  executeTdxTaskCleanup,
  getTdxTaskDetail,
  prepareTdxTaskSession,
  sendTdxTaskSmsCode,
  updateTdxTaskStatus,
} from '../api/client'
import { useAsyncData } from '../composables/useAsyncData'

const route = useRoute()
const taskId = route.params.taskId || 'tdx-task-001'
const { data: task, loading, error, refresh } = useAsyncData(() => getTdxTaskDetail(taskId), null)
const hasPreparedSession = computed(() => Boolean(task.value?.orderId && task.value?.wxCode))

const actionState = reactive({
  smsCode: '123456',
  rotate: false,
  submitting: false,
  message: '',
  error: '',
})

function resetMessages() {
  actionState.message = ''
  actionState.error = ''
}

async function setStatus(status, currentStep) {
  resetMessages()
  actionState.submitting = true
  try {
    await updateTdxTaskStatus({ id: taskId, status, currentStep })
    actionState.message = `任务状态已更新为 ${status}。`
    await refresh()
  } catch (err) {
    actionState.error = err instanceof Error ? err.message : '更新任务状态失败。'
  } finally {
    actionState.submitting = false
  }
}

async function handleSendCode() {
  resetMessages()
  actionState.submitting = true
  try {
    const response = await sendTdxTaskSmsCode(taskId)
    actionState.message = response.result?.payload?.message || '已尝试发送短信验证码。'
    await refresh()
  } catch (err) {
    actionState.error = err instanceof Error ? err.message : '发送短信验证码失败。'
  } finally {
    actionState.submitting = false
  }
}

async function handleExecuteCleanup() {
  resetMessages()
  if (!hasPreparedSession.value) {
    actionState.error = '执行清理前请先准备好有效会话。'
    return
  }
  actionState.submitting = true
  try {
    await executeTdxTaskCleanup(taskId)
    actionState.message = '清理执行完成。'
    await refresh()
  } catch (err) {
    actionState.error = err instanceof Error ? err.message : '执行清理失败。'
  } finally {
    actionState.submitting = false
  }
}

async function handlePrepareSession() {
  resetMessages()
  if (!actionState.smsCode || actionState.smsCode.trim().length < 4) {
    actionState.error = '准备会话前请输入有效的短信验证码。'
    return
  }
  actionState.submitting = true
  try {
    await prepareTdxTaskSession({ id: taskId, smsCode: actionState.smsCode, rotate: actionState.rotate })
    actionState.message = '任务会话已准备完成。'
    await refresh()
  } catch (err) {
    actionState.error = err instanceof Error ? err.message : '准备任务会话失败。'
  } finally {
    actionState.submitting = false
  }
}
</script>

<template>
  <AppShell
    active="tdx"
    title="tdx二次申诉"
    subtitle="查看当前任务状态并处理短信验证码。"
  >
    <template #actions>
      <button class="btn btn-primary" type="button" @click="setStatus('processing', '上游清理执行中')" :disabled="actionState.submitting">
        标记为执行中
      </button>
      <button class="btn btn-soft" type="button" @click="setStatus('completed', '清理已完成')" :disabled="actionState.submitting">
        标记为已完成
      </button>
      <button class="btn btn-soft" type="button" @click="setStatus('failed', '需要人工跟进')" :disabled="actionState.submitting">
        标记为失败 / 人工
      </button>
    </template>

    <StatePanel v-if="loading" title="正在加载泰迪熊任务..." description="正在准备任务状态、会话详情和运行时说明。" />
    <StatePanel v-else-if="error" variant="error" title="加载泰迪熊任务失败" :description="error" />

    <template v-else>
      <section class="grid-cards">
        <article class="card kpi-card">
          <div class="kpi-label">任务编号</div>
          <div class="kpi-value" style="font-size: 20px;">{{ task.id }}</div>
          <div class="kpi-meta"><span>{{ task.createdAt }}</span><span>{{ task.status }}</span></div>
        </article>
        <article class="card kpi-card">
          <div class="kpi-label">手机号</div>
          <div class="kpi-value">{{ task.phone }}</div>
          <div class="kpi-meta"><span>泰迪熊标记</span><span>{{ task.updatedAt }}</span></div>
        </article>
        <article class="card kpi-card">
          <div class="kpi-label">当前会话</div>
          <div class="kpi-value" style="font-size: 20px;">{{ task.orderId || '(尚未分配)' }}</div>
          <div class="kpi-meta"><span>{{ task.wxCode || '(暂无 wxCode)' }}</span><span>上游</span></div>
        </article>
        <article class="card kpi-card">
          <div class="kpi-label">当前步骤</div>
          <div class="kpi-value">{{ task.currentStep }}</div>
          <div class="kpi-meta"><span>{{ task.status }}</span><span>工作流</span></div>
        </article>
      </section>

      <section class="layout-main">
        <section class="card panel">
          <SectionHeader title="任务概览" subtitle="围绕当前任务状态和会话信息给出结构化摘要。" pill="详情" />
          <DetailFieldGrid
            :fields="[
              { label: '任务编号', value: task.id },
              { label: '手机号', value: task.phone },
              { label: '会话编号', value: task.orderId || '(尚未分配)' },
              { label: 'wxCode', value: task.wxCode || '(尚未分配)' },
              { label: '状态', value: task.status },
              { label: '当前步骤', value: task.currentStep },
            ]"
          />
          <ExecutionSummaryPanel
            v-if="task.executionSummary"
            :summary="task.executionSummary"
            title="执行摘要"
            subtitle="来自当前系统的任务执行进度汇总。"
          />

          <section class="card panel nested-panel">
            <SectionHeader title="任务动作" subtitle="推动单条任务完成发短信、准备会话和执行清理。" />
            <div class="form-grid">
              <div class="field">
                <label>短信验证码</label>
                <input v-model="actionState.smsCode" />
              </div>
              <div class="field">
                <label>准备前是否轮换会话</label>
                <input :value="actionState.rotate ? '是' : '否'" @click="actionState.rotate = !actionState.rotate" readonly />
              </div>
              <div class="field field-full">
                <div class="detail-action-row">
                  <button class="btn btn-soft" type="button" @click="handleSendCode" :disabled="actionState.submitting">发送短信验证码</button>
                  <button class="btn btn-primary" type="button" @click="handlePrepareSession" :disabled="actionState.submitting">准备会话</button>
                  <button class="btn btn-soft" type="button" @click="handleExecuteCleanup" :disabled="actionState.submitting || !hasPreparedSession">执行清理</button>
                </div>
              </div>
            </div>
            <StatePanel v-if="actionState.message" title="最近一次操作结果" :description="actionState.message" />
            <StatePanel v-if="actionState.error" variant="error" title="操作失败" :description="actionState.error" />
          </section>
        </section>

        <section class="card panel">
          <SectionHeader title="任务时间线" subtitle="用于驱动人工跟进和快速状态核查。" />
          <TimelineList :items="task.notes || []" />
        </section>
      </section>

    </template>
  </AppShell>
</template>
