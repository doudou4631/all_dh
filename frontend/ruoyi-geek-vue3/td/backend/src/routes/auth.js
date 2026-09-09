import { ok } from '../lib/response.js'
import {
  createSession,
  enrichUserProfile,
  getSession,
  parseBearerToken,
  removeSession,
  validateCredentials,
} from '../lib/auth.js'
import { createCaptcha, verifyCaptcha } from '../lib/captcha.js'
import { getSystemSettingValue } from '../data/systemSettingsStore.js'
import { changeLiveUserPassword } from '../data/liveUserStore.js'

export default async function authRoutes(app) {
  app.get('/captcha', async () => ok(createCaptcha(), '验证码已生成'))

  app.get('/meta', async () => {
    const siteTitle = await getSystemSettingValue('siteTitle', '业务管理系统')
    const loginTitle = await getSystemSettingValue('loginTitle', '登录')
    const loginNotice = await getSystemSettingValue('loginNotice', '')
    return ok({ siteTitle, loginTitle, loginNotice })
  })

  app.post('/login', async (request, reply) => {
    const body = request.body || {}
    if (!verifyCaptcha(body.captchaId, body.captcha)) {
      return reply.code(400).send({ success: false, message: '验证码错误，请重新输入', data: null })
    }
    const user = await validateCredentials(body)
    if (!user) {
      return reply.code(401).send({ success: false, message: '账号或密码错误', data: null })
    }
    return ok(createSession(await enrichUserProfile(user)), '登录成功')
  })

  app.get('/session', async (request, reply) => {
    const token = parseBearerToken(request.headers.authorization)
    const session = getSession(token)
    if (!session) {
      return reply.code(401).send({ success: false, message: '登录状态已失效，请重新登录', data: null })
    }
    session.user = await enrichUserProfile(session.user)
    return ok(session)
  })

  app.post('/logout', async (request) => {
    const token = parseBearerToken(request.headers.authorization)
    if (token) removeSession(token)
    return ok(true, '已退出登录')
  })

  app.post('/change-password', async (request, reply) => {
    const token = parseBearerToken(request.headers.authorization)
    const session = getSession(token)
    if (!session) {
      return reply.code(401).send({ success: false, message: '登录状态已失效，请重新登录', data: null })
    }
    const body = request.body || {}
    if (!body.oldPassword || !body.newPassword) {
      return reply.code(400).send({ success: false, message: '请填写原密码和新密码', data: null })
    }
    if (!session.user.liveUserId) {
      return reply.code(400).send({ success: false, message: '当前账号不是线上旧版用户，无法在此处修改密码', data: null })
    }
    const result = await changeLiveUserPassword(session.user.liveUserId, body.oldPassword, body.newPassword)
    if (!result.ok) {
      return reply.code(400).send({ success: false, message: result.message, data: null })
    }
    return ok(true, result.message)
  })
}
