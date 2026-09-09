import { readFile, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const settingsPath = resolve(process.cwd(), 'data', 'tdx-settings.json')
const DEFAULT_QMF_APP_ID = '78811'

const defaults = {
  mode: 'mock',
  originalIp: '192.168.114.250',
  maxOrderReuse: 3,
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
  dfLoginUrl: 'http://api.dengfeng.app/api/Member/login',
  dfCreateOrderUrl: 'http://api.dengfeng.app/api/Order/CreateOrder',
  dfTwoAuthUrl: 'http://api.dengfeng.app/api/Order/twoauth',
  dfOrderViewUrl: 'http://api.dengfeng.app/api/Order/view',
  dfUsername: '',
  dfPassword: '',
  dfProjectId: '7979',
  dfOrderType: '1',
}

function normalizeSettings(settings = {}) {
  const next = { ...defaults, ...(settings || {}) }
  next.qmfAppId = String(next.qmfAppId || '').trim() || DEFAULT_QMF_APP_ID
  next.qmfQrCode = String(next.qmfQrCode || '').trim() || '1'
  next.dfProjectId = String(next.dfProjectId || '').trim() || '7979'
  next.dfOrderType = String(next.dfOrderType || '').trim() || '1'
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
  const dengfengReady = Boolean(
    sharedReady &&
    settings.dfUsername &&
    settings.dfPassword &&
    settings.dfLoginUrl &&
    settings.dfCreateOrderUrl &&
    settings.dfTwoAuthUrl &&
    settings.dfOrderViewUrl &&
    settings.dfProjectId &&
    settings.dfOrderType,
  )
  return {
    mode: settings.mode,
    originalIp: settings.originalIp,
    maxOrderReuse: settings.maxOrderReuse,
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
    dfLoginUrl: settings.dfLoginUrl,
    dfCreateOrderUrl: settings.dfCreateOrderUrl,
    dfTwoAuthUrl: settings.dfTwoAuthUrl,
    dfOrderViewUrl: settings.dfOrderViewUrl,
    dfUsername: settings.dfUsername,
    dengfengPasswordConfigured: Boolean(settings.dfPassword),
    dfProjectId: settings.dfProjectId,
    dfOrderType: settings.dfOrderType,
    qmfReady,
    dengfengReady,
    readyForLive: Boolean(sharedReady && (qmfReady || dengfengReady)),
  }
}

