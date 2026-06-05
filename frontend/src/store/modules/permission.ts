import auth from '@/plugins/auth'
import { router } from '@/router'
import Layout from '@/layout/index.vue'
import ParentView from '@/components/ParentView/index.vue'
import InnerLink from '@/layout/components/InnerLink/index.vue'
import { defineStore } from 'pinia'
import type { Component } from 'vue'
import { RouteItem } from '@/types/route'
import { constantRoutes } from '@/router/routes/staticRoutes'
import { dynamicRoutes } from '@/router/routes/asyncRoutes'
import { deepClone } from '@/utils'
import { getRouters } from '@/api/login'
import { listMarkUserPlatformPrice } from '@/api/server/markUser'

// 匹配views里面所有的.vue文件
const modules = import.meta.glob(['../../**/views/**/*.vue', '../../**/view/**/*.vue'])

// 定义 store 状态接口
interface PermissionState {
  routes: RouteItem[]
  addRoutes: RouteItem[]
  defaultRoutes: RouteItem[]
  topbarRouters: RouteItem[]
  sidebarRouters: RouteItem[]
}

interface MarkPlatformOption {
  platformCode: string
  platformName: string
}

function parseRouteQuery(query: unknown): Record<string, any> {
  if (!query) return {}
  if (typeof query === 'string') {
    try {
      return JSON.parse(query)
    } catch (e) {
      return {}
    }
  }
  if (typeof query === 'object') {
    return query as Record<string, any>
  }
  return {}
}

function normalizeMarkPlatformOptions(rawList: unknown): MarkPlatformOption[] {
  if (!Array.isArray(rawList)) return []
  const seen = new Set<string>()
  const result: MarkPlatformOption[] = []
  rawList.forEach((item: any) => {
    const platformCode = String(item?.platformCode || '').trim()
    if (!platformCode || seen.has(platformCode)) return
    seen.add(platformCode)
    const platformName = String(item?.platformName || platformCode).trim() || platformCode
    result.push({ platformCode, platformName })
  })
  return result
}

function isMarkUserPlatformRoute(route: RouteItem): boolean {
  return typeof route.component === 'string' && route.component === 'server/mark/user/index'
}

function getPlatformCodeFromRoute(route: RouteItem): string {
  const query = parseRouteQuery(route.query)
  return String(query?.platformCode || '').trim()
}

function buildMarkUserPath(index: number, fallbackPath = 'markUser'): string {
  if (index === 0 && fallbackPath) return String(fallbackPath)
  return `markUser${index + 1}`
}
function buildMarkUserQuery(platform: MarkPlatformOption): string {
  return JSON.stringify({
    platformCode: platform.platformCode,
    platformName: platform.platformName
  })
}

function rewriteMarkUserChildren(children: RouteItem[], platformList: MarkPlatformOption[]): RouteItem[] {
  if (!Array.isArray(children) || children.length === 0) return children
  const markChildren = children.filter(item => isMarkUserPlatformRoute(item))
  if (markChildren.length === 0) {
    return children.map(item => ({
      ...item,
      children: item.children ? rewriteMarkUserChildren(item.children, platformList) : item.children
    }))
  }
  const baseRoute = markChildren[0]
  const existingByCode = new Map<string, RouteItem>()
  const existingPaths = new Set<string>()
  markChildren.forEach(item => {
    const code = getPlatformCodeFromRoute(item)
    if (code) existingByCode.set(code, item)
    if (item.path) existingPaths.add(String(item.path))
  })

  const rewrittenMarkChildren: RouteItem[] = platformList.map((platform, index) => {
    const source = existingByCode.get(platform.platformCode) || baseRoute
    const existing = existingByCode.get(platform.platformCode)
    const cloned = deepClone(source)
    const pathFallback = typeof source.path === 'string' && source.path ? source.path : 'markUser'
    const basePath = buildMarkUserPath(index, pathFallback)
    let newPath = existing?.path || basePath
    let suffix = 1
    while (existingPaths.has(String(newPath)) && !existing) {
      newPath = `${basePath}-${suffix}`
      suffix += 1
    }
    existingPaths.add(String(newPath))
    cloned.path = String(newPath)
    cloned.query = buildMarkUserQuery(platform)
    cloned.meta = {
      ...(cloned.meta || {}),
      title: platform.platformName
    }
    return cloned
  })
  const rewrittenPathSet = new Set(
    rewrittenMarkChildren
      .map(item => String(item.path || '').trim())
      .filter(path => path.length > 0)
  )
  const fallbackPlatform = platformList[0]
  const fallbackRoutes: RouteItem[] = []
  if (fallbackPlatform) {
    markChildren.forEach((item, index) => {
      const itemPath = String(item.path || '').trim()
      if (!itemPath || rewrittenPathSet.has(itemPath)) return
      const fallbackRoute = deepClone(item)
      fallbackRoute.hidden = true
      fallbackRoute.query = buildMarkUserQuery(fallbackPlatform)
      fallbackRoute.meta = {
        ...(fallbackRoute.meta || {}),
        title: fallbackPlatform.platformName
      }
      if (typeof fallbackRoute.name === 'string' && fallbackRoute.name.length > 0) {
        fallbackRoute.name = `${fallbackRoute.name}__fallback`
      } else {
        fallbackRoute.name = `markUserFallback${index + 1}`
      }
      fallbackRoutes.push(fallbackRoute)
    })
  }

  const rewrittenChildren: RouteItem[] = []
  let injected = false
  children.forEach(item => {
    if (isMarkUserPlatformRoute(item)) {
      if (!injected) {
        rewrittenChildren.push(...rewrittenMarkChildren)
        rewrittenChildren.push(...fallbackRoutes)
        injected = true
      }
      return
    }
    rewrittenChildren.push({
      ...item,
      children: item.children ? rewriteMarkUserChildren(item.children, platformList) : item.children
    })
  })
  return rewrittenChildren
}

