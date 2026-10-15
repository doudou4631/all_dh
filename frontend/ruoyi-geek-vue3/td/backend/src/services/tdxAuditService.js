import { formatChinaDateTime } from '../lib/time.js'
import { getTdxOrderSessionUsedCount } from './tdxRuntimeStore.js'

const DEFAULT_CAPTURED_AT = () => formatChinaDateTime()

function normalizeCapturedAt(value = '') {
  const text = normalizeText(value)
  if (!text) return ''
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(text)) return text
  return formatChinaDateTime(text)
}

function normalizeText(value = '') {
  return String(value ?? '').trim()
}

function firstText(...values) {
  return values
    .map((value) => normalizeText(value))
    .find(Boolean) || ''
}

function normalizeLine(line = 'line1') {
  const value = normalizeText(line).toLowerCase()
  if (value === 'line2') return 'line2'
  if (value === 'line3') return 'line3'
  return 'line1'
}

function providerForLine(line = 'line1') {
  return normalizeLine(line) === 'line2' ? 'External API' : 'QMF'
}

function pickPayload({ payload = null, sessionResult = null, execution = null, failure = null } = {}) {
  return payload
    || sessionResult?.raw
    || sessionResult?.login?.payload
    || sessionResult?.payload
    || execution?.result?.raw
    || execution?.raw
    || failure?.qmfPayload
    || failure?.raw
    || failure?.payload
    || failure?.login?.payload
    || null
}

function pickOrderId({ orderId = '', payload = null, sessionResult = null, execution = null, failure = null } = {}) {
  const data = payload?.data || {}
  const nestedOrder = data.order || payload?.order || {}
  return firstText(
    orderId,
    sessionResult?.orderId,
    execution?.result?.orderId,
    failure?.orderId,
    failure?.qmfOrderId,
    data.orderId,
    data.orderID,
    data.o_id,
    data.order_id,
    nestedOrder.id,
    nestedOrder.o_id,
    nestedOrder.order_id,
    payload?.orderId,
    payload?.orderID,
    payload?.o_id,
    payload?.order_id,
  )
}

function pickWxCode({ wxCode = '', payload = null, sessionResult = null, execution = null } = {}) {
  const data = payload?.data || {}
  return firstText(
    wxCode,
    sessionResult?.wxCode,
    execution?.result?.wxCode,
    data.wxCode,
    data.wxid,
    data.openid,
    data.openId,
    payload?.wxCode,
    payload?.openid,
    payload?.openId,
  )
}

function pickExplicitWxid(value = null) {
  if (!value || typeof value !== 'object') return ''
  const data = value.data && typeof value.data === 'object' ? value.data : value
  const nested = data.wxid
  return firstText(
    typeof nested === 'string' || typeof nested === 'number' ? nested : '',
    nested && typeof nested === 'object' ? nested.wxid : '',
    nested && typeof nested === 'object' ? nested.wxId : '',
    data.wxId,
    data.wechatId,
    data.wechat_id,
    value.wxid,
    value.wxId,
    value.wechatId,
    value.wechat_id,
  )
}

function pickWxid({ wxid = '', payload = null, sessionResult = null, execution = null, failure = null, previous = null } = {}) {
  const payloads = [
    payload,
    payload?.orderView,
    payload?.twoAuth,
    payload?.createOrder,
    sessionResult?.raw,
    execution?.result?.raw,
    failure?.raw,
    failure?.payload,
    previous?.upstreamPayload,
  ].filter(Boolean)
  return firstText(
    wxid,
    sessionResult?.wxid,
    execution?.result?.wxid,
    failure?.wxid,
    previous?.wxid,
    ...payloads.map((item) => pickExplicitWxid(item)),
  ).slice(0, 255)
}

function pickCode({ payload = null, sessionResult = null, execution = null, failure = null } = {}) {
  const payloads = [payload, payload?.createOrder, payload?.twoAuth, payload?.orderView].filter(Boolean)
  for (const item of payloads) {
    const code = item?.code ?? item?.statusCode ?? item?.data?.code
    if (code !== undefined && code !== null && String(code).trim() !== '') return code
  }
  return sessionResult?.login?.payload?.code
    ?? execution?.result?.upstreamCode
    ?? failure?.code
    ?? null
}

function pickMessage({ payload = null, sessionResult = null, execution = null, failure = null, message = '' } = {}) {
  const payloads = [payload, payload?.createOrder, payload?.twoAuth, payload?.orderView].filter(Boolean)
  return firstText(
    message,
    ...payloads.flatMap((item) => [item?.message, item?.msg, item?.error, item?.data?.message, item?.data?.msg]),
    sessionResult?.message,
    sessionResult?.login?.message,
    sessionResult?.login?.payload?.message,
    sessionResult?.login?.payload?.msg,
    execution?.result?.message,
    execution?.message,
    failure?.message,
    failure?.error,
  )
}

