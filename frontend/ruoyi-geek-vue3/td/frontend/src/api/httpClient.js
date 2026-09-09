import { AUTH_TOKEN_KEY } from '../stores/authStore'

const DEFAULT_BASE_URL = typeof window !== 'undefined' ? `${window.location.protocol}//${window.location.host}/backend` : 'http://127.0.0.1:4300'
const BASE_URL = import.meta.env.VITE_API_BASE_URL || DEFAULT_BASE_URL

function readToken() {
  if (typeof window === 'undefined') return ''
  return window.localStorage.getItem(AUTH_TOKEN_KEY) || ''
}

function normalizeNetworkErrorMessage(error) {
  const text = error instanceof Error ? error.message : String(error || '')
  if (/Failed to fetch|NetworkError|Load failed|ERR_|ECONNREFUSED|ECONNRESET|ETIMEDOUT|fetch/i.test(text)) {
    return '网络波动，请刷新重试'
  }
  return text || '网络波动，请刷新重试'
}

async function readJsonPayload(response) {
  try {
    return await response.json()
  } catch {
    return {}
  }
}

function fallbackHttpMessage(status = 0) {
  if (Number(status) === 401) return '登录已过期，请重新登录。'
  return '网络波动，请刷新重试'
}

async function request(path, options = {}) {
  const token = readToken()
  const headers = {
    ...(options.headers || {}),
  }

  if (options.body) {
    headers['Content-Type'] = 'application/json'
  }

  if (options.auth !== false && token) {
    headers.Authorization = `Bearer ${token}`
  }

  let response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method: options.method || 'GET',
      headers,
      body: options.body ? JSON.stringify(options.body) : undefined,
    })
  } catch (error) {
    throw new Error(normalizeNetworkErrorMessage(error))
  }

  const payload = await readJsonPayload(response)
  if (!response.ok || payload.success === false) {
    throw new Error(payload.message || fallbackHttpMessage(response.status))
  }
  return payload.data
}

export const httpClient = {
  get: (path, options = {}) => request(path, options),
  post: (path, body, options = {}) => request(path, { ...options, method: 'POST', body }),
  put: (path, body, options = {}) => request(path, { ...options, method: 'PUT', body }),
  patch: (path, body, options = {}) => request(path, { ...options, method: 'PATCH', body }),
  delete: (path, options = {}) => request(path, { ...options, method: 'DELETE' }),
}
