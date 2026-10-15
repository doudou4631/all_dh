import assert from 'node:assert/strict'
import test from 'node:test'
import { buildApp } from '../src/app.js'

async function buildIntegrationApp(t) {
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
  return app
}

function integrationHeaders() {
  return { 'x-tdx-integration-token': 'test-integration-token' }
}

test('line1 and line3 reject requests while those channels are under maintenance', async (t) => {
  const app = await buildIntegrationApp(t)

  for (const line of ['line1', 'line3']) {
    const response = await app.inject({
      method: 'POST',
      url: '/api/tdx/sms-receive/login',
      headers: integrationHeaders(),
      // The maintenance guard must run before phone or line3 source validation.
      payload: { phone: 'not-a-phone', smsCode: '1234', line },
    })

    assert.equal(response.statusCode, 503, `${line} should return maintenance status`)
    assert.match(response.json().message, new RegExp(`${line === 'line1' ? '线路1' : '线路3'}.*维护`))
    assert.equal(response.json().data?.line, line)
    assert.equal(response.json().data?.maintenance, true)
  }
})

test('standalone Node service keeps line2 delegated to the external API', async (t) => {
  const app = await buildIntegrationApp(t)
  const response = await app.inject({
    method: 'POST',
    url: '/api/tdx/sms-receive/login',
    headers: integrationHeaders(),
    // The standalone Node service does not proxy the external API. Java's
    // production Gateway owns line2, so this service must keep returning its
    // explicit migration response instead of attempting a local request.
    payload: { phone: 'not-a-phone', smsCode: '1234', line: 'line2' },
  })

  assert.equal(response.statusCode, 410)
  assert.match(response.json().message, /线路2.*外部 API/)
  assert.equal(response.json().data?.replacement, 'tdx-line2-external-api')
})
