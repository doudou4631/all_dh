import { getSession, parseBearerToken } from './auth.js'
import { query } from '../data/liveDb.js'
import { timingSafeEqual } from 'node:crypto'

function isAllowed(value) {
  return value === '允许' || value === 'allow' || value === true
}

function getModuleCandidates(moduleName = '') {
  const normalized = String(moduleName || '').trim()
  if (!normalized) return []
  if (normalized === '代理管理') return ['代理管理', '代理商体系']
  if (normalized === '代理商体系') return ['代理商体系', '代理管理']
  return [normalized]
}

export async function requireAuth(request, reply) {
  const integrationToken = String(process.env.TDX_INTEGRATION_TOKEN || '').trim()
  const suppliedIntegrationToken = String(request.headers['x-tdx-integration-token'] || '').trim()
  const integrationPath = String(request.url || '').split('?')[0]
  const expectedTokenBuffer = Buffer.from(integrationToken)
  const suppliedTokenBuffer = Buffer.from(suppliedIntegrationToken)
  const integrationTokenMatches = integrationToken
    && suppliedIntegrationToken
    && expectedTokenBuffer.length === suppliedTokenBuffer.length
    && timingSafeEqual(expectedTokenBuffer, suppliedTokenBuffer)

  const isIntegrationSmsPath = integrationPath.startsWith('/api/tdx/sms-receive/')
  const isIntegrationAuditPath = integrationPath === '/api/tdx/tasks'
  const isIntegrationLine3RetryPath = /^\/api\/tdx\/tasks\/[^/]+\/retry-line3$/.test(integrationPath)
  if (integrationTokenMatches && (isIntegrationSmsPath || isIntegrationAuditPath || isIntegrationLine3RetryPath)) {
    const liveUserId = Number(process.env.TDX_INTEGRATION_USER_ID || 0)
    if (!liveUserId) {
      return reply.code(503).send({
        success: false,
        message: 'TDX 服务账号未配置。',
        data: null,
      })
    }
    request.currentUser = {
      liveUserId,
      username: String(process.env.TDX_INTEGRATION_USERNAME || 'tdx-integration').trim(),
      role: 'super-admin',
      daililevel: 1,
      access: {
        modules: [{ module: '泰迪熊标记', read: '允许', create: '允许', edit: '允许', delete: '禁止' }],
        menuUrls: [],
      },
    }
    request.tdxIntegration = true
    return
  }

  const token = parseBearerToken(request.headers.authorization)
  const session = getSession(token)

  if (!session) {
    return reply.code(401).send({
      success: false,
      message: '需要先登录后才能访问。',
      data: null,
    })
  }

  request.currentUser = session.user
}

export function requireModuleAccess(moduleName, action = 'read') {
  return async function moduleAccessGuard(request, reply) {
    const modules = request.currentUser?.access?.modules || []
    const candidates = getModuleCandidates(moduleName)
    const target = modules.find((item) => candidates.includes(String(item?.module || '').trim()))

    const allowed = target && (isAllowed(target[action]) || isAllowed(target.read))
    if (!allowed) {
      return reply.code(403).send({
        success: false,
        message: `当前账号无权访问【${moduleName}】模块。`,
        data: {
          module: moduleName,
          action,
        },
      })
    }
  }
}

export async function requireSecondaryAgentOrAdmin(request, reply) {
  const level = Number(request.currentUser?.daililevel || 0)
  const roleId = Number(request.currentUser?.roleId || 0)
  const isAdmin = request.currentUser?.role === 'super-admin' || request.currentUser?.username === 'admin'
  
  let hasDbPermission = false
  if (roleId > 0) {
    const rows = await query(
      `SELECT 1 FROM t_menu m JOIN t_menu_role_action ra ON ra.MenuId = m.Id
        WHERE ra.RoleId = ? AND m.MenuUrl = ? AND ra.ActionId IN (0, 3) AND m.Status = 1 LIMIT 1`,
      [roleId, '/business/orderdeal/chaxiaomi']
    )
    hasDbPermission = rows.length > 0
  }

  if (level !== 2 && !isAdmin && !hasDbPermission) {
    return reply.code(403).send({
      success: false,
      message: '仅二级代理及管理员账号可访问此模块。',
      data: null,
    })
  }
}
