import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/fixed-layout.css'
import './styles/bottom-nav.css'
import './styles/profile-page.css'
import './styles/batch-page.css'
import './styles.css'

function normalizeTargetPath(value) {
  return String(value || '').replace(/\/index\.html(?=([?#]|$))/, '/')
}
function normalizeAppBase() {
  const base = String(import.meta.env.BASE_URL || '/').trim()
  if (!base || base === '/') return ''
  return base.endsWith('/') ? base.slice(0, -1) : base
}

function getBootstrapRedirectPath() {
  let params = null
  try {
    params = new URLSearchParams(window.location.search || '')
  } catch (error) {
    return ''
  }
  const raw = String(params.get('to') || '').trim()
  if (!raw) return ''
  if (raw.charAt(0) !== '/') return ''
  if (raw.indexOf('//') === 0) return ''

  const appBase = normalizeAppBase()
  if (!appBase) return normalizeTargetPath(raw)
  if (raw === appBase) return '/'
  if (raw.indexOf(`${appBase}/`) === 0) {
    return normalizeTargetPath(raw.slice(appBase.length) || '/')
  }
  return normalizeTargetPath(raw)
}

async function bootstrap() {
  const app = createApp(App)
  app.use(router)
  const redirectPath = getBootstrapRedirectPath()

  await router.isReady()
  if (redirectPath) {
    try {
      await router.replace(redirectPath)
    } catch (error) {}
  }
  app.mount('#app')
}

bootstrap()
