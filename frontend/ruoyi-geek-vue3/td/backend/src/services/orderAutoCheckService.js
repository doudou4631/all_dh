import { query } from '../data/liveDb.js'
import { normalizeFailReasonDesc } from '../lib/orderFailReason.js'
import { formatChinaDateTime } from '../lib/time.js'
import { reconcileTdxSmsReceiveBillingByOrderId } from './tdxBillingService.js'
import { queryTdxCleanupProgress, executeTdxCleanupTask } from './tdxCleanupService.js'
import { getSavedTdxLoginToken } from './tdxRuntimeStore.js'

const TDX_SMS_RECEIVE_COMPANY_CODE = 'duanxingettaidixiong'
const TDX_SMS_RECEIVE_COMPANY_NAME = 'tdx二次申诉'
const TDX_REFUNDED_PENDING_MESSAGE = '该号码显示正在审核，已退款'
const DEFAULT_INTERVAL_MS = 30 * 60 * 1000
const DEFAULT_INITIAL_DELAY_MS = 60 * 1000
const DEFAULT_BATCH_SIZE = 20
const DEFAULT_LOOKBACK_DAYS = 30

const lastCheckedAtByOrderId = new Map()
let running = false

function parsePositiveInt(value, fallback, max = 1000000) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) return fallback
  return Math.min(Math.floor(parsed), max)
}

function isAutoCheckEnabled() {
  return String(process.env.ORDER_AUTOCHECK_ENABLED || '1') !== '0'
}

function resolveIntervalMs() {
  return parsePositiveInt(process.env.ORDER_AUTOCHECK_INTERVAL_MS, DEFAULT_INTERVAL_MS, 24 * 60 * 60 * 1000)
}

function resolveInitialDelayMs() {
  return parsePositiveInt(process.env.ORDER_AUTOCHECK_INITIAL_DELAY_MS, DEFAULT_INITIAL_DELAY_MS, 60 * 60 * 1000)
}

function resolveBatchSize() {
  return parsePositiveInt(process.env.ORDER_AUTOCHECK_BATCH_SIZE, DEFAULT_BATCH_SIZE, 200)
}

function resolveLookbackDays() {
  return parsePositiveInt(process.env.ORDER_AUTOCHECK_LOOKBACK_DAYS, DEFAULT_LOOKBACK_DAYS, 365)
}

function shouldCheckInMemory(orderId, minIntervalMs) {
  const id = Number(orderId || 0)
  if (!id) return false
  const now = Date.now()
  const last = lastCheckedAtByOrderId.get(id) || 0
  if (now - last < minIntervalMs) return false
  lastCheckedAtByOrderId.set(id, now)
  return true
}

function normalizeText(value = '') {
  return String(value || '').trim()
}

function isTdxOrder(order = {}) {
  const companyCode = normalizeText(order.companycode)
  const companyName = normalizeText(order.companyname).toLowerCase()
  return companyCode === TDX_SMS_RECEIVE_COMPANY_CODE || companyName.includes(TDX_SMS_RECEIVE_COMPANY_NAME)
}

async function listPendingAutoCheckOrders(limit, lookbackDays) {
  const safeLimit = parsePositiveInt(limit, DEFAULT_BATCH_SIZE, 200)
  const safeLookbackDays = parsePositiveInt(lookbackDays, DEFAULT_LOOKBACK_DAYS, 365)
  return query(
    `SELECT Id, phone, companycode, companyname, submitres, orderstate, createtime, successtime, failtime, UpdateOn, dealquxiaores
       FROM t_orderdeal
      WHERE orderstate = 33
        AND createtime >= DATE_SUB(NOW(), INTERVAL ? DAY)
        AND (
          companycode = ?
          OR companyname LIKE '%tdx二次申诉%'
        )
      ORDER BY COALESCE(UpdateOn, createtime) ASC, Id ASC
      LIMIT ${safeLimit}`,
    [safeLookbackDays, TDX_SMS_RECEIVE_COMPANY_CODE],
  )
}

