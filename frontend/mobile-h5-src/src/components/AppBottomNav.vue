<template>
  <nav class="app-bottom-nav" aria-label="底部导航">
    <template v-for="item in navItems" :key="item.id">
      <button
        v-if="item.action === 'contact'"
        type="button"
        :class="['app-bottom-nav-item', { 'is-active': item.id === activeId }]"
        @click="openWechatModal"
      >
        <span class="nav-icon" v-html="icons.service"></span>
        <span class="nav-label">{{ item.label }}</span>
      </button>
      <a
        v-else
        :href="resolveHref(item.href)"
        :class="['app-bottom-nav-item', { 'is-active': item.id === activeId }]"
        @click="handleNavClick(item, $event)"
      >
        <span class="nav-icon" v-html="iconByNavId(item.id)"></span>
        <span class="nav-label">{{ item.label }}</span>
      </a>
    </template>
  </nav>

  <div class="bn-wechat-modal" :hidden="!uiState.wechatModalVisible">
    <div class="bn-wechat-modal-mask" @click="closeWechatModal"></div>
    <div class="bn-wechat-modal-panel" role="dialog" aria-modal="true" aria-label="联系客服">
      <img class="bn-wechat-modal-qr" :src="wechatQrUrl" alt="客服微信二维码" />
      <p class="bn-wechat-modal-tip">长按识别二维码添加客服微信</p>
      <p class="bn-wechat-modal-tip" style="margin-top: 8px">
        或拨打
        <a class="bn-wechat-phone-link" :href="phoneHref">{{ servicePhone }}</a>
      </p>
    </div>
  </div>

  <div id="bn-toast" class="bn-toast" :class="{ 'is-show': uiState.toastVisible }">
    {{ uiState.toastMessage }}
  </div>

  <div id="bn-confirm-modal" class="bn-confirm-modal" :hidden="!uiState.confirmVisible">
    <div class="bn-confirm-mask" @click="cancelConfirmDialog"></div>
    <div class="bn-confirm-panel" role="dialog" aria-modal="true" aria-label="提示">
      <p class="bn-confirm-message">{{ uiState.confirmMessage }}</p>
      <div class="bn-confirm-actions">
        <button type="button" class="bn-confirm-btn bn-confirm-btn--cancel" @click="cancelConfirmDialog">取消</button>
        <button type="button" class="bn-confirm-btn bn-confirm-btn--ok" @click="approveConfirmDialog">确定</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useMobileRuntimeConfig } from '@/runtime/mobileRuntimeConfig'
import {
  useBottomNavUiState,
  openWechatModal,
  closeWechatModal,
  cancelConfirmDialog,
  approveConfirmDialog,
  registerWindowBottomNavBridge
} from '@/services/bottomNavUi'
import { isLoggedIn } from '@/services/profileSession'

const icons = {
  home:
    '<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path class="nav-icon-stroke" d="M4 10.5L12 4l8 6.5V19a1.5 1.5 0 0 1-1.5 1.5H15v-6H9v6H5.5A1.5 1.5 0 0 1 4 19v-8.5Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/></svg>',
  search:
    '<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><circle class="nav-icon-stroke" cx="11" cy="11" r="6.5" stroke="currentColor" stroke-width="1.8"/><path class="nav-icon-stroke" d="M16 16l4.5 4.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
  batch:
    '<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><rect class="nav-icon-stroke" x="4" y="5" width="16" height="14" rx="1.5" stroke="currentColor" stroke-width="1.8"/><path class="nav-icon-stroke" d="M8 9h8M8 12h8M8 15h5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
  service:
    '<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path class="nav-icon-stroke" d="M5 10.5V9a7 7 0 0 1 14 0v1.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path class="nav-icon-stroke" d="M6 10.5h-.5A2.5 2.5 0 0 0 3 13v1.5A2.5 2.5 0 0 0 5.5 17H6m12-6.5h.5A2.5 2.5 0 0 1 21 13v1.5a2.5 2.5 0 0 1-2.5 2.5H18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><rect class="nav-icon-stroke" x="5" y="10.5" width="14" height="8" rx="2" stroke="currentColor" stroke-width="1.8"/></svg>',
  profile:
    '<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><circle class="nav-icon-stroke" cx="12" cy="8" r="3.5" stroke="currentColor" stroke-width="1.8"/><path class="nav-icon-stroke" d="M5.5 19.5c1.2-3 3.4-4.5 6.5-4.5s5.3 1.5 6.5 4.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>'
}

const route = useRoute()
const uiState = useBottomNavUiState()
const { state: runtimeState, resolveHref, toPath, loadMobileRuntimeConfig } = useMobileRuntimeConfig()

const navItems = computed(() => [
  { id: 'home', label: '首页', href: runtimeState.config.navHomeUrl || '/' },
  { id: 'free-query', label: '免费查询', href: runtimeState.config.navQueryUrl || '/?tab=query' },
  { id: 'batch', label: '批量查询', href: runtimeState.config.navBatchUrl || '/batch/' },
  { id: 'contact', label: '联系客服', action: 'contact' },
  { id: 'profile', label: '个人中心', href: runtimeState.config.navProfileUrl || '/profile/' }
])

