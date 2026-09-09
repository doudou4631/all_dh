import authRoutes from './auth.js'
import tdxRoutes from './tdx.js'
import { ok } from '../lib/response.js'
import { requireAuth } from '../lib/authGuard.js'

export default async function registerRoutes(app) {
  app.get('/health', async () => ok({ service: 'td-sms-receive', status: 'ok' }))

  await app.register(async function api(apiApp) {
    await apiApp.register(authRoutes, { prefix: '/auth' })

    await apiApp.register(async function protectedApi(protectedApp) {
      protectedApp.addHook('preHandler', requireAuth)
      await protectedApp.register(tdxRoutes, { prefix: '/tdx' })
    })
  }, { prefix: '/api' })
}
