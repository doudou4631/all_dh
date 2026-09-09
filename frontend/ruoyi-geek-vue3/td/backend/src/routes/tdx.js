import { ok } from '../lib/response.js'
import { requireModuleAccess } from '../lib/authGuard.js'
import { processTencent3hCleanup, processTencent23dCleanup } from '../services/tencentCleanupService.js'
import {
  allocateTdxOrderSession,
  createTdxSmsReceiveSession,
  getTdxRuntimeSnapshot,
  isHardTdxSessionFailureMessage,
  isTdxSessionCooldownMessage,
  persistTdxSmsReceiveLoginToken,
  precheckTdxPhone,
  rotateTdxOrderSession,
  sendTdxSmsCode,
  validateLiveTdxConfig,
  runTdxHealthCheck,
  executeTdxCleanupTask,
} from '../services/tdxCleanupService.js'
import {
  TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
  getTdxSmsReceiveBalanceGuard,
  isTdxSmsReceiveInsufficientBalanceResult,
  reconcileTdxSmsReceiveBillingByOrderId,
} from '../services/tdxBillingService.js'
import { buildTdxExecutionPlan, summarizeTdxExecutionPlan } from '../services/tdxExecutionPlan.js'
import { buildTdxExecutorProfile } from '../services/tdxExecutorProfile.js'
import { createTdxTask, findTdxTask, listTdxTasks, updateTdxTask } from '../data/tdxTaskStore.js'
import { readTdxSettings, summarizeTdxSettings, writeTdxSettings } from '../data/tdxSettingsStore.js'
import { isSystemSwitchEnabled } from '../data/systemSettingsStore.js'
import { getTdxConfig } from '../config/tdxConfig.js'
import { normalizeFailReasonDesc } from '../lib/orderFailReason.js'
import { formatChinaDateTime } from '../lib/time.js'
import { query } from '../data/liveDb.js'

function nowText() {
  return formatChinaDateTime()
}

const TDX_SMS_RECEIVE_COMPANY_CODE = 'duanxingettaidixiong'
const TDX_SMS_RECEIVE_COMPANY_NAME = 'tdx\u4e8c\u6b21\u7533\u8bc9'
const TDX_SMS_RECEIVE_SAVED_RESULT = '\u4f1a\u8bdd\u5df2\u4fdd\u5b58\uff0c\u7b49\u5f85\u7ee7\u7eed\u7533\u8bc9\u5904\u7406\u3002'
const TDX_REPEAT_APPEAL_WINDOW_DAYS = 30
const TDX_REPEAT_APPEAL_MESSAGE = `该号码${TDX_REPEAT_APPEAL_WINDOW_DAYS}天内已申诉过，暂不可重复提交`
const TDX_SMS_RECEIVE_SESSION_RETRY_TASK_TYPE = 'sms-receive-session-retry'
const TDX_SMS_RECEIVE_PENDING_MESSAGE = '系统正在后台处理，请稍后到订单详情查看结果。'
const TDX_DIRECT_SUCCESS_MESSAGE = '泰迪未来标记已取消'
const TDX_REFUNDED_PENDING_MESSAGE = '该号码显示正在审核，已退款'
const TDX_REFUNDED_RECEIPT_STATUS = '审核中'
const TDX_REFUNDED_RECEIPT_RESULT_MESSAGE = '标记未消除，本次退款'
const TDX_SMS_RECEIVE_PENDING_STEP = '上游会话异常，系统正在后台自动重试...'
const TDX_SESSION_RETRY_DEFAULT_INTERVAL_MS = 15000
const TDX_SESSION_RETRY_COOLDOWN_DEFAULT_INTERVAL_MS = 75000
const TDX_SESSION_RETRY_DEFAULT_INITIAL_DELAY_MS = 3000
const TDX_SESSION_RETRY_DEFAULT_MAX_ATTEMPTS = 6

let tdxSessionRetryTimer = null
let tdxSessionRetryRunning = false
let tdxSessionRetryStarted = false
let tdxSessionRetryStopped = false
let tdxSessionRetryNextRunAtMs = 0

function batchTimestampFromText(text = nowText()) {
  const compact = String(text || nowText()).replace(/\D/g, '')
  return `${compact.slice(2, 8)}_${compact.slice(8, 14)}000`
}

function buildTdxSmsReceiveBatchNo(createdAt = nowText()) {
  const compact = String(createdAt || nowText()).replace(/\D/g, '')
  const datePart = compact.slice(2, 8) || '000000'
  const timePart = compact.slice(8, 14) || '000000'
  const serialPart = String(Math.floor(1000 + Math.random() * 9000))
  return `TDX-${datePart}-${timePart}-${serialPart}`
}

async function resolveTdxSmsReceiveOrderPrice(userId = 0) {
  if (!Number(userId)) return 0
  const rows = await query('SELECT dealsmsgettdx FROM t_user WHERE Id = ? LIMIT 1', [Number(userId)])
  return Math.abs(Number(rows[0]?.dealsmsgettdx || 0))
}

async function ensureTdxSmsReceiveRequestBalance(request, {
  phone = '',
  orderDealId = 0,
  orderPrice = null,
  createdAt = null,
} = {}) {
  const userId = Number(request.currentUser?.liveUserId || 0)
  return getTdxSmsReceiveBalanceGuard({
    userId,
    phone,
    orderDealId,
    orderPrice,
    createdAt,
  })
}

function buildTdxInsufficientBalancePayload(balanceGuard = {}) {
  return {
    reason: String(balanceGuard.reason || 'insufficient-balance').trim() || 'insufficient-balance',
    balance: Number(balanceGuard.balance || 0),
    requiredAmount: Number(balanceGuard.requiredAmount || 0),
    deficit: Number(balanceGuard.deficit || 0),
    orderDealId: Number(balanceGuard.orderDealId || 0),
    orderNumber: String(balanceGuard.orderNumber || '').trim(),
  }
}

async function markTdxOrderDealInsufficientBalance(orderDealId = 0, balanceGuard = {}) {
  const id = Number(orderDealId || 0)
  if (!id) return false
  const updatedAt = nowText()
  const detail = `insufficient-balance: balance=${Number(balanceGuard.balance || 0)}, required=${Number(balanceGuard.requiredAmount || 0)}, deficit=${Number(balanceGuard.deficit || 0)}`
  await updateTdxSmsReceiveOrderDealById(id, {
    queryres: 'AppealFailed',
    dealquxiaores: TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
    orderstate: 777,
    successtime: null,
    failtime: updatedAt,
    failreason: TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
    failreasondesc: normalizeFailReasonDesc(detail),
    UpdateOn: updatedAt,
  })
  return true
}

async function findRecentLocalSuccessfulTdxAppeal(phone, windowDays = TDX_REPEAT_APPEAL_WINDOW_DAYS) {
  const normalizedPhone = String(phone || '').trim()
  if (!/^1\d{10}$/.test(normalizedPhone)) return null
  const safeWindowDays = Math.max(1, Math.min(365, Number(windowDays || TDX_REPEAT_APPEAL_WINDOW_DAYS)))
  const currentTime = nowText()
  const rows = await query(
    `SELECT Id, userid, username, orderpicinumber, createtime, successtime, queryres, orderstate
       FROM t_orderdeal
      WHERE phone = ?
        AND companycode = ?
        AND COALESCE(successtime, createtime, CreateOn, UpdateOn) >= DATE_SUB(?, INTERVAL ${safeWindowDays} DAY)
        AND COALESCE(successtime, createtime, CreateOn, UpdateOn) < DATE(?)
        AND (queryres = 'AppealCompleted' OR orderstate IN (6, 55, 666))
      ORDER BY COALESCE(successtime, createtime, CreateOn, UpdateOn) DESC, Id DESC
      LIMIT 1`,
    [normalizedPhone, TDX_SMS_RECEIVE_COMPANY_CODE, currentTime, currentTime],
  )

  const recent = rows[0]
  if (!recent) return null
  return {
    id: Number(recent.Id || 0),
    userid: Number(recent.userid || 0),
    username: String(recent.username || '').trim(),
    orderpicinumber: String(recent.orderpicinumber || '').trim(),
    createtime: recent.createtime || null,
    successtime: recent.successtime || null,
    queryres: String(recent.queryres || '').trim(),
    orderstate: Number(recent.orderstate || 0),
  }
}

async function saveTdxSmsReceiveOrderDeal({ request, phone, smsCode, sessionResult }) {
  const userId = Number(request.currentUser?.liveUserId || 0)
  const username = String(request.currentUser?.username || request.currentUser?.name || '').trim()
  if (!userId) {
    throw new Error('当前账号缺少旧版用户 ID，无法写入订单详情。')
  }

  const normalizedPhone = String(phone || '').trim()
  const createdAt = nowText()
  const orderPrice = await resolveTdxSmsReceiveOrderPrice(userId)
  const safeNote = String(smsCode || '').trim().slice(0, 32)
  const submitRes = String(sessionResult?.orderId || '').trim().slice(0, 500)

  const existing = await query(
    `SELECT Id, orderpicinumber
       FROM t_orderdeal
      WHERE userid = ?
        AND phone = ?
        AND companycode = ?
        AND createtime >= DATE(?)
        AND createtime < DATE_ADD(DATE(?), INTERVAL 1 DAY)
      ORDER BY Id DESC
      LIMIT 1`,
    [userId, normalizedPhone, TDX_SMS_RECEIVE_COMPANY_CODE, createdAt, createdAt],
  )

  if (existing[0]?.Id) {
    await query(
      `UPDATE t_orderdeal
          SET username = ?,
              companyname = ?,
              submitres = ?,
              queryres = ?,
              dealquxiaores = ?,
              orderstate = ?,
              orderstatenote = ?,
              orderprice = ?,
              successtime = NULL,
              failtime = NULL,
              failreason = NULL,
              failreasondesc = NULL,
              UpdateOn = ?
        WHERE Id = ?`,
      [
        username,
        TDX_SMS_RECEIVE_COMPANY_NAME,
        submitRes,
        'TokenSaved',
        TDX_SMS_RECEIVE_SAVED_RESULT,
        333,
        safeNote,
        orderPrice,
        createdAt,
        Number(existing[0].Id),
      ],
    )
    const saved = {
      id: Number(existing[0].Id),
      orderpicinumber: existing[0].orderpicinumber || '',
      phone: normalizedPhone,
      submittedAt: createdAt,
      created: false,
    }
    const billing = await reconcileTdxSmsReceiveBillingByOrderId(saved.id, { reason: 'order-reused' })
    return { ...saved, billing }
  }

  const batchNo = buildTdxSmsReceiveBatchNo(createdAt)
  const result = await query(
    `INSERT INTO t_orderdeal (
       userid, username, phone, companycode, companyname,
       submitres, queryres, dealquxiaores, orderstate, orderstatenote,
       orderpicinumber, orderprice, createtime, successtime, failtime,
       failreason, failreasondesc, CreateOn, UpdateOn, CreateBy, UpdateBy
     )
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, NULL, ?, ?, ?, ?)`,
    [
      userId,
      username,
      normalizedPhone,
      TDX_SMS_RECEIVE_COMPANY_CODE,
      TDX_SMS_RECEIVE_COMPANY_NAME,
      submitRes,
      'TokenSaved',
      TDX_SMS_RECEIVE_SAVED_RESULT,
      333,
      safeNote,
      batchNo,
      orderPrice,
      createdAt,
      createdAt,
      createdAt,
      userId,
      userId,
    ],
  )

  const saved = {
    id: Number(result.insertId || 0),
    orderpicinumber: batchNo,
    phone: normalizedPhone,
    submittedAt: createdAt,
    created: true,
  }
  const billing = await reconcileTdxSmsReceiveBillingByOrderId(saved.id, { reason: 'order-created' })
  return { ...saved, billing }
}

async function updateTdxSmsReceiveOrderDealById(id, patch = {}) {
  const orderDealId = Number(id || 0)
  if (!orderDealId) return false
  const fields = []
  const params = []
  for (const key of ['submitres', 'queryres', 'dealquxiaores', 'orderstate', 'orderstatenote', 'successtime', 'failtime', 'failreason', 'failreasondesc', 'UpdateOn']) {
    if (Object.prototype.hasOwnProperty.call(patch, key)) {
      fields.push(`${key} = ?`)
      params.push(key === 'failreasondesc' ? normalizeFailReasonDesc(patch[key]) : patch[key])
    }
  }
  if (!fields.length) return false
  params.push(orderDealId)
  await query(`UPDATE t_orderdeal SET ${fields.join(', ')} WHERE Id = ?`, params)
  return true
}

