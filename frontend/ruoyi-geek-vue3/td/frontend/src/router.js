import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from './pages/LoginPage.vue'
import TdxSmsReceivePage from './pages/TdxSmsReceivePage.vue'
import TdxTaskDetailPage from './pages/TdxTaskDetailPage.vue'
import TdxAuditPage from './pages/TdxAuditPage.vue'
import { authState, isAuthenticated, restoreSession } from './stores/authStore'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/appeal' },
    { path: '/login', component: LoginPage, meta: { public: true } },
    { path: '/appeal', component: TdxSmsReceivePage, meta: { requiresAuth: true } },
    { path: '/appeal/:taskId', component: TdxTaskDetailPage, meta: { requiresAuth: true } },
    { path: '/audit', component: TdxAuditPage, meta: { requiresAuth: true } },
  ],
})

router.beforeEach(async (to) => {
  if (!authState.ready) await restoreSession()
  if (to.meta.requiresAuth && !isAuthenticated()) return '/login'
  if (to.path === '/login' && isAuthenticated()) return '/appeal'
  return true
})

export default router
