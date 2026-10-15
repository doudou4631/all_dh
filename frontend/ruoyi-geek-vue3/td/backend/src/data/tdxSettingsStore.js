import { readFile, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const settingsPath = resolve(process.cwd(), 'data', 'tdx-settings.json')
const DEFAULT_QMF_APP_ID = '78811'

const defaults = {
  mode: 'mock',
  originalIp: '192.168.114.250',
  maxOrderReuse: 3,
  line1Maintenance: true,
  // Local Node does not proxy the external line2 API; Java owns that channel.
  line2Maintenance: true,
  line3Maintenance: true,
  sendSmsUrl: 'https://customer-apply-diversion.teddymobile.cn/api/v1/as/auth/sendSms',
  loginSmsUrl: 'https://customer-apply-diversion.teddymobile.cn/api/v1/auth/login-sms',
  qmfLoginUrl: 'http://115.238.185.26:8881/public/userLogin',
  qmfCodeUrl: 'http://115.238.185.26:8881/code',
  answersStartUrl: 'https://customer-apply-diversion.teddymobile.cn/api/v1/answers/start',
  checkAppletPhoneUrl: 'https://customer-apply-diversion.teddymobile.cn/api/v1/forward/inter/checkAppletPhone',
  answersSubmitUrl: 'https://customer-apply-diversion.teddymobile.cn/api/v1/answers/submit-and-next',
  queryProgressUrl: 'https://customer-apply-diversion.teddymobile.cn/api/v1/forward/inter/queryAppletProgressList',
  qmfUsername: '',
  qmfPassword: '',
  qmfAppId: DEFAULT_QMF_APP_ID,
  qmfQrCode: '1',
}

function normalizeSettings(settings = {}) {
  const next = { ...defaults, ...(settings || {}) }
  for (const key of [
    'dfLoginUrl', 'dfCreateOrderUrl', 'dfTwoAuthUrl', 'dfOrderViewUrl',
    'dfUsername', 'dfPassword', 'dfProjectId', 'dfOrderType', 'dfOrderUrl', 'dfOrderAndId',
  ]) delete next[key]
  next.qmfAppId = String(next.qmfAppId || '').trim() || DEFAULT_QMF_APP_ID
  next.qmfQrCode = String(next.qmfQrCode || '').trim() || '1'
  for (const key of ['line1Maintenance', 'line2Maintenance', 'line3Maintenance']) {
    if (typeof next[key] === 'string') next[key] = ['1', 'true', 'yes', 'on'].includes(next[key].trim().toLowerCase())
    else next[key] = Boolean(next[key])
  }
  return next
}

export async function readTdxSettings() {
  try {
    const raw = await readFile(settingsPath, 'utf-8')
    return normalizeSettings(JSON.parse(raw))
  } catch {
    return normalizeSettings()
  }
}

export async function writeTdxSettings(patch) {
  const current = await readTdxSettings()
  const next = {
    ...current,
    ...patch,
    maxOrderReuse: Number(patch.maxOrderReuse ?? current.maxOrderReuse ?? defaults.maxOrderReuse),
  }
  await writeFile(settingsPath, JSON.stringify(next, null, 2), 'utf-8')
  return next
}

export function summarizeTdxSettings(settings) {
  const sharedReady = Boolean(
    settings.sendSmsUrl &&
    settings.loginSmsUrl &&
    settings.answersStartUrl &&
    settings.checkAppletPhoneUrl &&
    settings.answersSubmitUrl &&
    settings.queryProgressUrl,
  )
  const qmfReady = Boolean(
    sharedReady &&
    settings.qmfUsername &&
    settings.qmfPassword &&
    settings.qmfAppId &&
    settings.qmfLoginUrl &&
    settings.qmfCodeUrl,
  )
  return {
    mode: settings.mode,
    originalIp: settings.originalIp,
    maxOrderReuse: settings.maxOrderReuse,
    line1Maintenance: Boolean(settings.line1Maintenance),
    line2Maintenance: Boolean(settings.line2Maintenance),
    line3Maintenance: Boolean(settings.line3Maintenance),
    sendSmsUrl: settings.sendSmsUrl,
    loginSmsUrl: settings.loginSmsUrl,
    qmfLoginUrl: settings.qmfLoginUrl,
    qmfCodeUrl: settings.qmfCodeUrl,
    answersStartUrl: settings.answersStartUrl,
    checkAppletPhoneUrl: settings.checkAppletPhoneUrl,
    answersSubmitUrl: settings.answersSubmitUrl,
    queryProgressUrl: settings.queryProgressUrl,
    qmfUsername: settings.qmfUsername,
    qmfPasswordConfigured: Boolean(settings.qmfPassword),
    qmfAppId: settings.qmfAppId,
    qmfQrCode: settings.qmfQrCode,
    qmfReady,
    line2ExternalApi: true,
    readyForLive: Boolean(sharedReady && qmfReady),
  }
}