async function saveTdxSmsReceiveRetryingOrderDeal({ request, phone, smsCode, retryMessage = TDX_SMS_RECEIVE_PENDING_MESSAGE }) {
  const userId = Number(request.currentUser?.liveUserId || 0)
  const username = String(request.currentUser?.username || request.currentUser?.name || '').trim()
  if (!userId) {
    throw new Error('当前账号缺少旧版用户 ID，无法写入订单详情。')
  }

  const normalizedPhone = String(phone || '').trim()
  const createdAt = nowText()
  const orderPrice = await resolveTdxSmsReceiveOrderPrice(userId)
  const safeNote = String(smsCode || '').trim().slice(0, 32)
  const publicMessage = String(retryMessage || TDX_SMS_RECEIVE_PENDING_MESSAGE).trim() || TDX_SMS_RECEIVE_PENDING_MESSAGE

  const existing = await query(
    `SELECT Id, orderpicinumber
       FROM t_orderdeal
      WHERE userid = ?
        AND phone = ?
        AND companycode = ?
        AND createtime >= DATE(?)
        AND createtime < DATE_ADD(DATE(?), INTERVAL 1 DAY)
      ORDER BY Id DESC
      LIMIT 1`,
    [userId, normalizedPhone, TDX_SMS_RECEIVE_COMPANY_CODE, createdAt, createdAt],
  )

  if (existing[0]?.Id) {
    await query(
      `UPDATE t_orderdeal
          SET username = ?,
              companyname = ?,
              submitres = ?,
              queryres = ?,
              dealquxiaores = ?,
              orderstate = ?,
              orderstatenote = ?,
              orderprice = ?,
              successtime = NULL,
              failtime = NULL,
              failreason = NULL,
              failreasondesc = NULL,
              UpdateOn = ?
        WHERE Id = ?`,
      [
        username,
        TDX_SMS_RECEIVE_COMPANY_NAME,
        '',
        'SessionRetrying',
        publicMessage,
        333,
        safeNote,
        orderPrice,
        createdAt,
        Number(existing[0].Id),
      ],
    )
    const saved = {
      id: Number(existing[0].Id),
      orderpicinumber: existing[0].orderpicinumber || '',
      phone: normalizedPhone,
      submittedAt: createdAt,
      created: false,
    }
    const billing = await reconcileTdxSmsReceiveBillingByOrderId(saved.id, { reason: 'session-retry-reused' })
    return { ...saved, billing }
  }

  const batchNo = buildTdxSmsReceiveBatchNo(createdAt)
  const result = await query(
    `INSERT INTO t_orderdeal (
       userid, username, phone, companycode, companyname,
       submitres, queryres, dealquxiaores, orderstate, orderstatenote,
       orderpicinumber, orderprice, createtime, successtime, failtime,
       failreason, failreasondesc, CreateOn, UpdateOn, CreateBy, UpdateBy
     )
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, NULL, ?, ?, ?, ?)`,
    [
      userId,
      username,
      normalizedPhone,
      TDX_SMS_RECEIVE_COMPANY_CODE,
      TDX_SMS_RECEIVE_COMPANY_NAME,
      '',
      'SessionRetrying',
      publicMessage,
      333,
      safeNote,
      batchNo,
      orderPrice,
      createdAt,
      createdAt,
      createdAt,
      userId,
      userId,
    ],
  )

  const saved = {
    id: Number(result.insertId || 0),
    orderpicinumber: batchNo,
    phone: normalizedPhone,
    submittedAt: createdAt,
    created: true,
  }
  const billing = await reconcileTdxSmsReceiveBillingByOrderId(saved.id, { reason: 'session-retry-created' })
  return { ...saved, billing }
}

function parsePositiveInt(value, fallback, { min = 1, max = Number.MAX_SAFE_INTEGER } = {}) {
  const num = Number.parseInt(String(value ?? ''), 10)
  if (!Number.isFinite(num)) return fallback
  return Math.min(max, Math.max(min, num))
}

function formatTdxRetryAt(ms = 0) {
  const safeMs = Number(ms || 0)
  if (!safeMs || safeMs < 0) return ''
  return formatChinaDateTime(new Date(safeMs))
}

function isTdxSessionRetryableDetail(message = '') {
  const text = String(message || '').trim()
  if (!text) return true
  if (isTdxSessionConfigFailureDetail(text)) return false
  if (isTdxDirectUserFacingSessionFailureDetail(text)) return false
  if (isTdxTransientNetworkFailureMessage(text)) return true
  return true
}

function isTdxSessionConfigFailureDetail(message = '') {
  const text = String(message || '').trim()
  if (!text) return false
  return /TDX live mode is not ready|is required in live mode|QMF appId is required|QMF username is required|QMF password is required/i.test(text)
}

function resolveTdxSessionFailureDetail(sessionFailure = null) {
  const detail = String(
    sessionFailure instanceof Error
      ? sessionFailure.message
      : typeof sessionFailure === 'string'
        ? sessionFailure
      : sessionFailure?.message ||
        sessionFailure?.login?.message ||
        sessionFailure?.login?.payload?.message ||
        sessionFailure?.login?.payload?.msg ||
        sessionFailure?.failureStep ||
        '',
  ).trim()
  return detail.slice(0, 1000)
}

function resolveTdxSessionFailureStep(sessionFailure = null) {
  return String(
    sessionFailure instanceof Error
      ? ''
      : sessionFailure?.failureStep ||
        sessionFailure?.login?.step ||
        '',
  ).trim()
}

function resolveTdxSessionCooldownMs(sessionFailure = null) {
  const value = Number(
    sessionFailure instanceof Error
      ? sessionFailure.cooldownMs
      : sessionFailure?.cooldownMs ||
        sessionFailure?.login?.cooldownMs ||
        sessionFailure?.raw?.cooldownMs ||
        0,
  )
  return Number.isFinite(value) && value > 0 ? Math.min(600000, Math.max(60000, value)) : 0
}

function isTdxSessionCooldownFailure(sessionFailure = null) {
  if (resolveTdxSessionCooldownMs(sessionFailure)) return true
  const detail = resolveTdxSessionFailureDetail(sessionFailure)
  return isTdxSessionCooldownMessage(detail)
}

function resolveTdxSessionQueuedAttempts({ previousAttempts = 0, currentAttempts = 0, sessionFailure = null } = {}) {
  return Math.max(0, Number(currentAttempts || 0))
}

function shouldQueueTdxSessionRetry({ attempts = 0, maxAttempts = 1, sessionFailure = null, handled = false } = {}) {
  if (!handled) return false
  return Number(attempts || 0) < Math.max(1, Number(maxAttempts || 1))
}

function isTdxBackendHandledSessionFailure(result = {}) {
  if (!result || result.ok) return false
  const failureStep = String(result.failureStep || '').trim()
  const detail = [
    result.message,
    result.publicMessage,
    result.login?.message,
    result.login?.payload?.message,
    failureStep,
  ]
    .filter(Boolean)
    .join('；')
  if (isTdxDirectUserFacingSessionFailureDetail(detail)) {
    return false
  }
  if (failureStep === 'login-sms') {
    return false
  }
  if (['prepare-session', 'save-login-token'].includes(failureStep)) {
    return isTdxSessionRetryableDetail(detail || failureStep)
  }
  return isTdxSessionRetryableDetail(detail)
}

function isTdxBackendHandledSessionError(error) {
  const detail = resolveTdxSessionFailureDetail(error)
  if (isTdxDirectUserFacingSessionFailureDetail(detail)) return false
  return isTdxSessionRetryableDetail(detail)
}

function resolveTdxSessionRetryIntervalMs() {
  return parsePositiveInt(process.env.TDX_SESSION_RETRY_INTERVAL_MS, TDX_SESSION_RETRY_DEFAULT_INTERVAL_MS, { min: 1000, max: 300000 })
}

function resolveTdxSessionRetryInitialDelayMs() {
  return parsePositiveInt(process.env.TDX_SESSION_RETRY_INITIAL_DELAY_MS, TDX_SESSION_RETRY_DEFAULT_INITIAL_DELAY_MS, { min: 500, max: 60000 })
}

function resolveTdxSessionRetryDelayMs(sessionFailure = null, fallbackMs = resolveTdxSessionRetryIntervalMs()) {
  const baseDelayMs = Math.max(1000, Number(fallbackMs || resolveTdxSessionRetryIntervalMs()))
  const explicitCooldownMs = resolveTdxSessionCooldownMs(sessionFailure)
  if (explicitCooldownMs) {
    return Math.max(baseDelayMs, explicitCooldownMs)
  }
  const detail = resolveTdxSessionFailureDetail(sessionFailure)
  if (!isTdxSessionCooldownMessage(detail)) {
    return baseDelayMs
  }
  const cooldownDelayMs = parsePositiveInt(
    process.env.TDX_SESSION_RETRY_COOLDOWN_INTERVAL_MS,
    TDX_SESSION_RETRY_COOLDOWN_DEFAULT_INTERVAL_MS,
    { min: 60000, max: 600000 },
  )
  return Math.max(baseDelayMs, cooldownDelayMs)
}

function resolveTdxSessionRetryMaxAttempts() {
  return parsePositiveInt(process.env.TDX_SESSION_RETRY_MAX_ATTEMPTS, TDX_SESSION_RETRY_DEFAULT_MAX_ATTEMPTS, { min: 1, max: 50 })
}

function buildTdxSessionRetryState({
  attempts = 0,
  maxAttempts = resolveTdxSessionRetryMaxAttempts(),
  nextRetryAtMs = Date.now() + resolveTdxSessionRetryInitialDelayMs(),
  status = 'queued',
  lastError = '',
  lastFailureStep = '',
  lastAttemptAtMs = 0,
  qmfNewOrderOpened = false,
} = {}) {
  const safeAttempts = Math.max(0, Number(attempts || 0))
  const safeMaxAttempts = Math.max(1, Number(maxAttempts || resolveTdxSessionRetryMaxAttempts()))
  const safeNextRetryAtMs = Math.max(0, Number(nextRetryAtMs || 0))
  const safeLastAttemptAtMs = Math.max(0, Number(lastAttemptAtMs || 0))
  return {
    status: String(status || 'queued').trim() || 'queued',
    attempts: safeAttempts,
    maxAttempts: safeMaxAttempts,
    nextRetryAtMs: safeNextRetryAtMs,
    nextRetryAt: formatTdxRetryAt(safeNextRetryAtMs),
    lastAttemptAtMs: safeLastAttemptAtMs,
    lastAttemptAt: formatTdxRetryAt(safeLastAttemptAtMs),
    lastError: String(lastError || '').trim().slice(0, 1000),
    lastFailureStep: String(lastFailureStep || '').trim().slice(0, 200),
    qmfNewOrderOpened: Boolean(qmfNewOrderOpened),
  }
}

function buildTdxSaveLoginTokenRetryContext({ phone = '', sessionFailure = null, fallback = null } = {}) {
  const fallbackContext = fallback && typeof fallback === 'object' ? fallback : null
  const failureContext =
    sessionFailure && typeof sessionFailure === 'object' && sessionFailure.retryContext && typeof sessionFailure.retryContext === 'object'
      ? sessionFailure.retryContext
      : null
  const source = failureContext || fallbackContext || null
  const failureStep = resolveTdxSessionFailureStep(sessionFailure)
  if (!failureContext && failureStep !== 'save-login-token') {
    return null
  }

  const retryContext = {
    mode: 'save-login-token',
    phone: String(phone || sessionFailure?.phone || source?.phone || '').trim(),
    orderId: String(sessionFailure?.orderId || source?.orderId || '').trim(),
    wxCode: String(sessionFailure?.wxCode || source?.wxCode || '').trim(),
    loginToken: String(sessionFailure?.login?.token || source?.loginToken || '').trim(),
    line: String(sessionFailure?.line || source?.line || fallbackContext?.line || 'line1').trim() || 'line1',
  }

  return isTdxSaveLoginTokenRetryContext(retryContext) ? retryContext : null
}

function isTdxSaveLoginTokenRetryContext(retryContext = {}) {
  return String(retryContext?.mode || '').trim() === 'save-login-token'
    && Boolean(
      String(retryContext?.phone || '').trim()
      && String(retryContext?.orderId || '').trim()
      && String(retryContext?.wxCode || '').trim()
      && String(retryContext?.loginToken || '').trim(),
    )
}

function buildTdxTaskRequest(task = {}) {
  const username = String(task.username || task.userName || task.name || '当前账号').trim() || '当前账号'
  return {
    tdxIntegration: Boolean(task.tdxIntegration),
    currentUser: {
      liveUserId: Number(task.liveUserId || task.userId || task.userid || 0),
      username,
      name: username,
    },
    body: {
      taskId: String(task.id || '').trim(),
      line: String(task.line || task.tdxLine || 'line1').trim() || 'line1',
    },
  }
}

async function findOrCreateTdxCleanupTask({ request, phone, sessionResult, smsCode }) {
  const requestedTaskId = String(request.body?.taskId || request.body?.id || '').trim()
  const resolvedLine = String(sessionResult?.line || request.body?.line || 'line1').trim() || 'line1'
  const tasks = await listTdxTasks()
  const task =
    (requestedTaskId ? tasks.find((item) => item.id === requestedTaskId && ['queued', 'processing'].includes(item.status)) : null) ||
    tasks.find((item) =>
      String(item.phone).trim() === String(phone).trim()
      && String(item.line || item.tdxLine || 'line1').trim() === resolvedLine
      && ['queued', 'processing'].includes(item.status))

  if (task) {
    const updatedAt = nowText()
    const next = await updateTdxTask(task.id, {
      line: resolvedLine,
      orderId: sessionResult.orderId,
      wxCode: sessionResult.wxCode,
      loginToken: sessionResult.login?.token || task.loginToken || '',
      status: 'processing',
      currentStep: '已关联会话，系统正在自动执行清理...',
      updatedAt,
      skipCheckAppletPhone: true,
      result: {
        ...(task.result || {}),
        tokenSaved: true,
        orderId: sessionResult.orderId,
        wxCode: sessionResult.wxCode,
      },
      notes: [
        { title: '登录成功自动触发清理', description: '系统检测到会话已就绪，已自动关联并立即执行申诉清理。', meta: updatedAt },
        ...(task.notes || []),
      ],
    })
    return { task: next, created: false }
  }

  const createdAt = nowText()
  const createdTask = await createTdxTask({
    id: `tdx-task-${Date.now()}`,
    phone,
    line: resolvedLine,
    username: request.currentUser?.name || request.currentUser?.username || '当前账号',
    companyname: TDX_SMS_RECEIVE_COMPANY_NAME,
    businessType: 'tdx',
    orderstatenote: String(smsCode || '').trim(),
    batchLabel: TDX_SMS_RECEIVE_COMPANY_NAME,
    orderId: sessionResult.orderId,
    wxCode: sessionResult.wxCode,
    loginToken: sessionResult.login?.token || '',
    status: 'processing',
    currentStep: '已创建任务，系统正在自动执行申诉清理...',
    skipCheckAppletPhone: true,
    createdAt,
    updatedAt: createdAt,
    failreason: '',
    notes: [
      { title: '任务已创建', description: `已为 ${phone} 创建 tdx二次申诉 任务。`, meta: createdAt },
      { title: '会话结果', description: '上游会话准备成功，正在立即执行补交申诉。', meta: '已就绪' },
    ],
    result: {
      tokenSaved: true,
      orderId: sessionResult.orderId,
      wxCode: sessionResult.wxCode,
    },
  })

  return { task: createdTask, created: true }
}