async function markOrderCompleted(order, message = '') {
  const updatedAt = formatChinaDateTime()
  const detail = message || '订单状态已自动更新为处理完成。'
  await query(
    `UPDATE t_orderdeal
        SET queryres = ?,
            dealquxiaores = ?,
            orderstate = 6,
            successtime = COALESCE(successtime, ?),
            failtime = NULL,
            failreason = NULL,
            failreasondesc = NULL,
            UpdateOn = ?
      WHERE Id = ?
        AND orderstate = 33`,
    ['AutoCheckCompleted', detail, updatedAt, updatedAt, Number(order.Id)],
  )
}

async function touchOrderAutoCheck(order, message = '') {
  const updatedAt = formatChinaDateTime()
  const detail = normalizeText(message)
  if (!detail) {
    await query('UPDATE t_orderdeal SET UpdateOn = ? WHERE Id = ? AND orderstate = 33', [updatedAt, Number(order.Id)])
    return
  }
  await query(
    `UPDATE t_orderdeal
        SET dealquxiaores = ?,
            UpdateOn = ?
      WHERE Id = ?
        AND orderstate = 33`,
    [detail, updatedAt, Number(order.Id)],
  )
}

async function markOrderRefundedPendingReview(order, message = '', failureReason = '') {
  const updatedAt = formatChinaDateTime()
  const detail = normalizeFailReasonDesc(failureReason || message || '上游返回已处理状态，但未返回直接成功标记。')
  await query(
    `UPDATE t_orderdeal
        SET queryres = ?,
            dealquxiaores = ?,
            orderstate = 777,
            successtime = NULL,
            failtime = ?,
            failreason = ?,
            failreasondesc = ?,
            UpdateOn = ?
      WHERE Id = ?
        AND orderstate = 33`,
    ['AutoCheckRefunded', TDX_REFUNDED_PENDING_MESSAGE, updatedAt, TDX_REFUNDED_PENDING_MESSAGE, detail, updatedAt, Number(order.Id)],
  )
}

export async function checkTdxOrder(order) {
  const orderId = Number(order.Id || 0)
  const savedLogin = await getSavedTdxLoginToken(order.phone)
  const savedToken = savedLogin?.token || ''

  // A failed progress query is inconclusive. Do not resubmit, reconcile billing,
  // or mutate the order until the upstream query has returned a usable result.
  const result = await queryTdxCleanupProgress({
    phone: order.phone,
    orderId: savedLogin?.orderId || order.submitres,
    loginToken: savedToken,
  })
  if (!result || result.ok === false) {
    return {
      id: orderId,
      type: 'tdx',
      status: 'error',
      ok: false,
      skipped: true,
      message: normalizeText(result?.message || result?.result?.message || '查询失败，已跳过自动巡检'),
      billing: null,
    }
  }

  let billing = null
  if (orderId) {
    billing = await reconcileTdxSmsReceiveBillingByOrderId(orderId, { reason: 'auto-check-pending' })
  }

  // Parse existing retry count from dealquxiaores
  let retryCount = 0
  const match = String(order.dealquxiaores || '').match(/【已自动重申\s*(\d+)\s*次】/)
  if (match) {
    retryCount = parseInt(match[1], 10)
  }

  let isResubmitted = false
  if (savedToken) {
    try {
      const sessionOrderId = String(savedLogin?.orderId || order.submitres || '').trim()
      const sessionWxCode = String(savedLogin?.wxCode || '').trim()
      const pseudoTask = {
        id: `orderdeal-${order.Id}`,
        phone: order.phone,
        line: order.line || order.tdxLine || 'line1',
        orderId: sessionOrderId,
        wxCode: sessionWxCode,
        loginToken: savedToken,
        skipCheckAppletPhone: true,
        result: { login: { token: savedToken } },
      }
      console.log(`[order-auto-check] Auto-resubmitting appeal for order ${orderId} using token...`)
      await executeTdxCleanupTask(pseudoTask)
      isResubmitted = true
    } catch (err) {
      console.error(`[order-auto-check] Auto-resubmit failed for order ${orderId}:`, err.message || err)
    }
  }

  if (isResubmitted) {
    retryCount += 1
  }

  let finalStatus = normalizeText(result?.result?.status || (result?.ok === false ? 'failed' : 'processing'))
  let message = normalizeText(result?.result?.message || '')
  const refundSuggested = result?.result?.refundSuggested === true
  const failureReason = normalizeText(result?.result?.failureReason || '')
  const opinion = result?.result?.progress?.opinion || ''

  const isTargetStatus = (finalStatus === 'processing' && 
                          (message === '待处理' || opinion.includes('请您耐心等待，工作人员会核实号码情况')))

  if (finalStatus === 'completed') {
    await markOrderCompleted(order, message || '号码标记已取消，部分手机端可能需要3-5个工作日的同步时间，请耐心等待。')
    if (orderId) {
      billing = await reconcileTdxSmsReceiveBillingByOrderId(orderId, { reason: 'auto-check-completed' })
    }
  } else if (refundSuggested) {
    await markOrderRefundedPendingReview(order, message, failureReason)
    if (orderId) {
      billing = await reconcileTdxSmsReceiveBillingByOrderId(orderId, { reason: 'auto-check-refunded' })
    }
  } else if (isTargetStatus && retryCount >= 3) {
    // Failed and refund if retried 3 times or more and still in this status
    const refundMsg = `自动重申${retryCount}次后仍为待处理状态，已自动判定为失败并退款。`
    console.log(`[order-auto-check] Order ${orderId} reached 3 retries in processing state. Refunding...`)
    await markOrderRefundedPendingReview(order, refundMsg, '自动重申3次仍为待处理，已自动退款。')
    if (orderId) {
      billing = await reconcileTdxSmsReceiveBillingByOrderId(orderId, { reason: 'auto-check-refunded' })
    }
    finalStatus = 'failed'
  } else {
    let statusText = message || '处理中'
    if (retryCount > 0) {
      statusText = `${statusText}【已自动重申${retryCount}次】`
    }
    await touchOrderAutoCheck(order, statusText)
  }
  return {
    id: orderId,
    type: 'tdx',
    status: finalStatus || 'processing',
    ok: result?.ok !== false,
    refundSuggested,
    billing,
  }
}