const servicePhone = computed(() => String(runtimeState.config.servicePhone || '13027616171'))
const phoneHref = computed(() => `tel:${servicePhone.value}`)
const wechatQrUrl = computed(() => String(runtimeState.config.wechatQrUrl || '/mobile-h5/assets/icons/customer-wechat.png'))

const activeId = computed(() => {
  const path = normalizePath(route.path || '/')
  if (path === '/batch') return 'batch'
  if (path.startsWith('/profile')) return 'profile'
  if (path.startsWith('/result')) return 'free-query'
  if (path.startsWith('/captcha') || path.startsWith('/pages/captcha')) return 'free-query'
  if (String(route.query.tab || '').trim() === 'query') return 'free-query'
  return 'home'
})

function normalizePath(value) {
  const text = String(value || '/').trim() || '/'
  return text.replace(/\/+$/, '') || '/'
}

function iconByNavId(id) {
  if (id === 'free-query') return icons.search
  if (id === 'contact') return icons.service
  if (id === 'batch') return icons.batch
  if (id === 'profile') return icons.profile
  return icons.home
}

function getBatchLoginRedirectHref() {
  const batchPath = toPath(resolveHref('/batch/'))
  const redirect = encodeURIComponent(batchPath || '/batch/')
  return resolveHref(`/profile/?redirect=${redirect}`)
}

function onHomeAndNeedScrollSearch() {
  const path = normalizePath(route.path || '/')
  return (path === '/' || path === '/index.html') && String(route.query.tab || '').trim() !== 'query'
}

function scrollToSearch() {
  const el =
    document.querySelector('.search-section[data-v-41c99d40]') ||
    document.querySelector('.search-section') ||
    document.querySelector('.search-box')
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    const input = document.querySelector('.search-input .uni-input-input') || document.querySelector('.search-input input')
    if (input) setTimeout(() => input.focus(), 350)
  }
}

function handleNavClick(item, event) {
  if (item.id === 'free-query' && onHomeAndNeedScrollSearch()) {
    event.preventDefault()
    scrollToSearch()
    return
  }
  if (item.id === 'batch' && !isLoggedIn()) {
    event.preventDefault()
    window.location.assign(getBatchLoginRedirectHref())
  }
}

function handleKeydown(event) {
  if (event.key !== 'Escape') return
  if (uiState.confirmVisible) {
    cancelConfirmDialog()
    return
  }
  if (uiState.wechatModalVisible) {
    closeWechatModal()
  }
}

let stopWechatWatch = null
let stopConfirmWatch = null

onMounted(() => {
  registerWindowBottomNavBridge()
  document.body.classList.add('has-bottom-nav')
  stopWechatWatch = watch(
    () => uiState.wechatModalVisible,
    (visible) => {
      document.body.classList.toggle('bn-modal-open', !!visible)
    },
    { immediate: true }
  )
  stopConfirmWatch = watch(
    () => uiState.confirmVisible,
    (visible) => {
      document.body.classList.toggle('bn-confirm-open', !!visible)
    },
    { immediate: true }
  )
  document.addEventListener('keydown', handleKeydown)
  loadMobileRuntimeConfig()
})

onBeforeUnmount(() => {
  if (stopWechatWatch) stopWechatWatch()
  if (stopConfirmWatch) stopConfirmWatch()
  document.removeEventListener('keydown', handleKeydown)
  document.body.classList.remove('bn-modal-open')
  document.body.classList.remove('bn-confirm-open')
  document.body.classList.remove('has-bottom-nav')
})
</script>

<style>
.bn-toast {
  position: fixed;
  left: 50%;
  bottom: 96px;
  transform: translate(-50%, 8px);
  background: rgba(28, 28, 30, 0.92);
  color: #fff;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.4;
  max-width: 82vw;
  text-align: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.22);
  z-index: 2300;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.bn-toast.is-show {
  opacity: 1;
  transform: translate(-50%, 0);
}

.bn-confirm-modal {
  position: fixed;
  inset: 0;
  z-index: 2400;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
}

.bn-confirm-modal[hidden] {
  display: none !important;
}

.bn-confirm-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
}

.bn-confirm-panel {
  position: relative;
  width: min(320px, 90vw);
  background: #fff;
  border-radius: 14px;
  padding: 18px 16px 14px;
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.24);
}

.bn-confirm-message {
  margin: 0;
  color: #1f2d3d;
  font-size: 15px;
  line-height: 1.6;
  text-align: center;
  word-break: break-word;
}

.bn-confirm-actions {
  margin-top: 14px;
  display: flex;
  gap: 10px;
}

.bn-confirm-btn {
  flex: 1;
  height: 38px;
  border-radius: 10px;
  border: 1px solid #d9e0ee;
  background: #fff;
  color: #334155;
  font-size: 14px;
  cursor: pointer;
}

.bn-confirm-btn--ok {
  border-color: #1f6bff;
  background: #1f6bff;
  color: #fff;
}
</style>