function resolveTdxExecutionStatus(execution) {
  return execution?.result?.status || (execution?.ok ? 'processing' : 'failed')
}

function resolveTdxExecutionFailureDetail(execution) {
  return normalizeFailReasonDesc(
    execution?.result?.failureReason ||
    execution?.result?.message ||
    execution?.message ||
    execution?.result?.failureStep ||
    '',
  )
}

function isTdxRefundSuggestedExecution(execution) {
  return execution?.result?.refundSuggested === true
}

function resolveTdxExecutionResultMessage(execution, fallback = '') {
  return String(
    execution?.result?.message ||
    execution?.message ||
    fallback ||
    '',
  ).trim()
}

function findTdxExecutionStep(execution, key = '') {
  const safeKey = String(key || '').trim()
  if (!safeKey || !Array.isArray(execution?.result?.steps)) return null
  return execution.result.steps.find((step) => String(step?.key || '').trim() === safeKey) || null
}

function hasTdxExecutionReachedSubmission(execution) {
  const rawStatus = resolveTdxExecutionStatus(execution)
  if (rawStatus === 'completed' || rawStatus === 'processing') return true
  const submitStatus = String(findTdxExecutionStep(execution, 'submit-answers')?.status || '').trim().toLowerCase()
  const evidenceStatus = String(findTdxExecutionStep(execution, 'upload-evidence')?.status || '').trim().toLowerCase()
  return submitStatus === 'completed' && evidenceStatus === 'completed'
}

function resolveTdxExecutionLifecycle(execution) {
  const rawStatus = resolveTdxExecutionStatus(execution)
  const submitted = hasTdxExecutionReachedSubmission(execution)
  if (rawStatus === 'completed') {
    return { status: 'completed', rawStatus, submitted: true }
  }
  if (rawStatus === 'failed' && submitted && !isTdxRefundSuggestedExecution(execution)) {
    return { status: 'processing', rawStatus, submitted: true, normalizedFromQueryFailure: true }
  }
  return {
    status: rawStatus === 'failed' ? 'failed' : 'processing',
    rawStatus,
    submitted,
    normalizedFromQueryFailure: false,
  }
}

function resolveTdxExecutionPublicMessage(execution, lifecycle = resolveTdxExecutionLifecycle(execution)) {
  if (lifecycle.status === 'completed') {
    return (
      resolveTdxExecutionResultMessage(
        execution,
        '号码标记已取消，部分手机端可能需要3-5个工作日的同步时间，请耐心等待。',
      ) || '号码标记已取消，部分手机端可能需要3-5个工作日的同步时间，请耐心等待。'
    )
  }
  if (isTdxRefundSuggestedExecution(execution)) {
    return TDX_REFUNDED_RECEIPT_RESULT_MESSAGE
  }
  if (lifecycle.status === 'failed') {
    return '申诉处理失败，请管理员查看详情。'
  }
  if (lifecycle.rawStatus === 'failed') {
    return '申诉已提交，等待处理。'
  }
  return (
    publicTdxProgressMessage(
      resolveTdxExecutionResultMessage(
        execution,
        '申诉已提交，等待处理。',
      ),
    ) || '申诉已提交，等待处理。'
  )
}

function normalizeTdxDateTimeText(value, fallback = '') {
  const text = String(value || '').trim()
  if (!text) return String(fallback || '').trim()
  if (/^\d{4}-\d{2}-\d{2}( \d{2}:\d{2}:\d{2})?$/.test(text)) return text
  const parsed = new Date(text)
  if (Number.isNaN(parsed.getTime())) return text
  return formatChinaDateTime(parsed)
}

function normalizeTdxDateText(value, fallback = '') {
  const normalized = normalizeTdxDateTimeText(value, fallback)
  return normalized ? normalized.slice(0, 10) : ''
}

function buildTdxSmsReceiveSuccessDialog({
  phone = '',
  orderDeal = null,
  autoCleanupResult = null,
  fallbackTime = nowText(),
  appealStatus = '已处理',
  resultMessage = '',
} = {}) {
  const execution = autoCleanupResult?.execution || null
  const progress = execution?.result?.progress || null
  const submitTime = normalizeTdxDateTimeText(
    orderDeal?.submittedAt || orderDeal?.createtime || fallbackTime,
    fallbackTime,
  )
  const appealSourceTime = progress?.auditTime || progress?.createTime || submitTime
  const normalizedStatus = String(appealStatus || '已处理').trim() || '已处理'
  const fallbackMessage = normalizedStatus === '处理中'
    ? '号码申诉已提交，请等待1-2个小时更新结果'
    : '号码标记已取消，部分手机端可能需要3-5个工作日的同步时间，请耐心等待。'
  const resolvedResultMessage =
    publicTdxProgressMessage(resultMessage)
    || resolveTdxExecutionResultMessage(
      execution,
      fallbackMessage,
    )
    || fallbackMessage

  return {
    phone: String(phone || orderDeal?.phone || '').trim(),
    appealStatus: normalizedStatus,
    resultMessage: resolvedResultMessage,
    appealDate: normalizeTdxDateText(appealSourceTime, submitTime),
    submitTime,
  }
}

function buildTdxSmsReceiveOrderPatchFromExecution(execution, updatedAt, task) {
  const lifecycle = resolveTdxExecutionLifecycle(execution)
  const finalStatus = lifecycle.status
  const completed = finalStatus === 'completed'
  const failed = finalStatus === 'failed'
  const refundSuggested = isTdxRefundSuggestedExecution(execution)
  const failureDetail = failed ? resolveTdxExecutionFailureDetail(execution) : ''
  const publicResultMessage = resolveTdxExecutionPublicMessage(execution, lifecycle)
  return {
    submitres: task?.orderId || execution?.result?.orderId || '',
    queryres: completed ? 'AppealCompleted' : refundSuggested ? 'AppealRefunded' : failed ? 'AppealFailed' : 'AppealSubmitted',
    dealquxiaores: completed
      ? (publicResultMessage || '申诉已处理完成。')
      : refundSuggested
        ? TDX_REFUNDED_RECEIPT_RESULT_MESSAGE
      : failed
        ? '申诉处理失败，请管理员查看详情。'
        : (publicResultMessage || '申诉已提交，等待处理。'),
    orderstate: completed ? 6 : failed ? 777 : 33,
    successtime: completed ? updatedAt : null,
    failtime: failed ? updatedAt : null,
    failreason: failed ? (refundSuggested ? TDX_REFUNDED_RECEIPT_RESULT_MESSAGE : '申诉处理失败，请管理员查看详情。') : null,
    failreasondesc: failed ? (failureDetail || execution?.result?.failureStep || 'auto-cleanup') : null,
    UpdateOn: updatedAt,
  }
}

async function executeAndPersistTdxSmsReceiveCleanup({ request, task, orderDeal }) {
  const taskId = task?.id
  let execution
  let thrownError = null

  try {
    execution = await executeTdxCleanupTask({ ...task, skipCheckAppletPhone: true })
  } catch (err) {
    thrownError = err
    execution = {
      ok: false,
      result: {
        status: 'failed',
        message: err instanceof Error ? err.message : '申诉处理失败',
        failureStep: 'auto-cleanup',
      },
    }
  }

  const updatedAt = nowText()
  const lifecycle = resolveTdxExecutionLifecycle(execution)
  const finalStatus = lifecycle.status
  const refundSuggested = isTdxRefundSuggestedExecution(execution)
  const publicMessage = resolveTdxExecutionPublicMessage(execution, lifecycle)
  const currentStep = finalStatus === 'completed'
    ? '申诉已处理完成'
    : refundSuggested
      ? '失败并已退款'
    : finalStatus === 'failed'
      ? '申诉处理失败'
      : lifecycle.rawStatus === 'failed'
        ? '申诉已提交，等待处理（进度稍后同步）'
        : '申诉已提交，等待处理'

  let updatedTask = task
  if (taskId) {
    updatedTask = await updateTdxTask(taskId, {
      status: finalStatus,
      currentStep,
      updatedAt,
      result: {
        ...(task.result || {}),
        execution,
      },
      notes: [
        {
          title: '提交后自动执行申诉',
          description: finalStatus === 'completed'
            ? '提交后已自动创建/关联任务并完成申诉处理。'
            : refundSuggested
              ? '提交后已自动创建/关联任务，但上游显示号码仍在审核，系统已自动退款并停止后续自动申诉。'
            : finalStatus === 'failed'
              ? '提交后已自动创建/关联任务，但申诉处理失败，请管理员查看任务详情。'
              : lifecycle.rawStatus === 'failed'
                ? '提交后已自动创建/关联任务，申诉已提交成功，进度查询稍后继续同步。'
                : '提交后已自动创建/关联任务，并已提交申诉等待处理。',
          meta: updatedAt,
        },
        ...(task.notes || []),
      ],
    })
  }

  let orderPatch = null
  let billing = null
  if (orderDeal?.id) {
    orderPatch = buildTdxSmsReceiveOrderPatchFromExecution(execution, updatedAt, task)
    await updateTdxSmsReceiveOrderDealById(orderDeal.id, orderPatch)
    billing = await reconcileTdxSmsReceiveBillingByOrderId(orderDeal.id, {
      reason: refundSuggested ? 'execute-refunded' : finalStatus === 'failed' ? 'execute-failed' : 'execute-submitted',
    })
  }

  return {
    task: updatedTask || task,
    execution,
    finalStatus,
    rawFinalStatus: lifecycle.rawStatus,
    publicMessage,
    orderPatch,
    billing,
    thrownError,
  }
}

async function syncTdxIntegrationTask(task = {}) {
  const attempts = Number(task.integrationCheckAttempts || 0) + 1
  let execution
  try {
    execution = await executeTdxCleanupTask({ ...task, skipCheckAppletPhone: true })
  } catch (err) {
    execution = {
      ok: false,
      result: {
        status: attempts >= 3 ? 'failed' : 'processing',
        refundSuggested: attempts >= 3,
        message: attempts >= 3 ? '多次自动处理后仍未成功，本次退款' : '上游暂时不可用，等待下次自动处理',
        failureReason: err instanceof Error ? err.message : String(err || ''),
      },
    }
  }

  let lifecycle = resolveTdxExecutionLifecycle(execution)
  let refundSuggested = isTdxRefundSuggestedExecution(execution)
  if (lifecycle.status === 'processing' && attempts >= 3) {
    execution = {
      ...execution,
      ok: false,
      result: {
        ...(execution?.result || {}),
        status: 'failed',
        refundSuggested: true,
        message: '自动重申3次后仍未处理完成，本次退款',
      },
    }
    lifecycle = resolveTdxExecutionLifecycle(execution)
    refundSuggested = true
  }

  const status = lifecycle.status
  const message = resolveTdxExecutionPublicMessage(execution, lifecycle)
  const updatedAt = nowText()
  const updatedTask = await updateTdxTask(task.id, {
    status,
    currentStep: status === 'completed'
      ? '申诉已处理完成'
      : status === 'failed'
        ? '申诉处理失败，等待当前平台退款'
        : '申诉已提交，等待处理',
    updatedAt,
    integrationCheckAttempts: attempts,
    result: { ...(task.result || {}), execution },
    notes: [
      { title: '当前平台自动复查', description: message, meta: `${updatedAt} / 第${attempts}次` },
      ...(task.notes || []),
    ],
  })
  return { task: updatedTask, status, rawStatus: lifecycle.rawStatus, message, refundSuggested, attempts }
}

function resolveTdxCompanyName(code = '') {
  const key = String(code || '').trim()
  if (key === 'tdx-voice') return '泰迪熊语音'
  if (key === 'tdx-sms-send') return '泰迪熊发送短信'
  return '泰迪熊'
}

function validateSettingsPayload(body) {
  const errors = []
  if (body.maxOrderReuse !== undefined && Number(body.maxOrderReuse) < 1) {
    errors.push('maxOrderReuse must be at least 1')
  }
  if (body.mode && !['mock', 'live'].includes(body.mode)) {
    errors.push('mode must be mock or live')
  }
  return errors
}

function filterTasksForList(tasks, query = {}) {
  const status = String(query.status || '').trim().toLowerCase()
  const keyword = String(query.q || '').trim().toLowerCase()
  return tasks.filter((task) => {
    const matchesStatus = !status || status === 'all' || String(task.status || '').toLowerCase() === status
    const haystack = [task.id, task.phone, task.orderId, task.currentStep, task.batchLabel].filter(Boolean).join(' ').toLowerCase()
    const matchesKeyword = !keyword || haystack.includes(keyword)
    return matchesStatus && matchesKeyword
  })
}

function buildTaskNotes(task) {
  return [
    { title: 'Task status', description: `Current status is ${task.status}.`, meta: 'Status' },
    { title: 'Current step', description: task.currentStep, meta: 'Step' },
    { title: 'Order session', description: `${task.orderId} / ${task.wxCode}`, meta: 'QMF' },
  ]
}

function hasPreparedSession(task) {
  return Boolean(task?.orderId && task?.wxCode)
}

function isAllowedTaskStatus(status) {
  return ['queued', 'processing', 'completed', 'failed'].includes(String(status || '').trim().toLowerCase())
}

function normalizeIds(input) {
  return Array.isArray(input) ? [...new Set(input.map((item) => String(item).trim()).filter(Boolean))] : []
}

function isAdminRequest(request) {
  const user = request.currentUser || {}
  const candidates = [
    user.username,
    user.userName,
    user.name,
    user.role,
    user.roleName,
    user.access?.roleName,
  ]
    .filter(Boolean)
    .map((item) => String(item).trim().toLowerCase())
  return candidates.some((item) => item === 'admin' || item.includes('admin') || item.includes('管理员'))
}

function stripSensitive(value) {
  if (Array.isArray(value)) return value.map(stripSensitive)
  if (!value || typeof value !== 'object') return value
  const result = {}
  for (const [key, item] of Object.entries(value)) {
    const lower = key.toLowerCase()
    if (lower.includes('token') || lower.includes('authorization')) {
      result[key] = '[已隐藏]'
    } else {
      result[key] = stripSensitive(item)
    }
  }
  return result
}

