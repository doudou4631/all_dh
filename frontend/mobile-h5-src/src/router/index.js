import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import ResultPage from '@/pages/ResultPage.vue'
import BatchPage from '@/pages/BatchPage.vue'
import ProfilePage from '@/pages/ProfilePage.vue'
import QueryRecordsPage from '@/pages/QueryRecordsPage.vue'
import RechargeRecordsPage from '@/pages/RechargeRecordsPage.vue'
import AboutPage from '@/pages/AboutPage.vue'
import AgreementPage from '@/pages/AgreementPage.vue'
import PrivacyPage from '@/pages/PrivacyPage.vue'
import TdxCaptchaPage from '@/pages/TdxCaptchaPage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      alias: '/index.html',
      name: 'home',
      component: HomePage
    },
    {
      path: '/result',
      alias: '/result/',
      name: 'result',
      component: ResultPage
    },
    {
      path: '/batch',
      alias: '/batch/',
      name: 'batch',
      component: BatchPage
    },
    {
      path: '/profile',
      alias: '/profile/',
      name: 'profile',
      component: ProfilePage
    },
    {
      path: '/profile/query-records.html',
      alias: ['/profile/query-records', '/profile/query-records/'],
      name: 'query-records',
      component: QueryRecordsPage
    },
    {
      path: '/profile/recharge-records.html',
      alias: ['/profile/recharge-records', '/profile/recharge-records/'],
      name: 'recharge-records',
      component: RechargeRecordsPage
    },
    {
      path: '/profile/about.html',
      alias: ['/profile/about', '/profile/about/'],
      name: 'profile-about',
      component: AboutPage
    },
    {
      path: '/profile/agreement.html',
      alias: ['/profile/agreement', '/profile/agreement/'],
      name: 'profile-agreement',
      component: AgreementPage
    },
    {
      path: '/profile/privacy.html',
      alias: ['/profile/privacy', '/profile/privacy/'],
      name: 'profile-privacy',
      component: PrivacyPage
    },
    {
      path: '/captcha/tdx',
      alias: ['/captcha/tdx/', '/captcha/tdx.html', '/pages/captcha/tdx', '/pages/captcha/tdx/'],
      name: 'tdx-captcha',
      component: TdxCaptchaPage
    }
  ]
})

export default router
