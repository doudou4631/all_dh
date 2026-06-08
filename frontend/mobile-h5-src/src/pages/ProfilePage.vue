<template>
  <div class="profile-page">
    <header class="profile-hero">
      <button type="button" class="profile-user" @click="handleUserClick">
        <div class="profile-avatar" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="8" r="3.5" stroke="currentColor" stroke-width="1.6" />
            <path
              d="M5.5 19.5c1.2-3 3.4-4.5 6.5-4.5s5.3 1.5 6.5 4.5"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
            />
          </svg>
        </div>
        <div class="profile-user-text">
          <p class="profile-login-title">{{ profileTitle }}</p>
          <p class="profile-login-desc">{{ profileDesc }}</p>
        </div>
        <span class="profile-user-arrow" aria-hidden="true">›</span>
      </button>
    </header>

    <div class="profile-stats-card">
      <button type="button" class="profile-stat" @click="handlePointsClick">
        <span class="profile-stat-num" :class="{ 'is-muted': !loggedIn }">{{ pointsText }}</span>
        <span class="profile-stat-label">当前积分</span>
      </button>
      <span class="profile-stat-divider" aria-hidden="true"></span>
      <button type="button" class="profile-stat" @click="handleTodayQueryClick">
        <span class="profile-stat-num" :class="{ 'is-muted': !loggedIn }">{{ todayQueryText }}</span>
        <span class="profile-stat-label">今日查询</span>
      </button>
      <span class="profile-stat-divider" aria-hidden="true"></span>
      <button type="button" class="profile-stat" @click="handleRechargeClick">
        <span class="profile-stat-num profile-stat-action">{{ rechargeText }}</span>
        <span class="profile-stat-label">{{ rechargeLabel }}</span>
      </button>
    </div>

    <div class="profile-body">
      <div class="profile-card">
        <button type="button" class="profile-row" @click="goBatch">
          <span class="profile-row-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <rect x="4" y="5" width="16" height="14" rx="1.5" stroke="currentColor" stroke-width="1.6" />
              <path d="M8 9h8M8 12h8M8 15h5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
            </svg>
          </span>
          <span class="profile-row-label">批量查询</span>
          <span class="profile-row-arrow">›</span>
        </button>
        <button type="button" class="profile-row" @click="goQueryRecords">
          <span class="profile-row-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="8" stroke="currentColor" stroke-width="1.6" />
              <path d="M12 8v4l3 2" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
            </svg>
          </span>
          <span class="profile-row-label">查询记录</span>
          <span class="profile-row-arrow">›</span>
        </button>
        <button type="button" class="profile-row" @click="goRechargeRecords">
          <span class="profile-row-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <rect x="3" y="6" width="18" height="13" rx="2" stroke="currentColor" stroke-width="1.6" />
              <path d="M3 10h18M7 15h2" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
            </svg>
          </span>
          <span class="profile-row-label">充值记录</span>
          <span class="profile-row-arrow">›</span>
        </button>
        <button type="button" class="profile-row" @click="openPasswordModal">
          <span class="profile-row-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <rect x="5" y="11" width="14" height="10" rx="2" stroke="currentColor" stroke-width="1.6" />
              <path d="M8 11V8a4 4 0 1 1 8 0v3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
            </svg>
          </span>
          <span class="profile-row-label">修改密码</span>
          <span class="profile-row-arrow">›</span>
        </button>
      </div>

      <div class="profile-card">
        <button type="button" class="profile-row" @click="openWechatModal">
          <span class="profile-row-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M12 13a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" stroke="currentColor" stroke-width="1.6" />
              <path
                d="M6 20c.8-3 2.8-4.5 6-4.5s5.2 1.5 6 4.5"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linecap="round"
              />
            </svg>
          </span>
          <span class="profile-row-label">在线客服</span>
          <span class="profile-row-arrow">›</span>
        </button>
        <button type="button" class="profile-row" @click="inviteFriends">
          <span class="profile-row-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M12 21s-7-4.5-7-10a4 4 0 0 1 7-2 4 4 0 0 1 7 2c0 5.5-7 10-7 10Z"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
            </svg>
          </span>
          <span class="profile-row-label">邀请好友</span>
          <span class="profile-row-arrow">›</span>
        </button>
        <a class="profile-row" :href="resolveHref('/profile/about.html')">
          <span class="profile-row-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <rect x="5" y="3" width="14" height="18" rx="2" stroke="currentColor" stroke-width="1.6" />
              <path d="M9 8h6M9 12h6M9 16h4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
            </svg>
          </span>
          <span class="profile-row-label">关于我们</span>
          <span class="profile-row-arrow">›</span>
        </a>
      </div>

      <div class="profile-card">
        <a class="profile-row" :href="resolveHref('/profile/agreement.html')">
          <span class="profile-row-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M8 4h8l4 4v12a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
              <path d="M16 4v4h4M9 12h6M9 16h6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
            </svg>
          </span>
          <span class="profile-row-label">用户协议</span>
          <span class="profile-row-arrow">›</span>
        </a>
        <a class="profile-row" :href="resolveHref('/profile/privacy.html')">
          <span class="profile-row-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M12 3l7 3v6c0 4.5-3 7.8-7 9-4-1.2-7-4.5-7-9V6l7-3Z"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
              <path
                d="M9.5 12.5l1.8 1.8 3.7-3.7"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </span>
          <span class="profile-row-label">隐私协议</span>
          <span class="profile-row-arrow">›</span>
        </a>
      </div>
    </div>
  </div>

  <div class="profile-login-modal" :hidden="!loginModalVisible">
    <div class="profile-login-mask" @click="closeLoginModal"></div>
    <div class="profile-login-wrap">
      <div class="profile-login-panel" role="dialog" aria-label="账号登录">
        <h2>账号登录</h2>
        <div class="profile-field">
          <input
            v-model.trim="loginAccountInput"
            type="text"
            maxlength="32"
            placeholder="请输入账号"
            autocomplete="username"
            @keydown.enter="submitLogin"
          />
        </div>
        <div class="profile-field">
          <input
            v-model="loginPasswordInput"
            type="password"
            maxlength="32"
            placeholder="请输入密码"
            autocomplete="current-password"
            @keydown.enter="submitLogin"
          />
        </div>
        <p class="profile-login-tip">账户由客服开通，忘记密码请联系客服</p>
        <button type="button" class="profile-submit-btn" :disabled="loginSubmitting" @click="submitLogin">
          {{ loginSubmitting ? '登录中...' : '登录' }}
        </button>
        <button type="button" class="profile-close-btn" @click="closeLoginModal">取消</button>
      </div>
    </div>
  </div>

  <div class="profile-login-modal" :hidden="!passwordModalVisible">
    <div class="profile-login-mask" @click="closePasswordModal"></div>
    <div class="profile-login-wrap">
      <div class="profile-login-panel" role="dialog" aria-label="修改密码">
        <h2>修改密码</h2>
        <div class="profile-field">
          <input v-model="oldPassword" type="password" maxlength="32" placeholder="请输入原密码" autocomplete="current-password" />
        </div>
        <div class="profile-field">
          <input
            v-model="newPassword"
            type="password"
            maxlength="32"
            placeholder="请输入新密码（至少6位）"
            autocomplete="new-password"
          />
        </div>
        <div class="profile-field">
          <input
            v-model="confirmPassword"
            type="password"
            maxlength="32"
            placeholder="请再次输入新密码"
            autocomplete="new-password"
          />
        </div>
        <button type="button" class="profile-submit-btn" @click="submitPasswordChange">确认修改</button>
        <button type="button" class="profile-close-btn" @click="closePasswordModal">取消</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useMobileRuntimeConfig } from '@/runtime/mobileRuntimeConfig'