function normalizePublicTdxFailureText(value = '') {
  const text = String(value || '').trim()
  if (!text) return '网络波动，请刷新重试'
  if (isTdxSessionConfigFailureDetail(text)) {
    return '网络波动，请刷新重试'
  }
  if (isTdxTransientNetworkFailureMessage(text)) {
    return '网络波动，请刷新重试'
  }
  if (isTdxSmsCodeFailureMessage(text)) {
    return '验证码错误或已失效，请重新获取后再试'
  }
  if (text.includes('当天已提交过申诉') || text.includes('不可重复提交')) {
    return '号码当天已提交过申诉，不可重复提交'
  }
  if (/(?:30\s*天|三十天|30日|近30天|一个月内|月内).*(?:申诉|提交)|(?:申诉|提交).*(?:30\s*天|三十天|30日|近30天|一个月内|月内)|(?:已申诉过|已提交过).*(?:30\s*天|三十天|30日|近30天|一个月内|月内)/i.test(text)) {
    return TDX_REPEAT_APPEAL_MESSAGE
  }
  if (/(?:\u6388\u6743\u4f1a\u8bdd|wxCode|wxcode|openId|openid|token|session|\u4e0a\u6e38|QMF|orderId)/i.test(text)) {
    return '网络波动，请刷新重试'
  }
  return text
}

function extractEmbeddedTdxMessage(value = '') {
  const text = String(value || '')
  const fields = ['message', 'msg']
  for (const field of fields) {
    const pattern = new RegExp(`"${field}"\\s*:\\s*"((?:\\\\.|[^"\\\\])*)"`, 'i')
    const match = text.match(pattern)
    if (!match) continue
    const encoded = `"${String(match[1] || '').replace(/\r/g, '\\r').replace(/\n/g, '\\n')}"`
    try {
      const decoded = JSON.parse(encoded)
      const normalized = String(decoded || '').trim()
      if (normalized) return normalized
    } catch {}
    const fallback = String(match[1] || '')
      .replace(/\\u([0-9a-fA-F]{4})/g, (_, hex) => String.fromCharCode(Number.parseInt(hex, 16)))
      .replace(/\\"/g, '"')
      .replace(/\\\\/g, '\\')
      .replace(/\\r/g, ' ')
      .replace(/\\n/g, ' ')
      .replace(/\\t/g, '')
      .trim()
    if (fallback) return fallback
  }
  return ''
}

function isTdxTechnicalLeakMessage(value = '') {
  const text = String(value || '').trim()
  if (!text) return false
  return /\[[^\]]+\]|https?:\/\/|(?:[a-z0-9-]+\.)+[a-z]{2,}(?:\/\S*)?|returned HTTP|TDX\s*proxy|upstream|rawText|non-JSON|sendSms|auth\/sendSms|customer-apply-diversion/i.test(text)
}

