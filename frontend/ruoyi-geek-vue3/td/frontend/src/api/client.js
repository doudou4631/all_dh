const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:4320/api'

function readToken() {
  if (typeof window === 'undefined') return ''
  return window.localStorage.getItem('td_ui_token') || ''
}

async function request(path, options = {}) {
  const headers = { ...(options.headers || {}) }
  if (options.body) headers['Content-Type'] = 'application/json'
  const token = readToken()
  if (options.auth !== false && token) headers.Authorization = `Bearer ${token}`

  const response = await fetch(`${BASE_URL}${path}`, {
    method: options.method || 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  })

  let payload = {}
  try {
    payload = await response.json()
  } catch {}

  if (!response.ok || payload.success === false) {
    throw new Error(payload.message || '请求失败')
  }
  return payload.data
}

export const httpClient = {
  get: (path, options = {}) => request(path, options),
  post: (path, body, options = {}) => request(path, { ...options, method: 'POST', body }),
  patch: (path, body, options = {}) => request(path, { ...options, method: 'PATCH', body }),
}

export async function getLoginCaptcha() {
  return httpClient.get('/auth/captcha', { auth: false })
}

export async function getAppMeta() {
  return httpClient.get('/auth/meta', { auth: false })
}

export async function login(body) {
  return httpClient.post('/auth/login', body, { auth: false })
}

export async function getSession() {
  return httpClient.get('/auth/session')
}

export async function logout() {
  return httpClient.post('/auth/logout', {})
}

export async function getSettings() {
  return httpClient.get('/tdx/settings').catch(() => ({ switches: [{ key: 'tdxMarkEnabled', enabled: true }] }))
}

export async function precheckTdxPhone(body) {
  return httpClient.post('/tdx/sms-receive/precheck', body)
}

export async function sendTdxSmsCode(body) {
  return httpClient.post('/tdx/sms-receive/send-code', body)
}

export async function loginTdxSmsReceive(body) {
  return httpClient.post('/tdx/sms-receive/login', body)
}

export async function getTdxTaskDetail(id) {
  return httpClient.get(`/tdx/tasks/${id}`)
}

export async function updateTdxTaskStatus({ id, ...body }) {
  return httpClient.patch(`/tdx/tasks/${id}/status`, body)
}

export async function sendTdxTaskSmsCode(id) {
  return httpClient.post(`/tdx/tasks/${id}/send-code`, {})
}

export async function prepareTdxTaskSession({ id, ...body }) {
  return httpClient.post(`/tdx/tasks/${id}/prepare-session`, body)
}

export async function executeTdxTaskCleanup(id) {
  return httpClient.post(`/tdx/tasks/${id}/execute`, {})
}

export async function getNavigationMenus() {
  return [{
    id: 'tdx-root',
    title: 'TDX 二次申诉',
    href: '',
    children: [
      { id: 'tdx-appeal', title: '二次申诉', href: '/appeal', module: '泰迪熊标记', children: [] },
    ],
  }]
}
