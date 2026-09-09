import mysql from 'mysql2/promise'

let pool = null
let poolKey = ''

export function getConfig() {
  return {
    host: process.env.RIGHTCONTROL_DB_HOST || '127.0.0.1',
    port: Number(process.env.RIGHTCONTROL_DB_PORT || 3306),
    user: process.env.RIGHTCONTROL_DB_USER || 'root',
    password: process.env.RIGHTCONTROL_DB_PASSWORD ?? 'YOUR_DB_PASSWORD',
    database: process.env.RIGHTCONTROL_DB_NAME || 'RightControl',
    charset: 'utf8mb4',
    connectTimeout: 10000,
    dateStrings: true,
  }
}

function getPool() {
  const config = getConfig()
  if (config.password === 'YOUR_DB_PASSWORD') {
    throw new Error('RIGHTCONTROL_DB_PASSWORD is not configured')
  }
  if (!config.password && process.env.RIGHTCONTROL_DB_ALLOW_EMPTY_PASSWORD !== 'true') {
    throw new Error('RIGHTCONTROL_DB_PASSWORD is empty; set RIGHTCONTROL_DB_ALLOW_EMPTY_PASSWORD=true for a passwordless local database')
  }
  const nextPoolKey = JSON.stringify(config)
  if (!pool || poolKey !== nextPoolKey) {
    if (pool) {
      pool.end().catch(() => {})
    }
    poolKey = nextPoolKey
    pool = mysql.createPool({
      ...config,
      waitForConnections: true,
      connectionLimit: Number(process.env.RIGHTCONTROL_DB_POOL_LIMIT || 10),
      maxIdle: Number(process.env.RIGHTCONTROL_DB_POOL_IDLE || 5),
      idleTimeout: Number(process.env.RIGHTCONTROL_DB_POOL_IDLE_TIMEOUT_MS || 60000),
      enableKeepAlive: true,
      keepAliveInitialDelay: 0,
    })
  }
  return pool
}

export async function withConnection(handler) {
  const connection = await getPool().getConnection()
  try {
    return await handler(connection)
  } finally {
    connection.release()
  }
}

export async function closePool() {
  if (!pool) return
  const oldPool = pool
  pool = null
  poolKey = ''
  await oldPool.end()
}

export async function withTransaction(handler) {
  return withConnection(async (connection) => {
    await connection.beginTransaction()
    try {
      const result = await handler(connection)
      await connection.commit()
      return result
    } catch (error) {
      try {
        await connection.rollback()
      } catch {}
      throw error
    }
  })
}

export async function query(sql, params = []) {
  return withConnection(async (connection) => {
    const [rows] = await connection.query(sql, params)
    return rows
  })
}