function pickFailureStep({ failureStep = '', sessionResult = null, execution = null, failure = null } = {}) {
  const explicit = firstText(
    failureStep,
    sessionResult?.failureStep,
    execution?.result?.failureStep,
    failure?.failureStep,
  )
  if (explicit) return explicit

  const steps = execution?.result?.steps || execution?.steps
  if (!Array.isArray(steps)) return ''
  const terminalStep = steps.find((step) => ['failed', 'blocked'].includes(String(step?.status || '').trim().toLowerCase()))
  if (terminalStep?.key) return String(terminalStep.key).trim()
  if (String(execution?.result?.status || '').trim().toLowerCase() === 'failed') {
    const activeStep = steps.find((step) => String(step?.status || '').trim().toLowerCase() === 'active')
    if (activeStep?.key) return String(activeStep.key).trim()
  }
  return ''
}

function pickUsedCount({ orderId = '', sessionResult = null, execution = null, previous = null } = {}) {
  const value = sessionResult?.usedCount ?? execution?.result?.usedCount ?? previous?.usedCount
  if (value !== null && value !== undefined && String(value).trim() !== '') {
    const parsed = Number(value)
    if (Number.isFinite(parsed) && parsed >= 0) return parsed
  }

  return getTdxOrderSessionUsedCount(orderId)
}

function pickQmfStage({ payload = null, sessionResult = null, failure = null, previous = null } = {}) {
  return firstText(
    payload?._qmfStage,
    sessionResult?.qmfStage,
    failure?.qmfStage,
    previous?.qmfStage,
  )
}

function redactSensitive(value, seen = new WeakSet()) {
  if (!value || typeof value !== 'object') return value
  if (seen.has(value)) return '[循环引用]'
  seen.add(value)
  if (Array.isArray(value)) return value.map((item) => redactSensitive(item, seen))

  const result = {}
  for (const [key, item] of Object.entries(value)) {
    const lower = String(key).toLowerCase()
    if (lower.includes('token') || lower.includes('authorization') || lower.includes('password') || lower === 'pwd' || lower.includes('smscode') || lower.includes('wxcode') || lower === 'openid' || lower.includes('jscode')) {
      result[key] = '[已隐藏]'
    } else {
      result[key] = redactSensitive(item, seen)
    }
  }
  return result
}

export function buildTdxUpstreamAudit({
  line = 'line1',
  orderId = '',
  wxCode = '',
  wxid = '',
  sessionResult = null,
  execution = null,
  failure = null,
  payload = null,
  failureStep = '',
  message = '',
  capturedAt = '',
  existing = null,
} = {}) {
  const previous = existing && typeof existing === 'object' ? existing : {}
  const sourcePayload = pickPayload({ payload, sessionResult, execution, failure })
  const resolvedLine = normalizeLine(line || previous.line)
  const resolvedOrderId = pickOrderId({ orderId, payload: sourcePayload, sessionResult, execution, failure }) || firstText(previous.orderId)
  const resolvedWxCode = pickWxCode({ wxCode, payload: sourcePayload, sessionResult, execution }) || firstText(previous.wxCode)
  const resolvedWxid = pickWxid({ wxid, payload: sourcePayload, sessionResult, execution, failure, previous })
  const resolvedCode = pickCode({ payload: sourcePayload, sessionResult, execution, failure })
  const resolvedMessage = pickMessage({ payload: sourcePayload, sessionResult, execution, failure, message })
  const resolvedFailureStep = pickFailureStep({ failureStep, sessionResult, execution, failure }) || firstText(previous.failureStep)
  const qmfStage = pickQmfStage({ payload: sourcePayload, sessionResult, failure, previous })
  const usedCount = pickUsedCount({ orderId: resolvedOrderId, sessionResult, execution, previous })
  const reused = typeof sessionResult?.reused === 'boolean'
    ? sessionResult.reused
    : typeof execution?.result?.reused === 'boolean'
      ? execution.result.reused
      : typeof previous.reused === 'boolean' ? previous.reused : null

  return {
    provider: providerForLine(resolvedLine),
    line: resolvedLine,
    orderId: resolvedOrderId,
    wxCode: resolvedWxCode,
    wxid: resolvedWxid,
    failureStep: resolvedFailureStep,
    qmfStage,
    upstreamCode: resolvedCode ?? previous.upstreamCode ?? null,
    upstreamMessage: resolvedMessage || firstText(previous.upstreamMessage),
    upstreamPayload: sourcePayload ? redactSensitive(sourcePayload) : (previous.upstreamPayload || null),
    usedCount,
    reused,
    capturedAt: firstText(capturedAt, previous.capturedAt) || DEFAULT_CAPTURED_AT(),
  }
}

export function sanitizeTdxUpstreamAudit(audit = {}, { admin = false } = {}) {
  const safe = redactSensitive(audit || {})
  safe.capturedAt = normalizeCapturedAt(safe.capturedAt)
  if (admin) return safe
  return {
    provider: safe.provider || '',
    line: safe.line || '',
    failureStep: safe.failureStep || '',
    qmfStage: safe.qmfStage || '',
    upstreamCode: safe.upstreamCode ?? null,
    upstreamMessage: safe.upstreamMessage || '',
    capturedAt: safe.capturedAt,
  }
}