import { requestRemoteLogin } from '@/services/freeQueryBridge'
import { getTodayQueryCount } from '@/services/queryStats'
import {
  clearLogin,
  getAccount,
  getAccountPassword,
  getPoints,
  getToken,
  isLoggedIn,
  maskAccount,
  setAccount,
  setAccountPassword,
  setPoints,
  setToken
} from '@/services/profileSession'
import { confirmDialog, notify, openWechatModal } from '@/services/bottomNavUi'

const { state, resolveHref, loadMobileRuntimeConfig } = useMobileRuntimeConfig()

const account = ref(getAccount())
const token = ref(getToken())

const loginModalVisible = ref(false)
const passwordModalVisible = ref(false)
const loginSubmitting = ref(false)

const loginAccountInput = ref('')
const loginPasswordInput = ref('')
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

const loggedIn = computed(() => !!(account.value && token.value))
const profileTitle = computed(() => (loggedIn.value ? maskAccount(account.value) : '登录/注册'))
const profileDesc = computed(() => (loggedIn.value ? '欢迎使用批量查询服务' : '取消号码标记，提升号码接听效率~'))
const pointsText = computed(() => (loggedIn.value ? String(getPoints(account.value)) : '--'))
const todayQueryText = computed(() => (loggedIn.value ? String(getTodayQueryCount(account.value)) : '--'))
const rechargeText = computed(() => (loggedIn.value ? '充值' : '登录'))
const rechargeLabel = computed(() => (loggedIn.value ? '联系客服' : '立即登录'))

