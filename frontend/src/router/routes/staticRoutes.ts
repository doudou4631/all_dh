import { RouteItem } from '@/types/route'
const Layout = () => import('@/layout/index.vue')

// ????,???????RouteItem??
export const constantRoutes: RouteItem[] = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue')
      }
    ]
  },
  {
    path: '/auth',
    component: () => import('@/views/auth/index.vue'),
    hidden: true,
    children: [
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/auth/login.vue'),
        hidden: true
      }
    ]
  },
  {
    path: '/free-query2',
    component: () => import('@/views/public/free-query2/index.vue'),
    hidden: true,
    meta: { title: '??????2' }
  },
  {
    path: '/free_query2',
    redirect: '/free-query2',
    hidden: true
  },
  {
    path: '/free-query-marked',
    component: () => import('@/views/public/free-query-marked/index.vue'),
    hidden: true,
    meta: { title: '?????????????' }
  },
  {
    path: '/free_query_marked',
    redirect: '/free-query-marked',
    hidden: true
  },
  {
    path: '/userModel',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'notice',
        component: () => import('@/views/server/user/notice.vue'),
        name: 'UserSysNotice',
        meta: { title: '????', icon: 'message' }
      }
    ]
  },
  {
    path: '/401',
    component: () => import('@/views/error/401.vue'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: '/index',
    children: [
      {
        path: '/index',
        component: () => import('@/views/index.vue'),
        name: 'Index',
        meta: { title: '??', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile',
        component: () => import('@/views/system/user/profile/index.vue'),
        name: 'Profile',
        meta: { title: '????', icon: 'user' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/error/404.vue'),
    hidden: true
  },
]
