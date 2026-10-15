import assert from 'node:assert/strict'
import test from 'node:test'
import { buildApp } from '../src/app.js'

test('mounts public auth and protected TDX routes below /api', async (t) => {
  const previousAutoCheck = process.env.ORDER_AUTOCHECK_ENABLED
  const previousIntegrationToken = process.env.TDX_INTEGRATION_TOKEN
  const previousIntegrationUserId = process.env.TDX_INTEGRATION_USER_ID
  process.env.ORDER_AUTOCHECK_ENABLED = '0'
  process.env.TDX_INTEGRATION_TOKEN = 'test-integration-token'
  process.env.TDX_INTEGRATION_USER_ID = '1'
  const app = await buildApp()
  t.after(async () => {
    await app.close()
    if (previousAutoCheck === undefined) delete process.env.ORDER_AUTOCHECK_ENABLED
    else process.env.ORDER_AUTOCHECK_ENABLED = previousAutoCheck
    if (previousIntegrationToken === undefined) delete process.env.TDX_INTEGRATION_TOKEN
    else process.env.TDX_INTEGRATION_TOKEN = previousIntegrationToken
    if (previousIntegrationUserId === undefined) delete process.env.TDX_INTEGRATION_USER_ID
    else process.env.TDX_INTEGRATION_USER_ID = previousIntegrationUserId
  })

  const health = await app.inject({ method: 'GET', url: '/health' })
  const captcha = await app.inject({ method: 'GET', url: '/api/auth/captcha' })
  const legacyCaptcha = await app.inject({ method: 'GET', url: '/auth/captcha' })
  const protectedRoute = await app.inject({ method: 'GET', url: '/api/tdx/settings' })
  const missingChannel = await app.inject({
    method: 'POST',
    url: '/api/tdx/sms-receive/login',
    headers: { 'x-tdx-integration-token': 'test-integration-token' },
    payload: { phone: '13800138000', smsCode: '123456', line: 'line1' },
  })
  const invalidLine3Phone = await app.inject({
    method: 'POST',
    url: '/api/tdx/sms-receive/retry-line3',
    headers: { 'x-tdx-integration-token': 'test-integration-token' },
    payload: { phone: 'not-a-phone', smsCode: '123456' },
  })
  const invalidLine3SendPhone = await app.inject({
    method: 'POST',
    url: '/api/tdx/sms-receive/send-code',
    headers: { 'x-tdx-integration-token': 'test-integration-token' },
    payload: { phone: 'not-a-phone', line: 'line3' },
  })
  const missingLine3Source = await app.inject({
    method: 'POST',
    url: '/api/tdx/sms-receive/send-code',
    headers: { 'x-tdx-integration-token': 'test-integration-token' },
    payload: { phone: '13800138000', line: 'line3' },
  })

  assert.equal(health.statusCode, 200)
  assert.equal(captcha.statusCode, 200)
  assert.equal(legacyCaptcha.statusCode, 404)
  assert.equal(protectedRoute.statusCode, 401)
  assert.equal(missingChannel.statusCode, 503)
  assert.equal(missingChannel.json().message, '线路1暂时维护，请选择线路2。')
  assert.equal(invalidLine3Phone.statusCode, 503)
  assert.equal(invalidLine3Phone.json().message, '线路3暂时维护，请选择线路2。')
  assert.equal(invalidLine3SendPhone.statusCode, 503)
  assert.equal(invalidLine3SendPhone.json().message, '线路3暂时维护，请选择线路2。')
  assert.equal(missingLine3Source.statusCode, 503)
  assert.equal(missingLine3Source.json().message, '线路3暂时维护，请选择线路2。')
})