function refreshSession() {
  account.value = getAccount()
  token.value = getToken()
}

function requireLogin(tip = '请先登录') {
  if (loggedIn.value) return true
  notify(tip)
  openLoginModal()
  return false
}

function openLoginModal() {
  loginModalVisible.value = true
}

function closeLoginModal() {
  loginModalVisible.value = false
}

function closePasswordModal() {
  passwordModalVisible.value = false
  oldPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
}

function openPasswordModal() {
  if (!requireLogin()) return
  passwordModalVisible.value = true
}

function isAbsoluteUrl(url) {
  const text = String(url || '').toLowerCase()
  return text.indexOf('http://') === 0 || text.indexOf('https://') === 0
}

function getPostLoginRedirectPath() {
  try {
    const params = new URLSearchParams(window.location.search || '')
    const raw = String(params.get('redirect') || '').trim()
    if (!raw) return ''
    if (raw.charAt(0) !== '/') return ''
    if (raw.indexOf('//') === 0) return ''
    if (raw.indexOf('/profile') === 0 || raw.indexOf('/mobile-h5/profile') === 0) return ''
    return raw
  } catch (error) {
    return ''
  }
}

function redirectAfterLoginIfNeeded() {
  const path = getPostLoginRedirectPath()
  if (!path) return false
  window.location.assign(resolveHref(path))
  return true
}

function handleUserClick() {
  if (!loggedIn.value) {
    openLoginModal()
    return
  }
  confirmDialog('是否退出登录？', () => {
    clearLogin()
    refreshSession()
    window.location.assign(resolveHref('/'))
  })
}

function validateLoginForm(accountInput, passwordInput) {
  if (!accountInput || !passwordInput) return '请输入账号和密码'
  if (accountInput.length < 2) return '账号至少 2 个字符'
  if (passwordInput.length < 5) return '密码至少 5 位'
  return ''
}

function applyRemoteLoginResult(loginData, accountInput, passwordInput) {
  const loginToken = String(loginData?.token || '').trim()
  if (!loginToken) return false
  let loginAccount = String(
    loginData?.account || loginData?.userName || loginData?.phone || loginData?.nickName || accountInput || ''
  ).trim()
  if (!loginAccount) {
    loginAccount = String(accountInput || '').trim()
  }
  if (!loginAccount) return false

  setAccount(loginAccount)
  setToken(loginToken)

  const points = Number(loginData?.points)
  if (!Number.isNaN(points)) {
    setPoints(points, loginAccount)
  }

  if (String(passwordInput || '').trim()) {
    setAccountPassword(loginAccount, passwordInput)
  }
  return true
}

