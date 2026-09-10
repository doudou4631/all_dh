import { ok } from '../lib/response.js'
import { requireModuleAccess } from '../lib/authGuard.js'
import { processTencent3hCleanup, processTencent23dCleanup } from '../services/tencentCleanupService.js'
import {
  allocateTdxOrderSession,
  createTdxSmsReceiveSession,
  getTdxRuntimeSnapshot,
  isHardTdxSessionFailureMessage,
  isTdxWechatPhoneLimitMessage,
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
import { buildTdxUpstreamAudit, sanitizeTdxUpstreamAudit } from '../services/tdxAuditService.js'
import { createTdxTask, findTdxTask, listTdxTasks, updateTdxTask } from '../data/tdxTaskStore.js'
import { readTdxSettings, summarizeTdxSettings, writeTdxSettings } from '../data/tdxSettingsStore.js'
import { isSystemSwitchEnabled } from '../data/systemSettingsStore.js'
import { getTdxConfig } from '../config/tdxConfig.js'
import { normalizeFailReasonDesc } from '../lib/orderFailReason.js'
import { formatChinaDateTime } from '../lib/time.js'
import { query } from '../data/liveDb.js'
import { getTdxOrderSessionUsedCount } from '../services/tdxRuntimeStore.js'

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
const TDX_LINE3_RETRY_MAX_ATTEMPTS = 2
const TDX_LINE2_MAINTENANCE_MESSAGE = '线路2暂时维护，请选择线路1。'

let tdxSessionRetryTimer = null
let tdxSessionRetryRunning = false
let tdxSessionRetryStarted = false
let tdxSessionRetryStopped = false
let tdxSessionRetryNextRunAtMs = 0
const tdxCleanupTaskCreationLocks = new Map()
const tdxLine3RetryLocks = new Map()

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
  const rows = await query(
    `SELECT Id, userid, username, orderpicinumber, createtime, successtime, queryres, orderstate
       FROM t_orderdeal
      WHERE phone = ?
        AND companycode = ?
        AND COALESCE(successtime, createtime, CreateOn, UpdateOn) >= DATE_SUB(?, INTERVAL ${safeWindowDays} DAY)
        AND (queryres = 'AppealCompleted' OR orderstate IN (6, 55, 666))
      ORDER BY COALESCE(successtime, createtime, CreateOn, UpdateOn) DESC, Id DESC
      LIMIT 1`,
    [normalizedPhone, TDX_SMS_RECEIVE_COMPANY_CODE, nowText()],
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
    `SELECT Id, orderpicinumber, orderstate
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
    const existingOrderState = Number(existing[0].orderstate || 0)
    if ([6, 55, 666, 777].includes(existingOrderState)) {
      const saved = {
        id: Number(existing[0].Id),
        orderpicinumber: existing[0].orderpicinumber || '',
        phone: normalizedPhone,
        submittedAt: createdAt,
        created: false,
        terminal: true,
      }
      const billing = await reconcileTdxSmsReceiveBillingByOrderId(saved.id, { reason: 'order-terminal-reused' })
      return { ...saved, billing }
    }
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
  const previous = Math.max(0, Number(previousAttempts || 0))
  const current = Math.max(0, Number(currentAttempts || 0))
  return isTdxSessionCooldownFailure(sessionFailure) ? previous : current
}

function shouldQueueTdxSessionRetry({ attempts = 0, maxAttempts = 1, sessionFailure = null, handled = false } = {}) {
  if (!handled) return false
  if (isTdxSessionCooldownFailure(sessionFailure)) return true
  return Number(attempts || 0) < Number(maxAttempts || 1)
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
  const line = resolveStoredTdxTaskLine(task) || 'line1'
  return {
    currentUser: {
      liveUserId: Number(task.liveUserId || task.userId || task.userid || 0),
      username,
      name: username,
    },
    body: {
      taskId: String(task.id || '').trim(),
      line,
    },
  }
}

function resolveStoredTdxTaskLine(task = {}) {
  const candidates = [
    task.line,
    task.tdxLine,
    task.retryContext?.line,
    task.result?.upstreamAudit?.line,
    task.debug?.lastSessionFailure?.line,
  ]
  for (const candidate of candidates) {
    const value = String(candidate || '').trim().toLowerCase()
    if (value === 'line1' || value === 'line2' || value === 'line3') return value
  }
  return ''
}

async function withTdxCleanupTaskCreationLock(orderDealId, work) {
  if (!orderDealId) return work()

  const key = String(orderDealId)
  const previous = tdxCleanupTaskCreationLocks.get(key) || Promise.resolve()
  let release
  const current = new Promise((resolve) => {
    release = resolve
  })
  const queue = previous.catch(() => undefined).then(() => current)
  tdxCleanupTaskCreationLocks.set(key, queue)

  await previous.catch(() => undefined)
  try {
    return await work()
  } finally {
    release()
    if (tdxCleanupTaskCreationLocks.get(key) === queue) {
      tdxCleanupTaskCreationLocks.delete(key)
    }
  }
}

async function findOrCreateTdxCleanupTask(args) {
  const orderDealId = Number(args.orderDeal?.id || 0)
  return withTdxCleanupTaskCreationLock(orderDealId, () => findOrCreateTdxCleanupTaskLocked(args, orderDealId))
}

async function findOrCreateTdxCleanupTaskLocked({ request, phone, sessionResult, smsCode, orderDeal = null }, orderDealId = 0) {
  const requestedTaskId = String(request.body?.taskId || request.body?.id || '').trim()
  const resolvedLine = String(sessionResult?.line || request.body?.line || 'line1').trim() || 'line1'
  const tasks = await listTdxTasks()
  const matchingOrderTasks = orderDealId > 0
    ? tasks.filter((item) => Number(item.orderDealId || 0) === orderDealId)
    : []
  const requestedTask = requestedTaskId ? tasks.find((item) => item.id === requestedTaskId) : null
  const task =
    (requestedTask
      && isTdxPendingTaskStatus(requestedTask.status)
      && (!orderDealId || Number(requestedTask.orderDealId || 0) === orderDealId)
      ? requestedTask
      : null) ||
    matchingOrderTasks.find((item) => isTdxPendingTaskStatus(item.status)) ||
    (!orderDealId
      ? tasks.find((item) =>
          String(item.phone).trim() === String(phone).trim()
          && String(item.line || item.tdxLine || 'line1').trim() === resolvedLine
          && isTdxPendingTaskStatus(item.status))
      : null)

  if (task) {
    const updatedAt = nowText()
    const upstreamAudit = buildTaskUpstreamAudit(task, {
      line: resolvedLine,
      orderId: sessionResult.orderId,
      wxCode: sessionResult.wxCode,
      sessionResult,
    })
    const next = await updateTdxTask(task.id, {
      line: resolvedLine,
      orderId: sessionResult.orderId,
      wxCode: sessionResult.wxCode,
      loginToken: sessionResult.login?.token || task.loginToken || '',
      status: 'processing',
      currentStep: '已关联会话，系统正在自动执行清理...',
      updatedAt,
      skipCheckAppletPhone: true,
      orderDealId: Number(orderDealId || task.orderDealId || 0),
      orderpicinumber: String(orderDeal?.orderpicinumber || task.orderpicinumber || '').trim(),
      result: {
        ...(task.result || {}),
        tokenSaved: true,
        orderId: sessionResult.orderId,
        wxCode: sessionResult.wxCode,
        upstreamAudit,
      },
      notes: [
        { title: '登录成功自动触发清理', description: '系统检测到会话已就绪，已自动关联并立即执行申诉清理。', meta: updatedAt },
        ...(task.notes || []),
      ],
    })
    return { task: next, created: false, shouldExecute: true }
  }

  const terminalTask = matchingOrderTasks.find((item) => ['completed', 'failed'].includes(String(item.status || '').trim().toLowerCase()))
  if (terminalTask) {
    const terminalStatus = String(terminalTask.status || '').trim().toLowerCase()
    return {
      task: terminalTask,
      created: false,
      shouldExecute: false,
      idempotent: true,
      message: terminalStatus === 'completed'
        ? '该订单已完成申诉，未重复提交。'
        : '该订单已有失败的申诉任务，未重复提交；请管理员手动确认后再重试。',
    }
  }

  const createdAt = nowText()
  const upstreamAudit = buildTaskUpstreamAudit({}, {
    line: resolvedLine,
    orderId: sessionResult.orderId,
    wxCode: sessionResult.wxCode,
    sessionResult,
  })
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
    orderDealId: Number(orderDealId || 0),
    orderpicinumber: String(orderDeal?.orderpicinumber || '').trim(),
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
      upstreamAudit,
    },
  })

  return { task: createdTask, created: true, shouldExecute: true }
}

function buildTdxIdempotentCleanupResult(taskResult = {}) {
  const task = taskResult.task || {}
  const finalStatus = ['completed', 'failed'].includes(String(task.status || '').trim().toLowerCase())
    ? String(task.status || '').trim().toLowerCase()
    : 'failed'
  const publicMessage = taskResult.message || (finalStatus === 'completed'
    ? '该订单已完成申诉，未重复提交。'
    : '该订单已有失败的申诉任务，未重复提交；请管理员手动确认后再重试。')
  const execution = task.result?.execution || {
    ok: finalStatus === 'completed',
    result: {
      status: finalStatus,
      message: publicMessage,
    },
  }

  return {
    task,
    execution,
    finalStatus,
    rawFinalStatus: finalStatus,
    publicMessage,
    idempotent: true,
  }
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
    ? '号码申诉已提交，请等待3-5个工作日的审核时间'
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
  const alreadySubmitted = execution?.result?.alreadySubmitted === true
    || execution?.result?.businessOutcome === 'already-submitted'
  const refundSuggested = isTdxRefundSuggestedExecution(execution)
  const failureDetail = failed ? resolveTdxExecutionFailureDetail(execution) : ''
  const publicResultMessage = resolveTdxExecutionPublicMessage(execution, lifecycle)
  return {
    submitres: task?.orderId || execution?.result?.orderId || '',
    queryres: alreadySubmitted ? 'AppealAlreadySubmitted' : completed ? 'AppealCompleted' : refundSuggested ? 'AppealRefunded' : failed ? 'AppealFailed' : 'AppealSubmitted',
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
    const upstreamAudit = buildTaskUpstreamAudit(task, {
      execution,
      orderId: task?.orderId || execution?.result?.orderId || '',
      wxCode: task?.wxCode || execution?.result?.wxCode || '',
    })
    updatedTask = await updateTdxTask(taskId, {
      status: finalStatus,
      currentStep,
      updatedAt,
      result: {
        ...(task.result || {}),
        execution,
        upstreamAudit,
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
  const date = String(query.date || '').trim()
  const status = String(query.status || '').trim().toLowerCase()
  const keyword = String(query.q || '').trim().toLowerCase()
  return tasks.filter((task) => {
    const audit = task.result?.upstreamAudit || {}
    const capturedAt = String(audit.capturedAt || task.updatedAt || task.createdAt || '').trim()
    const capturedDate = tdxAuditDateKey(capturedAt)
    const matchesDate = !date || capturedDate === date
    const matchesStatus = !status || status === 'all' || String(task.status || '').toLowerCase() === status
    const haystack = [
      task.id,
      task.phone,
      task.orderId,
      task.orderDealId,
      task.orderpicinumber,
      task.currentStep,
      task.batchLabel,
      task.line,
      audit.orderId,
      audit.provider,
      audit.upstreamMessage,
    ].filter(Boolean).join(' ').toLowerCase()
    const matchesKeyword = !keyword || haystack.includes(keyword)
    return matchesDate && matchesStatus && matchesKeyword
  })
}

// Keep the audit date semantics identical to the admin panel. Most task
// timestamps are stored as China-local `YYYY-MM-DD HH:mm:ss` strings, but
// upstream payloads may contain ISO timestamps with Z/offsets. Those must be
// converted to Asia/Shanghai before comparing a requested calendar date.
function tdxAuditDateKey(value) {
  const raw = String(value || '').trim()
  if (!raw) return ''
  const hasTimezone = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(raw)
  if (!hasTimezone) return raw.match(/^(\d{4}-\d{2}-\d{2})/)?.[1] || ''

  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return raw.match(/^(\d{4}-\d{2}-\d{2})/)?.[1] || ''
  const parts = Object.fromEntries(new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(date).map((part) => [part.type, part.value]))
  return `${parts.year}-${parts.month}-${parts.day}`
}

const TDX_AUDIT_RECOVERY_START = '2026-08-31 00:00:00'

function firstTaskDate(...values) {
  return values
    .map((value) => String(value || '').trim())
    .find(Boolean) || ''
}

function inferRecoveredTdxLine(order = {}) {
  const submitres = String(order.submitres || '').trim()
  return /^\d{1,12}$/.test(submitres) ? 'line2' : 'line1'
}

function buildRecoveredTdxAuditTask(order = {}) {
  const line = inferRecoveredTdxLine(order)
  const orderState = Number(order.orderstate || 0)
  const queryres = String(order.queryres || '').trim()
  const status = orderState === 777 || ['AppealFailed', 'AutoCheckRefunded'].includes(queryres)
    ? 'failed'
    : orderState === 6 || [55, 666].includes(orderState) || ['AppealCompleted', 'AutoCheckCompleted'].includes(queryres)
      ? 'completed'
      : orderState === 33
        ? 'processing'
        : 'processing'
  const createdAt = firstTaskDate(order.createtime, order.CreateOn, order.UpdateOn)
  const updatedAt = firstTaskDate(order.UpdateOn, order.successtime, order.failtime, order.createtime, order.CreateOn)
  const statusLabel = status === 'completed'
    ? 'Database history: appeal completed'
    : status === 'failed'
      ? 'Database history: appeal failed or refunded'
      : 'Database history: processing'
  const upstreamMessage = firstTaskDate(order.dealquxiaores, order.queryres, order.failreason, order.failreasondesc).slice(0, 500)

  return {
    id: `tdx-db-recovered-${Number(order.Id || 0)}`,
    source: 'database-recovered',
    readOnly: true,
    recoveredFrom: 't_orderdeal',
    phone: String(order.phone || '').trim(),
    line,
    username: String(order.username || '').trim(),
    companyname: String(order.companyname || '').trim() || TDX_SMS_RECEIVE_COMPANY_NAME,
    businessType: 'tdx',
    batchLabel: TDX_SMS_RECEIVE_COMPANY_NAME,
    orderId: String(order.submitres || '').trim(),
    orderDealId: Number(order.Id || 0),
    orderpicinumber: String(order.orderpicinumber || '').trim(),
    status,
    currentStep: statusLabel,
    createdAt,
    updatedAt,
    failreason: String(order.failreason || order.failreasondesc || '').trim(),
    notes: [
      {
        title: 'Database history recovery',
        description: 'Recovered from t_orderdeal after the task store lost its mapping.',
        meta: updatedAt || createdAt,
      },
    ],
    result: {
      upstreamAudit: {
        provider: line === 'line2' ? 'Dengfeng' : 'QMF',
        line,
        orderId: String(order.submitres || '').trim(),
        wxid: '',
        failureStep: '',
        qmfStage: '',
        upstreamCode: null,
        upstreamMessage,
        upstreamPayload: null,
        usedCount: null,
        reused: null,
        capturedAt: updatedAt || createdAt,
      },
    },
  }
}

function taskAuditOrderId(task = {}) {
  return String(
    task.result?.upstreamAudit?.orderId
      || task.orderId
      || task.result?.orderId
      || task.result?.execution?.result?.orderId
      || '',
  ).trim()
}

function hydrateMissingTdxAuditUsage(tasks = []) {
  const phonesByOrder = new Map()
  const countByTaskId = new Map()
  const chronologicalTasks = [...tasks].sort((left, right) => {
    const createdCompare = String(left.createdAt || '').localeCompare(String(right.createdAt || ''))
    return createdCompare || String(left.id || '').localeCompare(String(right.id || ''))
  })

  for (const task of chronologicalTasks) {
    const orderId = taskAuditOrderId(task)
    const phone = String(task.phone || '').trim()
    if (!orderId || !phone) continue
    if (!phonesByOrder.has(orderId)) phonesByOrder.set(orderId, new Set())
    const phones = phonesByOrder.get(orderId)
    if (!phones.has(phone)) phones.add(phone)
    countByTaskId.set(task.id, phones.size)
  }

  return tasks.map((task) => {
    const audit = task.result?.upstreamAudit || task.upstreamAudit || null
    const orderId = taskAuditOrderId(task) || String(audit?.orderId || '').trim()
    if (!orderId || (audit?.usedCount !== null && audit?.usedCount !== undefined && String(audit.usedCount).trim() !== '')) {
      return task
    }

    // Persisted audit data is authoritative for new tasks. For older tasks,
    // use the chronological phone sequence so each order shows 1, 2, 3
    // instead of the session's final cumulative count on every row.
    const sequenceCount = countByTaskId.get(task.id)
    const fallbackCount = sequenceCount || (getTdxOrderSessionUsedCount(orderId) ?? 0)
    if (!fallbackCount) return task

    return {
      ...task,
      result: {
        ...(task.result && typeof task.result === 'object' ? task.result : {}),
        upstreamAudit: {
          ...(audit && typeof audit === 'object' ? audit : {}),
          orderId: audit?.orderId || orderId,
          usedCount: fallbackCount,
        },
      },
    }
  })
}

async function listTdxAuditTasks() {
  const tasks = await listTdxTasks()
  const knownOrderDealIds = new Set(
    tasks
      .map((task) => Number(task.orderDealId || 0))
      .filter(Boolean),
  )

  try {
    const rows = await query(
      `SELECT Id, username, phone, companyname, submitres, queryres, dealquxiaores,
              orderstate, orderpicinumber, createtime, successtime, failtime,
              failreason, failreasondesc, UpdateOn, CreateOn
         FROM t_orderdeal
        WHERE companycode = ?
          AND COALESCE(createtime, CreateOn, UpdateOn) >= ?
        ORDER BY COALESCE(createtime, CreateOn, UpdateOn) DESC, Id DESC`,
      [TDX_SMS_RECEIVE_COMPANY_CODE, TDX_AUDIT_RECOVERY_START],
    )
    const recovered = rows
      .filter((row) => !knownOrderDealIds.has(Number(row.Id || 0)))
      .map(buildRecoveredTdxAuditTask)
    return hydrateMissingTdxAuditUsage(
      [...tasks, ...recovered].sort((left, right) => String(right.createdAt || '').localeCompare(String(left.createdAt || ''))),
    )
  } catch (error) {
    console.error('[tdx-audit] database recovery query failed:', error instanceof Error ? error.message : String(error))
    return hydrateMissingTdxAuditUsage(tasks)
  }
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

function isLine3RetryTask(task = {}) {
  return String(task?.line || '').trim().toLowerCase() === 'line3' && Boolean(task?.line3Retry)
}

function isTdxLine3AuditRetryEligible(task = {}) {
  const audit = task?.result?.upstreamAudit || {}
  const sourceLine = resolveStoredTdxTaskLine(task) || 'line1'
  const status = String(task?.status || '').trim().toLowerCase()
  if (sourceLine !== 'line1' || status !== 'failed') return false

  const failureText = [
    audit.upstreamMessage,
    audit.upstreamPayload?.message,
    task?.failreason,
    task?.result?.message,
  ]
    .map((value) => String(value || '').trim())
    .filter(Boolean)
    .join(' ')
  if (isHardTdxSessionFailureMessage(failureText) || isTdxWechatPhoneLimitMessage(failureText)) return false

  return Boolean(String(audit.orderId || '').trim() && String(audit.wxCode || '').trim())
}

function resolveTdxLine3ReusableSession(task = {}) {
  const audit = task?.result?.upstreamAudit || {}
  const taskOrderId = String(task?.orderId || '').trim()
  const taskWxCode = String(task?.wxCode || '').trim()
  const auditEligible = isTdxLine3AuditRetryEligible(task)
  const orderId = taskOrderId || (auditEligible ? String(audit.orderId || '').trim() : '')
  const wxCode = taskWxCode || (auditEligible ? String(audit.wxCode || '').trim() : '')
  if (!orderId || !wxCode) return null
  return {
    orderId,
    wxCode,
    fromAudit: !taskOrderId || !taskWxCode,
  }
}

function isLine3RetryableOrderDeal(order = {}) {
  const state = Number(order.orderstate || 0)
  const queryres = String(order.queryres || '').trim().toLowerCase()
  const text = [order.dealquxiaores, order.failreason, order.failreasondesc]
    .map((value) => String(value || '').trim())
    .join(' ')
  return state === 777
    || queryres === 'autocheckrefunded'
    || queryres === 'appealfailed'
    || /退款|失败|refunded|failed/i.test(text)
}

async function loadTdxLine3RetryOrderDeal(task = {}) {
  const orderDealId = Number(task.orderDealId || 0)
  if (!orderDealId) return null
  const rows = await query(
    `SELECT Id, userid, username, phone, companycode, companyname, submitres, queryres, dealquxiaores,
            orderstate, failreason, failreasondesc, orderpicinumber, createtime, failtime, UpdateOn
       FROM t_orderdeal
      WHERE Id = ?
      LIMIT 1`,
    [orderDealId],
  )
  return rows[0] || null
}

function canRequestUseTdxLine3Order(request, orderDeal = {}) {
  // The server-to-server integration token carries a super-admin role for
  // module access, but it must still be scoped to its configured service user
  // when reusing a failed order.
  if (request?.tdxIntegration) {
    const integrationUserId = Number(request.currentUser?.liveUserId || 0)
    return integrationUserId > 0 && Number(orderDeal.userid || 0) === integrationUserId
  }
  if (isAdminRequest(request)) return true
  const currentUserId = Number(request.currentUser?.liveUserId || 0)
  return currentUserId > 0 && Number(orderDeal.userid || 0) === currentUserId
}

function isUnusedTdxLine3SourceTask(task = {}, retrySourceIds = new Set()) {
  const sourceLine = resolveStoredTdxTaskLine(task) || 'line1'
  const retryState = line3RetryStatus(task)
  const retryableState = isLine3RetryableState(task)
  const reusableSession = resolveTdxLine3ReusableSession(task)
  return !isLine3RetryTask(task)
    && sourceLine === 'line1'
    && Boolean(task.phone && reusableSession?.orderId && reusableSession?.wxCode && Number(task.orderDealId || 0) > 0)
    && (!task.line3RetryAttemptedAt || retryableState)
    && (!['processing', 'completed', 'failed'].includes(retryState) || retryableState)
    && !retrySourceIds.has(String(task.id || '').trim())
}

async function findTdxLine3SourceCandidates(request, phone = '') {
  const normalizedPhone = String(phone || '').trim()
  if (!/^1\d{10}$/.test(normalizedPhone)) return []

  const tasks = await listTdxTasks()
  const retrySourceIds = new Set(
    tasks
      .filter((item) => isLine3RetryTask(item))
      .map((item) => String(item.retryOf || '').trim())
      .filter(Boolean),
  )
  const candidates = []

  for (const task of tasks) {
    if (String(task.phone || '').trim() !== normalizedPhone) continue
    if (!isUnusedTdxLine3SourceTask(task, retrySourceIds)) continue

    const orderDeal = await loadTdxLine3RetryOrderDeal(task)
    const reusableSession = resolveTdxLine3ReusableSession(task)
    if (!reusableSession) continue
    if (!orderDeal) continue
    if (String(orderDeal.companycode || '').trim() !== TDX_SMS_RECEIVE_COMPANY_CODE) continue
    if (!canRequestUseTdxLine3Order(request, orderDeal)) continue
    if (String(orderDeal.phone || '').trim() !== normalizedPhone) continue
    if (String(orderDeal.submitres || '').trim() && String(orderDeal.submitres || '').trim() !== reusableSession.orderId) continue
    if (!isLine3RetryableOrderDeal(orderDeal) && String(task.status || '').trim().toLowerCase() !== 'failed') continue

    candidates.push({ task, orderDeal })
  }

  return candidates.sort((left, right) => {
    const leftTime = String(left.orderDeal?.failtime || left.task?.updatedAt || left.task?.createdAt || '')
    const rightTime = String(right.orderDeal?.failtime || right.task?.updatedAt || right.task?.createdAt || '')
    return rightTime.localeCompare(leftTime)
  })
}

function summarizeTdxLine3SourceCandidate(candidate = {}) {
  const task = candidate.task || {}
  const orderDeal = candidate.orderDeal || {}
  return {
    taskId: String(task.id || '').trim(),
    localOrderNumber: String(task.orderpicinumber || orderDeal.orderpicinumber || '').trim(),
    status: String(task.status || '').trim(),
    failedAt: String(orderDeal.failtime || task.updatedAt || '').trim(),
    createdAt: String(task.createdAt || orderDeal.createtime || '').trim(),
  }
}

function line3RetryStatus(task = {}) {
  return String(task?.line3RetryState || '').trim().toLowerCase()
}

function line3RetryCount(task = {}) {
  const count = Number.parseInt(String(task?.line3RetryCount ?? 0), 10)
  return Number.isFinite(count) && count >= 0 ? count : 0
}

function isLine3RetryableState(task = {}) {
  return line3RetryStatus(task) === 'retryable-failed'
    && line3RetryCount(task) < TDX_LINE3_RETRY_MAX_ATTEMPTS
}

function isLine3RetryableFailure(detail = '', failureStep = '') {
  const text = String(detail || '').trim()
  const step = String(failureStep || '').trim().toLowerCase()
  if (!text) return false
  if (/test[-\s]?forced|temporary test failure|原会话保留|测试模拟/i.test(text)) return true
  if (isTdxSmsCodeFailureMessage(text) || isTdxTransientNetworkFailureMessage(text)) return true
  return step === 'login-sms' && /(?:timeout|timed out|网络|连接|socket|fetch failed|aborted)/i.test(text)
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

function buildTaskUpstreamAudit(task = {}, {
  line = '',
  orderId = '',
  wxCode = '',
  wxid = '',
  sessionResult = null,
  execution = null,
  failure = null,
  payload = null,
  failureStep = '',
  message = '',
} = {}) {
  return buildTdxUpstreamAudit({
    line: line || task.line || task.tdxLine || 'line1',
    orderId: orderId || task.orderId || task.submitres || '',
    wxCode: wxCode || task.wxCode || '',
    wxid,
    sessionResult,
    execution,
    failure,
    payload,
    failureStep,
    message,
    existing: task.result?.upstreamAudit || null,
  })
}

function normalizePublicTdxFailureText(value = '') {
  const text = String(value || '').trim()
  if (text.includes('不同的微信绑定') || text.includes('微信绑定不一致')) {
    return '线路3复用失败：上游微信绑定不一致，本次未提交'
  }
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
    provider: result?.line === 'line2' ? 'Dengfeng' : 'QMF',
    line: result?.line || '',
    orderId: admin ? result.orderId || '' : '',
    upstreamOrderId: admin ? result.orderId || '' : '',
    wxCode: admin ? result.wxCode || '' : '',
    wxid: admin ? result.wxid || '' : '',
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
  safe.line3ReuseAvailable = Boolean(resolveTdxLine3ReusableSession(task))

  if (safe.source === 'database-recovered') {
    safe.readOnly = true
    safe.currentStep = safe.currentStep || 'Database history: read-only audit record'
  }

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

  const storedAudit = task.result?.upstreamAudit || task.upstreamAudit || null
  const hasAuditContext = Boolean(
    storedAudit
      || task.orderId
      || task.wxCode
      || task.result?.orderId
      || task.result?.wxCode
      || task.result?.execution
      || task.sessionRetry
      || task.failreason,
  )
  if (hasAuditContext) {
    const executionResult = task.result?.execution?.result || {}
    const failureStep = storedAudit?.failureStep
      || executionResult.failureStep
      || task.result?.failureStep
      || task.sessionRetry?.lastFailureStep
      || ''
    const failureMessage = storedAudit?.upstreamMessage
      || task.sessionRetry?.lastError
      || executionResult.message
      || task.result?.message
      || task.failreason
      || ''
    const enrichedAudit = buildTdxUpstreamAudit({
      line: storedAudit?.line || task.line || task.tdxLine || 'line1',
      orderId: storedAudit?.orderId || task.orderId || task.result?.orderId || task.result?.execution?.result?.orderId || '',
      wxCode: storedAudit?.wxCode || task.wxCode || task.result?.wxCode || '',
      sessionResult: task.result || null,
      execution: task.result?.execution || null,
      failureStep,
      message: failureMessage,
      payload: storedAudit?.upstreamPayload || null,
      // Older tasks may not have an audit timestamp yet. Keep their audit
      // date tied to the task lifecycle instead of assigning the current time
      // during every list request, which would break date filtering in the UI.
      capturedAt: storedAudit?.capturedAt || task.updatedAt || task.createdAt || '',
      existing: storedAudit,
    })
    safe.result = {
      ...(safe.result && typeof safe.result === 'object' ? safe.result : {}),
      upstreamAudit: sanitizeTdxUpstreamAudit(enrichedAudit, { admin }),
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
  const publicMessage = isTdxSessionConfigFailureDetail(actual)
    ? '\u7533\u8bc9\u901a\u9053\u672a\u914d\u7f6e\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458\u3002'
    : maskActual
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
  const resolvedLine = String(
    sessionFailure?.line
    || task?.line
    || task?.tdxLine
    || request.body?.line
    || request.body?.tdxLine
    || 'line1',
  ).trim().toLowerCase() === 'line2' ? 'line2' : 'line1'
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
  const upstreamAudit = buildTaskUpstreamAudit(task || {}, {
    line: resolvedLine,
    orderId: sessionFailure?.orderId || sessionFailure?.qmfOrderId || task?.orderId || '',
    wxCode: sessionFailure?.wxCode || task?.wxCode || '',
    failure: sessionFailure,
    failureStep: resolveTdxSessionFailureStep(sessionFailure),
    message: resolveTdxSessionFailureDetail(sessionFailure),
  })

  if (task) {
    const updated = await updateTdxTask(task.id, {
      taskType: TDX_SMS_RECEIVE_SESSION_RETRY_TASK_TYPE,
      flow: 'sms-receive',
      liveUserId: userId,
      username: request.currentUser?.name || request.currentUser?.username || task.username || '当前账号',
      phone,
      line: resolvedLine,
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
        upstreamAudit,
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
    liveUserId: userId,
    phone,
    line: resolvedLine,
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
      upstreamAudit,
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
  const attempts = Number(task.sessionRetry?.attempts || 0)
  const maxAttempts = Number(task.sessionRetry?.maxAttempts || resolveTdxSessionRetryMaxAttempts())
  const upstreamAudit = buildTaskUpstreamAudit(task, {
    failureStep,
    message: detail,
  })
  const updatedTask = await updateTdxTask(task.id, {
    status: 'failed',
    currentStep: '后台自动处理失败，请管理员查看详情。',
    updatedAt,
    failreason: '申诉处理失败，请管理员查看详情。',
    result: {
      status: 'failed',
      message: '申诉处理失败，请管理员查看详情。',
      sessionRetry: true,
      upstreamAudit,
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
  const attempts = previousAttempts + 1
  const maxAttempts = Number(task.sessionRetry?.maxAttempts || resolveTdxSessionRetryMaxAttempts())
  let qmfNewOrderOpenedInFlow = Boolean(task.sessionRetry?.qmfNewOrderOpened)
  const storedLine = resolveStoredTdxTaskLine(task)
  if (isTdxLine2(storedLine) && !(await isTdxLineAvailable(storedLine))) {
    await updateTdxTask(task.id, {
      status: 'paused',
      currentStep: '线路2暂时维护，后台自动重试已暂停。',
      updatedAt,
      sessionRetry: {
        ...(task.sessionRetry || {}),
        status: 'paused',
        nextRetryAtMs: 0,
        lastError: TDX_LINE2_MAINTENANCE_MESSAGE,
        lastFailureStep: 'line-maintenance',
      },
      notes: [
        { title: '线路2维护暂停', description: '线路2暂时维护，后台自动重试已暂停。', meta: updatedAt },
        ...(task.notes || []),
      ],
    })
    return
  }
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
  const retryLine = resolveStoredTdxTaskLine(runningTask)
  if (isTdxSessionRetryTask(runningTask) && !retryLine) {
    await markTdxSessionRetryTaskFailed(
      runningTask,
      '后台任务缺少原始线路信息，已停止处理，避免误切换到其他线路。',
      'missing-line',
      { qmfNewOrderOpened: qmfNewOrderOpenedInFlow },
    )
    return
  }
  const taskRequest = buildTdxTaskRequest(runningTask)
  const saveLoginTokenRetryContext = isTdxSaveLoginTokenRetryContext(runningTask.retryContext)
    ? runningTask.retryContext
    : null

  const balanceGuard = await ensureTdxSmsReceiveRequestBalance(taskRequest, {
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
        line: saveLoginTokenRetryContext.line || retryLine || 'line1',
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
        line: retryLine || 'line1',
      })
    }
  } catch (err) {
    qmfNewOrderOpenedInFlow = qmfNewOrderOpenedInFlow || Boolean(err?.qmfNewOrderOpened)
    const detail = resolveTdxSessionFailureDetail(err)
    const handled = isTdxBackendHandledSessionError(err)
    if (shouldQueueTdxSessionRetry({ attempts, maxAttempts, sessionFailure: err, handled })) {
      const retryAtMs = Date.now() + resolveTdxSessionRetryDelayMs(err, resolveTdxSessionRetryIntervalMs())
      const queuedAttempts = resolveTdxSessionQueuedAttempts({ previousAttempts, currentAttempts: attempts, sessionFailure: err })
      const upstreamAudit = buildTaskUpstreamAudit(runningTask, {
        line: err?.line || retryLine || 'line1',
        orderId: err?.orderId || err?.qmfOrderId || runningTask.orderId || '',
        wxCode: err?.wxCode || runningTask.wxCode || '',
        failure: err,
        failureStep: 'create-session-exception',
        message: detail,
      })
      await updateTdxTask(runningTask.id, {
        status: 'processing',
        line: retryLine,
        currentStep: TDX_SMS_RECEIVE_PENDING_STEP,
        updatedAt: nowText(),
        result: {
          status: 'processing',
          message: TDX_SMS_RECEIVE_PENDING_MESSAGE,
          sessionRetry: true,
          upstreamAudit,
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
  const sessionLine = String(sessionResult?.line || '').trim().toLowerCase()
  if (retryLine && sessionLine && sessionLine !== retryLine) {
    await markTdxSessionRetryTaskFailed(
      runningTask,
      `上游返回线路 ${sessionLine}，与任务原始线路 ${retryLine} 不一致，已停止处理。`,
      'line-mismatch',
      { qmfNewOrderOpened: qmfNewOrderOpenedInFlow },
    )
    return
  }
  if (!sessionResult?.ok) {
    const detail = resolveTdxSessionFailureDetail(sessionResult)
    const failureStep = resolveTdxSessionFailureStep(sessionResult) || 'session-retry'
    const handled = isTdxBackendHandledSessionFailure(sessionResult)
    if (shouldQueueTdxSessionRetry({ attempts, maxAttempts, sessionFailure: sessionResult, handled })) {
      const retryAtMs = Date.now() + resolveTdxSessionRetryDelayMs(sessionResult, resolveTdxSessionRetryIntervalMs())
      const queuedAttempts = resolveTdxSessionQueuedAttempts({ previousAttempts, currentAttempts: attempts, sessionFailure: sessionResult })
      const upstreamAudit = buildTaskUpstreamAudit(runningTask, {
        line: sessionResult?.line || retryLine || 'line1',
        orderId: sessionResult?.orderId || runningTask.orderId || '',
        wxCode: sessionResult?.wxCode || runningTask.wxCode || '',
        sessionResult,
        failureStep,
        message: detail,
      })
      await updateTdxTask(runningTask.id, {
        status: 'processing',
        line: retryLine,
        currentStep: TDX_SMS_RECEIVE_PENDING_STEP,
        updatedAt: nowText(),
        result: {
          status: 'processing',
          message: TDX_SMS_RECEIVE_PENDING_MESSAGE,
          sessionRetry: true,
          upstreamAudit,
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
    const orderDeal = await saveTdxSmsReceiveOrderDeal({
      request: taskRequest,
      phone,
      smsCode,
      sessionResult,
    })
    if (isTdxSmsReceiveInsufficientBalanceResult(orderDeal?.billing)) {
      await markTdxOrderDealInsufficientBalance(orderDeal.id, orderDeal.billing)
      await updateTdxTask(runningTask.id, {
        status: 'failed',
        line: retryLine,
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
      orderDeal,
    })
    const autoCleanupResult = taskResult.shouldExecute
      ? await executeAndPersistTdxSmsReceiveCleanup({
          request: taskRequest,
          task: taskResult.task,
          orderDeal,
        })
      : buildTdxIdempotentCleanupResult(taskResult)
    if (taskResult.shouldExecute && taskResult.task?.id) {
      await updateTdxTask(taskResult.task.id, {
        line: retryLine,
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
    if (taskResult.shouldExecute && autoCleanupResult?.finalStatus === 'failed' && Number(orderDeal?.id || 0)) {
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

function isTdxLine2(line = 'line1') {
  return String(line || 'line1').trim().toLowerCase() === 'line2'
}

function isTdxLine3(line = 'line1') {
  return String(line || 'line1').trim().toLowerCase() === 'line3'
}

function rejectDirectTdxLine3(reply) {
  return reply.code(409).send({
    success: false,
    message: '线路3只能从已有失败任务复用原订单和原微信，不能直接新建订单或登录。',
    data: null,
  })
}

async function isTdxLineAvailable(line = 'line1') {
  if (!isTdxLine2(line)) return true
  const config = await getTdxConfig()
  return !config.line2Maintenance
}

async function ensureTdxLineAvailable(reply, line = 'line1') {
  if (await isTdxLineAvailable(line)) return null
  return reply.code(503).send({ success: false, message: TDX_LINE2_MAINTENANCE_MESSAGE, data: null })
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
    return ok({ ...summary, line2Maintenance: Boolean(merged.line2Maintenance), liveValidationErrors: validateLiveTdxConfig(merged) })
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
    return ok({ ...summary, line2Maintenance: Boolean(merged.line2Maintenance), liveValidationErrors: validateLiveTdxConfig(merged) }, '泰迪熊设置已更新')
  })

  app.get('/runtime', { preHandler: requireModuleAccess('泰迪熊标记', 'read') }, async () => ok(await getTdxRuntimeSnapshot()))

  app.get('/health', { preHandler: requireModuleAccess('泰迪熊标记', 'read') }, async () => ok(await runTdxHealthCheck()))
  app.post('/runtime/rotate', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    const line = request.body?.line || 'line1'
    if (isTdxLine3(line)) return rejectDirectTdxLine3(reply)
    const lineGuard = await ensureTdxLineAvailable(reply, line)
    if (lineGuard) return lineGuard
    return ok(await rotateTdxOrderSession(line), '泰迪熊运行时已轮换')
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
    const { phone, line = 'line1', businessType = 'tdx', sourceTaskId = '', taskId = '' } = request.body || {}
    if (businessType.includes('tengx')) {
      return ok({ ok: true, message: '请在您手机的腾讯申诉页面获取验证码，并在下方输入提交' }, '请手动在手机上获取腾讯验证码')
    }
    if (isTdxLine3(line)) {
      const normalizedPhone = String(phone || '').trim()
      if (!/^1\d{10}$/.test(normalizedPhone)) {
        return reply.code(400).send({ success: false, message: '请输入正确的手机号码', data: null })
      }
      const candidates = await findTdxLine3SourceCandidates(request, normalizedPhone)
      const requestedTaskId = String(sourceTaskId || taskId || '').trim()
      const selected = requestedTaskId
        ? candidates.find((candidate) => candidate.task?.id === requestedTaskId)
        : candidates.length === 1 ? candidates[0] : null
      if (requestedTaskId && !selected) {
        return reply.code(409).send({
          success: false,
          message: '所选失败订单不可复用，请重新选择。',
          data: { candidates: candidates.map(summarizeTdxLine3SourceCandidate) },
        })
      }
      if (!selected) {
        return reply.code(409).send({
          success: false,
          message: candidates.length > 1 ? '找到多个可复用失败订单，请选择本地订单批次。' : '当前账号下没有可复用的线路1失败订单。',
          data: { candidates: candidates.map(summarizeTdxLine3SourceCandidate) },
        })
      }
      try {
        const result = await sendTdxSmsCode(normalizedPhone, 'line1')
        if (!result.ok) {
          return sendTdxSmsSendFailure(reply, request, result, '验证码发送失败，请稍后重试。')
        }
        const updatedAt = nowText()
        const updatedSource = await updateTdxTask(selected.task.id, {
          updatedAt,
          notes: [
            { title: '线路3验证码已发送', description: '已按当前账号和手机号复用线路1失败订单，等待提交新的短信验证码。', meta: updatedAt },
            ...(selected.task.notes || buildTaskNotes(selected.task)),
          ],
        })
        return ok({
          ok: true,
          mode: result.mode,
          line: 'line3',
          sourceTask: sanitizeTaskForResponse(updatedSource, request),
          candidate: summarizeTdxLine3SourceCandidate(selected),
        }, '短信验证码已发起发送')
      } catch (err) {
        return sendTdxFailure(reply, request, err, '发送验证码失败，请稍后重试。', 502)
      }
    }
    const lineGuard = await ensureTdxLineAvailable(reply, line)
    if (lineGuard) return lineGuard
    try {
      const precheckBlock = await resolveTdxSmsSendPrecheckBlock(phone, request)
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

  app.get('/sms-receive/line3-candidates', { preHandler: requireModuleAccess('泰迪熊标记', 'read') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    const phone = String(request.query?.phone || '').trim()
    if (!/^1\d{10}$/.test(phone)) {
      return reply.code(400).send({ success: false, message: '请输入正确的手机号码', data: null })
    }
    const candidates = await findTdxLine3SourceCandidates(request, phone)
    return ok({ candidates: candidates.map(summarizeTdxLine3SourceCandidate) })
  })

  app.post('/sms-receive/retry-line3', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard

    const phone = String(request.body?.phone || '').trim()
    if (!/^1\d{10}$/.test(phone)) {
      return reply.code(400).send({ success: false, message: '请输入正确的手机号码', data: null })
    }

    const candidates = await findTdxLine3SourceCandidates(request, phone)
    const requestedTaskId = String(request.body?.sourceTaskId || request.body?.taskId || '').trim()
    const selected = requestedTaskId
      ? candidates.find((candidate) => candidate.task?.id === requestedTaskId)
      : candidates.length === 1 ? candidates[0] : null
    const sourceTask = selected?.task
    if (requestedTaskId && !selected) {
      return reply.code(409).send({
        success: false,
        message: '所选失败订单不可复用，请重新选择。',
        data: { candidates: candidates.map(summarizeTdxLine3SourceCandidate) },
      })
    }
    if (!sourceTask?.id) {
      return reply.code(409).send({
        success: false,
        message: candidates.length > 1 ? '找到多个可复用失败订单，请选择本地订单批次。' : '当前账号下没有可复用的线路1失败订单。',
        data: { candidates: candidates.map(summarizeTdxLine3SourceCandidate) },
      })
    }

    // Reuse the existing task-id handler so all validation, locking, execution,
    // billing, and response behavior stays identical for both entry points.
    const injected = await app.inject({
      method: 'POST',
      url: `/api/tdx/tasks/${encodeURIComponent(String(sourceTask.id))}/retry-line3`,
      headers: {
        authorization: request.headers.authorization || '',
        'x-tdx-integration-token': request.headers['x-tdx-integration-token'] || '',
        'content-type': 'application/json',
      },
      payload: { smsCode: request.body?.smsCode || '' },
    })
    let payload
    try {
      payload = injected.json()
    } catch {
      payload = { success: false, message: '线路3复用提交失败', data: null }
    }
    return reply.code(injected.statusCode).send(payload)
  })

  app.post('/sms-receive/precheck', { preHandler: requireModuleAccess('泰迪熊标记', 'read') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    const { phone, line = 'line1' } = request.body || {}
    if (!/^1\d{10}$/.test(String(phone || '').trim())) {
      return reply.code(400).send({ success: false, message: '请输入正确的手机号', data: null })
    }
    const lineGuard = await ensureTdxLineAvailable(reply, line)
    if (lineGuard) return lineGuard
    if (isTdxLine3(line)) return rejectDirectTdxLine3(reply)
    const recentAppeal = await findRecentLocalSuccessfulTdxAppeal(phone)
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
    const lineGuard = await ensureTdxLineAvailable(reply, line)
    if (lineGuard) return lineGuard
    if (isTdxLine3(line)) return rejectDirectTdxLine3(reply)
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
    const lineGuard = await ensureTdxLineAvailable(reply, line)
    if (lineGuard) return lineGuard
    if (isTdxLine3(line)) return rejectDirectTdxLine3(reply)
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
    try {
      if (!request.tdxIntegration) {
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
      }
    } catch (err) {
      return sendTdxFailure(reply, request, err, '订单记录保存失败，请联系管理员。', 500)
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
        orderDeal,
      })
      autoCleanupStarted = Boolean(taskResult.shouldExecute)
      autoCleanupCreatedTask = Boolean(taskResult.created)
      autoCleanupTaskId = taskResult.task?.id || ''
      autoCleanupResult = taskResult.shouldExecute
        ? await executeAndPersistTdxSmsReceiveCleanup({
            request,
            task: taskResult.task,
            orderDeal,
          })
        : buildTdxIdempotentCleanupResult(taskResult)
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
        : (autoCleanupResult?.publicMessage || '号码申诉已提交，请等待3-5个工作日的审核时间'),
      autoCleanupCompleted: autoCleanupStatus === 'completed',
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
                  : (autoCleanupResult?.publicMessage || '号码申诉已提交，请等待3-5个工作日的审核时间')
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
      return reply.code(autoCleanupResult?.idempotent ? 409 : 502).send({
        success: false,
        message: autoCleanupResult?.idempotent
          ? autoCleanupResult.publicMessage
          : (isAdminRequest(request)
            ? (autoCleanupResult?.execution?.result?.message || '提交失败，申诉处理未完成。')
            : '提交失败，申诉处理未完成，请联系管理员。'),
        data: responsePayload,
      })
    }

    return ok(
      responsePayload,
      autoCleanupRefunded ? TDX_REFUNDED_RECEIPT_RESULT_MESSAGE : '处理成功，请稍后到订单详情查看结果',
    )
  })

  app.get('/tasks', { preHandler: requireModuleAccess('泰迪熊标记', 'read') }, async (request) => {
    const tasks = await listTdxAuditTasks()
    return ok(filterTasksForList(tasks, request.query || {}).map((task) => sanitizeTaskForResponse(task, request)))
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
      if (!(await isTdxLineAvailable(task.line || 'line1'))) {
        skipped.push({ id, reason: TDX_LINE2_MAINTENANCE_MESSAGE })
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
      if (!(await isTdxLineAvailable(task.line || 'line1'))) {
        skipped.push({ id, reason: TDX_LINE2_MAINTENANCE_MESSAGE })
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
      const upstreamAudit = buildTaskUpstreamAudit(task, {
        execution: result,
        orderId: task.orderId || result?.result?.orderId || '',
        wxCode: task.wxCode || result?.result?.wxCode || '',
      })
      const next = await updateTdxTask(id, {
        status,
        currentStep,
        updatedAt,
        result: {
          ...(task.result || {}),
          execution: result,
          upstreamAudit,
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
    if (isLine3RetryTask(task)) {
      return reply.code(409).send({ success: false, message: '线路3任务请向源失败任务发送验证码，线路3任务本身不能重新发送。', data: null })
    }
    const lineGuard = await ensureTdxLineAvailable(reply, task.line || 'line1')
    if (lineGuard) return lineGuard
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

  app.post('/tasks/:id/retry-line3', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard

    const sourceTask = await findTdxTask(request.params.id)
    if (!sourceTask) return reply.code(404).send({ success: false, message: '未找到泰迪熊任务', data: null })
    if (isLine3RetryTask(sourceTask)) {
      return reply.code(409).send({ success: false, message: '线路3重试任务不能再次使用线路3。', data: null })
    }

    const reusableSession = resolveTdxLine3ReusableSession(sourceTask)
    const accessOrderDeal = await loadTdxLine3RetryOrderDeal(sourceTask)
    if (accessOrderDeal && !canRequestUseTdxLine3Order(request, accessOrderDeal)) {
      return reply.code(403).send({
        success: false,
        message: '当前账号不能复用该失败订单。',
        data: null,
      })
    }

    const existingRetry = (await listTdxTasks()).find((item) => item?.retryOf === sourceTask.id && isLine3RetryTask(item))
    if (existingRetry) {
      return ok({
        task: sanitizeTaskForResponse(existingRetry, request),
        reused: true,
        alreadyAttempted: true,
        line: 'line3',
        sourceLine: resolveStoredTdxTaskLine(sourceTask) || 'line1',
      }, '线路3已执行过一次，返回原重试任务')
    }

    const smsCode = String(request.body?.smsCode || '').trim()
    if (smsCode.length < 4) {
      return reply.code(400).send({ success: false, message: '线路3重试前必须提供有效的短信验证码。', data: null })
    }
    if (!/^1\d{10}$/.test(String(sourceTask.phone || '').trim())) {
      return reply.code(400).send({ success: false, message: '原任务缺少有效手机号，无法执行线路3复用。', data: null })
    }
    if (!reusableSession?.orderId || !reusableSession?.wxCode || Number(sourceTask.orderDealId || 0) <= 0) {
      return reply.code(409).send({ success: false, message: '原任务缺少原订单、原微信或本地订单，无法复用。', data: null })
    }
    const sourceLine = resolveStoredTdxTaskLine(sourceTask) || 'line1'
    if (sourceLine === 'line3') {
      return reply.code(409).send({ success: false, message: '线路3只能从原始失败任务进入，不能嵌套复用。', data: null })
    }
    if (sourceLine !== 'line1') {
      return reply.code(409).send({ success: false, message: '线路3当前仅支持复用线路1的失败订单，线路2不能进入该复用流程。', data: null })
    }

    const orderDeal = await loadTdxLine3RetryOrderDeal(sourceTask)
    if (!orderDeal) {
      return reply.code(409).send({ success: false, message: '未找到对应的泰迪熊本地订单，无法执行线路3复用。', data: null })
    }
    if (orderDeal && String(orderDeal.companycode || '').trim() !== TDX_SMS_RECEIVE_COMPANY_CODE) {
      return reply.code(409).send({ success: false, message: '本地订单类型不支持线路3复用。', data: null })
    }
    if (orderDeal && !canRequestUseTdxLine3Order(request, orderDeal)) {
      return reply.code(403).send({
        success: false,
        message: '当前账号不能复用该失败订单。',
        data: null,
      })
    }
    if (orderDeal && String(orderDeal.phone || '').trim() !== String(sourceTask.phone || '').trim()) {
      return reply.code(409).send({ success: false, message: '原任务手机号与本地订单不一致，已停止复用。', data: null })
    }
    if (orderDeal && String(orderDeal.submitres || '').trim() && String(orderDeal.submitres || '').trim() !== reusableSession.orderId) {
      return reply.code(409).send({ success: false, message: '本地订单的上游订单已变化，不能复用原订单。', data: null })
    }
    if (orderDeal && !isLine3RetryableOrderDeal(orderDeal) && String(sourceTask.status || '').trim().toLowerCase() !== 'failed') {
      return reply.code(409).send({ success: false, message: '只有失败或已退款订单可以进入线路3复用。', data: null })
    }

    const markerState = line3RetryStatus(sourceTask)
    const retryCount = line3RetryCount(sourceTask)
    const retryableMarker = markerState === 'retryable-failed'
    const retryableState = isLine3RetryableState(sourceTask)
    if (retryableMarker && retryCount >= TDX_LINE3_RETRY_MAX_ATTEMPTS) {
      return reply.code(409).send({
        success: false,
        message: `线路3可重试次数已用完（最多 ${TDX_LINE3_RETRY_MAX_ATTEMPTS} 次）。`,
        data: null,
      })
    }
    if ((sourceTask.line3RetryAttemptedAt && !retryableState) || (['processing', 'completed', 'failed'].includes(markerState) && !retryableState)) {
      return reply.code(409).send({
        success: false,
        message: markerState === 'processing' ? '线路3正在处理中，请稍后查看重试任务。' : '线路3已执行过一次，不允许重复提交。',
        data: null,
      })
    }

    const lockKey = String(sourceTask.id)
    const existingLock = tdxLine3RetryLocks.get(lockKey)
    if (existingLock) return existingLock

    const work = (async () => {
      const attemptedAt = nowText()
      const attemptCount = retryCount + 1
      const marked = await updateTdxTask(sourceTask.id, {
        line3RetryState: 'processing',
        line3RetryCount: attemptCount,
        line3RetryAttemptedAt: attemptedAt,
        updatedAt: attemptedAt,
        currentStep: '线路3正在复用原订单和原微信，等待新的短信验证码提交。',
        notes: [
          { title: '线路3复用开始', description: '仅复用原上游订单和原微信，禁止新开订单或更换微信。', meta: attemptedAt },
          ...(sourceTask.notes || buildTaskNotes(sourceTask)),
        ],
      }) || sourceTask

      try {
        const sessionResult = await createTdxSmsReceiveSession({
          phone: sourceTask.phone,
          smsCode,
          rotate: false,
          allowNewOrder: false,
          line: sourceLine,
          reuseOrderId: reusableSession.orderId,
          reuseWxCode: reusableSession.wxCode,
        })
        if (!sessionResult?.ok) throw new Error(sessionResult?.message || '上游拒绝线路3复用。')
        if (String(sessionResult.orderId || '').trim() !== reusableSession.orderId) {
          throw new Error('上游返回了不同的订单，已拒绝线路3提交。')
        }
        if (String(sessionResult.wxCode || '').trim() !== reusableSession.wxCode) {
          throw new Error('上游返回了不同的微信绑定，已拒绝线路3提交。')
        }

        const retryCreatedAt = nowText()
        const retryTask = await createTdxTask({
          id: `tdx-task-line3-${Date.now()}`,
          phone: sourceTask.phone,
          line: 'line3',
          sourceLine,
          retryOf: sourceTask.id,
          line3Retry: true,
          username: sourceTask.username || request.currentUser?.name || request.currentUser?.username || '当前账号',
          companyname: TDX_SMS_RECEIVE_COMPANY_NAME,
          businessType: 'tdx',
          orderstatenote: smsCode,
          batchLabel: `${sourceTask.orderpicinumber || sourceTask.id} / 线路3复用`,
          orderId: reusableSession.orderId,
          wxCode: reusableSession.wxCode,
          orderDealId: Number(sourceTask.orderDealId || 0),
          orderpicinumber: sourceTask.orderpicinumber || orderDeal.orderpicinumber || '',
          loginToken: sessionResult.login?.token || sourceTask.loginToken || '',
          skipCheckAppletPhone: false,
          status: 'processing',
          currentStep: '线路3已复用原订单和原微信，正在重新提交申诉。',
          createdAt: retryCreatedAt,
          updatedAt: retryCreatedAt,
          failreason: '',
          notes: [
            { title: '线路3复用任务已创建', description: '本次只使用原上游订单和原微信，最多执行一次。', meta: retryCreatedAt },
            ...(sourceTask.notes || []),
          ],
          result: {
            ...stripSensitive(sessionResult),
            line3Retry: true,
            retryOf: sourceTask.id,
            sourceLine,
            reusedOrderId: true,
            reusedWxCode: true,
            upstreamAudit: buildTaskUpstreamAudit(sourceTask, {
              line: 'line3',
              sessionResult,
              orderId: reusableSession.orderId,
              wxCode: reusableSession.wxCode,
            }),
          },
        })

        await updateTdxTask(sourceTask.id, {
          line3RetryState: 'processing',
          line3RetryTaskId: retryTask.id,
          updatedAt: retryCreatedAt,
          notes: [
            { title: '线路3复用任务已关联', description: `已创建一次性线路3任务 ${retryTask.id}。`, meta: retryCreatedAt },
            ...(marked.notes || []),
          ],
        })

        let execution
        try {
          execution = await executeTdxCleanupTask(retryTask)
        } catch (err) {
          execution = {
            ok: false,
            result: {
              status: 'failed',
              message: err instanceof Error ? err.message : String(err || '线路3执行失败'),
              failureStep: 'line3-execute',
            },
          }
        }

        const updatedAt = nowText()
        const status = String(execution?.result?.status || (execution?.ok ? 'completed' : 'failed')).trim().toLowerCase()
        const refundSuggested = execution?.result?.refundSuggested === true
        const alreadySubmitted = execution?.result?.alreadySubmitted === true
          || execution?.result?.businessOutcome === 'already-submitted'
        const finalStatus = status === 'completed' ? 'completed' : status === 'failed' ? 'failed' : 'processing'
        const upstreamAudit = buildTaskUpstreamAudit(retryTask, {
          line: 'line3',
          execution,
          orderId: reusableSession.orderId,
          wxCode: reusableSession.wxCode,
        })
        const updatedRetryTask = await updateTdxTask(retryTask.id, {
          status: finalStatus,
          currentStep: finalStatus === 'completed'
            ? alreadySubmitted
              ? '线路3复核确认当天已有申诉记录，已完成，无需重复提交。'
              : '线路3复用提交完成，等待上游最终结果。'
            : finalStatus === 'failed'
              ? '线路3复用提交失败，已停止，不会自动换微信或新开订单。'
              : '线路3复用提交成功，等待上游处理结果。',
          updatedAt,
          failreason: finalStatus === 'failed' ? '线路3复用提交失败' : '',
          result: {
            ...(retryTask.result || {}),
            execution,
            upstreamAudit,
            line3Retry: true,
            retryOf: sourceTask.id,
            reusedOrderId: true,
            reusedWxCode: true,
          },
          notes: [
            {
              title: '线路3复用执行结果',
              description: finalStatus === 'failed'
                ? '执行失败，本次线路3尝试已结束。'
                : finalStatus === 'completed'
                  ? alreadySubmitted
                    ? '上游确认当天已有申诉记录，未重复提交，本次按已完成处理。'
                    : '已使用原订单和原微信完成重新提交。'
                  : '已使用原订单和原微信重新提交，等待上游处理。',
              meta: updatedAt,
            },
            ...(retryTask.notes || []),
          ],
        })

        if (sourceTask.orderDealId) {
          await updateTdxSmsReceiveOrderDealById(sourceTask.orderDealId, buildTdxSmsReceiveOrderPatchFromExecution(execution, updatedAt, {
            ...retryTask,
            orderId: reusableSession.orderId,
          }))
          await reconcileTdxSmsReceiveBillingByOrderId(sourceTask.orderDealId, { reason: 'line3-retry-executed' })
        }

        await updateTdxTask(sourceTask.id, {
          line3RetryState: finalStatus,
          line3RetryTaskId: retryTask.id,
          line3RetryFinishedAt: updatedAt,
          updatedAt,
          currentStep: finalStatus === 'failed'
            ? '线路3复用失败，本次尝试已结束。'
            : finalStatus === 'completed'
              ? alreadySubmitted
                ? '线路3复核完成，确认当天已有申诉记录。'
                : '线路3复用已提交，等待上游最终结果。'
              : '线路3复用已提交，等待上游处理。',
          notes: [
            { title: '线路3复用完成', description: `关联任务 ${retryTask.id} 状态为 ${finalStatus}。`, meta: updatedAt },
            ...(marked.notes || []),
          ],
        })

        const payload = {
          task: sanitizeTaskForResponse(updatedRetryTask, request),
          sourceTask: sanitizeTaskForResponse(await findTdxTask(sourceTask.id), request),
          execution: isAdminRequest(request) ? stripSensitive(execution) : { ok: Boolean(execution?.ok), result: { status: finalStatus } },
          reused: true,
          alreadyAttempted: false,
          line: 'line3',
          sourceLine,
          orderId: reusableSession.orderId,
          wxCodeMatched: true,
          refundSuggested,
        }
        if (finalStatus === 'failed') {
          return reply.code(502).send({ success: false, message: '线路3复用提交失败，本次尝试已结束。', data: payload })
        }
        return ok(payload, finalStatus === 'completed'
          ? alreadySubmitted
            ? '线路3已确认该号码当天已有申诉记录，按完成处理并保留原订单扣费'
            : '线路3已复用原订单和原微信完成提交'
          : '线路3已复用原订单和原微信提交，等待上游处理')
      } catch (err) {
        const failedAt = nowText()
        const detail = err instanceof Error ? err.message : String(err || '线路3复用失败')
        const failureStep = String(err?.failureStep || '').trim()
        const retryable = attemptCount < TDX_LINE3_RETRY_MAX_ATTEMPTS
          && isLine3RetryableFailure(detail, failureStep)
        const nextState = retryable ? 'retryable-failed' : 'failed'
        await updateTdxTask(sourceTask.id, {
          line3RetryState: nextState,
          line3RetryFinishedAt: failedAt,
          updatedAt: failedAt,
          currentStep: retryable ? '线路3复用失败，可重新获取验证码后重试。' : '线路3复用失败，本次尝试已结束。',
          line3RetryError: detail,
          notes: [
            {
              title: retryable ? '线路3复用失败，可重试' : '线路3复用失败',
              description: retryable
                ? `本次失败属于可重试类型，还可尝试 ${TDX_LINE3_RETRY_MAX_ATTEMPTS - attemptCount} 次。`
                : '上游未接受原订单或原微信，本次尝试已结束。',
              meta: failedAt,
            },
            ...(marked.notes || []),
          ],
        })
        return sendTdxFailure(
          reply,
          request,
          err,
          retryable ? '线路3复用失败，可重新获取验证码后重试。' : '线路3复用失败，本次尝试已结束。',
          502,
        )
      }
    })()
    tdxLine3RetryLocks.set(lockKey, work)
    try {
      return await work
    } finally {
      if (tdxLine3RetryLocks.get(lockKey) === work) tdxLine3RetryLocks.delete(lockKey)
    }
  })

  app.post('/tasks/:id/execute', { preHandler: requireModuleAccess('泰迪熊标记', 'edit') }, async (request, reply) => {
    const guard = await ensureTdxMarkEnabled(reply)
    if (guard) return guard
    const task = await findTdxTask(request.params.id)
    if (isLine3RetryTask(task)) {
      return reply.code(409).send({ success: false, message: '线路3复用任务已在创建时执行，不能再次单独执行清理。', data: null })
    }
    if (!task) return reply.code(404).send({ success: false, message: '未找到泰迪熊任务', data: null })
    const lineGuard = await ensureTdxLineAvailable(reply, task.line || 'line1')
    if (lineGuard) return lineGuard
    if (!hasPreparedSession(task)) {
      return reply.code(409).send({ success: false, message: '执行清理前请先准备有效的 orderId / wxCode 会话', data: null })
    }
    const result = await executeTdxCleanupTask(task)
    const updatedAt = nowText()
    const status = result?.result?.status || (result.ok ? 'completed' : 'processing')
    const currentStep = result?.result?.message || (result.ok ? '清理执行完成' : '等待继续处理')
    const upstreamAudit = buildTaskUpstreamAudit(task, {
      execution: result,
      orderId: task.orderId || result?.result?.orderId || '',
      wxCode: task.wxCode || result?.result?.wxCode || '',
    })
    const updated = await updateTdxTask(request.params.id, {
      status,
      currentStep,
      updatedAt,
      result: {
        ...(task.result || {}),
        execution: result,
        upstreamAudit,
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
    const line = task.line || body.line || 'line1'
    if (isLine3RetryTask(task) || isTdxLine3(line)) {
      return reply.code(409).send({ success: false, message: '线路3任务已固定复用原订单和原微信，请勿再次准备会话。', data: null })
    }
    const lineGuard = await ensureTdxLineAvailable(reply, line)
    if (lineGuard) return lineGuard
    if (!body.smsCode || String(body.smsCode).trim().length < 4) {
      return reply.code(400).send({ success: false, message: '准备会话前必须提供有效的短信验证码', data: null })
    }
    let result
    try {
      result = await createTdxSmsReceiveSession({
        phone: task.phone,
        smsCode: body.smsCode || '',
        rotate: Boolean(body.rotate),
        line,
      })
    } catch (err) {
      return sendTdxFailure(reply, request, err, '登录失败，请核对手机号和验证码后重试。', 502)
    }
    const updatedAt = nowText()
    const upstreamAudit = buildTaskUpstreamAudit(task, {
      line,
      sessionResult: result,
      orderId: result.orderId || task.orderId || '',
      wxCode: result.wxCode || task.wxCode || '',
    })
    const updated = await updateTdxTask(request.params.id, {
      orderId: result.orderId || task.orderId,
      wxCode: result.wxCode || task.wxCode,
      status: result.ok ? 'processing' : 'failed',
      currentStep: result.ok ? 'Session ready, waiting for cleanup execution' : 'Session preparation failed',
      updatedAt,
      result: {
        ...stripSensitive(result),
        upstreamAudit,
      },
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
    if (isTdxLine3(line)) return rejectDirectTdxLine3(reply)
    const lineGuard = await ensureTdxLineAvailable(reply, line)
    if (lineGuard) return lineGuard
    let session
    try {
      session = await createTdxSmsReceiveSession({ phone, smsCode, rotate, line })
    } catch (err) {
      return sendTdxFailure(reply, request, err, '登录失败，请核对手机号和验证码后重试。', 502)
    }
    const createdAt = nowText()
    const resolvedCompanyName = companyname || resolveTdxCompanyName(businessType)
    const upstreamAudit = buildTaskUpstreamAudit({}, {
      line,
      orderId: session.orderId || '',
      wxCode: session.wxCode || '',
      sessionResult: session,
    })
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
      result: {
        ...stripSensitive(session),
        upstreamAudit,
      },
    })
    return ok(sanitizeTaskForResponse(task, request), '泰迪熊任务已创建')
  })
}
