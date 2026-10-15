import { readTdxSettings } from '../data/tdxSettingsStore.js'
import { getSystemSettingValue } from '../data/systemSettingsStore.js'

const DEFAULT_QMF_APP_ID = '78811'

function readEnv(key, fallback = '') {
  const value = process.env[key]
  return typeof value === 'string' && value.length ? value : fallback
}

function readEnvBoolean(key, fallback = false) {
  const value = process.env[key]
  if (typeof value !== 'string' || !value.trim()) return Boolean(fallback)
  return ['1', 'true', 'yes', 'on'].includes(value.trim().toLowerCase())
}

export async function getTdxConfig() {
  const stored = await readTdxSettings()
  const legacyDetectApi = await getSystemSettingValue('apixiaomi', '')
  const strictLive = readEnv('RIGHTCONTROL_STRICT_LIVE', '') === '1'
  const mode = strictLive ? 'live' : readEnv('TDX_MODE', stored.mode || 'mock')
  return {
    mode,
    originalIp: readEnv('TDX_ORIGINAL_IP', stored.originalIp || ''),
    maxOrderReuse: Number(readEnv('TDX_MAX_ORDER_REUSE', String(stored.maxOrderReuse || 3))),
    // The Java gateway owns external line2. This standalone service only runs line1/3 locally.
    line1Maintenance: readEnvBoolean('TDX_LINE1_MAINTENANCE', stored.line1Maintenance),
    line2Maintenance: true,
    line3Maintenance: readEnvBoolean('TDX_LINE3_MAINTENANCE', stored.line3Maintenance),
    sendSmsUrl: readEnv('TDX_SEND_SMS_URL', stored.sendSmsUrl || 'https://customer-apply-diversion.teddymobile.cn/api/v1/as/auth/sendSms'),
    loginSmsUrl: readEnv('TDX_LOGIN_SMS_URL', stored.loginSmsUrl || 'https://customer-apply-diversion.teddymobile.cn/api/v1/auth/login-sms'),
    qmfLoginUrl: readEnv('QMF_LOGIN_URL', stored.qmfLoginUrl || 'http://api.qmfj66pp.cn/public/userLogin'),
    qmfCodeUrl: readEnv('QMF_CODE_URL', stored.qmfCodeUrl || 'http://api.qmfj66pp.cn/code'),
    answersStartUrl: readEnv('TDX_ANSWERS_START_URL', stored.answersStartUrl || 'https://customer-apply-diversion.teddymobile.cn/api/v1/answers/start'),
    checkAppletPhoneUrl: readEnv('TDX_CHECK_APPLET_PHONE_URL', stored.checkAppletPhoneUrl || 'https://customer-apply-diversion.teddymobile.cn/api/v1/forward/inter/checkAppletPhone'),
    answersSubmitUrl: readEnv('TDX_ANSWERS_SUBMIT_URL', stored.answersSubmitUrl || 'https://customer-apply-diversion.teddymobile.cn/api/v1/answers/submit-and-next'),
    queryProgressUrl: readEnv('TDX_QUERY_PROGRESS_URL', stored.queryProgressUrl || 'https://customer-apply-diversion.teddymobile.cn/api/v1/forward/inter/queryAppletProgressList'),
    qmfUsername: readEnv('QMF_USERNAME', stored.qmfUsername || ''),
    qmfPassword: readEnv('QMF_PASSWORD', stored.qmfPassword || ''),
    qmfAppId: readEnv('QMF_APP_ID', stored.qmfAppId || DEFAULT_QMF_APP_ID),
    qmfQrCode: readEnv('QMF_QR_CODE', stored.qmfQrCode || '1'),
    xiaomiApiUrl: readEnv('TDX_DETECT_API_URL', stored.xiaomiApiUrl || legacyDetectApi || ''),
  }
}

