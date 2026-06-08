<template>
  <main class="home-container">
    <header class="home-header">
      <h1 class="home-title">取消号码标记</h1>
      <p class="home-subtitle">高效提升您号码接听效率~</p>
    </header>

    <section class="home-search-section">
      <form class="home-search-box" @submit.prevent="handleSearch">
        <input
          v-model.trim="phoneNumber"
          class="home-search-input"
          type="tel"
          inputmode="numeric"
          maxlength="15"
          placeholder="请输入查询的手机/座机号码"
        />
        <button class="home-search-btn" type="submit">
          <span class="home-search-btn-text">全网查询</span>
        </button>
      </form>
    </section>

    <section class="home-platform-section">
      <h2 class="home-section-title">支持去除以下平台标记</h2>
      <div class="home-platform-list">
        <article v-for="item in platformItems" :key="item.name" class="home-platform-item">
          <img class="home-platform-icon" :src="item.icon" :alt="item.name" loading="lazy" decoding="async" />
          <span class="home-platform-name">{{ item.name }}</span>
        </article>
      </div>
    </section>

    <section class="home-intro-section">
      <h2 class="home-section-title">申诉说明</h2>
      <div class="home-intro-content">
        <p class="home-intro-text">{{ appealDescription }}</p>
      </div>
    </section>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { loadMobileRuntimeConfig, useMobileRuntimeConfig } from '@/runtime/mobileRuntimeConfig'
import { notify } from '@/services/bottomNavUi'

const DEFAULT_APPEAL_DESCRIPTION =
  '泰迪熊：普通标记申诉后6小时生效（30天内二次处理会驳回）；提示10个工作日要暂停拨号；腾讯管家：审核1-3个工作日（期间暂停外呼）；360卫士：三个月内只能处理一次'

const platformItems = [
  { name: '360', icon: '/mobile-h5/assets/icons/360-home.png' },
  { name: '百度', icon: '/mobile-h5/assets/icons/baidu-home.ico' },
  { name: '泰迪熊', icon: '/mobile-h5/assets/icons/teddy-home.png' },
  { name: '联通', icon: '/mobile-h5/assets/icons/unicom-home.ico' },
  { name: '腾讯', icon: '/mobile-h5/assets/icons/tencent.png' },
  { name: '移动高频', icon: '/mobile-h5/assets/icons/mobile-home.png' },
  { name: '电话邦', icon: '/mobile-h5/assets/icons/dianhuabang-home.ico' },
  { name: '搜狗', icon: '/mobile-h5/assets/icons/sogou-home.ico' }
]

const phoneNumber = ref('')
const appealDescription = ref(DEFAULT_APPEAL_DESCRIPTION)

const { state, resolveHref } = useMobileRuntimeConfig()

function ensureTrailingSlash(url) {
  const text = String(url || '').trim()
  if (!text) return ''
  return text.endsWith('/') ? text : `${text}/`
}

async function loadAppealDescription() {
  try {
    const apiBase = ensureTrailingSlash(state.config.apiBase || '/prod-api/')
    const response = await fetch(`${apiBase}user/getinfo`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8'
      }
    })
    if (!response.ok) return
    const payload = await response.json()
    const remoteText = String(payload?.data?.appealDescription || '').trim()
    if (remoteText) {
      appealDescription.value = remoteText
    }
  } catch (error) {}
}

function handleSearch() {
  const phone = String(phoneNumber.value || '').trim()
  if (!phone) {
    notify('请输入手机号码')
    return
  }
  if (!/^\d{7,15}$/.test(phone)) {
    notify('请输入正确的号码')
    return
  }
  window.location.assign(resolveHref(`/result/?phone=${encodeURIComponent(phone)}`))
}

onMounted(() => {
  loadMobileRuntimeConfig().finally(() => {
    loadAppealDescription()
  })
})
</script>

<style src="@/styles/home-page.css"></style>
