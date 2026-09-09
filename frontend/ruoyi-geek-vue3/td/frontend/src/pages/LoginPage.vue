<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAppMeta, getLoginCaptcha } from '../api/client'
import { signIn } from '../stores/authStore'

const router = useRouter()
const loading = ref(false)
const captchaLoading = ref(false)
const error = ref('')
const captchaImage = ref('')
const pageMeta = reactive({
  siteTitle: '业务管理系统',
  loginTitle: '登录',
  loginNotice: '',
})

const form = reactive({
  username: '',
  password: '',
  captcha: '',
  captchaId: '',
})

async function loadPageMeta() {
  try {
    const meta = await getAppMeta()
    pageMeta.siteTitle = meta.siteTitle || '业务管理系统'
    pageMeta.loginTitle = meta.loginTitle || '登录'
    pageMeta.loginNotice = meta.loginNotice || ''
    if (typeof document !== 'undefined') {
      document.title = pageMeta.siteTitle
    }
  } catch {
    if (typeof document !== 'undefined') {
      document.title = '业务管理系统'
    }
  }
}

async function refreshCaptcha() {
  captchaLoading.value = true
  try {
    const result = await getLoginCaptcha()
    captchaImage.value = result.imageData
    form.captchaId = result.captchaId
    form.captcha = ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载验证码失败'
  } finally {
    captchaLoading.value = false
  }
}

async function handleLogin() {
  error.value = ''
  if (!form.username || !form.password || !form.captcha || !form.captchaId) {
    error.value = '请完整填写账号、密码和验证码。'
    return
  }

  loading.value = true
  try {
    await signIn({
      username: form.username,
      password: form.password,
      captcha: form.captcha,
      captchaId: form.captchaId,
    })
    await router.push('/dashboard')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败'
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadPageMeta()
  await refreshCaptcha()
})
</script>

<template>
  <main class="login-page">
    <section class="login-layout login-layout-simple">
      <div class="login-form-wrap">
        <h2>{{ pageMeta.loginTitle }}</h2>
        <p v-if="pageMeta.loginNotice" class="muted" style="margin: -6px 0 14px;">
          {{ pageMeta.loginNotice }}
        </p>
        <form class="form-grid" @submit.prevent="handleLogin">
          <div class="field">
            <label>账号</label>
            <input v-model="form.username" type="text" placeholder="请输入账号" />
          </div>
          <div class="field">
            <label>密码</label>
            <input v-model="form.password" type="password" placeholder="请输入密码" />
          </div>
          <div class="field">
            <label>验证码</label>
            <div class="captcha-row">
              <input v-model="form.captcha" type="text" maxlength="4" placeholder="请输入验证码" />
              <div class="captcha-side">
                <div class="captcha-box">
                  <img
                    v-if="captchaImage"
                    :src="captchaImage"
                    alt="验证码"
                    class="captcha-image"
                  />
                </div>
                <a href="javascript:;" class="muted captcha-refresh-link" @click.prevent="refreshCaptcha">
                  {{ captchaLoading ? '加载中...' : '看不清换一张' }}
                </a>
              </div>
            </div>
          </div>
          <div v-if="error" class="login-error">{{ error }}</div>
          <button class="btn btn-primary" :disabled="loading" type="submit">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </form>
      </div>
    </section>
  </main>
</template>