async function submitLogin() {
  if (loginSubmitting.value) return
  const accountInput = String(loginAccountInput.value || '').trim()
  const passwordInput = String(loginPasswordInput.value || '')
  const formError = validateLoginForm(accountInput, passwordInput)
  if (formError) {
    notify(formError)
    return
  }

  loginSubmitting.value = true
  try {
    const data = await requestRemoteLogin(accountInput, passwordInput, state.config.apiBase)
    if (!applyRemoteLoginResult(data, accountInput, passwordInput)) {
      notify('登录失败，请稍后重试')
      return
    }
    refreshSession()
    loginAccountInput.value = ''
    loginPasswordInput.value = ''
    closeLoginModal()
    redirectAfterLoginIfNeeded()
  } catch (error) {
    notify(error?.message || '登录失败')
  } finally {
    loginSubmitting.value = false
  }
}

function submitPasswordChange() {
  if (!requireLogin()) return
  const accountValue = account.value
  const oldPwd = String(oldPassword.value || '')
  const newPwd = String(newPassword.value || '')
  const confirmPwd = String(confirmPassword.value || '')

  if (!oldPwd || !newPwd || !confirmPwd) {
    notify('请填写完整密码信息')
    return
  }
  if (getAccountPassword(accountValue) !== oldPwd) {
    notify('原密码错误')
    return
  }
  if (newPwd.length < 6) {
    notify('新密码至少 6 位')
    return
  }
  if (newPwd !== confirmPwd) {
    notify('两次输入的新密码不一致')
    return
  }

  setAccountPassword(accountValue, newPwd)
  closePasswordModal()
  notify('密码修改成功，请使用新密码登录')
}

function handlePointsClick() {
  if (!requireLogin()) return
  window.location.assign(resolveHref('/profile/recharge-records.html'))
}

function handleTodayQueryClick() {
  if (!requireLogin()) return
  window.location.assign(resolveHref('/profile/query-records.html'))
}

function handleRechargeClick() {
  if (!loggedIn.value) {
    openLoginModal()
    return
  }
  openWechatModal()
}

function goBatch() {
  if (!requireLogin()) return
  window.location.assign(resolveHref('/batch/'))
}

function goQueryRecords() {
  if (!requireLogin()) return
  window.location.assign(resolveHref('/profile/query-records.html'))
}

function goRechargeRecords() {
  if (!requireLogin()) return
  window.location.assign(resolveHref('/profile/recharge-records.html'))
}

function inviteFriends() {
  const text = '取消号码标记，提升号码接听效率~'
  const sharePath = resolveHref('/')
  const shareUrl = isAbsoluteUrl(sharePath) ? sharePath : `${window.location.origin}${sharePath}`
  if (navigator.share) {
    navigator
      .share({ title: '取消号码标记', text, url: shareUrl })
      .catch(() => {})
    return
  }
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard
      .writeText(shareUrl)
      .then(() => {
        notify('邀请链接已复制，快去分享吧')
      })
      .catch(() => {
        notify(`请复制链接分享给好友：${shareUrl}`)
      })
    return
  }
  notify(`请复制链接分享给好友：${shareUrl}`)
}

const stopModalWatch = watch(
  [loginModalVisible, passwordModalVisible],
  ([loginOpen, passwordOpen]) => {
    document.body.classList.toggle('profile-modal-open', !!(loginOpen || passwordOpen))
  },
  { immediate: true }
)

onMounted(() => {
  loadMobileRuntimeConfig()
  refreshSession()
  if (isLoggedIn() && redirectAfterLoginIfNeeded()) return
})

onBeforeUnmount(() => {
  stopModalWatch()
  document.body.classList.remove('profile-modal-open')
})
</script>
