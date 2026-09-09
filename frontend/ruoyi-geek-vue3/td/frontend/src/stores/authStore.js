import { reactive } from 'vue'
import { getSession, login, logout } from '../api/client'

export const AUTH_TOKEN_KEY = 'td_ui_token'
export const AUTH_USER_KEY = 'td_ui_user'

function safeRead(key) {
  if (typeof window === 'undefined') return ''
  return window.localStorage.getItem(key) || ''
}

function safeReadUser() {
  if (typeof window === 'undefined') return null
  const raw = window.localStorage.getItem(AUTH_USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

function persistSession(token, user) {
  window.localStorage.setItem(AUTH_TOKEN_KEY, token)
  window.localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
}

function clearPersistedSession() {
  window.localStorage.removeItem(AUTH_TOKEN_KEY)
  window.localStorage.removeItem(AUTH_USER_KEY)
}

export const authState = reactive({
  token: safeRead(AUTH_TOKEN_KEY),
  user: safeReadUser(),
  ready: false,
})

function applySession(session) {
  authState.token = session.token
  authState.user = session.user
  persistSession(session.token, session.user)
}

export function isAuthenticated() {
  return Boolean(authState.token)
}

export function canReadModule() {
  return isAuthenticated()
}

export async function restoreSession() {
  if (!authState.token) {
    authState.ready = true
    return null
  }
  try {
    const session = await getSession()
    applySession(session)
    return session.user
  } catch {
    authState.token = ''
    authState.user = null
    clearPersistedSession()
    return null
  } finally {
    authState.ready = true
  }
}

export async function signIn(credentials) {
  const session = await login(credentials)
  applySession(session)
  authState.ready = true
  return session.user
}

export async function signOut() {
  try {
    if (authState.token) await logout()
  } finally {
    authState.token = ''
    authState.user = null
    authState.ready = true
    clearPersistedSession()
  }
}