async function rewriteMarkUserRoutesByTemplate(routes: RouteItem[]): Promise<RouteItem[]> {
  if (!Array.isArray(routes) || routes.length === 0) return routes
  if (!auth.hasPermi('server:markUser:price:list')) return routes
  try {
    const resp: any = await listMarkUserPlatformPrice()
    const platformList = normalizeMarkPlatformOptions(resp?.data)
    if (platformList.length === 0) return routes
    return rewriteMarkUserChildren(routes, platformList)
  } catch (e) {
    return routes
  }
}

/**
 * 权限管理模块
 * 
 * 路由生成说明：
 * 1. generateRoutes方法负责生成所有路由：
 *    - 从后端获取动态路由数据
 *    - 处理动态路由数据（过滤、转换组件等）
 *    - 根据设置决定TopNav菜单的数据来源
 * 
 * 2. TopNav菜单数据生成规则：
 *    - 启用TopNav导入本地路由：constantRoutes + defaultRoutes
 *    - 关闭TopNav导入本地路由：仅使用defaultRoutes
 *    - 通过settingsStore.topNavMixMenu控制
 */

const usePermissionStore = defineStore(
  'permission',
  {
    state: (): PermissionState => ({
      routes: [],
      addRoutes: [],
      defaultRoutes: [],
      topbarRouters: [],
      sidebarRouters: []
    }),
    actions: {
      setRoutes(routes: RouteItem[]) {
        this.routes = [...constantRoutes, ...routes];
      },
      setDefaultRoutes(routes: RouteItem[]) {
        this.defaultRoutes = deepClone(routes);
      },
      setTopbarRoutes(routes: RouteItem[]) {
        this.topbarRouters = deepClone(routes);
      },
      setSidebarRouters(routes: RouteItem[]) {
        this.sidebarRouters = deepClone(routes);
      },
      generateRoutes(): Promise<RouteItem[]> {
        return new Promise(resolve => {
          // 向后端请求路由数据
          getRouters().then(async res => {
            const routeData = await rewriteMarkUserRoutesByTemplate(deepClone(res.data))
            const sidebarRoutes = constantRoutes.concat(filterAsyncRouter(deepClone(routeData)))
            const rewriteRoutes = filterAsyncRouter(deepClone(routeData), true)
            const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
            asyncRoutes.forEach(route => { router.addRoute(route) })
            this.setRoutes(rewriteRoutes)
            this.setSidebarRouters(sidebarRoutes)
            this.setDefaultRoutes(sidebarRoutes)
            this.setTopbarRoutes(sidebarRoutes)
            resolve(rewriteRoutes)
          })
        })
      }
    }
  })

// 遍历后台传来的路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap: RouteItem[], type = false): RouteItem[] {
  return asyncRouterMap.filter(route => {
    // 确保route有hidden属性
    if (route.hidden === undefined) {
      route.hidden = false;
    }

    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      // Layout ParentView 组件特殊处理
      if (typeof route.component === 'string') {
        if (route.component === 'Layout') {
          route.component = Layout
        } else if (route.component === 'ParentView') {
          route.component = ParentView
        } else if (route.component === 'InnerLink') {
          route.component = InnerLink
        } else {
          const viewPath = route.component
          route.meta = { ...route.meta, componentView: viewPath }
          route.component = loadView(viewPath)
        }
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, type)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}
/**
 * 递归过滤并处理路由子项，将所有子路由展平为一个数组，并根据父路由调整路径。
 * 如果遇到组件为 'ParentView' 的路由，会将其子路由的路径拼接父路由路径，并继续递归处理。
 * 
 * @param childrenMap 路由子项数组
 * @param lastRouter 上一级父路由（可选），用于路径拼接
 * @returns 处理后的路由子项数组
 */
function filterChildren(childrenMap: RouteItem[], lastRouter?: RouteItem): RouteItem[] {
  const children: RouteItem[] = []
  childrenMap.forEach((el) => {
    const item = { ...el, hidden: false } // 确保hidden属性存在
    if (el.children && el.children.length) {
      if (el.component === 'ParentView' && !lastRouter) {
        el.children.forEach((c: RouteItem) => {
          c.path = el.path + '/' + c.path
          if (c.children && c.children.length) {
            children.push(...filterChildren(c.children, c))
            return
          }
          children.push(c)
        })
        return
      }
    }
    if (lastRouter && lastRouter.path) {
      item.path = lastRouter.path + '/' + item.path
    }
    children.push(item)
  })
  return children
}

// 动态路由遍历，验证是否具备权限
function filterDynamicRoutes(routes: readonly RouteItem[]): RouteItem[] {
  const res: RouteItem[] = []
  routes.forEach(route => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route)
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route)
      }
    }
  })
  return res
}

const loadView = (view: string): (() => Promise<Component>) => {
  let res: (() => Promise<Component>) | undefined
  for (const path in modules) {
    // 要考虑views 或者view 两种情况
    let dir = ''
    if (path.includes('modules/')) {
      dir += path.split('modules/')[1].split('/view')[0]
      dir += "/"
    }
    if (path.includes('views/')) {
      dir += path.split('views/')[1].split('.vue')[0]
    } else if (path.includes('view/')) {
      dir += path.split('view/')[1].split('.vue')[0]
    }
    if (dir === view) {
      res = modules[path] as () => Promise<Component>
    }
  }
  return res || (() => Promise.resolve({} as Component))
}

export default usePermissionStore
