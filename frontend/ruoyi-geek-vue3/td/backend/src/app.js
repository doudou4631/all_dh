import Fastify from 'fastify'
import cors from '@fastify/cors'
import registerRoutes from './routes/index.js'
import { startOrderAutoCheckService } from './services/orderAutoCheckService.js'

export async function buildApp() {
  const app = Fastify({ logger: false })

  await app.register(cors, {
    origin: process.env.CORS_ORIGIN?.split(',').map((item) => item.trim()) || true,
  })

  await app.register(registerRoutes)

  const stopOrderAutoCheck = startOrderAutoCheckService()
  app.addHook('onClose', async () => {
    stopOrderAutoCheck?.()
  })

  return app
}