export async function runOrderAutoCheckOnce({
  batchSize = resolveBatchSize(),
  lookbackDays = resolveLookbackDays(),
  minIntervalMs = resolveIntervalMs(),
} = {}) {
  if (running) return { skipped: true, reason: 'already-running', checked: 0, completed: 0, results: [] }
  running = true
  try {
    const rows = await listPendingAutoCheckOrders(batchSize, lookbackDays)
    const results = []
    for (const order of rows) {
      const id = Number(order.Id || 0)
      if (!shouldCheckInMemory(id, minIntervalMs)) continue
      if (!isTdxOrder(order)) continue
      try {
        results.push(await checkTdxOrder(order))
      } catch (error) {
        results.push({
          id,
          type: 'tdx',
          ok: false,
          status: 'error',
          message: error instanceof Error ? error.message : String(error),
        })
      }
    }
    return {
      skipped: false,
      checked: results.length,
      completed: results.filter((item) => item.status === 'completed').length,
      results,
    }
  } finally {
    running = false
  }
}

export function startOrderAutoCheckService() {
  if (!isAutoCheckEnabled()) return () => {}

  const intervalMs = resolveIntervalMs()
  const initialDelayMs = resolveInitialDelayMs()
  let stopped = false
  let startupTimer = null
  let intervalTimer = null

  const tick = async () => {
    if (stopped) return
    try {
      const summary = await runOrderAutoCheckOnce({ minIntervalMs: intervalMs })
      if (!summary.skipped && summary.checked > 0) {
        console.log(`[order-auto-check] checked=${summary.checked} completed=${summary.completed}`)
      }
    } catch (error) {
      console.error('[order-auto-check] failed:', error instanceof Error ? error.message : error)
    }
  }

  startupTimer = setTimeout(() => {
    tick()
    intervalTimer = setInterval(tick, intervalMs)
  }, initialDelayMs)

  return () => {
    stopped = true
    if (startupTimer) clearTimeout(startupTimer)
    if (intervalTimer) clearInterval(intervalTimer)
  }
}