function publicTdxFailureMessage(fallback = '网络波动，请刷新重试') {
  const text = String(fallback || '').trim()
  if (!text) return '网络波动，请刷新重试'
  if (isTdxSessionConfigFailureDetail(text)) {
    return '申诉通道未配置，请联系管理员。'
  }
  if (/^\s*[\[{]/.test(text) || /"(?:message|msg)"\s*:/.test(text)) {
    const embedded = extractEmbeddedTdxMessage(text)
    if (embedded) return normalizePublicTdxFailureText(embedded)
  }
  if (isTdxTechnicalLeakMessage(text)) {
    const embedded = extractEmbeddedTdxMessage(text)
    if (embedded) return normalizePublicTdxFailureText(embedded)
    return '网络波动，请刷新重试'
  }
  return normalizePublicTdxFailureText(text)
}

function isTdxSmsCodeFailureMessage(message = '') {
  const text = String(message || '').trim()
  if (!text) return false
  return /(?:短信)?验证码.*(?:错误|有误|不正确|失效|过期|无效)|(?:短信码|校验码|动态码).*(?:错误|有误|不正确|失效|过期|无效)|(?:invalid|wrong|incorrect|expired).{0,12}(?:sms\s*code|verification code|code)|(?:sms\s*code|verification code|code).{0,12}(?:invalid|wrong|incorrect|expired)/i.test(text)
}

function isTdxOtherWechatLoginMessage(message = '') {
  const text = String(message || '').trim()
  if (!text) return false
  return /该手机号通过(?:其他|其它)微信登录|(?:其他|其它)微信登录|无法使用当前微信登录|当前微信登录/.test(text)
}

function isTdxDirectUserFacingSessionFailureDetail(message = '') {
  const text = String(message || '').trim()
  if (!text) return false
  if (isTdxSessionConfigFailureDetail(text)) return true
  if (isHardTdxSessionFailureMessage(text)) return true
  if (isTdxSmsCodeFailureMessage(text)) return true
  if (isTdxOtherWechatLoginMessage(text)) return true
  if (isTdxDailyLimitPrecheckMessage(text) || isTdxRepeatAppealWindowMessage(text)) return true
  return /手机号.*(?:错误|有误|不正确|无效|不存在)|phone.*(?:invalid|wrong|incorrect)|号码.*(?:暂不符合申诉条件|不可申诉|无标记|未标记)|(?:未标记|无标记).*(?:号码|手机号)/i.test(text)
}

function isTdxTransientNetworkFailureMessage(message = '') {
  const text = String(message || '').trim()
  return /(?:read|write)?\s*ECONNRESET|socket hang up|fetch failed|network error|ECONNREFUSED|ETIMEDOUT|EHOSTUNREACH|UND_ERR_SOCKET|aborted/i.test(text)
}

function publicTdxProgressMessage(value = '') {
  const text = String(value || '').trim()
  if (!text) return ''
  if (/(?:\u6388\u6743\u4f1a\u8bdd|wxCode|wxcode|openId|openid|token|session|\u4e0a\u6e38|QMF|orderId|upstream|progress\s*record|final\s*progress|cleanup\s*task|submitted\s+successfully|waiting\s+for.*processing|returned\s+yet|retumed\s+yet)/i.test(text)) {
    return '\u5df2\u63d0\u4ea4\uff0c\u6b63\u5728\u5904\u7406\u3002'
  }
  return text
}

function isTdxDailyLimitPrecheckMessage(message = '') {
  const text = String(message || '').trim()
  return /\u4eca\u65e5\u7533\u8bc9\u5df2\u8fbe\u4e0a|\u4eca\u5929\u7533\u8bc9\u5df2\u8fbe\u4e0a|\u6bcf\u65e5\u540c\u4e00\u7528\u6237\u7533\u8bc9\u4e0d\u5f97\u8d85\u8fc75\u6b21|\u6bcf\u5929\u540c\u4e00\u7528\u6237\u7533\u8bc9\u4e0d\u5f97\u8d85\u8fc75\u6b21|\u7533\u8bc9\u4e0d\u5f97\u8d85\u8fc75\u6b21/.test(text)
}

function isTdxRepeatAppealWindowMessage(message = '') {
  const text = String(message || '').trim()
  return /(?:30\s*\u5929|\u4e09\u5341\u5929|30\u65e5|\u8fd130\u5929|\u4e00\u4e2a\u6708\u5185|\u6708\u5185).*(?:\u7533\u8bc9|\u63d0\u4ea4)|(?:\u7533\u8bc9|\u63d0\u4ea4).*(?:30\s*\u5929|\u4e09\u5341\u5929|30\u65e5|\u8fd130\u5929|\u4e00\u4e2a\u6708\u5185|\u6708\u5185)|(?:\u5df2\u7533\u8bc9\u8fc7|\u5df2\u63d0\u4ea4\u8fc7).*(?:30\s*\u5929|\u4e09\u5341\u5929|30\u65e5|\u8fd130\u5929|\u4e00\u4e2a\u6708\u5185|\u6708\u5185)/i.test(text)
}

function isTdxUnmarkedPrecheckResult(result = {}) {
  const status = String(result?.status || '').trim()
  const message = String(result?.message || '').trim()

  if (/(?:\u672a\u6807\u8bb0|\u65e0\u6807\u8bb0|\u6ca1\u6709\u6807\u8bb0|\u672a\u88ab\u6807\u8bb0|\u6682\u672a\u6807\u8bb0|\u672a\u67e5\u8be2\u5230.{0,6}\u6807\u8bb0)/.test(message)) {
    return true
  }

  if (!message && status.includes('\u672a\u6807\u8bb0')) {
    return true
  }

  if (status.includes('\u672a\u6807\u8bb0') && !isTdxDailyLimitPrecheckMessage(message) && !isTdxRepeatAppealWindowMessage(message) && !message.includes('\u4e0d\u53ef\u91cd\u590d\u63d0\u4ea4') && !message.includes('\u6807\u8bb0\u5df2\u53d6\u6d88')) {
    return true
  }

  return false
}

function publicTdxPrecheckFailureMessage(result = {}) {
  const message = String(result?.message || '').trim()
  const status = String(result?.status || '').trim()
  if (message.includes('\u5f53\u5929\u5df2\u63d0\u4ea4\u8fc7\u7533\u8bc9')) {
    return '\u53f7\u7801\u5f53\u5929\u5df2\u63d0\u4ea4\u8fc7\u7533\u8bc9\uff0c\u4e0d\u53ef\u91cd\u590d\u63d0\u4ea4'
  }
  if (isTdxRepeatAppealWindowMessage(message)) {
    return TDX_REPEAT_APPEAL_MESSAGE
  }
  if (message.includes('\u4e0d\u53ef\u91cd\u590d\u63d0\u4ea4')) {
    return '\u53f7\u7801\u5f53\u5929\u5df2\u63d0\u4ea4\u8fc7\u7533\u8bc9\uff0c\u4e0d\u53ef\u91cd\u590d\u63d0\u4ea4'
  }
  return message || status || '\u53f7\u7801\u6682\u4e0d\u7b26\u5408\u7533\u8bc9\u6761\u4ef6\u3002'
}

function resolveTdxSmsSendFailureMessage(result = {}, fallback = '验证码发送失败，请稍后重试。') {
  const payload = result?.payload || {}
  const candidates = [
    payload?.message,
    payload?.msg,
    payload?.data?.message,
    payload?.data?.msg,
    payload?.raw?.message,
    payload?.raw?.msg,
    result?.message,
    result?.error,
    fallback,
  ]
  const message = candidates
    .map((item) => String(item || '').trim())
    .find(Boolean)
  return publicTdxFailureMessage(message || fallback)
}

function sendTdxSmsSendFailure(reply, request, result = {}, fallback = '验证码发送失败，请稍后重试。') {
  const admin = isAdminRequest(request)
  const message = resolveTdxSmsSendFailureMessage(result, fallback)
  return reply.code(502).send({
    success: false,
    message,
    data: admin ? stripSensitive({ result, message }) : null,
  })
}

function buildTdxPrecheckBlockResponse(result = {}, request) {
  const admin = isAdminRequest(request)
  return {
    success: false,
    message: admin ? (String(result?.message || '').trim() || '号码暂不符合申诉条件。') : publicTdxPrecheckFailureMessage(result),
    data: admin
      ? stripSensitive(result)
      : { ok: Boolean(result?.ok), allowed: false, status: String(result?.status || '不可申诉').trim() || '不可申诉' },
  }
}

async function resolveTdxSmsSendPrecheckBlock(phone, request, { includeRecentAppeal = true } = {}) {
  const normalizedPhone = String(phone || '').trim()
  if (!/^1\d{10}$/.test(normalizedPhone)) {
    return {
      statusCode: 400,
      response: { success: false, message: '请输入正确的手机号', data: null },
    }
  }

  if (includeRecentAppeal) {
    const recentAppeal = await findRecentLocalSuccessfulTdxAppeal(normalizedPhone)
    if (recentAppeal) {
      return {
        statusCode: 409,
        response: {
          success: false,
          message: TDX_REPEAT_APPEAL_MESSAGE,
          data: isAdminRequest(request)
            ? { ok: true, allowed: false, status: '30天内已申诉过', recentAppeal }
            : { ok: true, allowed: false, status: '30天内已申诉过' },
        },
      }
    }
  }

  const result = await precheckTdxPhone(normalizedPhone)
  if (!result.allowed) {
    return {
      statusCode: 409,
      response: buildTdxPrecheckBlockResponse(result, request),
    }
  }
  return null
}

function sanitizeSessionResult(result = {}, request) {
  const admin = isAdminRequest(request)
  const base = {
    ok: Boolean(result.ok),
    orderId: admin ? result.orderId || '' : '',
    wxCode: admin ? result.wxCode || '' : '',
    usedCount: Number(result.usedCount || 0),
    tokenSaved: Boolean(result.tokenSaved || result.login?.token),
  }
  if (result.ok) {
    return {
      ...base,
      message: '登录成功，会话已保存，可继续申诉。',
    }
  }
  if (admin) {
    return stripSensitive({
      ...base,
      message: result.message || result.login?.message || result.login?.payload?.message || '上游短信登录失败',
      login: result.login || null,
    })
  }
  return {
    ...base,
    message: publicTdxFailureMessage(result.publicMessage || result.message || '登录失败，请核对手机号和验证码后重试。'),
  }
}

function sanitizeTaskForResponse(task = {}, request) {
  const admin = isAdminRequest(request)
  const safe = stripSensitive(task)

  if (!admin) {
    delete safe.orderId
    delete safe.wxCode
    delete safe.debug
    delete safe.retryContext
    if (safe.currentStep) safe.currentStep = publicTdxProgressMessage(safe.currentStep)
    if (Array.isArray(safe.notes)) safe.notes = []
    if (safe.sessionRetry && typeof safe.sessionRetry === 'object') {
      safe.sessionRetry = {
        status: String(safe.sessionRetry.status || '').trim(),
        attempts: Number(safe.sessionRetry.attempts || 0),
        maxAttempts: Number(safe.sessionRetry.maxAttempts || 0),
        nextRetryAt: String(safe.sessionRetry.nextRetryAt || '').trim(),
      }
    }
  }

  if (safe.result && typeof safe.result === 'object') {
    if ('login' in safe.result || 'tokenSaved' in safe.result || 'orderId' in safe.result || 'wxCode' in safe.result) {
      safe.result = sanitizeSessionResult(task.result, request)
    } else if (!admin && safe.result.execution && safe.result.execution.ok === false) {
      safe.result.execution = {
        ok: false,
        result: {
          status: safe.result.execution?.result?.status || 'failed',
          message: '清理失败，请稍后重试。',
        },
      }
    }
  }

  if (!admin && String(safe.status || '').toLowerCase() === 'failed') {
    safe.currentStep = '处理失败，请稍后重试。'
    safe.failreason = safe.failreason ? publicTdxFailureMessage('处理失败，请稍后重试。') : ''
  }

  return safe
}

function sendTdxFailure(reply, request, error, fallback = '网络波动，请刷新重试', statusCode = 400, options = {}) {
  const admin = isAdminRequest(request)
  const maskActual = Boolean(options?.maskActual)
  const actual = error instanceof Error ? error.message : String(error || fallback)
  const publicMessage = maskActual
    ? publicTdxFailureMessage(fallback)
    : publicTdxFailureMessage(actual || fallback)
  return reply.code(statusCode).send({
    success: false,
    message: publicMessage,
    data: admin ? stripSensitive({ error: actual, publicMessage }) : null,
  })
}

function isTdxSessionRetryTask(task = {}) {
  return String(task?.taskType || '').trim() === TDX_SMS_RECEIVE_SESSION_RETRY_TASK_TYPE
}

function isTdxPendingTaskStatus(status = '') {
  return ['queued', 'processing'].includes(String(status || '').trim().toLowerCase())
}

function isDueTdxSessionRetryTask(task = {}, nowMs = Date.now()) {
  if (!isTdxSessionRetryTask(task) || !isTdxPendingTaskStatus(task.status) || hasPreparedSession(task)) {
    return false
  }
  const nextRetryAtMs = Number(task.sessionRetry?.nextRetryAtMs || 0)
  return !nextRetryAtMs || nextRetryAtMs <= nowMs
}

function scheduleTdxSessionRetryRun(delayMs = resolveTdxSessionRetryIntervalMs()) {
  if (tdxSessionRetryStopped) return
  const waitMs = Math.max(200, Number(delayMs || resolveTdxSessionRetryIntervalMs()))
  const targetAtMs = Date.now() + waitMs
  if (tdxSessionRetryTimer && tdxSessionRetryNextRunAtMs && tdxSessionRetryNextRunAtMs <= targetAtMs) {
    return
  }
  if (tdxSessionRetryTimer) {
    clearTimeout(tdxSessionRetryTimer)
  }
  tdxSessionRetryNextRunAtMs = targetAtMs
  tdxSessionRetryTimer = setTimeout(() => {
    tdxSessionRetryTimer = null
    tdxSessionRetryNextRunAtMs = 0
    void runTdxSessionRetryTick()
  }, waitMs)
}

async function scheduleNextTdxSessionRetryRunFromStore() {
  if (tdxSessionRetryStopped) return
  const tasks = await listTdxTasks()
  const pendingTasks = tasks.filter((task) => isTdxSessionRetryTask(task) && isTdxPendingTaskStatus(task.status) && !hasPreparedSession(task))
  if (!pendingTasks.length) return
  const nowMs = Date.now()
  const nextRetryAtMs = pendingTasks.reduce((min, task) => {
    const taskNextMs = Number(task.sessionRetry?.nextRetryAtMs || 0) || nowMs
    return Math.min(min, taskNextMs)
  }, Number.MAX_SAFE_INTEGER)
  const delayMs = nextRetryAtMs <= nowMs ? 200 : nextRetryAtMs - nowMs
  scheduleTdxSessionRetryRun(delayMs)
}

function pokeTdxSessionRetryService(delayMs = resolveTdxSessionRetryInitialDelayMs()) {
  if (!tdxSessionRetryStarted || tdxSessionRetryStopped) return
  scheduleTdxSessionRetryRun(delayMs)
}

function stopTdxSessionRetryService() {
  tdxSessionRetryStopped = true
  tdxSessionRetryStarted = false
  tdxSessionRetryRunning = false
  tdxSessionRetryNextRunAtMs = 0
  if (tdxSessionRetryTimer) {
    clearTimeout(tdxSessionRetryTimer)
    tdxSessionRetryTimer = null
  }
}

function startTdxSessionRetryService() {
  if (tdxSessionRetryStarted) return stopTdxSessionRetryService
  tdxSessionRetryStopped = false
  tdxSessionRetryStarted = true
  scheduleTdxSessionRetryRun(resolveTdxSessionRetryInitialDelayMs())
  return stopTdxSessionRetryService
}

async function findOrCreateTdxSessionRetryTask({ request, phone, smsCode, taskId = '', sessionFailure = null, orderDeal = null }) {
  const requestedTaskId = String(taskId || request.body?.taskId || request.body?.id || '').trim()
  const userId = Number(request.currentUser?.liveUserId || 0)
  const tasks = await listTdxTasks()
  const task =
    (requestedTaskId
      ? tasks.find((item) => item.id === requestedTaskId && isTdxPendingTaskStatus(item.status))
      : null) ||
    tasks.find(
      (item) =>
        isTdxSessionRetryTask(item) &&
        isTdxPendingTaskStatus(item.status) &&
        !hasPreparedSession(item) &&
        String(item.phone || '').trim() === String(phone || '').trim() &&
        Number(item.liveUserId || item.userId || item.userid || 0) === userId,
    )

  const updatedAt = nowText()
  const nextRetryDelayMs = resolveTdxSessionRetryDelayMs(sessionFailure, resolveTdxSessionRetryInitialDelayMs())
  const nextRetryAtMs = Date.now() + nextRetryDelayMs
  const maxAttempts = Number(task?.sessionRetry?.maxAttempts || resolveTdxSessionRetryMaxAttempts())
  const qmfNewOrderOpened = Boolean(task?.sessionRetry?.qmfNewOrderOpened || sessionFailure?.qmfNewOrderOpened)
  const retryContext =
    buildTdxSaveLoginTokenRetryContext({ phone, sessionFailure, fallback: task?.retryContext || null })
    || (resolveTdxSessionFailureStep(sessionFailure) === 'save-login-token' && isTdxSaveLoginTokenRetryContext(task?.retryContext)
      ? task.retryContext
      : null)
  const retryState = buildTdxSessionRetryState({
    attempts: Number(task?.sessionRetry?.attempts || 0),
    maxAttempts,
    nextRetryAtMs,
    status: 'queued',
    lastError: resolveTdxSessionFailureDetail(sessionFailure),
    lastFailureStep: resolveTdxSessionFailureStep(sessionFailure),
    lastAttemptAtMs: Number(task?.sessionRetry?.lastAttemptAtMs || 0),
    qmfNewOrderOpened,
  })
  const debug = {
    ...(task?.debug && typeof task.debug === 'object' ? task.debug : {}),
    lastSessionFailure: stripSensitive(sessionFailure || null),
    lastQueuedAt: updatedAt,
  }

  if (task) {
    const updated = await updateTdxTask(task.id, {
      taskType: TDX_SMS_RECEIVE_SESSION_RETRY_TASK_TYPE,
      flow: 'sms-receive',
      tdxIntegration: Boolean(request.tdxIntegration),
      liveUserId: userId,
      username: request.currentUser?.name || request.currentUser?.username || task.username || '当前账号',
      phone,
      companyname: TDX_SMS_RECEIVE_COMPANY_NAME,
      businessType: 'tdx',
      batchLabel: TDX_SMS_RECEIVE_COMPANY_NAME,
      orderstatenote: String(smsCode || '').trim(),
      orderDealId: Number(orderDeal?.id || task.orderDealId || 0),
      orderpicinumber: String(orderDeal?.orderpicinumber || task.orderpicinumber || '').trim(),
      status: 'processing',
      currentStep: TDX_SMS_RECEIVE_PENDING_STEP,
      updatedAt,
      failreason: '',
      result: {
        status: 'processing',
        message: TDX_SMS_RECEIVE_PENDING_MESSAGE,
        sessionRetry: true,
      },
      retryContext,
      sessionRetry: retryState,
      debug,
      notes: [
        { title: '上游异常自动重试排队', description: '检测到上游授权/会话异常，系统已转为后台静默重试。', meta: updatedAt },
        ...(task.notes || []),
      ],
    })
    return { task: updated, created: false, nextRetryDelayMs }
  }

  const createdAt = updatedAt
  const createdTask = await createTdxTask({
    id: `tdx-task-${Date.now()}`,
    taskType: TDX_SMS_RECEIVE_SESSION_RETRY_TASK_TYPE,
    flow: 'sms-receive',
    tdxIntegration: Boolean(request.tdxIntegration),
    liveUserId: userId,
    phone,
    username: request.currentUser?.name || request.currentUser?.username || '当前账号',
    companyname: TDX_SMS_RECEIVE_COMPANY_NAME,
    businessType: 'tdx',
    orderstatenote: String(smsCode || '').trim(),
    batchLabel: TDX_SMS_RECEIVE_COMPANY_NAME,
    orderDealId: Number(orderDeal?.id || 0),
    orderpicinumber: String(orderDeal?.orderpicinumber || '').trim(),
    status: 'processing',
    currentStep: TDX_SMS_RECEIVE_PENDING_STEP,
    createdAt,
    updatedAt,
    failreason: '',
    result: {
      status: 'processing',
      message: TDX_SMS_RECEIVE_PENDING_MESSAGE,
      sessionRetry: true,
    },
    retryContext,
    sessionRetry: retryState,
    debug,
    notes: [
      { title: '任务已创建', description: `已为 ${phone} 创建 tdx二次申诉 后台重试任务。`, meta: createdAt },
      { title: '上游异常自动重试排队', description: '检测到上游授权/会话异常，系统将自动在后台重试。', meta: createdAt },
    ],
  })
  return { task: createdTask, created: true, nextRetryDelayMs }
}

async function queueTdxSmsReceiveSessionRetry({ request, phone, smsCode, taskId = '', sessionFailure = null }) {
  const balanceGuard = request.tdxIntegration
    ? { ok: true }
    : await ensureTdxSmsReceiveRequestBalance(request, { phone })
  if (!balanceGuard.ok) {
    return {
      blocked: true,
      reason: String(balanceGuard.reason || 'insufficient-balance').trim() || 'insufficient-balance',
      message: TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
      balance: balanceGuard,
      orderDeal: null,
      task: null,
      createdTask: false,
    }
  }

  let orderDeal = null
  if (!request.tdxIntegration) {
    try {
      orderDeal = await saveTdxSmsReceiveRetryingOrderDeal({
        request,
        phone,
        smsCode,
        retryMessage: TDX_SMS_RECEIVE_PENDING_MESSAGE,
      })
    } catch (err) {
      console.error('Failed to persist TDX SMS receive retry order detail:', err)
    }
  }
  if (isTdxSmsReceiveInsufficientBalanceResult(orderDeal?.billing)) {
    await markTdxOrderDealInsufficientBalance(orderDeal.id, orderDeal.billing)
    return {
      blocked: true,
      reason: 'insufficient-balance',
      message: TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
      balance: orderDeal.billing,
      orderDeal,
      task: null,
      createdTask: false,
    }
  }
  const taskResult = await findOrCreateTdxSessionRetryTask({
    request,
    phone,
    smsCode,
    taskId,
    sessionFailure,
    orderDeal,
  })
  pokeTdxSessionRetryService(Math.min(Number(taskResult.nextRetryDelayMs || 500), 1000))
  return {
    orderDeal,
    task: taskResult.task,
    createdTask: Boolean(taskResult.created),
  }
}

function buildTdxSessionRetryQueuedResponse({ request, phone, deferred = null } = {}) {
  const task = deferred?.task || null
  return {
    ok: true,
    tokenSaved: false,
    usedCount: 0,
    message: TDX_SMS_RECEIVE_PENDING_MESSAGE,
    orderDeal: deferred?.orderDeal || null,
    autoCleanupStarted: true,
    autoCleanupTaskId: task?.id || '',
    autoCleanupCreatedTask: Boolean(deferred?.createdTask),
    autoCleanupStatus: 'processing',
    autoCleanupRawStatus: 'session-retrying',
    autoCleanupCompleted: false,
    successDialog: buildTdxSmsReceiveSuccessDialog({
      phone,
      orderDeal: deferred?.orderDeal || null,
      appealStatus: '处理中',
      resultMessage: TDX_SMS_RECEIVE_PENDING_MESSAGE,
      fallbackTime: deferred?.orderDeal?.submittedAt || nowText(),
      autoCleanupResult: {
        execution: {
          result: {
            status: 'processing',
            rawStatus: 'session-retrying',
            message: TDX_SMS_RECEIVE_PENDING_MESSAGE,
          },
        },
      },
    }),
    autoCleanupTask: task ? sanitizeTaskForResponse(task, request) : null,
    autoCleanupExecution: {
      ok: true,
      result: {
        status: 'processing',
        rawStatus: 'session-retrying',
        message: TDX_SMS_RECEIVE_PENDING_MESSAGE,
      },
    },
    sessionRetryQueued: true,
    phone: String(phone || deferred?.orderDeal?.phone || '').trim(),
  }
}

async function markTdxSessionRetryTaskFailed(task = {}, detail = '', failureStep = '', { qmfNewOrderOpened = task.sessionRetry?.qmfNewOrderOpened } = {}) {
  const updatedAt = nowText()
  const maxAttempts = Number(task.sessionRetry?.maxAttempts || resolveTdxSessionRetryMaxAttempts())
  const attempts = Math.min(Number(task.sessionRetry?.attempts || 0), maxAttempts)
  const updatedTask = await updateTdxTask(task.id, {
    status: 'failed',
    currentStep: '后台自动处理失败，请管理员查看详情。',
    updatedAt,
    failreason: '申诉处理失败，请管理员查看详情。',
    result: {
      status: 'failed',
      message: '申诉处理失败，请管理员查看详情。',
      sessionRetry: true,
    },
    sessionRetry: buildTdxSessionRetryState({
      attempts,
      maxAttempts,
      nextRetryAtMs: 0,
      status: 'failed',
      lastError: detail,
      lastFailureStep: failureStep,
      lastAttemptAtMs: Date.now(),
      qmfNewOrderOpened: Boolean(qmfNewOrderOpened),
    }),
    debug: {
      ...(task.debug && typeof task.debug === 'object' ? task.debug : {}),
      lastSessionFailure: { detail, failureStep, updatedAt },
    },
    notes: [
      { title: '后台自动重试失败', description: '多次处理后仍未恢复，请管理员查看详情。', meta: updatedAt },
      ...(task.notes || []),
    ],
  })

  if (Number(task.orderDealId || 0)) {
    await updateTdxSmsReceiveOrderDealById(task.orderDealId, {
      queryres: 'AppealFailed',
      dealquxiaores: '申诉处理失败，请管理员查看详情。',
      orderstate: 777,
      successtime: null,
      failtime: updatedAt,
      failreason: '申诉处理失败，请管理员查看详情。',
      failreasondesc: normalizeFailReasonDesc(detail || failureStep || 'session-retry-failed'),
      UpdateOn: updatedAt,
    })
    await reconcileTdxSmsReceiveBillingByOrderId(task.orderDealId, { reason: 'session-retry-failed' })
  }

  return updatedTask
}

async function processTdxSessionRetryTask(task = {}) {
  const startedAtMs = Date.now()
  const updatedAt = nowText()
  const previousAttempts = Number(task.sessionRetry?.attempts || 0)
  const maxAttempts = Number(task.sessionRetry?.maxAttempts || resolveTdxSessionRetryMaxAttempts())
  if (previousAttempts >= maxAttempts) {
    await markTdxSessionRetryTaskFailed(
      task,
      `Session retry limit reached (${previousAttempts}/${maxAttempts}).`,
      String(task.sessionRetry?.lastFailureStep || 'session-retry-limit'),
    )
    return
  }
  const attempts = previousAttempts + 1
  let qmfNewOrderOpenedInFlow = Boolean(task.sessionRetry?.qmfNewOrderOpened)
  const runningTask =
    (await updateTdxTask(task.id, {
      status: 'processing',
      currentStep: TDX_SMS_RECEIVE_PENDING_STEP,
      updatedAt,
      failreason: '',
      sessionRetry: buildTdxSessionRetryState({
        attempts,
        maxAttempts,
        nextRetryAtMs: 0,
        status: 'running',
        lastError: '',
        lastFailureStep: '',
        lastAttemptAtMs: startedAtMs,
        qmfNewOrderOpened: qmfNewOrderOpenedInFlow,
      }),
      notes: [
        { title: '后台自动重试开始', description: `正在执行第 ${attempts} 次后台会话重试。`, meta: updatedAt },
        ...(task.notes || []),
      ],
    })) || task

  const phone = String(runningTask.phone || '').trim()
  const smsCode = String(runningTask.orderstatenote || '').trim()
  const lastFailureStep = String(runningTask.sessionRetry?.lastFailureStep || '').trim()
  const taskRequest = buildTdxTaskRequest(runningTask)
  const saveLoginTokenRetryContext = isTdxSaveLoginTokenRetryContext(runningTask.retryContext)
    ? runningTask.retryContext
    : null

  const balanceGuard = taskRequest.tdxIntegration
    ? { ok: true }
    : await ensureTdxSmsReceiveRequestBalance(taskRequest, {
        phone,
        orderDealId: runningTask.orderDealId,
      })
  if (!balanceGuard.ok) {
    const failedAt = nowText()
    await updateTdxTask(runningTask.id, {
      status: 'failed',
      currentStep: '余额不足，已停止后台自动处理，请先充值后重试。',
      updatedAt: failedAt,
      failreason: TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
      result: {
        status: 'failed',
        message: TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
        sessionRetry: true,
      },
      sessionRetry: buildTdxSessionRetryState({
        attempts,
        maxAttempts,
        nextRetryAtMs: 0,
        status: 'failed',
        lastError: 'insufficient-balance',
        lastFailureStep: 'balance-check',
        lastAttemptAtMs: startedAtMs,
        qmfNewOrderOpened: qmfNewOrderOpenedInFlow,
      }),
      notes: [
        { title: '余额不足停止处理', description: '本地余额不足，后台自动重试已停止。', meta: failedAt },
        ...(runningTask.notes || []),
      ],
    })
    if (Number(runningTask.orderDealId || 0)) {
      await markTdxOrderDealInsufficientBalance(runningTask.orderDealId, balanceGuard)
    }
    return
  }

  let sessionResult
  if (lastFailureStep === 'save-login-token' && !saveLoginTokenRetryContext) {
    await markTdxSessionRetryTaskFailed(
      runningTask,
      '缺少已成功的 login-sms 响应，无法仅重试 save-login-token。',
      'save-login-token',
      { qmfNewOrderOpened: qmfNewOrderOpenedInFlow },
    )
    return
  }
  try {
    if (saveLoginTokenRetryContext) {
      sessionResult = await persistTdxSmsReceiveLoginToken({
        phone: saveLoginTokenRetryContext.phone || phone,
        token: saveLoginTokenRetryContext.loginToken,
        orderId: saveLoginTokenRetryContext.orderId,
        wxCode: saveLoginTokenRetryContext.wxCode,
        line: saveLoginTokenRetryContext.line || runningTask.line || 'line1',
        login: {
          ok: true,
          token: saveLoginTokenRetryContext.loginToken,
          message: '复用已成功的 login-sms 响应重新提交 save-login-token。',
          payload: null,
        },
      })
    } else {
      sessionResult = await createTdxSmsReceiveSession({
        phone,
        smsCode,
        rotate: false,
        allowNewOrder: !qmfNewOrderOpenedInFlow,
        line: runningTask.line || 'line1',
      })
    }
  } catch (err) {
    qmfNewOrderOpenedInFlow = qmfNewOrderOpenedInFlow || Boolean(err?.qmfNewOrderOpened)
    const detail = resolveTdxSessionFailureDetail(err)
    const handled = isTdxBackendHandledSessionError(err)
    if (shouldQueueTdxSessionRetry({ attempts, maxAttempts, sessionFailure: err, handled })) {
      const retryAtMs = Date.now() + resolveTdxSessionRetryDelayMs(err, resolveTdxSessionRetryIntervalMs())
      const queuedAttempts = resolveTdxSessionQueuedAttempts({ previousAttempts, currentAttempts: attempts, sessionFailure: err })
      await updateTdxTask(runningTask.id, {
        status: 'processing',
        currentStep: TDX_SMS_RECEIVE_PENDING_STEP,
        updatedAt: nowText(),
        result: {
          status: 'processing',
          message: TDX_SMS_RECEIVE_PENDING_MESSAGE,
          sessionRetry: true,
        },
        sessionRetry: buildTdxSessionRetryState({
          attempts: queuedAttempts,
          maxAttempts,
          nextRetryAtMs: retryAtMs,
          status: 'queued',
          lastError: detail,
          lastFailureStep: 'create-session-exception',
          lastAttemptAtMs: startedAtMs,
          qmfNewOrderOpened: qmfNewOrderOpenedInFlow,
        }),
        debug: {
          ...(runningTask.debug && typeof runningTask.debug === 'object' ? runningTask.debug : {}),
          lastSessionFailure: stripSensitive({ message: detail, failureStep: 'create-session-exception' }),
        },
        notes: [
          { title: '后台自动重试继续排队', description: '上游仍未恢复，系统将继续后台处理。', meta: nowText() },
          ...(runningTask.notes || []),
        ],
      })
      if (Number(runningTask.orderDealId || 0)) {
        await updateTdxSmsReceiveOrderDealById(runningTask.orderDealId, {
          queryres: 'SessionRetrying',
          dealquxiaores: TDX_SMS_RECEIVE_PENDING_MESSAGE,
          orderstate: 333,
          successtime: null,
          failtime: null,
          failreason: null,
          failreasondesc: null,
          UpdateOn: nowText(),
        })
      }
      return
    }
    await markTdxSessionRetryTaskFailed(runningTask, detail, 'create-session-exception', { qmfNewOrderOpened: qmfNewOrderOpenedInFlow })
    return
  }

  qmfNewOrderOpenedInFlow = qmfNewOrderOpenedInFlow || Boolean(sessionResult?.qmfNewOrderOpened)
  if (!sessionResult?.ok) {
    const detail = resolveTdxSessionFailureDetail(sessionResult)
    const failureStep = resolveTdxSessionFailureStep(sessionResult) || 'session-retry'
    const handled = isTdxBackendHandledSessionFailure(sessionResult)
    if (shouldQueueTdxSessionRetry({ attempts, maxAttempts, sessionFailure: sessionResult, handled })) {
      const retryAtMs = Date.now() + resolveTdxSessionRetryDelayMs(sessionResult, resolveTdxSessionRetryIntervalMs())
      const queuedAttempts = resolveTdxSessionQueuedAttempts({ previousAttempts, currentAttempts: attempts, sessionFailure: sessionResult })
      await updateTdxTask(runningTask.id, {
        status: 'processing',
        currentStep: TDX_SMS_RECEIVE_PENDING_STEP,
        updatedAt: nowText(),
        result: {
          status: 'processing',
          message: TDX_SMS_RECEIVE_PENDING_MESSAGE,
          sessionRetry: true,
        },
        sessionRetry: buildTdxSessionRetryState({
          attempts: queuedAttempts,
          maxAttempts,
          nextRetryAtMs: retryAtMs,
          status: 'queued',
          lastError: detail,
          lastFailureStep: failureStep,
          lastAttemptAtMs: startedAtMs,
          qmfNewOrderOpened: qmfNewOrderOpenedInFlow,
        }),
        debug: {
          ...(runningTask.debug && typeof runningTask.debug === 'object' ? runningTask.debug : {}),
          lastSessionFailure: stripSensitive(sessionResult),
        },
        notes: [
          { title: '后台自动重试继续排队', description: '上游授权/会话仍未恢复，系统将继续后台处理。', meta: nowText() },
          ...(runningTask.notes || []),
        ],
      })
      if (Number(runningTask.orderDealId || 0)) {
        await updateTdxSmsReceiveOrderDealById(runningTask.orderDealId, {
          queryres: 'SessionRetrying',
          dealquxiaores: TDX_SMS_RECEIVE_PENDING_MESSAGE,
          orderstate: 333,
          successtime: null,
          failtime: null,
          failreason: null,
          failreasondesc: null,
          UpdateOn: nowText(),
        })
      }
      return
    }
    await markTdxSessionRetryTaskFailed(runningTask, detail, failureStep, { qmfNewOrderOpened: qmfNewOrderOpenedInFlow })
    return
  }

  try {
    const orderDeal = taskRequest.tdxIntegration
      ? null
      : await saveTdxSmsReceiveOrderDeal({
          request: taskRequest,
          phone,
          smsCode,
          sessionResult,
        })
    if (isTdxSmsReceiveInsufficientBalanceResult(orderDeal?.billing)) {
      await markTdxOrderDealInsufficientBalance(orderDeal.id, orderDeal.billing)
      await updateTdxTask(runningTask.id, {
        status: 'failed',
        currentStep: '余额不足，已停止后台自动处理，请先充值后重试。',
        updatedAt: nowText(),
        failreason: TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
        result: {
          status: 'failed',
          message: TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
          sessionRetry: true,
        },
      })
      return
    }
    const taskResult = await findOrCreateTdxCleanupTask({
      request: { ...taskRequest, body: { ...(taskRequest.body || {}), taskId: runningTask.id } },
      phone,
      sessionResult,
      smsCode,
    })
    const autoCleanupResult = await executeAndPersistTdxSmsReceiveCleanup({
      request: taskRequest,
      task: taskResult.task,
      orderDeal,
    })
    if (taskResult.task?.id) {
      await updateTdxTask(taskResult.task.id, {
        sessionRetry: buildTdxSessionRetryState({
          attempts,
          maxAttempts,
          nextRetryAtMs: 0,
          status: 'completed',
          lastError: '',
          lastFailureStep: '',
          lastAttemptAtMs: startedAtMs,
          qmfNewOrderOpened: qmfNewOrderOpenedInFlow,
        }),
        debug: {
          ...(taskResult.task.debug && typeof taskResult.task.debug === 'object' ? taskResult.task.debug : {}),
          lastSessionFailure: null,
          lastSessionRecoveredAt: nowText(),
        },
        orderDealId: Number(orderDeal?.id || runningTask.orderDealId || 0),
        orderpicinumber: String(orderDeal?.orderpicinumber || runningTask.orderpicinumber || '').trim(),
        retryContext: null,
        updatedAt: nowText(),
      })
    }
    if (autoCleanupResult?.finalStatus === 'failed' && Number(orderDeal?.id || 0)) {
      await reconcileTdxSmsReceiveBillingByOrderId(orderDeal.id, { reason: 'session-retry-execute-failed' })
    }
  } catch (err) {
    const detail = resolveTdxSessionFailureDetail(err)
    await markTdxSessionRetryTaskFailed(runningTask, detail, 'session-retry-cleanup', { qmfNewOrderOpened: qmfNewOrderOpenedInFlow })
  }
}

async function runTdxSessionRetryTick() {
  if (tdxSessionRetryStopped || tdxSessionRetryRunning) return
  tdxSessionRetryRunning = true
  try {
    const nowMs = Date.now()
    const tasks = await listTdxTasks()
    const dueTasks = tasks.filter((task) => isDueTdxSessionRetryTask(task, nowMs))
    for (const task of dueTasks) {
      try {
        await processTdxSessionRetryTask(task)
      } catch (err) {
        console.error('Failed to process TDX session retry task:', err)
      }
    }
  } catch (err) {
    console.error('TDX session retry service tick failed:', err)
  } finally {
    tdxSessionRetryRunning = false
    await scheduleNextTdxSessionRetryRunFromStore()
  }
}

async function ensureTdxMarkEnabled(reply) {
  const enabled = await isSystemSwitchEnabled('tdxMarkEnabled', true)
  if (!enabled) {
    return reply.code(409).send({ success: false, message: '系统已关闭泰迪熊标记模块，请先到系统设置开启后再试', data: null })
  }
  return null
}

async function ensureBulkActionEnabled(reply) {
  const enabled = await isSystemSwitchEnabled('bulkActionEnabled', true)
  if (!enabled) {
    return reply.code(409).send({ success: false, message: '系统已关闭批量操作，请先到系统设置开启后再试', data: null })
  }
  return null
}

export default async function tdxRoutes(app) {
  const stopTdxSessionRetry = startTdxSessionRetryService()
  app.addHook('onClose', async () => {
    stopTdxSessionRetry?.()
  })

  app.get('/executor-profile', { preHandler: requireModuleAccess('泰迪熊标记', 'read') }, async () => {
    const config = await getTdxConfig()
    return ok(buildTdxExecutorProfile(config))
  })

  app.get('/settings', { preHandler: requireModuleAccess('泰迪熊标记', 'read') }, async () => {
    const settings = await readTdxSettings()
    const merged = await getTdxConfig()
    const summary = summarizeTdxSettings(settings)
    return ok({ ...summary, liveValidationErrors: validateLiveTdxConfig(merged) })
  })

  app.patch('/settings', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const body = request.body || {}
    const validationErrors = validateSettingsPayload(body)
    if (validationErrors.length) {
      return reply.code(400).send({ success: false, message: validationErrors.join('; '), data: validationErrors })
    }
    const next = await writeTdxSettings(body)
    const merged = await getTdxConfig()
    const summary = summarizeTdxSettings(next)
    return ok({ ...summary, liveValidationErrors: validateLiveTdxConfig(merged) }, '泰迪熊设置已更新')
  })

  app.get('/runtime', { preHandler: requireModuleAccess('泰迪熊标记', 'read') }, async () => ok(await getTdxRuntimeSnapshot()))

  app.get('/health', { preHandler: requireModuleAccess('泰迪熊标记', 'read') }, async () => ok(await runTdxHealthCheck()))
  app.post('/runtime/rotate', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    return ok(await rotateTdxOrderSession(request.body?.line || 'line1'), '泰迪熊运行时已轮换')
  })

  app.post('/sms-receive/send-code', { preHandler: async (request, reply) => {
    const { businessType = 'tdx' } = request.body || {}
    const isTencent = businessType.includes('tengx')
    const moduleName = isTencent ? '标记处理' : '泰迪熊标记'
    const guard = requireModuleAccess(moduleName, 'create')
    return guard(request, reply)
  } }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    const { phone, line = 'line1', businessType = 'tdx' } = request.body || {}
    if (businessType.includes('tengx')) {
      return ok({ ok: true, message: '请在您手机的腾讯申诉页面获取验证码，并在下方输入提交' }, '请手动在手机上获取腾讯验证码')
    }
    try {
      const precheckBlock = request.tdxIntegration ? null : await resolveTdxSmsSendPrecheckBlock(phone, request)
      if (precheckBlock) {
        return reply.code(precheckBlock.statusCode).send(precheckBlock.response)
      }
      const result = await sendTdxSmsCode(phone, line)
      if (!result.ok) {
        return sendTdxSmsSendFailure(reply, request, result, '验证码发送失败，请稍后重试。')
      }
      return ok({ ok: true, mode: result.mode, line: result.line || line }, '短信验证码已发起发送')
    } catch (err) {
      return sendTdxFailure(reply, request, err, '发送验证码失败，请稍后重试。', 502)
    }
  })

  app.post('/sms-receive/precheck', { preHandler: requireModuleAccess('泰迪熊标记', 'read') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    const { phone } = request.body || {}
    if (!/^1\d{10}$/.test(String(phone || '').trim())) {
      return reply.code(400).send({ success: false, message: '请输入正确的手机号', data: null })
    }
    const recentAppeal = request.tdxIntegration ? null : await findRecentLocalSuccessfulTdxAppeal(phone)
    if (recentAppeal) {
      return reply.code(409).send({
        success: false,
        message: TDX_REPEAT_APPEAL_MESSAGE,
        data: isAdminRequest(request)
          ? { ok: true, allowed: false, status: '30天内已申诉过', recentAppeal }
          : { ok: true, allowed: false, status: '30天内已申诉过' },
      })
    }
    let result
    try {
      result = await precheckTdxPhone(phone)
    } catch (err) {
      return sendTdxFailure(reply, request, err, '号码预检查失败，请稍后重试。', 502)
    }
    if (!result.allowed) {
      const admin = isAdminRequest(request)
      return reply.code(409).send({
        success: false,
        message: admin ? (result.message || '号码暂不符合申诉条件。') : publicTdxPrecheckFailureMessage(result),
        data: admin ? stripSensitive(result) : { ok: Boolean(result.ok), allowed: false, status: result.status || '不可申诉' },
      })
    }
    return ok({ ok: true, allowed: true, status: result.status, message: result.message }, '号码预检查通过')
  })

  app.post('/sms-receive/order-session', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    const { rotate = false, line = 'line1' } = request.body || {}
    try {
      const session = await allocateTdxOrderSession({ forceRotate: rotate, line })
      return ok({ orderId: session.orderId, wxCode: session.wxCode, reused: session.reused, usedCount: session.usedCount, line: session.line || line }, 'orderId 会话已分配')
    } catch (err) {
      return sendTdxFailure(reply, request, err, '会话分配失败，请稍后重试。', 502)
    }
  })

  app.post('/sms-receive/login', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    const { phone, smsCode, rotate = false, taskId = '', line = 'line1' } = request.body || {}
    if (!/^1\d{10}$/.test(String(phone || '').trim())) {
      return reply.code(400).send({ success: false, message: '请输入正确的手机号', data: null })
    }
    if (!smsCode || String(smsCode).trim().length < 4) {
      return reply.code(400).send({ success: false, message: '请输入短信验证码', data: null })
    }
    const balanceGuard = request.tdxIntegration
      ? { ok: true }
      : await ensureTdxSmsReceiveRequestBalance(request, { phone })
    if (!balanceGuard.ok) {
      return reply.code(409).send({
        success: false,
        message: TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
        data: isAdminRequest(request) ? buildTdxInsufficientBalancePayload(balanceGuard) : null,
      })
    }
    let sessionResult
    try {
      sessionResult = await createTdxSmsReceiveSession({ phone, smsCode, rotate, line })
    } catch (err) {
      if (!isTdxDirectUserFacingSessionFailureDetail(resolveTdxSessionFailureDetail(err))) {
        try {
          const deferred = await queueTdxSmsReceiveSessionRetry({ request, phone, smsCode, taskId, sessionFailure: err })
          if (deferred?.blocked) {
            return reply.code(409).send({
              success: false,
              message: deferred.message || TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
              data: isAdminRequest(request) ? buildTdxInsufficientBalancePayload(deferred.balance) : null,
            })
          }
          return ok(buildTdxSessionRetryQueuedResponse({ request, phone, deferred }), '处理成功，请稍后到订单详情查看结果')
        } catch (queueErr) {
          console.error('Failed to queue TDX SMS receive session retry after exception:', queueErr)
          return sendTdxFailure(reply, request, queueErr, '网络波动，请刷新重试', 502, { maskActual: true })
        }
      }
      return sendTdxFailure(reply, request, err, '登录失败，请核对手机号和验证码后重试。', 502)
    }
    if (!sessionResult.ok) {
      if (!isTdxDirectUserFacingSessionFailureDetail(resolveTdxSessionFailureDetail(sessionResult))) {
        try {
          const deferred = await queueTdxSmsReceiveSessionRetry({ request, phone, smsCode, taskId, sessionFailure: sessionResult })
          if (deferred?.blocked) {
            return reply.code(409).send({
              success: false,
              message: deferred.message || TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
              data: isAdminRequest(request) ? buildTdxInsufficientBalancePayload(deferred.balance) : null,
            })
          }
          return ok(buildTdxSessionRetryQueuedResponse({ request, phone, deferred }), '处理成功，请稍后到订单详情查看结果')
        } catch (queueErr) {
          console.error('Failed to queue TDX SMS receive session retry after handled failure:', queueErr)
          return sendTdxFailure(reply, request, queueErr, '网络波动，请刷新重试', 502, { maskActual: true })
        }
      }
      const safe = sanitizeSessionResult(sessionResult, request)
      return reply.code(400).send({
        success: false,
        message: safe.message,
        data: safe,
      })
    }

    let orderDeal = null
    if (!request.tdxIntegration) {
      try {
        orderDeal = await saveTdxSmsReceiveOrderDeal({
          request,
          phone,
          smsCode,
          sessionResult,
        })
        if (isTdxSmsReceiveInsufficientBalanceResult(orderDeal?.billing)) {
          await markTdxOrderDealInsufficientBalance(orderDeal.id, orderDeal.billing)
          return reply.code(409).send({
            success: false,
            message: TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
            data: isAdminRequest(request) ? buildTdxInsufficientBalancePayload(orderDeal.billing) : null,
          })
        }
      } catch (err) {
        return sendTdxFailure(reply, request, err, '订单记录保存失败，请联系管理员。', 500)
      }
    }

    let autoCleanupStarted = false
    let autoCleanupTaskId = ''
    let autoCleanupCreatedTask = false
    let autoCleanupStatus = ''
    let autoCleanupResult = null
    let autoCleanupRefunded = false
    try {
      const taskResult = await findOrCreateTdxCleanupTask({
        request: { ...request, body: { ...(request.body || {}), taskId } },
        phone,
        sessionResult,
        smsCode,
      })
      autoCleanupStarted = true
      autoCleanupCreatedTask = Boolean(taskResult.created)
      autoCleanupTaskId = taskResult.task?.id || ''
      autoCleanupResult = await executeAndPersistTdxSmsReceiveCleanup({
        request,
        task: taskResult.task,
        orderDeal,
      })
      autoCleanupStatus = autoCleanupResult.finalStatus
      autoCleanupRefunded = Boolean(autoCleanupResult?.execution?.result?.refundSuggested)
    } catch (err) {
      console.error('Failed to create/execute TDX SMS receive cleanup task:', err)
      if (orderDeal?.id) {
        const failedAt = nowText()
        const failureDetail = normalizeFailReasonDesc(err instanceof Error ? err.message : err || 'auto-cleanup')
        await updateTdxSmsReceiveOrderDealById(orderDeal.id, {
          queryres: 'AppealFailed',
          dealquxiaores: '申诉处理失败，请管理员查看详情。',
          orderstate: 777,
          failtime: failedAt,
          failreason: '申诉处理失败，请管理员查看详情。',
          failreasondesc: failureDetail || 'auto-cleanup',
          UpdateOn: failedAt,
        })
        await reconcileTdxSmsReceiveBillingByOrderId(orderDeal.id, { reason: 'execute-exception' })
      }
      return sendTdxFailure(reply, request, err, '提交失败，申诉处理未完成，请联系管理员。', 502)
    }

    const responsePayload = {
      ...sanitizeSessionResult(sessionResult, request),
      orderDeal,
      autoCleanupStarted,
      autoCleanupTaskId,
      autoCleanupCreatedTask,
      autoCleanupStatus,
      autoCleanupRawStatus: autoCleanupResult?.rawFinalStatus || autoCleanupStatus,
      autoCleanupMessage: autoCleanupRefunded
        ? TDX_REFUNDED_RECEIPT_RESULT_MESSAGE
        : (autoCleanupResult?.publicMessage || '号码申诉已提交，请等待1-2个小时更新结果'),
      autoCleanupCompleted: autoCleanupStatus === 'completed',
      autoCleanupRefunded,
      successDialog: ['completed', 'processing'].includes(autoCleanupStatus) || autoCleanupRefunded
        ? buildTdxSmsReceiveSuccessDialog({
            phone,
            orderDeal,
            autoCleanupResult,
            appealStatus: autoCleanupRefunded
              ? TDX_REFUNDED_RECEIPT_STATUS
              : (autoCleanupStatus === 'completed' ? '已处理' : '处理中'),
            resultMessage: autoCleanupStatus === 'completed'
              ? ''
              : (
                autoCleanupRefunded
                  ? TDX_REFUNDED_RECEIPT_RESULT_MESSAGE
                  : (autoCleanupResult?.publicMessage || '号码申诉已提交，请等待1-2个小时更新结果')
              ),
            fallbackTime: orderDeal?.submittedAt || nowText(),
          })
        : null,
      autoCleanupTask: autoCleanupResult?.task ? sanitizeTaskForResponse(autoCleanupResult.task, request) : null,
      autoCleanupExecution: isAdminRequest(request)
        ? stripSensitive(autoCleanupResult?.execution || null)
        : autoCleanupResult?.execution
          ? {
              ok: Boolean(autoCleanupResult.execution.ok),
              result: {
                status: autoCleanupStatus,
                rawStatus: autoCleanupResult?.rawFinalStatus || autoCleanupStatus,
                message: autoCleanupRefunded
                  ? TDX_REFUNDED_RECEIPT_RESULT_MESSAGE
                  : (autoCleanupResult?.publicMessage || '申诉已提交，等待处理。'),
              },
            }
          : null,
    }

    if (autoCleanupStatus === 'failed' && !autoCleanupRefunded) {
      return reply.code(502).send({
        success: false,
        message: isAdminRequest(request)
          ? (autoCleanupResult?.execution?.result?.message || '提交失败，申诉处理未完成。')
          : '提交失败，申诉处理未完成，请联系管理员。',
        data: responsePayload,
      })
    }

    const submitMessage = autoCleanupRefunded
      ? TDX_REFUNDED_RECEIPT_RESULT_MESSAGE
      : autoCleanupStatus === 'completed'
        ? '处理成功，请稍后到订单详情查看结果'
        : autoCleanupStatus === 'failed'
          ? '处理失败，请稍后到订单详情查看原因'
          : '申诉处理中，请稍等'
    return ok(responsePayload, submitMessage)
  })

  app.get('/tasks', { preHandler: requireModuleAccess('泰迪熊标记', 'read') }, async (request) => {
    const tasks = await listTdxTasks()
    return ok(filterTasksForList(tasks, request.query || {}).map((task) => sanitizeTaskForResponse(task, request)))
  })


  app.post('/sms-receive/tasks/:id/sync', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    const task = await findTdxTask(request.params.id)
    if (!task) return reply.code(404).send({ success: false, message: '未找到泰迪熊任务', data: null })
    if (['completed', 'failed'].includes(String(task.status || '').trim())) {
      const execution = task.result?.execution || null
      return ok({
        taskId: task.id,
        status: task.status,
        rawStatus: resolveTdxExecutionStatus(execution),
        message: resolveTdxExecutionPublicMessage(execution),
        refundSuggested: isTdxRefundSuggestedExecution(execution),
        attempts: Number(task.integrationCheckAttempts || 0),
      })
    }
    const result = await syncTdxIntegrationTask(task)
    return ok({
      taskId: result.task?.id || task.id,
      status: result.status,
      rawStatus: result.rawStatus,
      message: result.message,
      refundSuggested: result.refundSuggested,
      attempts: result.attempts,
    }, '任务状态已同步')
  })

  app.post('/tasks/bulk-send-code', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const tdxGuard = await ensureTdxMarkEnabled(reply)
    if (tdxGuard) return tdxGuard
    const bulkGuard = await ensureBulkActionEnabled(reply)
    if (bulkGuard) return bulkGuard
    const ids = normalizeIds(request.body?.ids)
    if (!ids.length) {
      return reply.code(400).send({ success: false, message: '至少需要一个任务 ID', data: null })
    }

    const updated = []
    const missingIds = []
    const skipped = []
    for (const id of ids) {
      const task = await findTdxTask(id)
      if (!task) {
        missingIds.push(id)
        continue
      }

      try {
        const precheckBlock = await resolveTdxSmsSendPrecheckBlock(task.phone, request)
        if (precheckBlock) {
          skipped.push({ id, reason: precheckBlock.response?.message || '号码预检查未通过' })
          continue
        }
        const result = await sendTdxSmsCode(task.phone, task.line || 'line1')
        if (!result.ok) {
          skipped.push({ id, reason: resolveTdxSmsSendFailureMessage(result) })
          continue
        }
        const updatedAt = nowText()
        const next = await updateTdxTask(id, {
          updatedAt,
          notes: [
            { title: 'Bulk SMS code sent', description: result.payload?.message || 'Bulk SMS send attempted for current task.', meta: updatedAt },
            ...(task.notes || buildTaskNotes(task)),
          ],
        })
        updated.push(sanitizeTaskForResponse(next, request))
      } catch (err) {
        skipped.push({ id, reason: publicTdxFailureMessage(err?.message || '发送验证码失败，请稍后重试。') })
      }
    }

    return ok({
      requestedCount: ids.length,
      updatedCount: updated.length,
      missingIds,
      skipped,
      updated,
    }, '批量短信发送已完成')
  })

  app.post('/tasks/bulk-execute', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const tdxGuard = await ensureTdxMarkEnabled(reply)
    if (tdxGuard) return tdxGuard
    const bulkGuard = await ensureBulkActionEnabled(reply)
    if (bulkGuard) return bulkGuard
    const ids = normalizeIds(request.body?.ids)
    if (!ids.length) {
      return reply.code(400).send({ success: false, message: '至少需要一个任务 ID', data: null })
    }

    const updated = []
    const missingIds = []
    const skipped = []

    for (const id of ids) {
      const task = await findTdxTask(id)
      if (!task) {
        missingIds.push(id)
        continue
      }
      if (!hasPreparedSession(task)) {
        skipped.push({ id, reason: 'missing prepared session' })
        continue
      }

      const result = await executeTdxCleanupTask(task)
      const updatedAt = nowText()
      const status = result?.result?.status || (result.ok ? 'completed' : 'processing')
      const currentStep = result?.result?.message || (result.ok ? '清理执行完成' : '等待继续处理')
      const next = await updateTdxTask(id, {
        status,
        currentStep,
        updatedAt,
        result: {
          ...(task.result || {}),
          execution: result,
        },
        notes: [
          { title: '批量执行清理', description: result.result?.message || '已发起批量清理执行。', meta: updatedAt },
          ...(task.notes || buildTaskNotes(task)),
        ],
      })
      updated.push(sanitizeTaskForResponse(next, request))
    }

    return ok({
      requestedCount: ids.length,
      updatedCount: updated.length,
      missingIds,
      skipped,
      updated,
    }, '批量执行清理已完成')
  })

  app.post('/tasks/bulk-status', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const tdxGuard = await ensureTdxMarkEnabled(reply)
    if (tdxGuard) return tdxGuard
    const bulkGuard = await ensureBulkActionEnabled(reply)
    if (bulkGuard) return bulkGuard
    const body = request.body || {}
    const status = String(body.status || '').trim().toLowerCase()
    const ids = normalizeIds(body.ids)
    const currentStep = body.currentStep ? String(body.currentStep) : ''

    if (!isAllowedTaskStatus(status)) {
      return reply.code(400).send({ success: false, message: '状态仅支持 queued、processing、completed、failed', data: null })
    }
    if (!ids.length) {
      return reply.code(400).send({ success: false, message: '至少需要一个任务 ID', data: null })
    }

    const updatedAt = nowText()
    const updated = []
    const missingIds = []
    for (const id of ids) {
      const task = await findTdxTask(id)
      if (!task) {
        missingIds.push(id)
        continue
      }
      const next = await updateTdxTask(id, {
        status,
        currentStep: currentStep || task.currentStep,
        updatedAt,
        notes: [
          { title: 'Bulk status updated', description: `Task moved to ${status}.`, meta: updatedAt },
          ...(task.notes || buildTaskNotes(task)),
        ],
      })
      updated.push(sanitizeTaskForResponse(next, request))
    }

    return ok({
      status,
      requestedCount: ids.length,
      updatedCount: updated.length,
      missingIds,
      updated,
    }, '批量状态更新已完成')
  })


  app.post('/tasks/:id/send-code', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    const task = await findTdxTask(request.params.id)
    if (!task) return reply.code(404).send({ success: false, message: '未找到泰迪熊任务', data: null })
    try {
      const precheckBlock = await resolveTdxSmsSendPrecheckBlock(task.phone, request)
      if (precheckBlock) {
        return reply.code(precheckBlock.statusCode).send(precheckBlock.response)
      }
      const result = await sendTdxSmsCode(task.phone, task.line || 'line1')
      if (!result.ok) {
        return sendTdxSmsSendFailure(reply, request, result, '验证码发送失败，请稍后重试。')
      }
      const updatedAt = nowText()
      const updated = await updateTdxTask(request.params.id, {
        updatedAt,
        orderstatenote: result.payload?.mockSmsCode || task.orderstatenote || '',
        notes: [
          { title: 'SMS code sent', description: result.payload?.message || 'SMS send attempted for current task.', meta: updatedAt },
          ...(task.notes || buildTaskNotes(task)),
        ],
      })
      return ok({ task: sanitizeTaskForResponse(updated, request), result: stripSensitive(result) }, '短信验证码已发送')
    } catch (err) {
      return sendTdxFailure(reply, request, err, '发送验证码失败，请稍后重试。', 502)
    }
  })

  app.post('/tasks/:id/execute', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    const task = await findTdxTask(request.params.id)
    if (!task) return reply.code(404).send({ success: false, message: '未找到泰迪熊任务', data: null })
    if (!hasPreparedSession(task)) {
      return reply.code(409).send({ success: false, message: '执行清理前请先准备有效的 orderId / wxCode 会话', data: null })
    }
    const result = await executeTdxCleanupTask(task)
    const updatedAt = nowText()
    const status = result?.result?.status || (result.ok ? 'completed' : 'processing')
    const currentStep = result?.result?.message || (result.ok ? '清理执行完成' : '等待继续处理')
    const updated = await updateTdxTask(request.params.id, {
      status,
      currentStep,
      updatedAt,
      result: {
        ...(task.result || {}),
        execution: result,
      },
      notes: [
        { title: 'Cleanup executed', description: result.result?.message || 'Cleanup execution attempted.', meta: updatedAt },
        ...(task.notes || buildTaskNotes(task)),
      ],
    })
    return ok(sanitizeTaskForResponse(updated, request), '泰迪熊清理已执行')
  })

  app.post('/tasks/:id/prepare-session', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    const task = await findTdxTask(request.params.id)
    if (!task) return reply.code(404).send({ success: false, message: '未找到泰迪熊任务', data: null })
    const body = request.body || {}
    if (!body.smsCode || String(body.smsCode).trim().length < 4) {
      return reply.code(400).send({ success: false, message: '准备会话前必须提供有效的短信验证码', data: null })
    }
    let result
    try {
      result = await createTdxSmsReceiveSession({
        phone: task.phone,
        smsCode: body.smsCode || '',
        rotate: Boolean(body.rotate),
        line: task.line || body.line || 'line1',
      })
    } catch (err) {
      return sendTdxFailure(reply, request, err, '登录失败，请核对手机号和验证码后重试。', 502)
    }
    const updatedAt = nowText()
    const updated = await updateTdxTask(request.params.id, {
      orderId: result.orderId || task.orderId,
      wxCode: result.wxCode || task.wxCode,
      status: result.ok ? 'processing' : 'failed',
      currentStep: result.ok ? 'Session ready, waiting for cleanup execution' : 'Session preparation failed',
      updatedAt,
      result: stripSensitive(result),
      notes: [
        { title: 'Session prepared', description: result.ok ? 'Order session and wxCode prepared successfully.' : 'Order session preparation failed.', meta: updatedAt },
        ...(task.notes || buildTaskNotes(task)),
      ],
    })
    return ok(sanitizeTaskForResponse(updated, request), '泰迪熊任务会话已准备')
  })

  app.get('/tasks/:id', { preHandler: requireModuleAccess('泰迪熊标记', 'read') }, async (request, reply) => {
    const task = await findTdxTask(request.params.id)
    if (!task) return reply.code(404).send({ success: false, message: '未找到泰迪熊任务', data: null })
    const config = await getTdxConfig()
    const executePlan = buildTdxExecutionPlan(task, config, { execution: task.result?.execution })
    return ok(sanitizeTaskForResponse({
      ...task,
      notes: task.notes || buildTaskNotes(task),
      executePlan,
      executionSummary: summarizeTdxExecutionPlan(executePlan),
    }, request))
  })

  app.patch('/tasks/:id/status', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    const task = await findTdxTask(request.params.id)
    if (!task) return reply.code(404).send({ success: false, message: '未找到泰迪熊任务', data: null })
    const body = request.body || {}
    const updatedAt = nowText()
    const updated = await updateTdxTask(request.params.id, {
      status: body.status || task.status,
      currentStep: body.currentStep || task.currentStep,
      updatedAt,
      notes: [
        { title: 'Status updated', description: `Task moved to ${body.status || task.status}.`, meta: updatedAt },
        ...(task.notes || buildTaskNotes(task)),
      ],
    })
    return ok(sanitizeTaskForResponse(updated, request), '泰迪熊任务状态已更新')
  })

  app.post('/tasks', { preHandler: async (request, reply) => {
    const { businessType = 'tdx' } = request.body || {}
    const targetModule = (businessType === 'tdx' || businessType.includes('tdx')) ? '泰迪熊标记' : '标记处理'
    const guard = requireModuleAccess(targetModule, 'create')
    return guard(request, reply)
  } }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    const { phone, smsCode, rotate = false, businessType = 'tdx', batchLabel = '', companyname = '', line = 'line1' } = request.body || {}

    // Intercept Tencent submissions
    if (businessType === 'qctengx3h') {
      try {
        const userId = Number(request.currentUser?.liveUserId || 0)
        const result = await processTencent3hCleanup(userId, phone, smsCode)
        return ok({ id: result.batchId, status: 'completed' }, '腾讯速清申诉已提交！')
      } catch (err) {
        return reply.code(400).send({ success: false, message: err.message, data: null })
      }
    }

    if (businessType === 'qctengx23d') {
      try {
        const userId = Number(request.currentUser?.liveUserId || 0)
        const result = await processTencent23dCleanup(userId, phone, smsCode)
        return ok({ id: result.batchId, status: 'queued' }, '腾讯 2-3 天清理已提交！')
      } catch (err) {
        return reply.code(400).send({ success: false, message: err.message, data: null })
      }
    }
    let session
    try {
      session = await createTdxSmsReceiveSession({ phone, smsCode, rotate, line })
    } catch (err) {
      return sendTdxFailure(reply, request, err, '登录失败，请核对手机号和验证码后重试。', 502)
    }
    const createdAt = nowText()
    const resolvedCompanyName = companyname || resolveTdxCompanyName(businessType)
    const task = await createTdxTask({
      id: `tdx-task-${Date.now()}`,
      phone,
      line,
      username: request.currentUser?.name || request.currentUser?.username || '当前账号',
      companyname: resolvedCompanyName,
      businessType,
      orderstatenote: smsCode || '',
      batchLabel: batchLabel || resolvedCompanyName,
      orderId: session.orderId,
      wxCode: session.wxCode,
      status: session.ok ? 'queued' : 'failed',
      currentStep: session.ok ? 'Session ready for upstream cleanup' : publicTdxFailureMessage(session.publicMessage || '登录失败，请核对手机号和验证码后重试。'),
      createdAt,
      updatedAt: createdAt,
      failreason: session.ok ? '' : publicTdxFailureMessage(session.publicMessage || '登录失败，请核对手机号和验证码后重试。'),
      notes: [
        { title: '任务已创建', description: `已为 ${phone} 创建泰迪熊标记任务。`, meta: createdAt },
        { title: '会话结果', description: session.ok ? '上游会话准备成功。' : '上游会话准备失败。', meta: session.ok ? '已就绪' : '失败' },
      ],
      result: stripSensitive(session),
    })
    return ok(sanitizeTaskForResponse(task, request), '泰迪熊任务已创建')
  })
}
